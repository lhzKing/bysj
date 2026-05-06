package com.example.trace.security;

import com.example.trace.annotation.RequirePermission;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.method.HandlerMethod;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PermissionInterceptorTest {

    @Mock
    private PermissionService permissionService;

    private PermissionInterceptor interceptor;

    @BeforeEach
    void setUp() {
        interceptor = new PermissionInterceptor(permissionService);
    }

    @Test
    void preHandle_shouldUseAnnotationPermissionWhenPresent() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/roles");
        request.setAttribute("roleId", 7L);
        MockHttpServletResponse response = new MockHttpServletResponse();
        HandlerMethod handlerMethod = new HandlerMethod(new TestController(), TestController.class.getMethod("annotated"));

        when(permissionService.hasPermission(7L, new String[]{"role:view"}, false)).thenReturn(true);

        assertThat(interceptor.preHandle(request, response, handlerMethod)).isTrue();
        verify(permissionService).hasPermission(7L, new String[]{"role:view"}, false);
        verify(permissionService, never()).hasApiPermission(anyLong(), anyString(), anyString());
    }

    @Test
    void preHandle_shouldFallbackToApiPermissionWhenNoAnnotationExists() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/roles");
        request.setAttribute("roleId", 9L);
        MockHttpServletResponse response = new MockHttpServletResponse();
        HandlerMethod handlerMethod = new HandlerMethod(new TestController(), TestController.class.getMethod("fallback"));

        when(permissionService.hasApiPermission(9L, "GET", "/api/roles")).thenReturn(true);

        assertThat(interceptor.preHandle(request, response, handlerMethod)).isTrue();
        verify(permissionService).hasApiPermission(9L, "GET", "/api/roles");
        verify(permissionService, never()).hasPermission(anyLong(), any(String[].class), anyBoolean());
    }

    @Test
    void preHandle_shouldRejectWithUnauthorizedWhenRoleIdMissing() throws Exception {
        // 模拟"拦截器顺序被改 / LoginInterceptor 未运行"导致 roleId 没有写入请求属性。
        // 旧实现会 fail-open（return true）；新实现必须 fail-closed 返回 401。
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/users");
        // 故意不调用 request.setAttribute("roleId", ...)
        MockHttpServletResponse response = new MockHttpServletResponse();
        HandlerMethod handlerMethod = new HandlerMethod(new TestController(), TestController.class.getMethod("fallback"));

        assertThat(interceptor.preHandle(request, response, handlerMethod)).isFalse();
        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentType()).isEqualTo("application/json;charset=UTF-8");
        // 不应触发任何 PermissionService 查询——直接被防御性短路拒绝
        verifyNoInteractions(permissionService);
    }

    @Test
    void preHandle_shouldAllowAnonymousAnnotatedMethodEvenWithoutRoleId() throws Exception {
        // 显式标注 allowAnonymous=true 的接口在 LoginInterceptor 跳过的场景下也必须能放行——
        // allowAnonymous fast-path 必须早于防御性 roleId 检查，否则会把"匿名可访问"语义破坏。
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/anonymous-allowed");
        // 故意不设 roleId
        MockHttpServletResponse response = new MockHttpServletResponse();
        HandlerMethod handlerMethod = new HandlerMethod(new TestController(), TestController.class.getMethod("anonymous"));

        assertThat(interceptor.preHandle(request, response, handlerMethod)).isTrue();
        // 没有真正校验权限，所以 PermissionService 不应被调用
        verifyNoInteractions(permissionService);
    }

    @Test
    void preHandle_shouldDenyForbiddenWhenAnnotationPermissionFails() throws Exception {
        // 已登录但角色权限不足——返回 403（FORBIDDEN），与 401 区分；body 用 BizCode.FORBIDDEN(10003)。
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/roles");
        request.setAttribute("roleId", 5L);
        MockHttpServletResponse response = new MockHttpServletResponse();
        HandlerMethod handlerMethod = new HandlerMethod(new TestController(), TestController.class.getMethod("annotated"));

        when(permissionService.hasPermission(5L, new String[]{"role:view"}, false)).thenReturn(false);

        assertThat(interceptor.preHandle(request, response, handlerMethod)).isFalse();
        assertThat(response.getStatus()).isEqualTo(403);
    }

    @Test
    void preHandle_shouldDenyForbiddenWhenApiPathPermissionFails() throws Exception {
        // 走 API path 匹配的路径，权限不足同样返回 403——验证未被防御性 401 改动误伤。
        MockHttpServletRequest request = new MockHttpServletRequest("DELETE", "/api/parts/100");
        request.setAttribute("roleId", 6L);
        MockHttpServletResponse response = new MockHttpServletResponse();
        HandlerMethod handlerMethod = new HandlerMethod(new TestController(), TestController.class.getMethod("fallback"));

        when(permissionService.hasApiPermission(6L, "DELETE", "/api/parts/100")).thenReturn(false);

        assertThat(interceptor.preHandle(request, response, handlerMethod)).isFalse();
        assertThat(response.getStatus()).isEqualTo(403);
    }

    static class TestController {
        @RequirePermission("role:view")
        public void annotated() {
        }

        public void fallback() {
        }

        @RequirePermission(allowAnonymous = true)
        public void anonymous() {
        }
    }
}
