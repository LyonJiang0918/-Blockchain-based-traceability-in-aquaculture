package com.trace.dao;

import com.trace.entity.InspectionRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface InspectionRecordRepository extends JpaRepository<InspectionRecord, Long> {
    Optional<InspectionRecord> findByRecordId(String recordId);
    List<InspectionRecord> findByBatchIdOrderByInspectDateDesc(String batchId);
    boolean existsByRecordId(String recordId);
}
