package com.example.trace.service.impl;

import com.example.trace.dto.ChainVerifyResponse;
import com.example.trace.dto.ProduceAssignRequest;
import com.example.trace.dto.ProduceAssignResponse;
import com.example.trace.dto.ScanTraceRequest;
import com.example.trace.dto.TraceAvailableActionsResponse;
import com.example.trace.dto.TraceCodeActivateRequest;
import com.example.trace.dto.TraceCodeActivateResponse;
import com.example.trace.dto.TraceCodeLabelActionRequest;
import com.example.trace.dto.TraceCodeLabelActionResponse;
import com.example.trace.dto.TraceDetailResponse;
import com.example.trace.common.BizCode;
import com.example.trace.common.BizException;
import com.example.trace.enums.ActionType;
import com.example.trace.entity.TraceLifecycleLog;
import com.example.trace.entity.TraceSnapshot;
import com.example.trace.mapper.TraceLifecycleLogMapper;
import com.example.trace.mapper.TraceSnapshotMapper;
import com.example.trace.security.PermissionService;
import com.example.trace.service.impl.support.TraceAvailableActionService;
import com.example.trace.service.impl.support.TraceChainVerifyService;
import com.example.trace.service.impl.support.TraceCodeActivationService;
import com.example.trace.service.impl.support.TraceCodeAssignmentService;
import com.example.trace.service.impl.support.TraceCodeLabelService;
import com.example.trace.service.impl.support.TraceScanRetryExecutor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TraceServiceImplTest {

    @Mock
    private TraceCodeAssignmentService traceCodeAssignmentService;
    @Mock
    private TraceScanRetryExecutor traceScanRetryExecutor;
    @Mock
    private TraceSnapshotMapper traceSnapshotMapper;
    @Mock
    private TraceLifecycleLogMapper traceLifecycleLogMapper;
    @Mock
    private TraceChainVerifyService traceChainVerifyService;
    @Mock
    private TraceAvailableActionService traceAvailableActionService;
    @Mock
    private TraceCodeLabelService traceCodeLabelService;
    @Mock
    private TraceCodeActivationService traceCodeActivationService;
    @Mock
    private PermissionService permissionService;

    private TraceServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new TraceServiceImpl(
                traceCodeAssignmentService,
                traceScanRetryExecutor,
                traceSnapshotMapper,
                traceLifecycleLogMapper,
                traceChainVerifyService,
                traceAvailableActionService,
                traceCodeLabelService,
                traceCodeActivationService,
                permissionService
        );
    }

    @Test
    void produceAssign_shouldDelegateToAssignmentService() {
        ProduceAssignRequest request = new ProduceAssignRequest();
        ProduceAssignResponse expected = new ProduceAssignResponse(1, List.of("trace-1"));
        when(traceCodeAssignmentService.produceAssign(request, "tester")).thenReturn(expected);

        ProduceAssignResponse response = service.produceAssign(request, "tester");

        assertThat(response).isSameAs(expected);
    }

    @Test
    void scan_shouldDelegateToRetryExecutor() {
        ScanTraceRequest request = new ScanTraceRequest();

        service.scan(request, "tester");

        verify(traceScanRetryExecutor).execute(request, "tester");
    }

    @Test
    void labelActions_shouldDelegateToTraceCodeLabelService() {
        TraceCodeLabelActionRequest request = new TraceCodeLabelActionRequest();
        TraceCodeLabelActionResponse expected = TraceCodeLabelActionResponse.builder()
                .traceCode("trace-1")
                .actionType(ActionType.PRINT_CODE)
                .build();
        when(traceCodeLabelService.printCode("trace-1", request, "tester")).thenReturn(expected);
        TraceCodeLabelActionResponse reprintExpected = TraceCodeLabelActionResponse.builder()
                .traceCode("trace-1")
                .actionType(ActionType.REPRINT_CODE)
                .build();
        TraceCodeLabelActionResponse voidExpected = TraceCodeLabelActionResponse.builder()
                .traceCode("trace-1")
                .actionType(ActionType.VOID_CODE)
                .build();
        when(traceCodeLabelService.reprintCode("trace-1", request, "tester")).thenReturn(reprintExpected);
        when(traceCodeLabelService.voidCode("trace-1", request, "tester")).thenReturn(voidExpected);

        TraceCodeLabelActionResponse response = service.printCode("trace-1", request, "tester");
        TraceCodeLabelActionResponse reprint = service.reprintCode("trace-1", request, "tester");
        TraceCodeLabelActionResponse voided = service.voidCode("trace-1", request, "tester");

        assertThat(response).isSameAs(expected);
        assertThat(reprint).isSameAs(reprintExpected);
        assertThat(voided).isSameAs(voidExpected);
        verify(traceCodeLabelService).printCode("trace-1", request, "tester");
        verify(traceCodeLabelService).reprintCode("trace-1", request, "tester");
        verify(traceCodeLabelService).voidCode("trace-1", request, "tester");
    }

    @Test
    void activateCode_shouldDelegateToTraceCodeActivationService() {
        TraceCodeActivateRequest request = new TraceCodeActivateRequest();
        TraceCodeActivateResponse expected = TraceCodeActivateResponse.builder()
                .traceCode("trace-1")
                .actionType(ActionType.ACTIVATE_CODE)
                .codeStatus("ACTIVATED")
                .build();
        when(traceCodeActivationService.activateCode("trace-1", request, "tester")).thenReturn(expected);

        TraceCodeActivateResponse response = service.activateCode("trace-1", request, "tester");

        assertThat(response).isSameAs(expected);
        verify(traceCodeActivationService).activateCode("trace-1", request, "tester");
    }

    @Test
    void detail_shouldReturnEffectiveHistoryByDefault() {
        TraceSnapshot snapshot = new TraceSnapshot();
        snapshot.setTraceCode("trace-1");
        TraceLifecycleLog log = new TraceLifecycleLog();
        when(traceSnapshotMapper.selectById("trace-1")).thenReturn(snapshot);
        when(traceLifecycleLogMapper.selectEffectiveHistory("trace-1")).thenReturn(List.of(log));

        TraceDetailResponse response = service.detail("trace-1", null, 6L);

        assertThat(response.getSnapshot()).isSameAs(snapshot);
        assertThat(response.getHistory()).containsExactly(log);
        assertThat(response.getView()).isEqualTo("effective");
        verify(traceLifecycleLogMapper, never()).selectFullChain("trace-1");
    }

    @Test
    void detail_shouldReturnAuditFullChainWhenRoleHasAuditPermission() {
        TraceSnapshot snapshot = new TraceSnapshot();
        snapshot.setTraceCode("trace-1");
        TraceLifecycleLog original = new TraceLifecycleLog();
        original.setId(1L);
        TraceLifecycleLog correction = new TraceLifecycleLog();
        correction.setId(2L);
        correction.setCorrectionOf(1L);
        when(traceSnapshotMapper.selectById("trace-1")).thenReturn(snapshot);
        when(permissionService.hasPermission(2L, TraceServiceImpl.TRACE_AUDIT_VIEW_PERMISSION)).thenReturn(true);
        when(traceLifecycleLogMapper.selectFullChain("trace-1")).thenReturn(List.of(original, correction));

        TraceDetailResponse response = service.detail("trace-1", "audit", 2L);

        assertThat(response.getView()).isEqualTo("audit");
        assertThat(response.getHistory()).containsExactly(original, correction);
        verify(traceLifecycleLogMapper, never()).selectEffectiveHistory("trace-1");
    }

    @Test
    void detail_shouldRejectAuditViewWithoutAuditPermission() {
        TraceSnapshot snapshot = new TraceSnapshot();
        snapshot.setTraceCode("trace-1");
        when(traceSnapshotMapper.selectById("trace-1")).thenReturn(snapshot);
        when(permissionService.hasPermission(6L, TraceServiceImpl.TRACE_AUDIT_VIEW_PERMISSION)).thenReturn(false);

        assertThatThrownBy(() -> service.detail("trace-1", "audit", 6L))
                .isInstanceOf(BizException.class)
                .satisfies(error -> {
                    BizException exception = (BizException) error;
                    assertThat(exception.getCode()).isEqualTo(BizCode.FORBIDDEN);
                    assertThat(exception.getMessage()).contains("trace:audit:view");
                });

        verify(traceLifecycleLogMapper, never()).selectEffectiveHistory("trace-1");
        verify(traceLifecycleLogMapper, never()).selectFullChain("trace-1");
    }

    @Test
    void detail_shouldRejectUnknownView() {
        TraceSnapshot snapshot = new TraceSnapshot();
        snapshot.setTraceCode("trace-1");
        when(traceSnapshotMapper.selectById("trace-1")).thenReturn(snapshot);

        assertThatThrownBy(() -> service.detail("trace-1", "raw", 2L))
                .isInstanceOf(BizException.class)
                .satisfies(error -> assertThat(((BizException) error).getCode()).isEqualTo(BizCode.PARAM_ERROR));

        verify(traceLifecycleLogMapper, never()).selectEffectiveHistory("trace-1");
        verify(traceLifecycleLogMapper, never()).selectFullChain("trace-1");
    }

    @Test
    void verifyChain_shouldDelegateToChainVerifyService() {
        ChainVerifyResponse expected = ChainVerifyResponse.success(1, "hash", "sig", "pub", 1L);
        when(traceChainVerifyService.verify("trace-1")).thenReturn(expected);

        ChainVerifyResponse response = service.verifyChain("trace-1");

        assertThat(response).isSameAs(expected);
    }

    @Test
    void availableActions_shouldDelegateToAvailableActionService() {
        TraceAvailableActionsResponse expected = TraceAvailableActionsResponse.builder()
                .traceCode("trace-1")
                .recommendedAction(ActionType.INBOUND)
                .build();
        when(traceAvailableActionService.availableActions("trace-1", 4L)).thenReturn(expected);

        TraceAvailableActionsResponse response = service.availableActions("trace-1", 4L);

        assertThat(response).isSameAs(expected);
    }
}
