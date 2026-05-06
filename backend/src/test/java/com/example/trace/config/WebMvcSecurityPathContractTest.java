package com.example.trace.config;

import com.example.trace.security.LoginInterceptor;
import com.example.trace.security.PermissionInterceptor;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.handler.MappedInterceptor;
import org.springframework.web.util.ServletRequestPathUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class WebMvcSecurityPathContractTest {

    @Test
    void webMvcConfig_shouldProtectManagementApisAndKeepAnonymousAuthEntryPoints() {
        LoginInterceptor loginInterceptor = mock(LoginInterceptor.class);
        PermissionInterceptor permissionInterceptor = mock(PermissionInterceptor.class);

        List<MappedInterceptor> mappedInterceptors = mappedInterceptors(loginInterceptor, permissionInterceptor);

        MappedInterceptor loginMapping = mappingFor(mappedInterceptors, loginInterceptor);
        assertThat(loginMapping.getIncludePathPatterns()).containsExactly("/api/**");
        assertThat(loginMapping.getExcludePathPatterns())
                .containsExactly("/api/auth/login", "/api/auth/register", "/api/traces/public-key");

        assertThat(loginMapping.matches(request("GET", "/api/users"))).isTrue();
        assertThat(loginMapping.matches(request("POST", "/api/users"))).isTrue();
        assertThat(loginMapping.matches(request("GET", "/api/parts"))).isTrue();
        assertThat(loginMapping.matches(request("POST", "/api/auth/register"))).isFalse();
        assertThat(loginMapping.matches(request("POST", "/api/auth/login"))).isFalse();
        assertThat(loginMapping.matches(request("GET", "/api/traces/public-key"))).isFalse();
    }

    @Test
    void webMvcConfig_shouldRunLoginBeforePermissionAndSkipPermissionForAuthApis() {
        LoginInterceptor loginInterceptor = mock(LoginInterceptor.class);
        PermissionInterceptor permissionInterceptor = mock(PermissionInterceptor.class);

        List<MappedInterceptor> mappedInterceptors = mappedInterceptors(loginInterceptor, permissionInterceptor);

        assertThat(mappedInterceptors)
                .extracting(MappedInterceptor::getInterceptor)
                .containsExactly(loginInterceptor, permissionInterceptor);

        MappedInterceptor permissionMapping = mappingFor(mappedInterceptors, permissionInterceptor);
        assertThat(permissionMapping.getIncludePathPatterns()).containsExactly("/api/**");
        assertThat(permissionMapping.getExcludePathPatterns())
                .containsExactly("/api/auth/**", "/api/traces/public-key");

        assertThat(permissionMapping.matches(request("GET", "/api/users"))).isTrue();
        assertThat(permissionMapping.matches(request("POST", "/api/traces/TRACE-001/events"))).isTrue();
        assertThat(permissionMapping.matches(request("POST", "/api/auth/register"))).isFalse();
        assertThat(permissionMapping.matches(request("POST", "/api/auth/refresh"))).isFalse();
        assertThat(permissionMapping.matches(request("GET", "/api/traces/public-key"))).isFalse();
    }

    private static List<MappedInterceptor> mappedInterceptors(
            LoginInterceptor loginInterceptor,
            PermissionInterceptor permissionInterceptor
    ) {
        ExposedInterceptorRegistry registry = new ExposedInterceptorRegistry();
        new WebMvcConfig(loginInterceptor, permissionInterceptor).addInterceptors(registry);

        return registry.exposedInterceptors().stream()
                .map(MappedInterceptor.class::cast)
                .toList();
    }

    private static MappedInterceptor mappingFor(
            List<MappedInterceptor> mappings,
            HandlerInterceptor target
    ) {
        return mappings.stream()
                .filter(mapping -> mapping.getInterceptor() == target)
                .findFirst()
                .orElseThrow();
    }

    private static MockHttpServletRequest request(String method, String path) {
        MockHttpServletRequest request = new MockHttpServletRequest(method, path);
        request.setServletPath(path);
        ServletRequestPathUtils.parseAndCache(request);
        return request;
    }

    private static final class ExposedInterceptorRegistry extends InterceptorRegistry {
        List<Object> exposedInterceptors() {
            return super.getInterceptors();
        }
    }
}
