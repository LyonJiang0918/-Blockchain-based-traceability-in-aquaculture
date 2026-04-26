package com.trace.dto;

import lombok.Data;
import java.math.BigInteger;

/**
 * 流转记录数据传输对象
 */
@Data
public class TransferRecordDTO {
    private String recordId;
    private String batchId;
    private Integer fromStage;  // 0-养殖，1-加工，2-物流，3-零售
    private Integer toStage;
    private String fromParty;
    private String toParty;
    private BigInteger transferDate;
    private BigInteger quantity;
    private String transportInfo;
    private String metaHash;
    private Long createdAt;

    public String getStageText(Integer stage) {
        if (stage == null) return "未知";
        switch (stage) {
            case 0: return "养殖";
            case 1: return "加工";
            case 2: return "物流";
            case 3: return "零售";
            default: return "未知";
        }
    }
}



