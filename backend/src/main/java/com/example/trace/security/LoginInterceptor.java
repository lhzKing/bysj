package com.example.trace.security;

import com.example.trace.common.ApiResponse;
import com.example.trace.common.BizCode;
import com.example.trace.entity.SysUser;
import com.example.trace.mapper.SysUserMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;

/**
 * 登录拦截器 - JWT 验证
 * Authorization: Bearer <jwt-token>
 */
@Component
public class LoginInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(LoginInterceptor.class);
    private static final String ATTR_USERNAME = "username";
    private static final String ATTR_USER_ID = "userId";
    private static final String ATTR_ROLE = "role";
    private static final String ATTR_ROLE_ID = "roleId";
    private static final String BEARER_PREFIX = "Bearer ";
    private final ObjectMapper objectMapper = new ObjectMapper();

    private final JwtUtil jwtUtil;
    private final TokenStore tokenStore;
    private final PermissionService permissionService;
    private final SysUserMapper userMapper;

    public LoginInterceptor(JwtUtil jwtUtil, TokenStore tokenStore, 
                           PermissionService permissionService, SysUserMapper userMapper) {
        this.jwtUtil = jwtUtil;
        this.tokenStore = tokenStore;
        this.permissionService = permissionService;
        this.userMapper = userMapper;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws IOException {
        // 放行 OPTIONS 预检请求
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        String authHeader = request.getHeader("Authorization");
        log.debug("Auth header metadata: present={}, bearerScheme={}",
                authHeader != null && !authHeader.isBlank(),
                hasBearerScheme(authHeader));

        String token = extractToken(authHeader);
        
        if (token == null) {
            log.debug("Authentication credential missing");
            return sendUnauthorized(response, "未授权，请先登录");
        }

        // 验证 JWT Token
        if (!jwtUtil.validateToken(token)) {
            log.debug("JWT Token validation failed");
            return sendUnauthorized(response, "Token 无效或已过期，请重新登录");
        }

        // 检查 Token 是否在黑名单中（已登出）。Redis 异常必须 fail-closed，不能继续放行。
        try {
            if (tokenStore.isBlacklisted(token)) {
                log.debug("Token is blacklisted (logged out)");
                return sendUnauthorized(response, "Token 已失效，请重新登录");
            }
        } catch (TokenStoreException e) {
            log.warn("Authentication state store unavailable: operation={}", e.getOperation());
            return sendServiceUnavailable(response, "认证状态存储暂不可用，请稍后重试");
        }

        // 从 Token 中提取用户信息并设置到 request attribute
        String username = jwtUtil.getUsernameFromToken(token);
        String roleCode = jwtUtil.getRoleFromToken(token);
        Integer tokenVersion = jwtUtil.getTokenVersionFromToken(token);
        
        log.debug("JWT validated successfully: username={}, role={}, tokenVersion={}", 
                username, roleCode, tokenVersion);
        
        // 校验 Token 版本号（防止密码修改/角色变更后旧 Token 继续使用）
        SysUser user = userMapper.selectUserWithRole(username);
        if (user == null) {
            log.warn("User not found in database: {}", username);
            return sendUnauthorized(response, "用户不存在，请重新登录");
        }
        
        if (!user.isEnabled()) {
            log.warn("User is disabled: {}", username);
            return sendUnauthorized(response, "账号已被禁用，请联系管理员");
        }
        
        // Token 版本校验：如果用户的 tokenVersion 大于 Token 中的版本，说明 Token 已过时
        Integer userTokenVersion = user.getTokenVersion();
        if (tokenVersion == null || tokenVersion < userTokenVersion) {
            log.warn("Token version mismatch: token={}, user={}", tokenVersion, userTokenVersion);
            return sendUnauthorized(response, "Token 已失效（密码或角色已变更），请重新登录");
        }
        
        // 获取角色ID（用于权限校验）
        Long roleId = permissionService.getRoleIdByCode(roleCode);
        if (roleId == null) {
            log.warn("Role not found in database: {}", roleCode);
            return sendUnauthorized(response, "用户角色无效，请联系管理员");
        }
        
        request.setAttribute(ATTR_USERNAME, username);
        request.setAttribute(ATTR_USER_ID, user.getId());
        request.setAttribute(ATTR_ROLE, roleCode);
        request.setAttribute(ATTR_ROLE_ID, roleId);
        
        return true;
    }

    /**
     * 提取 Token，支持 Bearer 前缀格式
     */
    private boolean hasBearerScheme(String authHeader) {
        return authHeader != null && authHeader.startsWith(BEARER_PREFIX);
    }

    private String extractToken(String authHeader) {
        if (authHeader == null || authHeader.isBlank()) {
            return null;
        }
        // 标准 Bearer 格式
        if (authHeader.startsWith(BEARER_PREFIX)) {
            return authHeader.substring(BEARER_PREFIX.length());
        }
        // 兼容旧版直接传 token（不推荐）
        return authHeader;
    }

    /**
     * 发送 401 未授权响应
     */
    private boolean sendUnauthorized(HttpServletResponse response, String message) throws IOException {
        response.setStatus(401);
        response.setContentType("application/json;charset=UTF-8");
        ApiResponse<Void> errorResponse = ApiResponse.fail(BizCode.UNAUTHORIZED, message);
        response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
        return false;
    }

    private boolean sendServiceUnavailable(HttpServletResponse response, String message) throws IOException {
        response.setStatus(503);
        response.setContentType("application/json;charset=UTF-8");
        ApiResponse<Void> errorResponse = ApiResponse.fail(BizCode.SERVER_ERROR, 503, message);
        response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
        return false;
    }
}
