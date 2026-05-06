package com.example.trace.security.permission;

import com.example.trace.entity.SysPermission;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;

/**
 * Matches API permission entries against request method and path.
 */
@Component
public class ApiPermissionMatcher {

    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    public boolean matches(SysPermission permission, String method, String path) {
        String permissionMethod = permission.getApiMethod();
        if (permissionMethod != null
                && !"*".equals(permissionMethod)
                && !permissionMethod.equalsIgnoreCase(method)) {
            return false;
        }

        String pattern = permission.getApiPattern();
        if (pattern == null || pattern.isBlank()) {
            return false;
        }

        return pathMatcher.match(toAntPattern(pattern), path);
    }

    private String toAntPattern(String pattern) {
        return pattern.replace("*", "**");
    }
}
