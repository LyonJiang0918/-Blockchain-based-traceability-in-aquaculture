package com.trace.dao;

import com.trace.entity.TransferRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TransferRecordRepository extends JpaRepository<TransferRecord, Long> {
    Optional<TransferRecord> findByRecordId(String recordId);
    List<TransferRecord> findByBatchIdOrderByTransferDateDesc(String batchId);
    boolean existsByRecordId(String recordId);
}
