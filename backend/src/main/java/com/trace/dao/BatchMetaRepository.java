package com.trace.dao;

import com.trace.entity.BatchMeta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BatchMetaRepository extends JpaRepository<BatchMeta, Long> {
    Optional<BatchMeta> findByBatchId(String batchId);

    @Query("SELECT b FROM BatchMeta b WHERE LOWER(b.batchId) = LOWER(:id)")
    Optional<BatchMeta> findByBatchIdIgnoreCase(@Param("id") String batchId);

    Optional<BatchMeta> findByMetaHash(String metaHash);
    boolean existsByBatchId(String batchId);
}



