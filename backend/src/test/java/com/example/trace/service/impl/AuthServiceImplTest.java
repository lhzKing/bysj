package com.example.trace.service.impl;

import com.example.trace.common.BizCode;
import com.example.trace.common.BizException;
import com.example.trace.dto.ChangePasswordRequest;
import com.example.trace.dto.LoginRequest;
import com.example.trace.dto.LoginResponse;
import com.example.trace.dto.RegisterRequest;
import com.example.trace.dto.UserInfoResponse;
import com.example.trace.entity.SysRole;
import com.example.trace.entity.SysUser;
import com.example.trace.mapper.SysRoleMapper;
import com.example.trace.mapper.SysUserMapper;
import com.example.trace.security.JwtUtil;
import com.example.trace.security.PasswordEncoder;
import com.example.trace.security.PermissionService;
import com.example.trace.security.TokenStore;
import com.example.trace.security.TokenStoreException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock private SysUserMapper sysUserMapper;
    @Mock private SysRoleMapper sysRoleMapper;
    @Mock private JwtUtil jwtUtil;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private TokenStore tokenStore;
    @Mock private PermissionService permissionService;

    @InjectMocks
    private AuthServiceImpl authService;

    @Test
    void login_shouldReturnPermissionCodesFromPermissionService() {
        LoginRequest request = new LoginRequest();
        request.setUsername("alice");
        request.setPassword("secret");
        request.setRememberMe(true);

        SysRole role = new SysRole();
        role.setRoleCode("ADMIN");

        SysUser user = new SysUser();
        user.setId(1L);
        user.setUsername("alice");
        user.setPassword("hashed");
        user.setRoleId(8L);
        user.setRole(role);
        user.setStatus(1);
        user.setTokenVersion(3);

        when(sysUserMapper.selectUserWithRole("alice")).thenReturn(user);
        when(passwordEncoder.matches("secret", "hashed")).thenReturn(true);
        when(jwtUtil.generateToken("alice", "ADMIN", 3, true)).thenReturn("jwt-token");
        when(permissionService.getPermissionCodes(8L)).thenReturn(Set.of("trace:view", "trace:scan"));

        LoginResponse response = authService.login(request);

        assertThat(response.getToken()).isEqualTo("jwt-token");
        assertThat(response.getUsername()).isEqualTo("alice");
        assertThat(response.getRole()).isEqualTo("ADMIN");
        assertThat(response.getPermissions()).containsExactlyInAnyOrder("trace:view", "trace:scan");
    }

    @Test
    void register_shouldUseConflictStatusWhenUsernameExists() {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("alice");
        request.setPassword("secret123");

        SysUser existed = new SysUser();
        existed.setId(1L);
        existed.setUsername("alice");
        when(sysUserMapper.selectOne(any())).thenReturn(existed);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(BizException.class)
                .satisfies(ex -> {
                    BizException bizException = (BizException) ex;
                    assertThat(bizException.getCode()).isEqualTo(BizCode.USER_EXISTS);
                    assertThat(bizException.getHttpStatus()).isEqualTo(409);
                });
    }

    @Test
    void register_shouldCreateSelfServiceUserViaDefaultRole() {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("new_user");
        request.setPassword("abc123");

        SysRole defaultRole = new SysRole();
        defaultRole.setId(6L);
        defaultRole.setRoleCode("USER");

        when(sysUserMapper.selectOne(any())).thenReturn(null);
        when(sysRoleMapper.selectOne(any())).thenReturn(defaultRole);
        when(passwordEncoder.encode("abc123")).thenReturn("encoded-pass");
        when(jwtUtil.generateToken("new_user", "USER", 0, false)).thenReturn("register-token");

        LoginResponse response = authService.register(request);

        assertThat(response.getToken()).isEqualTo("register-token");
        assertThat(response.getUsername()).isEqualTo("new_user");
        assertThat(response.getRole()).isEqualTo("USER");

        ArgumentCaptor<SysUser> userCaptor = ArgumentCaptor.forClass(SysUser.class);
        verify(sysUserMapper).insert(userCaptor.capture());
        SysUser inserted = userCaptor.getValue();
        assertThat(inserted.getUsername()).isEqualTo("new_user");
        assertThat(inserted.getPassword()).isEqualTo("encoded-pass");
        assertThat(inserted.getRoleId()).isEqualTo(6L);
        assertThat(inserted.getStatus()).isEqualTo(1);
        verifyNoInteractions(tokenStore);
    }

    @Test
    void getCurrentUser_shouldReturnUserInfoAndPermissions() {
        SysRole role = new SysRole();
        role.setRoleCode("ADMIN");
        role.setRoleName("系统管理员");

        SysUser user = new SysUser();
        user.setId(5L);
        user.setUsername("alice");
        user.setRole(role);
        user.setRoleCode("ADMIN");
        user.setStatus(1);
        user.setCreateTime(LocalDateTime.of(2026, 5, 2, 10, 0, 0));

        when(sysUserMapper.selectUserWithRole("alice")).thenReturn(user);
        when(permissionService.getPermissionCodes(8L)).thenReturn(Set.of("user:view", "trace:view"));

        UserInfoResponse response = authService.getCurrentUser("alice", 8L);

        assertThat(response.getUsername()).isEqualTo("alice");
        assertThat(response.getRoleCode()).isEqualTo("ADMIN");
        assertThat(response.getRoleName()).isEqualTo("系统管理员");
        assertThat(response.getPermissions()).containsExactlyInAnyOrder("user:view", "trace:view");
    }

    @Test
    void changePassword_shouldIncrementTokenVersionAndBlacklistCurrentToken() {
        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setOldPassword("old-pass");
        request.setNewPassword("new-pass");

        SysUser user = new SysUser();
        user.setId(5L);
        user.setUsername("alice");
        user.setPassword("old-hash");
        user.setStatus(1);
        user.setTokenVersion(2);

        when(sysUserMapper.selectUserWithRole("alice")).thenReturn(user);
        when(passwordEncoder.matches("old-pass", "old-hash")).thenReturn(true);
        when(passwordEncoder.matches("new-pass", "old-hash")).thenReturn(false);
        when(passwordEncoder.encode("new-pass")).thenReturn("new-hash");

        authService.changePassword("alice", "Bearer token-123", request);

        ArgumentCaptor<SysUser> userCaptor = ArgumentCaptor.forClass(SysUser.class);
        verify(sysUserMapper).updateById(userCaptor.capture());
        SysUser updated = userCaptor.getValue();
        assertThat(updated.getPassword()).isEqualTo("new-hash");
        assertThat(updated.getTokenVersion()).isEqualTo(3);
        verify(tokenStore).addToBlacklist("token-123");
    }

    @Test
    void changePassword_shouldKeepBadRequestForOldPasswordMismatch() {
        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setOldPassword("wrong-pass");
        request.setNewPassword("new-pass");

        SysUser user = new SysUser();
        user.setId(5L);
        user.setUsername("alice");
        user.setPassword("old-hash");
        user.setStatus(1);
        user.setTokenVersion(2);

        when(sysUserMapper.selectUserWithRole("alice")).thenReturn(user);
        when(passwordEncoder.matches("wrong-pass", "old-hash")).thenReturn(false);

        assertThatThrownBy(() -> authService.changePassword("alice", null, request))
                .isInstanceOf(BizException.class)
                .satisfies(ex -> {
                    BizException bizException = (BizException) ex;
                    assertThat(bizException.getCode()).isEqualTo(BizCode.PASSWORD_ERROR);
                    assertThat(bizException.getHttpStatus()).isEqualTo(400);
                });
    }

    @Test
    void logout_shouldExposeBlacklistWriteFailure() {
        doThrow(new TokenStoreException("blacklist-add", "Redis unavailable"))
                .when(tokenStore)
                .addToBlacklist("token-123");

        assertThatThrownBy(() -> authService.logout("Bearer token-123"))
                .isInstanceOf(TokenStoreException.class);
    }

    @Test
    void refreshToken_shouldReturnNewTokenPayload() {
        when(jwtUtil.validateToken("old-token")).thenReturn(true);
        when(tokenStore.isBlacklisted("old-token")).thenReturn(false);
        when(jwtUtil.refreshToken("old-token", true)).thenReturn("new-token");
        when(jwtUtil.getUsernameFromToken("new-token")).thenReturn("alice");
        when(jwtUtil.getRoleFromToken("new-token")).thenReturn("ADMIN");

        LoginResponse response = authService.refreshToken("Bearer old-token", true);

        assertThat(response.getToken()).isEqualTo("new-token");
        assertThat(response.getUsername()).isEqualTo("alice");
        assertThat(response.getRole()).isEqualTo("ADMIN");
        verify(tokenStore).addToBlacklist("old-token");
    }

    @Test
    void refreshToken_shouldExposeBlacklistReadFailure() {
        when(jwtUtil.validateToken("old-token")).thenReturn(true);
        when(tokenStore.isBlacklisted("old-token"))
                .thenThrow(new TokenStoreException("blacklist-check", "Redis unavailable"));

        assertThatThrownBy(() -> authService.refreshToken("Bearer old-token", false))
                .isInstanceOf(TokenStoreException.class);
    }

    @Test
    void refreshToken_shouldExposeBlacklistWriteFailure() {
        when(jwtUtil.validateToken("old-token")).thenReturn(true);
        when(tokenStore.isBlacklisted("old-token")).thenReturn(false);
        when(jwtUtil.refreshToken("old-token", true)).thenReturn("new-token");
        doThrow(new TokenStoreException("blacklist-add", "Redis unavailable"))
                .when(tokenStore)
                .addToBlacklist("old-token");

        assertThatThrownBy(() -> authService.refreshToken("Bearer old-token", true))
                .isInstanceOf(TokenStoreException.class);
    }

    @Test
    void changePassword_shouldExposeBlacklistWriteFailure() {
        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setOldPassword("old-pass");
        request.setNewPassword("new-pass");

        SysUser user = new SysUser();
        user.setId(5L);
        user.setUsername("alice");
        user.setPassword("old-hash");
        user.setStatus(1);
        user.setTokenVersion(2);

        when(sysUserMapper.selectUserWithRole("alice")).thenReturn(user);
        when(passwordEncoder.matches("old-pass", "old-hash")).thenReturn(true);
        when(passwordEncoder.matches("new-pass", "old-hash")).thenReturn(false);
        when(passwordEncoder.encode("new-pass")).thenReturn("new-hash");
        doThrow(new TokenStoreException("blacklist-add", "Redis unavailable"))
                .when(tokenStore)
                .addToBlacklist("token-123");

        assertThatThrownBy(() -> authService.changePassword("alice", "Bearer token-123", request))
                .isInstanceOf(TokenStoreException.class);
    }
}
