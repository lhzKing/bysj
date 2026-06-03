package com.example.trace.controller;

import com.example.trace.annotation.RequirePermission;
import com.example.trace.common.ApiResponse;
import com.example.trace.dto.TraceAggregationBatchBindRequest;
import com.example.trace.dto.TraceAggregationBatchBindResponse;
import com.example.trace.dto.TraceAggregationBindRequest;
import com.example.trace.dto.TraceAggregationReleaseRequest;
import com.example.trace.dto.TraceAggregationResponse;
import com.example.trace.service.TraceAggregationService;
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
@RequestMapping("/api/trace-aggregations")
@RequiredArgsConstructor
public class TraceAggregationController {

    private static final String ATTR_USER_ID = "userId";
    private static final String ATTR_USERNAME = "username";

    private final TraceAggregationService traceAggregationService;

    @PostMapping
    @RequirePermission({"trace:task:scan", "trace:create", "trace:scan", "trace:outbound", "trace:transfer"})
    public ResponseEntity<ApiResponse<TraceAggregationResponse>> bindChild(
            @Valid @RequestBody TraceAggregationBindRequest request,
            HttpServletRequest httpRequest
    ) {
        TraceAggregationResponse response = traceAggregationService.bindChild(
                request,
                (Long) httpRequest.getAttribute(ATTR_USER_ID),
                operator(httpRequest)
        );
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.created(response, "聚合关系创建成功"));
    }

    @PostMapping("/batch")
    @RequirePermission({"trace:task:scan", "trace:create", "trace:scan", "trace:outbound", "trace:transfer"})
    public ApiResponse<TraceAggregationBatchBindResponse> bindChildrenBatch(
            @Valid @RequestBody TraceAggregationBatchBindRequest request,
            HttpServletRequest httpRequest
    ) {
        // 批量绑定按「跳过失败继续」语义，部分失败仍是正常结果（数据里给逐项 success/failure），
        // 故统一返回 200 + 汇总，由前端按 success_count / failure_count 提示，而非用 HTTP 状态表达。
        TraceAggregationBatchBindResponse response = traceAggregationService.bindChildrenBatch(
                request,
                (Long) httpRequest.getAttribute(ATTR_USER_ID),
                operator(httpRequest)
        );
        return ApiResponse.success(response, "批量绑定完成");
    }

    @PostMapping("/{relationId}/release")
    @RequirePermission({"trace:task:scan", "trace:create", "trace:scan", "trace:outbound", "trace:transfer"})
    public ApiResponse<TraceAggregationResponse> releaseRelation(
            @PathVariable Long relationId,
            @RequestBody(required = false) @Valid TraceAggregationReleaseRequest request,
            HttpServletRequest httpRequest
    ) {
        return ApiResponse.success(traceAggregationService.releaseRelation(
                relationId,
                request,
                (Long) httpRequest.getAttribute(ATTR_USER_ID),
                operator(httpRequest)
        ));
    }

    @GetMapping
    @RequirePermission("trace:view")
    public ApiResponse<List<TraceAggregationResponse>> listAllActive(
            @RequestParam(name = "relation_type", required = false) String relationType
    ) {
        return ApiResponse.success(traceAggregationService.listAllActive(relationType));
    }

    @GetMapping("/children")
    @RequirePermission("trace:view")
    public ApiResponse<List<TraceAggregationResponse>> listActiveChildren(@RequestParam(name = "parent_code") String parentCode) {
        return ApiResponse.success(traceAggregationService.listActiveChildren(parentCode));
    }

    @GetMapping("/parents")
    @RequirePermission("trace:view")
    public ApiResponse<List<TraceAggregationResponse>> listActiveParents(@RequestParam(name = "child_code") String childCode) {
        return ApiResponse.success(traceAggregationService.listActiveParents(childCode));
    }

    @GetMapping("/history/by-parent")
    @RequirePermission("trace:view")
    public ApiResponse<List<TraceAggregationResponse>> listHistoryByParent(@RequestParam(name = "parent_code") String parentCode) {
        return ApiResponse.success(traceAggregationService.listHistoryByParent(parentCode));
    }

    @GetMapping("/history/by-child")
    @RequirePermission("trace:view")
    public ApiResponse<List<TraceAggregationResponse>> listHistoryByChild(@RequestParam(name = "child_code") String childCode) {
        return ApiResponse.success(traceAggregationService.listHistoryByChild(childCode));
    }

    private String operator(HttpServletRequest request) {
        Object value = request.getAttribute(ATTR_USERNAME);
        return value == null ? "unknown" : value.toString();
    }
}
