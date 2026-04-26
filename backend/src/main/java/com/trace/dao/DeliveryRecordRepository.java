package com.trace.dao;

import com.trace.entity.DeliveryRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 送达记录数据访问层
 */
@Repository
public interface DeliveryRecordRepository extends JpaRepository<DeliveryRecord, Long> {

    /**
     * 根据养殖群ID查询送达记录
     */
    List<DeliveryRecord> findByBatchId(String batchId);

    /**
     * 根据养殖群ID和阶段查询送达记录
     * stage=1：出栏待加工（养殖场→加工厂）
     * stage=2：待收货（加工厂→销售商）
     */
    Optional<DeliveryRecord> findByBatchIdAndStage(String batchId, Integer stage);

    /**
     * 查询某接收方可操作的养殖群列表（用于 PROCESS 或 SALES 查看自己待处理的批次）
     */
    List<DeliveryRecord> findByToRoleAndToIdAndStageAndStatus(String toRole, String toId, Integer stage, Integer status);

    /**
     * 查询某接收方待处理的送达记录
     */
    List<DeliveryRecord> findByToRoleAndToIdAndStageAndStatusIn(String toRole, String toId, Integer stage, List<Integer> statuses);

    /**
     * 根据养殖群ID删除所有送达记录（用于批次删除时清理）
     */
    void deleteByBatchId(String batchId);

    /**
     * 根据养殖群ID和阶段删除送达记录（撤回时清理）
     */
    void deleteByBatchIdAndStage(String batchId, Integer stage);
}
