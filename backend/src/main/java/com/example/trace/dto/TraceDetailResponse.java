package com.example.trace.dto;

import com.example.trace.entity.TraceLifecycleLog;
import com.example.trace.entity.TraceSnapshot;
import lombok.Data;

import java.util.List;

@Data
public class TraceDetailResponse {
    private TraceSnapshot snapshot;
    private List<TraceLifecycleLog> history;
    private String view;
    private List<TraceAggregationHistoryResponse> aggregationHistory;

    public TraceDetailResponse(TraceSnapshot snapshot, List<TraceLifecycleLog> history) {
        this(snapshot, history, "effective");
    }

    public TraceDetailResponse(TraceSnapshot snapshot, List<TraceLifecycleLog> history, String view) {
        this(snapshot, history, view, List.of());
    }

    public TraceDetailResponse(
            TraceSnapshot snapshot,
            List<TraceLifecycleLog> history,
            String view,
            List<TraceAggregationHistoryResponse> aggregationHistory
    ) {
        this.snapshot = snapshot;
        this.history = history;
        this.view = view;
        this.aggregationHistory = aggregationHistory == null ? List.of() : aggregationHistory;
    }
}
