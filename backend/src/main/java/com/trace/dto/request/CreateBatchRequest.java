package com.trace.dto.request;

import lombok.Data;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.math.BigInteger;

/**
 * 创建批次请求
 */
@Data
public class CreateBatchRequest {
    @NotBlank(message = "养殖群ID不能为空")
    private String batchId;

    @NotBlank(message = "养殖场ID不能为空")
    private String farmId;

    @NotBlank(message = "品种不能为空")
    private String species;

    /**
     * 品种分类（禽类/牲畜/水产）
     * POULTRY - 禽类
     * LIVESTOCK - 牲畜
     * AQUATIC - 水产
     */
    private String speciesCategory;

    @NotNull(message = "数量不能为空")
    private BigInteger quantity;

    @NotBlank(message = "地理位置不能为空")
    private String location;

    private String metaJson;  // 链下详细数据（JSON格式）
}



