package com.trace.service;

import com.trace.dao.BatchMetaRepository;
import com.trace.dao.GrowthRecordRepository;
import com.trace.dto.GrowthRecordDTO;
import com.trace.dto.request.CreateGrowthRecordRequest;
import com.trace.entity.GrowthRecord;
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
 * 成长记录服务
 */
@Slf4j
@Service
public class GrowthRecordService {

    @Autowired
    private GrowthRecordRepository growthRecordRepository;

    @Autowired
    private BatchMetaRepository batchMetaRepository;

    /**
     * 创建成长记录
     */
    public String createGrowthRecord(CreateGrowthRecordRequest request) {
        try {
            // 验证养殖群是否存在
            if (!batchMetaRepository.existsByBatchId(request.getGroupId())) {
                throw new RuntimeException("养殖群不存在: " + request.getGroupId());
            }

            // 检查记录ID是否已存在
            if (growthRecordRepository.existsByRecordId(request.getRecordId())) {
                throw new RuntimeException("记录ID已存在: " + request.getRecordId());
            }

            // 构建记录信息用于计算哈希
            String recordInfo = buildRecordInfo(request);
            String metaHash = HashUtil.sha256(request.getRecordId() + "\n" + recordInfo);

            // 创建记录实体
            GrowthRecord growthRecord = new GrowthRecord();
            growthRecord.setRecordId(request.getRecordId());
            growthRecord.setGroupId(request.getGroupId());
            growthRecord.setAvgWeight(request.getAvgWeight());
            growthRecord.setMaxWeight(request.getMaxWeight());
            growthRecord.setMinWeight(request.getMinWeight());
            growthRecord.setHealthStatus(request.getHealthStatus());
            growthRecord.setSurvivalCount(request.getSurvivalCount());
            growthRecord.setDeathCount(request.getDeathCount());
            growthRecord.setCullCount(request.getCullCount());
            growthRecord.setGrowthStage(request.getGrowthStage());
            growthRecord.setAppearanceCondition(request.getAppearanceCondition());
            growthRecord.setVitalityScore(request.getVitalityScore());
            growthRecord.setDescription(request.getDescription());
            growthRecord.setInspector(request.getInspector());
            growthRecord.setMetaHash(metaHash);
            growthRecord.setStatus(0);

            // 设置记录日期
            if (request.getRecordDate() != null) {
                growthRecord.setRecordDate(LocalDateTime.ofInstant(
                    java.time.Instant.ofEpochSecond(request.getRecordDate()),
                    ZoneId.systemDefault()
                ));
            } else {
                growthRecord.setRecordDate(LocalDateTime.now());
            }

            growthRecordRepository.save(growthRecord);
            log.info("创建成长记录成功，recordId={}, groupId={}", request.getRecordId(), request.getGroupId());

            return metaHash;

        } catch (Exception e) {
            log.error("创建成长记录失败", e);
            throw new RuntimeException("创建成长记录失败: " + e.getMessage());
        }
    }

    /**
     * 查询养殖群的所有成长记录
     */
    public List<GrowthRecordDTO> listByGroupId(String groupId) {
        try {
            List<GrowthRecord> records = growthRecordRepository.findByGroupIdOrderByRecordDateDesc(groupId);
            return records.stream()
                    .map(this::convertToDTO)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("查询成长记录失败", e);
            throw new RuntimeException("查询成长记录失败: " + e.getMessage());
        }
    }

    /**
     * 查询记录详情
     */
    public GrowthRecordDTO getByRecordId(String recordId) {
        try {
            Optional<GrowthRecord> opt = growthRecordRepository.findByRecordId(recordId);
            if (!opt.isPresent()) {
                throw new RuntimeException("记录不存在: " + recordId);
            }
            return convertToDTO(opt.get());
        } catch (Exception e) {
            log.error("查询成长记录失败", e);
            throw new RuntimeException("查询成长记录失败: " + e.getMessage());
        }
    }

    /**
     * 作废记录
     */
    public void voidRecord(String recordId) {
        try {
            Optional<GrowthRecord> opt = growthRecordRepository.findByRecordId(recordId);
            if (!opt.isPresent()) {
                throw new RuntimeException("记录不存在: " + recordId);
            }
            GrowthRecord record = opt.get();
            record.setStatus(1);
            growthRecordRepository.save(record);
            log.info("成长记录已作废，recordId={}", recordId);
        } catch (Exception e) {
            log.error("作废成长记录失败", e);
            throw new RuntimeException("作废成长记录失败: " + e.getMessage());
        }
    }

    /**
     * 构建记录信息字符串（用于计算哈希）
     */
    private String buildRecordInfo(CreateGrowthRecordRequest request) {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("\"recordId\":\"").append(request.getRecordId()).append("\",");
        sb.append("\"groupId\":\"").append(request.getGroupId()).append("\"");
        if (request.getAvgWeight() != null) {
            sb.append(",\"avgWeight\":").append(request.getAvgWeight());
        }
        if (request.getHealthStatus() != null) {
            sb.append(",\"healthStatus\":\"").append(request.getHealthStatus()).append("\"");
        }
        if (request.getGrowthStage() != null) {
            sb.append(",\"growthStage\":\"").append(request.getGrowthStage()).append("\"");
        }
        sb.append("}");
        return sb.toString();
    }

    /**
     * 转换为DTO
     */
    private GrowthRecordDTO convertToDTO(GrowthRecord record) {
        GrowthRecordDTO dto = new GrowthRecordDTO();
        dto.setRecordId(record.getRecordId());
        dto.setGroupId(record.getGroupId());
        dto.setAvgWeight(record.getAvgWeight());
        dto.setMaxWeight(record.getMaxWeight());
        dto.setMinWeight(record.getMinWeight());
        dto.setHealthStatus(record.getHealthStatus());
        dto.setHealthStatusText(record.getHealthStatusText());
        dto.setSurvivalCount(record.getSurvivalCount());
        dto.setDeathCount(record.getDeathCount());
        dto.setCullCount(record.getCullCount());
        dto.setGrowthStage(record.getGrowthStage());
        dto.setGrowthStageText(record.getGrowthStageText());
        dto.setAppearanceCondition(record.getAppearanceCondition());
        dto.setVitalityScore(record.getVitalityScore());
        dto.setDescription(record.getDescription());
        dto.setInspector(record.getInspector());
        dto.setMetaHash(record.getMetaHash());
        dto.setStatus(record.getStatus());
        dto.setStatusText(record.getStatusText());
        if (record.getRecordDate() != null) {
            dto.setRecordDate(record.getRecordDate().atZone(ZoneId.systemDefault()).toEpochSecond());
        }
        if (record.getCreatedAt() != null) {
            dto.setCreatedAt(record.getCreatedAt().atZone(ZoneId.systemDefault()).toEpochSecond());
        }
        return dto;
    }
}
