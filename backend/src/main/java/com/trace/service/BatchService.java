package com.trace.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.trace.contract.BatchRegistry;
import com.trace.dao.BatchMetaRepository;
import com.trace.dto.BatchDTO;
import com.trace.dto.request.CreateBatchRequest;
import com.trace.entity.BatchMeta;
import com.trace.entity.BatchStatus;
import com.trace.entity.DeliveryRecord;
import com.trace.util.HashUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.math.BigInteger;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 批次服务
 */
@Slf4j
@Service
public class BatchService {

    @Autowired
    private ChainService chainService;

    @Autowired
    private BatchMetaRepository batchMetaRepository;

    @Autowired
    private RolePermissionService rolePermissionService;

    @Autowired
    private DeliveryRecordService deliveryRecordService;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * 作废单个养殖群（区块链特性：数据不可删除，只能作废标记）
     */
    public void invalidateBatch(String batchId, String reason, String operatorId) {
        try {
            Optional<BatchMeta> opt = batchMetaRepository.findByBatchId(batchId);
            if (!opt.isPresent()) {
                throw new RuntimeException("养殖群不存在: " + batchId);
            }
            BatchMeta meta = opt.get();
            meta.setInvalidated(true);
            meta.setInvalidatedAt(java.time.LocalDateTime.now());
            meta.setInvalidatedBy(operatorId);
            meta.setInvalidateReason(reason);
            batchMetaRepository.save(meta);
            log.info("已作废养殖群：batchId={}，原因={}，操作人={}", batchId, reason, operatorId);
        } catch (Exception e) {
            log.error("作废养殖群失败", e);
            throw new RuntimeException("作废失败: " + e.getMessage());
        }
    }

    /**
     * 作废所有养殖群（管理员专用）
     * 区块链特性：数据不可删除，只能全部标记为作废
     */
    public void invalidateAllBatches(String reason, String operatorId) {
        try {
            List<BatchMeta> all = batchMetaRepository.findAll();
            LocalDateTime now = java.time.LocalDateTime.now();
            for (BatchMeta meta : all) {
                meta.setInvalidated(true);
                meta.setInvalidatedAt(now);
                meta.setInvalidatedBy(operatorId);
                meta.setInvalidateReason(reason);
            }
            batchMetaRepository.saveAll(all);
            log.info("已作废全部养殖群，数量={}，原因={}，操作人={}", all.size(), reason, operatorId);
        } catch (Exception e) {
            log.error("批量作废养殖群失败", e);
            throw new RuntimeException("批量作废失败: " + e.getMessage());
        }
    }

    /**
     * 查询养殖群列表（排除已作废的，除非特别说明）
     * @param includeInvalidated 是否包含已作废的养殖群
     */
    public List<BatchDTO> listBatches(Integer status, String role, String farmId, boolean includeInvalidated) {
        try {
            List<BatchMeta> allMeta = batchMetaRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt"));
            return allMeta.stream()
                    .filter(meta -> includeInvalidated || !Boolean.TRUE.equals(meta.getInvalidated()))
                    .map(meta -> {
                        try {
                            BatchDTO dto = buildBatchDTO(meta);
                            if (Boolean.TRUE.equals(meta.getInvalidated())) {
                                dto.setInvalidated(true);
                                dto.setInvalidateReason(meta.getInvalidateReason());
                            }
                            if (status != null && !status.equals(dto.getStatus())) {
                                return null;
                            }
                            if ("FARM".equals(role)) {
                                if (farmId == null || !farmId.equals(dto.getFarmId())) {
                                    return null;
                                }
                            }
                            if ("PROCESS".equals(role)) {
                                if (dto.getStatus() == null || dto.getStatus() < 1 || dto.getStatus() > 6) {
                                    return null;
                                }
                                String processId = (farmId != null && !farmId.isEmpty()) ? farmId : "";
                                if (!deliveryRecordService.canReceiverOperate(dto.getGroupId(), "PROCESS", processId, DeliveryRecordService.STAGE_TO_PROCESS)) {
                                    return null;
                                }
                            }
                            if ("SALES".equals(role)) {
                                if (dto.getStatus() == null || dto.getStatus() < 4 || dto.getStatus() > 6) {
                                    return null;
                                }
                                String salesId = (farmId != null && !farmId.isEmpty()) ? farmId : "";
                                if (!deliveryRecordService.canReceiverOperate(dto.getGroupId(), "SALES", salesId, DeliveryRecordService.STAGE_TO_SALES)) {
                                    return null;
                                }
                            }
                            return dto;
                        } catch (Exception e) {
                            log.warn("解析批次 {} 失败，跳过", meta.getBatchId(), e);
                            return null;
                        }
                    })
                    .filter(dto -> dto != null)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("查询批次列表失败", e);
            throw new RuntimeException("查询批次列表失败: " + e.getMessage());
        }
    }

