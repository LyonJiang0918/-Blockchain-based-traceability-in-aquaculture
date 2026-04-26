package com.trace.dto.request;

import lombok.Data;
import javax.validation.constraints.NotBlank;

/**
 * 创建单体动物请求
 */
@Data
public class CreateAnimalRequest {
    /** 耳标号（系统自动生成，前端传入） */
    @NotBlank(message = "耳标号不能为空")
    private String animalId;

    /** 所属养殖群ID */
    @NotBlank(message = "养殖群ID不能为空")
    private String batchId;

    /** 品种分类：POULTRY/LIVESTOCK/AQUATIC/OTHER */
    private String speciesCategory;

    /** 品种名称 */
    private String species;

    /** 所属养殖场ID */
    private String farmId;

    /** 备注 */
    private String remark;
}
