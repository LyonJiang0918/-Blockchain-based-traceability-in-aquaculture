package com.trace.entity;

import lombok.Data;

import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 成长记录实体 - 记录养殖群的体重、健康状态等成长信息
 */
@Entity
@Table(name = "growth_record")
@Data
public class GrowthRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 记录唯一标识
     */
    @Column(unique = true, nullable = false)
    private String recordId;

    /**
     * 所属养殖群ID
     */
    @Column(nullable = false)
    private String groupId;

    /**
     * 记录日期
     */
    private LocalDateTime recordDate;

    /**
     * 平均体重（公斤）
     */
    @Column(precision = 10, scale = 2)
    private BigDecimal avgWeight;

    /**
     * 最大体重（公斤）
     */
    @Column(precision = 10, scale = 2)
    private BigDecimal maxWeight;

    /**
     * 最小体重（公斤）
     */
    @Column(precision = 10, scale = 2)
    private BigDecimal minWeight;

    /**
     * 健康状态
     * HEALTHY - 健康
     * NORMAL - 正常
     * SICK - 患病
     * WEAK - 弱雏
     */
    private String healthStatus;

    /**
     * 存活数量
     */
    private Integer survivalCount;

    /**
     * 死淘数量
     */
    private Integer deathCount;

    /**
     * 淘汰数量
     */
    private Integer cullCount;

    /**
     * 发育阶段
     * CHICK - 育雏期
     * GROWING - 生长期
     * FATTENING - 育肥期
     * LAYING - 产蛋期
     */
    private String growthStage;

    /**
     * 羽毛/皮毛状态
     */
    private String appearanceCondition;

    /**
     * 活力评分（1-10）
     */
    private Integer vitalityScore;

    /**
     * 备注说明
     */
    @Column(columnDefinition = "TEXT")
    private String description;

    /**
     * 检测员
     */
    private String inspector;

    /**
     * 数据哈希
     */
    @Column(unique = true)
    private String metaHash;

    /**
     * 记录状态：0正常 1已作废
     */
    @Column(nullable = false)
    private Integer status = 0;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (status == null) status = 0;
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public String getHealthStatusText() {
        if (healthStatus == null) return "未知";
        switch (healthStatus) {
            case "HEALTHY": return "健康";
            case "NORMAL": return "正常";
            case "SICK": return "患病";
            case "WEAK": return "弱雏";
            default: return healthStatus;
        }
    }

    public String getGrowthStageText() {
        if (growthStage == null) return "未知";
        switch (growthStage) {
            case "CHICK": return "育雏期";
            case "GROWING": return "生长期";
            case "FATTENING": return "育肥期";
            case "LAYING": return "产蛋期";
            default: return growthStage;
        }
    }

    public String getStatusText() {
        return status == 1 ? "已作废" : "正常";
    }
}
