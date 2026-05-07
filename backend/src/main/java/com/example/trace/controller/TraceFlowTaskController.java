package com.example.trace.controller;

import com.example.trace.annotation.RequirePermission;
import com.example.trace.common.ApiResponse;
import com.example.trace.dto.TraceFlowTaskCompleteRequest;
import com.example.trace.dto.TraceFlowTaskCreateRequest;
import com.example.trace.dto.TraceFlowTaskResponse;
import com.example.trace.dto.TraceFlowTaskScanRequest;
import com.example.trace.enums.TraceFlowTaskStatus;
import com.example.trace.enums.TraceFlowTaskType;
import com.example.trace.service.TraceFlowTaskService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/trace-flow-tasks")
@RequiredArgsConstructor
public class TraceFlowTaskController {

    private static final String ATTR_USER_ID = "userId";
    private static final String ATTR_USERNAME = "username";

    private final TraceFlowTaskService traceFlowTaskService;

    @GetMapping
    @RequirePermission("trace:view")
    public ApiResponse<List<TraceFlowTaskResponse>> listTasks(
            @RequestParam(name = "task_type", required = false) TraceFlowTaskType taskType,
            @RequestParam(required = false) TraceFlowTaskStatus status
    ) {
        return ApiResponse.success(traceFlowTaskService.listTasks(taskType, status));
    }

    @GetMapping("/{id}")
    @RequirePermission("trace:view")
    public ApiResponse<TraceFlowTaskResponse> getTask(@PathVariable Long id) {
        return ApiResponse.success(traceFlowTaskService.getTaskById(id));
    }

    @GetMapping("/no/{taskNo}")
    @RequirePermission("trace:view")
    public ApiResponse<TraceFlowTaskResponse> getTaskByNo(@PathVariable String taskNo) {
        return ApiResponse.success(traceFlowTaskService.getTaskByNo(taskNo));
    }

    @PostMapping
    @RequirePermission({"trace:task:create", "trace:create", "trace:scan", "trace:inbound", "trace:outbound", "trace:transfer"})
    public ResponseEntity<ApiResponse<TraceFlowTaskResponse>> createTask(
            @Valid @RequestBody TraceFlowTaskCreateRequest request,
            HttpServletRequest httpRequest
    ) {
        TraceFlowTaskResponse response = traceFlowTaskService.createTask(
                request,
                (Long) httpRequest.getAttribute(ATTR_USER_ID),
                operator(httpRequest)
        );
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.created(response, "流转任务创建成功"));
    }

    @PostMapping("/{id}/cancel")
    @RequirePermission({"trace:task:create", "trace:task:complete", "trace:create", "trace:scan", "trace:inbound", "trace:outbound", "trace:transfer"})
    public ApiResponse<TraceFlowTaskResponse> cancelTask(@PathVariable Long id) {
        return ApiResponse.success(traceFlowTaskService.cancelTask(id));
    }

    @PostMapping("/{id}/scan")
    @RequirePermission({"trace:task:scan", "trace:scan", "trace:outbound", "trace:inbound", "trace:transfer"})
    public ApiResponse<TraceFlowTaskResponse> scanTask(
            @PathVariable Long id,
            @Valid @RequestBody TraceFlowTaskScanRequest request,
            HttpServletRequest httpRequest
    ) {
        return ApiResponse.success(traceFlowTaskService.scanTask(
                id,
                request,
                (Long) httpRequest.getAttribute(ATTR_USER_ID),
                operator(httpRequest)
        ));
    }

    @PostMapping("/{id}/complete")
    @RequirePermission({"trace:task:complete", "trace:create", "trace:scan", "trace:inbound", "trace:outbound", "trace:transfer"})
    public ApiResponse<TraceFlowTaskResponse> completeTask(
            @PathVariable Long id,
            @RequestBody(required = false) @Valid TraceFlowTaskCompleteRequest request
    ) {
        return ApiResponse.success(traceFlowTaskService.completeTask(id, request));
    }

    private String operator(HttpServletRequest request) {
        Object value = request.getAttribute(ATTR_USERNAME);
        return value == null ? "unknown" : value.toString();
    }
}
