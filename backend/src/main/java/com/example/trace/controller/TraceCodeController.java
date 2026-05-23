package com.example.trace.controller;

import com.example.trace.annotation.RequirePermission;
import com.example.trace.common.ApiResponse;
import com.example.trace.dto.TraceAssignBatchCodeResponse;
import com.example.trace.dto.TraceCodeActivateRequest;
import com.example.trace.dto.TraceCodeActivateResponse;
import com.example.trace.service.TraceService;
import com.example.trace.service.impl.support.TraceAssignBatchCodeQueryService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/trace-codes")
public class TraceCodeController {

    private static final String ATTR_USERNAME = "username";

    private final TraceService traceService;
    private final TraceAssignBatchCodeQueryService traceAssignBatchCodeQueryService;

    public TraceCodeController(
            TraceService traceService,
            TraceAssignBatchCodeQueryService traceAssignBatchCodeQueryService
    ) {
        this.traceService = traceService;
        this.traceAssignBatchCodeQueryService = traceAssignBatchCodeQueryService;
    }

    /**
     * 按追溯码反查单品码详情（QR 双入口的工作台一侧）。
     * GET /api/trace-codes/{traceCode}
     */
    @GetMapping("/{traceCode}")
    @RequirePermission("trace:view")
    public ResponseEntity<ApiResponse<TraceAssignBatchCodeResponse>> getByTraceCode(
            @PathVariable String traceCode
    ) {
        TraceAssignBatchCodeResponse response = traceAssignBatchCodeQueryService.findByTraceCode(traceCode);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 单品码扫码激活/复核。
     * POST /api/trace-codes/{traceCode}/activate
     */
    @PostMapping("/{traceCode}/activate")
    @RequirePermission({"trace:code:activate", "trace:create"})
    public ResponseEntity<ApiResponse<TraceCodeActivateResponse>> activateCode(
            @PathVariable String traceCode,
            @RequestBody(required = false) @Valid TraceCodeActivateRequest request,
            HttpServletRequest httpReq
    ) {
        TraceCodeActivateResponse response = traceService.activateCode(traceCode, request, operator(httpReq));
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.created(response, "单品码激活成功"));
    }

    private String operator(HttpServletRequest request) {
        Object v = request.getAttribute(ATTR_USERNAME);
        return v == null ? "unknown" : v.toString();
    }
}
