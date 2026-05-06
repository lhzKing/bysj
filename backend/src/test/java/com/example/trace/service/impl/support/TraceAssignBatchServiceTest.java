package com.example.trace.service.impl.support;

import com.example.trace.common.BizCode;
import com.example.trace.common.BizException;
import com.example.trace.entity.BasePartSpec;
import com.example.trace.entity.TraceAssignBatch;
import com.example.trace.entity.TraceNode;
import com.example.trace.enums.TraceAssignBatchStatus;
import com.example.trace.mapper.BasePartSpecMapper;
import com.example.trace.mapper.TraceAssignBatchMapper;
import com.example.trace.mapper.TraceNodeMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TraceAssignBatchServiceTest {

    @Mock
    private TraceAssignBatchMapper traceAssignBatchMapper;
    @Mock
    private BasePartSpecMapper basePartSpecMapper;
    @Mock
    private TraceNodeMapper traceNodeMapper;

    private TraceAssignBatchService service;

    @BeforeEach
    void setUp() {
        service = new TraceAssignBatchService(traceAssignBatchMapper, basePartSpecMapper, traceNodeMapper);
    }

    @Test
    void createBatch_shouldPersistInitialBatchContainer() {
        BasePartSpec spu = new BasePartSpec();
        spu.setId(1L);
        when(basePartSpecMapper.selectById(1L)).thenReturn(spu);
        when(traceNodeMapper.selectById(10L)).thenReturn(enabledNode(10L));

        TraceAssignBatch batch = service.createBatch(new TraceAssignBatchService.CreateCommand(
                " ASSIGN-001 ",
                " PO-20260505-001 ",
                1L,
                100,
                10L,
                7L,
                " producer "
        ));

        ArgumentCaptor<TraceAssignBatch> batchCaptor = ArgumentCaptor.forClass(TraceAssignBatch.class);
        verify(traceAssignBatchMapper).insert(batchCaptor.capture());
        TraceAssignBatch persisted = batchCaptor.getValue();
        assertThat(batch).isSameAs(persisted);
        assertThat(persisted.getBatchNo()).isEqualTo("ASSIGN-001");
        assertThat(persisted.getProductionOrderNo()).isEqualTo("PO-20260505-001");
        assertThat(persisted.getSpuId()).isEqualTo(1L);
        assertThat(persisted.getQuantityRequested()).isEqualTo(100);
        assertThat(persisted.getQuantityGenerated()).isZero();
        assertThat(persisted.getQuantityPrinted()).isZero();
        assertThat(persisted.getQuantityActivated()).isZero();
        assertThat(persisted.getManufacturerNodeId()).isEqualTo(10L);
        assertThat(persisted.getStatus()).isEqualTo(TraceAssignBatchStatus.CREATED.name());
        assertThat(persisted.getOperatorId()).isEqualTo(7L);
        assertThat(persisted.getOperatorUsername()).isEqualTo("producer");
    }

    @Test
    void createBatch_shouldRejectDuplicateBatchNo() {
        BasePartSpec spu = new BasePartSpec();
        spu.setId(1L);
        when(basePartSpecMapper.selectById(1L)).thenReturn(spu);
        when(traceAssignBatchMapper.selectByBatchNo("ASSIGN-001")).thenReturn(new TraceAssignBatch());

        assertThatThrownBy(() -> service.createBatch(new TraceAssignBatchService.CreateCommand(
                "ASSIGN-001",
                null,
                1L,
                10,
                null,
                null,
                "producer"
        )))
                .isInstanceOf(BizException.class)
                .satisfies(error -> assertThat(((BizException) error).getCode()).isEqualTo(BizCode.CONFLICT));
    }

    @Test
    void createBatch_shouldRejectUnknownSpu() {
        when(basePartSpecMapper.selectById(404L)).thenReturn(null);

        assertThatThrownBy(() -> service.createBatch(new TraceAssignBatchService.CreateCommand(
                "ASSIGN-404",
                null,
                404L,
                10,
                null,
                null,
                "producer"
        )))
                .isInstanceOf(BizException.class)
                .satisfies(error -> assertThat(((BizException) error).getCode()).isEqualTo(BizCode.PARAM_ERROR));
    }

    @Test
    void markGenerationResult_shouldSetGeneratedOrPartialStatus() {
        TraceAssignBatch batch = new TraceAssignBatch();
        batch.setId(99L);
        batch.setQuantityRequested(100);
        when(traceAssignBatchMapper.selectById(99L)).thenReturn(batch);

        service.markGenerationResult(99L, 45);

        ArgumentCaptor<TraceAssignBatch> batchCaptor = ArgumentCaptor.forClass(TraceAssignBatch.class);
        verify(traceAssignBatchMapper).updateById(batchCaptor.capture());
        assertThat(batchCaptor.getValue().getQuantityGenerated()).isEqualTo(45);
        assertThat(batchCaptor.getValue().getStatus()).isEqualTo(TraceAssignBatchStatus.PARTIAL_FAILED.name());
    }

    @Test
    void markGenerationResult_shouldRejectGeneratedCountAboveRequestedQuantity() {
        TraceAssignBatch batch = new TraceAssignBatch();
        batch.setId(99L);
        batch.setQuantityRequested(100);
        when(traceAssignBatchMapper.selectById(99L)).thenReturn(batch);

        assertThatThrownBy(() -> service.markGenerationResult(99L, 101))
                .isInstanceOf(BizException.class)
                .satisfies(error -> assertThat(((BizException) error).getCode()).isEqualTo(BizCode.PARAM_ERROR));
    }

    @Test
    void createBatch_shouldRejectDisabledManufacturerNode() {
        BasePartSpec spu = new BasePartSpec();
        spu.setId(1L);
        when(basePartSpecMapper.selectById(1L)).thenReturn(spu);
        TraceNode disabled = enabledNode(10L);
        disabled.setEnabled(false);
        when(traceNodeMapper.selectById(10L)).thenReturn(disabled);

        assertThatThrownBy(() -> service.createBatch(new TraceAssignBatchService.CreateCommand(
                "ASSIGN-NODE",
                null,
                1L,
                10,
                10L,
                null,
                "producer"
        )))
                .isInstanceOf(BizException.class)
                .satisfies(error -> assertThat(((BizException) error).getCode())
                        .isEqualTo(BizCode.INVALID_ACTION_TYPE));
    }

    private static TraceNode enabledNode(Long id) {
        TraceNode node = new TraceNode();
        node.setId(id);
        node.setEnabled(true);
        return node;
    }
}
