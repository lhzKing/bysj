package com.example.trace.service.impl;

import com.example.trace.common.BizCode;
import com.example.trace.common.BizException;
import com.example.trace.config.TraceBatchProperties;
import com.example.trace.config.TraceDemoDataProperties;
import com.example.trace.entity.BasePartSpec;
import com.example.trace.entity.TraceLifecycleLog;
import com.example.trace.entity.TraceSnapshot;
import com.example.trace.mapper.BasePartSpecMapper;
import com.example.trace.mapper.TraceCodeMapper;
import com.example.trace.mapper.TraceLifecycleLogMapper;
import com.example.trace.mapper.TraceSnapshotMapper;
import com.example.trace.service.impl.support.TraceBatchCommitter;
import com.example.trace.util.SignatureUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TraceDemoDataServiceImplTest {

    @Mock
    private BasePartSpecMapper partSpecMapper;
    @Mock
    private TraceLifecycleLogMapper logMapper;
    @Mock
    private TraceSnapshotMapper snapshotMapper;
    @Mock
    private TraceCodeMapper traceCodeMapper;
    @Mock
    private SignatureUtil signatureUtil;
    @Mock
    private TraceDemoDataProperties traceDemoDataProperties;

    private TraceBatchProperties batchProperties;
    private TraceBatchCommitter batchCommitter;
    private TraceDemoDataServiceImpl service;

    @BeforeEach
    void setUp() {
        when(traceDemoDataProperties.isEnabled()).thenReturn(true);
        batchProperties = new TraceBatchProperties();
        // Two-pass construction so the @Lazy self-proxy points at the real instance.
        batchCommitter = new TraceBatchCommitter(logMapper, snapshotMapper, traceCodeMapper,
                null, null, null, null, batchProperties, null);
        batchCommitter = new TraceBatchCommitter(logMapper, snapshotMapper, traceCodeMapper,
                null, null, null, null, batchProperties, batchCommitter);
        service = new TraceDemoDataServiceImpl(
                partSpecMapper,
                logMapper,
                snapshotMapper,
                traceCodeMapper,
                signatureUtil,
                traceDemoDataProperties,
                batchCommitter
        );
    }

    @Test
    void clearTraceData_shouldDeleteLogsBeforeSnapshotsAndReturnCounts() {
        when(logMapper.selectCount(null)).thenReturn(7L);
        when(snapshotMapper.selectCount(null)).thenReturn(4L);
        when(traceCodeMapper.selectCount(null)).thenReturn(3L);

        Map<String, Object> result = service.clearTraceData("superadmin", "SUPER_ADMIN");

        InOrder inOrder = inOrder(logMapper, snapshotMapper);
        inOrder.verify(logMapper).selectCount(null);
        inOrder.verify(logMapper).delete(null);
        inOrder.verify(snapshotMapper).selectCount(null);
        inOrder.verify(snapshotMapper).delete(null);
        verify(traceCodeMapper).selectCount(null);
        verify(traceCodeMapper).delete(null);
        assertThat(result).containsEntry("deletedLogs", 7L)
                .containsEntry("deletedSnapshots", 4L)
                .containsEntry("deletedTraceCodes", 3L);
    }

    @Test
    void generateSampleData_shouldReturnAggregatedCounts() {
        when(traceDemoDataProperties.getMaxGenerateCount()).thenReturn(500);
        when(partSpecMapper.selectList(null)).thenReturn(List.of(existingSpec(1L, "SPU-VALVE-002")));
        when(snapshotMapper.selectById(anyString())).thenReturn(null);
        when(signatureUtil.getKeyId()).thenReturn("default");
        when(signatureUtil.getKeyVersion()).thenReturn(1);
        when(signatureUtil.sign(anyString())).thenReturn("signed");

        Map<String, Object> result = service.generateSampleData(1, "superadmin", "SUPER_ADMIN");

        assertThat(result.get("traceCodes")).isEqualTo(1);
        assertThat(result.get("partSpecs")).isInstanceOf(Integer.class);
        assertThat(result.get("lifecycleLogs")).isInstanceOf(Integer.class);
        verify(snapshotMapper).insert(any(TraceSnapshot.class));
        verify(logMapper, atLeastOnce()).insert(any(TraceLifecycleLog.class));
    }

    @Test
    void generateSampleData_shouldRejectCountAboveConfiguredMaximum() {
        when(traceDemoDataProperties.getMaxGenerateCount()).thenReturn(5);

        assertThatThrownBy(() -> service.generateSampleData(6, "admin", "ADMIN"))
                .isInstanceOf(BizException.class)
                .satisfies(error -> {
                    BizException exception = (BizException) error;
                    assertThat(exception.getCode()).isEqualTo(BizCode.PARAM_ERROR);
                    assertThat(exception.getMessage()).isEqualTo("count 必须在 1 到 5 之间");
                });

        verifyNoInteractions(partSpecMapper, logMapper, snapshotMapper);
        verify(signatureUtil, never()).sign(anyString());
    }

    @Test
    void clearTraceData_shouldRejectWhenAdminEndpointDisabled() {
        when(traceDemoDataProperties.isEnabled()).thenReturn(false);

        assertThatThrownBy(() -> service.clearTraceData("admin", "ADMIN"))
                .isInstanceOf(BizException.class)
                .satisfies(error -> {
                    BizException exception = (BizException) error;
                    assertThat(exception.getCode()).isEqualTo(BizCode.FORBIDDEN);
                    assertThat(exception.getMessage()).isEqualTo("当前环境已禁用示例数据管理接口");
                });

        verifyNoInteractions(logMapper, snapshotMapper);
    }

    private static BasePartSpec existingSpec(Long id, String partCode) {
        BasePartSpec spec = new BasePartSpec();
        spec.setId(id);
        spec.setPartCode(partCode);
        return spec;
    }
}
