package com.trace.entity;

import lombok.Data;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * 养殖群链下详细数据实体
 * 对应一个养殖单元：一窝鸡、一圈牛羊、一片蜂箱等
 */
@Entity
@Table(name = "batch_meta")
@Data
public class BatchMeta {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 养殖群ID（原 batchId）
     * 格式示例：GROUP20260403001（一窝白羽鸡）
     */
    @Column(unique = true, nullable = false)
    private String batchId;

    @Column(columnDefinition = "TEXT")
    private String metaJson;  // JSON格式的详细数据

    private String fileUrl;   // 附件文件URL

    @Column(unique = true)
    private String metaHash;  // 数据哈希值

    /** 养殖群状态：0=在养 1=出栏 2=加工中 3=已加工 4=已销售 */
    @Column(nullable = false)
    private Integer status = 0;

    /** 是否已作废（区块链特性：数据不可删除，只能作废标记） */
    @Column(nullable = false)
    private Boolean invalidated = false;

    /** 作废时间 */
    private LocalDateTime invalidatedAt;

    /** 作废操作人 */
    private String invalidatedBy;

    /** 作废原因 */
    private String invalidateReason;

    /** 该群登记的动物总数（INTEGER，默认0） */
    private Integer animalCount = 0;

    /** 已出栏动物数 */
    private Integer slaughteredCount = 0;

    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        createdAt = LocalDateTime.now();
        if (status == null) {
            status = 0;
        }
        if (invalidated == null) {
            invalidated = false;
        }
    }
}



