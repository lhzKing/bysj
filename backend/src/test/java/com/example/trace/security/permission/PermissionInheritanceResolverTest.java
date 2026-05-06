package com.example.trace.security.permission;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class PermissionInheritanceResolverTest {

    private final PermissionInheritanceResolver resolver = new PermissionInheritanceResolver();

    @Test
    void expand_shouldResolveTransitiveInheritance() {
        assertThat(resolver.expand(Set.of("trace:transfer")))
            .containsExactlyInAnyOrder("trace:transfer", "trace:view");
    }

    @Test
    void expand_shouldLetAuditViewInheritBaseTraceView() {
        assertThat(resolver.expand(Set.of("trace:audit:view")))
            .containsExactlyInAnyOrder("trace:audit:view", "trace:view");
    }

    @Test
    void expand_shouldReturnEmptySetForEmptyInput() {
        assertThat(resolver.expand(Set.of())).isEmpty();
        assertThat(resolver.expand(null)).isEmpty();
    }
}
