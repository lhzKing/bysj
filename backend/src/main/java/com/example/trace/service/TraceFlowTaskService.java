package com.example.trace.service;

import com.example.trace.dto.TraceFlowTaskCompleteRequest;
import com.example.trace.dto.TraceFlowTaskCreateRequest;
import com.example.trace.dto.TraceFlowTaskResponse;
import com.example.trace.dto.TraceFlowTaskScanRequest;
import com.example.trace.enums.TraceFlowTaskStatus;
import com.example.trace.enums.TraceFlowTaskType;

import java.util.List;

public interface TraceFlowTaskService {

    List<TraceFlowTaskResponse> listTasks(TraceFlowTaskType taskType, TraceFlowTaskStatus status);

    TraceFlowTaskResponse getTaskById(Long id);

    TraceFlowTaskResponse getTaskByNo(String taskNo);

    TraceFlowTaskResponse createTask(
            TraceFlowTaskCreateRequest request,
            Long operatorUserId,
            String operatorUsername
    );

    TraceFlowTaskResponse cancelTask(Long id);

    TraceFlowTaskResponse completeTask(Long id, TraceFlowTaskCompleteRequest request);

    TraceFlowTaskResponse scanTask(
            Long id,
            TraceFlowTaskScanRequest request,
            Long operatorUserId,
            String operatorUsername
    );
}
