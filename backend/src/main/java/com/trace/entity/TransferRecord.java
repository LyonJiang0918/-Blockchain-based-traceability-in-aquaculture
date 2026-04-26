package com.trace.entity;

import lombok.Data;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * 流转记录实体
 */
@Entity
@Table(name = "transfer_record")
@Data
public class TransferRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String recordId;

    @Column(nullable = false)
    private String batchId;

    private Integer fromStage;

    private Integer toStage;

    private String fromParty;

    private String toParty;

    private LocalDateTime transferDate;

    private java.math.BigDecimal quantity;

    @Column(columnDefinition = "TEXT")
    private String transportInfo;

    @Column(unique = true)
    private String metaHash;

    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        createdAt = LocalDateTime.now();
        if (transferDate == null) {
            transferDate = LocalDateTime.now();
        }
    }
}
