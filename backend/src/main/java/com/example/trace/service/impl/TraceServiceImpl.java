package com.example.trace.service.impl;

import com.example.trace.common.BizCode;
import com.example.trace.common.BizException;
import com.example.trace.dto.ChainVerifyResponse;
import com.example.trace.dto.ProduceAssignRequest;
import com.example.trace.dto.ProduceAssignResponse;
import com.example.trace.dto.ScanTraceRequest;
import com.example.trace.dto.TraceAvailableActionsResponse;
import com.example.trace.dto.TraceAggregationHistoryResponse;
import com.example.trace.dto.TraceCodeActivateRequest;
import com.example.trace.dto.TraceCodeActivateResponse;
import com.example.trace.dto.TraceCodeLabelActionRequest;
import com.example.trace.dto.TraceCodeLabelActionResponse;
import com.example.trace.dto.TraceDetailResponse;
import com.example.trace.entity.TraceAggregation;
import com.example.trace.entity.TraceSnapshot;
import com.example.trace.enums.TraceAggregationRelationType;
import com.example.trace.mapper.TraceAggregationMapper;
import com.example.trace.mapper.TraceLifecycleLogMapper;
import com.example.trace.mapper.TraceSnapshotMapper;
import com.example.trace.security.PermissionService;
import com.example.trace.service.TraceService;
import com.example.trace.service.impl.support.TraceAvailableActionService;
import com.example.trace.service.impl.support.TraceChainVerifyService;
import com.example.trace.service.impl.support.TraceCodeActivationService;
import com.example.trace.service.impl.support.TraceCodeAssignmentService;
import com.example.trace.service.impl.support.TraceCodeLabelService;
import com.example.trace.service.impl.support.TraceScanRetryExecutor;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class TraceServiceImpl implements TraceService {

    public static final String VIEW_EFFECTIVE = "effective";
    public static final String VIEW_AUDIT = "audit";
    public static final String TRACE_AUDIT_VIEW_PERMISSION = "trace:audit:view";

    private final TraceCodeAssignmentService traceCodeAssignmentService;
    private final TraceScanRetryExecutor traceScanRetryExecutor;
    private final TraceSnapshotMapper traceSnapshotMapper;
    private final TraceLifecycleLogMapper traceLifecycleLogMapper;
    private final TraceChainVerifyService traceChainVerifyService;
    private final TraceAvailableActionService traceAvailableActionService;
    private final TraceCodeLabelService traceCodeLabelService;
    private final TraceCodeActivationService traceCodeActivationService;
    private final PermissionService permissionService;
    private final TraceAggregationMapper traceAggregationMapper;

    public TraceServiceImpl(
            TraceCodeAssignmentService traceCodeAssignmentService,
            TraceScanRetryExecutor traceScanRetryExecutor,
            TraceSnapshotMapper traceSnapshotMapper,
            TraceLifecycleLogMapper traceLifecycleLogMapper,
            TraceChainVerifyService traceChainVerifyService,
            TraceAvailableActionService traceAvailableActionService,
            TraceCodeLabelService traceCodeLabelService,
            TraceCodeActivationService traceCodeActivationService,
            PermissionService permissionService,
            TraceAggregationMapper traceAggregationMapper
    ) {
        this.traceCodeAssignmentService = traceCodeAssignmentService;
        this.traceScanRetryExecutor = traceScanRetryExecutor;
        this.traceSnapshotMapper = traceSnapshotMapper;
        this.traceLifecycleLogMapper = traceLifecycleLogMapper;
        this.traceChainVerifyService = traceChainVerifyService;
        this.traceAvailableActionService = traceAvailableActionService;
        this.traceCodeLabelService = traceCodeLabelService;
        this.traceCodeActivationService = traceCodeActivationService;
        this.permissionService = permissionService;
        this.traceAggregationMapper = traceAggregationMapper;
    }

    @Override
    public ProduceAssignResponse produceAssign(ProduceAssignRequest request, String operator) {
        return traceCodeAssignmentService.produceAssign(request, operator);
    }

    @Override
    public void scan(ScanTraceRequest request, String operator) {
        traceScanRetryExecutor.execute(request, operator);
    }

    @Override
    public TraceCodeLabelActionResponse printCode(
            String traceCode,
            TraceCodeLabelActionRequest request,
            String operator
    ) {
        return traceCodeLabelService.printCode(traceCode, request, operator);
    }

    @Override
    public TraceCodeLabelActionResponse reprintCode(
            String traceCode,
            TraceCodeLabelActionRequest request,
            String operator
    ) {
        return traceCodeLabelService.reprintCode(traceCode, request, operator);
    }

    @Override
    public TraceCodeLabelActionResponse voidCode(
            String traceCode,
            TraceCodeLabelActionRequest request,
            String operator
    ) {
        return traceCodeLabelService.voidCode(traceCode, request, operator);
    }

    @Override
    public TraceCodeActivateResponse activateCode(
            String traceCode,
            TraceCodeActivateRequest request,
            String operator
    ) {
        return traceCodeActivationService.activateCode(traceCode, request, operator);
    }

    @Override
    public TraceDetailResponse detail(String traceCode, String view, Long roleId) {
        TraceSnapshot snapshot = traceSnapshotMapper.selectById(traceCode);
        if (snapshot == null) {
            throw new BizException(BizCode.TRACE_NOT_FOUND, 404, "未知溯源码: " + traceCode);
        }

        String normalizedView = normalizeDetailView(view);
        if (VIEW_AUDIT.equals(normalizedView)) {
            ensureAuditViewPermission(roleId);
            List<TraceAggregationHistoryResponse> aggregationHistory = buildAggregationHistory(snapshot.getTraceCode());
            return new TraceDetailResponse(
                    snapshot,
                    traceLifecycleLogMapper.selectFullChain(traceCode),
                    VIEW_AUDIT,
                    aggregationHistory
            );
        }
        List<TraceAggregationHistoryResponse> aggregationHistory = buildAggregationHistory(snapshot.getTraceCode());
        return new TraceDetailResponse(
                snapshot,
                traceLifecycleLogMapper.selectEffectiveHistory(traceCode),
                VIEW_EFFECTIVE,
                aggregationHistory
        );
    }

    @Override
    public TraceAvailableActionsResponse availableActions(String traceCode, Long roleId) {
        return traceAvailableActionService.availableActions(traceCode, roleId);
    }

    @Override
    public TraceAvailableActionsResponse availableActions(String traceCode, Long roleId, Long userId) {
        return traceAvailableActionService.availableActions(traceCode, roleId, userId);
    }

    @Override
    public ChainVerifyResponse verifyChain(String traceCode) {
        return traceChainVerifyService.verify(traceCode);
    }

    @Override
    public String getPublicKey() {
        return traceChainVerifyService.getPublicKey();
    }

    @Override
    public String getCurrentSignatureKeyId() {
        return traceChainVerifyService.getCurrentKeyId();
    }

    @Override
    public Integer getCurrentSignatureKeyVersion() {
        return traceChainVerifyService.getCurrentKeyVersion();
    }

    private String normalizeDetailView(String view) {
        if (view == null || view.isBlank()) {
            return VIEW_EFFECTIVE;
        }
        String normalizedView = view.trim().toLowerCase();
        if (VIEW_EFFECTIVE.equals(normalizedView) || VIEW_AUDIT.equals(normalizedView)) {
            return normalizedView;
        }
        throw new BizException(BizCode.PARAM_ERROR, "view 仅支持 effective 或 audit");
    }

    private void ensureAuditViewPermission(Long roleId) {
        if (roleId == null || !permissionService.hasPermission(roleId, TRACE_AUDIT_VIEW_PERMISSION)) {
            throw new BizException(BizCode.FORBIDDEN, "无权限查看审计完整视图: " + TRACE_AUDIT_VIEW_PERMISSION);
        }
    }

    private List<TraceAggregationHistoryResponse> buildAggregationHistory(String traceCode) {
        Map<String, TraceAggregationHistoryResponse> responsesByKey = new LinkedHashMap<>();
        List<TraceAggregation> directRelations = safeRelations(traceAggregationMapper.selectHistoryByChild(traceCode));
        directRelations.forEach(relation -> {
            TraceAggregationHistoryResponse response = toAggregationHistoryResponse(relation, true, 1, null);
            responsesByKey.put(historyKey(response), response);
        });

        directRelations.stream()
                .map(TraceAggregation::getParentCode)
                .filter(parentCode -> parentCode != null && !parentCode.isBlank())
                .distinct()
                .flatMap(parentCode -> safeRelations(traceAggregationMapper.selectHistoryByChild(parentCode)).stream()
                        .map(relation -> toAggregationHistoryResponse(relation, false, 2, parentCode)))
                .forEach(response -> responsesByKey.putIfAbsent(historyKey(response), response));

        return responsesByKey.values().stream()
                .sorted(Comparator
                        .comparing(TraceAggregationHistoryResponse::getActive, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(TraceAggregationHistoryResponse::getBindTime, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(TraceAggregationHistoryResponse::getRelationId, Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
    }

    private List<TraceAggregation> safeRelations(List<TraceAggregation> relations) {
        return relations == null ? List.of() : relations;
    }

    private TraceAggregationHistoryResponse toAggregationHistoryResponse(
            TraceAggregation relation,
            boolean direct,
            int level,
            String viaCode
    ) {
        TraceAggregationRelationType relationType = parseRelationType(relation);
        return TraceAggregationHistoryResponse.builder()
                .relationId(relation.getId())
                .parentCode(relation.getParentCode())
                .childCode(relation.getChildCode())
                .relationType(relationType)
                .relationTypeLabel(relationType.getLabel())
                .active(relation.getActive())
                .direct(direct)
                .level(level)
                .viaCode(viaCode)
                .bindTime(relation.getBindTime())
                .releaseTime(relation.getReleaseTime())
                .remark(relation.getRemark())
                .build();
    }

    private TraceAggregationRelationType parseRelationType(TraceAggregation relation) {
        try {
            return TraceAggregationRelationType.fromString(relation.getRelationType());
        } catch (IllegalArgumentException e) {
            throw new BizException(BizCode.BAD_REQUEST,
                    "聚合关系类型非法: relationId=" + relation.getId()
                            + ", relationType=" + relation.getRelationType());
        }
    }

    private String historyKey(TraceAggregationHistoryResponse response) {
        return (response.getRelationId() == null ? "null" : response.getRelationId())
                + ":" + response.getLevel()
                + ":" + response.getParentCode()
                + ":" + response.getChildCode();
    }
}
