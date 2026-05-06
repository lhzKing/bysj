package com.example.trace.service.impl.support;

import com.example.trace.dto.ScanTraceRequest;

@FunctionalInterface
public interface TraceScanExecutor {
    void execute(ScanTraceRequest request, String operator);

    default boolean executeAndReturnCreated(ScanTraceRequest request, String operator) {
        execute(request, operator);
        return true;
    }
}
