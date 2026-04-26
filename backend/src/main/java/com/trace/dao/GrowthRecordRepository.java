package com.trace.dao;

import com.trace.entity.GrowthRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 成长记录数据访问层
 */
@Repository
public interface GrowthRecordRepository extends JpaRepository<GrowthRecord, Long> {

    Optional<GrowthRecord> findByRecordId(String recordId);

    List<GrowthRecord> findByGroupId(String groupId);

    List<GrowthRecord> findByGroupIdOrderByRecordDateDesc(String groupId);

    boolean existsByRecordId(String recordId);
}
