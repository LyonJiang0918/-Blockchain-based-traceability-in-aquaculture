package com.trace.dto.request;

import lombok.Data;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * 创建加工记录请求
 */
@Data
public class CreateProcessRecordRequest {
    /** 加工记录ID（系统自动生成，前端传入） */
    private String recordId;

    /** 来源养殖群ID */
    @NotBlank(message = "养殖群ID不能为空")
    private String batchId;

    /** 加工类型：SLAUGHTER-屠宰分割 PACKAGING-包装 PROCESSING-深加工 */
    @NotBlank(message = "加工类型不能为空")
    private String processType;

    /**
     * 输入动物ID列表（JSON数组字符串）
     * 格式：["ANIMAL20260405...","ANIMAL20260405..."]
     */
    private String inputAnimalIds;

    /**
     * 产出副产品ID列表（JSON数组字符串）
     * 格式：["EGG20260405001","MEAT20260405001"]
     */
    private String outputProductIds;

    /** 输入动物数量 */
    @NotNull(message = "输入动物数量不能为空")
    private Integer inputCount;

    /** 产出产品数量 */
    private Integer outputCount;

    /** 操作员 */
    private String operator;

    /** 备注 */
    private String remark;
}