    /**
     * 查询批次列表（默认排除已作废）
     */
    public List<BatchDTO> listBatches(Integer status, String role, String farmId) {
        return listBatches(status, role, farmId, false);
    }

    /**
     * 创建批次
     * 注意：方法本身不加 @Transactional，以便 save 使用仓储默认事务并立即提交；
     * 若整段包在同一事务里，链上调用等后续步骤异常会导致整笔回滚，Navicat 里看不到数据。
     */
    public String createBatch(CreateBatchRequest request) {
        try {
            // 1. 构建链下详细数据 JSON（始终合并主字段与前端传入的扩展 metaJson，避免仅存 description/source 导致查询缺字段）
            String metaJson = buildMetaJson(request);

            // 2. 计算链下数据哈希（须包含 batchId，避免不同批次因相同扩展 metaJson 触发 meta_hash 唯一约束冲突）
            String metaHash = HashUtil.sha256(request.getBatchId() + "\n" + metaJson);

            // 3. 保存链下详细数据（独立事务提交，不受后续链上调用影响）
            BatchMeta batchMeta = new BatchMeta();
            batchMeta.setBatchId(request.getBatchId());
            batchMeta.setMetaJson(metaJson);
            batchMeta.setMetaHash(metaHash);
            batchMeta.setStatus(0);
            batchMetaRepository.save(batchMeta);
            log.info("已持久化 batch_meta，batchId={}，id={}", request.getBatchId(), batchMeta.getId());

            // 4. 调用智能合约上链（锚定批次创建摘要）
            String txHash = chainService.createBatch(
                    request.getBatchId(),
                    request.getFarmId(),
                    request.getSpecies(),
                    request.getQuantity() != null ? request.getQuantity().intValueExact() : 0,
                    request.getLocation(),
                    metaHash
            );

            logTransactionProof("批次创建", batchMeta.getBatchId(), txHash, batchMeta);
            return txHash;

        } catch (Exception e) {
            log.error("创建批次失败", e);
            throw new RuntimeException("创建批次失败: " + e.getMessage());
        }
    }

    /**
     * 返回在栏（撤回出栏操作）
     * 条件：当前状态=1（已出栏），且加工厂尚未开始加工
     */
    public String returnToFarm(String batchId, String userRole, String userFarmId) {
        Optional<BatchMeta> opt = batchMetaRepository.findByBatchId(batchId);
        if (!opt.isPresent()) {
            throw new RuntimeException("批次不存在: " + batchId);
        }
        BatchMeta meta = opt.get();
        String batchFarmId = extractFromJson(meta.getMetaJson(), "farmId");

        // 权限校验：只有管理员或所属养殖场可以撤回
        if (!rolePermissionService.canSlaughter(userRole, batchFarmId, userFarmId)) {
            throw new RuntimeException("无权限撤回，只有管理员或所属养殖场可以操作");
        }

        // 状态校验：必须是已出栏状态（status=1）
        if (meta.getStatus() != 1) {
            throw new RuntimeException("只有已出栏状态的批次才能撤回");
        }

        // 检查加工厂是否已加工（不能撤回已加工的批次）
        Optional<DeliveryRecord> deliveryOpt = deliveryRecordService.getDeliveryRecordByBatchIdAndStage(
            batchId, DeliveryRecordService.STAGE_TO_PROCESS);
        if (deliveryOpt.isPresent()) {
            DeliveryRecord record = deliveryOpt.get();
            // 如果送达记录状态为已完成（已加工），不能撤回
            if (record.getStatus() == DeliveryRecordService.STATUS_COMPLETED) {
                throw new RuntimeException("加工厂已完成加工，无法撤回");
            }
        }

        // 先调用链上合约，链上成功后再更新链下状态，降低链下先变更导致的不一致风险
        String reason = "管理员回退：加工尚未开始，误触状态纠正";
        String txHash = chainService.adminRollback(
                batchId,
                1,
                0,
                reason,
                meta.getMetaHash()
        );

        // 状态改为在养（status=0）
        meta.setStatus(0);
        batchMetaRepository.save(meta);

        // 删除送达记录
        deliveryOpt.ifPresent(r -> deliveryRecordService.deleteDelivery(batchId, DeliveryRecordService.STAGE_TO_PROCESS));

        logTransactionProof("管理员回退", batchId, txHash, meta);
        return txHash;
    }

