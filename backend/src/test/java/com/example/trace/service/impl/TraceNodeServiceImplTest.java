package com.example.trace.service.impl;

import com.example.trace.common.BizCode;
import com.example.trace.common.BizException;
import com.example.trace.dto.TraceNodeCreateRequest;
import com.example.trace.dto.TraceNodeResponse;
import com.example.trace.dto.TraceNodeUpdateRequest;
import com.example.trace.entity.TraceNode;
import com.example.trace.enums.TraceNodeType;
import com.example.trace.mapper.TraceNodeMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TraceNodeServiceImplTest {

    @Mock
    private TraceNodeMapper traceNodeMapper;

    private TraceNodeServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new TraceNodeServiceImpl(traceNodeMapper);
    }

    @Test
    void createNode_shouldPersistStructuredEnabledNode() {
        TraceNodeCreateRequest request = createRequest();

        TraceNodeResponse response = service.createNode(request);

        ArgumentCaptor<TraceNode> nodeCaptor = ArgumentCaptor.forClass(TraceNode.class);
        verify(traceNodeMapper).insert(nodeCaptor.capture());
        TraceNode node = nodeCaptor.getValue();
        assertThat(response.getNodeCode()).isEqualTo("FACTORY-BJ-001");
        assertThat(node.getNodeCode()).isEqualTo("FACTORY-BJ-001");
        assertThat(node.getNodeName()).isEqualTo("北京工厂");
        assertThat(node.getNodeType()).isEqualTo(TraceNodeType.FACTORY.name());
        assertThat(node.getOrgId()).isEqualTo(3L);
        assertThat(node.getProvince()).isEqualTo("北京市");
        assertThat(node.getCity()).isEqualTo("北京市");
        assertThat(node.getAddress()).isEqualTo("北京亦庄工业园 1 号");
        assertThat(node.getEnabled()).isTrue();
    }

    @Test
    void createNode_shouldRejectDuplicateNodeCode() {
        when(traceNodeMapper.selectByNodeCode("FACTORY-BJ-001")).thenReturn(node(9L, true));

        assertThatThrownBy(() -> service.createNode(createRequest()))
                .isInstanceOf(BizException.class)
                .satisfies(error -> assertThat(((BizException) error).getCode()).isEqualTo(BizCode.CONFLICT));
    }

    @Test
    void updateNode_shouldAllowDisablingNodeAndChangingLocationFields() {
        TraceNode existing = node(9L, true);
        when(traceNodeMapper.selectById(9L)).thenReturn(existing);
        TraceNodeUpdateRequest request = new TraceNodeUpdateRequest();
        request.setNodeName(" 上海仓库 ");
        request.setNodeType(TraceNodeType.WAREHOUSE);
        request.setProvince("上海");
        request.setCity("上海市");
        request.setAddress("浦东仓储园");
        request.setEnabled(false);

        TraceNodeResponse response = service.updateNode(9L, request);

        assertThat(response.getNodeName()).isEqualTo("上海仓库");
        assertThat(response.getNodeType()).isEqualTo(TraceNodeType.WAREHOUSE.name());
        assertThat(response.getProvince()).isEqualTo("上海市");
        assertThat(response.getEnabled()).isFalse();
        verify(traceNodeMapper).updateById(existing);
    }

    @Test
    void getNodeByCode_shouldNormalizeCodeAndReturnNode() {
        TraceNode node = node(9L, true);
        when(traceNodeMapper.selectByNodeCode("FACTORY-BJ-001")).thenReturn(node);

        TraceNodeResponse response = service.getNodeByCode(" factory-bj-001 ");

        assertThat(response.getId()).isEqualTo(9L);
        assertThat(response.getNodeCode()).isEqualTo("FACTORY-BJ-001");
    }

    @Test
    void listNodes_shouldConvertMapperRowsToResponses() {
        when(traceNodeMapper.selectList(any())).thenReturn(List.of(node(1L, true), node(2L, false)));

        List<TraceNodeResponse> nodes = service.listNodes("工厂", TraceNodeType.FACTORY, null);

        assertThat(nodes).hasSize(2);
        assertThat(nodes).extracting(TraceNodeResponse::getEnabled).containsExactly(true, false);
    }

    @Test
    void deleteNode_shouldRejectUnknownNode() {
        when(traceNodeMapper.selectById(404L)).thenReturn(null);

        assertThatThrownBy(() -> service.deleteNode(404L))
                .isInstanceOf(BizException.class)
                .satisfies(error -> assertThat(((BizException) error).getCode()).isEqualTo(BizCode.NOT_FOUND));
    }

    private static TraceNodeCreateRequest createRequest() {
        TraceNodeCreateRequest request = new TraceNodeCreateRequest();
        request.setNodeCode(" factory-bj-001 ");
        request.setNodeName(" 北京工厂 ");
        request.setNodeType(TraceNodeType.FACTORY);
        request.setOrgId(3L);
        request.setProvince("北京");
        request.setCity(" 北京市 ");
        request.setAddress(" 北京亦庄工业园 1 号 ");
        return request;
    }

    private static TraceNode node(Long id, boolean enabled) {
        TraceNode node = new TraceNode();
        node.setId(id);
        node.setNodeCode("FACTORY-BJ-001");
        node.setNodeName("北京工厂");
        node.setNodeType(TraceNodeType.FACTORY.name());
        node.setOrgId(3L);
        node.setProvince("北京市");
        node.setCity("北京市");
        node.setAddress("北京亦庄工业园 1 号");
        node.setEnabled(enabled);
        return node;
    }
}
