package com.example.trace.controller;

import com.example.trace.annotation.RequirePermission;
import com.example.trace.common.ApiResponse;
import com.example.trace.common.BizCode;
import com.example.trace.common.BizException;
import com.example.trace.dto.ChainVerifyResponse;
import com.example.trace.dto.PageResponse;
import com.example.trace.dto.ProduceAssignRequest;
import com.example.trace.dto.ProduceAssignResponse;
import com.example.trace.dto.ScanTraceRequest;
import com.example.trace.dto.TraceAvailableActionsResponse;
import com.example.trace.dto.TraceCodeLabelActionRequest;
import com.example.trace.dto.TraceCodeLabelActionResponse;
import com.example.trace.dto.TraceCorrectionRequest;
import com.example.trace.dto.TraceDetailResponse;
import com.example.trace.dto.TraceExceptionCloseRequest;
import com.example.trace.dto.TraceFlowTaskCandidateResponse;
import com.example.trace.dto.TraceListItemResponse;
import com.example.trace.dto.TracePageRequest;
import com.example.trace.service.policy.TraceActionPermissionPolicy;
import com.example.trace.service.TraceFlowTaskService;
import com.example.trace.service.TraceService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 溯源控制器 - RESTful API
 * 资源: traces (溯源实例)
 * 
 * 权限要求：
 * - trace:batch:create（生产赋码批次；trace:create 保留为历史兼容权限）
 * - trace:code:print（打印、重打、作废标签）
 * - trace:scan（超级扫码权限，可执行所有扫码动作）
 * - trace:inbound / trace:outbound / trace:transfer（细粒度扫码权限）
 * - trace:exception:handle（异常上报、解除冻结、审计纠错）
 * - trace:view（溯源查询）
 */
@RestController
@RequestMapping("/api/traces")
public class TraceController {

    private static final String ATTR_USERNAME = "username";
    private static final String ATTR_USER_ID = "userId";
    private static final String ATTR_ROLE_ID = "roleId";

    private final TraceService traceService;
    private final TraceFlowTaskService traceFlowTaskService;
    private final TraceActionPermissionPolicy traceActionPermissionPolicy;

    public TraceController(
            TraceService traceService,
            TraceFlowTaskService traceFlowTaskService,
            TraceActionPermissionPolicy traceActionPermissionPolicy
    ) {
        this.traceService = traceService;
        this.traceFlowTaskService = traceFlowTaskService;
        this.traceActionPermissionPolicy = traceActionPermissionPolicy;
    }

