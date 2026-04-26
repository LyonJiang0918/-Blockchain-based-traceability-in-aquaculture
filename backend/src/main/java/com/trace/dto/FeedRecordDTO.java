package com.trace.dto;

import lombok.Data;
import java.math.BigDecimal;

/**
 * 饲料投喂记录数据传输对象
 */
@Data
public class FeedRecordDTO {
    private Long id;
    private String recordId;
    private String groupId;
    private String feedBatchId;
    private String feedType;
    private String feedTypeText;
    private String feedBrand;
    private Long feedDate;
    private BigDecimal amount;
    private BigDecimal unitCost;
    private BigDecimal totalCost;
    private String feedingMethod;
    private String feedingMethodText;
    private String operator;
    private String description;
    private String metaHash;
    private Integer status;
    private String statusText;
    private Long createdAt;
}
