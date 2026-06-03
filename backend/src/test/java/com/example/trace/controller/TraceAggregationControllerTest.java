package com.example.trace.controller;

import com.example.trace.common.ApiResponse;
import com.example.trace.dto.TraceAggregationBatchBindRequest;
import com.example.trace.dto.TraceAggregationBatchBindResponse;
import com.example.trace.dto.TraceAggregationBindRequest;
import com.example.trace.dto.TraceAggregationReleaseRequest;
import com.example.trace.dto.TraceAggregationResponse;
import com.example.trace.enums.TraceAggregationRelationType;
import com.example.trace.service.TraceAggregationService;
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
class TraceAggregationControllerTest {

    @Mock
    private TraceAggregationService traceAggregationService;

    @InjectMocks
    private TraceAggregationController controller;

    @Test
    void bindChild_shouldPassOperatorContextAndReturnCreated() {
        TraceAggregationBindRequest request = bindRequest();
        TraceAggregationResponse expected = response(9L, true);
        MockHttpServletRequest httpRequest = request();
        when(traceAggregationService.bindChild(request, 7L, "operator-a")).thenReturn(expected);

        ResponseEntity<ApiResponse<TraceAggregationResponse>> response = controller.bindChild(request, httpRequest);

        assertThat(response.getStatusCode().value()).isEqualTo(201);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getData()).isSameAs(expected);
        verify(traceAggregationService).bindChild(request, 7L, "operator-a");
    }

    @Test
    void bindChildrenBatch_shouldDelegateToServiceAndReturnSummary() {
        TraceAggregationBatchBindRequest request = new TraceAggregationBatchBindRequest();
        request.setParentCode("CARTON-001");
        request.setRelationType(TraceAggregationRelationType.CARTON);
        request.setChildCodes(List.of("TRACE-001", "TRACE-002"));
        TraceAggregationBatchBindResponse expected = TraceAggregationBatchBindResponse.builder()
                .parentCode("CARTON-001")
                .relationType(TraceAggregationRelationType.CARTON)
                .totalRequested(2)
                .successCount(2)
                .failureCount(0)
                .build();
        MockHttpServletRequest httpRequest = request();
        when(traceAggregationService.bindChildrenBatch(request, 7L, "operator-a")).thenReturn(expected);

        ApiResponse<TraceAggregationBatchBindResponse> response = controller.bindChildrenBatch(request, httpRequest);

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(response.getData()).isSameAs(expected);
        verify(traceAggregationService).bindChildrenBatch(request, 7L, "operator-a");
    }

    @Test
    void releaseRelation_shouldDelegateToService() {
        TraceAggregationReleaseRequest request = new TraceAggregationReleaseRequest();
        request.setRemark("拆箱");
        TraceAggregationResponse expected = response(9L, false);
        MockHttpServletRequest httpRequest = request();
        when(traceAggregationService.releaseRelation(9L, request, 7L, "operator-a")).thenReturn(expected);

        assertThat(controller.releaseRelation(9L, request, httpRequest).getData()).isSameAs(expected);
        verify(traceAggregationService).releaseRelation(9L, request, 7L, "operator-a");
    }

    @Test
    void listQueries_shouldDelegateCodesToService() {
        TraceAggregationResponse expected = response(9L, true);
        when(traceAggregationService.listActiveChildren("CARTON-001")).thenReturn(List.of(expected));
        when(traceAggregationService.listActiveParents("TRACE-001")).thenReturn(List.of(expected));
        when(traceAggregationService.listHistoryByParent("CARTON-001")).thenReturn(List.of(expected));
        when(traceAggregationService.listHistoryByChild("TRACE-001")).thenReturn(List.of(expected));

        assertThat(controller.listActiveChildren("CARTON-001").getData()).containsExactly(expected);
        assertThat(controller.listActiveParents("TRACE-001").getData()).containsExactly(expected);
        assertThat(controller.listHistoryByParent("CARTON-001").getData()).containsExactly(expected);
        assertThat(controller.listHistoryByChild("TRACE-001").getData()).containsExactly(expected);
    }

    private static TraceAggregationBindRequest bindRequest() {
        TraceAggregationBindRequest request = new TraceAggregationBindRequest();
        request.setParentCode("CARTON-001");
        request.setChildCode("TRACE-001");
        request.setRelationType(TraceAggregationRelationType.CARTON);
        return request;
    }

    private static MockHttpServletRequest request() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute("userId", 7L);
        request.setAttribute("username", "operator-a");
        return request;
    }

    private static TraceAggregationResponse response(Long id, boolean active) {
        return TraceAggregationResponse.builder()
                .id(id)
                .parentCode("CARTON-001")
                .childCode("TRACE-001")
                .relationType(TraceAggregationRelationType.CARTON)
                .relationTypeLabel("箱码")
                .active(active)
                .build();
    }
}
