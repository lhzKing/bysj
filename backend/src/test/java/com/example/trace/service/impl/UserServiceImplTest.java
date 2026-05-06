package com.example.trace.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.trace.common.BizCode;
import com.example.trace.common.BizException;
import com.example.trace.dto.PageResponse;
import com.example.trace.dto.UserListRequest;
import com.example.trace.dto.UserUpdateRequest;
import com.example.trace.dto.UserResponse;
import com.example.trace.entity.SysRole;
import com.example.trace.entity.SysUser;
import com.example.trace.mapper.SysRoleMapper;
import com.example.trace.mapper.SysUserMapper;
import com.example.trace.security.PasswordEncoder;
import com.example.trace.service.policy.RolePolicy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collection;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings({"rawtypes", "unchecked"})
class UserServiceImplTest {

    @Mock
    private SysUserMapper userMapper;
    @Mock
    private SysRoleMapper roleMapper;
    @Mock
    private PasswordEncoder passwordEncoder;

    private UserServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new UserServiceImpl(userMapper, roleMapper, passwordEncoder, new RolePolicy());
    }

    @Test
    void listUsers_shouldBatchLoadDeduplicatedRolesWithoutPerUserSelectById_forSuperAdmin() {
        SysUser alice = user(1L, "alice", 11L);
        SysUser bob = user(2L, "bob", 12L);
        SysUser carol = user(3L, "carol", 11L);

        Page<SysUser> page = new Page<>(1, 10);
        page.setRecords(List.of(alice, bob, carol));
        page.setTotal(3);

        SysRole admin = role(11L, "ADMIN", "Administrator");
        SysRole user = role(12L, "USER", "Standard User");

        when(userMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(page);
        when(roleMapper.selectBatchIds(any())).thenReturn(List.of(admin, user));

        PageResponse<UserResponse> response = service.listUsers(new UserListRequest(), "SUPER_ADMIN");

        ArgumentCaptor<Collection> roleIdsCaptor = ArgumentCaptor.forClass(Collection.class);

        assertThat(response.getList()).extracting(UserResponse::getRoleCode).containsExactly("ADMIN", "USER", "ADMIN");
        assertThat(response.getList()).extracting(UserResponse::getRoleName)
            .containsExactly("Administrator", "Standard User", "Administrator");
        verify(roleMapper).selectBatchIds(roleIdsCaptor.capture());
        assertThat(roleIdsCaptor.getValue()).containsExactly(11L, 12L);
        verify(roleMapper, never()).selectList(any(LambdaQueryWrapper.class));
        verify(roleMapper, never()).selectById(anyLong());
    }

    @Test
    void listUsers_shouldLoadExcludedRolesAndBatchVisibleRoleIds_forNonSuperAdmin() {
        SysUser producerA = user(10L, "producer-a", 21L);
        SysUser warehouse = user(11L, "warehouse", 22L);
        SysUser producerB = user(12L, "producer-b", 21L);

        Page<SysUser> page = new Page<>(1, 10);
        page.setRecords(List.of(producerA, warehouse, producerB));
        page.setTotal(3);

        SysRole superAdmin = role(31L, "SUPER_ADMIN", "Super Admin");
        SysRole admin = role(32L, "ADMIN", "Administrator");
        SysRole producer = role(21L, "PRODUCER", "Producer");
        SysRole warehouseRole = role(22L, "WAREHOUSE", "Warehouse");

        when(roleMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(superAdmin, admin));
        when(userMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(page);
        when(roleMapper.selectBatchIds(any())).thenReturn(List.of(producer, warehouseRole));

        PageResponse<UserResponse> response = service.listUsers(new UserListRequest(), "ADMIN");

        ArgumentCaptor<Collection> roleIdsCaptor = ArgumentCaptor.forClass(Collection.class);
        InOrder inOrder = inOrder(roleMapper, userMapper);

        assertThat(response.getList()).extracting(UserResponse::getRoleCode)
            .containsExactly("PRODUCER", "WAREHOUSE", "PRODUCER");
        inOrder.verify(roleMapper).selectList(any(LambdaQueryWrapper.class));
        inOrder.verify(userMapper).selectPage(any(Page.class), any(LambdaQueryWrapper.class));
        inOrder.verify(roleMapper).selectBatchIds(roleIdsCaptor.capture());
        assertThat(roleIdsCaptor.getValue()).containsExactly(21L, 22L);
        verify(roleMapper, never()).selectById(anyLong());
    }

    @Test
    void updateUser_shouldRejectInvalidStatusBeforeLoadingUser() {
        UserUpdateRequest request = new UserUpdateRequest();
        request.setStatus(2);

        assertThatThrownBy(() -> service.updateUser(1L, request, "ADMIN"))
            .isInstanceOf(BizException.class)
            .extracting("code")
            .isEqualTo(BizCode.BAD_REQUEST);

        verifyNoInteractions(userMapper, roleMapper, passwordEncoder);
    }

    @Test
    void toggleUserStatus_shouldRejectInvalidStatusBeforeLoadingUser() {
        assertThatThrownBy(() -> service.toggleUserStatus(1L, 2, "ADMIN"))
            .isInstanceOf(BizException.class)
            .extracting("code")
            .isEqualTo(BizCode.BAD_REQUEST);

        verifyNoInteractions(userMapper, roleMapper, passwordEncoder);
    }

    private static SysUser user(Long id, String username, Long roleId) {
        SysUser user = new SysUser();
        user.setId(id);
        user.setUsername(username);
        user.setRoleId(roleId);
        user.setStatus(1);
        return user;
    }

    private static SysRole role(Long id, String roleCode, String roleName) {
        SysRole role = new SysRole();
        role.setId(id);
        role.setRoleCode(roleCode);
        role.setRoleName(roleName);
        return role;
    }
}
