package com.example.trace.service.impl;

import com.example.trace.common.BizCode;
import com.example.trace.common.BizException;
import com.example.trace.dto.TraceUserNodeBindingResponse;
import com.example.trace.dto.TraceUserNodeBindingUpdateRequest;
import com.example.trace.entity.SysUser;
import com.example.trace.entity.TraceNode;
import com.example.trace.entity.TraceUserNodeBinding;
import com.example.trace.enums.ActionType;
import com.example.trace.mapper.SysUserMapper;
import com.example.trace.mapper.TraceNodeMapper;
import com.example.trace.mapper.TraceUserNodeBindingMapper;
import com.example.trace.service.TraceUserNodeBindingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collection;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TraceUserNodeBindingServiceImplTest {

    @Mock
    private TraceUserNodeBindingMapper bindingMapper;
    @Mock
    private TraceNodeMapper traceNodeMapper;
    @Mock
    private SysUserMapper userMapper;

    private TraceUserNodeBindingServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new TraceUserNodeBindingServiceImpl(bindingMapper, traceNodeMapper, userMapper);
    }

    @Test
    void replaceUserBindings_shouldReplaceWithUniqueEnabledNodesAndDefaultNode() {
        when(userMapper.selectById(7L)).thenReturn(user(7L));
        when(traceNodeMapper.selectBatchIds(any(Collection.class)))
                .thenReturn(List.of(node(1L, "FACTORY-BJ", "北京工厂", 10L, true),
                        node(2L, "WAREHOUSE-SH", "上海仓库", 20L, true)));
        when(bindingMapper.selectList(any())).thenReturn(List.of(
                binding(91L, 7L, 2L, 20L, true, true),
                binding(90L, 7L, 1L, 10L, false, true)
        ));

        TraceUserNodeBindingUpdateRequest request = new TraceUserNodeBindingUpdateRequest();
        request.setNodeIds(List.of(1L, 2L, 2L));
        request.setDefaultNodeId(2L);

        List<TraceUserNodeBindingResponse> responses = service.replaceUserBindings(7L, request);

        ArgumentCaptor<TraceUserNodeBinding> insertCaptor =
                ArgumentCaptor.forClass(TraceUserNodeBinding.class);
        verify(bindingMapper).delete(any());
        verify(bindingMapper, times(2)).insert(insertCaptor.capture());
        assertThat(insertCaptor.getAllValues())
                .extracting(TraceUserNodeBinding::getNodeId)
                .containsExactly(1L, 2L);
        assertThat(insertCaptor.getAllValues().get(0).getDefaultNode()).isFalse();
        assertThat(insertCaptor.getAllValues().get(1).getDefaultNode()).isTrue();
        assertThat(insertCaptor.getAllValues().get(0).getOrgId()).isEqualTo(10L);
        assertThat(responses).hasSize(2);
        assertThat(responses.get(0).getDefaultNode()).isTrue();
    }

    @Test
    void replaceUserBindings_shouldRejectDisabledNode() {
        when(userMapper.selectById(7L)).thenReturn(user(7L));
        when(traceNodeMapper.selectBatchIds(any(Collection.class)))
                .thenReturn(List.of(node(1L, "WAREHOUSE-X", "停用仓库", 10L, false)));
        TraceUserNodeBindingUpdateRequest request = new TraceUserNodeBindingUpdateRequest();
        request.setNodeIds(List.of(1L));

        assertThatThrownBy(() -> service.replaceUserBindings(7L, request))
                .isInstanceOf(BizException.class)
                .satisfies(error -> assertThat(((BizException) error).getCode()).isEqualTo(BizCode.BAD_REQUEST));
    }

    @Test
    void authorizeInbound_shouldAutoFillDefaultNodeWhenToNodeOmitted() {
        when(bindingMapper.selectList(any())).thenReturn(List.of(binding(91L, 7L, 2L, 20L, true, true)));
        when(traceNodeMapper.selectBatchIds(any(Collection.class)))
                .thenReturn(List.of(node(2L, "WAREHOUSE-SH", "上海仓库", 20L, true)));

        TraceUserNodeBindingService.RouteResolution resolution = service.authorizeAndResolveRoute(
                7L,
                ActionType.INBOUND,
                "北京工厂",
                null
        );

        assertThat(resolution.fromNode()).isEqualTo("北京工厂");
        assertThat(resolution.toNode()).isEqualTo("上海仓库");
        assertThat(resolution.operationNode().getNodeCode()).isEqualTo("WAREHOUSE-SH");
    }

    @Test
    void authorizeOutbound_shouldRejectWhenSourceNodeIsNotBoundToUser() {
        when(bindingMapper.selectList(any())).thenReturn(List.of(binding(90L, 7L, 1L, 10L, true, true)));
        when(traceNodeMapper.selectBatchIds(any(Collection.class)))
                .thenReturn(List.of(node(1L, "FACTORY-BJ", "北京工厂", 10L, true)));

        assertThatThrownBy(() -> service.authorizeAndResolveRoute(
                        7L,
                        ActionType.OUTBOUND,
                        "上海仓库",
                        "广州仓库"
                ))
                .isInstanceOf(BizException.class)
                .satisfies(error -> {
                    BizException exception = (BizException) error;
                    assertThat(exception.getCode()).isEqualTo(BizCode.FORBIDDEN);
                    assertThat(exception.getMessage()).contains("来源节点").contains("上海仓库");
                });
    }

    @Test
    void authorizeRoute_shouldRejectUserWithoutAnyEnabledNodeBinding() {
        when(bindingMapper.selectList(any())).thenReturn(List.of());

        assertThatThrownBy(() -> service.authorizeAndResolveRoute(7L, ActionType.INBOUND, null, "上海仓库"))
                .isInstanceOf(BizException.class)
                .satisfies(error -> assertThat(((BizException) error).getCode()).isEqualTo(BizCode.FORBIDDEN));
    }

    @Test
    void canExecuteActionAtCurrentNode_shouldRequireCurrentNodeBindingForOutbound() {
        when(bindingMapper.selectList(any())).thenReturn(List.of(binding(90L, 7L, 1L, 10L, true, true)));
        when(traceNodeMapper.selectBatchIds(any(Collection.class)))
                .thenReturn(List.of(node(1L, "FACTORY-BJ", "北京工厂", 10L, true)));

        assertThat(service.canExecuteActionAtCurrentNode(7L, ActionType.OUTBOUND, "上海仓库")).isFalse();
        assertThat(service.canExecuteActionAtCurrentNode(7L, ActionType.OUTBOUND, "FACTORY-BJ")).isTrue();
    }

    private static SysUser user(Long id) {
        SysUser user = new SysUser();
        user.setId(id);
        user.setUsername("operator-" + id);
        user.setStatus(1);
        return user;
    }

    private static TraceUserNodeBinding binding(
            Long id,
            Long userId,
            Long nodeId,
            Long orgId,
            boolean defaultNode,
            boolean enabled
    ) {
        TraceUserNodeBinding binding = new TraceUserNodeBinding();
        binding.setId(id);
        binding.setUserId(userId);
        binding.setNodeId(nodeId);
        binding.setOrgId(orgId);
        binding.setDefaultNode(defaultNode);
        binding.setEnabled(enabled);
        return binding;
    }

    private static TraceNode node(Long id, String code, String name, Long orgId, boolean enabled) {
        TraceNode node = new TraceNode();
        node.setId(id);
        node.setNodeCode(code);
        node.setNodeName(name);
        node.setNodeType("WAREHOUSE");
        node.setOrgId(orgId);
        node.setProvince("上海市");
        node.setCity("上海市");
        node.setAddress("浦东仓储园");
        node.setEnabled(enabled);
        return node;
    }
}
