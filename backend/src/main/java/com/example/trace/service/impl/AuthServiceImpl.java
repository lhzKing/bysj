package com.example.trace.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
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
import com.example.trace.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Set;

/**
 * 认证服务实现。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private static final String BEARER_PREFIX = "Bearer ";
    private static final String DEFAULT_ROLE_CODE = "USER";

    private final SysUserMapper sysUserMapper;
    private final SysRoleMapper sysRoleMapper;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;
    private final TokenStore tokenStore;
    private final PermissionService permissionService;

    @Override
    public LoginResponse login(LoginRequest request) {
        log.debug("Login attempt: username={}, rememberMe={}", request.getUsername(), request.isRememberMe());

        SysUser user = sysUserMapper.selectUserWithRole(request.getUsername());
        if (user == null) {
            log.debug("User not found: {}", request.getUsername());
            throw new BizException(BizCode.PASSWORD_ERROR, "用户名或密码错误");
        }

        if (!user.isEnabled()) {
            log.debug("User disabled: {}", request.getUsername());
            throw new BizException(BizCode.FORBIDDEN, "账号已被禁用，请联系管理员");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            log.debug("Credential mismatch for user: {}", request.getUsername());
            throw new BizException(BizCode.PASSWORD_ERROR, "用户名或密码错误");
        }

        String roleCode = resolveRoleCode(user);
        String token = jwtUtil.generateToken(
                user.getUsername(),
                roleCode,
                user.getTokenVersion(),
                request.isRememberMe()
        );
        log.info("User logged in successfully: {}", user.getUsername());

        Set<String> permissionCodes = permissionService.getPermissionCodes(user.getRoleId());
        return new LoginResponse(token, user.getUsername(), roleCode, new ArrayList<>(permissionCodes));
    }

    @Override
    public LoginResponse register(RegisterRequest request) {
        log.debug("Register attempt: username={}", request.getUsername());

        SysUser existed = sysUserMapper.selectOne(
                new QueryWrapper<SysUser>()
                        .eq("username", request.getUsername())
                        .last("LIMIT 1")
        );
        if (existed != null) {
            throw new BizException(BizCode.USER_EXISTS, "用户名已存在");
        }

        SysRole defaultRole = sysRoleMapper.selectOne(
                new QueryWrapper<SysRole>()
                        .eq("role_code", DEFAULT_ROLE_CODE)
        );
        if (defaultRole == null) {
            log.error("Default role not found: {}", DEFAULT_ROLE_CODE);
            throw new BizException(BizCode.SERVER_ERROR, "系统配置错误，请联系管理员");
        }

        SysUser user = new SysUser();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRoleId(defaultRole.getId());
        user.setStatus(1);
        sysUserMapper.insert(user);

        String roleCode = defaultRole.getRoleCode() != null ? defaultRole.getRoleCode() : DEFAULT_ROLE_CODE;
        String token = jwtUtil.generateToken(user.getUsername(), roleCode, user.getTokenVersion(), false);
        log.info("User registered successfully: {}", user.getUsername());

        return new LoginResponse(token, user.getUsername(), roleCode, new ArrayList<>());
    }

    @Override
    public void logout(String authorizationHeader) {
        String token = extractToken(authorizationHeader);
        if (token != null) {
            tokenStore.addToBlacklist(token);
            log.info("User logged out, token added to blacklist");
        }
    }

    @Override
    public LoginResponse refreshToken(String authorizationHeader, boolean rememberMe) {
        String oldToken = extractToken(authorizationHeader);
        if (oldToken == null) {
            throw new BizException(BizCode.UNAUTHORIZED, "未提供 Token");
        }

        if (!jwtUtil.validateToken(oldToken)) {
            throw new BizException(BizCode.UNAUTHORIZED, "Token 无效或已过期");
        }

        if (tokenStore.isBlacklisted(oldToken)) {
            throw new BizException(BizCode.UNAUTHORIZED, "Token 已失效");
        }

        String newToken = jwtUtil.refreshToken(oldToken, rememberMe);
        if (newToken == null) {
            throw new BizException(BizCode.UNAUTHORIZED, "刷新 Token 失败");
        }

        tokenStore.addToBlacklist(oldToken);

        String username = jwtUtil.getUsernameFromToken(newToken);
        String role = jwtUtil.getRoleFromToken(newToken);
        log.info("Token refreshed for user: {}", username);

        return new LoginResponse(newToken, username, role, new ArrayList<>());
    }

    @Override
    public UserInfoResponse getCurrentUser(String username, Long roleId) {
        if (username == null) {
            throw new BizException(BizCode.UNAUTHORIZED, "未登录");
        }

        SysUser user = sysUserMapper.selectUserWithRole(username);
        if (user == null) {
            throw new BizException(BizCode.UNAUTHORIZED, "用户不存在");
        }

        UserInfoResponse response = new UserInfoResponse();
        response.setId(user.getId());
        response.setUsername(user.getUsername());
        response.setRoleCode(user.getRoleCode());
        response.setStatus(user.getStatus());
        response.setCreateTime(user.getCreateTime());

        if (user.getRole() != null) {
            response.setRoleName(user.getRole().getRoleName());
        }

        if (roleId != null) {
            Set<String> permissions = permissionService.getPermissionCodes(roleId);
            response.setPermissions(new ArrayList<>(permissions));
        }

        return response;
    }

    @Override
    public void changePassword(String username, String authorizationHeader, ChangePasswordRequest request) {
        if (username == null) {
            throw new BizException(BizCode.UNAUTHORIZED, "未登录");
        }

        SysUser user = sysUserMapper.selectUserWithRole(username);
        if (user == null) {
            throw new BizException(BizCode.UNAUTHORIZED, "用户不存在");
        }

        if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
            throw new BizException(BizCode.PASSWORD_ERROR, 400, "原密码错误");
        }

        if (passwordEncoder.matches(request.getNewPassword(), user.getPassword())) {
            throw new BizException(BizCode.BAD_REQUEST, "新密码不能与原密码相同");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setTokenVersion(user.getTokenVersion() + 1);
        sysUserMapper.updateById(user);

        String currentToken = extractToken(authorizationHeader);
        if (currentToken != null) {
            tokenStore.addToBlacklist(currentToken);
            log.info("Auth session invalidated after credential update: username={}", username);
        }

        log.info("User credential updated: {}, tokenVersion incremented to {}",
                username, user.getTokenVersion());
    }

    private String resolveRoleCode(SysUser user) {
        if (user.getRoleCode() != null) {
            return user.getRoleCode();
        }
        if (user.getRole() != null) {
            return user.getRole().getRoleCode();
        }
        return null;
    }

    private String extractToken(String authorizationHeader) {
        if (authorizationHeader == null || authorizationHeader.isBlank()) {
            return null;
        }
        if (authorizationHeader.startsWith(BEARER_PREFIX)) {
            return authorizationHeader.substring(BEARER_PREFIX.length());
        }
        return authorizationHeader;
    }
}
