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

    public TraceDetailResponse(TraceSnapshot snapshot, List<TraceLifecycleLog> history) {
        this(snapshot, history, "effective");
    }

    public TraceDetailResponse(TraceSnapshot snapshot, List<TraceLifecycleLog> history, String view) {
        this.snapshot = snapshot;
        this.history = history;
        this.view = view;
    }
}
