package com.trace.service;

import com.trace.dao.BatchMetaRepository;
import com.trace.dao.VetRecordRepository;
import com.trace.dto.VetRecordDTO;
import com.trace.dto.request.CreateVetRecordRequest;
import com.trace.entity.VetRecord;
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
 * 兽医/疫苗记录服务
 */
@Slf4j
@Service
public class VetRecordService {

    @Autowired
    private VetRecordRepository vetRecordRepository;

    @Autowired
    private BatchMetaRepository batchMetaRepository;

    @Autowired
    private ChainService chainService;

    /**
     * 创建兽医/疫苗记录
     */
    public String createVetRecord(CreateVetRecordRequest request) {
        try {
            // 验证养殖群是否存在
            if (!batchMetaRepository.existsByBatchId(request.getGroupId())) {
                throw new RuntimeException("养殖群不存在: " + request.getGroupId());
            }

            // 检查记录ID是否已存在
            if (vetRecordRepository.existsByRecordId(request.getRecordId())) {
                throw new RuntimeException("记录ID已存在: " + request.getRecordId());
            }

            // 构建记录信息用于计算哈希
            String recordInfo = buildRecordInfo(request);
            String metaHash = HashUtil.sha256(request.getRecordId() + "\n" + recordInfo);

            // 创建记录实体
            VetRecord vetRecord = new VetRecord();
            vetRecord.setRecordId(request.getRecordId());
            vetRecord.setGroupId(request.getGroupId());
            vetRecord.setRecordType(request.getRecordType());
            vetRecord.setMedicineName(request.getMedicineName());
            vetRecord.setMedicineId(request.getMedicineId());
            vetRecord.setVaccineType(request.getVaccineType());
            vetRecord.setManufacturer(request.getManufacturer());
            vetRecord.setBatchNumber(request.getBatchNumber());
            vetRecord.setDosage(request.getDosage());
            vetRecord.setDosageUnit(request.getDosageUnit());
            vetRecord.setAdministrationRoute(request.getAdministrationRoute());
            vetRecord.setVetName(request.getVetName());
            vetRecord.setVetLicense(request.getVetLicense());
            vetRecord.setVetInstitution(request.getVetInstitution());
            vetRecord.setTargetAnimals(request.getTargetAnimals());
            vetRecord.setDiagnosis(request.getDiagnosis());
            vetRecord.setDescription(request.getDescription());
            vetRecord.setMetaHash(metaHash);
            vetRecord.setStatus(0);

            // 设置有效期
            if (request.getExpiryDate() != null) {
                vetRecord.setExpiryDate(LocalDateTime.ofInstant(
                    java.time.Instant.ofEpochSecond(request.getExpiryDate()),
                    ZoneId.systemDefault()
                ));
            }

            // 设置操作日期
            if (request.getOperationDate() != null) {
                vetRecord.setOperationDate(LocalDateTime.ofInstant(
                    java.time.Instant.ofEpochSecond(request.getOperationDate()),
                    ZoneId.systemDefault()
                ));
            } else {
                vetRecord.setOperationDate(LocalDateTime.now());
            }

            vetRecordRepository.save(vetRecord);
            log.info("创建兽医记录成功，recordId={}, groupId={}", request.getRecordId(), request.getGroupId());

            chainService.createVetRecord(
                    request.getRecordId(),
                    request.getGroupId(),
                    request.getRecordType(),
                    request.getVetLicense(),
                    request.getVetName(),
                    request.getMedicineName(),
                    request.getVaccineType(),
                    request.getDosage() != null ? request.getDosage().toPlainString() : null,
                    request.getDiagnosis(),
                    metaHash
            );

            return metaHash;

        } catch (Exception e) {
            log.error("创建兽医记录失败", e);
            throw new RuntimeException("创建兽医记录失败: " + e.getMessage());
        }
    }

    /**
     * 查询养殖群的所有兽医记录
     */
    public List<VetRecordDTO> listByGroupId(String groupId) {
        try {
            List<VetRecord> records = vetRecordRepository.findByGroupIdOrderByOperationDateDesc(groupId);
            return records.stream()
                    .map(this::convertToDTO)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("查询兽医记录失败", e);
            throw new RuntimeException("查询兽医记录失败: " + e.getMessage());
        }
    }

