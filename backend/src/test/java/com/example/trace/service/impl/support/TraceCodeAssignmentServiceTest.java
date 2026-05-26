package com.example.trace.service.impl.support;

import com.example.trace.common.BizCode;
import com.example.trace.common.BizException;
import com.example.trace.config.TraceBatchProperties;
import com.example.trace.dto.ProduceAssignRequest;
import com.example.trace.dto.ProduceAssignResponse;
import com.example.trace.entity.BasePartSpec;
import com.example.trace.entity.TraceAssignBatch;
import com.example.trace.entity.TraceCode;
import com.example.trace.entity.TraceLifecycleLog;
import com.example.trace.entity.TraceSnapshot;
import com.example.trace.enums.ActionType;
import com.example.trace.enums.TraceAssignBatchStatus;
import com.example.trace.mapper.BasePartSpecMapper;
import com.example.trace.mapper.TraceCodeMapper;
import com.example.trace.mapper.TraceLifecycleLogMapper;
import com.example.trace.mapper.TraceSnapshotMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TraceCodeAssignmentServiceTest {

    @Mock
    private BasePartSpecMapper basePartSpecMapper;
    @Mock
    private TraceSnapshotMapper traceSnapshotMapper;
    @Mock
    private TraceLifecycleLogMapper traceLifecycleLogMapper;
    @Mock
    private TraceLogFactory traceLogFactory;
    @Mock
    private TraceCodeMapper traceCodeMapper;
    @Mock
    private TraceAssignBatchService traceAssignBatchService;

    private TraceBatchProperties batchProperties;
    private TraceBatchCommitter batchCommitter;
    private TraceCodeStatusService traceCodeStatusService;
    private TraceCodeAssignmentService service;

    @BeforeEach
    void setUp() {
        batchProperties = new TraceBatchProperties();
        // Real committer wired around the mocks; we exercise the chunk-loop instead of stubbing it.
        // selfProxy points at the same instance so persistPairsChunk is invoked directly (no AOP in unit tests).
        batchCommitter = new TraceBatchCommitter(
                traceLifecycleLogMapper,
                traceSnapshotMapper,
                traceCodeMapper,
                null,
                null,
                null,
                null,
                batchProperties,
                null
        );
        // After construction, replace the @Lazy self-proxy field with `this` via reflection-free trick:
        // we re-instantiate with the real `this` reference now that `batchCommitter` exists.
        batchCommitter = new TraceBatchCommitter(
                traceLifecycleLogMapper,
                traceSnapshotMapper,
                traceCodeMapper,
                null,
                null,
                null,
                null,
                batchProperties,
                batchCommitter
        );
        traceCodeStatusService = new TraceCodeStatusService(traceCodeMapper);
        service = new TraceCodeAssignmentService(
                basePartSpecMapper,
                traceLogFactory,
                batchCommitter,
                traceCodeStatusService,
                traceAssignBatchService,
                new com.example.trace.config.TraceQrProperties()
        );
    }

    @Test
    void produceAssign_shouldRejectQuantityAboveMaximumBeforeDbAccess() {
        ProduceAssignRequest request = validRequest(ProduceAssignRequest.MAX_QUANTITY + 1);

        assertThatThrownBy(() -> service.produceAssign(request, "tester"))
                .isInstanceOf(BizException.class)
                .satisfies(error -> {
                    BizException exception = (BizException) error;
                    assertThat(exception.getCode()).isEqualTo(BizCode.PARAM_ERROR);
                    assertThat(exception.getMessage()).isEqualTo("quantity 必须在 1 到 500 之间");
                });

        verifyNoInteractions(basePartSpecMapper, traceAssignBatchService, traceSnapshotMapper,
                traceLifecycleLogMapper, traceCodeMapper, traceLogFactory);
    }

    @Test
    void produceAssign_shouldAllowQuantityAtMaximum() {
        ProduceAssignRequest request = validRequest(ProduceAssignRequest.MAX_QUANTITY);
        stubBatch(10L, "ASSIGN-001");
        stubLogFactory();

        ProduceAssignResponse response = service.produceAssign(request, "tester");

        assertThat(response.getBatchId()).isEqualTo(10L);
        assertThat(response.getBatchNo()).isEqualTo("ASSIGN-001");
        assertThat(response.getRequestedCount()).isEqualTo(ProduceAssignRequest.MAX_QUANTITY);
        assertThat(response.getGeneratedCount()).isEqualTo(ProduceAssignRequest.MAX_QUANTITY);
        assertThat(response.getBatchStatus()).isEqualTo(TraceAssignBatchStatus.GENERATED.name());
        assertThat(response.isPartialFailure()).isFalse();
        assertThat(response.getWarning()).isNull();
        assertThat(response.getTraceCodes())
                .hasSize(ProduceAssignRequest.MAX_QUANTITY)
                .doesNotHaveDuplicates();
        verify(traceLifecycleLogMapper, times(ProduceAssignRequest.MAX_QUANTITY))
                .insert(any(TraceLifecycleLog.class));
        verify(traceSnapshotMapper, times(ProduceAssignRequest.MAX_QUANTITY))
                .insert(any(TraceSnapshot.class));
        verify(traceCodeMapper, times(ProduceAssignRequest.MAX_QUANTITY))
                .insert(any(TraceCode.class));
        verify(traceAssignBatchService).markGenerating(10L);
        verify(traceAssignBatchService).markGenerationResult(10L, ProduceAssignRequest.MAX_QUANTITY);
    }

    @Test
    void produceAssign_shouldChunkInsertsByConfiguredCommitSize() {
        // T-P1-01 batching contract: a quantity that is not a clean multiple of commit-size
        // must still produce exactly `quantity` total inserts; the last chunk shrinks to fit.
        batchProperties.setCommitSize(20);
        ProduceAssignRequest request = validRequest(50);
        stubBatch(11L, "ASSIGN-002");
        stubLogFactory();

        ProduceAssignResponse response = service.produceAssign(request, "tester");

        assertThat(response.getGeneratedCount()).isEqualTo(50);
        verify(traceLifecycleLogMapper, times(50)).insert(any(TraceLifecycleLog.class));
        verify(traceSnapshotMapper, times(50)).insert(any(TraceSnapshot.class));
        verify(traceCodeMapper, times(50)).insert(any(TraceCode.class));
        verify(traceAssignBatchService).markGenerationResult(11L, 50);
    }

    @Test
    void produceAssign_shouldFillSnapshotLastLogIdAfterLogInsertAssignsPk() {
        // The committer must wire snapshot.lastLogId only after the log INSERT runs, because
        // MyBatis-Plus assigns the auto-generated PK during insert(). This locks that ordering in.
        ProduceAssignRequest request = validRequest(3);
        stubBatch(12L, "ASSIGN-003");
        stubLogFactory();

        service.produceAssign(request, "tester");

        ArgumentCaptor<TraceSnapshot> snapshotCaptor = ArgumentCaptor.forClass(TraceSnapshot.class);
        verify(traceSnapshotMapper, times(3)).insert(snapshotCaptor.capture());
        List<TraceSnapshot> persisted = snapshotCaptor.getAllValues();
        assertThat(persisted).allSatisfy(snapshot ->
                assertThat(snapshot.getLastLogId()).as("snapshot.lastLogId must be populated by the committer")
                        .isNotNull()
                        .isPositive()
        );
    }

    @Test
    void produceAssign_shouldCreateGeneratedTraceCodeStatusRowsWithBatchId() {
        ProduceAssignRequest request = validRequest(3);
        stubBatch(13L, "ASSIGN-004");
        stubLogFactory();

        service.produceAssign(request, "tester");

        ArgumentCaptor<TraceCode> codeCaptor = ArgumentCaptor.forClass(TraceCode.class);
        verify(traceCodeMapper, times(3)).insert(codeCaptor.capture());
        assertThat(codeCaptor.getAllValues())
                .extracting(TraceCode::getSerialNo)
                .containsExactly(1, 2, 3);
        assertThat(codeCaptor.getAllValues())
                .allSatisfy(code -> {
                    assertThat(code.getBatchId()).isEqualTo(13L);
                    assertThat(code.getSpuId()).isEqualTo(1L);
                    assertThat(code.getCurrentSnapshotId()).isEqualTo(code.getTraceCode());
                });
    }

    @Test
    void produceAssign_shouldResolvePartCodeAndCreateAssignmentBatchBeforeGeneratingSingleItemCodes() {
        ProduceAssignRequest request = validRequest(2);
        request.setSpuId(null);
        request.setPartCode("P-001");
        request.setManufacturerNodeId(7L);
        BasePartSpec part = new BasePartSpec();
        part.setId(88L);
        when(basePartSpecMapper.selectByPartCode("P-001")).thenReturn(part);
        TraceAssignBatch batch = new TraceAssignBatch();
        batch.setId(15L);
        batch.setBatchNo("ASSIGN-006");
        batch.setSpuId(88L);
        batch.setQuantityRequested(2);
        when(traceAssignBatchService.createBatch(any(TraceAssignBatchService.CreateCommand.class)))
                .thenReturn(batch);
        stubLogFactory();

        ProduceAssignResponse response = service.produceAssign(request, "producer-a");

        assertThat(response.getBatchId()).isEqualTo(15L);
        assertThat(response.getGeneratedCount()).isEqualTo(2);
        verify(traceAssignBatchService).createBatch(argThat(command ->
                "ASSIGN-REQ".equals(command.batchNo())
                        && "PO-REQ".equals(command.productionOrderNo())
                        && Long.valueOf(88L).equals(command.spuId())
                        && Integer.valueOf(2).equals(command.quantityRequested())
                        && Long.valueOf(7L).equals(command.manufacturerNodeId())
                        && "producer-a".equals(command.operatorUsername())
        ));
        verify(traceAssignBatchService).markGenerating(15L);
        verify(traceAssignBatchService).markGenerationResult(15L, 2);

        ArgumentCaptor<TraceLifecycleLog> logCaptor = ArgumentCaptor.forClass(TraceLifecycleLog.class);
        verify(traceLifecycleLogMapper, times(2)).insert(logCaptor.capture());
        assertThat(logCaptor.getAllValues())
                .extracting(TraceLifecycleLog::getSpuId)
                .containsOnly(88L);

        ArgumentCaptor<TraceSnapshot> snapshotCaptor = ArgumentCaptor.forClass(TraceSnapshot.class);
        verify(traceSnapshotMapper, times(2)).insert(snapshotCaptor.capture());
        assertThat(snapshotCaptor.getAllValues()).allSatisfy(snapshot -> {
            assertThat(snapshot.getSpuId()).isEqualTo(88L);
            assertThat(snapshot.getCurrentNode()).isEqualTo("工厂A");
            assertThat(snapshot.getProvince()).isEqualTo("浙江省");
            assertThat(snapshot.getLastLogId()).isNotNull();
        });
    }

    @Test
    void produceAssign_shouldMarkBatchPartialAndReturnOnlyCommittedCodesWhenLaterChunkFails() {
        batchProperties.setCommitSize(2);
        ProduceAssignRequest request = validRequest(5);
        stubBatch(14L, "ASSIGN-005");
        stubLogCreationOnly();
        when(traceLifecycleLogMapper.insert(any(TraceLifecycleLog.class)))
                .thenAnswer(invocation -> {
                    TraceLifecycleLog logArg = invocation.getArgument(0);
                    logArg.setId(1L);
                    return 1;
                })
                .thenAnswer(invocation -> {
                    TraceLifecycleLog logArg = invocation.getArgument(0);
                    logArg.setId(2L);
                    return 1;
                })
                .thenThrow(new RuntimeException("chunk-2 failed"));

        ProduceAssignResponse response = service.produceAssign(request, "tester");

        assertThat(response.getBatchId()).isEqualTo(14L);
        assertThat(response.getBatchNo()).isEqualTo("ASSIGN-005");
        assertThat(response.getRequestedCount()).isEqualTo(5);
        assertThat(response.getGeneratedCount()).isEqualTo(2);
        assertThat(response.getTraceCodes()).hasSize(2).doesNotHaveDuplicates();
        assertThat(response.getBatchStatus()).isEqualTo(TraceAssignBatchStatus.PARTIAL_FAILED.name());
        assertThat(response.isPartialFailure()).isTrue();
        assertThat(response.getWarning()).contains("部分生成失败");

        verify(traceAssignBatchService).markGenerationResult(14L, 2);
        verify(traceSnapshotMapper, times(2)).insert(any(TraceSnapshot.class));
        verify(traceCodeMapper, times(2)).insert(any(TraceCode.class));
    }

    private static ProduceAssignRequest validRequest(int quantity) {
        ProduceAssignRequest request = new ProduceAssignRequest();
        request.setSpuId(1L);
        request.setQuantity(quantity);
        request.setBatchNo("ASSIGN-REQ");
        request.setProductionOrderNo("PO-REQ");
        request.setManufacturerNode("工厂A");
        request.setProvince("浙江省");
        request.setCity("杭州市");
        return request;
    }

    private void stubBatch(Long batchId, String batchNo) {
        TraceAssignBatch batch = new TraceAssignBatch();
        batch.setId(batchId);
        batch.setBatchNo(batchNo);
        batch.setSpuId(1L);
        batch.setQuantityRequested(ProduceAssignRequest.MAX_QUANTITY);
        when(traceAssignBatchService.createBatch(any(TraceAssignBatchService.CreateCommand.class)))
                .thenReturn(batch);
    }

    private void stubLogFactory() {
        stubLogCreationOnly();
        // Simulate MyBatis-Plus PK assignment on insert(): the committer reads log.getId()
        // immediately after the call. We mutate the same entity in the answer.
        AtomicLong dbIdSequence = new AtomicLong();
        when(traceLifecycleLogMapper.insert(any(TraceLifecycleLog.class))).thenAnswer(invocation -> {
            TraceLifecycleLog logArg = invocation.getArgument(0);
            logArg.setId(dbIdSequence.incrementAndGet());
            return 1;
        });
    }

    private void stubLogCreationOnly() {
        AtomicLong idSequence = new AtomicLong();
        when(traceLogFactory.createLog(
                anyString(),
                any(Long.class),
                any(ActionType.class),
                nullable(String.class),
                nullable(String.class),
                nullable(String.class),
                nullable(String.class),
                nullable(String.class),
                any(LocalDateTime.class),
                any(LocalDateTime.class),
                anyString(),
                nullable(Long.class),
                anyString()
        )).thenAnswer(invocation -> {
            long id = idSequence.incrementAndGet();
            TraceLifecycleLog log = new TraceLifecycleLog();
            log.setTraceCode(invocation.getArgument(0));
            log.setSpuId(invocation.getArgument(1));
            log.setActionType(((ActionType) invocation.getArgument(2)).getCode());
            log.setFromNode(invocation.getArgument(3));
            log.setToNode(invocation.getArgument(4));
            log.setProvince(invocation.getArgument(5));
            log.setCity(invocation.getArgument(6));
            log.setRemark(invocation.getArgument(7));
            log.setEventTime(invocation.getArgument(8));
            log.setCurrentHash("hash-" + id);
            return log;
        });
    }
}
