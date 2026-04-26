package com.trace.service;

import com.trace.dao.TransferRecordRepository;
import com.trace.dto.TransferRecordDTO;
import com.trace.dto.request.CreateTransferRecordRequest;
import com.trace.entity.TransferRecord;
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
public class TransferRecordService {

    @Autowired
    private TransferRecordRepository transferRecordRepository;

    @Autowired
    private ChainService chainService;

    public String createTransferRecord(CreateTransferRecordRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("流转记录不能为空");
        }
        if (transferRecordRepository.existsByRecordId(request.getRecordId())) {
            throw new RuntimeException("记录ID已存在: " + request.getRecordId());
        }

        TransferRecord record = new TransferRecord();
        record.setRecordId(request.getRecordId());
        record.setBatchId(request.getBatchId());
        record.setFromStage(request.getFromStage());
        record.setToStage(request.getToStage());
        record.setFromParty(request.getFromParty());
        record.setToParty(request.getToParty());
        if (request.getTransferDate() != null) {
            record.setTransferDate(LocalDateTime.ofInstant(
                    java.time.Instant.ofEpochSecond(request.getTransferDate().longValue()),
                    ZoneId.systemDefault()));
        }
        record.setQuantity(request.getQuantity());
        record.setTransportInfo(request.getTransportInfo());
        String metaHash = HashUtil.sha256(request.getRecordId() + "\n" + request.getBatchId() + "\n" + request.getFromStage() + "\n" + request.getToStage());
        record.setMetaHash(metaHash);
        transferRecordRepository.save(record);

        chainService.createTransferRecord(
                request.getRecordId(),
                request.getBatchId(),
                request.getFromStage(),
                request.getToStage(),
                request.getFromParty(),
                request.getToParty(),
                request.getTransferDate() != null ? request.getTransferDate().intValueExact() : (int) LocalDateTime.now().atZone(ZoneId.systemDefault()).toEpochSecond(),
                request.getQuantity() != null ? request.getQuantity().intValueExact() : 0,
                metaHash
        );
        return metaHash;
    }

    public List<TransferRecordDTO> listByBatchId(String batchId) {
        return transferRecordRepository.findByBatchIdOrderByTransferDateDesc(batchId)
                .stream().map(this::toDTO).collect(Collectors.toList());
    }

    public TransferRecordDTO getByRecordId(String recordId) {
        Optional<TransferRecord> opt = transferRecordRepository.findByRecordId(recordId);
        if (!opt.isPresent()) {
            throw new RuntimeException("流转记录不存在: " + recordId);
        }
        return toDTO(opt.get());
    }

    private TransferRecordDTO toDTO(TransferRecord record) {
        TransferRecordDTO dto = new TransferRecordDTO();
        dto.setRecordId(record.getRecordId());
        dto.setBatchId(record.getBatchId());
        dto.setFromStage(record.getFromStage());
        dto.setToStage(record.getToStage());
        dto.setFromParty(record.getFromParty());
        dto.setToParty(record.getToParty());
        if (record.getTransferDate() != null) {
            dto.setTransferDate(java.math.BigInteger.valueOf(record.getTransferDate().atZone(ZoneId.systemDefault()).toEpochSecond()));
        }
        dto.setQuantity(record.getQuantity() != null ? record.getQuantity().toBigInteger() : null);
        dto.setTransportInfo(record.getTransportInfo());
        dto.setMetaHash(record.getMetaHash());
        if (record.getCreatedAt() != null) {
            dto.setCreatedAt(record.getCreatedAt().atZone(ZoneId.systemDefault()).toEpochSecond());
        }
        return dto;
    }
}