    /**
     * 按类型查询兽医记录
     */
    public List<VetRecordDTO> listByGroupIdAndType(String groupId, Integer recordType) {
        try {
            List<VetRecord> records = vetRecordRepository.findByGroupIdAndRecordType(groupId, recordType);
            return records.stream()
                    .map(this::convertToDTO)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("查询兽医记录失败", e);
            throw new RuntimeException("查询兽医记录失败: " + e.getMessage());
        }
    }

    /**
     * 查询记录详情
     */
    public VetRecordDTO getByRecordId(String recordId) {
        try {
            Optional<VetRecord> opt = vetRecordRepository.findByRecordId(recordId);
            if (!opt.isPresent()) {
                throw new RuntimeException("记录不存在: " + recordId);
            }
            return convertToDTO(opt.get());
        } catch (Exception e) {
            log.error("查询兽医记录失败", e);
            throw new RuntimeException("查询兽医记录失败: " + e.getMessage());
        }
    }

    /**
     * 作废记录
     */
    public void voidRecord(String recordId) {
        try {
            Optional<VetRecord> opt = vetRecordRepository.findByRecordId(recordId);
            if (!opt.isPresent()) {
                throw new RuntimeException("记录不存在: " + recordId);
            }
            VetRecord record = opt.get();
            record.setStatus(1);
            vetRecordRepository.save(record);
            log.info("兽医记录已作废，recordId={}", recordId);
        } catch (Exception e) {
            log.error("作废兽医记录失败", e);
            throw new RuntimeException("作废兽医记录失败: " + e.getMessage());
        }
    }

    /**
     * 构建记录信息字符串（用于计算哈希）
     */
    private String buildRecordInfo(CreateVetRecordRequest request) {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("\"recordId\":\"").append(request.getRecordId()).append("\",");
        sb.append("\"groupId\":\"").append(request.getGroupId()).append("\",");
        sb.append("\"recordType\":").append(request.getRecordType()).append(",");
        sb.append("\"medicineName\":\"").append(request.getMedicineName()).append("\"");
        if (request.getVetName() != null) {
            sb.append(",\"vetName\":\"").append(request.getVetName()).append("\"");
        }
        if (request.getDiagnosis() != null) {
            sb.append(",\"diagnosis\":\"").append(request.getDiagnosis()).append("\"");
        }
        sb.append("}");
        return sb.toString();
    }

    /**
     * 转换为DTO
     */
    private VetRecordDTO convertToDTO(VetRecord record) {
        VetRecordDTO dto = new VetRecordDTO();
        dto.setRecordId(record.getRecordId());
        dto.setGroupId(record.getGroupId());
        dto.setRecordType(record.getRecordType());
        dto.setRecordTypeText(record.getRecordTypeText());
        dto.setMedicineId(record.getMedicineId());
        dto.setMedicineName(record.getMedicineName());
        dto.setVaccineType(record.getVaccineType());
        dto.setVaccineTypeText(record.getVaccineTypeText());
        dto.setManufacturer(record.getManufacturer());
        dto.setBatchNumber(record.getBatchNumber());
        dto.setDosage(record.getDosage());
        dto.setDosageUnit(record.getDosageUnit());
        dto.setAdministrationRoute(record.getAdministrationRoute());
        dto.setVetName(record.getVetName());
        dto.setVetLicense(record.getVetLicense());
        dto.setVetInstitution(record.getVetInstitution());
        dto.setTargetAnimals(record.getTargetAnimals());
        dto.setDiagnosis(record.getDiagnosis());
        dto.setDescription(record.getDescription());
        dto.setMetaHash(record.getMetaHash());
        dto.setStatus(record.getStatus());
        dto.setStatusText(record.getStatusText());
        if (record.getExpiryDate() != null) {
            dto.setExpiryDate(record.getExpiryDate().atZone(ZoneId.systemDefault()).toEpochSecond());
        }
        if (record.getOperationDate() != null) {
            dto.setOperationDate(record.getOperationDate().atZone(ZoneId.systemDefault()).toEpochSecond());
        }
        if (record.getCreatedAt() != null) {
            dto.setCreatedAt(record.getCreatedAt().atZone(ZoneId.systemDefault()).toEpochSecond());
        }
        return dto;
    }
}
