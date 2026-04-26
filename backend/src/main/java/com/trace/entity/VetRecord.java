package com.trace.entity;

import lombok.Data;

import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 兽医/疫苗记录实体
 */
@Entity
@Table(name = "vet_record")
@Data
public class VetRecord {
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
     * 记录类型：0-免疫（疫苗） 1-用药 2-治疗
     */
    @Column(nullable = false)
    private Integer recordType;

    /**
     * 疫苗/药品编号
     */
    private String medicineId;

    /**
     * 疫苗/药品名称
     */
    @Column(nullable = false)
    private String medicineName;

    /**
     * 疫苗类型（免疫专用）
     * NEWCASTLE - 新城疫
     * BIRD_FLU - 禽流感
     * BURSAL - 法氏囊
     * RABIES - 狂犬病（羊）
     * FOOT_MOUTH - 口蹄疫（牛羊）
     * BRUCELLOSIS - 布病（羊）
     * OTHER - 其他
     */
    private String vaccineType;

    /**
     * 生产厂家
     */
    private String manufacturer;

    /**
     * 批号
     */
    private String batchNumber;

    /**
     * 有效期
     */
    private LocalDateTime expiryDate;

    /**
     * 操作日期
     */
    private LocalDateTime operationDate;

    /**
     * 用量/剂量
     */
    @Column(precision = 10, scale = 2)
    private BigDecimal dosage;

    /**
     * 剂量单位
     */
    private String dosageUnit;

    /**
     * 用药途径
     * INJECTION - 注射
     * ORAL - 口服
     * DRINKING_WATER - 饮水
     * SPRAY - 喷雾
     */
    private String administrationRoute;

    /**
     * 兽医姓名
     */
    private String vetName;

    /**
     * 兽医执照号
     */
    private String vetLicense;

    /**
     * 执业机构
     */
    private String vetInstitution;

    /**
     * 免疫/治疗对象
     */
    private String targetAnimals;

    /**
     * 病症描述
     */
    @Column(columnDefinition = "TEXT")
    private String diagnosis;

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

    public String getRecordTypeText() {
        if (recordType == null) return "未知";
        switch (recordType) {
            case 0: return "免疫";
            case 1: return "用药";
            case 2: return "治疗";
            default: return "未知";
        }
    }

    public String getVaccineTypeText() {
        if (vaccineType == null) return "";
        switch (vaccineType) {
            case "NEWCASTLE": return "新城疫";
            case "BIRD_FLU": return "禽流感";
            case "BURSAL": return "法氏囊";
            case "RABIES": return "狂犬病";
            case "FOOT_MOUTH": return "口蹄疫";
            case "BRUCELLOSIS": return "布病";
            case "OTHER": return "其他";
            default: return vaccineType;
        }
    }

    public String getStatusText() {
        return status == 1 ? "已作废" : "正常";
    }
}
