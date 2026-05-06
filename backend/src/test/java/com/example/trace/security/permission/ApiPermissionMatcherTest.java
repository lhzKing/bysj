package com.example.trace.security.permission;

import com.example.trace.entity.SysPermission;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ApiPermissionMatcherTest {

    private final ApiPermissionMatcher matcher = new ApiPermissionMatcher();

    @Test
    void matches_shouldSupportWildcardMethodAndPath() {
        SysPermission permission = new SysPermission();
        permission.setApiMethod("*");
        permission.setApiPattern("/api/traces/*");

        assertThat(matcher.matches(permission, "GET", "/api/traces/123")).isTrue();
        assertThat(matcher.matches(permission, "POST", "/api/traces/123/events")).isTrue();
    }

    @Test
    void matches_shouldRejectMismatchedMethodOrBlankPattern() {
        SysPermission permission = new SysPermission();
        permission.setApiMethod("GET");
        permission.setApiPattern("/api/users/*");

        assertThat(matcher.matches(permission, "POST", "/api/users/1")).isFalse();

        permission.setApiPattern(" ");
        assertThat(matcher.matches(permission, "GET", "/api/users/1")).isFalse();
    }
}
