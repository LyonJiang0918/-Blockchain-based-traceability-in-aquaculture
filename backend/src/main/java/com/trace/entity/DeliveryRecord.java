package com.trace.entity;

import lombok.Data;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * 送达记录实体
 * 管理养殖群在各环节的流转关系：
 * 1. 养殖场出栏时 → 指定送达的加工厂
 * 2. 加工厂完成加工时 → 指定送达的销售商
 * 3. 销售商收货后 → 记录最终状态
 *
 * 通过此表实现"只有被指定的一方才能操作"的权限控制
 */
@Entity
@Table(name = "delivery_record")
@Data
public class DeliveryRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 养殖群ID（FK → batch_meta.batchId）
     */
    @Column(nullable = false)
    private String batchId;

    /**
     * 发送方角色：FARM（养殖场）/ PROCESS（加工厂）
     */
    @Column(nullable = false, length = 20)
    private String fromRole;

    /**
     * 发送方ID（养殖场ID 或 加工厂ID）
     */
    @Column(nullable = false)
    private String fromId;

    /**
     * 接收方角色：PROCESS（加工厂）/ SALES（销售商）
     */
    @Column(nullable = false, length = 20)
    private String toRole;

    /**
     * 接收方ID（加工厂ID 或 销售商ID）
     */
    @Column(nullable = false)
    private String toId;

    /**
     * 流转阶段：
     * 1 = 出栏待加工（养殖场→加工厂）
     * 2 = 待收货（加工厂→销售商）
     */
    @Column(nullable = false)
    private Integer stage;

    /**
     * 状态：
     * 0 = 待处理
     * 1 = 已接受/已完成
     * 2 = 已拒绝
     */
    @Column(nullable = false)
    private Integer status = 0;

    /**
     * 批次在送达时的数量
     */
    private Integer quantity;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (status == null) {
            status = 0;
        }
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
