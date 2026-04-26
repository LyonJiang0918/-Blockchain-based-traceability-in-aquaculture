package com.trace.entity;

import lombok.Data;
import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * 加工记录实体
 * 记录加工批次输入的动物和产出的副产品
 */
@Entity
@Table(name = "process_record")
@Data
public class ProcessRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 加工记录ID（系统生成） */
    @Column(unique = true, nullable = false)
    private String recordId;

    /** 来源养殖群ID */
    @Column(nullable = false)
    private String batchId;

    /** 加工厂ID */
    private String processFactoryId;

    /** 加工类型：SLAUGHTER-屠宰分割 PACKAGING-包装 PROCESSING-深加工 */
    @Column(nullable = false)
    private String processType;

    /**
     * 输入动物ID列表（JSON数组）
     * 格式：["ANIMAL20260405...","ANIMAL20260405..."]
     */
    @Column(columnDefinition = "TEXT")
    private String inputAnimalIds;

    /**
     * 产出副产品ID列表（JSON数组）
     * 格式：["EGG20260405001","MEAT20260405001"]
     */
    @Column(columnDefinition = "TEXT")
    private String outputProductIds;

    /** 输入动物数量 */
    private Integer inputCount;

    /** 产出产品数量 */
    private Integer outputCount;

    /** 操作员 */
    private String operator;

    /** 加工开始时间 */
    private LocalDateTime processStartTime;

    /** 加工完成时间 */
    private LocalDateTime processEndTime;

    /**
     * 状态：0=加工中 1=已完成
     */
    @Column(nullable = false)
    private Integer status = 0;

    /** 数据哈希 */
    @Column(unique = true)
    private String metaHash;

    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        createdAt = LocalDateTime.now();
        if (processStartTime == null) {
            processStartTime = LocalDateTime.now();
        }
        if (status == null) {
            status = 0;
        }
    }
}
