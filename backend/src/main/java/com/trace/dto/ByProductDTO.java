package com.trace.dto;

import lombok.Data;
import java.math.BigDecimal;

/**
 * 农副产品记录数据传输对象
 */
@Data
public class ByProductDTO {
    private Long id;
    private String productId;
    private String groupId;
    private String productType;
    private String productTypeText;
    private String productName;
    private BigDecimal quantity;
    private String unit;
    private Long productionDate;
    private String productionBatch;
    private String qualityGrade;
    private String storageMethod;
    private String description;
    private String metaHash;
    private Integer status;
    private String statusText;
    private Long createdAt;
}
