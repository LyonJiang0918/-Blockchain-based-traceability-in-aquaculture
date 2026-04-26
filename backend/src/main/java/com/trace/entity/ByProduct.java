package com.trace.entity;

import lombok.Data;

import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 农副产品实体 - 记录养殖群产生的农副产品
 * 例如：鸡产蛋、羊产羊毛/羊奶、牛产牛奶等
 */
@Entity
@Table(name = "by_product")
@Data
public class ByProduct {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 副产品唯一标识
     */
    @Column(unique = true, nullable = false)
    private String productId;

    /**
     * 所属养殖群ID（关联 batch_meta 表）
     */
    @Column(nullable = false)
    private String groupId;

    /**
     * 副产品类型
     * 蛋类: EGG
     * 毛类: WOOL
     * 奶类: MILK
     * 肉类: MEAT
     * 其他: OTHER
     */
    @Column(nullable = false)
    private String productType;

    /**
     * 副产品名称（具体名称如：鸡蛋、羊毛、羊奶等）
     */
    @Column(nullable = false)
    private String productName;

    /**
     * 产量/数量
     */
    @Column(nullable = false)
    private BigDecimal quantity;

    /**
     * 单位（个、公斤、升等）
     */
    @Column(nullable = false)
    private String unit;

    /**
     * 生产日期
     */
    private LocalDateTime productionDate;

    /**
     * 生产批次号
     */
    private String productionBatch;

    /**
     * 质量等级（优/良/合格）
     */
    private String qualityGrade;

    /**
     * 存储方式
     */
    private String storageMethod;

    /**
     * 备注说明
     */
    @Column(columnDefinition = "TEXT")
    private String description;

    /**
     * 数据哈希（用于区块链存证）
     */
    @Column(unique = true)
    private String metaHash;

    /**
     * 状态：0-库存中 1-已销售 2-已使用 3-过期
     */
    @Column(nullable = false)
    private Integer status = 0;

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

    /**
     * 获取副产品类型文本
     */
    public String getProductTypeText() {
        if (productType == null) return "未知";
        switch (productType) {
            case "EGG": return "蛋类";
            case "WOOL": return "毛类";
            case "MILK": return "奶类";
            case "MEAT": return "肉类";
            case "OTHER": return "其他";
            default: return productType;
        }
    }

    /**
     * 获取状态文本
     */
    public String getStatusText() {
        if (status == null) return "未知";
        switch (status) {
            case 0: return "库存中";
            case 1: return "已销售";
            case 2: return "已使用";
            case 3: return "过期";
            default: return "未知";
        }
    }
}
