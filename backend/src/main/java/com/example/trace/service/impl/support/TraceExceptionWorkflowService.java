package com.example.trace.service.impl.support;

import com.example.trace.common.BizCode;
import com.example.trace.common.BizException;
import com.example.trace.dto.ScanTraceRequest;
import com.example.trace.dto.TraceCodeLabelActionResponse;
import com.example.trace.dto.TraceCorrectionRequest;
import com.example.trace.dto.TraceExceptionCloseRequest;
import com.example.trace.entity.TraceLifecycleLog;
import com.example.trace.entity.TraceSnapshot;
import com.example.trace.enums.ActionType;
import com.example.trace.mapper.TraceLifecycleLogMapper;
import com.example.trace.mapper.TraceSnapshotMapper;
import com.example.trace.validation.TraceLocationFieldConstraints;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Service
public class TraceExceptionWorkflowService {

    private final TraceScanRetryExecutor traceScanRetryExecutor;
    private final TraceSnapshotMapper traceSnapshotMapper;
    private final TraceLifecycleLogMapper traceLifecycleLogMapper;

    public TraceExceptionWorkflowService(
            TraceScanRetryExecutor traceScanRetryExecutor,
            TraceSnapshotMapper traceSnapshotMapper,
            TraceLifecycleLogMapper traceLifecycleLogMapper
    ) {
        this.traceScanRetryExecutor = traceScanRetryExecutor;
        this.traceSnapshotMapper = traceSnapshotMapper;
        this.traceLifecycleLogMapper = traceLifecycleLogMapper;
    }

    public TraceCodeLabelActionResponse closeException(
            String traceCode,
            TraceExceptionCloseRequest request,
            Long operatorUserId,
            String operator
    ) {
        String normalizedTraceCode = normalizeTraceCode(traceCode);
        if (request == null) {
            throw new BizException(BizCode.PARAM_ERROR, "解除异常冻结参数不能为空");
        }
        String remark = requireRemark(request.getRemark(), "EXCEPTION_CLOSE 必须填写解除原因说明");

        ScanTraceRequest scanRequest = new ScanTraceRequest();
        scanRequest.setTraceCode(normalizedTraceCode);
        scanRequest.setOperatorUserId(operatorUserId);
        scanRequest.setActionType(ActionType.EXCEPTION_CLOSE);
        scanRequest.setRemark(remark);
        scanRequest.setEventTime(request.getEventTime());
        scanRequest.setIdempotencyKey(request.getIdempotencyKey());

        traceScanRetryExecutor.execute(scanRequest, operator);
        return buildActionResponse(normalizedTraceCode, ActionType.EXCEPTION_CLOSE, remark);
    }

    public TraceCodeLabelActionResponse correctLifecycleLog(
            String traceCode,
            TraceCorrectionRequest request,
            Long operatorUserId,
            String operator
    ) {
        String normalizedTraceCode = normalizeTraceCode(traceCode);
        if (request == null) {
            throw new BizException(BizCode.PARAM_ERROR, "审计纠错参数不能为空");
        }
        if (request.getCorrectionOf() == null) {
            throw new BizException(BizCode.PARAM_ERROR, "CORRECTION 类型必须指定 correctionOf 参数");
        }
        String remark = requireRemark(request.getRemark(), "CORRECTION 必须填写纠错原因说明");
        TraceLifecycleLog original = traceLifecycleLogMapper.selectById(request.getCorrectionOf());
        if (original == null) {
            throw new BizException(BizCode.PARAM_ERROR,
                    "被修正的日志不存在: " + request.getCorrectionOf());
        }
        if (!Objects.equals(original.getTraceCode(), normalizedTraceCode)) {
            throw new BizException(BizCode.PARAM_ERROR,
                    "跨链修正攻击检测：不能修正其他溯源码的日志");
        }

        ScanTraceRequest scanRequest = new ScanTraceRequest();
        scanRequest.setTraceCode(normalizedTraceCode);
        scanRequest.setOperatorUserId(operatorUserId);
        scanRequest.setActionType(ActionType.CORRECTION);
        scanRequest.setCorrectionOf(request.getCorrectionOf());
        scanRequest.setFromNode(request.getFromNode());
        scanRequest.setToNode(request.getToNode());
        scanRequest.setProvince(request.getProvince());
        scanRequest.setCity(request.getCity());
        scanRequest.setRemark(remark);
        scanRequest.setEventTime(request.getEventTime());
        scanRequest.setIdempotencyKey(request.getIdempotencyKey());

        traceScanRetryExecutor.execute(scanRequest, operator);
        return buildActionResponse(normalizedTraceCode, ActionType.CORRECTION, remark);
    }

    private TraceCodeLabelActionResponse buildActionResponse(
            String traceCode,
            ActionType actionType,
            String remark
    ) {
        TraceSnapshot snapshot = traceSnapshotMapper.selectById(traceCode);
        if (snapshot == null) {
            throw new BizException(BizCode.TRACE_NOT_FOUND, "未知溯源码: " + traceCode);
        }
        TraceLifecycleLog latest = traceLifecycleLogMapper.selectLatestByTraceCode(traceCode);
        return TraceCodeLabelActionResponse.builder()
                .traceCode(traceCode)
                .actionType(actionType)
                .currentStatus(snapshot.getCurrentStatus())
                .lifecycleLogId(latest == null ? null : latest.getId())
                .eventTime(latest == null || latest.getEventTime() == null
                        ? null
                        : latest.getEventTime().toString())
                .remark(remark)
                .build();
    }

    private String normalizeTraceCode(String traceCode) {
        if (traceCode == null || traceCode.isBlank()) {
            throw new BizException(BizCode.PARAM_ERROR, "traceCode 不能为空");
        }
        return traceCode.trim();
    }

    private String requireRemark(String rawRemark, String message) {
        String remark = TraceLocationFieldConstraints.normalizeRemark("remark", rawRemark);
        if (remark == null) {
            throw new BizException(BizCode.PARAM_ERROR, message);
        }
        return remark;
    }
}
