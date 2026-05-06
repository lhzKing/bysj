package com.example.trace.controller;

import com.example.trace.annotation.RequirePermission;
import com.example.trace.common.ApiResponse;
import com.example.trace.dto.TraceAssignBatchCodeResponse;
import com.example.trace.dto.TraceAssignBatchReconciliationResponse;
import com.example.trace.service.impl.support.TraceAssignBatchCodeQueryService;
import com.example.trace.service.impl.support.TraceAssignBatchReconciliationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/trace-batches")
public class TraceAssignBatchController {

    private final TraceAssignBatchReconciliationService reconciliationService;
    private final TraceAssignBatchCodeQueryService codeQueryService;

    public TraceAssignBatchController(
            TraceAssignBatchReconciliationService reconciliationService,
            TraceAssignBatchCodeQueryService codeQueryService
    ) {
        this.reconciliationService = reconciliationService;
        this.codeQueryService = codeQueryService;
    }

    /**
     * Assignment-batch detail with production quantity reconciliation.
     * GET /api/trace-batches/{batchId}
     */
    @GetMapping("/{batchId}")
    @RequirePermission("trace:view")
    public ResponseEntity<ApiResponse<TraceAssignBatchReconciliationResponse>> getBatchDetail(
            @PathVariable Long batchId
    ) {
        return ResponseEntity.ok(ApiResponse.success(reconciliationService.getReconciliation(batchId)));
    }

    /**
     * Assignment-batch generated single-code list.
     * GET /api/trace-batches/{batchId}/codes
     */
    @GetMapping("/{batchId}/codes")
    @RequirePermission("trace:view")
    public ResponseEntity<ApiResponse<List<TraceAssignBatchCodeResponse>>> listBatchCodes(
            @PathVariable Long batchId
    ) {
        return ResponseEntity.ok(ApiResponse.success(codeQueryService.listCodes(batchId)));
    }
}
