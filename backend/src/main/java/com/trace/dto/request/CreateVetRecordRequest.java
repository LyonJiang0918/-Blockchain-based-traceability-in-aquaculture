package com.trace.dto.request;

import lombok.Data;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

/**
 * 创建兽医/疫苗记录请求
 */
@Data
public class CreateVetRecordRequest {

    @NotBlank(message = "记录ID不能为空")
    private String recordId;

    @NotBlank(message = "养殖群ID不能为空")
    private String groupId;

    @NotNull(message = "记录类型不能为空")
    private Integer recordType;  // 0-免疫 1-用药 2-治疗

    @NotBlank(message = "药品名称不能为空")
    private String medicineName;

    private String medicineId;

    /** 疫苗专用字段 */
    private String vaccineType;  // NEWCASTLE / BIRD_FLU / BURSAL 等
    private String manufacturer;  // 生产厂家
    private String batchNumber;  // 批号
    private Long expiryDate;  // 有效期

    @NotNull(message = "操作日期不能为空")
    private Long operationDate;

    private BigDecimal dosage;  // 用量
    private String dosageUnit;  // 剂量单位
    private String administrationRoute;  // 用药途径

    private String vetName;  // 兽医姓名
    private String vetLicense;  // 执照号
    private String vetInstitution;  // 执业机构
    private String targetAnimals;  // 免疫/治疗对象

    /** 治疗专用 */
    private String diagnosis;  // 病症描述

    private String description;  // 备注
}
