package com.example.trace.service.impl;

import com.example.trace.common.BizCode;
import com.example.trace.common.BizException;
import com.example.trace.entity.BasePartSpec;
import com.example.trace.mapper.BasePartSpecMapper;
import com.example.trace.mapper.TraceLifecycleLogMapper;
import com.example.trace.mapper.TraceCodeMapper;
import com.example.trace.mapper.TraceSnapshotMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PartServiceImplTest {

    @Mock
    private BasePartSpecMapper partMapper;
    @Mock
    private TraceSnapshotMapper traceSnapshotMapper;
    @Mock
    private TraceLifecycleLogMapper traceLifecycleLogMapper;
    @Mock
    private TraceCodeMapper traceCodeMapper;

    private PartServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new PartServiceImpl(partMapper, traceSnapshotMapper, traceLifecycleLogMapper, traceCodeMapper);
    }

    @Test
    void deletePart_shouldRejectWhenPartIsReferencedBySnapshot() {
        when(partMapper.selectById(1L)).thenReturn(part(1L, "SPU-001"));
        when(traceSnapshotMapper.selectReferencedSpuIds(List.of(1L))).thenReturn(List.of(1L));
        when(traceLifecycleLogMapper.selectReferencedSpuIds(List.of(1L))).thenReturn(List.of());
        when(traceCodeMapper.selectReferencedSpuIds(List.of(1L))).thenReturn(List.of());

        assertThatThrownBy(() -> service.deletePart(1L))
                .isInstanceOf(BizException.class)
                .satisfies(error -> {
                    BizException exception = (BizException) error;
                    assertThat(exception.getCode()).isEqualTo(BizCode.CONFLICT);
                    assertThat(exception.getHttpStatus()).isEqualTo(409);
                    assertThat(exception.getMessage()).contains("1");
                });

        verify(partMapper, never()).deleteById(1L);
    }

    @Test
    void deletePart_shouldRejectWhenPartIsReferencedByLifecycleLog() {
        when(partMapper.selectById(2L)).thenReturn(part(2L, "SPU-002"));
        when(traceSnapshotMapper.selectReferencedSpuIds(List.of(2L))).thenReturn(List.of());
        when(traceLifecycleLogMapper.selectReferencedSpuIds(List.of(2L))).thenReturn(List.of(2L));
        when(traceCodeMapper.selectReferencedSpuIds(List.of(2L))).thenReturn(List.of());

        assertThatThrownBy(() -> service.deletePart(2L))
                .isInstanceOf(BizException.class)
                .satisfies(error -> {
                    BizException exception = (BizException) error;
                    assertThat(exception.getCode()).isEqualTo(BizCode.CONFLICT);
                    assertThat(exception.getMessage()).contains("2");
                });

        verify(partMapper, never()).deleteById(2L);
    }

    @Test
    void deletePart_shouldDeleteWhenPartIsNotReferenced() {
        when(partMapper.selectById(3L)).thenReturn(part(3L, "SPU-003"));
        when(traceSnapshotMapper.selectReferencedSpuIds(List.of(3L))).thenReturn(List.of());
        when(traceLifecycleLogMapper.selectReferencedSpuIds(List.of(3L))).thenReturn(List.of());
        when(traceCodeMapper.selectReferencedSpuIds(List.of(3L))).thenReturn(List.of());

        service.deletePart(3L);

        verify(partMapper).deleteById(3L);
    }

    @Test
    void batchDelete_shouldRejectReferencedIdsAndSkipDeletion() {
        when(traceSnapshotMapper.selectReferencedSpuIds(List.of(1L, 2L, 3L))).thenReturn(List.of(2L));
        when(traceLifecycleLogMapper.selectReferencedSpuIds(List.of(1L, 2L, 3L))).thenReturn(List.of(3L));
        when(traceCodeMapper.selectReferencedSpuIds(List.of(1L, 2L, 3L))).thenReturn(List.of());

        assertThatThrownBy(() -> service.batchDelete(List.of(3L, 2L, 2L, 1L)))
                .isInstanceOf(BizException.class)
                .satisfies(error -> {
                    BizException exception = (BizException) error;
                    assertThat(exception.getCode()).isEqualTo(BizCode.CONFLICT);
                    assertThat(exception.getMessage()).contains("2").contains("3");
                });

        verify(partMapper, never()).deleteBatchIds(List.of(1L, 2L, 3L));
    }

    @Test
    void batchDelete_shouldDeleteDistinctUnreferencedIds() {
        when(traceSnapshotMapper.selectReferencedSpuIds(List.of(4L, 5L))).thenReturn(List.of());
        when(traceLifecycleLogMapper.selectReferencedSpuIds(List.of(4L, 5L))).thenReturn(List.of());
        when(traceCodeMapper.selectReferencedSpuIds(List.of(4L, 5L))).thenReturn(List.of());
        when(partMapper.deleteBatchIds(List.of(4L, 5L))).thenReturn(2);

        int deleted = service.batchDelete(List.of(5L, 4L, 5L));

        assertThat(deleted).isEqualTo(2);
        ArgumentCaptor<List<Long>> captor = ArgumentCaptor.forClass(List.class);
        verify(partMapper).deleteBatchIds(captor.capture());
        assertThat(captor.getValue()).containsExactly(4L, 5L);
    }

    private static BasePartSpec part(Long id, String partCode) {
        BasePartSpec part = new BasePartSpec();
        part.setId(id);
        part.setPartCode(partCode);
        part.setPartName("Part " + id);
        part.setPartType("type");
        return part;
    }
}