    /**
     * 查询批次信息
     */
    public BatchDTO getBatch(String groupId) {
        try {
            Optional<BatchMeta> metaOpt = batchMetaRepository.findByBatchId(groupId);
            if (!metaOpt.isPresent()) {
                throw new RuntimeException("养殖群不存在: " + groupId);
            }
            return buildBatchDTO(metaOpt.get());
        } catch (Exception e) {
            log.error("查询养殖群失败", e);
            throw new RuntimeException("查询养殖群失败: " + e.getMessage());
        }
    }

    /**
     * 构建 BatchDTO（供列表和详情共用）
     */
    private BatchDTO buildBatchDTO(BatchMeta meta) {
        BatchDTO dto = new BatchDTO();
        dto.setGroupId(meta.getBatchId());  // 养殖群ID
        dto.setMetaHash(meta.getMetaHash());
        dto.setStatus(meta.getStatus() != null ? meta.getStatus() : 0);

        if (meta.getMetaJson() != null && !meta.getMetaJson().isEmpty()) {
            try {
                dto.setFarmId(extractFromJson(meta.getMetaJson(), "farmId"));
                dto.setSpecies(extractFromJson(meta.getMetaJson(), "species"));
                dto.setSpeciesCategory(extractFromJson(meta.getMetaJson(), "speciesCategory"));
                String quantityStr = extractFromJson(meta.getMetaJson(), "quantity");
                if (quantityStr != null) {
                    try { dto.setQuantity(new BigInteger(quantityStr)); } catch (Exception ignored) {}
                }
                dto.setLocation(extractFromJson(meta.getMetaJson(), "location"));
            } catch (Exception e) {
                log.warn("解析批次 JSON 失败", e);
            }
        }

        dto.setCreatedAt(meta.getCreatedAt() != null ?
                meta.getCreatedAt().atZone(java.time.ZoneId.systemDefault()).toEpochSecond() :
                System.currentTimeMillis() / 1000);

        return dto;
    }
    
    /**
     * 构建元数据 JSON（Jackson 安全序列化，特殊字符自动转义）
     */
    private String buildMetaJson(CreateBatchRequest request) {
        try {
            java.util.Map<String, Object> map = new java.util.LinkedHashMap<>();
            map.put("groupId", request.getBatchId());
            map.put("farmId", request.getFarmId());
            map.put("species", request.getSpecies());
            if (request.getSpeciesCategory() != null) {
                map.put("speciesCategory", request.getSpeciesCategory());
            }
            map.put("quantity", request.getQuantity());
            map.put("location", request.getLocation());
            if (request.getMetaJson() != null && !request.getMetaJson().trim().isEmpty()) {
                try {
                    @SuppressWarnings("unchecked")
                    java.util.Map<String, Object> extra = objectMapper.readValue(
                            request.getMetaJson(), java.util.Map.class);
                    if (extra != null) {
                        for (java.util.Map.Entry<String, Object> e : extra.entrySet()) {
                            if (!map.containsKey(e.getKey())) {
                                map.put(e.getKey(), e.getValue());
                            }
                        }
                    }
                } catch (Exception ignored) {}
            }
            return objectMapper.writeValueAsString(map);
        } catch (JsonProcessingException e) {
            log.error("序列化批次元数据失败", e);
            throw new RuntimeException("序列化批次元数据失败: " + e.getMessage());
        }
    }

