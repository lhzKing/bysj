package com.example.trace.security;

import com.example.trace.annotation.RequirePermission;
import com.example.trace.common.ApiResponse;
import com.example.trace.common.BizCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;

/**
 * 权限拦截器 - 基于动态权限配置
 * 
 * 权限校验逻辑：
 * 1. 检查方法级别 @RequirePermission 注解
 * 2. 如果没有，检查类级别注解
 * 3. 如果都没有，检查 API 路径匹配
 */
@Component
public class PermissionInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(PermissionInterceptor.class);
    private static final String ATTR_ROLE_ID = "roleId";
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final PermissionService permissionService;

    public PermissionInterceptor(PermissionService permissionService) {
        this.permissionService = permissionService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws IOException {
        // 只处理 Controller 方法
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }

        // 注解解析提前到 roleId 检查之前，因为 allowAnonymous=true 的方法显式声明"无需登录"，
        // 不应该被防御性 401 误伤。
        RequirePermission methodAnnotation = handlerMethod.getMethodAnnotation(RequirePermission.class);
        RequirePermission classAnnotation = handlerMethod.getBeanType().getAnnotation(RequirePermission.class);
        RequirePermission effectiveAnnotation = methodAnnotation != null ? methodAnnotation : classAnnotation;

        // 1. allowAnonymous fast-path：注解显式声明"匿名可访问"——直接放行，不依赖 roleId
        if (effectiveAnnotation != null && effectiveAnnotation.allowAnonymous()) {
            return true;
        }

        // 2. 防御性 roleId 校验
        // 到达此处的请求，按 WebMvcConfig 拦截链顺序（LoginInterceptor.order=1 < PermissionInterceptor.order=2）
        // 应已被 LoginInterceptor 写入 roleId 属性。如果 roleId 仍然 null，意味着拦截器顺序被改、
        // 新过滤器抢先转发到这里、或被单独复用——此时绝不能 return true（fail-open）让权限检查被跳过。
        // 返回 401 + WARN 提示运维核查拦截器配置；正常匿名路径请用 WebMvcConfig.excludePathPatterns
        // 或 @RequirePermission(allowAnonymous = true) 显式声明，根本不应触达此分支。
        Long roleId = (Long) request.getAttribute(ATTR_ROLE_ID);
        if (roleId == null) {
            log.warn("PermissionInterceptor reached without roleId; check interceptor order. method={}, path={}",
                    request.getMethod(), request.getRequestURI());
            return denyAccess(response, "未授权，请先登录", 401, BizCode.UNAUTHORIZED);
        }

        // 3. 方法级注解优先
        if (methodAnnotation != null) {
            return checkAnnotationPermission(methodAnnotation, roleId, request, response);
        }

        // 4. 类级注解
        if (classAnnotation != null) {
            return checkAnnotationPermission(classAnnotation, roleId, request, response);
        }

        // 5. 都没有 → 走 API 路径权限匹配
        String method = request.getMethod();
        String path = request.getRequestURI();

        if (!permissionService.hasApiPermission(roleId, method, path)) {
            log.warn("API permission denied: {} {} for roleId={}", method, path, roleId);
            return denyAccess(response, "无权限访问此接口");
        }

        return true;
    }

    /**
     * 检查注解定义的权限
     */
    private boolean checkAnnotationPermission(
            RequirePermission annotation,
            Long roleId,
            HttpServletRequest request,
            HttpServletResponse response
    ) throws IOException {
        // 允许匿名访问
        if (annotation.allowAnonymous()) {
            return true;
        }

        String[] requiredPerms = annotation.value();
        
        // 空数组表示只需要登录，不需要特定权限
        if (requiredPerms.length == 0) {
            return true;
        }

        if (!permissionService.hasPermission(roleId, requiredPerms, annotation.matchAll())) {
            log.warn("Permission denied: required={}, matchAll={}, roleId={}", 
                    String.join(",", requiredPerms), annotation.matchAll(), roleId);
            return denyAccess(response, "无权限执行此操作");
        }

        return true;
    }

    /**
     * 拒绝访问，默认返回 403（FORBIDDEN）。
     */
    private boolean denyAccess(HttpServletResponse response, String message) throws IOException {
        return denyAccess(response, message, 403, BizCode.FORBIDDEN);
    }

    /**
     * 拒绝访问，可指定 HTTP 状态码与业务码。401 用于"未登录/会话失效"，403 用于"已登录但权限不足"。
     */
    private boolean denyAccess(HttpServletResponse response, String message, int httpStatus, int bizCode) throws IOException {
        response.setStatus(httpStatus);
        response.setContentType("application/json;charset=UTF-8");
        ApiResponse<Void> errorResponse = ApiResponse.fail(bizCode, message);
        response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
        return false;
    }
}
