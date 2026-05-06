package com.example.trace.security;

import com.example.trace.entity.SysPermission;
import com.example.trace.entity.SysRole;
import com.example.trace.mapper.SysPermissionMapper;
import com.example.trace.mapper.SysRoleMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PermissionServiceTest {

    @Mock
    private SysPermissionMapper permissionMapper;
    @Mock
    private SysRoleMapper roleMapper;

    @Test
    void getPermissionCodes_shouldApplyTransitiveInheritance() {
        PermissionService service = new PermissionService(permissionMapper, roleMapper);

        SysPermission permission = new SysPermission();
        permission.setPermCode("trace:inbound");
        when(permissionMapper.selectByRoleId(1L)).thenReturn(List.of(permission));

        assertThat(service.getPermissionCodes(1L))
                .contains("trace:inbound", "trace:view")
                .doesNotContain("trace:scan");
    }

    @Test
    void getPermissionCodes_shouldUseCacheUntilCleared() {
        PermissionService service = new PermissionService(permissionMapper, roleMapper);

        SysPermission permission = new SysPermission();
        permission.setPermCode("user:manage");
        when(permissionMapper.selectByRoleId(2L)).thenReturn(List.of(permission));

        Set<String> first = service.getPermissionCodes(2L);
        Set<String> second = service.getPermissionCodes(2L);

        assertThat(first).contains("user:manage", "user:view");
        assertThat(second).contains("user:manage", "user:view");
        verify(permissionMapper, times(1)).selectByRoleId(2L);

        service.clearCache(2L);
        service.getPermissionCodes(2L);
        verify(permissionMapper, times(2)).selectByRoleId(2L);
    }

    @Test
    void hasPermission_shouldSupportMatchAnyAndMatchAll() {
        PermissionService service = new PermissionService(permissionMapper, roleMapper);

        SysPermission permission = new SysPermission();
        permission.setPermCode("trace:outbound");
        when(permissionMapper.selectByRoleId(3L)).thenReturn(List.of(permission));

        assertThat(service.hasPermission(3L, new String[]{"trace:scan", "part:view"}, false)).isFalse();
        assertThat(service.hasPermission(3L, new String[]{"trace:outbound", "trace:view"}, true)).isTrue();
        assertThat(service.hasPermission(3L, new String[]{"trace:outbound", "part:view"}, true)).isFalse();
        assertThat(service.hasPermission(3L, new String[]{"part:view", "role:view"}, false)).isFalse();
    }

    @Test
    void hasApiPermission_shouldMatchMethodAndWildcardPathAndUseCache() {
        PermissionService service = new PermissionService(permissionMapper, roleMapper);

        SysPermission permission = new SysPermission();
        permission.setApiMethod("GET");
        permission.setApiPattern("/api/users/*");
        when(permissionMapper.selectByRoleId(4L)).thenReturn(List.of(permission));

        assertThat(service.hasApiPermission(4L, "GET", "/api/users/12")).isTrue();
        assertThat(service.hasApiPermission(4L, "GET", "/api/users/34")).isTrue();
        assertThat(service.hasApiPermission(4L, "POST", "/api/users/12")).isFalse();
        assertThat(service.hasApiPermission(4L, "GET", "/api/roles/12")).isFalse();
        verify(permissionMapper, times(1)).selectByRoleId(4L);
    }

    @Test
    void clearCache_shouldClearAllRoleCaches() {
        PermissionService service = new PermissionService(permissionMapper, roleMapper);

        SysPermission codePermission = new SysPermission();
        codePermission.setPermCode("role:manage");
        codePermission.setApiMethod("GET");
        codePermission.setApiPattern("/api/roles");
        when(permissionMapper.selectByRoleId(5L)).thenReturn(List.of(codePermission));

        service.getPermissionCodes(5L);
        service.hasApiPermission(5L, "GET", "/api/roles");
        verify(permissionMapper, times(2)).selectByRoleId(5L);

        service.clearCache();
        service.getPermissionCodes(5L);
        service.hasApiPermission(5L, "GET", "/api/roles");
        verify(permissionMapper, times(4)).selectByRoleId(5L);
    }

    @Test
    void getRoleIdByCode_shouldReturnResolvedRoleIdOrNull() {
        PermissionService service = new PermissionService(permissionMapper, roleMapper);

        SysRole role = new SysRole();
        role.setId(8L);
        role.setRoleCode("ADMIN");
        when(roleMapper.selectOne(any())).thenReturn(role).thenReturn(null);

        assertThat(service.getRoleIdByCode("ADMIN")).isEqualTo(8L);
        assertThat(service.getRoleIdByCode("UNKNOWN")).isNull();
        assertThat(service.getRoleIdByCode(null)).isNull();
        verify(roleMapper, times(2)).selectOne(any());
    }

    @Test
    void hasPermission_shouldAllowBlankRequirement() {
        PermissionService service = new PermissionService(permissionMapper, roleMapper);

        assertThat(service.hasPermission(9L, "")).isTrue();
        assertThat(service.hasPermission(9L, (String[]) null, false)).isTrue();
        assertThat(service.hasPermission(9L, new String[0], true)).isTrue();
        verifyNoInteractions(permissionMapper);
    }
}
