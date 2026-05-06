package com.example.trace.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * Token 黑名单存储 - Redis 实现
 * 
 * 使用 Redis 存储已登出/失效的 Token 的 JTI（JWT ID）
 * Key 格式：token:blacklist:{jti}
 * TTL：与 Token 剩余有效期相同，过期后自动清理
 * 
 * 用于以下场景：
 * 1. 用户主动登出
 * 2. Token 刷新后旧 Token 失效
 * 3. 用户修改密码后强制失效
 */
@Component
public class TokenStore {

    private static final Logger log = LoggerFactory.getLogger(TokenStore.class);
    private static final String BLACKLIST_PREFIX = "token:blacklist:";
    private static final String OP_ADD = "blacklist-add";
    private static final String OP_CHECK = "blacklist-check";
    
    private final StringRedisTemplate redisTemplate;
    private final JwtUtil jwtUtil;

    public TokenStore(StringRedisTemplate redisTemplate, JwtUtil jwtUtil) {
        this.redisTemplate = redisTemplate;
        this.jwtUtil = jwtUtil;
        log.info("TokenStore initialized with Redis backend");
    }

    /**
     * 将 Token 加入黑名单（用于登出）
     * 使用 JTI 作为 key，TTL 为 Token 剩余有效期
     *
     * @param token JWT Token
     */
    public void addToBlacklist(String token) {
        if (token == null || token.isBlank()) {
            return;
        }
        
        try {
            String jti = jwtUtil.getJtiFromToken(token);
            if (jti == null) {
                log.warn("Cannot add token to blacklist: JTI not found");
                throw new TokenStoreException(OP_ADD, "Cannot revoke token without JTI");
            }
            
            // 计算剩余有效期（秒）
            long ttlSeconds = jwtUtil.getRemainingSeconds(token);
            if (ttlSeconds <= 0) {
                // Token 已过期，无需加入黑名单
                log.debug("Token already expired, skipping blacklist");
                return;
            }
            
            String key = BLACKLIST_PREFIX + jti;
            redisTemplate.opsForValue().set(key, "1", ttlSeconds, TimeUnit.SECONDS);
            log.debug("Token added to blacklist: jti={}, ttl={}s", jti, ttlSeconds);
        } catch (TokenStoreException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to add token to blacklist: {}", e.getClass().getSimpleName());
            throw new TokenStoreException(OP_ADD, "Token blacklist storage unavailable", e);
        }
    }

    /**
     * 检查 Token 是否在黑名单中
     *
     * @param token JWT Token
     * @return true=已被加入黑名单（已登出）
     */
    public boolean isBlacklisted(String token) {
        if (token == null) {
            return false;
        }
        
        try {
            String jti = jwtUtil.getJtiFromToken(token);
            if (jti == null) {
                return false;
            }
            
            String key = BLACKLIST_PREFIX + jti;
            Boolean exists = redisTemplate.hasKey(key);
            return Boolean.TRUE.equals(exists);
        } catch (Exception e) {
            log.error("Failed to check token blacklist; failing closed: {}", e.getClass().getSimpleName());
            throw new TokenStoreException(OP_CHECK, "Token blacklist storage unavailable", e);
        }
    }

    /**
     * 从黑名单移除 Token（一般不需要，TTL 自动过期）
     *
     * @param token JWT Token
     */
    public void removeFromBlacklist(String token) {
        if (token == null) {
            return;
        }
        
        try {
            String jti = jwtUtil.getJtiFromToken(token);
            if (jti != null) {
                String key = BLACKLIST_PREFIX + jti;
                redisTemplate.delete(key);
                log.debug("Token removed from blacklist: jti={}", jti);
            }
        } catch (Exception e) {
            log.error("Failed to remove token from blacklist: {}", e.getClass().getSimpleName());
        }
    }

    /**
     * 清空黑名单（仅用于测试）
     */
    public void clearBlacklist() {
        try {
            var keys = redisTemplate.keys(BLACKLIST_PREFIX + "*");
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
                log.info("Blacklist cleared: {} keys deleted", keys.size());
            }
        } catch (Exception e) {
            log.error("Failed to clear blacklist: {}", e.getClass().getSimpleName());
        }
    }

    /**
     * 获取黑名单大小（用于监控）
     */
    public long getBlacklistSize() {
        try {
            var keys = redisTemplate.keys(BLACKLIST_PREFIX + "*");
            return keys != null ? keys.size() : 0;
        } catch (Exception e) {
            log.error("Failed to get blacklist size: {}", e.getClass().getSimpleName());
            return 0;
        }
    }

}
