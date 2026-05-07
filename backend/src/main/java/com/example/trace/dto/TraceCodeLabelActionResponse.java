package com.example.trace.dto;

import com.example.trace.enums.ActionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TraceCodeLabelActionResponse {

    private String traceCode;
    private ActionType actionType;
    private String codeStatus;
    private Integer printCount;
    private Long lifecycleLogId;
    private String eventTime;
    private String remark;
    private String currentStatus;
}
