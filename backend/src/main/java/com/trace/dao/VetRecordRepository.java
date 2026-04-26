package com.trace.dao;

import com.trace.entity.VetRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 兽医记录数据访问层
 */
@Repository
public interface VetRecordRepository extends JpaRepository<VetRecord, Long> {

    Optional<VetRecord> findByRecordId(String recordId);

    List<VetRecord> findByGroupId(String groupId);

    List<VetRecord> findByGroupIdAndRecordType(String groupId, Integer recordType);

    List<VetRecord> findByGroupIdOrderByOperationDateDesc(String groupId);

    boolean existsByRecordId(String recordId);
}
