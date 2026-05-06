package com.example.trace.config;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class SecurityDefaultsConfigTest {

    @Test
    void applicationYaml_shouldKeepShortJwtDefaultsForLocalStorageRiskCompensation() throws IOException {
        try (var input = getClass().getResourceAsStream("/application.yml")) {
            assertThat(input).isNotNull();
            String yaml = new String(input.readAllBytes(), StandardCharsets.UTF_8);

            assertThat(yaml).contains("expiration: ${TRACE_JWT_EXPIRATION_HOURS:2}");
            assertThat(yaml).contains("remember-expiration: ${TRACE_JWT_REMEMBER_EXPIRATION_DAYS:1}");
        }
    }
}

