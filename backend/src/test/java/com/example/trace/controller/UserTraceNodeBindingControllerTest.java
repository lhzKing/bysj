package com.example.trace.controller;

import com.example.trace.dto.TraceUserNodeBindingResponse;
import com.example.trace.dto.TraceUserNodeBindingUpdateRequest;
import com.example.trace.service.TraceUserNodeBindingService;
import com.example.trace.service.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserTraceNodeBindingControllerTest {

    @Mock
    private UserService userService;
    @Mock
    private TraceUserNodeBindingService traceUserNodeBindingService;

    @InjectMocks
    private UserController userController;

    @Test
    void listMyTraceNodeBindings_shouldUseCurrentRequestUserId() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute("userId", 7L);
        List<TraceUserNodeBindingResponse> expected = List.of(response(91L, 7L, 2L));
        when(traceUserNodeBindingService.listUserBindings(7L)).thenReturn(expected);

        assertThat(userController.listMyTraceNodeBindings(request).getData()).isSameAs(expected);
        verify(traceUserNodeBindingService).listUserBindings(7L);
    }

    @Test
    void replaceUserTraceNodeBindings_shouldDelegateToBindingService() {
        TraceUserNodeBindingUpdateRequest request = new TraceUserNodeBindingUpdateRequest();
        request.setNodeIds(List.of(1L, 2L));
        request.setDefaultNodeId(2L);
        List<TraceUserNodeBindingResponse> expected = List.of(response(91L, 7L, 2L));
        when(traceUserNodeBindingService.replaceUserBindings(7L, request)).thenReturn(expected);

        assertThat(userController.replaceUserTraceNodeBindings(7L, request).getData()).isSameAs(expected);
        verify(traceUserNodeBindingService).replaceUserBindings(7L, request);
    }

    private static TraceUserNodeBindingResponse response(Long bindingId, Long userId, Long nodeId) {
        return TraceUserNodeBindingResponse.builder()
                .bindingId(bindingId)
                .userId(userId)
                .nodeId(nodeId)
                .nodeCode("WAREHOUSE-SH")
                .nodeName("上海仓库")
                .defaultNode(true)
                .build();
    }
}
