package com.example.trace.service.impl;

import com.example.trace.dto.ChainVerifyResponse;
import com.example.trace.dto.PageResponse;
import com.example.trace.dto.ProduceAssignRequest;
import com.example.trace.dto.ProduceAssignResponse;
import com.example.trace.dto.ScanTraceRequest;
import com.example.trace.dto.TraceAvailableActionsResponse;
import com.example.trace.dto.TraceCodeActivateRequest;
import com.example.trace.dto.TraceCodeActivateResponse;
import com.example.trace.dto.TraceCodeLabelActionRequest;
import com.example.trace.dto.TraceCodeLabelActionResponse;
import com.example.trace.dto.TraceCorrectionRequest;
import com.example.trace.dto.TraceDetailResponse;
import com.example.trace.dto.TraceExceptionCloseRequest;
import com.example.trace.dto.TraceListItemResponse;
import com.example.trace.dto.TracePageRequest;
import com.example.trace.common.BizCode;
import com.example.trace.common.BizException;
import com.example.trace.entity.TraceAggregation;
import com.example.trace.enums.ActionType;
import com.example.trace.entity.TraceLifecycleLog;
import com.example.trace.entity.TraceSnapshot;
import com.example.trace.mapper.TraceAggregationMapper;
import com.example.trace.mapper.TraceLifecycleLogMapper;
import com.example.trace.mapper.TraceSnapshotMapper;
import com.example.trace.security.PermissionService;
import com.example.trace.service.impl.support.TraceAvailableActionService;
import com.example.trace.service.impl.support.TraceChainVerifyService;
import com.example.trace.service.impl.support.TraceCodeActivationService;
import com.example.trace.service.impl.support.TraceCodeAssignmentService;
import com.example.trace.service.impl.support.TraceCodeLabelService;
import com.example.trace.service.impl.support.TraceExceptionWorkflowService;
import com.example.trace.service.impl.support.TraceScanRetryExecutor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
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
    private TraceExceptionWorkflowService traceExceptionWorkflowService;
    @Mock
    private PermissionService permissionService;
    @Mock
    private TraceAggregationMapper traceAggregationMapper;

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
                traceExceptionWorkflowService,
                permissionService,
                traceAggregationMapper
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
    void listTraces_shouldDelegateToMapperWithDefaultsAndReturnPagedResponse() {
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<TraceListItemResponse> mapperPage =
                new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(1, 10);
        TraceListItemResponse row = TraceListItemResponse.builder()
                .traceCode("TRC-PAGE-001")
                .currentStatus("IN_STOCK")
                .build();
        mapperPage.setRecords(List.of(row));
        mapperPage.setTotal(1L);
        when(traceSnapshotMapper.selectTracePage(
                any(),
                isNull(),
                eq(List.of()),
                isNull(),
                isNull(),
                isNull(),
                isNull(),
                isNull(),
                isNull(),
                isNull(),
                eq("last_event_time"),
                eq(false)
        )).thenReturn(mapperPage);

        TracePageRequest request = new TracePageRequest();
        PageResponse<TraceListItemResponse> response = service.listTraces(request);

        assertThat(response.getTotal()).isEqualTo(1L);
        assertThat(response.getList()).hasSize(1);
        assertThat(response.getList().get(0).getTraceCode()).isEqualTo("TRC-PAGE-001");
        assertThat(response.getPage()).isEqualTo(1);
        assertThat(response.getSize()).isEqualTo(10);
    }

    @Test
    void listTraces_shouldParseStatusCsvAndDateRange() {
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<TraceListItemResponse> mapperPage =
                new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(2, 20);
        mapperPage.setRecords(List.of());
        mapperPage.setTotal(0L);
        when(traceSnapshotMapper.selectTracePage(
                any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), anyString(), anyBoolean()
        )).thenReturn(mapperPage);

        TracePageRequest request = new TracePageRequest();
        request.setStatus("IN_STOCK,IN_TRANSIT");
        request.setEventTimeFrom("2026-05-01T00:00:00");
        request.setEventTimeTo("2026-05-08T23:59:59");
        request.setKeyword("  TRC  ");
        request.setSpuId(7L);
        request.setSort("trace_code");
        request.setOrder("asc");
        request.setPage(2);
        request.setSize(20);

        service.listTraces(request);

        verify(traceSnapshotMapper).selectTracePage(
                any(),
                eq("TRC"),
                eq(List.of("IN_STOCK", "IN_TRANSIT")),
                eq(7L),
                isNull(),
                isNull(),
                isNull(),
                isNull(),
                eq(LocalDateTime.parse("2026-05-01T00:00:00")),
                eq(LocalDateTime.parse("2026-05-08T23:59:59")),
                eq("trace_code"),
                eq(true)
        );
    }

    @Test
    void listTraces_shouldRejectIllegalStatus() {
        TracePageRequest request = new TracePageRequest();
        request.setStatus("UNKNOWN");

        assertThatThrownBy(() -> service.listTraces(request))
                .isInstanceOf(BizException.class)
                .satisfies(error -> {
                    BizException exception = (BizException) error;
                    assertThat(exception.getCode()).isEqualTo(BizCode.PARAM_ERROR);
                    assertThat(exception.getMessage()).contains("UNKNOWN");
                });
        verify(traceSnapshotMapper, never()).selectTracePage(
                any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), anyString(), anyBoolean()
        );
    }

    @Test
    void listTraces_shouldRejectIllegalDateTime() {
        TracePageRequest request = new TracePageRequest();
        request.setEventTimeFrom("not-a-date");

        assertThatThrownBy(() -> service.listTraces(request))
                .isInstanceOf(BizException.class)
                .satisfies(error -> {
                    BizException exception = (BizException) error;
                    assertThat(exception.getCode()).isEqualTo(BizCode.PARAM_ERROR);
                    assertThat(exception.getMessage()).contains("event_time_from");
                });
    }

    @Test
    void listTraces_shouldClampPageSizeToTwoHundred() {
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<TraceListItemResponse> mapperPage =
                new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(1, 200);
        mapperPage.setRecords(List.of());
        mapperPage.setTotal(0L);
        when(traceSnapshotMapper.selectTracePage(
                any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), anyString(), anyBoolean()
        )).thenReturn(mapperPage);

        TracePageRequest request = new TracePageRequest();
        request.setSize(500);
        PageResponse<TraceListItemResponse> response = service.listTraces(request);

        assertThat(response.getSize()).isEqualTo(200);
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
    void exceptionWorkflowActions_shouldDelegateToWorkflowService() {
        TraceExceptionCloseRequest closeRequest = new TraceExceptionCloseRequest();
        closeRequest.setRemark("质检完成");
        TraceCodeLabelActionResponse closeExpected = TraceCodeLabelActionResponse.builder()
                .traceCode("trace-1")
                .actionType(ActionType.EXCEPTION_CLOSE)
                .currentStatus("IN_STOCK")
                .build();
        when(traceExceptionWorkflowService.closeException("trace-1", closeRequest, 7L, "tester"))
                .thenReturn(closeExpected);

        TraceCorrectionRequest correctionRequest = new TraceCorrectionRequest();
        correctionRequest.setCorrectionOf(9L);
        correctionRequest.setRemark("修正目标节点");
        TraceCodeLabelActionResponse correctionExpected = TraceCodeLabelActionResponse.builder()
                .traceCode("trace-1")
                .actionType(ActionType.CORRECTION)
                .build();
        when(traceExceptionWorkflowService.correctLifecycleLog("trace-1", correctionRequest, 7L, "tester"))
                .thenReturn(correctionExpected);

        TraceCodeLabelActionResponse closeResponse =
                service.closeException("trace-1", closeRequest, 7L, "tester");
        TraceCodeLabelActionResponse correctionResponse =
                service.correctLifecycleLog("trace-1", correctionRequest, 7L, "tester");

        assertThat(closeResponse).isSameAs(closeExpected);
        assertThat(correctionResponse).isSameAs(correctionExpected);
        verify(traceExceptionWorkflowService).closeException("trace-1", closeRequest, 7L, "tester");
        verify(traceExceptionWorkflowService).correctLifecycleLog("trace-1", correctionRequest, 7L, "tester");
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
        assertThat(response.getAggregationHistory()).isEmpty();
        verify(traceLifecycleLogMapper, never()).selectFullChain("trace-1");
    }

    @Test
    void detail_shouldIncludeDirectAndIndirectAggregationHistory() {
        TraceSnapshot snapshot = new TraceSnapshot();
        snapshot.setTraceCode("TRACE-001");
        TraceLifecycleLog log = new TraceLifecycleLog();
        TraceAggregation carton = aggregation(11L, "CARTON-001", "TRACE-001", "CARTON", true);
        TraceAggregation pallet = aggregation(12L, "PALLET-001", "CARTON-001", "PALLET", true);
        TraceAggregation oldCarton = aggregation(10L, "CARTON-OLD", "TRACE-001", "CARTON", false);
        when(traceSnapshotMapper.selectById("TRACE-001")).thenReturn(snapshot);
        when(traceLifecycleLogMapper.selectEffectiveHistory("TRACE-001")).thenReturn(List.of(log));
        when(traceAggregationMapper.selectHistoryByChild("TRACE-001")).thenReturn(List.of(carton, oldCarton));
        when(traceAggregationMapper.selectHistoryByChild("CARTON-001")).thenReturn(List.of(pallet));
        when(traceAggregationMapper.selectHistoryByChild("CARTON-OLD")).thenReturn(List.of());

        TraceDetailResponse response = service.detail("TRACE-001", "effective", 6L);

        assertThat(response.getAggregationHistory()).hasSize(3);
        assertThat(response.getAggregationHistory().get(0).getParentCode()).isEqualTo("PALLET-001");
        assertThat(response.getAggregationHistory().get(0).getDirect()).isFalse();
        assertThat(response.getAggregationHistory().get(0).getLevel()).isEqualTo(2);
        assertThat(response.getAggregationHistory().get(0).getViaCode()).isEqualTo("CARTON-001");
        assertThat(response.getAggregationHistory().get(1).getParentCode()).isEqualTo("CARTON-001");
        assertThat(response.getAggregationHistory().get(1).getDirect()).isTrue();
        assertThat(response.getAggregationHistory().get(2).getParentCode()).isEqualTo("CARTON-OLD");
        assertThat(response.getAggregationHistory().get(2).getActive()).isFalse();
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

    private static TraceAggregation aggregation(
            Long id,
            String parentCode,
            String childCode,
            String relationType,
            boolean active
    ) {
        TraceAggregation aggregation = new TraceAggregation();
        aggregation.setId(id);
        aggregation.setParentCode(parentCode);
        aggregation.setChildCode(childCode);
        aggregation.setRelationType(relationType);
        aggregation.setActive(active);
        aggregation.setBindTime(LocalDateTime.of(2026, 5, 7, 10, id.intValue() % 60));
        if (!active) {
            aggregation.setReleaseTime(LocalDateTime.of(2026, 5, 7, 11, id.intValue() % 60));
        }
        return aggregation;
    }
}
