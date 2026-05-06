package com.example.trace.service.policy;

import com.example.trace.common.BizCode;
import com.example.trace.common.BizException;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Centralized role governance policy.
 *
 * <p>User and role management services must use this component for role
 * priority, protected built-in roles, and permission delegation guardrails so
 * the hierarchy does not drift between services.</p>
 */
@Component
public class RolePolicy {

    public static final String SUPER_ADMIN = "SUPER_ADMIN";
    public static final String ADMIN = "ADMIN";
    public static final String PRODUCER = "PRODUCER";
    public static final String WAREHOUSE = "WAREHOUSE";
    public static final String LOGISTICS = "LOGISTICS";
    public static final String USER = "USER";

    private static final String PROTECTED_ROOT_USERNAME = "superadmin";

    private static final Map<String, Integer> ROLE_PRIORITY = Map.of(
        SUPER_ADMIN, 3,
        ADMIN, 2,
        PRODUCER, 1,
        WAREHOUSE, 1,
        LOGISTICS, 1,
        USER, 1
    );

    private static final Set<String> SYSTEM_ROLE_CODES = Set.of(
        SUPER_ADMIN,
        ADMIN,
        PRODUCER,
        WAREHOUSE,
        LOGISTICS,
        USER
    );

    private static final List<String> MANAGEMENT_ROLE_CODES = List.of(SUPER_ADMIN, ADMIN);

    private static final Set<String> PROTECTED_PERMISSION_PREFIXES = Set.of("user:", "role:");

    public int priorityOf(String roleCode) {
        return ROLE_PRIORITY.getOrDefault(roleCode, 0);
    }

    public boolean isSuperAdmin(String roleCode) {
        return SUPER_ADMIN.equals(roleCode);
    }

    public boolean isSystemRole(String roleCode) {
        return SYSTEM_ROLE_CODES.contains(roleCode);
    }

    public boolean isProtectedManagementRole(String roleCode) {
        return MANAGEMENT_ROLE_CODES.contains(roleCode);
    }

    public List<String> managementRoleCodes() {
        return MANAGEMENT_ROLE_CODES;
    }

    public boolean canManageRole(String operatorRoleCode, String targetRoleCode) {
        return priorityOf(operatorRoleCode) > priorityOf(targetRoleCode);
    }

    public boolean canResetCredential(String operatorRoleCode, String targetRoleCode) {
        return isSuperAdmin(operatorRoleCode) || !isProtectedManagementRole(targetRoleCode);
    }

    public boolean isProtectedRootUsername(String username) {
        return PROTECTED_ROOT_USERNAME.equals(username);
    }

    public void ensureOperatorRoleContext(String operatorRoleCode, String context) {
        if (!StringUtils.hasText(operatorRoleCode)) {
            String suffix = StringUtils.hasText(context) ? " for " + context : "";
            throw new BizException(BizCode.FORBIDDEN, "Missing operator role context" + suffix);
        }
    }

    public boolean canAssignProtectedPermissions(String operatorRoleCode) {
        return isSuperAdmin(operatorRoleCode);
    }

    public boolean isProtectedPermissionForDelegation(String permissionCode) {
        if (!StringUtils.hasText(permissionCode)) {
            return false;
        }
        return PROTECTED_PERMISSION_PREFIXES.stream().anyMatch(permissionCode::startsWith);
    }

    public List<String> protectedPermissionCodes(Collection<String> permissionCodes) {
        if (permissionCodes == null || permissionCodes.isEmpty()) {
            return List.of();
        }
        return permissionCodes.stream()
            .filter(this::isProtectedPermissionForDelegation)
            .distinct()
            .sorted()
            .toList();
    }
}
