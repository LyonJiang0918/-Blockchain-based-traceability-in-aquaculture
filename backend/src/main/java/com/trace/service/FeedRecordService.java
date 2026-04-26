package com.trace.service;

import com.trace.dao.BatchMetaRepository;
import com.trace.dao.FeedRecordRepository;
import com.trace.dto.FeedRecordDTO;
import com.trace.dto.request.CreateFeedRecordRequest;
import com.trace.entity.FeedRecord;
import com.trace.util.HashUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 饲料投喂记录服务
 */
@Slf4j
@Service
public class FeedRecordService {

    @Autowired
    private FeedRecordRepository feedRecordRepository;

    @Autowired
    private BatchMetaRepository batchMetaRepository;

    @Autowired
    private ChainService chainService;

    /**
     * 创建饲料投喂记录
     */
    public String createFeedRecord(CreateFeedRecordRequest request) {
        try {
            // 验证养殖群是否存在
            if (!batchMetaRepository.existsByBatchId(request.getGroupId())) {
                throw new RuntimeException("养殖群不存在: " + request.getGroupId());
            }

            // 检查记录ID是否已存在
            if (feedRecordRepository.existsByRecordId(request.getRecordId())) {
                throw new RuntimeException("记录ID已存在: " + request.getRecordId());
            }

            // 计算总成本
            if (request.getTotalCost() == null && request.getUnitCost() != null && request.getAmount() != null) {
                request.setTotalCost(request.getUnitCost().multiply(request.getAmount()));
            }

            // 构建记录信息用于计算哈希
            String recordInfo = buildRecordInfo(request);
            String metaHash = HashUtil.sha256(request.getRecordId() + "\n" + recordInfo);

            // 创建记录实体
            FeedRecord feedRecord = new FeedRecord();
            feedRecord.setRecordId(request.getRecordId());
            feedRecord.setGroupId(request.getGroupId());
            feedRecord.setFeedType(request.getFeedType());
            feedRecord.setFeedBatchId(request.getFeedBatchId());
            feedRecord.setFeedBrand(request.getFeedBrand());
            feedRecord.setAmount(request.getAmount());
            feedRecord.setUnitCost(request.getUnitCost());
            feedRecord.setTotalCost(request.getTotalCost());
            feedRecord.setFeedingMethod(request.getFeedingMethod());
            feedRecord.setOperator(request.getOperator());
            feedRecord.setDescription(request.getDescription());
            feedRecord.setMetaHash(metaHash);
            feedRecord.setStatus(0);

            // 设置投喂日期
            if (request.getFeedDate() != null) {
                feedRecord.setFeedDate(LocalDateTime.ofInstant(
                    java.time.Instant.ofEpochSecond(request.getFeedDate()),
                    ZoneId.systemDefault()
                ));
            } else {
                feedRecord.setFeedDate(LocalDateTime.now());
            }

            feedRecordRepository.save(feedRecord);
            log.info("创建饲料投喂记录成功，recordId={}, groupId={}", request.getRecordId(), request.getGroupId());

            chainService.createFeedRecord(
                    request.getRecordId(),
                    request.getGroupId(),
                    request.getFeedType(),
                    request.getFeedBatchId(),
                    request.getFeedBrand(),
                    request.getAmount() != null ? request.getAmount().intValue() : 0,
                    request.getFeedingMethod(),
                    metaHash
            );

            return metaHash;

        } catch (Exception e) {
            log.error("创建饲料投喂记录失败", e);
            throw new RuntimeException("创建饲料投喂记录失败: " + e.getMessage());
        }
    }

    /**
     * 查询养殖群的所有饲料投喂记录
     */
    public List<FeedRecordDTO> listByGroupId(String groupId) {
        try {
            List<FeedRecord> records = feedRecordRepository.findByGroupIdOrderByFeedDateDesc(groupId);
            return records.stream()
                    .map(this::convertToDTO)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("查询饲料投喂记录失败", e);
            throw new RuntimeException("查询饲料投喂记录失败: " + e.getMessage());
        }
    }

    /**
     * 查询记录详情
     */
    public FeedRecordDTO getByRecordId(String recordId) {
        try {
            Optional<FeedRecord> opt = feedRecordRepository.findByRecordId(recordId);
            if (!opt.isPresent()) {
                throw new RuntimeException("记录不存在: " + recordId);
            }
            return convertToDTO(opt.get());
        } catch (Exception e) {
            log.error("查询饲料投喂记录失败", e);
            throw new RuntimeException("查询饲料投喂记录失败: " + e.getMessage());
        }
    }

    /**
     * 作废记录
     */
    public void voidRecord(String recordId) {
        try {
            Optional<FeedRecord> opt = feedRecordRepository.findByRecordId(recordId);
            if (!opt.isPresent()) {
                throw new RuntimeException("记录不存在: " + recordId);
            }
            FeedRecord record = opt.get();
            record.setStatus(1);
            feedRecordRepository.save(record);
            log.info("饲料投喂记录已作废，recordId={}", recordId);
        } catch (Exception e) {
            log.error("作废饲料投喂记录失败", e);
            throw new RuntimeException("作废饲料投喂记录失败: " + e.getMessage());
        }
    }

    /**
     * 构建记录信息字符串（用于计算哈希）
     */
    private String buildRecordInfo(CreateFeedRecordRequest request) {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("\"recordId\":\"").append(request.getRecordId()).append("\",");
        sb.append("\"groupId\":\"").append(request.getGroupId()).append("\",");
        sb.append("\"feedType\":\"").append(request.getFeedType()).append("\",");
        sb.append("\"amount\":").append(request.getAmount());
        if (request.getFeedingMethod() != null) {
            sb.append(",\"feedingMethod\":\"").append(request.getFeedingMethod()).append("\"");
        }
        if (request.getOperator() != null) {
            sb.append(",\"operator\":\"").append(request.getOperator()).append("\"");
        }
        sb.append("}");
        return sb.toString();
    }

    /**
     * 转换为DTO
     */
    private FeedRecordDTO convertToDTO(FeedRecord record) {
        FeedRecordDTO dto = new FeedRecordDTO();
        dto.setRecordId(record.getRecordId());
        dto.setGroupId(record.getGroupId());
        dto.setFeedType(record.getFeedType());
        dto.setFeedTypeText(record.getFeedTypeText());
        dto.setFeedBatchId(record.getFeedBatchId());
        dto.setFeedBrand(record.getFeedBrand());
        dto.setAmount(record.getAmount());
        dto.setUnitCost(record.getUnitCost());
        dto.setTotalCost(record.getTotalCost());
        dto.setFeedingMethod(record.getFeedingMethod());
        dto.setFeedingMethodText(record.getFeedingMethodText());
        dto.setOperator(record.getOperator());
        dto.setDescription(record.getDescription());
        dto.setMetaHash(record.getMetaHash());
        dto.setStatus(record.getStatus());
        dto.setStatusText(record.getStatusText());
        if (record.getFeedDate() != null) {
            dto.setFeedDate(record.getFeedDate().atZone(ZoneId.systemDefault()).toEpochSecond());
        }
        if (record.getCreatedAt() != null) {
            dto.setCreatedAt(record.getCreatedAt().atZone(ZoneId.systemDefault()).toEpochSecond());
        }
        return dto;
    }
}