    /**
     * 从 JSON 安全提取字段（Jackson 解析，失败时回退正则）
     * 支持：数字(无引号)、数字字符串(有引号)、普通字符串
     */
    private String extractFromJson(String json, String key) {
        if (json == null || json.isEmpty()) return null;
        try {
            @SuppressWarnings("unchecked")
            java.util.Map<String, Object> map = objectMapper.readValue(json, java.util.Map.class);
            Object val = map.get(key);
            return val != null ? String.valueOf(val) : null;
        } catch (Exception e) {
            log.warn("Jackson 解析 JSON 失败，正则回退: {}", e.getMessage());
            try {
                // 兼容 quoted 字符串值，如 "quantity":"500" 或 "species":"白羽鸡"
                String quotedPattern = "\"" + key + "\"\\s*:\\s*\"([^\"\\\\]*(?:\\\\.[^\"\\\\]*)*)\"";
                java.util.regex.Matcher m1 = java.util.regex.Pattern.compile(quotedPattern).matcher(json);
                if (m1.find()) {
                    return m1.group(1).replace("\\\"", "\"").replace("\\\\", "\\");
                }
                // 兼容 quoted 数字值，如 "quantity":"123"
                String quotedNumPattern = "\"" + key + "\"\\s*:\\s*\"(\\d+)\"";
                java.util.regex.Matcher m2 = java.util.regex.Pattern.compile(quotedNumPattern).matcher(json);
                if (m2.find()) {
                    return m2.group(1);
                }
                // 原始无引号数字
                String numPattern = "\"" + key + "\"\\s*:\\s*(\\d+)";
                java.util.regex.Matcher m3 = java.util.regex.Pattern.compile(numPattern).matcher(json);
                if (m3.find()) {
                    return m3.group(1);
                }
            } catch (Exception ignored) {}
            return null;
        }
    }

