package com.example.trace.controller;

import com.example.trace.common.ApiResponse;
import com.example.trace.dto.TraceFlowTaskCompleteRequest;
import com.example.trace.dto.TraceFlowTaskCreateRequest;
import com.example.trace.dto.TraceFlowTaskResponse;
import com.example.trace.dto.TraceFlowTaskScanRequest;
import com.example.trace.enums.TraceFlowTaskStatus;
import com.example.trace.enums.TraceFlowTaskType;
import com.example.trace.service.TraceFlowTaskService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TraceFlowTaskControllerTest {

    @Mock
    private TraceFlowTaskService traceFlowTaskService;

    @InjectMocks
    private TraceFlowTaskController controller;

    @Test
    void createTask_shouldPassOperatorContextAndReturnCreated() {
        TraceFlowTaskCreateRequest request = createRequest();
        MockHttpServletRequest httpRequest = request();
        TraceFlowTaskResponse expected = response(9L, TraceFlowTaskStatus.CREATED);
        when(traceFlowTaskService.createTask(request, 7L, "operator-a")).thenReturn(expected);

        ResponseEntity<ApiResponse<TraceFlowTaskResponse>> response = controller.createTask(request, httpRequest);

        assertThat(response.getStatusCode().value()).isEqualTo(201);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getData()).isSameAs(expected);
        verify(traceFlowTaskService).createTask(request, 7L, "operator-a");
    }

    @Test
    void listAndGetTask_shouldDelegateToService() {
        TraceFlowTaskResponse expected = response(9L, TraceFlowTaskStatus.CREATED);
        when(traceFlowTaskService.listTasks(TraceFlowTaskType.OUTBOUND, TraceFlowTaskStatus.CREATED))
                .thenReturn(List.of(expected));
        when(traceFlowTaskService.getTaskById(9L)).thenReturn(expected);
        when(traceFlowTaskService.getTaskByNo("SHIP-001")).thenReturn(expected);

        assertThat(controller.listTasks(TraceFlowTaskType.OUTBOUND, TraceFlowTaskStatus.CREATED).getData())
                .containsExactly(expected);
        assertThat(controller.getTask(9L).getData()).isSameAs(expected);
        assertThat(controller.getTaskByNo("SHIP-001").getData()).isSameAs(expected);
    }

    @Test
    void cancelAndCompleteTask_shouldDelegateToService() {
        TraceFlowTaskResponse cancelled = response(9L, TraceFlowTaskStatus.CANCELLED);
        TraceFlowTaskResponse completed = response(9L, TraceFlowTaskStatus.COMPLETED);
        TraceFlowTaskCompleteRequest request = new TraceFlowTaskCompleteRequest();
        request.setActualQuantity(100);
        when(traceFlowTaskService.cancelTask(9L)).thenReturn(cancelled);
        when(traceFlowTaskService.completeTask(9L, request)).thenReturn(completed);

        assertThat(controller.cancelTask(9L).getData()).isSameAs(cancelled);
        assertThat(controller.completeTask(9L, request).getData()).isSameAs(completed);
        verify(traceFlowTaskService).cancelTask(9L);
        verify(traceFlowTaskService).completeTask(9L, request);
    }

    @Test
    void scanTask_shouldPassOperatorContextAndDelegateToService() {
        TraceFlowTaskScanRequest request = new TraceFlowTaskScanRequest();
        request.setTraceCode("TRACE-001");
        TraceFlowTaskResponse scanned = response(9L, TraceFlowTaskStatus.PROCESSING);
        MockHttpServletRequest httpRequest = request();
        when(traceFlowTaskService.scanTask(9L, request, 7L, "operator-a")).thenReturn(scanned);

        assertThat(controller.scanTask(9L, request, httpRequest).getData()).isSameAs(scanned);

        verify(traceFlowTaskService).scanTask(9L, request, 7L, "operator-a");
    }

    private static TraceFlowTaskCreateRequest createRequest() {
        TraceFlowTaskCreateRequest request = new TraceFlowTaskCreateRequest();
        request.setTaskNo("SHIP-001");
        request.setTaskType(TraceFlowTaskType.OUTBOUND);
        request.setSourceNodeId(1L);
        request.setTargetNodeId(2L);
        request.setExpectedQuantity(100);
        return request;
    }

    private static MockHttpServletRequest request() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute("userId", 7L);
        request.setAttribute("username", "operator-a");
        return request;
    }

    private static TraceFlowTaskResponse response(Long id, TraceFlowTaskStatus status) {
        return TraceFlowTaskResponse.builder()
                .id(id)
                .taskNo("SHIP-001")
                .taskType(TraceFlowTaskType.OUTBOUND)
                .status(status)
                .sourceNodeId(1L)
                .targetNodeId(2L)
                .expectedQuantity(100)
                .actualQuantity(status == TraceFlowTaskStatus.COMPLETED ? 100 : 0)
                .build();
    }
}
