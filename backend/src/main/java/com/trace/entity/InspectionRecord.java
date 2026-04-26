package com.trace.entity;

import lombok.Data;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * 检验记录实体
 */
@Entity
@Table(name = "inspection_record")
@Data
public class InspectionRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String recordId;

    @Column(nullable = false)
    private String batchId;

    private String inspectorId;

    private String inspectorName;

    private LocalDateTime inspectDate;

    /** 0-合格，1-不合格 */
    private Integer result;

    @Column(columnDefinition = "TEXT")
    private String reportHash;

    @Column(unique = true)
    private String metaHash;

    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        createdAt = LocalDateTime.now();
        if (inspectDate == null) {
            inspectDate = LocalDateTime.now();
        }
    }
}