    /**
     * 更新批次状态（带角色权限校验）
     * @param batchId 批次ID
     * @param newStatus 新状态
     * @param userRole 当前用户角色
     * @param userFarmId 当前用户所属养殖场ID（仅 FARM 角色时有效）
     * @param targetProcessId 目标加工厂ID（出栏时指定）
     * @param targetSalesId 目标销售商ID（送至零售商时指定）
     *
     * 状态流转规则：
     * 0=在养 → 1=出栏（需选加工厂）
     * 1=出栏 → 2=加工中
     * 2=加工中 → 3=加工完成
     * 3=加工完成 → 4=送至零售商（需选零售商）
     * 4=送至零售商 → 5=上架
     * 5=上架 → 6=已销售
     */
    public String updateBatchStatus(String batchId, Integer newStatus, String userRole, String userFarmId,
                                    String targetProcessId, String targetSalesId) {
        try {
            Optional<BatchMeta> opt = batchMetaRepository.findByBatchId(batchId);
            if (!opt.isPresent()) {
                throw new RuntimeException("批次不存在: " + batchId);
            }
            BatchMeta meta = opt.get();
            Integer currentStatus = meta.getStatus();

            // 从 metaJson 中提取 farmId 用于权限校验
            String batchFarmId = extractFromJson(meta.getMetaJson(), "farmId");
            boolean isAdmin = "ADMIN".equals(userRole);

            // 1. 校验状态转换是否合法（正向一步）或管理员单步回退
            boolean forwardOk = rolePermissionService.isValidStatusTransition(currentStatus, newStatus);
            boolean adminRollbackOk = isAdmin
                    && rolePermissionService.canRollback(userRole)
                    && rolePermissionService.isValidAdminRollbackTransition(currentStatus, newStatus);
            if (!forwardOk && !adminRollbackOk) {
                String fromDesc = BatchStatus.fromValue(currentStatus).getDescription();
                String toDesc = BatchStatus.fromValue(newStatus).getDescription();
                throw new RuntimeException("状态流转非法：" + fromDesc + " 不能直接转为 " + toDesc);
            }

            // 2. 角色权限与业务副作用：管理员单步回退仅改状态，避免把「回退到出栏」当成再次出栏而重复建送达记录
            if (!adminRollbackOk) {
                if (newStatus == 1) {
                    // 出栏：创建送达记录（养殖场→加工厂）
                    if (!rolePermissionService.canSlaughter(userRole, batchFarmId, userFarmId)) {
                        throw new RuntimeException("无权限执行出栏操作，只有管理员或所属养殖场可以操作");
                    }
                    // 出栏时必须指定目标加工厂
                    if (targetProcessId == null || targetProcessId.isEmpty()) {
                        throw new RuntimeException("出栏时必须指定目标加工厂");
                    }
                    // 创建送达记录
                    deliveryRecordService.createDelivery(
                        batchId, "FARM", batchFarmId, "PROCESS", targetProcessId,
                        DeliveryRecordService.STAGE_TO_PROCESS, null
                    );
                    log.info("已创建送达记录：batchId={} → targetProcessId={}", batchId, targetProcessId);
                } else if (newStatus == 2) {
                    // 送至加工：加工厂只能操作被指定送达给自己的批次；管理员不受送达关系限制
                    if (!rolePermissionService.canStartProcess(userRole, currentStatus)) {
                        throw new RuntimeException("无权限启动加工，只有管理员或加工厂可以操作已出栏的批次");
                    }
                    if (!isAdmin && !deliveryRecordService.canReceiverOperate(batchId, "PROCESS", userFarmId, DeliveryRecordService.STAGE_TO_PROCESS)) {
                        throw new RuntimeException("该批次未被指定送达给您，无权启动加工");
                    }
                } else if (newStatus == 3) {
                    // 加工完成：同上，管理员跳过送达校验
                    if (!rolePermissionService.canMarkProcessed(userRole)) {
                        throw new RuntimeException("无权限标记加工完成，只有管理员或加工厂可以操作");
                    }
                    if (!isAdmin && !deliveryRecordService.canReceiverOperate(batchId, "PROCESS", userFarmId, DeliveryRecordService.STAGE_TO_PROCESS)) {
                        throw new RuntimeException("该批次未被指定送达给您，无权标记加工完成");
                    }
                    // 标记送达记录为已完成
                    deliveryRecordService.markAsCompleted(batchId, DeliveryRecordService.STAGE_TO_PROCESS);
                    log.info("已标记送达完成：batchId={}", batchId);
                } else if (newStatus == 4) {
                    // 送至零售商：加工厂须为被指定的接收方；管理员不受送达关系限制
                    if (!rolePermissionService.canToRetailer(userRole)) {
                        throw new RuntimeException("无权限送至零售商，只有管理员或加工厂可以操作");
                    }
                    if (!isAdmin && !deliveryRecordService.canReceiverOperate(batchId, "PROCESS", userFarmId, DeliveryRecordService.STAGE_TO_PROCESS)) {
                        throw new RuntimeException("该批次未被指定送达给您，无权送至零售商");
                    }
                    // 送至零售商时必须指定目标销售商
                    if (targetSalesId == null || targetSalesId.isEmpty()) {
                        throw new RuntimeException("送至零售商时必须指定目标销售商");
                    }
                    // 创建送达记录的发送方加工厂：加工厂账号用 userFarmId；管理员从出栏送达记录中取目标加工厂
                    String processSenderId = userFarmId;
                    if (isAdmin) {
                        Optional<DeliveryRecord> processDr = deliveryRecordService.getDeliveryRecordByBatchIdAndStage(
                                batchId, DeliveryRecordService.STAGE_TO_PROCESS);
                        if (processDr.isPresent()) {
                            processSenderId = processDr.get().getToId();
                        }
                    }
                    if (processSenderId == null || processSenderId.isEmpty()) {
                        throw new RuntimeException("无法确定加工厂身份，请先完成出栏并指定加工厂，或使用加工厂账号操作");
                    }
                    // 创建新的送达记录（加工厂→销售商）
                    deliveryRecordService.createDelivery(
                        batchId, "PROCESS", processSenderId, "SALES", targetSalesId,
                        DeliveryRecordService.STAGE_TO_SALES, null
                    );
                    log.info("已创建送达记录（送至零售商）：batchId={} → targetSalesId={}", batchId, targetSalesId);
                } else if (newStatus == 5) {
                    // 上架：销售商须为被指定的接收方；管理员跳过送达校验
                    if (!rolePermissionService.canOnShelf(userRole)) {
                        throw new RuntimeException("无权限上架，只有管理员或销售商可以操作");
                    }
                    if (!isAdmin && !deliveryRecordService.canReceiverOperate(batchId, "SALES", userFarmId, DeliveryRecordService.STAGE_TO_SALES)) {
                        throw new RuntimeException("该批次未被指定送达给您，无权上架");
                    }
                } else if (newStatus == 6) {
                    // 已销售：同上，管理员跳过送达校验
                    if (!rolePermissionService.canSales(userRole)) {
                        throw new RuntimeException("无权限标记已销售，只有管理员或销售商可以操作");
                    }
                    if (!isAdmin && !deliveryRecordService.canReceiverOperate(batchId, "SALES", userFarmId, DeliveryRecordService.STAGE_TO_SALES)) {
                        throw new RuntimeException("该批次未被指定送达给您，无权标记已销售");
                    }
                    // 标记送达记录为已完成
                    deliveryRecordService.markAsCompleted(batchId, DeliveryRecordService.STAGE_TO_SALES);
                    log.info("已标记送达完成（已销售）：batchId={}", batchId);
                }
            } else {
                log.info("管理员回退状态：batchId={} {}→{}", batchId, currentStatus, newStatus);
            }

            meta.setStatus(newStatus);
            batchMetaRepository.save(meta);
            log.info("已持久化批次状态，batchId={}, status={}", batchId, newStatus);

            String txHash = chainService.updateBatchStatus(
                    batchId,
                    currentStatus,
                    newStatus,
                    meta.getMetaHash()
            );

            logTransactionProof("批次状态更新", batchId, txHash, meta);
            return txHash;

        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            log.error("更新批次状态失败", e);
            throw new RuntimeException("更新批次状态失败: " + e.getMessage());
        }
    }

