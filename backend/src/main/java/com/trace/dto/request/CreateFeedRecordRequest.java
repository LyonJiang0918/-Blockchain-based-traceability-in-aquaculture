package com.trace.dto.request;

import lombok.Data;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

/**
 * 创建饲料投喂记录请求
 */
@Data
public class CreateFeedRecordRequest {

    @NotBlank(message = "记录ID不能为空")
    private String recordId;

    @NotBlank(message = "养殖群ID不能为空")
    private String groupId;

    @NotBlank(message = "饲料类型不能为空")
    private String feedType;  // CORN / SOYBEAN / WHEAT / FORMULA / GREEN / OTHER

    private String feedBatchId;
    private String feedBrand;

    @NotNull(message = "投喂日期不能为空")
    private Long feedDate;

    @NotNull(message = "投喂量不能为空")
    private BigDecimal amount;

    private BigDecimal unitCost;
    private BigDecimal totalCost;

    private String feedingMethod;  // MANUAL / AUTOMATIC / FREE_RANGE

    private String operator;
    private String description;
}
