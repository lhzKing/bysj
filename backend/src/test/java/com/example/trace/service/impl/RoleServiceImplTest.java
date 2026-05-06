package com.example.trace.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.trace.common.BizCode;
import com.example.trace.common.BizException;
import com.example.trace.dto.PermissionResponse;
import com.example.trace.dto.RoleUpdateRequest;
import com.example.trace.dto.RoleResponse;
import com.example.trace.entity.SysPermission;
import com.example.trace.entity.SysRole;
import com.example.trace.mapper.SysPermissionMapper;
import com.example.trace.mapper.SysRoleMapper;
import com.example.trace.mapper.SysRolePermissionMapper;
import com.example.trace.mapper.SysUserMapper;
import com.example.trace.security.PermissionService;
import com.example.trace.service.policy.RolePolicy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings({"rawtypes", "unchecked"})
class RoleServiceImplTest {

    @Mock
    private SysRoleMapper roleMapper;
    @Mock
    private SysPermissionMapper permissionMapper;
    @Mock
    private SysRolePermissionMapper rolePermissionMapper;
    @Mock
    private SysUserMapper userMapper;
    @Mock
    private PermissionService permissionService;

    private RoleServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new RoleServiceImpl(
            roleMapper,
            permissionMapper,
            rolePermissionMapper,
            userMapper,
            permissionService,
            new RolePolicy()
        );
    }

    @Test
    void listRoles_shouldReturnPermissionCountWithoutLoadingFullPermissionsPerRole() {
        SysRole admin = role(1L, "ADMIN", "Administrator");
        SysRole user = role(2L, "USER", "Standard User");

        when(roleMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(admin, user));
        when(rolePermissionMapper.countPermissionByRoleIds(any())).thenReturn(List.of(
            new SysRolePermissionMapper.RolePermissionCount(1L, 3),
            new SysRolePermissionMapper.RolePermissionCount(2L, 1)
        ));

        List<RoleResponse> responses = service.listRoles();

        ArgumentCaptor<List<Long>> roleIdsCaptor = ArgumentCaptor.forClass(List.class);
        verify(rolePermissionMapper).countPermissionByRoleIds(roleIdsCaptor.capture());

        assertThat(roleIdsCaptor.getValue()).containsExactly(1L, 2L);
        assertThat(responses).extracting(RoleResponse::getRoleCode).containsExactly("ADMIN", "USER");
        assertThat(responses).extracting(RoleResponse::getPermissionCount).containsExactly(3, 1);
        assertThat(responses).allSatisfy(response -> assertThat(response.getPermissions()).isNull());
        verify(permissionMapper, never()).selectByRoleId(any());
    }

    @Test
    void listRoles_shouldDefaultPermissionCountToZeroWhenGroupedQueryHasNoRowForRole() {
        SysRole admin = role(1L, "ADMIN", "Administrator");
        SysRole guest = role(2L, "GUEST", "Guest");

        when(roleMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(admin, guest));
        when(rolePermissionMapper.countPermissionByRoleIds(any())).thenReturn(List.of(
            new SysRolePermissionMapper.RolePermissionCount(1L, 3)
        ));

        List<RoleResponse> responses = service.listRoles();

        assertThat(responses).extracting(RoleResponse::getRoleCode).containsExactly("ADMIN", "GUEST");
        assertThat(responses).extracting(RoleResponse::getPermissionCount).containsExactly(3, 0);
        assertThat(responses).allSatisfy(response -> assertThat(response.getPermissions()).isNull());
        verify(permissionMapper, never()).selectByRoleId(any());
    }

    @Test
    void getRoleById_shouldStillReturnFullPermissions() {
        SysRole admin = role(1L, "ADMIN", "Administrator");
        SysPermission manageUsers = permission(10L, "user:manage", "Manage Users");
        SysPermission manageRoles = permission(11L, "role:manage", "Manage Roles");

        when(roleMapper.selectById(1L)).thenReturn(admin);
        when(permissionMapper.selectByRoleId(1L)).thenReturn(List.of(manageUsers, manageRoles));

        RoleResponse response = service.getRoleById(1L);

        assertThat(response.getRoleCode()).isEqualTo("ADMIN");
        assertThat(response.getPermissionCount()).isEqualTo(2);
        assertThat(response.getPermissions())
            .extracting(PermissionResponse::getPermCode)
            .containsExactly("user:manage", "role:manage");
        verify(permissionMapper).selectByRoleId(1L);
        verify(rolePermissionMapper, never()).countPermissionByRoleIds(any());
    }

    @Test
    void updateRole_shouldRejectAdminUpdatingSuperAdminRole() {
        SysRole superAdmin = role(1L, "SUPER_ADMIN", "Super Admin");
        RoleUpdateRequest request = new RoleUpdateRequest();
        request.setRoleName("Updated");

        when(roleMapper.selectById(1L)).thenReturn(superAdmin);

        assertThatThrownBy(() -> service.updateRole(1L, request, "ADMIN"))
            .isInstanceOf(BizException.class)
            .extracting("code")
            .isEqualTo(BizCode.FORBIDDEN);

        verify(roleMapper, never()).updateById(any(SysRole.class));
        verifyNoInteractions(permissionService);
    }

    @Test
    void updateRole_shouldRejectSuperAdminUpdatingOwnRoleDefinition() {
        SysRole superAdmin = role(1L, "SUPER_ADMIN", "Super Admin");
        RoleUpdateRequest request = new RoleUpdateRequest();
        request.setRoleName("Updated");

        when(roleMapper.selectById(1L)).thenReturn(superAdmin);

        assertThatThrownBy(() -> service.updateRole(1L, request, "SUPER_ADMIN"))
            .isInstanceOf(BizException.class)
            .extracting("code")
            .isEqualTo(BizCode.FORBIDDEN);

        verify(roleMapper, never()).updateById(any(SysRole.class));
        verifyNoInteractions(permissionService);
    }

    @Test
    void deleteRole_shouldTreatSuperAdminAsSystemRole() {
        SysRole superAdmin = role(1L, "SUPER_ADMIN", "Super Admin");

        when(roleMapper.selectById(1L)).thenReturn(superAdmin);

        assertThatThrownBy(() -> service.deleteRole(1L, "SUPER_ADMIN"))
            .isInstanceOf(BizException.class)
            .extracting("code")
            .isEqualTo(BizCode.FORBIDDEN);

        verify(rolePermissionMapper, never()).deleteByRoleId(any());
        verify(roleMapper, never()).deleteById(1L);
    }

    @Test
    void assignPermissions_shouldRejectAdminModifyingSuperAdminPermissions() {
        SysRole superAdmin = role(1L, "SUPER_ADMIN", "Super Admin");

        when(roleMapper.selectById(1L)).thenReturn(superAdmin);

        assertThatThrownBy(() -> service.assignPermissions(1L, List.of(1L, 2L), "ADMIN"))
            .isInstanceOf(BizException.class)
            .extracting("code")
            .isEqualTo(BizCode.FORBIDDEN);

        verify(permissionMapper, never()).selectBatchIds(any());
        verify(rolePermissionMapper, never()).deleteByRoleId(any());
        verify(rolePermissionMapper, never()).batchInsert(any(), any());
    }

    @Test
    void assignPermissions_shouldRejectSuperAdminModifyingOwnPermissions() {
        SysRole superAdmin = role(1L, "SUPER_ADMIN", "Super Admin");

        when(roleMapper.selectById(1L)).thenReturn(superAdmin);

        assertThatThrownBy(() -> service.assignPermissions(1L, List.of(1L, 2L), "SUPER_ADMIN"))
            .isInstanceOf(BizException.class)
            .extracting("code")
            .isEqualTo(BizCode.FORBIDDEN);

        verify(permissionMapper, never()).selectBatchIds(any());
        verify(rolePermissionMapper, never()).deleteByRoleId(any());
        verify(rolePermissionMapper, never()).batchInsert(any(), any());
    }

    @Test
    void assignPermissions_shouldRejectAdminGrantingProtectedPermissionsToLowerRole() {
        SysRole customRole = role(9L, "AUDITOR", "Auditor");
        SysPermission traceView = permission(3L, "trace:view", "Trace View");
        SysPermission roleManage = permission(8L, "role:manage", "Role Manage");

        when(roleMapper.selectById(9L)).thenReturn(customRole);
        when(permissionMapper.selectBatchIds(List.of(3L, 8L))).thenReturn(List.of(traceView, roleManage));

        assertThatThrownBy(() -> service.assignPermissions(9L, List.of(3L, 8L), "ADMIN"))
            .isInstanceOf(BizException.class)
            .extracting("code")
            .isEqualTo(BizCode.FORBIDDEN);

        verify(rolePermissionMapper, never()).deleteByRoleId(any());
        verify(rolePermissionMapper, never()).batchInsert(any(), any());
        verify(permissionService, never()).clearCache();
    }

    @Test
    void assignPermissions_shouldAllowAdminAssigningBusinessPermissionsToLowerRole() {
        SysRole customRole = role(9L, "AUDITOR", "Auditor");
        SysPermission traceView = permission(3L, "trace:view", "Trace View");
        SysPermission dashboardView = permission(4L, "dashboard:view", "Dashboard View");

        when(roleMapper.selectById(9L)).thenReturn(customRole);
        when(permissionMapper.selectBatchIds(List.of(3L, 4L))).thenReturn(List.of(traceView, dashboardView));
        when(permissionMapper.selectByRoleId(9L)).thenReturn(List.of(traceView, dashboardView));

        RoleResponse response = service.assignPermissions(9L, List.of(3L, 4L), "ADMIN");

        assertThat(response.getRoleCode()).isEqualTo("AUDITOR");
        assertThat(response.getPermissionCount()).isEqualTo(2);
        verify(rolePermissionMapper).deleteByRoleId(9L);
        verify(rolePermissionMapper).batchInsert(9L, List.of(3L, 4L));
        verify(permissionService).clearCache();
    }

    private static SysRole role(Long id, String roleCode, String roleName) {
        SysRole role = new SysRole();
        role.setId(id);
        role.setRoleCode(roleCode);
        role.setRoleName(roleName);
        return role;
    }

    private static SysPermission permission(Long id, String permCode, String permName) {
        SysPermission permission = new SysPermission();
        permission.setId(id);
        permission.setPermCode(permCode);
        permission.setPermName(permName);
        return permission;
    }
}
