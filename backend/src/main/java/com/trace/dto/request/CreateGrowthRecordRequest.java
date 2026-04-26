package com.trace.dto.request;

import lombok.Data;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

/**
 * 创建成长记录请求
 */
@Data
public class CreateGrowthRecordRequest {

    @NotBlank(message = "记录ID不能为空")
    private String recordId;

    @NotBlank(message = "养殖群ID不能为空")
    private String groupId;

    @NotNull(message = "记录日期不能为空")
    private Long recordDate;

    /** 体重相关 */
    private BigDecimal avgWeight;
    private BigDecimal maxWeight;
    private BigDecimal minWeight;

    /** 健康状态 */
    private String healthStatus;  // HEALTHY / NORMAL / SICK / WEAK

    /** 数量统计 */
    private Integer survivalCount;
    private Integer deathCount;
    private Integer cullCount;

    /** 发育阶段 */
    private String growthStage;  // CHICK / GROWING / FATTENING / LAYING

    /** 其他 */
    private String appearanceCondition;
    private Integer vitalityScore;
    private String description;
    private String inspector;
}
