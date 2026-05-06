package com.example.trace.service.impl.support;

import com.example.trace.common.BizCode;
import com.example.trace.common.BizException;
import com.example.trace.dto.TraceAssignBatchReconciliationResponse;
import com.example.trace.entity.TraceAssignBatch;
import com.example.trace.enums.TraceAssignBatchStatus;
import com.example.trace.mapper.TraceAssignBatchMapper;
import com.example.trace.mapper.TraceCodeMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TraceAssignBatchReconciliationServiceTest {

    @Mock
    private TraceAssignBatchMapper traceAssignBatchMapper;
    @Mock
    private TraceCodeMapper traceCodeMapper;

    private TraceAssignBatchReconciliationService service;

    @BeforeEach
    void setUp() {
        service = new TraceAssignBatchReconciliationService(traceAssignBatchMapper, traceCodeMapper);
    }

    @Test
    void getReconciliation_shouldReturnConsistentCountsWhenBatchLifecycleIsComplete() {
        TraceAssignBatch batch = batch();
        batch.setQuantityGenerated(3);
        batch.setQuantityPrinted(3);
        batch.setQuantityActivated(3);
        when(traceAssignBatchMapper.selectById(9L)).thenReturn(batch);
        when(traceCodeMapper.countByBatchId(9L)).thenReturn(3);
        when(traceCodeMapper.countPrintedCodesByBatchId(9L)).thenReturn(3);
        when(traceCodeMapper.sumPrintCountByBatchId(9L)).thenReturn(4);
        when(traceCodeMapper.countActivatedCodesByBatchId(9L)).thenReturn(3);
        when(traceCodeMapper.countInboundCodesByBatchId(9L)).thenReturn(3);
        when(traceCodeMapper.countVoidedCodesByBatchId(9L)).thenReturn(0);

        TraceAssignBatchReconciliationResponse response = service.getReconciliation(9L);

        assertThat(response.getBatchId()).isEqualTo(9L);
        assertThat(response.getBatchNo()).isEqualTo("ASSIGN-009");
        assertThat(response.getQuantityRequested()).isEqualTo(3);
        assertThat(response.getQuantityGenerated()).isEqualTo(3);
        assertThat(response.getQuantityPrinted()).isEqualTo(3);
        assertThat(response.getQuantityActivated()).isEqualTo(3);
        assertThat(response.getQuantityInbound()).isEqualTo(3);
        assertThat(response.getQuantityVoided()).isZero();
        assertThat(response.getPrintOperationCount()).isEqualTo(4);
        assertThat(response.getConsistent()).isTrue();
        assertThat(response.getReconciliationStatus())
                .isEqualTo(TraceAssignBatchReconciliationService.STATUS_CONSISTENT);
        assertThat(response.getDiscrepancyReasons()).isEmpty();
    }

    @Test
    void getReconciliation_shouldMarkMismatchWhenCountsDriftOrDoNotCoverPlan() {
        TraceAssignBatch batch = batch();
        batch.setQuantityGenerated(3);
        batch.setQuantityPrinted(0);
        batch.setQuantityActivated(0);
        when(traceAssignBatchMapper.selectById(9L)).thenReturn(batch);
        when(traceCodeMapper.countByBatchId(9L)).thenReturn(3);
        when(traceCodeMapper.countPrintedCodesByBatchId(9L)).thenReturn(2);
        when(traceCodeMapper.sumPrintCountByBatchId(9L)).thenReturn(2);
        when(traceCodeMapper.countActivatedCodesByBatchId(9L)).thenReturn(1);
        when(traceCodeMapper.countInboundCodesByBatchId(9L)).thenReturn(0);
        when(traceCodeMapper.countVoidedCodesByBatchId(9L)).thenReturn(1);

        TraceAssignBatchReconciliationResponse response = service.getReconciliation(9L);

        assertThat(response.getConsistent()).isFalse();
        assertThat(response.getReconciliationStatus())
                .isEqualTo(TraceAssignBatchReconciliationService.STATUS_MISMATCH);
        assertThat(response.getDiscrepancyReasons())
                .anySatisfy(reason -> assertThat(reason).contains("批次记录的打印数量"))
                .anySatisfy(reason -> assertThat(reason).contains("已激活数量 + 作废数量"))
                .anySatisfy(reason -> assertThat(reason).contains("入库数量与激活数量"));
    }

    @Test
    void getReconciliation_shouldRejectUnknownBatch() {
        when(traceAssignBatchMapper.selectById(404L)).thenReturn(null);

        assertThatThrownBy(() -> service.getReconciliation(404L))
                .isInstanceOf(BizException.class)
                .satisfies(error -> {
                    BizException exception = (BizException) error;
                    assertThat(exception.getCode()).isEqualTo(BizCode.NOT_FOUND);
                    assertThat(exception.getMessage()).contains("赋码批次不存在");
                });
    }

    private static TraceAssignBatch batch() {
        TraceAssignBatch batch = new TraceAssignBatch();
        batch.setId(9L);
        batch.setBatchNo("ASSIGN-009");
        batch.setProductionOrderNo("PO-009");
        batch.setSpuId(1L);
        batch.setQuantityRequested(3);
        batch.setStatus(TraceAssignBatchStatus.GENERATED.name());
        return batch;
    }
}
