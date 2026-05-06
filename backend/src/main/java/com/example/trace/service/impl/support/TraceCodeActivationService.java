package com.example.trace.service.impl.support;

import com.example.trace.common.BizCode;
import com.example.trace.common.BizException;
import com.example.trace.dto.TraceCodeActivateRequest;
import com.example.trace.dto.TraceCodeActivateResponse;
import com.example.trace.entity.TraceCode;
import com.example.trace.entity.TraceLifecycleLog;
import com.example.trace.entity.TraceSnapshot;
import com.example.trace.enums.ActionType;
import com.example.trace.mapper.TraceLifecycleLogMapper;
import com.example.trace.mapper.TraceSnapshotMapper;
import com.example.trace.util.DateTimeUtil;
import com.example.trace.util.HashUtil;
import com.example.trace.validation.TraceLocationFieldConstraints;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Service
public class TraceCodeActivationService {

    private final TraceCodeStatusService traceCodeStatusService;
    private final TraceSnapshotMapper traceSnapshotMapper;
    private final TraceLifecycleLogMapper traceLifecycleLogMapper;
    private final TraceLogFactory traceLogFactory;

    public TraceCodeActivationService(
            TraceCodeStatusService traceCodeStatusService,
            TraceSnapshotMapper traceSnapshotMapper,
            TraceLifecycleLogMapper traceLifecycleLogMapper,
            TraceLogFactory traceLogFactory
    ) {
        this.traceCodeStatusService = traceCodeStatusService;
        this.traceSnapshotMapper = traceSnapshotMapper;
        this.traceLifecycleLogMapper = traceLifecycleLogMapper;
        this.traceLogFactory = traceLogFactory;
    }

    @Transactional
    public TraceCodeActivateResponse activateCode(
            String rawTraceCode,
            TraceCodeActivateRequest request,
            String operator
    ) {
        String traceCode = normalizeTraceCode(rawTraceCode);
        TraceSnapshot snapshot = requireSnapshot(traceCode);

        String activationNode = resolveActivationNode(snapshot, request == null ? null : request.getActivationNode());
        String deviceId = TraceLocationFieldConstraints.normalizeIdempotencyKey(
                "deviceId",
                request == null ? null : request.getDeviceId()
        );
        String remark = TraceLocationFieldConstraints.normalizeRemark(
                "remark",
                request == null ? null : request.getRemark()
        );
        String auditRemark = buildAuditRemark(remark, deviceId);
        LocalDateTime eventTime = DateTimeUtil.parseOrNow(request == null ? null : request.getEventTime())
                .truncatedTo(ChronoUnit.SECONDS);
        LocalDateTime ingestTime = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);

        TraceCode activatedCode = traceCodeStatusService.markActivated(
                traceCode,
                null,
                operator,
                eventTime
        );

        TraceLifecycleLog activationLog = traceLogFactory.createLog(
                traceCode,
                snapshot.getSpuId(),
                ActionType.ACTIVATE_CODE,
                activationNode,
                null,
                snapshot.getProvince(),
                snapshot.getCity(),
                auditRemark,
                eventTime,
                ingestTime,
                HashUtil.safePrev(snapshot.getLastHash()),
                null,
                operator
        );
        traceLifecycleLogMapper.insert(activationLog);

        if ((snapshot.getCurrentNode() == null || snapshot.getCurrentNode().isBlank())
                && activationNode != null) {
            snapshot.setCurrentNode(activationNode);
            snapshot.setCurrentOwner(activationNode);
        }
        snapshot.setLastEventTime(eventTime);
        snapshot.setLastLogId(activationLog.getId());
        snapshot.setLastHash(activationLog.getCurrentHash());
        int updated = traceSnapshotMapper.updateById(snapshot);
        if (updated == 0) {
            throw new TraceOptimisticLockException("乐观锁冲突，traceCode: " + traceCode);
        }

        return TraceCodeActivateResponse.builder()
                .traceCode(traceCode)
                .actionType(ActionType.ACTIVATE_CODE)
                .codeStatus(activatedCode.getCodeStatus())
                .activationNode(activationNode)
                .deviceId(deviceId)
                .activatedByUsername(activatedCode.getActivatedByUsername())
                .activatedTime(activatedCode.getActivatedTime() == null
                        ? null
                        : activatedCode.getActivatedTime().toString())
                .lifecycleLogId(activationLog.getId())
                .remark(auditRemark)
                .build();
    }

    private TraceSnapshot requireSnapshot(String traceCode) {
        TraceSnapshot snapshot = traceSnapshotMapper.selectById(traceCode);
        if (snapshot == null) {
            throw new BizException(BizCode.TRACE_NOT_FOUND,
                    "未知溯源码，请先在生产环节赋码初始化: " + traceCode);
        }
        return snapshot;
    }

    private String resolveActivationNode(TraceSnapshot snapshot, String rawActivationNode) {
        String snapshotNode = TraceLocationFieldConstraints.normalizeNode(
                "currentNode",
                snapshot.getCurrentNode()
        );
        String requestedNode = TraceLocationFieldConstraints.normalizeNode(
                "activationNode",
                rawActivationNode
        );
        if (snapshotNode != null && requestedNode != null && !snapshotNode.equals(requestedNode)) {
            throw new BizException(BizCode.PARAM_ERROR,
                    "activationNode 与当前节点不一致: expected="
                            + snapshotNode
                            + ", actual="
                            + requestedNode);
        }
        return requestedNode == null ? snapshotNode : requestedNode;
    }

    private String normalizeTraceCode(String traceCode) {
        if (traceCode == null || traceCode.isBlank()) {
            throw new BizException(BizCode.PARAM_ERROR, "traceCode 不能为空");
        }
        return traceCode.trim();
    }

    private String buildAuditRemark(String remark, String deviceId) {
        String auditRemark = remark == null ? "扫码激活" : remark;
        if (deviceId != null) {
            auditRemark = auditRemark + "；deviceId=" + deviceId;
        }
        return TraceLocationFieldConstraints.normalizeRemark("remark", auditRemark);
    }
}
