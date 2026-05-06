package com.example.trace.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TokenStoreTest {

    @Mock private StringRedisTemplate redisTemplate;
    @Mock private ValueOperations<String, String> valueOperations;
    @Mock private JwtUtil jwtUtil;

    private TokenStore tokenStore;

    @BeforeEach
    void setUp() {
        tokenStore = new TokenStore(redisTemplate, jwtUtil);
    }

    @Test
    void isBlacklisted_shouldThrowAndFailClosedWhenRedisCheckFails() {
        when(jwtUtil.getJtiFromToken("jwt-token")).thenReturn("jti-1");
        when(redisTemplate.hasKey("token:blacklist:jti-1"))
                .thenThrow(new IllegalStateException("redis down"));

        assertThatThrownBy(() -> tokenStore.isBlacklisted("jwt-token"))
                .isInstanceOf(TokenStoreException.class)
                .satisfies(ex -> assertThat(((TokenStoreException) ex).getOperation())
                        .isEqualTo("blacklist-check"));
    }

    @Test
    void addToBlacklist_shouldThrowWhenRedisWriteFails() {
        when(jwtUtil.getJtiFromToken("jwt-token")).thenReturn("jti-1");
        when(jwtUtil.getRemainingSeconds("jwt-token")).thenReturn(120L);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        doThrow(new IllegalStateException("redis down"))
                .when(valueOperations)
                .set("token:blacklist:jti-1", "1", 120L, TimeUnit.SECONDS);

        assertThatThrownBy(() -> tokenStore.addToBlacklist("jwt-token"))
                .isInstanceOf(TokenStoreException.class)
                .satisfies(ex -> assertThat(((TokenStoreException) ex).getOperation())
                        .isEqualTo("blacklist-add"));
    }

    @Test
    void addToBlacklist_shouldThrowWhenJtiIsMissing() {
        when(jwtUtil.getJtiFromToken("legacy-token")).thenReturn(null);

        assertThatThrownBy(() -> tokenStore.addToBlacklist("legacy-token"))
                .isInstanceOf(TokenStoreException.class)
                .satisfies(ex -> assertThat(((TokenStoreException) ex).getOperation())
                        .isEqualTo("blacklist-add"));

        verifyNoInteractions(redisTemplate);
    }

    @Test
    void addToBlacklist_shouldSkipExpiredTokenWithoutRedisWrite() {
        when(jwtUtil.getJtiFromToken("expired-token")).thenReturn("jti-expired");
        when(jwtUtil.getRemainingSeconds("expired-token")).thenReturn(0L);

        tokenStore.addToBlacklist("expired-token");

        verify(redisTemplate, org.mockito.Mockito.never()).opsForValue();
    }
}
