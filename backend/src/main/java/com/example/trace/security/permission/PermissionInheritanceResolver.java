package com.example.trace.security.permission;

import org.springframework.stereotype.Component;

import java.util.ArrayDeque;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * Expands direct permission codes with transitive inheritance rules.
 */
@Component
public class PermissionInheritanceResolver {

    private static final Map<String, String> PERMISSION_INHERITANCE = Map.ofEntries(
        Map.entry("user:manage", "user:view"),
        Map.entry("role:manage", "role:view"),
        Map.entry("part:manage", "part:view"),
        Map.entry("trace:create", "trace:view"),
        Map.entry("trace:batch:create", "trace:view"),
        Map.entry("trace:code:print", "trace:view"),
        Map.entry("trace:code:activate", "trace:view"),
        Map.entry("trace:scan", "trace:view"),
        Map.entry("trace:inbound", "trace:view"),
        Map.entry("trace:outbound", "trace:view"),
        Map.entry("trace:transfer", "trace:view"),
        Map.entry("trace:task:create", "trace:view"),
        Map.entry("trace:task:scan", "trace:view"),
        Map.entry("trace:task:complete", "trace:view"),
        Map.entry("trace:exception:handle", "trace:view"),
        Map.entry("trace:audit:view", "trace:view")
    );

    public Set<String> expand(Set<String> directCodes) {
        if (directCodes == null || directCodes.isEmpty()) {
            return Set.of();
        }

        Set<String> expandedCodes = new LinkedHashSet<>(directCodes);
        ArrayDeque<String> pendingCodes = new ArrayDeque<>(directCodes);
        while (!pendingCodes.isEmpty()) {
            String code = pendingCodes.removeFirst();
            String inheritedCode = PERMISSION_INHERITANCE.get(code);
            if (inheritedCode != null && expandedCodes.add(inheritedCode)) {
                pendingCodes.addLast(inheritedCode);
            }
        }
        return expandedCodes;
    }
}
