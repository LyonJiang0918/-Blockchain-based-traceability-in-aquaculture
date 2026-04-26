package com.trace.dto.request;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CreateInspectionRecordRequest {
    private String recordId;
    private String batchId;
    private String inspectorId;
    private String inspectorName;
    private LocalDateTime inspectDate;
    private Integer result;
    private String reportHash;
}
