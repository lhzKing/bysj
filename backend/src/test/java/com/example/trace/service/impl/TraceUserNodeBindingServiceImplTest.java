package com.example.trace.service.impl;

import com.example.trace.common.BizCode;
import com.example.trace.common.BizException;
import com.example.trace.dto.TraceUserNodeBindingResponse;
import com.example.trace.dto.TraceUserNodeBindingUpdateRequest;
import com.example.trace.entity.SysRole;
import com.example.trace.entity.SysUser;
import com.example.trace.entity.TraceNode;
import com.example.trace.entity.TraceUserNodeBinding;
import com.example.trace.enums.ActionType;
import com.example.trace.mapper.SysRoleMapper;
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
    @Mock
    private SysRoleMapper roleMapper;

    private TraceUserNodeBindingServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new TraceUserNodeBindingServiceImpl(bindingMapper, traceNodeMapper, userMapper, roleMapper);
    }

    @Test
    void replaceUserBindings_shouldReplaceWithUniqueEnabledNodesAndDefaultNode() {
        when(userMapper.selectById(7L)).thenReturn(user(7L));
        when(roleMapper.selectById(4L)).thenReturn(role(4L, "WAREHOUSE"));
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
        when(roleMapper.selectById(4L)).thenReturn(role(4L, "WAREHOUSE"));
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

    @Test
    void replaceUserBindings_shouldRejectUserRoleBecauseItHasNoScanRbac() {
        // USER 角色没有 trace:inbound/outbound/transfer 等扫码权限，绑节点会成为死数据，
        // 因此 service 层在 RBAC 校验之外多加一道角色白名单守门。
        when(userMapper.selectById(99L)).thenReturn(user(99L, 6L)); // 6L = USER role
        when(roleMapper.selectById(6L)).thenReturn(role(6L, "USER"));

        TraceUserNodeBindingUpdateRequest request = new TraceUserNodeBindingUpdateRequest();
        request.setNodeIds(List.of(1L));

        assertThatThrownBy(() -> service.replaceUserBindings(99L, request))
                .isInstanceOf(BizException.class)
                .satisfies(error -> {
                    BizException ex = (BizException) error;
                    assertThat(ex.getCode()).isEqualTo(BizCode.PARAM_ERROR);
                    assertThat(ex.getMessage()).contains("USER").contains("不支持节点绑定");
                });

        // USER 应该在校验阶段就被拦下，不能 touch binding 表
        verify(bindingMapper, org.mockito.Mockito.never()).delete(any());
        verify(bindingMapper, org.mockito.Mockito.never()).insert(any(TraceUserNodeBinding.class));
    }

    @Test
    void replaceUserBindings_shouldAllowAdminAndSuperAdminForRescueScenarios() {
        // ADMIN / SUPER_ADMIN 允许绑节点用于救场（业务角色不可用时管理员临时操作）。
        // 实际安全防护由前端 warning banner + lifecycle_log.operator 审计字段共同承担。
        when(userMapper.selectById(2L)).thenReturn(user(2L, 2L)); // 2L = ADMIN
        when(roleMapper.selectById(2L)).thenReturn(role(2L, "ADMIN"));
        when(traceNodeMapper.selectBatchIds(any(Collection.class)))
                .thenReturn(List.of(node(1L, "FACTORY-BJ", "北京工厂", 10L, true)));
        when(bindingMapper.selectList(any())).thenReturn(List.of(binding(91L, 2L, 1L, 10L, true, true)));

        TraceUserNodeBindingUpdateRequest request = new TraceUserNodeBindingUpdateRequest();
        request.setNodeIds(List.of(1L));

        List<TraceUserNodeBindingResponse> responses = service.replaceUserBindings(2L, request);

        assertThat(responses).hasSize(1);
        verify(bindingMapper).insert(any(TraceUserNodeBinding.class));
    }

    private static SysUser user(Long id) {
        return user(id, 4L); // default to WAREHOUSE role (priority 1, on the binding whitelist)
    }

    private static SysUser user(Long id, Long roleId) {
        SysUser user = new SysUser();
        user.setId(id);
        user.setUsername("operator-" + id);
        user.setRoleId(roleId);
        user.setStatus(1);
        return user;
    }

    private static SysRole role(Long id, String roleCode) {
        SysRole role = new SysRole();
        role.setId(id);
        role.setRoleCode(roleCode);
        role.setRoleName(roleCode);
        return role;
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
