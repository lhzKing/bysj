package com.example.trace.service.impl.support;

import com.example.trace.common.BizCode;
import com.example.trace.common.BizException;
import com.example.trace.dto.TraceCodeActivateRequest;
import com.example.trace.dto.TraceCodeActivateResponse;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TraceCodeActivationServiceTest {

    @Mock
    private TraceCodeStatusService traceCodeStatusService;
    @Mock
    private TraceSnapshotMapper traceSnapshotMapper;
    @Mock
    private TraceLifecycleLogMapper traceLifecycleLogMapper;
    @Mock
    private TraceLogFactory traceLogFactory;

    private TraceCodeActivationService service;

    @BeforeEach
    void setUp() {
        service = new TraceCodeActivationService(
                traceCodeStatusService,
                traceSnapshotMapper,
                traceLifecycleLogMapper,
                traceLogFactory
        );
    }

    @Test
    void activateCode_shouldUpdateCodeStatusAndAppendAuditLogWithNodeDeviceAndTime() {
        TraceSnapshot snapshot = snapshot("TRACE-ACT");
        when(traceSnapshotMapper.selectById("TRACE-ACT")).thenReturn(snapshot);
        LocalDateTime eventTime = LocalDateTime.of(2026, 5, 6, 9, 30);
        TraceCode activated = code("TRACE-ACT", TraceCodeStatus.ACTIVATED);
        activated.setActivatedByUsername("producer");
        activated.setActivatedTime(eventTime);
        when(traceCodeStatusService.markActivated("TRACE-ACT", null, "producer", eventTime))
                .thenReturn(activated);
        when(traceLogFactory.createLog(
                any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any()
        )).thenAnswer(invocation -> {
            TraceLifecycleLog log = new TraceLifecycleLog();
            log.setId(12L);
            log.setActionType(((ActionType) invocation.getArgument(2)).getCode());
            log.setFromNode(invocation.getArgument(3));
            log.setRemark(invocation.getArgument(7));
            log.setEventTime(invocation.getArgument(8));
            log.setCurrentHash("hash-activate");
            return log;
        });
        when(traceSnapshotMapper.updateById(any(TraceSnapshot.class))).thenReturn(1);
        TraceCodeActivateRequest request = new TraceCodeActivateRequest();
        request.setActivationNode(" 工厂A ");
        request.setDeviceId(" SCANNER-01 ");
        request.setEventTime("2026-05-06T09:30:00");
        request.setRemark("扫码复核");

        TraceCodeActivateResponse response = service.activateCode(" TRACE-ACT ", request, "producer");

        assertThat(response.getActionType()).isEqualTo(ActionType.ACTIVATE_CODE);
        assertThat(response.getCodeStatus()).isEqualTo(TraceCodeStatus.ACTIVATED.name());
        assertThat(response.getActivationNode()).isEqualTo("工厂A");
        assertThat(response.getDeviceId()).isEqualTo("SCANNER-01");
        assertThat(response.getActivatedByUsername()).isEqualTo("producer");
        assertThat(response.getActivatedTime()).isEqualTo("2026-05-06T09:30");
        assertThat(response.getLifecycleLogId()).isEqualTo(12L);
        assertThat(response.getRemark()).isEqualTo("扫码复核；deviceId=SCANNER-01");

        verify(traceCodeStatusService).markActivated("TRACE-ACT", null, "producer", eventTime);
        verify(traceLogFactory).createLog(
                eq("TRACE-ACT"),
                eq(1L),
                eq(ActionType.ACTIVATE_CODE),
                eq("工厂A"),
                eq(null),
                eq("浙江省"),
                eq("杭州市"),
                eq("扫码复核；deviceId=SCANNER-01"),
                eq(eventTime),
                any(LocalDateTime.class),
                eq("prev-hash"),
                eq(null),
                eq("producer")
        );

        ArgumentCaptor<TraceLifecycleLog> logCaptor = ArgumentCaptor.forClass(TraceLifecycleLog.class);
        verify(traceLifecycleLogMapper).insert(logCaptor.capture());
        assertThat(logCaptor.getValue().getActionType()).isEqualTo(ActionType.ACTIVATE_CODE.getCode());
        assertThat(logCaptor.getValue().getFromNode()).isEqualTo("工厂A");
        assertThat(logCaptor.getValue().getRemark()).isEqualTo("扫码复核；deviceId=SCANNER-01");

        ArgumentCaptor<TraceSnapshot> snapshotCaptor = ArgumentCaptor.forClass(TraceSnapshot.class);
        verify(traceSnapshotMapper).updateById(snapshotCaptor.capture());
        assertThat(snapshotCaptor.getValue().getCurrentStatus()).isEqualTo(TraceStatus.INIT.getCode());
        assertThat(snapshotCaptor.getValue().getLastLogId()).isEqualTo(12L);
        assertThat(snapshotCaptor.getValue().getLastHash()).isEqualTo("hash-activate");
    }

    @Test
    void activateCode_shouldRejectMismatchedActivationNodeBeforeStatusUpdate() {
        when(traceSnapshotMapper.selectById("TRACE-ACT")).thenReturn(snapshot("TRACE-ACT"));
        TraceCodeActivateRequest request = new TraceCodeActivateRequest();
        request.setActivationNode("仓库B");

        assertThatThrownBy(() -> service.activateCode("TRACE-ACT", request, "producer"))
                .isInstanceOf(BizException.class)
                .satisfies(error -> {
                    BizException exception = (BizException) error;
                    assertThat(exception.getCode()).isEqualTo(BizCode.PARAM_ERROR);
                    assertThat(exception.getMessage()).contains("activationNode 与当前节点不一致");
                });

        verify(traceCodeStatusService, never())
                .markActivated(any(), any(), any(), any());
        verify(traceLifecycleLogMapper, never()).insert(any(TraceLifecycleLog.class));
    }

    @Test
    void activateCode_shouldRejectRepeatedActivationWithoutAppendingAuditLog() {
        when(traceSnapshotMapper.selectById("TRACE-ACT")).thenReturn(snapshot("TRACE-ACT"));
        BizException repeated = new BizException(BizCode.INVALID_ACTION_TYPE,
                "只有 GENERATED/PRINTED 单品码允许激活");
        when(traceCodeStatusService.markActivated(eq("TRACE-ACT"), eq(null), eq("producer"), any(LocalDateTime.class)))
                .thenThrow(repeated);

        assertThatThrownBy(() -> service.activateCode("TRACE-ACT", new TraceCodeActivateRequest(), "producer"))
                .isSameAs(repeated);

        verify(traceLifecycleLogMapper, never()).insert(any(TraceLifecycleLog.class));
        verify(traceSnapshotMapper, never()).updateById(any(TraceSnapshot.class));
    }

    private static TraceSnapshot snapshot(String traceCode) {
        TraceSnapshot snapshot = new TraceSnapshot();
        snapshot.setTraceCode(traceCode);
        snapshot.setSpuId(1L);
        snapshot.setCurrentStatus(TraceStatus.INIT.getCode());
        snapshot.setCurrentNode("工厂A");
        snapshot.setCurrentOwner("工厂A");
        snapshot.setProvince("浙江省");
        snapshot.setCity("杭州市");
        snapshot.setLastHash("prev-hash");
        return snapshot;
    }

    private static TraceCode code(String traceCode, TraceCodeStatus status) {
        TraceCode code = new TraceCode();
        code.setTraceCode(traceCode);
        code.setSpuId(1L);
        code.setCodeStatus(status.name());
        return code;
    }
}
