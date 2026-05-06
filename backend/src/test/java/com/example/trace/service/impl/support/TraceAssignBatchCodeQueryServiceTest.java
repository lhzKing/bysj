package com.example.trace.service.impl.support;

import com.example.trace.common.BizCode;
import com.example.trace.common.BizException;
import com.example.trace.dto.TraceAssignBatchCodeResponse;
import com.example.trace.entity.TraceAssignBatch;
import com.example.trace.entity.TraceCode;
import com.example.trace.mapper.TraceAssignBatchMapper;
import com.example.trace.mapper.TraceCodeMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TraceAssignBatchCodeQueryServiceTest {

    @Mock
    private TraceAssignBatchMapper traceAssignBatchMapper;
    @Mock
    private TraceCodeMapper traceCodeMapper;

    private TraceAssignBatchCodeQueryService service;

    @BeforeEach
    void setUp() {
        service = new TraceAssignBatchCodeQueryService(traceAssignBatchMapper, traceCodeMapper);
    }

    @Test
    void listCodes_shouldReturnBatchCodesOrderedByMapper() {
        TraceAssignBatch batch = new TraceAssignBatch();
        batch.setId(9L);
        when(traceAssignBatchMapper.selectById(9L)).thenReturn(batch);

        TraceCode code = new TraceCode();
        code.setBatchId(9L);
        code.setTraceCode("TRACE-001");
        code.setSpuId(1L);
        code.setSerialNo(1);
        code.setQrPayload("TRACE-001");
        code.setCodeStatus("GENERATED");
        code.setPrintCount(0);
        when(traceCodeMapper.selectByBatchId(9L)).thenReturn(List.of(code));

        List<TraceAssignBatchCodeResponse> responses = service.listCodes(9L);

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).getBatchId()).isEqualTo(9L);
        assertThat(responses.get(0).getTraceCode()).isEqualTo("TRACE-001");
        assertThat(responses.get(0).getCodeStatus()).isEqualTo("GENERATED");
        verify(traceCodeMapper).selectByBatchId(9L);
    }

    @Test
    void listCodes_shouldRejectUnknownBatch() {
        when(traceAssignBatchMapper.selectById(404L)).thenReturn(null);

        assertThatThrownBy(() -> service.listCodes(404L))
                .isInstanceOf(BizException.class)
                .satisfies(error -> {
                    BizException exception = (BizException) error;
                    assertThat(exception.getCode()).isEqualTo(BizCode.NOT_FOUND);
                    assertThat(exception.getMessage()).contains("赋码批次不存在");
                });
    }
}
