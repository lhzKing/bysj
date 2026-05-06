package com.example.trace.dto;

import com.example.trace.enums.ActionType;
import com.example.trace.enums.TraceStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Response for scan-time executable action recommendation.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TraceAvailableActionsResponse {

    private String traceCode;
    private TraceStatus currentStatus;
    private String currentStatusLabel;
    private String currentNode;
    private ActionType recommendedAction;

    @Builder.Default
    private List<AvailableAction> availableActions = new ArrayList<>();

    /**
     * Filled only when no action can be executed by the current user in the current state.
     */
    private String noActionReason;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AvailableAction {
        private ActionType actionType;
        private String label;
        private boolean requiresRemark;
        private TraceStatus nextStatus;
        private String nextStatusLabel;
        private String permissionHint;
    }
}
