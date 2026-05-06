package com.example.trace.service.impl.support;

import com.example.trace.common.BizCode;
import com.example.trace.common.BizException;
import com.example.trace.dto.TraceCodeLabelActionRequest;
import com.example.trace.dto.TraceCodeLabelActionResponse;
import com.example.trace.entity.TraceCode;
import com.example.trace.entity.TraceLifecycleLog;
import com.example.trace.entity.TraceSnapshot;
import com.example.trace.enums.ActionType;
import com.example.trace.enums.TraceCodeStatus;
import com.example.trace.enums.TraceStatus;
import com.example.trace.mapper.TraceLifecycleLogMapper;
import com.example.trace.mapper.TraceSnapshotMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TraceCodeLabelServiceTest {

    @Mock
    private TraceCodeStatusService traceCodeStatusService;
    @Mock
    private TraceSnapshotMapper traceSnapshotMapper;
    @Mock
    private TraceLifecycleLogMapper traceLifecycleLogMapper;
    @Mock
    private TraceLogFactory traceLogFactory;

    private TraceCodeLabelService service;

    @BeforeEach
    void setUp() {
        service = new TraceCodeLabelService(
                traceCodeStatusService,
                traceSnapshotMapper,
                traceLifecycleLogMapper,
                traceLogFactory
        );
    }

    @Test
    void printCode_shouldUpdateCodeStatusAndAppendAuditLogWithoutChangingSnapshotStatus() {
        TraceSnapshot snapshot = snapshot("TRACE-1");
        when(traceSnapshotMapper.selectById("TRACE-1")).thenReturn(snapshot);
        TraceCode printed = code("TRACE-1", TraceCodeStatus.PRINTED, 1);
        when(traceCodeStatusService.markPrinted("TRACE-1")).thenReturn(printed);
        when(traceLogFactory.createLog(
                any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any()
        )).thenAnswer(invocation -> {
            TraceLifecycleLog log = new TraceLifecycleLog();
            log.setActionType(((ActionType) invocation.getArgument(2)).getCode());
            log.setCurrentHash("hash-print");
            log.setId(99L);
            log.setEventTime(invocation.getArgument(8));
            log.setRemark(invocation.getArgument(7));
            return log;
        });
        TraceCodeLabelActionRequest request = new TraceCodeLabelActionRequest();
        request.setEventTime("2026-05-06T09:30:00");
        request.setRemark("首打");
        when(traceSnapshotMapper.updateById(any(TraceSnapshot.class))).thenReturn(1);

        TraceCodeLabelActionResponse response = service.printCode(" TRACE-1 ", request, "producer");

        assertThat(response.getActionType()).isEqualTo(ActionType.PRINT_CODE);
        assertThat(response.getCodeStatus()).isEqualTo(TraceCodeStatus.PRINTED.name());
        assertThat(response.getPrintCount()).isEqualTo(1);
        assertThat(response.getLifecycleLogId()).isEqualTo(99L);
        assertThat(response.getEventTime()).isEqualTo("2026-05-06T09:30");

        ArgumentCaptor<TraceLifecycleLog> logCaptor = ArgumentCaptor.forClass(TraceLifecycleLog.class);
        verify(traceLifecycleLogMapper).insert(logCaptor.capture());
        assertThat(logCaptor.getValue().getActionType()).isEqualTo(ActionType.PRINT_CODE.getCode());

        ArgumentCaptor<TraceSnapshot> snapshotCaptor = ArgumentCaptor.forClass(TraceSnapshot.class);
        verify(traceSnapshotMapper).updateById(snapshotCaptor.capture());
        assertThat(snapshotCaptor.getValue().getCurrentStatus()).isEqualTo(TraceStatus.INIT.getCode());
        assertThat(snapshotCaptor.getValue().getLastLogId()).isEqualTo(99L);
        assertThat(snapshotCaptor.getValue().getLastHash()).isEqualTo("hash-print");
    }

    @Test
    void reprintCode_shouldRequireRemark() {
        when(traceSnapshotMapper.selectById("TRACE-1")).thenReturn(snapshot("TRACE-1"));

        assertThatThrownBy(() -> service.reprintCode("TRACE-1", new TraceCodeLabelActionRequest(), "producer"))
                .isInstanceOf(BizException.class)
                .satisfies(error -> assertThat(((BizException) error).getCode()).isEqualTo(BizCode.PARAM_ERROR));
        verify(traceCodeStatusService, never()).markReprinted("TRACE-1");
    }

    @Test
    void reprintCode_shouldAppendReprintLogWithoutRollingBackActivatedStatus() {
        when(traceSnapshotMapper.selectById("TRACE-ACTIVE")).thenReturn(snapshot("TRACE-ACTIVE"));
        when(traceCodeStatusService.markReprinted("TRACE-ACTIVE"))
                .thenReturn(code("TRACE-ACTIVE", TraceCodeStatus.ACTIVATED, 3));
        when(traceLogFactory.createLog(
                any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any()
        )).thenAnswer(invocation -> {
            TraceLifecycleLog log = new TraceLifecycleLog();
            log.setId(8L);
            log.setActionType(((ActionType) invocation.getArgument(2)).getCode());
            log.setCurrentHash("hash-reprint");
            log.setEventTime(invocation.getArgument(8));
            log.setRemark(invocation.getArgument(7));
            return log;
        });
        when(traceSnapshotMapper.updateById(any(TraceSnapshot.class))).thenReturn(1);
        TraceCodeLabelActionRequest request = new TraceCodeLabelActionRequest();
        request.setRemark("已激活标签破损补打");

        TraceCodeLabelActionResponse response = service.reprintCode("TRACE-ACTIVE", request, "producer");

        assertThat(response.getActionType()).isEqualTo(ActionType.REPRINT_CODE);
        assertThat(response.getCodeStatus()).isEqualTo(TraceCodeStatus.ACTIVATED.name());
        assertThat(response.getPrintCount()).isEqualTo(3);
        assertThat(response.getRemark()).isEqualTo("已激活标签破损补打");
    }

    @Test
    void voidCode_shouldAppendVoidLogAndReturnVoidedStatus() {
        TraceSnapshot snapshot = snapshot("TRACE-VOID");
        when(traceSnapshotMapper.selectById("TRACE-VOID")).thenReturn(snapshot);
        when(traceCodeStatusService.markVoided("TRACE-VOID"))
                .thenReturn(code("TRACE-VOID", TraceCodeStatus.VOIDED, 0));
        TraceLifecycleLog log = new TraceLifecycleLog();
        log.setId(7L);
        log.setActionType(ActionType.VOID_CODE.getCode());
        log.setCurrentHash("hash-void");
        log.setEventTime(LocalDateTime.of(2026, 5, 6, 10, 0));
        log.setRemark("标签丢失");
        when(traceLogFactory.createLog(
                any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any()
        )).thenReturn(log);
        TraceCodeLabelActionRequest request = new TraceCodeLabelActionRequest();
        request.setRemark("标签丢失");
        when(traceSnapshotMapper.updateById(any(TraceSnapshot.class))).thenReturn(1);

        TraceCodeLabelActionResponse response = service.voidCode("TRACE-VOID", request, "producer");

        assertThat(response.getActionType()).isEqualTo(ActionType.VOID_CODE);
        assertThat(response.getCodeStatus()).isEqualTo(TraceCodeStatus.VOIDED.name());
        assertThat(response.getLifecycleLogId()).isEqualTo(7L);
    }

    @Test
    void voidCode_shouldRequireRemarkBeforeVoidingCode() {
        when(traceSnapshotMapper.selectById("TRACE-VOID")).thenReturn(snapshot("TRACE-VOID"));

        assertThatThrownBy(() -> service.voidCode("TRACE-VOID", new TraceCodeLabelActionRequest(), "producer"))
                .isInstanceOf(BizException.class)
                .satisfies(error -> assertThat(((BizException) error).getCode()).isEqualTo(BizCode.PARAM_ERROR));
        verify(traceCodeStatusService, never()).markVoided("TRACE-VOID");
    }

    private static TraceSnapshot snapshot(String traceCode) {
        TraceSnapshot snapshot = new TraceSnapshot();
        snapshot.setTraceCode(traceCode);
        snapshot.setSpuId(1L);
        snapshot.setCurrentStatus(TraceStatus.INIT.getCode());
        snapshot.setCurrentNode("工厂A");
        snapshot.setProvince("浙江省");
        snapshot.setCity("杭州市");
        snapshot.setLastHash("prev-hash");
        return snapshot;
    }

    private static TraceCode code(String traceCode, TraceCodeStatus status, int printCount) {
        TraceCode code = new TraceCode();
        code.setTraceCode(traceCode);
        code.setSpuId(1L);
        code.setCodeStatus(status.name());
        code.setPrintCount(printCount);
        return code;
    }
}
