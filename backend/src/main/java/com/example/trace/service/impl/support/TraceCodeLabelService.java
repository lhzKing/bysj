package com.example.trace.service.impl.support;

import com.example.trace.common.BizCode;
import com.example.trace.common.BizException;
import com.example.trace.dto.TraceCodeLabelActionRequest;
import com.example.trace.dto.TraceCodeLabelActionResponse;
import com.example.trace.entity.TraceCode;
import com.example.trace.entity.TraceLifecycleLog;
import com.example.trace.entity.TraceSnapshot;
import com.example.trace.enums.ActionType;
import com.example.trace.mapper.TraceLifecycleLogMapper;
import com.example.trace.mapper.TraceSnapshotMapper;
import com.example.trace.util.DateTimeUtil;
import com.example.trace.util.HashUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Service
public class TraceCodeLabelService {

    private final TraceCodeStatusService traceCodeStatusService;
    private final TraceSnapshotMapper traceSnapshotMapper;
    private final TraceLifecycleLogMapper traceLifecycleLogMapper;
    private final TraceLogFactory traceLogFactory;

    public TraceCodeLabelService(
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
    public TraceCodeLabelActionResponse printCode(
            String traceCode,
            TraceCodeLabelActionRequest request,
            String operator
    ) {
        return executeLabelAction(traceCode, request, operator, ActionType.PRINT_CODE);
    }

    @Transactional
    public TraceCodeLabelActionResponse reprintCode(
            String traceCode,
            TraceCodeLabelActionRequest request,
            String operator
    ) {
        return executeLabelAction(traceCode, request, operator, ActionType.REPRINT_CODE);
    }

    @Transactional
    public TraceCodeLabelActionResponse voidCode(
            String traceCode,
            TraceCodeLabelActionRequest request,
            String operator
    ) {
        return executeLabelAction(traceCode, request, operator, ActionType.VOID_CODE);
    }

    private TraceCodeLabelActionResponse executeLabelAction(
            String rawTraceCode,
            TraceCodeLabelActionRequest request,
            String operator,
            ActionType actionType
    ) {
        String traceCode = normalizeTraceCode(rawTraceCode);
        TraceSnapshot snapshot = requireSnapshot(traceCode);
        LocalDateTime eventTime = DateTimeUtil.parseOrNow(request == null ? null : request.getEventTime())
                .truncatedTo(ChronoUnit.SECONDS);
        LocalDateTime ingestTime = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
        String remark = resolveRemark(actionType, request == null ? null : request.getRemark());

        TraceCode code = switch (actionType) {
            case PRINT_CODE -> traceCodeStatusService.markPrinted(traceCode);
            case REPRINT_CODE -> traceCodeStatusService.markReprinted(traceCode);
            case VOID_CODE -> traceCodeStatusService.markVoided(traceCode);
            default -> throw new BizException(BizCode.INVALID_ACTION_TYPE,
                    "不支持的标签管理动作: " + actionType);
        };

        TraceLifecycleLog labelLog = traceLogFactory.createLog(
                traceCode,
                snapshot.getSpuId(),
                actionType,
                snapshot.getCurrentNode(),
                null,
                snapshot.getProvince(),
                snapshot.getCity(),
                remark,
                eventTime,
                ingestTime,
                HashUtil.safePrev(snapshot.getLastHash()),
                null,
                operator
        );
        traceLifecycleLogMapper.insert(labelLog);

        snapshot.setLastEventTime(eventTime);
        snapshot.setLastLogId(labelLog.getId());
        snapshot.setLastHash(labelLog.getCurrentHash());
        int updated = traceSnapshotMapper.updateById(snapshot);
        if (updated == 0) {
            throw new TraceOptimisticLockException("乐观锁冲突，traceCode: " + traceCode);
        }

        return TraceCodeLabelActionResponse.builder()
                .traceCode(traceCode)
                .actionType(actionType)
                .codeStatus(code.getCodeStatus())
                .printCount(code.getPrintCount())
                .lifecycleLogId(labelLog.getId())
                .eventTime(eventTime.toString())
                .remark(remark)
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

    private String normalizeTraceCode(String traceCode) {
        if (traceCode == null || traceCode.isBlank()) {
            throw new BizException(BizCode.PARAM_ERROR, "traceCode 不能为空");
        }
        return traceCode.trim();
    }

    private String resolveRemark(ActionType actionType, String rawRemark) {
        String remark = rawRemark == null || rawRemark.isBlank() ? null : rawRemark.trim();
        if (actionType == ActionType.PRINT_CODE) {
            return remark == null ? "标签打印" : remark;
        }
        if (remark == null) {
            throw new BizException(BizCode.PARAM_ERROR,
                    actionType.getCode() + " 必须填写原因说明");
        }
        return remark;
    }
}
