package com.example.trace.controller;

import com.example.trace.common.ApiResponse;
import com.example.trace.dto.TraceNodeCreateRequest;
import com.example.trace.dto.TraceNodeResponse;
import com.example.trace.dto.TraceNodeUpdateRequest;
import com.example.trace.enums.TraceNodeType;
import com.example.trace.service.TraceNodeService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TraceNodeControllerTest {

    @Mock
    private TraceNodeService traceNodeService;

    @InjectMocks
    private TraceNodeController controller;

    @Test
    void listNodes_shouldDelegateFiltersToService() {
        List<TraceNodeResponse> serviceResponse = List.of(node(1L, "FACTORY-BJ-001", true));
        when(traceNodeService.listNodes("北京", TraceNodeType.FACTORY, true)).thenReturn(serviceResponse);

        ApiResponse<List<TraceNodeResponse>> response =
                controller.listNodes("北京", TraceNodeType.FACTORY, true);

        assertThat(response.getData()).isSameAs(serviceResponse);
        verify(traceNodeService).listNodes("北京", TraceNodeType.FACTORY, true);
    }

    @Test
    void listSelectableNodes_shouldReturnOnlySelectableNodesFromService() {
        List<TraceNodeResponse> serviceResponse = List.of(node(2L, "WAREHOUSE-SH-001", true));
        when(traceNodeService.listSelectableNodes()).thenReturn(serviceResponse);

        ApiResponse<List<TraceNodeResponse>> response = controller.listSelectableNodes();

        assertThat(response.getData()).containsExactlyElementsOf(serviceResponse);
        verify(traceNodeService).listSelectableNodes();
    }

    @Test
    void getNodeByIdAndCode_shouldDelegateLookupArguments() {
        TraceNodeResponse byId = node(3L, "FACTORY-BJ-003", true);
        TraceNodeResponse byCode = node(4L, "SERVICE-GZ-001", true);
        when(traceNodeService.getNodeById(3L)).thenReturn(byId);
        when(traceNodeService.getNodeByCode("service-gz-001")).thenReturn(byCode);

        ApiResponse<TraceNodeResponse> idResponse = controller.getNode(3L);
        ApiResponse<TraceNodeResponse> codeResponse = controller.getNodeByCode("service-gz-001");

        assertThat(idResponse.getData()).isSameAs(byId);
        assertThat(codeResponse.getData()).isSameAs(byCode);
        verify(traceNodeService).getNodeById(3L);
        verify(traceNodeService).getNodeByCode("service-gz-001");
    }

    @Test
    void createUpdateDelete_shouldDelegateMutationsToService() {
        TraceNodeCreateRequest createRequest = new TraceNodeCreateRequest();
        createRequest.setNodeCode("FACTORY-BJ-001");
        createRequest.setNodeName("北京工厂");
        createRequest.setNodeType(TraceNodeType.FACTORY);
        createRequest.setProvince("北京市");
        createRequest.setCity("北京市");
        TraceNodeUpdateRequest updateRequest = new TraceNodeUpdateRequest();
        updateRequest.setEnabled(false);
        TraceNodeResponse created = node(5L, "FACTORY-BJ-001", true);
        TraceNodeResponse updated = node(5L, "FACTORY-BJ-001", false);
        when(traceNodeService.createNode(createRequest)).thenReturn(created);
        when(traceNodeService.updateNode(5L, updateRequest)).thenReturn(updated);

        ApiResponse<TraceNodeResponse> createResponse = controller.createNode(createRequest);
        ApiResponse<TraceNodeResponse> updateResponse = controller.updateNode(5L, updateRequest);
        ApiResponse<Void> deleteResponse = controller.deleteNode(5L);

        assertThat(createResponse.getData()).isSameAs(created);
        assertThat(updateResponse.getData()).isSameAs(updated);
        assertThat(deleteResponse.getData()).isNull();
        verify(traceNodeService).createNode(createRequest);
        verify(traceNodeService).updateNode(5L, updateRequest);
        verify(traceNodeService).deleteNode(5L);
    }

    private static TraceNodeResponse node(Long id, String nodeCode, boolean enabled) {
        return TraceNodeResponse.builder()
                .id(id)
                .nodeCode(nodeCode)
                .nodeName("节点-" + id)
                .nodeType(TraceNodeType.FACTORY.name())
                .province("北京市")
                .city("北京市")
                .enabled(enabled)
                .build();
    }
}