    private void logTransactionProof(String action, String batchId, String txHash, BatchMeta batchMeta) {
        try {
            java.util.Map<String, Object> receipt = chainService.getTransactionReceipt(txHash);
            System.out.println("\n==================== 上链铁证 ====================");
            System.out.println("【" + action + "】batchId=" + batchId);
            System.out.println("TransactionHash = " + txHash);
            if (receipt != null) {
                System.out.println("BlockNumber      = " + receipt.get("blockNumber"));
                System.out.println("ReceiptStatus    = " + receipt.get("status"));
            } else {
                System.out.println("BlockNumber      = <null>");
                System.out.println("ReceiptStatus    = <null>");
            }
            System.out.println("=================================================");

            if (batchMeta != null) {
                try {
                    // 模拟模式下，直接从 batchMeta 构造模拟数据进行回读验证
                    if (chainService.isMockMode()) {
                        System.out.println("【模拟数据回读验证】");
                        System.out.println("farmId   = " + extractFromJson(batchMeta.getMetaJson(), "farmId"));
                        System.out.println("species  = " + extractFromJson(batchMeta.getMetaJson(), "species"));
                        System.out.println("quantity = " + extractFromJson(batchMeta.getMetaJson(), "quantity"));
                        System.out.println("location = " + extractFromJson(batchMeta.getMetaJson(), "location"));
                        System.out.println("=================================================");
                    } else {
                        // 真实模式下的链上回读（当前未实现，保持原有逻辑）
                        log.warn("真实模式下的链上回读功能尚未实现，跳过。");
                    }
                } catch (Exception readEx) {
                    log.warn("链上回读失败但不影响主流程: batchId={}, txHash={}", batchId, txHash, readEx);
                }
            }
        } catch (Exception e) {
            log.warn("打印上链铁证失败: action={}, batchId={}, txHash={}", action, batchId, txHash, e);
        }
    }
}
