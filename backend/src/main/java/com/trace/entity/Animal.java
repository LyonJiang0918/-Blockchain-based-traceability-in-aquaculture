package com.trace.entity;

import lombok.Data;
import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * 单体动物实体
 * 最小追溯单位：每只动物有唯一耳标号（系统自动生成）
 * 入栏时从养殖群批次拆分登记
 */
@Entity
@Table(name = "animal")
@Data
public class Animal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 耳标号（系统自动生成：ANIMAL + 年月日时分秒 + 6位随机码）
     * 格式示例：ANIMAL2026040514300057a3f2b
     */
    @Column(unique = true, nullable = false)
    private String animalId;

    /** 所属养殖群ID（FK → batch_meta.batch_id） */
    @Column(nullable = false)
    private String batchId;

    /** 品种分类：POULTRY/LIVESTOCK/AQUATIC/OTHER */
    private String speciesCategory;

    /** 品种名称 */
    private String species;

    /** 所属养殖场ID */
    private String farmId;

    /**
     * 单体状态：0=在养 1=出栏 2=加工中 3=已加工 4=已销售
     */
    @Column(nullable = false)
    private Integer status = 0;

    /** 入栏/出生时间 */
    private LocalDateTime birthTime;

    /** 单体数据哈希（防篡改） */
    @Column(unique = true)
    private String individualHash;

    /** 备注 */
    private String remark;

    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        createdAt = LocalDateTime.now();
        if (birthTime == null) {
            birthTime = LocalDateTime.now();
        }
        if (status == null) {
            status = 0;
        }
    }
}
