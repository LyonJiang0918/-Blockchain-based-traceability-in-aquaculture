package com.trace.service;

import com.trace.dao.AnimalRepository;
import com.trace.dao.BatchMetaRepository;
import com.trace.dao.ProcessRecordRepository;
import com.trace.dto.request.CreateProcessRecordRequest;
import com.trace.entity.Animal;
import com.trace.entity.ProcessRecord;
import com.trace.util.HashUtil;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 加工服务
 */
@Slf4j
@Service
public class ProcessService {

    @Autowired
    private ProcessRecordRepository processRecordRepository;

    @Autowired
    private AnimalRepository animalRepository;

    @Autowired
    private BatchMetaRepository batchMetaRepository;

    @Autowired
    private AnimalService animalService;

    @Autowired
    private RolePermissionService rolePermissionService;

    @Autowired
    private ChainService chainService;

    @Autowired
    private DeliveryRecordService deliveryRecordService;

    private static final DateTimeFormatter ID_DATE_FORMATTER =
            DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 启动加工
     * 将动物状态从 1（出栏）改为 2（加工中）
     */
    public Object startProcess(CreateProcessRecordRequest request,
                               String userRole, String userFarmId) {
        // 权限校验：管理员可操作；加工厂只能操作已出栏(status>=1)的批次
        if (!rolePermissionService.canStartProcess(userRole, 1)) {
            throw new RuntimeException("无权限启动加工，只有管理员或加工厂可以操作已出栏的批次");
        }

        // 生成加工记录ID
        String recordId = generateRecordId();
        request.setRecordId(recordId);

        // 解析输入动物ID列表
        List<String> inputAnimalIds = parseJsonArray(request.getInputAnimalIds());
        if (inputAnimalIds == null || inputAnimalIds.isEmpty()) {
            throw new RuntimeException("输入动物ID列表不能为空");
        }

        // 将动物状态改为"加工中"（status=2）
        animalService.updateAnimalStatus(inputAnimalIds, 2);

        // 解析产出副产品ID列表
        List<String> outputProductIds = parseJsonArray(request.getOutputProductIds());

        // 保存加工记录
        ProcessRecord record = new ProcessRecord();
        record.setRecordId(recordId);
        record.setBatchId(request.getBatchId());
        record.setProcessFactoryId(userFarmId);
        record.setProcessType(request.getProcessType());
        record.setInputAnimalIds(request.getInputAnimalIds());
        record.setOutputProductIds(request.getOutputProductIds());
        record.setInputCount(inputAnimalIds.size());
        record.setOutputCount(outputProductIds != null ? outputProductIds.size() : 0);
        record.setOperator(request.getOperator());
        record.setProcessStartTime(LocalDateTime.now());
        record.setStatus(0);

        String hashInput = recordId + "\n" + request.getBatchId() + "\n" +
                request.getProcessType() + "\n" + inputAnimalIds.size();
        record.setMetaHash(HashUtil.sha256(hashInput));

        processRecordRepository.save(record);
        log.info("已创建加工记录，recordId={}, batchId={}, inputCount={}",
                recordId, request.getBatchId(), inputAnimalIds.size());

        chainService.startProcess(
                recordId,
                request.getBatchId(),
                request.getProcessType(),
                inputAnimalIds.size(),
                record.getMetaHash()
        );

        return Collections.singletonMap("recordId", recordId);
    }

    /**
     * 完成加工
     * 将动物状态从 2（加工中）改为 3（已加工）
     */
    public void completeProcess(String recordId, CreateProcessRecordRequest request,
                                String userRole) {
        // 权限校验：只有管理员和加工厂可以完成加工
        if (!rolePermissionService.canMarkProcessed(userRole)) {
            throw new RuntimeException("无权限完成加工，只有管理员或加工厂可以操作");
        }

        Optional<ProcessRecord> opt = processRecordRepository.findByRecordId(recordId);
        if (!opt.isPresent()) {
            throw new RuntimeException("加工记录不存在: " + recordId);
        }

        ProcessRecord record = opt.get();
        if (record.getStatus() != 0) {
            throw new RuntimeException("该加工记录已处于完成状态");
        }

        // 解析并更新动物状态
        List<String> inputAnimalIds = parseJsonArray(record.getInputAnimalIds());
        if (inputAnimalIds != null && !inputAnimalIds.isEmpty()) {
            animalService.updateAnimalStatus(inputAnimalIds, 3);
        }

        // 解析并更新产出副产品状态
        List<String> outputProductIds = parseJsonArray(record.getOutputProductIds());
        if (outputProductIds != null && !outputProductIds.isEmpty()) {
            updateByProductStatus(outputProductIds, 1); // 1=已加工
        }

        // 更新加工记录状态
        record.setStatus(1);
        record.setProcessEndTime(LocalDateTime.now());
        if (request != null && request.getOutputProductIds() != null) {
            record.setOutputProductIds(request.getOutputProductIds());
        }
        processRecordRepository.save(record);

        chainService.completeProcess(
                recordId,
                record.getBatchId(),
                record.getProcessType(),
                record.getOutputCount(),
                record.getMetaHash()
        );

        log.info("已完成加工，recordId={}, batchId={}", recordId, record.getBatchId());
    }

    /**
     * 查询某批次的加工记录
     */
    public List<ProcessRecord> listByBatchId(String batchId) {
        return processRecordRepository.findByBatchId(batchId);
    }

    /**
     * 查询所有加工中记录
     * 管理员可看全部；加工厂只能看自己被送达的
     */
    public List<ProcessRecord> listProcessing(String userRole, String userFarmId) {
        List<ProcessRecord> records = processRecordRepository.findByStatus(0);
        if ("ADMIN".equals(userRole)) {
            return records;
        }
        if ("PROCESS".equals(userRole)) {
            // 只返回 batchId 在当前用户送达记录中的记录
            List<String> allowedBatchIds = deliveryRecordService.getDeliveriesBySender(
                "PROCESS", userFarmId, DeliveryRecordService.STAGE_TO_PROCESS
            ).stream().map(r -> r.getBatchId()).collect(Collectors.toList());
            return records.stream()
                .filter(r -> allowedBatchIds.contains(r.getBatchId()))
                .collect(Collectors.toList());
        }
        return records;
    }

    /**
     * 更新副产品状态
     */
    private void updateByProductStatus(List<String> productIds, Integer newStatus) {
        // ByProductService 尚未完全集成，这里记录日志
        log.info("待更新副产品状态，productIds={}, status={}", productIds, newStatus);
    }

    /**
     * 生成加工记录ID
     */
    private String generateRecordId() {
        String timestamp = LocalDateTime.now().format(ID_DATE_FORMATTER);
        int random = (int) (Math.random() * 10000);
        return "PROC" + timestamp + String.format("%04d", random);
    }

    /**
     * 解析JSON数组字符串
     */
    private List<String> parseJsonArray(String jsonArray) {
        if (jsonArray == null || jsonArray.trim().isEmpty()) {
            return null;
        }
        try {
            return objectMapper.readValue(jsonArray, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            log.warn("解析JSON数组失败: {}", jsonArray, e);
            return null;
        }
    }
}
