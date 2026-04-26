package com.trace.dto.request;

import lombok.Data;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

/**
 * 创建农副产品请求
 */
@Data
public class CreateByProductRequest {
    @NotBlank(message = "副产品ID不能为空")
    private String productId;

    @NotBlank(message = "所属养殖群ID不能为空")
    private String groupId;

    @NotBlank(message = "副产品类型不能为空")
    private String productType;

    @NotBlank(message = "副产品名称不能为空")
    private String productName;

    @NotNull(message = "数量不能为空")
    private BigDecimal quantity;

    @NotBlank(message = "单位不能为空")
    private String unit;

    private Long productionDate;

    private String productionBatch;

    private String qualityGrade;

    private String storageMethod;

    private String description;
}