    /**
     * 生产赋码（创建溯源实例）
     * POST /api/traces
     */
    @PostMapping
    @RequirePermission({"trace:batch:create", "trace:create"})
    public ResponseEntity<ApiResponse<ProduceAssignResponse>> createTraces(
            @RequestBody @Valid ProduceAssignRequest request,
            HttpServletRequest httpReq
    ) {
        String operator = operator(httpReq);
        ProduceAssignResponse response = traceService.produceAssign(request, operator);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.created(response, "赋码成功"));
    }

    /**
     * 分页查询追溯列表（trace_snapshot 视图 + SPU/批次/最近动作聚合）。
     * GET /api/traces
     *
     * 查询参数：keyword / status (多值用逗号) / spu_id / batch_no / current_node /
     *           current_owner / province / event_time_from / event_time_to / page / size /
     *           sort (last_event_time|trace_code|update_time|current_status) / order (asc|desc)
     */
    @GetMapping
    @RequirePermission("trace:view")
    public ResponseEntity<ApiResponse<PageResponse<TraceListItemResponse>>> listTraces(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(name = "spu_id", required = false) Long spuId,
            @RequestParam(name = "batch_no", required = false) String batchNo,
            @RequestParam(name = "current_node", required = false) String currentNode,
            @RequestParam(name = "current_owner", required = false) String currentOwner,
            @RequestParam(required = false) String province,
            @RequestParam(name = "event_time_from", required = false) String eventTimeFrom,
            @RequestParam(name = "event_time_to", required = false) String eventTimeTo,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String sort,
            @RequestParam(defaultValue = "desc") String order
    ) {
        TracePageRequest request = new TracePageRequest();
        request.setKeyword(keyword);
        request.setStatus(status);
        request.setSpuId(spuId);
        request.setBatchNo(batchNo);
        request.setCurrentNode(currentNode);
        request.setCurrentOwner(currentOwner);
        request.setProvince(province);
        request.setEventTimeFrom(eventTimeFrom);
        request.setEventTimeTo(eventTimeTo);
        request.setPage(page);
        request.setSize(size);
        request.setSort(sort);
        request.setOrder(order);
        return ResponseEntity.ok(ApiResponse.success(traceService.listTraces(request)));
    }

    /**
     * 扫码流转（创建流转事件）
     * POST /api/traces/{traceCode}/events
     * 
     * 权限细分：
     * - trace:scan = 超级扫码权限，可执行所有扫码动作
     * - trace:inbound / trace:outbound / trace:transfer 仅允许对应动作
     * - trace:exception:handle 仅允许异常上报动作
     * - 其他动作（如 CORRECTION）仅允许 trace:scan
     */
    @PostMapping("/{traceCode}/events")
    @RequirePermission({"trace:scan", "trace:inbound", "trace:outbound", "trace:transfer", "trace:exception:handle"})
    public ResponseEntity<ApiResponse<Void>> createEvent(
            @PathVariable String traceCode,
            @RequestBody @Valid ScanTraceRequest request,
            HttpServletRequest httpReq
    ) {
        // 确保路径中的 traceCode 与 body 中的一致
        request.setTraceCode(traceCode);
        request.setOperatorUserId((Long) httpReq.getAttribute(ATTR_USER_ID));
        
        // 细粒度权限校验：trace:scan 为超级权限，其余细粒度权限仅允许对应动作。
        Long roleId = (Long) httpReq.getAttribute(ATTR_ROLE_ID);
        if (roleId != null) {
            if (!traceActionPermissionPolicy.canExecute(roleId, request.getActionType())) {
                throw new BizException(BizCode.FORBIDDEN,
                    "无权限执行该扫码动作: " + request.getActionType());
            }
        }
        
        String operator = operator(httpReq);
        traceService.scan(request, operator);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.created(null, "流转记录成功"));
    }

    /**
     * 解除异常冻结。
     * POST /api/traces/{traceCode}/exception/close
     */
    @PostMapping("/{traceCode}/exception/close")
    @RequirePermission({"trace:exception:handle", "trace:scan"})
    public ResponseEntity<ApiResponse<TraceCodeLabelActionResponse>> closeException(
            @PathVariable String traceCode,
            @RequestBody @Valid TraceExceptionCloseRequest request,
            HttpServletRequest httpReq
    ) {
        TraceCodeLabelActionResponse response = traceService.closeException(
                traceCode,
                request,
                (Long) httpReq.getAttribute(ATTR_USER_ID),
                operator(httpReq)
        );
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.created(response, "异常冻结已解除"));
    }

    /**
     * 红冲蓝补式审计纠错。
     * POST /api/traces/{traceCode}/corrections
     */
    @PostMapping("/{traceCode}/corrections")
    @RequirePermission({"trace:exception:handle", "trace:scan"})
    public ResponseEntity<ApiResponse<TraceCodeLabelActionResponse>> correctLifecycleLog(
            @PathVariable String traceCode,
            @RequestBody @Valid TraceCorrectionRequest request,
            HttpServletRequest httpReq
    ) {
        TraceCodeLabelActionResponse response = traceService.correctLifecycleLog(
                traceCode,
                request,
                (Long) httpReq.getAttribute(ATTR_USER_ID),
                operator(httpReq)
        );
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.created(response, "审计纠错已提交"));
    }

    /**
     * 打印标签。
     * POST /api/traces/{traceCode}/print
     */
    @PostMapping("/{traceCode}/print")
    @RequirePermission({"trace:code:print", "trace:create"})
    public ResponseEntity<ApiResponse<TraceCodeLabelActionResponse>> printCode(
            @PathVariable String traceCode,
            @RequestBody(required = false) @Valid TraceCodeLabelActionRequest request,
            HttpServletRequest httpReq
    ) {
        TraceCodeLabelActionResponse response = traceService.printCode(traceCode, request, operator(httpReq));
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.created(response, "标签打印成功"));
    }

    /**
     * 重打/补打标签。
     * POST /api/traces/{traceCode}/reprint
     */
    @PostMapping("/{traceCode}/reprint")
    @RequirePermission({"trace:code:print", "trace:create"})
    public ResponseEntity<ApiResponse<TraceCodeLabelActionResponse>> reprintCode(
            @PathVariable String traceCode,
            @RequestBody(required = false) @Valid TraceCodeLabelActionRequest request,
            HttpServletRequest httpReq
    ) {
        TraceCodeLabelActionResponse response = traceService.reprintCode(traceCode, request, operator(httpReq));
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.created(response, "标签重打成功"));
    }

    /**
     * 作废未激活标签。
     * POST /api/traces/{traceCode}/void
     */
    @PostMapping("/{traceCode}/void")
    @RequirePermission({"trace:code:print", "trace:create"})
    public ResponseEntity<ApiResponse<TraceCodeLabelActionResponse>> voidCode(
            @PathVariable String traceCode,
            @RequestBody(required = false) @Valid TraceCodeLabelActionRequest request,
            HttpServletRequest httpReq
    ) {
        TraceCodeLabelActionResponse response = traceService.voidCode(traceCode, request, operator(httpReq));
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.created(response, "标签作废成功"));
    }

    /**
     * 溯源详情
     * GET /api/traces/{traceCode}?view=effective|audit
     */
    @GetMapping("/{traceCode}")
    @RequirePermission("trace:view")
    public ResponseEntity<ApiResponse<TraceDetailResponse>> getTrace(
            @PathVariable String traceCode,
            @RequestParam(defaultValue = "effective") String view,
            HttpServletRequest httpReq
    ) {
        Long roleId = (Long) httpReq.getAttribute(ATTR_ROLE_ID);
        TraceDetailResponse response = traceService.detail(traceCode, view, roleId);
        if (response == null || response.getSnapshot() == null) {
            throw new BizException(BizCode.TRACE_NOT_FOUND, "溯源码不存在");
        }
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 扫码后可执行动作
     * GET /api/traces/{traceCode}/available-actions
     */
    @GetMapping("/{traceCode}/available-actions")
    @RequirePermission("trace:view")
    public ResponseEntity<ApiResponse<TraceAvailableActionsResponse>> availableActions(
            @PathVariable String traceCode,
            HttpServletRequest httpReq
    ) {
        Long roleId = (Long) httpReq.getAttribute(ATTR_ROLE_ID);
        Long userId = (Long) httpReq.getAttribute(ATTR_USER_ID);
        TraceAvailableActionsResponse response = traceService.availableActions(traceCode, roleId, userId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 扫码后可参与的开放运单候选列表
     * GET /api/traces/{traceCode}/candidate-flow-tasks
     *
     * 用于普通扫码弹窗（ScanFlowDialog）展示运单下拉 + 自动填充 fromNode/toNode/省/市，
     * 并把"普通扫码工位 ↔ 任务扫码工作台"两条链路在 UI 上接通。
     */
    @GetMapping("/{traceCode}/candidate-flow-tasks")
    @RequirePermission("trace:view")
    public ResponseEntity<ApiResponse<List<TraceFlowTaskCandidateResponse>>> candidateFlowTasks(
            @PathVariable String traceCode
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                traceFlowTaskService.findCandidateFlowTasksForTrace(traceCode)
        ));
    }

    /**
     * 验证溯源链完整性
     * GET /api/traces/{traceCode}/verify
     * 
     * 验证内容：
     * 1. Hash 链连续性（prevHash 是否正确链接）
     * 2. Hash 完整性（重算 Hash 是否匹配）
     * 3. 数字签名有效性（RSA 签名验证）
     */
    @GetMapping("/{traceCode}/verify")
    @RequirePermission("trace:view")
    public ResponseEntity<ApiResponse<ChainVerifyResponse>> verifyChain(@PathVariable String traceCode) {
        ChainVerifyResponse response = traceService.verifyChain(traceCode);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 获取验证公钥
     * GET /api/traces/public-key
     * 
     * 返回 RSA 公钥（Base64 编码），供第三方独立验证数字签名
     * 即使没有访问本系统，也可以验证数据真实性
     */
    @GetMapping("/public-key")
    public ResponseEntity<ApiResponse<Map<String, String>>> getPublicKey() {
        String publicKey = traceService.getPublicKey();
        return ResponseEntity.ok(ApiResponse.success(Map.of(
                "publicKey", publicKey,
                "keyId", traceService.getCurrentSignatureKeyId(),
                "keyVersion", String.valueOf(traceService.getCurrentSignatureKeyVersion()),
                "algorithm", "RSA",
                "signatureAlgorithm", "SHA256withRSA"
        )));
    }

    private String operator(HttpServletRequest request) {
        Object v = request.getAttribute(ATTR_USERNAME);
        return v == null ? "unknown" : v.toString();
    }
}
