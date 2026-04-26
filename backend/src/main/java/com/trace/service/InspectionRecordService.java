package com.trace.service;

import com.trace.dao.BatchMetaRepository;
import com.trace.dao.InspectionRecordRepository;
import com.trace.dto.InspectionRecordDTO;
import com.trace.dto.request.CreateInspectionRecordRequest;
import com.trace.entity.InspectionRecord;
import com.trace.util.HashUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
public class InspectionRecordService {

    @Autowired
    private InspectionRecordRepository inspectionRecordRepository;

    @Autowired
    private BatchMetaRepository batchMetaRepository;

    @Autowired
    private ChainService chainService;

    public String createInspectionRecord(CreateInspectionRecordRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("检验记录不能为空");
        }
        if (!batchMetaRepository.existsByBatchId(request.getBatchId())) {
            throw new RuntimeException("养殖群不存在: " + request.getBatchId());
        }
        if (inspectionRecordRepository.existsByRecordId(request.getRecordId())) {
            throw new RuntimeException("记录ID已存在: " + request.getRecordId());
        }

        InspectionRecord record = new InspectionRecord();
        record.setRecordId(request.getRecordId());
        record.setBatchId(request.getBatchId());
        record.setInspectorId(request.getInspectorId());
        record.setInspectorName(request.getInspectorName());
        record.setInspectDate(request.getInspectDate() != null ? request.getInspectDate() : LocalDateTime.now());
        record.setResult(request.getResult());
        record.setReportHash(request.getReportHash());
        String metaHash = HashUtil.sha256(request.getRecordId() + "\n" + request.getBatchId() + "\n" + request.getInspectorId() + "\n" + request.getResult());
        record.setMetaHash(metaHash);
        inspectionRecordRepository.save(record);

        chainService.createInspectionRecord(
                request.getRecordId(),
                request.getBatchId(),
                request.getInspectorId(),
                request.getInspectorName(),
                request.getInspectDate() != null ? (int) request.getInspectDate().atZone(ZoneId.systemDefault()).toEpochSecond() : (int) LocalDateTime.now().atZone(ZoneId.systemDefault()).toEpochSecond(),
                request.getResult(),
                request.getReportHash(),
                metaHash
        );
        return metaHash;
    }

    public List<InspectionRecordDTO> listByBatchId(String batchId) {
        return inspectionRecordRepository.findByBatchIdOrderByInspectDateDesc(batchId)
                .stream().map(this::toDTO).collect(Collectors.toList());
    }

    public InspectionRecordDTO getByRecordId(String recordId) {
        Optional<InspectionRecord> opt = inspectionRecordRepository.findByRecordId(recordId);
        if (!opt.isPresent()) {
            throw new RuntimeException("检验记录不存在: " + recordId);
        }
        return toDTO(opt.get());
    }

    private InspectionRecordDTO toDTO(InspectionRecord record) {
        InspectionRecordDTO dto = new InspectionRecordDTO();
        dto.setRecordId(record.getRecordId());
        dto.setBatchId(record.getBatchId());
        dto.setInspectorId(record.getInspectorId());
        dto.setInspectorName(record.getInspectorName());
        if (record.getInspectDate() != null) {
            dto.setInspectDate(java.math.BigInteger.valueOf(
                    record.getInspectDate().atZone(ZoneId.systemDefault()).toEpochSecond()));
        }
        dto.setResult(record.getResult());
        dto.setReportHash(record.getReportHash());
        dto.setMetaHash(record.getMetaHash());
        if (record.getCreatedAt() != null) {
            dto.setCreatedAt(record.getCreatedAt().atZone(ZoneId.systemDefault()).toEpochSecond());
        }
        return dto;
    }
}
