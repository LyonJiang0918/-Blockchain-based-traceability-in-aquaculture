package com.trace.dto.request;

import lombok.Data;

import java.math.BigDecimal;
import java.math.BigInteger;

@Data
public class CreateTransferRecordRequest {
    private String recordId;
    private String batchId;
    private Integer fromStage;
    private Integer toStage;
    private String fromParty;
    private String toParty;
    private BigInteger transferDate;
    private BigDecimal quantity;
    private String transportInfo;
}
