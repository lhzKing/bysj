package com.example.trace.dto;

import com.example.trace.enums.TraceAggregationRelationType;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class TraceAggregationHistoryResponse {

    private Long relationId;
    private String parentCode;
    private String childCode;
    private TraceAggregationRelationType relationType;
    private String relationTypeLabel;
    private Boolean active;
    private Boolean direct;
    private Integer level;
    private String viaCode;
    private String remark;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime bindTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime releaseTime;
}
