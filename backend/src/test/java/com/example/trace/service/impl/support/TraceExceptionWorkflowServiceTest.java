package com.example.trace.service.impl.support;

import com.example.trace.dto.ScanTraceRequest;
import com.example.trace.dto.TraceCodeLabelActionResponse;
import com.example.trace.dto.TraceCorrectionRequest;
import com.example.trace.dto.TraceExceptionCloseRequest;
import com.example.trace.entity.TraceLifecycleLog;
import com.example.trace.entity.TraceSnapshot;
import com.example.trace.enums.ActionType;
import com.example.trace.mapper.TraceLifecycleLogMapper;
import com.example.trace.mapper.TraceSnapshotMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TraceExceptionWorkflowServiceTest {

    @Mock
    private TraceScanRetryExecutor traceScanRetryExecutor;
    @Mock
    private TraceSnapshotMapper traceSnapshotMapper;
    @Mock
    private TraceLifecycleLogMapper traceLifecycleLogMapper;

    @Test
    void closeException_shouldCreateExceptionCloseScanRequestAndReturnLatestStatus() {
        TraceExceptionWorkflowService service = new TraceExceptionWorkflowService(
                traceScanRetryExecutor,
                traceSnapshotMapper,
                traceLifecycleLogMapper
        );
        TraceExceptionCloseRequest request = new TraceExceptionCloseRequest();
        request.setRemark("  复核无误，解除冻结  ");
        request.setEventTime("2026-05-07T12:10:00");
        request.setIdempotencyKey("close-001");
        TraceSnapshot snapshot = snapshot("TRACE-001", "IN_STOCK");
        when(traceSnapshotMapper.selectById("TRACE-001")).thenReturn(snapshot);
        when(traceLifecycleLogMapper.selectLatestByTraceCode("TRACE-001"))
                .thenReturn(log(88L, ActionType.EXCEPTION_CLOSE, "复核无误，解除冻结"));

        TraceCodeLabelActionResponse response =
                service.closeException("TRACE-001", request, 7L, "auditor");

        ArgumentCaptor<ScanTraceRequest> requestCaptor = ArgumentCaptor.forClass(ScanTraceRequest.class);
        verify(traceScanRetryExecutor).execute(requestCaptor.capture(), org.mockito.ArgumentMatchers.eq("auditor"));
        ScanTraceRequest scanRequest = requestCaptor.getValue();
        assertThat(scanRequest.getActionType()).isEqualTo(ActionType.EXCEPTION_CLOSE);
        assertThat(scanRequest.getTraceCode()).isEqualTo("TRACE-001");
        assertThat(scanRequest.getOperatorUserId()).isEqualTo(7L);
        assertThat(scanRequest.getRemark()).isEqualTo("复核无误，解除冻结");
        assertThat(scanRequest.getIdempotencyKey()).isEqualTo("close-001");

        assertThat(response.getActionType()).isEqualTo(ActionType.EXCEPTION_CLOSE);
        assertThat(response.getCurrentStatus()).isEqualTo("IN_STOCK");
        assertThat(response.getLifecycleLogId()).isEqualTo(88L);
    }

    @Test
    void correctLifecycleLog_shouldCreateCorrectionScanRequest() {
        TraceExceptionWorkflowService service = new TraceExceptionWorkflowService(
                traceScanRetryExecutor,
                traceSnapshotMapper,
                traceLifecycleLogMapper
        );
        TraceCorrectionRequest request = new TraceCorrectionRequest();
        request.setCorrectionOf(18L);
        request.setFromNode("节点A");
        request.setToNode("节点B");
        request.setRemark("  更正错误目标节点  ");
        request.setIdempotencyKey("corr-001");
        TraceLifecycleLog original = new TraceLifecycleLog();
        original.setId(18L);
        original.setTraceCode("TRACE-001");
        when(traceLifecycleLogMapper.selectById(18L)).thenReturn(original);
        when(traceSnapshotMapper.selectById("TRACE-001")).thenReturn(snapshot("TRACE-001", "EXCEPTION"));
        when(traceLifecycleLogMapper.selectLatestByTraceCode("TRACE-001"))
                .thenReturn(log(19L, ActionType.CORRECTION, "更正错误目标节点"));

        TraceCodeLabelActionResponse response =
                service.correctLifecycleLog("TRACE-001", request, 7L, "auditor");

        ArgumentCaptor<ScanTraceRequest> requestCaptor = ArgumentCaptor.forClass(ScanTraceRequest.class);
        verify(traceScanRetryExecutor).execute(requestCaptor.capture(), org.mockito.ArgumentMatchers.eq("auditor"));
        ScanTraceRequest scanRequest = requestCaptor.getValue();
        assertThat(scanRequest.getActionType()).isEqualTo(ActionType.CORRECTION);
        assertThat(scanRequest.getCorrectionOf()).isEqualTo(18L);
        assertThat(scanRequest.getFromNode()).isEqualTo("节点A");
        assertThat(scanRequest.getToNode()).isEqualTo("节点B");
        assertThat(scanRequest.getRemark()).isEqualTo("更正错误目标节点");
        assertThat(scanRequest.getIdempotencyKey()).isEqualTo("corr-001");

        assertThat(response.getActionType()).isEqualTo(ActionType.CORRECTION);
        assertThat(response.getLifecycleLogId()).isEqualTo(19L);
    }

    private static TraceSnapshot snapshot(String traceCode, String status) {
        TraceSnapshot snapshot = new TraceSnapshot();
        snapshot.setTraceCode(traceCode);
        snapshot.setCurrentStatus(status);
        return snapshot;
    }

    private static TraceLifecycleLog log(Long id, ActionType actionType, String remark) {
        TraceLifecycleLog log = new TraceLifecycleLog();
        log.setId(id);
        log.setActionType(actionType.getCode());
        log.setRemark(remark);
        log.setEventTime(LocalDateTime.of(2026, 5, 7, 12, id.intValue() % 60));
        return log;
    }
}
