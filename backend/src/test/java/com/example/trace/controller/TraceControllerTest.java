package com.example.trace.controller;

import com.example.trace.common.BizCode;
import com.example.trace.common.BizException;
import com.example.trace.common.ApiResponse;
import com.example.trace.dto.ProduceAssignRequest;
import com.example.trace.dto.ProduceAssignResponse;
import com.example.trace.dto.ScanTraceRequest;
import com.example.trace.dto.TraceAvailableActionsResponse;
import com.example.trace.dto.TraceCodeLabelActionRequest;
import com.example.trace.dto.TraceCodeLabelActionResponse;
import com.example.trace.dto.TraceDetailResponse;
import com.example.trace.entity.TraceLifecycleLog;
import com.example.trace.entity.TraceSnapshot;
import com.example.trace.enums.ActionType;
import com.example.trace.service.TraceService;
import com.example.trace.service.policy.TraceActionPermissionPolicy;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TraceControllerTest {

    @Mock
    private TraceService traceService;
    @Mock
    private TraceActionPermissionPolicy traceActionPermissionPolicy;

    @InjectMocks
    private TraceController traceController;

    @Test
    void createTraces_shouldReturnBatchOrientedAssignmentResponse() {
        ProduceAssignRequest request = new ProduceAssignRequest();
        request.setSpuId(1L);
        request.setQuantity(2);
        MockHttpServletRequest httpRequest = requestWithRoleId(3L);
        ProduceAssignResponse serviceResponse = new ProduceAssignResponse(
                9L,
                "ASSIGN-009",
                2,
                2,
                List.of("TRACE-1", "TRACE-2"),
                "GENERATED",
                false,
                null
        );
        when(traceService.produceAssign(request, "operator-a")).thenReturn(serviceResponse);

        ResponseEntity<ApiResponse<ProduceAssignResponse>> response =
                traceController.createTraces(request, httpRequest);

        assertThat(response.getStatusCode().value()).isEqualTo(201);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getData()).isSameAs(serviceResponse);
        assertThat(response.getBody().getData().getBatchId()).isEqualTo(9L);
        assertThat(response.getBody().getData().getBatchNo()).isEqualTo("ASSIGN-009");
        verify(traceService).produceAssign(request, "operator-a");
    }

    @Test
    void createEvent_shouldAllowInboundWhenRoleHasSpecificInboundPermission() {
        ScanTraceRequest request = scanRequest(ActionType.INBOUND);
        MockHttpServletRequest httpRequest = requestWithRoleId(4L);
        when(traceActionPermissionPolicy.canExecute(4L, ActionType.INBOUND)).thenReturn(true);

        traceController.createEvent("TRACE-001", request, httpRequest);

        ArgumentCaptor<ScanTraceRequest> requestCaptor = ArgumentCaptor.forClass(ScanTraceRequest.class);
        verify(traceService).scan(requestCaptor.capture(), eq("operator-a"));
        assertThat(requestCaptor.getValue().getTraceCode()).isEqualTo("TRACE-001");
        assertThat(requestCaptor.getValue().getOperatorUserId()).isEqualTo(77L);
        verify(traceActionPermissionPolicy).canExecute(4L, ActionType.INBOUND);
    }

    @Test
    void printCode_shouldReturnLabelActionResponse() {
        TraceCodeLabelActionRequest request = new TraceCodeLabelActionRequest();
        request.setRemark("首打");
        MockHttpServletRequest httpRequest = requestWithRoleId(3L);
        TraceCodeLabelActionResponse serviceResponse = TraceCodeLabelActionResponse.builder()
                .traceCode("TRACE-PRINT")
                .actionType(ActionType.PRINT_CODE)
                .codeStatus("PRINTED")
                .printCount(1)
                .build();
        when(traceService.printCode("TRACE-PRINT", request, "operator-a")).thenReturn(serviceResponse);

        ResponseEntity<ApiResponse<TraceCodeLabelActionResponse>> response =
                traceController.printCode("TRACE-PRINT", request, httpRequest);

        assertThat(response.getStatusCode().value()).isEqualTo(201);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getData()).isSameAs(serviceResponse);
        verify(traceService).printCode("TRACE-PRINT", request, "operator-a");
    }

    @Test
    void reprintAndVoidCode_shouldDelegateToService() {
        TraceCodeLabelActionRequest request = new TraceCodeLabelActionRequest();
        request.setRemark("标签损坏");
        MockHttpServletRequest httpRequest = requestWithRoleId(3L);
        TraceCodeLabelActionResponse reprintResponse = TraceCodeLabelActionResponse.builder()
                .traceCode("TRACE-LABEL")
                .actionType(ActionType.REPRINT_CODE)
                .codeStatus("ACTIVATED")
                .printCount(2)
                .build();
        TraceCodeLabelActionResponse voidResponse = TraceCodeLabelActionResponse.builder()
                .traceCode("TRACE-LABEL")
                .actionType(ActionType.VOID_CODE)
                .codeStatus("VOIDED")
                .printCount(1)
                .build();
        when(traceService.reprintCode("TRACE-LABEL", request, "operator-a")).thenReturn(reprintResponse);
        when(traceService.voidCode("TRACE-LABEL", request, "operator-a")).thenReturn(voidResponse);

        ResponseEntity<ApiResponse<TraceCodeLabelActionResponse>> reprint =
                traceController.reprintCode("TRACE-LABEL", request, httpRequest);
        ResponseEntity<ApiResponse<TraceCodeLabelActionResponse>> voided =
                traceController.voidCode("TRACE-LABEL", request, httpRequest);

        assertThat(reprint.getBody()).isNotNull();
        assertThat(reprint.getBody().getData()).isSameAs(reprintResponse);
        assertThat(voided.getBody()).isNotNull();
        assertThat(voided.getBody().getData()).isSameAs(voidResponse);
        verify(traceService).reprintCode("TRACE-LABEL", request, "operator-a");
        verify(traceService).voidCode("TRACE-LABEL", request, "operator-a");
    }

    @Test
    void createEvent_shouldAllowOutboundWhenRoleHasSuperScanPermission() {
        ScanTraceRequest request = scanRequest(ActionType.OUTBOUND);
        MockHttpServletRequest httpRequest = requestWithRoleId(2L);
        when(traceActionPermissionPolicy.canExecute(2L, ActionType.OUTBOUND)).thenReturn(true);

        traceController.createEvent("TRACE-002", request, httpRequest);

        verify(traceService).scan(any(ScanTraceRequest.class), eq("operator-a"));
        verify(traceActionPermissionPolicy).canExecute(2L, ActionType.OUTBOUND);
    }

    @Test
    void createEvent_shouldRejectOutboundWhenRoleOnlyHasInboundPermission() {
        ScanTraceRequest request = scanRequest(ActionType.OUTBOUND);
        MockHttpServletRequest httpRequest = requestWithRoleId(4L);
        when(traceActionPermissionPolicy.canExecute(4L, ActionType.OUTBOUND)).thenReturn(false);

        assertThatThrownBy(() -> traceController.createEvent("TRACE-003", request, httpRequest))
                .isInstanceOf(BizException.class)
                .satisfies(error -> {
                    BizException exception = (BizException) error;
                    assertThat(exception.getCode()).isEqualTo(BizCode.FORBIDDEN);
                    assertThat(exception.getMessage()).contains("OUTBOUND");
                });

        verify(traceService, never()).scan(any(ScanTraceRequest.class), eq("operator-a"));
    }

    @Test
    void createEvent_shouldRejectExceptionActionWithoutSuperScanPermission() {
        ScanTraceRequest request = scanRequest(ActionType.EXCEPTION);
        MockHttpServletRequest httpRequest = requestWithRoleId(5L);
        when(traceActionPermissionPolicy.canExecute(5L, ActionType.EXCEPTION)).thenReturn(false);

        assertThatThrownBy(() -> traceController.createEvent("TRACE-004", request, httpRequest))
                .isInstanceOf(BizException.class)
                .satisfies(error -> {
                    BizException exception = (BizException) error;
                    assertThat(exception.getCode()).isEqualTo(BizCode.FORBIDDEN);
                    assertThat(exception.getMessage()).contains("EXCEPTION");
                });

        verify(traceService, never()).scan(any(ScanTraceRequest.class), eq("operator-a"));
    }

    @Test
    void availableActions_shouldDelegateTraceCodeAndRoleIdToService() {
        MockHttpServletRequest httpRequest = requestWithRoleId(4L);
        TraceAvailableActionsResponse serviceResponse = TraceAvailableActionsResponse.builder()
                .traceCode("TRACE-005")
                .recommendedAction(ActionType.INBOUND)
                .build();
        when(traceService.availableActions("TRACE-005", 4L, 77L)).thenReturn(serviceResponse);

        ResponseEntity<ApiResponse<TraceAvailableActionsResponse>> response =
                traceController.availableActions("TRACE-005", httpRequest);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getData()).isSameAs(serviceResponse);
        verify(traceService).availableActions("TRACE-005", 4L, 77L);
    }

    @Test
    void getTrace_shouldDelegateRequestedViewAndRoleIdToService() {
        MockHttpServletRequest httpRequest = requestWithRoleId(2L);
        TraceSnapshot snapshot = new TraceSnapshot();
        snapshot.setTraceCode("TRACE-006");
        TraceDetailResponse serviceResponse = new TraceDetailResponse(
                snapshot,
                List.of(new TraceLifecycleLog()),
                "audit"
        );
        when(traceService.detail("TRACE-006", "audit", 2L)).thenReturn(serviceResponse);

        ResponseEntity<ApiResponse<TraceDetailResponse>> response =
                traceController.getTrace("TRACE-006", "audit", httpRequest);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getData()).isSameAs(serviceResponse);
        verify(traceService).detail("TRACE-006", "audit", 2L);
    }

    private static ScanTraceRequest scanRequest(ActionType actionType) {
        ScanTraceRequest request = new ScanTraceRequest();
        request.setActionType(actionType);
        request.setFromNode("节点A");
        request.setToNode("节点B");
        return request;
    }

    private static MockHttpServletRequest requestWithRoleId(Long roleId) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute("username", "operator-a");
        request.setAttribute("roleId", roleId);
        request.setAttribute("userId", 77L);
        return request;
    }
}
