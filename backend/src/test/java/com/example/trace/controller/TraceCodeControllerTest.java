package com.example.trace.controller;

import com.example.trace.common.ApiResponse;
import com.example.trace.dto.TraceCodeActivateRequest;
import com.example.trace.dto.TraceCodeActivateResponse;
import com.example.trace.enums.ActionType;
import com.example.trace.service.TraceService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TraceCodeControllerTest {

    @Mock
    private TraceService traceService;

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
}
