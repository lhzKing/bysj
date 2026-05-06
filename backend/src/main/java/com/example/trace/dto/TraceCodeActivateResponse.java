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
public class TraceCodeActivateResponse {

    private String traceCode;
    private ActionType actionType;
    private String codeStatus;
    private String activationNode;
    private String deviceId;
    private String activatedByUsername;
    private String activatedTime;
    private Long lifecycleLogId;
    private String remark;
}
