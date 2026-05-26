package com.example.trace.service.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.example.trace.common.BizException;
import com.example.trace.config.TraceDemoDataProperties;
import com.example.trace.entity.BasePartSpec;
import com.example.trace.entity.SysRole;
import com.example.trace.entity.SysUser;
import com.example.trace.entity.TraceNode;
import com.example.trace.entity.TraceUserNodeBinding;
import com.example.trace.mapper.BasePartSpecMapper;
import com.example.trace.mapper.SysRoleMapper;
import com.example.trace.mapper.SysUserMapper;
import com.example.trace.mapper.TraceNodeMapper;
import com.example.trace.mapper.TraceUserNodeBindingMapper;
import com.example.trace.security.PasswordEncoder;
import com.example.trace.service.impl.support.DemoMasterDataBlueprint;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TraceMasterDataSeedServiceImplTest {

    @Mock private SysUserMapper sysUserMapper;
    @Mock private SysRoleMapper sysRoleMapper;
    @Mock private TraceNodeMapper traceNodeMapper;
    @Mock private BasePartSpecMapper partSpecMapper;
    @Mock private TraceUserNodeBindingMapper bindingMapper;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private TraceDemoDataProperties traceDemoDataProperties;

    private TraceMasterDataSeedServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new TraceMasterDataSeedServiceImpl(
                sysUserMapper, sysRoleMapper, traceNodeMapper,
                partSpecMapper, bindingMapper, passwordEncoder, traceDemoDataProperties);
    }

    @Test
    void seedMasterData_emptyDatabase_insertsAllUsersNodesAndParts() {
        // Empty database: every selectOne returns null, so every blueprint row is a fresh insert.
        // For bindings, the user/node lookups also return null, so the binding lookup itself never runs.
        when(traceDemoDataProperties.isEnabled()).thenReturn(true);
        SysRole stubRole = new SysRole();
        stubRole.setId(3L);
        stubRole.setRoleCode("PRODUCER");
        when(sysRoleMapper.selectOne(any(Wrapper.class))).thenReturn(stubRole);
        when(sysUserMapper.selectOne(any(Wrapper.class))).thenReturn(null);
        when(traceNodeMapper.selectOne(any(Wrapper.class))).thenReturn(null);
        when(partSpecMapper.selectOne(any(Wrapper.class))).thenReturn(null);
        when(passwordEncoder.encode(anyString())).thenReturn("BCRYPT_HASH");
        // Insert assigns id back onto the entity (MyBatis-Plus behavior).
        AtomicLong userIdSeq = new AtomicLong(1);
        AtomicLong nodeIdSeq = new AtomicLong(1);
        AtomicLong partIdSeq = new AtomicLong(1);
        when(sysUserMapper.insert(any(SysUser.class))).thenAnswer(inv -> {
            ((SysUser) inv.getArgument(0)).setId(userIdSeq.getAndIncrement());
            return 1;
        });
        when(traceNodeMapper.insert(any(TraceNode.class))).thenAnswer(inv -> {
            ((TraceNode) inv.getArgument(0)).setId(nodeIdSeq.getAndIncrement());
            return 1;
        });
        when(partSpecMapper.insert(any(BasePartSpec.class))).thenAnswer(inv -> {
            ((BasePartSpec) inv.getArgument(0)).setId(partIdSeq.getAndIncrement());
            return 1;
        });

        Map<String, Object> result = service.seedMasterData("superadmin", "SUPER_ADMIN");

        // Insert call counts must match the blueprint sizes exactly.
        verify(sysUserMapper, times(DemoMasterDataBlueprint.DEMO_USERS.size())).insert(any(SysUser.class));
        verify(traceNodeMapper, times(DemoMasterDataBlueprint.DEMO_NODES.size())).insert(any(TraceNode.class));
        verify(partSpecMapper, times(DemoMasterDataBlueprint.DEMO_PARTS.size())).insert(any(BasePartSpec.class));
        verify(passwordEncoder, times(DemoMasterDataBlueprint.DEMO_USERS.size())).encode(anyString());

        // Result structure has the expected keys
        assertThat(result).containsOnlyKeys("demoUsers", "traceNodes", "partSpecs", "userNodeBindings");
        @SuppressWarnings("unchecked")
        Map<String, Integer> users = (Map<String, Integer>) result.get("demoUsers");
        assertThat(users.get("inserted")).isEqualTo(DemoMasterDataBlueprint.DEMO_USERS.size());
        assertThat(users.get("skipped")).isZero();
    }

    @Test
    void seedMasterData_secondCall_skipsEveryRow() {
        when(traceDemoDataProperties.isEnabled()).thenReturn(true);

        SysUser existingUser = new SysUser();
        existingUser.setId(1L);
        existingUser.setUsername("any-user");
        TraceNode existingNode = new TraceNode();
        existingNode.setId(1L);
        existingNode.setNodeCode("any-node");
        BasePartSpec existingPart = new BasePartSpec();
        existingPart.setId(1L);
        existingPart.setPartCode("any-part");
        TraceUserNodeBinding existingBinding = new TraceUserNodeBinding();
        existingBinding.setId(1L);

        when(sysUserMapper.selectOne(any(Wrapper.class))).thenReturn(existingUser);
        when(traceNodeMapper.selectOne(any(Wrapper.class))).thenReturn(existingNode);
        when(partSpecMapper.selectOne(any(Wrapper.class))).thenReturn(existingPart);
        when(bindingMapper.selectOne(any(Wrapper.class))).thenReturn(existingBinding);

        Map<String, Object> result = service.seedMasterData("superadmin", "SUPER_ADMIN");

        @SuppressWarnings("unchecked")
        Map<String, Integer> users = (Map<String, Integer>) result.get("demoUsers");
        @SuppressWarnings("unchecked")
        Map<String, Integer> nodes = (Map<String, Integer>) result.get("traceNodes");
        @SuppressWarnings("unchecked")
        Map<String, Integer> parts = (Map<String, Integer>) result.get("partSpecs");
        @SuppressWarnings("unchecked")
        Map<String, Integer> bindings = (Map<String, Integer>) result.get("userNodeBindings");

        assertThat(users.get("inserted")).isZero();
        assertThat(users.get("skipped")).isEqualTo(DemoMasterDataBlueprint.DEMO_USERS.size());
        assertThat(nodes.get("inserted")).isZero();
        assertThat(nodes.get("skipped")).isEqualTo(DemoMasterDataBlueprint.DEMO_NODES.size());
        assertThat(parts.get("inserted")).isZero();
        assertThat(parts.get("skipped")).isEqualTo(DemoMasterDataBlueprint.DEMO_PARTS.size());
        assertThat(bindings.get("inserted")).isZero();
        assertThat(bindings.get("skipped")).isEqualTo(DemoMasterDataBlueprint.DEMO_BINDINGS.size());

        // No insertions at all on the second call
        verify(sysUserMapper, never()).insert(any(SysUser.class));
        verify(traceNodeMapper, never()).insert(any(TraceNode.class));
        verify(partSpecMapper, never()).insert(any(BasePartSpec.class));
        verify(bindingMapper, never()).insert(any(TraceUserNodeBinding.class));
        verify(passwordEncoder, never()).encode(anyString());
    }

    @Test
    void seedMasterData_missingDefaultRole_failsFast() {
        when(traceDemoDataProperties.isEnabled()).thenReturn(true);
        when(sysUserMapper.selectOne(any(Wrapper.class))).thenReturn(null);
        when(sysRoleMapper.selectOne(any(Wrapper.class))).thenReturn(null); // role lookup misses

        assertThatThrownBy(() -> service.seedMasterData("superadmin", "SUPER_ADMIN"))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("sys_role 中缺少 role_code");
    }

    @Test
    void seedMasterData_rejectedWhenEndpointDisabled() {
        when(traceDemoDataProperties.isEnabled()).thenReturn(false);

        assertThatThrownBy(() -> service.seedMasterData("superadmin", "SUPER_ADMIN"))
                .isInstanceOf(BizException.class)
                .hasMessage("当前环境已禁用示例数据管理接口");

        verifyNoInteractions(sysUserMapper, traceNodeMapper, partSpecMapper, bindingMapper, passwordEncoder);
    }
}
