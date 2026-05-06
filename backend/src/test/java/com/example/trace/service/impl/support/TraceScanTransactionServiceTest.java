package com.example.trace.service.impl.support;

import com.example.trace.common.BizCode;
import com.example.trace.common.BizException;
import com.example.trace.dto.ChainVerifyResponse;
import com.example.trace.dto.ScanTraceRequest;
import com.example.trace.entity.TraceLifecycleLog;
import com.example.trace.entity.TraceScanIdempotency;
import com.example.trace.entity.TraceSnapshot;
import com.example.trace.enums.ActionType;
import com.example.trace.mapper.TraceLifecycleLogMapper;
import com.example.trace.mapper.TraceScanIdempotencyMapper;
import com.example.trace.mapper.TraceSnapshotMapper;
import com.example.trace.service.TraceUserNodeBindingService;
import com.example.trace.service.policy.TraceTransitionPolicy;
import com.example.trace.util.SignatureUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TraceScanTransactionServiceTest {

    @Mock
    private TraceSnapshotMapper traceSnapshotMapper;
    @Mock
    private TraceLifecycleLogMapper traceLifecycleLogMapper;
    @Mock
    private TraceScanIdempotencyMapper traceScanIdempotencyMapper;
    @Mock
    private SignatureUtil signatureUtil;
    @Mock
    private TraceCodeStatusService traceCodeStatusService;
    @Mock
    private TraceUserNodeBindingService traceUserNodeBindingService;

    private final TraceTransitionPolicy traceTransitionPolicy = new TraceTransitionPolicy();

    @Test
    void execute_shouldRejectCrossTraceCorrection() {
        TraceLogFactory logFactory = new TraceLogFactory(signatureUtil);
        TraceScanTransactionService service = new TraceScanTransactionService(
                traceSnapshotMapper,
                traceLifecycleLogMapper,
                traceScanIdempotencyMapper,
                logFactory,
                traceTransitionPolicy,
                traceCodeStatusService
        );

        ScanTraceRequest request = new ScanTraceRequest();
        request.setTraceCode("trace-1");
        request.setActionType(ActionType.CORRECTION);
        request.setCorrectionOf(10L);

        TraceSnapshot snapshot = new TraceSnapshot();
        snapshot.setTraceCode("trace-1");
        snapshot.setSpuId(1L);
        snapshot.setCurrentStatus("INIT");
        snapshot.setLastHash("hash-0");
        when(traceSnapshotMapper.selectById("trace-1")).thenReturn(snapshot);

        TraceLifecycleLog originalLog = new TraceLifecycleLog();
        originalLog.setId(10L);
        originalLog.setTraceCode("trace-other");
        when(traceLifecycleLogMapper.selectById(10L)).thenReturn(originalLog);

        assertThatThrownBy(() -> service.execute(request, "tester"))
                .isInstanceOf(BizException.class)
                .extracting(ex -> ((BizException) ex).getCode())
                .isEqualTo(BizCode.PARAM_ERROR);

        verify(traceLifecycleLogMapper, never()).insert(any(TraceLifecycleLog.class));
        verify(traceSnapshotMapper, never()).updateById(any(TraceSnapshot.class));
    }

    @Test
    void execute_shouldAppendLogAndUpdateSnapshotForNormalScan() {
        TraceLogFactory logFactory = new TraceLogFactory(signatureUtil);
        TraceScanTransactionService service = new TraceScanTransactionService(
                traceSnapshotMapper,
                traceLifecycleLogMapper,
                traceScanIdempotencyMapper,
                logFactory,
                traceTransitionPolicy,
                traceCodeStatusService
        );

        ScanTraceRequest request = new ScanTraceRequest();
        request.setTraceCode("trace-1");
        request.setActionType(ActionType.INBOUND);
        request.setFromNode("factory-A");
        request.setToNode("warehouse-B");
        request.setProvince("guangdong");
        request.setCity("shenzhen");
        request.setRemark("  Quality checked  ");

        TraceSnapshot snapshot = new TraceSnapshot();
        snapshot.setTraceCode("trace-1");
        snapshot.setSpuId(1L);
        snapshot.setCurrentStatus("INIT");
        snapshot.setCurrentNode("factory-A");
        snapshot.setCurrentOwner("factory-A");
        snapshot.setLastHash("hash-0");
        snapshot.setVersion(0);
        when(traceSnapshotMapper.selectById("trace-1")).thenReturn(snapshot);
        when(signatureUtil.getKeyId()).thenReturn("default");
        when(signatureUtil.getKeyVersion()).thenReturn(1);
        when(signatureUtil.sign(any())).thenReturn("signed-scan");
        doAnswer(invocation -> {
            TraceLifecycleLog log = invocation.getArgument(0);
            log.setId(456L);
            return 1;
        }).when(traceLifecycleLogMapper).insert(any(TraceLifecycleLog.class));
        when(traceSnapshotMapper.updateById(any(TraceSnapshot.class))).thenReturn(1);

        service.execute(request, "scanner");

        ArgumentCaptor<TraceLifecycleLog> logCaptor = ArgumentCaptor.forClass(TraceLifecycleLog.class);
        verify(traceLifecycleLogMapper).insert(logCaptor.capture());
        assertThat(logCaptor.getValue().getActionType()).isEqualTo(ActionType.INBOUND.getCode());
        assertThat(logCaptor.getValue().getFromNode()).isEqualTo("factory-A");
        assertThat(logCaptor.getValue().getToNode()).isEqualTo("warehouse-B");
        assertThat(logCaptor.getValue().getRemark()).isEqualTo("Quality checked");
        assertThat(logCaptor.getValue().getSignature()).isEqualTo("signed-scan");
        assertThat(logCaptor.getValue().getSignatureKeyId()).isEqualTo("default");
        assertThat(logCaptor.getValue().getSignatureKeyVersion()).isEqualTo(1);

        ArgumentCaptor<String> signatureDataCaptor = ArgumentCaptor.forClass(String.class);
        verify(signatureUtil).sign(signatureDataCaptor.capture());
        assertThat(signatureDataCaptor.getValue()).contains("remark=Quality checked");
        assertThat(signatureDataCaptor.getValue()).contains("operator=scanner");

        ArgumentCaptor<TraceSnapshot> snapshotCaptor = ArgumentCaptor.forClass(TraceSnapshot.class);
        verify(traceSnapshotMapper).updateById(snapshotCaptor.capture());
        TraceSnapshot updatedSnapshot = snapshotCaptor.getValue();
        assertThat(updatedSnapshot.getCurrentStatus()).isEqualTo("IN_STOCK");
        assertThat(updatedSnapshot.getCurrentNode()).isEqualTo("warehouse-B");
        assertThat(updatedSnapshot.getCurrentOwner()).isEqualTo("warehouse-B");
        assertThat(updatedSnapshot.getLastLogId()).isEqualTo(456L);
        assertThat(updatedSnapshot.getLastHash()).isNotBlank();
    }

    @Test
    void execute_shouldMoveInStockTraceToInTransitForOutboundTaskScan() {
        TraceLogFactory logFactory = new TraceLogFactory(signatureUtil);
        TraceScanTransactionService service = new TraceScanTransactionService(
                traceSnapshotMapper,
                traceLifecycleLogMapper,
                traceScanIdempotencyMapper,
                logFactory,
                traceTransitionPolicy,
                traceCodeStatusService
        );

        ScanTraceRequest request = new ScanTraceRequest();
        request.setTraceCode("trace-1");
        request.setActionType(ActionType.OUTBOUND);
        request.setFromNode("factory-A");
        request.setToNode("warehouse-B");
        request.setProvince("guangdong");
        request.setCity("shenzhen");

        TraceSnapshot snapshot = new TraceSnapshot();
        snapshot.setTraceCode("trace-1");
        snapshot.setSpuId(1L);
        snapshot.setCurrentStatus("IN_STOCK");
        snapshot.setCurrentNode("factory-A");
        snapshot.setCurrentOwner("factory-A");
        snapshot.setLastHash("hash-0");
        snapshot.setVersion(0);
        when(traceSnapshotMapper.selectById("trace-1")).thenReturn(snapshot);
        when(signatureUtil.getKeyId()).thenReturn("default");
        when(signatureUtil.getKeyVersion()).thenReturn(1);
        when(signatureUtil.sign(any())).thenReturn("signed-scan");
        doAnswer(invocation -> {
            TraceLifecycleLog log = invocation.getArgument(0);
            log.setId(789L);
            return 1;
        }).when(traceLifecycleLogMapper).insert(any(TraceLifecycleLog.class));
        when(traceSnapshotMapper.updateById(any(TraceSnapshot.class))).thenReturn(1);

        boolean created = service.executeAndReturnCreated(request, "scanner");

        assertThat(created).isTrue();
        ArgumentCaptor<TraceLifecycleLog> logCaptor = ArgumentCaptor.forClass(TraceLifecycleLog.class);
        verify(traceLifecycleLogMapper).insert(logCaptor.capture());
        assertThat(logCaptor.getValue().getActionType()).isEqualTo(ActionType.OUTBOUND.getCode());
        assertThat(logCaptor.getValue().getFromNode()).isEqualTo("factory-A");
        assertThat(logCaptor.getValue().getToNode()).isEqualTo("warehouse-B");

        ArgumentCaptor<TraceSnapshot> snapshotCaptor = ArgumentCaptor.forClass(TraceSnapshot.class);
        verify(traceSnapshotMapper).updateById(snapshotCaptor.capture());
        TraceSnapshot updatedSnapshot = snapshotCaptor.getValue();
        assertThat(updatedSnapshot.getCurrentStatus()).isEqualTo("IN_TRANSIT");
        assertThat(updatedSnapshot.getCurrentNode()).isEqualTo("warehouse-B");
        assertThat(updatedSnapshot.getCurrentOwner()).isEqualTo("warehouse-B");
        assertThat(updatedSnapshot.getLastLogId()).isEqualTo(789L);
    }

    @Test
    void execute_shouldDefaultFromNodeFromSnapshotCurrentNodeWhenRequestOmitsIt() {
        TraceLogFactory logFactory = new TraceLogFactory(signatureUtil);
        TraceScanTransactionService service = new TraceScanTransactionService(
                traceSnapshotMapper,
                traceLifecycleLogMapper,
                traceScanIdempotencyMapper,
                logFactory,
                traceTransitionPolicy,
                traceCodeStatusService
        );

        ScanTraceRequest request = new ScanTraceRequest();
        request.setTraceCode("trace-1");
        request.setActionType(ActionType.INBOUND);
        request.setToNode("warehouse-B");

        TraceSnapshot snapshot = new TraceSnapshot();
        snapshot.setTraceCode("trace-1");
        snapshot.setSpuId(1L);
        snapshot.setCurrentStatus("INIT");
        snapshot.setCurrentNode("factory-A");
        snapshot.setCurrentOwner("factory-A");
        snapshot.setLastHash("hash-0");
        snapshot.setVersion(0);
        when(traceSnapshotMapper.selectById("trace-1")).thenReturn(snapshot);
        when(signatureUtil.getKeyId()).thenReturn("default");
        when(signatureUtil.getKeyVersion()).thenReturn(1);
        when(signatureUtil.sign(any())).thenReturn("signed-scan");
        doAnswer(invocation -> {
            TraceLifecycleLog log = invocation.getArgument(0);
            log.setId(456L);
            return 1;
        }).when(traceLifecycleLogMapper).insert(any(TraceLifecycleLog.class));
        when(traceSnapshotMapper.updateById(any(TraceSnapshot.class))).thenReturn(1);

        service.execute(request, "scanner");

        ArgumentCaptor<TraceLifecycleLog> logCaptor = ArgumentCaptor.forClass(TraceLifecycleLog.class);
        verify(traceLifecycleLogMapper).insert(logCaptor.capture());
        assertThat(logCaptor.getValue().getFromNode()).isEqualTo("factory-A");
        assertThat(logCaptor.getValue().getToNode()).isEqualTo("warehouse-B");

        ArgumentCaptor<TraceSnapshot> snapshotCaptor = ArgumentCaptor.forClass(TraceSnapshot.class);
        verify(traceSnapshotMapper).updateById(snapshotCaptor.capture());
        assertThat(snapshotCaptor.getValue().getCurrentNode()).isEqualTo("warehouse-B");
    }

    @Test
    void execute_shouldPersistAndCompleteIdempotencyRecordWhenKeyProvided() {
        TraceLogFactory logFactory = new TraceLogFactory(signatureUtil);
        TraceScanTransactionService service = new TraceScanTransactionService(
                traceSnapshotMapper,
                traceLifecycleLogMapper,
                traceScanIdempotencyMapper,
                logFactory,
                traceTransitionPolicy,
                traceCodeStatusService
        );

        ScanTraceRequest request = new ScanTraceRequest();
        request.setTraceCode("trace-1");
        request.setActionType(ActionType.INBOUND);
        request.setToNode("warehouse-B");
        request.setIdempotencyKey("scan-key-001");

        TraceSnapshot snapshot = new TraceSnapshot();
        snapshot.setTraceCode("trace-1");
        snapshot.setSpuId(1L);
        snapshot.setCurrentStatus("INIT");
        snapshot.setCurrentNode("factory-A");
        snapshot.setCurrentOwner("factory-A");
        snapshot.setLastHash("hash-0");
        snapshot.setVersion(0);
        when(traceScanIdempotencyMapper.insert(any(TraceScanIdempotency.class))).thenAnswer(invocation -> {
            TraceScanIdempotency record = invocation.getArgument(0);
            record.setId(99L);
            return 1;
        });
        when(traceSnapshotMapper.selectById("trace-1")).thenReturn(snapshot);
        when(signatureUtil.getKeyId()).thenReturn("default");
        when(signatureUtil.getKeyVersion()).thenReturn(1);
        when(signatureUtil.sign(any())).thenReturn("signed-scan");
        doAnswer(invocation -> {
            TraceLifecycleLog log = invocation.getArgument(0);
            log.setId(456L);
            return 1;
        }).when(traceLifecycleLogMapper).insert(any(TraceLifecycleLog.class));
        when(traceSnapshotMapper.updateById(any(TraceSnapshot.class))).thenReturn(1);

        service.execute(request, "scanner");

        ArgumentCaptor<TraceScanIdempotency> insertCaptor =
                ArgumentCaptor.forClass(TraceScanIdempotency.class);
        verify(traceScanIdempotencyMapper).insert(insertCaptor.capture());
        assertThat(insertCaptor.getValue().getTraceCode()).isEqualTo("trace-1");
        assertThat(insertCaptor.getValue().getActionType()).isEqualTo(ActionType.INBOUND.getCode());
        assertThat(insertCaptor.getValue().getIdempotencyKey()).isEqualTo("scan-key-001");
        assertThat(insertCaptor.getValue().getStatus()).isEqualTo(TraceScanIdempotency.STATUS_SUCCEEDED);

        ArgumentCaptor<TraceScanIdempotency> updateCaptor =
                ArgumentCaptor.forClass(TraceScanIdempotency.class);
        verify(traceScanIdempotencyMapper).updateById(updateCaptor.capture());
        assertThat(updateCaptor.getValue().getId()).isEqualTo(99L);
        assertThat(updateCaptor.getValue().getLifecycleLogId()).isEqualTo(456L);
        assertThat(updateCaptor.getValue().getStatus()).isEqualTo(TraceScanIdempotency.STATUS_SUCCEEDED);
    }

    @Test
    void execute_shouldSkipDuplicateScanWhenIdempotencyRecordAlreadySucceeded() {
        TraceLogFactory logFactory = new TraceLogFactory(signatureUtil);
        TraceScanTransactionService service = new TraceScanTransactionService(
                traceSnapshotMapper,
                traceLifecycleLogMapper,
                traceScanIdempotencyMapper,
                logFactory,
                traceTransitionPolicy,
                traceCodeStatusService
        );

        ScanTraceRequest request = new ScanTraceRequest();
        request.setTraceCode("trace-1");
        request.setActionType(ActionType.INBOUND);
        request.setIdempotencyKey("scan-key-001");

        TraceScanIdempotency existing = new TraceScanIdempotency();
        existing.setId(99L);
        existing.setTraceCode("trace-1");
        existing.setActionType(ActionType.INBOUND.getCode());
        existing.setIdempotencyKey("scan-key-001");
        existing.setLifecycleLogId(456L);
        existing.setStatus(TraceScanIdempotency.STATUS_SUCCEEDED);
        when(traceScanIdempotencyMapper.insert(any(TraceScanIdempotency.class)))
                .thenThrow(new DuplicateKeyException("duplicate"));
        when(traceScanIdempotencyMapper.selectOne(any())).thenReturn(existing);

        boolean created = service.executeAndReturnCreated(request, "scanner");

        assertThat(created).isFalse();
        verify(traceSnapshotMapper, never()).selectById("trace-1");
        verify(traceLifecycleLogMapper, never()).insert(any(TraceLifecycleLog.class));
        verify(traceSnapshotMapper, never()).updateById(any(TraceSnapshot.class));
        verify(traceScanIdempotencyMapper, never()).updateById(any(TraceScanIdempotency.class));
    }

    @Test
    void execute_shouldRejectConcurrentDuplicateWhenIdempotencyRecordIsStillProcessing() {
        TraceLogFactory logFactory = new TraceLogFactory(signatureUtil);
        TraceScanTransactionService service = new TraceScanTransactionService(
                traceSnapshotMapper,
                traceLifecycleLogMapper,
                traceScanIdempotencyMapper,
                logFactory,
                traceTransitionPolicy,
                traceCodeStatusService
        );

        ScanTraceRequest request = new ScanTraceRequest();
        request.setTraceCode("trace-1");
        request.setActionType(ActionType.INBOUND);
        request.setIdempotencyKey("scan-key-001");

        TraceScanIdempotency existing = new TraceScanIdempotency();
        existing.setId(99L);
        existing.setTraceCode("trace-1");
        existing.setActionType(ActionType.INBOUND.getCode());
        existing.setIdempotencyKey("scan-key-001");
        existing.setStatus(TraceScanIdempotency.STATUS_PROCESSING);
        when(traceScanIdempotencyMapper.insert(any(TraceScanIdempotency.class)))
                .thenThrow(new DuplicateKeyException("duplicate"));
        when(traceScanIdempotencyMapper.selectOne(any())).thenReturn(existing);

        assertThatThrownBy(() -> service.execute(request, "scanner"))
                .isInstanceOf(BizException.class)
                .satisfies(ex -> assertThat(((BizException) ex).getCode())
                        .isEqualTo(BizCode.CONCURRENT_CONFLICT));

        verify(traceSnapshotMapper, never()).selectById("trace-1");
        verify(traceLifecycleLogMapper, never()).insert(any(TraceLifecycleLog.class));
        verify(traceSnapshotMapper, never()).updateById(any(TraceSnapshot.class));
    }

    @Test
    void execute_shouldRejectUnactivatedTraceCodeBeforeAnyPersistence() {
        TraceLogFactory logFactory = new TraceLogFactory(signatureUtil);
        TraceScanTransactionService service = new TraceScanTransactionService(
                traceSnapshotMapper,
                traceLifecycleLogMapper,
                traceScanIdempotencyMapper,
                logFactory,
                traceTransitionPolicy,
                traceCodeStatusService
        );

        ScanTraceRequest request = new ScanTraceRequest();
        request.setTraceCode("trace-generated");
        request.setActionType(ActionType.INBOUND);
        request.setIdempotencyKey("scan-key-001");

        doThrow(new BizException(BizCode.INVALID_ACTION_TYPE, "code status GENERATED is not activated"))
                .when(traceCodeStatusService)
                .ensureLifecycleMovementAllowed("trace-generated", ActionType.INBOUND);

        assertThatThrownBy(() -> service.execute(request, "scanner"))
                .isInstanceOf(BizException.class)
                .satisfies(ex -> assertThat(((BizException) ex).getCode())
                        .isEqualTo(BizCode.INVALID_ACTION_TYPE));

        verify(traceScanIdempotencyMapper, never()).insert(any(TraceScanIdempotency.class));
        verify(traceSnapshotMapper, never()).selectById("trace-generated");
        verify(traceLifecycleLogMapper, never()).insert(any(TraceLifecycleLog.class));
        verify(traceSnapshotMapper, never()).updateById(any(TraceSnapshot.class));
    }

    @Test
    void execute_shouldRejectForgedFromNodeWhenItDiffersFromSnapshotCurrentNode() {
        TraceLogFactory logFactory = new TraceLogFactory(signatureUtil);
        TraceScanTransactionService service = new TraceScanTransactionService(
                traceSnapshotMapper,
                traceLifecycleLogMapper,
                traceScanIdempotencyMapper,
                logFactory,
                traceTransitionPolicy,
                traceCodeStatusService
        );

        ScanTraceRequest request = new ScanTraceRequest();
        request.setTraceCode("trace-1");
        request.setActionType(ActionType.INBOUND);
        request.setFromNode("attacker-node");
        request.setToNode("warehouse-B");

        TraceSnapshot snapshot = new TraceSnapshot();
        snapshot.setTraceCode("trace-1");
        snapshot.setSpuId(1L);
        snapshot.setCurrentStatus("INIT");
        snapshot.setCurrentNode("factory-A");
        snapshot.setLastHash("hash-0");
        when(traceSnapshotMapper.selectById("trace-1")).thenReturn(snapshot);

        assertThatThrownBy(() -> service.execute(request, "scanner"))
                .isInstanceOf(BizException.class)
                .satisfies(ex -> {
                    BizException bizException = (BizException) ex;
                    assertThat(bizException.getCode()).isEqualTo(BizCode.PARAM_ERROR);
                    assertThat(bizException.getMessage()).contains("fromNode");
                    assertThat(bizException.getMessage()).contains("expected=factory-A");
                    assertThat(bizException.getMessage()).contains("actual=attacker-node");
                });

        verify(traceLifecycleLogMapper, never()).insert(any(TraceLifecycleLog.class));
        verify(traceSnapshotMapper, never()).updateById(any(TraceSnapshot.class));
    }


    @Test
    void execute_shouldRejectNodeActionWhenUserIsNotBoundToRouteNode() {
        TraceLogFactory logFactory = new TraceLogFactory(signatureUtil);
        TraceScanTransactionService service = new TraceScanTransactionService(
                traceSnapshotMapper,
                traceLifecycleLogMapper,
                traceScanIdempotencyMapper,
                logFactory,
                traceTransitionPolicy,
                traceCodeStatusService,
                traceUserNodeBindingService
        );

        ScanTraceRequest request = new ScanTraceRequest();
        request.setTraceCode("trace-1");
        request.setActionType(ActionType.INBOUND);
        request.setToNode("warehouse-B");
        request.setOperatorUserId(7L);

        TraceSnapshot snapshot = new TraceSnapshot();
        snapshot.setTraceCode("trace-1");
        snapshot.setSpuId(1L);
        snapshot.setCurrentStatus("INIT");
        snapshot.setCurrentNode("factory-A");
        snapshot.setLastHash("hash-0");
        when(traceSnapshotMapper.selectById("trace-1")).thenReturn(snapshot);
        when(traceUserNodeBindingService.authorizeAndResolveRoute(
                7L,
                ActionType.INBOUND,
                "factory-A",
                "warehouse-B"
        )).thenThrow(new BizException(BizCode.FORBIDDEN, "当前用户未绑定目标节点: warehouse-B"));

        assertThatThrownBy(() -> service.execute(request, "scanner"))
                .isInstanceOf(BizException.class)
                .satisfies(ex -> assertThat(((BizException) ex).getCode()).isEqualTo(BizCode.FORBIDDEN));

        verify(traceLifecycleLogMapper, never()).insert(any(TraceLifecycleLog.class));
        verify(traceSnapshotMapper, never()).updateById(any(TraceSnapshot.class));
    }

    @Test
    void execute_shouldRejectUnsafeRouteFieldsBeforePersistence() {
        TraceLogFactory logFactory = new TraceLogFactory(signatureUtil);
        TraceScanTransactionService service = new TraceScanTransactionService(
                traceSnapshotMapper,
                traceLifecycleLogMapper,
                traceScanIdempotencyMapper,
                logFactory,
                traceTransitionPolicy,
                traceCodeStatusService
        );

        ScanTraceRequest request = new ScanTraceRequest();
        request.setTraceCode("trace-1");
        request.setActionType(ActionType.INBOUND);
        request.setToNode("<img src=x onerror=alert(1)>");
        request.setProvince("jiangsu");
        request.setCity("suzhou");

        assertThatThrownBy(() -> service.execute(request, "scanner"))
                .isInstanceOf(BizException.class)
                .satisfies(ex -> {
                    BizException bizException = (BizException) ex;
                    assertThat(bizException.getCode()).isEqualTo(BizCode.PARAM_ERROR);
                    assertThat(bizException.getMessage()).contains("toNode contains unsupported characters");
                });

        verify(traceSnapshotMapper, never()).selectById("trace-1");
        verify(traceLifecycleLogMapper, never()).insert(any(TraceLifecycleLog.class));
        verify(traceSnapshotMapper, never()).updateById(any(TraceSnapshot.class));
    }

    @Test
    void execute_shouldRejectInvalidEventTimeBeforePersistence() {
        TraceLogFactory logFactory = new TraceLogFactory(signatureUtil);
        TraceScanTransactionService service = new TraceScanTransactionService(
                traceSnapshotMapper,
                traceLifecycleLogMapper,
                traceScanIdempotencyMapper,
                logFactory,
                traceTransitionPolicy,
                traceCodeStatusService
        );

        ScanTraceRequest request = new ScanTraceRequest();
        request.setTraceCode("trace-1");
        request.setActionType(ActionType.INBOUND);
        request.setFromNode("factory-A");
        request.setToNode("warehouse-B");
        request.setProvince("guangdong");
        request.setCity("shenzhen");
        request.setEventTime("invalid-time");

        assertThatThrownBy(() -> service.execute(request, "scanner"))
                .isInstanceOf(BizException.class)
                .satisfies(ex -> {
                    BizException bizException = (BizException) ex;
                    assertThat(bizException.getCode()).isEqualTo(BizCode.PARAM_ERROR);
                    assertThat(bizException.getMessage()).contains("eventTime must be ISO-8601");
                });

        verify(traceSnapshotMapper, never()).selectById("trace-1");
        verify(traceLifecycleLogMapper, never()).insert(any(TraceLifecycleLog.class));
        verify(traceSnapshotMapper, never()).updateById(any(TraceSnapshot.class));
    }

    @Test
    void execute_shouldRejectIllegalTransitionBeforePersistence() {
        TraceLogFactory logFactory = new TraceLogFactory(signatureUtil);
        TraceScanTransactionService service = new TraceScanTransactionService(
                traceSnapshotMapper,
                traceLifecycleLogMapper,
                traceScanIdempotencyMapper,
                logFactory,
                traceTransitionPolicy,
                traceCodeStatusService
        );

        ScanTraceRequest request = new ScanTraceRequest();
        request.setTraceCode("trace-1");
        request.setActionType(ActionType.OUTBOUND);
        request.setFromNode("factory-A");
        request.setToNode("warehouse-B");

        TraceSnapshot snapshot = new TraceSnapshot();
        snapshot.setTraceCode("trace-1");
        snapshot.setSpuId(1L);
        snapshot.setCurrentStatus("INIT");
        snapshot.setLastHash("hash-0");
        when(traceSnapshotMapper.selectById("trace-1")).thenReturn(snapshot);

        assertThatThrownBy(() -> service.execute(request, "scanner"))
                .isInstanceOf(BizException.class)
                .satisfies(ex -> {
                    BizException bizException = (BizException) ex;
                    assertThat(bizException.getCode()).isEqualTo(BizCode.INVALID_ACTION_TYPE);
                    assertThat(bizException.getMessage()).contains("currentStatus=INIT");
                    assertThat(bizException.getMessage()).contains("actionType=OUTBOUND");
                });

        verify(traceLifecycleLogMapper, never()).insert(any(TraceLifecycleLog.class));
        verify(traceSnapshotMapper, never()).updateById(any(TraceSnapshot.class));
    }

    @Test
    void execute_shouldRejectNormalScanWhenTraceIsFrozenInExceptionState() {
        TraceLogFactory logFactory = new TraceLogFactory(signatureUtil);
        TraceScanTransactionService service = new TraceScanTransactionService(
                traceSnapshotMapper,
                traceLifecycleLogMapper,
                traceScanIdempotencyMapper,
                logFactory,
                traceTransitionPolicy,
                traceCodeStatusService
        );

        ScanTraceRequest request = new ScanTraceRequest();
        request.setTraceCode("trace-1");
        request.setActionType(ActionType.INBOUND);
        request.setToNode("warehouse-B");

        TraceSnapshot snapshot = new TraceSnapshot();
        snapshot.setTraceCode("trace-1");
        snapshot.setSpuId(1L);
        snapshot.setCurrentStatus("EXCEPTION");
        snapshot.setLastHash("hash-0");
        when(traceSnapshotMapper.selectById("trace-1")).thenReturn(snapshot);

        assertThatThrownBy(() -> service.execute(request, "scanner"))
                .isInstanceOf(BizException.class)
                .satisfies(ex -> assertThat(((BizException) ex).getCode())
                        .isEqualTo(BizCode.INVALID_ACTION_TYPE));

        verify(traceLifecycleLogMapper, never()).insert(any(TraceLifecycleLog.class));
        verify(traceSnapshotMapper, never()).updateById(any(TraceSnapshot.class));
    }
}


