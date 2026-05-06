package com.example.trace.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TraceAssignBatchCodeResponse {

    private Long batchId;
    private String traceCode;
    private Long spuId;
    private Integer serialNo;
    private String qrPayload;
    private String codeStatus;
    private Integer printCount;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime activatedTime;

    private Long activatedBy;
    private String activatedByUsername;
    private String currentSnapshotId;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}
