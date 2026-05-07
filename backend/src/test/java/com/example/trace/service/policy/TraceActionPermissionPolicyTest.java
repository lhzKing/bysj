package com.example.trace.service.policy;

import com.example.trace.enums.ActionType;
import com.example.trace.security.PermissionService;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TraceActionPermissionPolicyTest {

    private final PermissionService permissionService = mock(PermissionService.class);
    private final TraceActionPermissionPolicy policy = new TraceActionPermissionPolicy(permissionService);

    @Test
    void canExecute_shouldAllowNewBusinessActionPermissionsOnlyForMatchingActions() {
        when(permissionService.getPermissionCodes(7L)).thenReturn(Set.of(
                "trace:code:print",
                "trace:code:activate",
                "trace:exception:handle"
        ));

        assertThat(policy.canExecute(7L, ActionType.PRINT_CODE)).isTrue();
        assertThat(policy.canExecute(7L, ActionType.REPRINT_CODE)).isTrue();
        assertThat(policy.canExecute(7L, ActionType.VOID_CODE)).isTrue();
        assertThat(policy.canExecute(7L, ActionType.ACTIVATE_CODE)).isTrue();
        assertThat(policy.canExecute(7L, ActionType.EXCEPTION)).isTrue();
        assertThat(policy.canExecute(7L, ActionType.OUTBOUND)).isFalse();
    }

    @Test
    void canExecute_shouldKeepTraceScanAsLegacySuperPermission() {
        when(permissionService.getPermissionCodes(8L)).thenReturn(Set.of("trace:scan"));

        assertThat(policy.canExecute(8L, ActionType.OUTBOUND)).isTrue();
        assertThat(policy.canExecute(8L, ActionType.EXCEPTION)).isTrue();
        assertThat(policy.canExecute(8L, ActionType.ACTIVATE_CODE)).isTrue();
    }

    @Test
    void filterExecutable_shouldRetainOnlyActionsGrantedByRolePermissions() {
        when(permissionService.getPermissionCodes(9L)).thenReturn(Set.of(
                "trace:outbound",
                "trace:exception:handle"
        ));

        assertThat(policy.filterExecutable(9L, List.of(
                ActionType.OUTBOUND,
                ActionType.INBOUND,
                ActionType.EXCEPTION
        ))).containsExactly(ActionType.OUTBOUND, ActionType.EXCEPTION);
    }

    @Test
    void permissionHint_shouldExposeSpecificBusinessPermissionWithLegacySuperFallback() {
        assertThat(policy.permissionHint(ActionType.EXCEPTION))
                .isEqualTo("trace:exception:handle or trace:scan");
        assertThat(policy.permissionHint(ActionType.ACTIVATE_CODE))
                .isEqualTo("trace:code:activate or trace:scan");
        assertThat(policy.permissionHint(ActionType.CORRECTION))
                .isEqualTo("trace:scan");
    }
}
