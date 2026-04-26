package com.trace.dto;

import lombok.Data;
import java.math.BigDecimal;

/**
 * 成长记录数据传输对象
 */
@Data
public class GrowthRecordDTO {
    private Long id;
    private String recordId;
    private String groupId;
    private Long recordDate;
    private BigDecimal avgWeight;
    private BigDecimal maxWeight;
    private BigDecimal minWeight;
    private String healthStatus;
    private String healthStatusText;
    private Integer survivalCount;
    private Integer deathCount;
    private Integer cullCount;
    private String growthStage;
    private String growthStageText;
    private String appearanceCondition;
    private Integer vitalityScore;
    private String description;
    private String inspector;
    private String metaHash;
    private Integer status;
    private String statusText;
    private Long createdAt;
}
