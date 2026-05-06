package com.example.trace.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Date;
import java.util.UUID;

/**
 * JWT 工具类 - 负责 Token 的生成、验证、解析
 * 
 * 配置项（application.yml）:
 * - jwt.secret: 签名密钥（至少32字符）
 * - jwt.expiration: 默认过期时间（小时）
 * - jwt.remember-expiration: 记住登录过期时间（天）
 */
@Component
public class JwtUtil {

    private static final Logger log = LoggerFactory.getLogger(JwtUtil.class);
    
    private static final String CLAIM_USERNAME = "username";
    private static final String CLAIM_ROLE = "role";
    private static final String CLAIM_TOKEN_VERSION = "token_version";
    private static final String CLAIM_JTI = "jti";

    private final SecretKey secretKey;
    private final Duration defaultExpiration;
    private final Duration rememberExpiration;

    public JwtUtil(
            @Value("${jwt.secret:}") String secret,
            @Value("${jwt.expiration:24}") int expirationHours,
            @Value("${jwt.remember-expiration:7}") int rememberExpirationDays
    ) {
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException("jwt.secret must be configured via TRACE_JWT_SECRET or profile-specific configuration");
        }
        byte[] secretBytes = secret.getBytes(StandardCharsets.UTF_8);
        if (secretBytes.length < 32) {
            throw new IllegalStateException("jwt.secret must be at least 32 bytes for HS256 signing");
        }
        this.secretKey = Keys.hmacShaKeyFor(secretBytes);
        this.defaultExpiration = Duration.ofHours(expirationHours);
        this.rememberExpiration = Duration.ofDays(rememberExpirationDays);
        
        log.info("JwtUtil initialized: defaultExpiration={}h, rememberExpiration={}d", 
                expirationHours, rememberExpirationDays);
    }

    /**
     * 生成 JWT Token
     *
     * @param username     用户名
     * @param role         用户角色
     * @param tokenVersion 用户Token版本号（用于强制失效）
     * @param rememberMe   是否记住登录（true: 1天过期，false: 2小时过期）
     * @return JWT Token 字符串
     */
    public String generateToken(String username, String role, Integer tokenVersion, boolean rememberMe) {
        Duration expiration = rememberMe ? rememberExpiration : defaultExpiration;
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expiration.toMillis());
        String jti = UUID.randomUUID().toString();

        return Jwts.builder()
                .id(jti)  // JWT ID，用于黑名单
                .subject(username)
                .claim(CLAIM_USERNAME, username)
                .claim(CLAIM_ROLE, role)
                .claim(CLAIM_TOKEN_VERSION, tokenVersion != null ? tokenVersion : 0)
                .claim(CLAIM_JTI, jti)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(secretKey, Jwts.SIG.HS256)
                .compact();
    }

    /**
     * 从 Token 中解析用户名
     */
    public String getUsernameFromToken(String token) {
        Claims claims = parseToken(token);
        return claims != null ? claims.getSubject() : null;
    }

    /**
     * 从 Token 中解析角色
     */
    public String getRoleFromToken(String token) {
        Claims claims = parseToken(token);
        return claims != null ? claims.get(CLAIM_ROLE, String.class) : null;
    }

    /**
     * 从 Token 中解析 Token 版本号
     */
    public Integer getTokenVersionFromToken(String token) {
        Claims claims = parseToken(token);
        if (claims == null) {
            return null;
        }
        Integer version = claims.get(CLAIM_TOKEN_VERSION, Integer.class);
        return version != null ? version : 0;
    }

    /**
     * 从 Token 中解析 JTI (JWT ID)
     */
    public String getJtiFromToken(String token) {
        Claims claims = parseToken(token);
        return claims != null ? claims.getId() : null;
    }

    /**
     * 验证 Token 是否有效
     *
     * @param token JWT Token
     * @return true=有效, false=无效或已过期
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (ExpiredJwtException e) {
            log.warn("JWT Token expired");
        } catch (MalformedJwtException e) {
            log.warn("JWT Token malformed");
        } catch (io.jsonwebtoken.security.SignatureException e) {
            log.warn("JWT signature verification failed (SignatureException)");
        } catch (SecurityException e) {
            log.warn("JWT signature verification failed (SecurityException)");
        } catch (IllegalArgumentException e) {
            log.warn("JWT Token is empty or invalid");
        } catch (JwtException e) {
            log.warn("JWT validation exception: {}", e.getClass().getSimpleName());
        } catch (Exception e) {
            log.error("Unexpected JWT validation exception: {}", e.getClass().getName());
        }
        return false;
    }

    /**
     * 检查 Token 是否即将过期（用于刷新判断）
     *
     * @param token     JWT Token
     * @param threshold 过期阈值（如剩余1小时内需刷新）
     * @return true=即将过期
     */
    public boolean isTokenExpiringSoon(String token, Duration threshold) {
        Claims claims = parseToken(token);
        if (claims == null) {
            return true;
        }
        Date expiration = claims.getExpiration();
        long remainingMillis = expiration.getTime() - System.currentTimeMillis();
        return remainingMillis > 0 && remainingMillis < threshold.toMillis();
    }

    /**
     * 获取 Token 的剩余过期时间（秒）
     */
    public long getRemainingSeconds(String token) {
        Claims claims = parseToken(token);
        if (claims == null) {
            return 0;
        }
        Date expiration = claims.getExpiration();
        long remainingMillis = expiration.getTime() - System.currentTimeMillis();
        return remainingMillis > 0 ? remainingMillis / 1000 : 0;
    }

    /**
     * 刷新 Token（签发新 Token，保持原有信息）
     *
     * @param oldToken     原 Token
     * @param tokenVersion 新的 Token 版本号
     * @param rememberMe   是否记住登录
     * @return 新 Token，如果原 Token 无效返回 null
     */
    public String refreshToken(String oldToken, Integer tokenVersion, boolean rememberMe) {
        Claims claims = parseToken(oldToken);
        if (claims == null) {
            return null;
        }
        String username = claims.getSubject();
        String role = claims.get(CLAIM_ROLE, String.class);
        return generateToken(username, role, tokenVersion, rememberMe);
    }

    /**
     * 刷新 Token（向后兼容，不传 tokenVersion）
     */
    public String refreshToken(String oldToken, boolean rememberMe) {
        Claims claims = parseToken(oldToken);
        if (claims == null) {
            return null;
        }
        Integer version = claims.get(CLAIM_TOKEN_VERSION, Integer.class);
        return refreshToken(oldToken, version != null ? version : 0, rememberMe);
    }

    /**
     * 获取 Token 的过期时间
     */
    public Date getExpirationFromToken(String token) {
        Claims claims = parseToken(token);
        return claims != null ? claims.getExpiration() : null;
    }

    /**
     * 解析 Token 获取 Claims
     */
    private Claims parseToken(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (JwtException e) {
            log.debug("Failed to parse JWT claims: {}", e.getClass().getSimpleName());
            return null;
        }
    }
}
