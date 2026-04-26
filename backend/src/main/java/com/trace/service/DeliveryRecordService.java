package com.trace.service;

import com.trace.dao.DeliveryRecordRepository;
import com.trace.entity.DeliveryRecord;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 送达记录服务
 * 管理养殖群在各环节的流转关系，实现"只有被指定的一方才能操作"的权限控制
 */
@Slf4j
@Service
public class DeliveryRecordService {

    public static final int STAGE_TO_PROCESS = 1;    // 出栏待加工（养殖场→加工厂）
    public static final int STAGE_TO_SALES = 2;      // 待收货（加工厂→销售商）

    public static final int STATUS_PENDING = 0;      // 待处理
    public static final int STATUS_COMPLETED = 1;    // 已完成/已接受
    public static final int STATUS_REJECTED = 2;     // 已拒绝

    @Autowired
    private DeliveryRecordRepository deliveryRecordRepository;

    /**
     * 创建送达记录（出栏时指定加工厂，或完成加工时指定销售商）
     */
    public DeliveryRecord createDelivery(String batchId, String fromRole, String fromId,
                                          String toRole, String toId, Integer stage, Integer quantity) {
        DeliveryRecord record = new DeliveryRecord();
        record.setBatchId(batchId);
        record.setFromRole(fromRole);
        record.setFromId(fromId);
        record.setToRole(toRole);
        record.setToId(toId);
        record.setStage(stage);
        record.setQuantity(quantity);
        record.setStatus(STATUS_PENDING);
        return deliveryRecordRepository.save(record);
    }

    /**
     * 查询某养殖群的所有送达记录
     */
    public List<DeliveryRecord> getDeliveryRecordsByBatchId(String batchId) {
        return deliveryRecordRepository.findByBatchId(batchId);
    }

    /**
     * 查询某养殖群在指定阶段的送达记录
     */
    public Optional<DeliveryRecord> getDeliveryRecordByBatchIdAndStage(String batchId, Integer stage) {
        return deliveryRecordRepository.findByBatchIdAndStage(batchId, stage);
    }

    /**
     * 查询某接收方可操作的养殖群列表
     * @param toRole 接收方角色（PROCESS 或 SALES）
     * @param toId 接收方ID
     * @param stage 阶段
     * @param status 状态（可传入多个状态）
     */
    public List<DeliveryRecord> getPendingDeliveriesForReceiver(String toRole, String toId, Integer stage, List<Integer> statuses) {
        return deliveryRecordRepository.findByToRoleAndToIdAndStageAndStatusIn(toRole, toId, stage, statuses);
    }

    /**
     * 标记送达记录为已完成
     */
    public DeliveryRecord markAsCompleted(String batchId, Integer stage) {
        Optional<DeliveryRecord> opt = deliveryRecordRepository.findByBatchIdAndStage(batchId, stage);
        if (opt.isPresent()) {
            DeliveryRecord record = opt.get();
            record.setStatus(STATUS_COMPLETED);
            return deliveryRecordRepository.save(record);
        }
        return null;
    }

    /**
     * 检查某接收方是否有权操作某养殖群
     */
    public boolean canReceiverOperate(String batchId, String toRole, String toId, Integer stage) {
        Optional<DeliveryRecord> opt = deliveryRecordRepository.findByBatchIdAndStage(batchId, stage);
        if (!opt.isPresent()) {
            return false;
        }
        DeliveryRecord record = opt.get();
        return record.getToRole().equals(toRole) && record.getToId().equals(toId);
    }

    /**
     * 查询某发送方发出的养殖群列表
     */
    public List<DeliveryRecord> getDeliveriesBySender(String fromRole, String fromId, Integer stage) {
        return deliveryRecordRepository.findAll().stream()
                .filter(r -> stage.equals(r.getStage())
                        && r.getFromRole().equals(fromRole)
                        && r.getFromId().equals(fromId))
                .collect(Collectors.toList());
    }

    /**
     * 删除某养殖群在指定阶段的送达记录
     */
    public void deleteDelivery(String batchId, Integer stage) {
        deliveryRecordRepository.deleteByBatchIdAndStage(batchId, stage);
    }

    /**
     * 清空所有送达记录（管理员批量删除时使用）
     */
    public void deleteAllRecords() {
        deliveryRecordRepository.deleteAll();
    }
}
