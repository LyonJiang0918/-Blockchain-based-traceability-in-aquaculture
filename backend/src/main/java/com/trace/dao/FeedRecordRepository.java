package com.trace.dao;

import com.trace.entity.FeedRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 饲料投喂记录数据访问层
 */
@Repository
public interface FeedRecordRepository extends JpaRepository<FeedRecord, Long> {

    Optional<FeedRecord> findByRecordId(String recordId);

    List<FeedRecord> findByGroupId(String groupId);

    List<FeedRecord> findByGroupIdOrderByFeedDateDesc(String groupId);

    boolean existsByRecordId(String recordId);
}
