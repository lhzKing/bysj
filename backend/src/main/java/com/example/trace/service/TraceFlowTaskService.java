package com.example.trace.service;

import com.example.trace.dto.TraceFlowTaskCandidateResponse;
import com.example.trace.dto.TraceFlowTaskCompleteRequest;
import com.example.trace.dto.TraceFlowTaskCreateRequest;
import com.example.trace.dto.TraceFlowTaskResponse;
import com.example.trace.dto.TraceFlowTaskScanRequest;
import com.example.trace.enums.TraceFlowTaskStatus;
import com.example.trace.enums.TraceFlowTaskType;

import java.util.List;

public interface TraceFlowTaskService {

    List<TraceFlowTaskResponse> listTasks(TraceFlowTaskType taskType, TraceFlowTaskStatus status);

    /**
     * 给定追溯码，返回它当前可参与扫码的开放运单候选列表（CREATED/PROCESSING）。
     * 仅保留 task_type + snapshot.currentStatus 能派生出有效 ScanPlan 的任务，
     * 用于普通扫码弹窗（ScanFlowDialog）的运单下拉选择 + 字段自动填充。
     */
    List<TraceFlowTaskCandidateResponse> findCandidateFlowTasksForTrace(String traceCode);

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
