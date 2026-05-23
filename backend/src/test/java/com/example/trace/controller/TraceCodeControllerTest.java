package com.example.trace.controller;

import com.example.trace.common.ApiResponse;
import com.example.trace.common.BizCode;
import com.example.trace.common.BizException;
import com.example.trace.dto.TraceAssignBatchCodeResponse;
import com.example.trace.dto.TraceCodeActivateRequest;
import com.example.trace.dto.TraceCodeActivateResponse;
import com.example.trace.enums.ActionType;
import com.example.trace.service.TraceService;
import com.example.trace.service.impl.support.TraceAssignBatchCodeQueryService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TraceCodeControllerTest {

    @Mock
    private TraceService traceService;

    @Mock
    private TraceAssignBatchCodeQueryService traceAssignBatchCodeQueryService;

    @InjectMocks
    private TraceCodeController traceCodeController;

    @Test
    void activateCode_shouldReturnCreatedActivationResponse() {
        TraceCodeActivateRequest request = new TraceCodeActivateRequest();
        request.setActivationNode("工厂A");
        request.setDeviceId("SCANNER-01");
        MockHttpServletRequest httpRequest = new MockHttpServletRequest();
        httpRequest.setAttribute("username", "operator-a");
        TraceCodeActivateResponse serviceResponse = TraceCodeActivateResponse.builder()
                .traceCode("TRACE-ACT")
                .actionType(ActionType.ACTIVATE_CODE)
                .codeStatus("ACTIVATED")
                .activationNode("工厂A")
                .deviceId("SCANNER-01")
                .build();
        when(traceService.activateCode("TRACE-ACT", request, "operator-a")).thenReturn(serviceResponse);

        ResponseEntity<ApiResponse<TraceCodeActivateResponse>> response =
                traceCodeController.activateCode("TRACE-ACT", request, httpRequest);

        assertThat(response.getStatusCode().value()).isEqualTo(201);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getData()).isSameAs(serviceResponse);
        verify(traceService).activateCode("TRACE-ACT", request, "operator-a");
    }

    @Test
    void getByTraceCode_shouldReturnQueriedResponse() {
        TraceAssignBatchCodeResponse serviceResponse = TraceAssignBatchCodeResponse.builder()
                .batchId(9L)
                .traceCode("TRACE-QR-1")
                .spuId(1L)
                .serialNo(3)
                .qrPayload("http://localhost/public/traces/TRACE-QR-1")
                .codeStatus("GENERATED")
                .printCount(0)
                .build();
        when(traceAssignBatchCodeQueryService.findByTraceCode("TRACE-QR-1")).thenReturn(serviceResponse);

        ResponseEntity<ApiResponse<TraceAssignBatchCodeResponse>> response =
                traceCodeController.getByTraceCode("TRACE-QR-1");

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getData()).isSameAs(serviceResponse);
        assertThat(response.getBody().getData().getBatchId()).isEqualTo(9L);
        verify(traceAssignBatchCodeQueryService).findByTraceCode("TRACE-QR-1");
    }

    @Test
    void getByTraceCode_shouldSupportHistoricCodeWithoutBatch() {
        // v11 历史回填的码 batch_id 为 NULL，service 返回时 batchId 字段为 null
        TraceAssignBatchCodeResponse serviceResponse = TraceAssignBatchCodeResponse.builder()
                .batchId(null)
                .traceCode("LEGACY-001")
                .spuId(5L)
                .qrPayload("LEGACY-001")
                .codeStatus("ACTIVATED")
                .printCount(0)
                .build();
        when(traceAssignBatchCodeQueryService.findByTraceCode("LEGACY-001")).thenReturn(serviceResponse);

        ResponseEntity<ApiResponse<TraceAssignBatchCodeResponse>> response =
                traceCodeController.getByTraceCode("LEGACY-001");

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody().getData().getBatchId()).isNull();
        assertThat(response.getBody().getData().getTraceCode()).isEqualTo("LEGACY-001");
    }

    @Test
    void getByTraceCode_shouldPropagateNotFoundException() {
        when(traceAssignBatchCodeQueryService.findByTraceCode("MISSING"))
                .thenThrow(new BizException(BizCode.NOT_FOUND, "追溯码不存在: MISSING"));

        assertThatThrownBy(() -> traceCodeController.getByTraceCode("MISSING"))
                .isInstanceOf(BizException.class)
                .satisfies(error -> {
                    BizException exception = (BizException) error;
                    assertThat(exception.getCode()).isEqualTo(BizCode.NOT_FOUND);
                    assertThat(exception.getMessage()).contains("MISSING");
                });
    }
}
