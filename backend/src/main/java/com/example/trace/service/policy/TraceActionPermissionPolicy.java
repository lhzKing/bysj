package com.example.trace.service.policy;

import com.example.trace.enums.ActionType;
import com.example.trace.security.PermissionService;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * Centralized permission policy for trace lifecycle actions.
 *
 * <p>{@code trace:scan} is kept as the legacy/super scan permission. Newer
 * business-action permissions unlock only their matching operation.</p>
 */
@Component
public class TraceActionPermissionPolicy {

    public static final String TRACE_SCAN = "trace:scan";
    public static final String TRACE_INBOUND = "trace:inbound";
    public static final String TRACE_OUTBOUND = "trace:outbound";
    public static final String TRACE_TRANSFER = "trace:transfer";
    public static final String TRACE_CODE_PRINT = "trace:code:print";
    public static final String TRACE_CODE_ACTIVATE = "trace:code:activate";
    public static final String TRACE_TASK_SCAN = "trace:task:scan";
    public static final String TRACE_EXCEPTION_HANDLE = "trace:exception:handle";

    private final PermissionService permissionService;

    public TraceActionPermissionPolicy(PermissionService permissionService) {
        this.permissionService = permissionService;
    }

    public boolean canExecute(Long roleId, ActionType actionType) {
        if (roleId == null || actionType == null) {
            return false;
        }
        return hasActionPermission(permissionService.getPermissionCodes(roleId), actionType);
    }

    public List<ActionType> filterExecutable(Long roleId, Collection<ActionType> actionTypes) {
        if (roleId == null || actionTypes == null || actionTypes.isEmpty()) {
            return List.of();
        }
        Set<String> permissionCodes = permissionService.getPermissionCodes(roleId);
        return actionTypes.stream()
                .filter(actionType -> hasActionPermission(permissionCodes, actionType))
                .toList();
    }

    public String permissionHint(ActionType actionType) {
        String specificPermission = specificPermission(actionType);
        if (specificPermission == null) {
            return TRACE_SCAN;
        }
        return specificPermission + " or " + TRACE_SCAN;
    }

    private boolean hasActionPermission(Set<String> permissionCodes, ActionType actionType) {
        if (permissionCodes == null || actionType == null) {
            return false;
        }
        if (permissionCodes.contains(TRACE_SCAN)) {
            return true;
        }
        String specificPermission = specificPermission(actionType);
        return specificPermission != null && permissionCodes.contains(specificPermission);
    }

    private String specificPermission(ActionType actionType) {
        return switch (actionType) {
            case PRINT_CODE, REPRINT_CODE, VOID_CODE -> TRACE_CODE_PRINT;
            case ACTIVATE_CODE -> TRACE_CODE_ACTIVATE;
            case PACK, UNPACK, PALLETIZE, UNPALLETIZE -> TRACE_TASK_SCAN;
            case INBOUND -> TRACE_INBOUND;
            case OUTBOUND -> TRACE_OUTBOUND;
            case TRANSFER -> TRACE_TRANSFER;
            case EXCEPTION -> TRACE_EXCEPTION_HANDLE;
            default -> null;
        };
    }
}
