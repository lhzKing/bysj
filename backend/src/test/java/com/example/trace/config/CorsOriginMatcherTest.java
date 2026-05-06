package com.example.trace.config;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CorsOriginMatcherTest {

    private final CorsOriginMatcher matcher = new CorsOriginMatcher();

    @Test
    void isAllowed_shouldSupportExactOriginAndWildcardPattern() {
        assertThat(matcher.isAllowed(
            "https://localhost:5173",
            List.of("https://localhost:5173"),
            List.of("https://192.168.*:5173")
        )).isTrue();

        assertThat(matcher.isAllowed(
            "https://192.168.2.10:5173",
            List.of("https://localhost:5173"),
            List.of("https://192.168.*:5173")
        )).isTrue();
    }

    @Test
    void isAllowed_shouldRejectOriginOutsideConfiguredLists() {
        assertThat(matcher.isAllowed(
            "https://evil.example.com",
            List.of("https://localhost:5173"),
            List.of("https://192.168.*:5173")
        )).isFalse();
    }
}
