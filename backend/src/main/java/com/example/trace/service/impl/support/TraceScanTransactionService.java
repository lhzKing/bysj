package com.example.trace.service.impl.support;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.trace.common.BizCode;
import com.example.trace.common.BizException;
import com.example.trace.dto.ScanTraceRequest;
import com.example.trace.entity.TraceLifecycleLog;
import com.example.trace.entity.TraceScanIdempotency;
import com.example.trace.entity.TraceSnapshot;
import com.example.trace.enums.ActionType;
import com.example.trace.enums.TraceStatus;
import com.example.trace.mapper.TraceLifecycleLogMapper;
import com.example.trace.mapper.TraceScanIdempotencyMapper;
import com.example.trace.mapper.TraceSnapshotMapper;
import com.example.trace.service.TraceUserNodeBindingService;
import com.example.trace.service.policy.TraceTransitionPolicy;
import com.example.trace.util.DateTimeUtil;
import com.example.trace.util.HashUtil;
import com.example.trace.util.ProvinceUtil;
import com.example.trace.validation.TraceLocationFieldConstraints;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class TraceScanTransactionService implements TraceScanExecutor {

    private final TraceSnapshotMapper traceSnapshotMapper;
    private final TraceLifecycleLogMapper traceLifecycleLogMapper;
    private final TraceScanIdempotencyMapper traceScanIdempotencyMapper;
    private final TraceLogFactory traceLogFactory;
    private final TraceTransitionPolicy traceTransitionPolicy;
    private final TraceCodeStatusService traceCodeStatusService;
    private final TraceUserNodeBindingService traceUserNodeBindingService;

    public TraceScanTransactionService(
            TraceSnapshotMapper traceSnapshotMapper,
            TraceLifecycleLogMapper traceLifecycleLogMapper,
            TraceScanIdempotencyMapper traceScanIdempotencyMapper,
            TraceLogFactory traceLogFactory,
            TraceTransitionPolicy traceTransitionPolicy,
            TraceCodeStatusService traceCodeStatusService
    ) {
        this(
                traceSnapshotMapper,
                traceLifecycleLogMapper,
                traceScanIdempotencyMapper,
                traceLogFactory,
                traceTransitionPolicy,
                traceCodeStatusService,
                null
        );
    }

    @Autowired
    public TraceScanTransactionService(
            TraceSnapshotMapper traceSnapshotMapper,
            TraceLifecycleLogMapper traceLifecycleLogMapper,
            TraceScanIdempotencyMapper traceScanIdempotencyMapper,
            TraceLogFactory traceLogFactory,
            TraceTransitionPolicy traceTransitionPolicy,
            TraceCodeStatusService traceCodeStatusService,
            TraceUserNodeBindingService traceUserNodeBindingService
    ) {
        this.traceSnapshotMapper = traceSnapshotMapper;
        this.traceLifecycleLogMapper = traceLifecycleLogMapper;
        this.traceScanIdempotencyMapper = traceScanIdempotencyMapper;
        this.traceLogFactory = traceLogFactory;
        this.traceTransitionPolicy = traceTransitionPolicy;
        this.traceCodeStatusService = traceCodeStatusService;
        this.traceUserNodeBindingService = traceUserNodeBindingService;
    }

    @Override
    public void execute(ScanTraceRequest request, String operator) {
        executeAndReturnCreated(request, operator);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean executeAndReturnCreated(ScanTraceRequest request, String operator) {
        normalizeAndValidateLocationFields(request);
        LocalDateTime eventTime = DateTimeUtil.parseOrNow(request.getEventTime())
                .truncatedTo(java.time.temporal.ChronoUnit.SECONDS);

        String traceCode = request.getTraceCode();
        ActionType actionType = request.getActionType();

        traceCodeStatusService.ensureLifecycleMovementAllowed(traceCode, actionType);
        IdempotencyDecision idempotencyDecision = acquireIdempotency(traceCode, actionType, request.getIdempotencyKey());
        if (idempotencyDecision.duplicateSucceeded()) {
            return false;
        }

        TraceSnapshot snapshot = traceSnapshotMapper.selectById(traceCode);
        if (snapshot == null) {
            throw new BizException(BizCode.TRACE_NOT_FOUND,
                    "未知溯源码，请先在生产环节赋码初始化: " + traceCode);
        }

        validateCorrection(traceCode, actionType, request.getCorrectionOf());
        TraceStatus currentStatus = parseCurrentStatus(snapshot);
        TraceStatus newStatus = traceTransitionPolicy.resolveNextStatus(
                currentStatus,
                actionType,
                request.getCorrectionOf()
        );
        ResolvedRouteNodes routeNodes = authorizeAndResolveRouteNodes(
                resolveRouteNodes(snapshot, request),
                request
        );

        LocalDateTime ingestTime = LocalDateTime.now()
                .truncatedTo(java.time.temporal.ChronoUnit.SECONDS);

        TraceLifecycleLog traceLog = traceLogFactory.createLog(
                traceCode,
                snapshot.getSpuId(),
                actionType,
                routeNodes.fromNode(),
                routeNodes.toNode(),
                ProvinceUtil.toFullName(request.getProvince()),
                request.getCity(),
                request.getRemark(),
                eventTime,
                ingestTime,
                HashUtil.safePrev(snapshot.getLastHash()),
                request.getCorrectionOf(),
                operator
        );
        traceLifecycleLogMapper.insert(traceLog);

        snapshot.setCurrentStatus(newStatus.getCode());

        if (routeNodes.toNode() != null && !routeNodes.toNode().isBlank()) {
            snapshot.setCurrentNode(routeNodes.toNode());
            snapshot.setCurrentOwner(routeNodes.toNode());
        }
        if (request.getProvince() != null && !request.getProvince().isBlank()) {
            snapshot.setProvince(ProvinceUtil.toFullName(request.getProvince()));
        }
        if (request.getCity() != null && !request.getCity().isBlank()) {
            snapshot.setCity(request.getCity());
        }
        snapshot.setLastEventTime(eventTime);
        snapshot.setLastLogId(traceLog.getId());
        snapshot.setLastHash(traceLog.getCurrentHash());

        int updated = traceSnapshotMapper.updateById(snapshot);
        if (updated == 0) {
            throw new TraceOptimisticLockException("乐观锁冲突，traceCode: " + traceCode);
        }
        if (actionType != ActionType.CORRECTION) {
            traceCodeStatusService.syncAfterLifecycleTransition(traceCode, newStatus);
        }
        markIdempotencySucceeded(idempotencyDecision.record(), traceLog.getId());
        return true;
    }

    private IdempotencyDecision acquireIdempotency(String traceCode, ActionType actionType, String idempotencyKey) {
        if (idempotencyKey == null) {
            return IdempotencyDecision.notRequested();
        }
        if (actionType == null) {
            throw new BizException(BizCode.INVALID_ACTION_TYPE, "actionType 不能为空");
        }

        TraceScanIdempotency record = new TraceScanIdempotency();
        record.setTraceCode(traceCode);
        record.setActionType(actionType.getCode());
        record.setIdempotencyKey(idempotencyKey);
        record.setStatus(TraceScanIdempotency.STATUS_PROCESSING);

        try {
            traceScanIdempotencyMapper.insert(record);
            return IdempotencyDecision.acquired(record);
        } catch (DuplicateKeyException e) {
            TraceScanIdempotency existing = selectIdempotency(traceCode, actionType, idempotencyKey);
            if (existing != null && existing.isSucceeded()) {
                return IdempotencyDecision.duplicateSucceeded(existing);
            }
            throw new BizException(BizCode.CONCURRENT_CONFLICT,
                    "扫码请求正在处理中，请勿重复提交: idempotencyKey=" + idempotencyKey);
        }
    }

    private TraceScanIdempotency selectIdempotency(String traceCode, ActionType actionType, String idempotencyKey) {
        return traceScanIdempotencyMapper.selectOne(new LambdaQueryWrapper<TraceScanIdempotency>()
                .eq(TraceScanIdempotency::getTraceCode, traceCode)
                .eq(TraceScanIdempotency::getActionType, actionType.getCode())
                .eq(TraceScanIdempotency::getIdempotencyKey, idempotencyKey));
    }

    private void markIdempotencySucceeded(TraceScanIdempotency record, Long lifecycleLogId) {
        if (record == null) {
            return;
        }
        record.setLifecycleLogId(lifecycleLogId);
        record.setStatus(TraceScanIdempotency.STATUS_SUCCEEDED);
        traceScanIdempotencyMapper.updateById(record);
    }

    private ResolvedRouteNodes resolveRouteNodes(TraceSnapshot snapshot, ScanTraceRequest request) {
        String snapshotCurrentNode = TraceLocationFieldConstraints.normalizeNode(
                "currentNode",
                snapshot.getCurrentNode()
        );
        String requestedFromNode = request.getFromNode();
        String fromNode = requestedFromNode;

        if (snapshotCurrentNode != null) {
            if (requestedFromNode == null) {
                fromNode = snapshotCurrentNode;
            } else if (request.getActionType() != ActionType.CORRECTION
                    && !snapshotCurrentNode.equals(requestedFromNode)) {
                throw new BizException(BizCode.PARAM_ERROR,
                        "fromNode 与当前节点不一致: expected="
                                + snapshotCurrentNode
                                + ", actual="
                                + requestedFromNode);
            }
        }

        String taskTargetNode = resolveTaskTargetNode(request);
        String requestedToNode = request.getToNode();
        String toNode = requestedToNode;
        if (taskTargetNode != null) {
            if (requestedToNode != null && !taskTargetNode.equals(requestedToNode)) {
                throw new BizException(BizCode.PARAM_ERROR,
                        "toNode 与流转任务目标节点不一致: expected="
                                + taskTargetNode
                                + ", actual="
                                + requestedToNode);
            }
            toNode = taskTargetNode;
        }

        return new ResolvedRouteNodes(fromNode, toNode);
    }


    private ResolvedRouteNodes authorizeAndResolveRouteNodes(
            ResolvedRouteNodes routeNodes,
            ScanTraceRequest request
    ) {
        if (traceUserNodeBindingService == null) {
            return routeNodes;
        }
        TraceUserNodeBindingService.RouteResolution resolution =
                traceUserNodeBindingService.authorizeAndResolveRoute(
                        request.getOperatorUserId(),
                        request.getActionType(),
                        routeNodes.fromNode(),
                        routeNodes.toNode()
                );
        if (resolution.operationNode() != null) {
            if (request.getProvince() == null || request.getProvince().isBlank()) {
                request.setProvince(resolution.operationNode().getProvince());
            }
            if (request.getCity() == null || request.getCity().isBlank()) {
                request.setCity(resolution.operationNode().getCity());
            }
        }
        return new ResolvedRouteNodes(resolution.fromNode(), resolution.toNode());
    }

    /**
     * B04 keeps the first-stage data model unchanged. B16-B20 will introduce
     * flow-task IDs and can replace this hook with real task target lookup.
     */
    private String resolveTaskTargetNode(ScanTraceRequest request) {
        return null;
    }

    private void normalizeAndValidateLocationFields(ScanTraceRequest request) {
        request.setFromNode(TraceLocationFieldConstraints.normalizeNode("fromNode", request.getFromNode()));
        request.setToNode(TraceLocationFieldConstraints.normalizeNode("toNode", request.getToNode()));
        request.setProvince(TraceLocationFieldConstraints.normalizeRegion("province", request.getProvince()));
        request.setCity(TraceLocationFieldConstraints.normalizeRegion("city", request.getCity()));
        request.setRemark(TraceLocationFieldConstraints.normalizeRemark("remark", request.getRemark()));
        request.setIdempotencyKey(TraceLocationFieldConstraints.normalizeIdempotencyKey(
                "idempotencyKey",
                request.getIdempotencyKey()
        ));
    }

    private void validateCorrection(String traceCode, ActionType actionType, Long correctionOf) {
        if (correctionOf != null && actionType != ActionType.CORRECTION) {
            throw new BizException(BizCode.PARAM_ERROR,
                    "只有 CORRECTION 类型允许指定 correctionOf 参数");
        }
        if (correctionOf != null) {
            TraceLifecycleLog originalLog = traceLifecycleLogMapper.selectById(correctionOf);
            if (originalLog == null) {
                throw new BizException(BizCode.PARAM_ERROR,
                        "被修正的日志不存在: " + correctionOf);
            }
            if (!originalLog.getTraceCode().equals(traceCode)) {
                throw new BizException(BizCode.PARAM_ERROR,
                        "跨链修正攻击检测：不能修正其他溯源码的日志");
            }
        } else if (actionType == ActionType.CORRECTION) {
            throw new BizException(BizCode.PARAM_ERROR,
                    "CORRECTION 类型必须指定 correctionOf 参数");
        }
    }

    private TraceStatus parseCurrentStatus(TraceSnapshot snapshot) {
        try {
            return TraceStatus.fromString(snapshot.getCurrentStatus());
        } catch (IllegalArgumentException e) {
            throw new BizException(BizCode.INVALID_ACTION_TYPE,
                    "溯源码当前状态非法，无法流转: traceCode="
                            + snapshot.getTraceCode()
                            + ", currentStatus="
                            + snapshot.getCurrentStatus());
        }
    }

    private record ResolvedRouteNodes(String fromNode, String toNode) {
    }

    private record IdempotencyDecision(TraceScanIdempotency record, boolean duplicateSucceeded) {

        static IdempotencyDecision notRequested() {
            return new IdempotencyDecision(null, false);
        }

        static IdempotencyDecision acquired(TraceScanIdempotency record) {
            return new IdempotencyDecision(record, false);
        }

        static IdempotencyDecision duplicateSucceeded(TraceScanIdempotency record) {
            return new IdempotencyDecision(record, true);
        }
    }
}
