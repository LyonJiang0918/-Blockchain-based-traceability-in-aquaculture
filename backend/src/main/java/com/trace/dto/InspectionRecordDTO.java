package com.trace.dto;

import lombok.Data;
import java.math.BigInteger;

/**
 * 检验记录数据传输对象
 */
@Data
public class InspectionRecordDTO {
    private String recordId;
    private String batchId;
    private String inspectorId;
    private String inspectorName;
    private BigInteger inspectDate;
    private Integer result;  // 0-合格，1-不合格
    private String reportHash;
    private String metaHash;
    private Long createdAt;

    public String getResultText() {
        if (result == null) return "未知";
        return result == 0 ? "合格" : "不合格";
    }
}



