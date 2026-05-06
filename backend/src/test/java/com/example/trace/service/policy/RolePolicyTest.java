package com.example.trace.service.policy;

import com.example.trace.common.BizCode;
import com.example.trace.common.BizException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RolePolicyTest {

    private final RolePolicy rolePolicy = new RolePolicy();

    @Test
    void priorityOf_shouldKeepSingleHierarchyForBuiltInAndCustomRoles() {
        assertThat(rolePolicy.priorityOf("SUPER_ADMIN")).isEqualTo(3);
        assertThat(rolePolicy.priorityOf("ADMIN")).isEqualTo(2);
        assertThat(rolePolicy.priorityOf("PRODUCER")).isEqualTo(1);
        assertThat(rolePolicy.priorityOf("WAREHOUSE")).isEqualTo(1);
        assertThat(rolePolicy.priorityOf("LOGISTICS")).isEqualTo(1);
        assertThat(rolePolicy.priorityOf("USER")).isEqualTo(1);
        assertThat(rolePolicy.priorityOf("AUDITOR")).isZero();
    }

    @Test
    void canManageRole_shouldRequireStrictlyHigherPriority() {
        assertThat(rolePolicy.canManageRole("SUPER_ADMIN", "ADMIN")).isTrue();
        assertThat(rolePolicy.canManageRole("ADMIN", "USER")).isTrue();
        assertThat(rolePolicy.canManageRole("ADMIN", "ADMIN")).isFalse();
        assertThat(rolePolicy.canManageRole("ADMIN", "SUPER_ADMIN")).isFalse();
        assertThat(rolePolicy.canManageRole("AUDITOR", "AUDITOR")).isFalse();
    }

    @Test
    void systemAndManagementRoleSets_shouldBeCentralized() {
        assertThat(rolePolicy.isSystemRole("SUPER_ADMIN")).isTrue();
        assertThat(rolePolicy.isSystemRole("USER")).isTrue();
        assertThat(rolePolicy.isSystemRole("AUDITOR")).isFalse();
        assertThat(rolePolicy.managementRoleCodes()).containsExactly("SUPER_ADMIN", "ADMIN");
        assertThat(rolePolicy.isProtectedManagementRole("ADMIN")).isTrue();
        assertThat(rolePolicy.isProtectedManagementRole("USER")).isFalse();
    }

    @Test
    void canResetCredential_shouldProtectManagementRolesExceptForSuperAdmin() {
        assertThat(rolePolicy.canResetCredential("SUPER_ADMIN", "SUPER_ADMIN")).isTrue();
        assertThat(rolePolicy.canResetCredential("SUPER_ADMIN", "ADMIN")).isTrue();
        assertThat(rolePolicy.canResetCredential("ADMIN", "ADMIN")).isFalse();
        assertThat(rolePolicy.canResetCredential("ADMIN", "USER")).isTrue();
    }

    @Test
    void protectedPermissionCodes_shouldReturnSortedDistinctManagementPermissionCodes() {
        assertThat(rolePolicy.protectedPermissionCodes(List.of(
            "trace:view",
            "role:manage",
            "user:view",
            "role:manage",
            "",
            "dashboard:view"
        ))).containsExactly("role:manage", "user:view");
    }

    @Test
    void ensureOperatorRoleContext_shouldRejectBlankRoleContext() {
        assertThatThrownBy(() -> rolePolicy.ensureOperatorRoleContext(" ", "role management"))
            .isInstanceOf(BizException.class)
            .extracting("code")
            .isEqualTo(BizCode.FORBIDDEN);
    }
}
