package com.trace.dto;

import lombok.Data;
import java.math.BigDecimal;

/**
 * 兽医记录数据传输对象
 */
@Data
public class VetRecordDTO {
    private Long id;
    private String recordId;
    private String groupId;
    private Integer recordType;
    private String recordTypeText;
    private String medicineId;
    private String medicineName;
    private String vaccineType;
    private String vaccineTypeText;
    private String manufacturer;
    private String batchNumber;
    private Long expiryDate;
    private Long operationDate;
    private BigDecimal dosage;
    private String dosageUnit;
    private String administrationRoute;
    private String vetName;
    private String vetLicense;
    private String vetInstitution;
    private String targetAnimals;
    private String diagnosis;
    private String description;
    private String metaHash;
    private Integer status;
    private String statusText;
    private Long createdAt;
}
