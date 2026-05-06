package com.example.trace.security;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;

class JwtUtilTest {

    private static final String TEST_SECRET = "JwtUtilTestSecretWithAtLeast32Bytes2026!";

    @Test
    void generateToken_shouldUseConfiguredShortDefaultExpiration() {
        JwtUtil jwtUtil = new JwtUtil(TEST_SECRET, 2, 1);

        Instant issuedBefore = Instant.now();
        String token = jwtUtil.generateToken("alice", "ADMIN", 3, false);
        Instant issuedAfter = Instant.now();

        assertExpirationIsBetween(jwtUtil.getExpirationFromToken(token), issuedBefore, issuedAfter, Duration.ofHours(2));
        assertThat(jwtUtil.getUsernameFromToken(token)).isEqualTo("alice");
        assertThat(jwtUtil.getRoleFromToken(token)).isEqualTo("ADMIN");
        assertThat(jwtUtil.getTokenVersionFromToken(token)).isEqualTo(3);
        assertThat(jwtUtil.getJtiFromToken(token)).isNotBlank();
    }

    @Test
    void generateToken_shouldUseConfiguredRememberMeExpiration() {
        JwtUtil jwtUtil = new JwtUtil(TEST_SECRET, 2, 1);

        Instant issuedBefore = Instant.now();
        String token = jwtUtil.generateToken("alice", "ADMIN", 3, true);
        Instant issuedAfter = Instant.now();

        assertExpirationIsBetween(jwtUtil.getExpirationFromToken(token), issuedBefore, issuedAfter, Duration.ofDays(1));
    }

    private static void assertExpirationIsBetween(
            Date expiration,
            Instant issuedBefore,
            Instant issuedAfter,
            Duration expectedRemaining
    ) {
        assertThat(expiration.toInstant())
                .isAfterOrEqualTo(issuedBefore.plus(expectedRemaining).minusSeconds(1))
                .isBeforeOrEqualTo(issuedAfter.plus(expectedRemaining).plusSeconds(1));
    }
}
