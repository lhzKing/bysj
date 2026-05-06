package com.example.trace.controller;

import com.example.trace.common.ApiResponse;
import com.example.trace.dto.TraceAssignBatchCodeResponse;
import com.example.trace.dto.TraceAssignBatchReconciliationResponse;
import com.example.trace.service.impl.support.TraceAssignBatchCodeQueryService;
import com.example.trace.service.impl.support.TraceAssignBatchReconciliationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TraceAssignBatchControllerTest {

    @Mock
    private TraceAssignBatchReconciliationService reconciliationService;
    @Mock
    private TraceAssignBatchCodeQueryService codeQueryService;

    @InjectMocks
    private TraceAssignBatchController controller;

    @Test
    void getBatchDetail_shouldReturnReconciliationResponse() {
        TraceAssignBatchReconciliationResponse serviceResponse =
                TraceAssignBatchReconciliationResponse.builder()
                        .batchId(9L)
                        .batchNo("ASSIGN-009")
                        .quantityRequested(3)
                        .quantityGenerated(3)
                        .quantityPrinted(3)
                        .quantityActivated(3)
                        .quantityInbound(3)
                        .quantityVoided(0)
                        .consistent(true)
                        .reconciliationStatus(TraceAssignBatchReconciliationService.STATUS_CONSISTENT)
                        .discrepancyReasons(List.of())
                        .build();
        when(reconciliationService.getReconciliation(9L)).thenReturn(serviceResponse);

        ResponseEntity<ApiResponse<TraceAssignBatchReconciliationResponse>> response =
                controller.getBatchDetail(9L);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getData()).isSameAs(serviceResponse);
        verify(reconciliationService).getReconciliation(9L);
    }

    @Test
    void listBatchCodes_shouldReturnGeneratedCodes() {
        TraceAssignBatchCodeResponse code = TraceAssignBatchCodeResponse.builder()
                .batchId(9L)
                .traceCode("TRACE-001")
                .serialNo(1)
                .codeStatus("GENERATED")
                .printCount(0)
                .build();
        when(codeQueryService.listCodes(9L)).thenReturn(List.of(code));

        ResponseEntity<ApiResponse<List<TraceAssignBatchCodeResponse>>> response =
                controller.listBatchCodes(9L);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getData()).containsExactly(code);
        verify(codeQueryService).listCodes(9L);
    }
}
