package com.trace.entity;

import lombok.Data;

import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 饲料投喂记录实体
 */
@Entity
@Table(name = "feed_record")
@Data
public class FeedRecord {
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
     * 饲料批次号
     */
    private String feedBatchId;

    /**
     * 饲料类型
     * CORN - 玉米
     * SOYBEAN - 豆粕
     * WHEAT - 小麦
     * FORMULA - 配合饲料
     * GREEN - 青绿饲料
     * OTHER - 其他
     */
    @Column(nullable = false)
    private String feedType;

    /**
     * 饲料品牌/名称
     */
    private String feedBrand;

    /**
     * 投喂日期
     */
    private LocalDateTime feedDate;

    /**
     * 投喂量（公斤）
     */
    @Column(precision = 10, scale = 2)
    private BigDecimal amount;

    /**
     * 单位成本（元/公斤）
     */
    @Column(precision = 10, scale = 2)
    private BigDecimal unitCost;

    /**
     * 总成本（元）
     */
    @Column(precision = 10, scale = 2)
    private BigDecimal totalCost;

    /**
     * 投喂方式
     * MANUAL - 人工投喂
     * AUTOMATIC - 自动投喂
     * FREE_RANGE - 放养觅食
     */
    private String feedingMethod;

    /**
     * 操作人员
     */
    private String operator;

    /**
     * 备注说明
     */
    @Column(columnDefinition = "TEXT")
    private String description;

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

    public String getFeedTypeText() {
        if (feedType == null) return "未知";
        switch (feedType) {
            case "CORN": return "玉米";
            case "SOYBEAN": return "豆粕";
            case "WHEAT": return "小麦";
            case "FORMULA": return "配合饲料";
            case "GREEN": return "青绿饲料";
            case "OTHER": return "其他";
            default: return feedType;
        }
    }

    public String getFeedingMethodText() {
        if (feedingMethod == null) return "未知";
        switch (feedingMethod) {
            case "MANUAL": return "人工投喂";
            case "AUTOMATIC": return "自动投喂";
            case "FREE_RANGE": return "放养觅食";
            default: return feedingMethod;
        }
    }

    public String getStatusText() {
        return status == 1 ? "已作废" : "正常";
    }
}
