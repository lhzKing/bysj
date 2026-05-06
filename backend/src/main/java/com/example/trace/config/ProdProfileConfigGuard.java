package com.example.trace.config;

import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.util.Set;

/**
 * Fails application startup early when the production profile is launched with
 * development defaults or missing mandatory security/runtime settings.
 */
public final class ProdProfileConfigGuard implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    static final String LEGACY_DEFAULT_JWT_SECRET = "TraceSystemDefaultSecretKey2024!@#$";
    static final String DEV_DEFAULT_JWT_SECRET = "TraceSystemDevOnlyJwtSecretKey2026!@#";
    static final String TEST_DEFAULT_JWT_SECRET = "TraceSystemTestOnlyJwtSecretKey2026!@#";

    private static final int MIN_JWT_SECRET_BYTES = 32;
    private static final Set<String> FORBIDDEN_JWT_SECRETS = Set.of(
            LEGACY_DEFAULT_JWT_SECRET,
            DEV_DEFAULT_JWT_SECRET,
            TEST_DEFAULT_JWT_SECRET
    );

    @Override
    public void initialize(ConfigurableApplicationContext applicationContext) {
        validate(applicationContext.getEnvironment());
    }

    static void validate(Environment environment) {
        if (!environment.acceptsProfiles(Profiles.of("prod", "production"))) {
            return;
        }

        validateJwtSecret(environment.getProperty("jwt.secret"));
        validateDatasource(environment);
        validateSignatureKeys(environment);
        validateCors(environment);
    }

    private static void validateJwtSecret(String secret) {
        if (!StringUtils.hasText(secret)) {
            throw new IllegalStateException("Production profile requires jwt.secret / TRACE_JWT_SECRET to be explicitly configured");
        }

        String trimmed = secret.trim();
        if (FORBIDDEN_JWT_SECRETS.contains(trimmed) || isPlaceholder(trimmed)) {
            throw new IllegalStateException("Production profile must not use a default or placeholder JWT secret");
        }

        if (trimmed.getBytes(StandardCharsets.UTF_8).length < MIN_JWT_SECRET_BYTES) {
            throw new IllegalStateException("Production JWT secret must be at least 32 bytes");
        }
    }

    private static void validateDatasource(Environment environment) {
        String url = trim(environment.getProperty("spring.datasource.url"));
        String username = trim(environment.getProperty("spring.datasource.username"));
        String password = trim(environment.getProperty("spring.datasource.password"));

        if (!StringUtils.hasText(url) || !StringUtils.hasText(username) || !StringUtils.hasText(password)) {
            throw new IllegalStateException("Production profile requires TRACE_DB_URL, TRACE_DB_USERNAME and TRACE_DB_PASSWORD to be explicitly configured");
        }

        if ("root".equalsIgnoreCase(username) && "root".equals(password)) {
            throw new IllegalStateException("Production profile must not use default root/root database credentials");
        }
    }

    private static void validateSignatureKeys(Environment environment) {
        if (environment.getProperty("trace.signature.auto-generate", Boolean.class, false)) {
            throw new IllegalStateException("Production profile must not enable TRACE_SIGNATURE_AUTO_GENERATE");
        }

        String privateKeyPath = trim(environment.getProperty("trace.signature.private-key-path"));
        String publicKeyPath = trim(environment.getProperty("trace.signature.public-key-path"));
        if (!StringUtils.hasText(privateKeyPath) || !StringUtils.hasText(publicKeyPath)) {
            throw new IllegalStateException("Production profile requires TRACE_SIGNATURE_PRIVATE_KEY_PATH and TRACE_SIGNATURE_PUBLIC_KEY_PATH");
        }
        if (isPlaceholder(privateKeyPath) || isPlaceholder(publicKeyPath)) {
            throw new IllegalStateException("Production signature key paths must not be placeholders");
        }
        if (privateKeyPath.equals(publicKeyPath)) {
            throw new IllegalStateException("Production signature private and public key paths must be different");
        }
        if (isWorkspaceDefaultKeyPath(privateKeyPath) || isWorkspaceDefaultKeyPath(publicKeyPath)) {
            throw new IllegalStateException("Production signature key paths must point to externally mounted files, not backend/keys defaults");
        }

        String keyId = trim(environment.getProperty("trace.signature.key-id"));
        String keyVersionValue = trim(environment.getProperty("trace.signature.key-version"));
        if (!StringUtils.hasText(keyId) || isPlaceholder(keyId)) {
            throw new IllegalStateException("Production profile requires TRACE_SIGNATURE_KEY_ID");
        }
        if (!StringUtils.hasText(keyVersionValue)) {
            throw new IllegalStateException("Production profile requires positive TRACE_SIGNATURE_KEY_VERSION");
        }
        try {
            if (Integer.parseInt(keyVersionValue) < 1) {
                throw new IllegalStateException("Production profile requires positive TRACE_SIGNATURE_KEY_VERSION");
            }
        } catch (NumberFormatException e) {
            throw new IllegalStateException("Production profile requires positive TRACE_SIGNATURE_KEY_VERSION", e);
        }
    }

    private static boolean isWorkspaceDefaultKeyPath(String path) {
        String normalized = path.replace("\\", "/").toLowerCase();
        // endsWith 已经覆盖 "d:/bysj/backend/keys/*.pem" 这类绝对路径——绝对路径自然以 "/backend/keys/*.pem" 结尾。
        // 不再单独写 equals 硬编码工作区，避免开发主机路径泄露到生产代码。
        return normalized.endsWith("/backend/keys/private_key.pem")
                || normalized.endsWith("/backend/keys/public_key.pem");
    }

    /**
     * 生产环境 CORS 必填校验。
     * <ul>
     *   <li>{@code cors.allowed-origins} 与 {@code cors.allowed-origin-patterns} 二者至少有一个非空，否则浏览器前端跨域调用会全部失败；</li>
     *   <li>不允许仅用 {@code *} 单独全开，避免 Origin 完全无防护；</li>
     *   <li>不接受占位符（如 {@code <replace-with-...>} / {@code changeme}）。</li>
     * </ul>
     */
    private static void validateCors(Environment environment) {
        String allowedOrigins = trim(environment.getProperty("cors.allowed-origins"));
        String allowedPatterns = trim(environment.getProperty("cors.allowed-origin-patterns"));

        boolean originsBlank = !StringUtils.hasText(allowedOrigins) || isPlaceholder(allowedOrigins);
        boolean patternsBlank = !StringUtils.hasText(allowedPatterns) || isPlaceholder(allowedPatterns);

        if (originsBlank && patternsBlank) {
            throw new IllegalStateException(
                    "Production profile requires TRACE_CORS_ALLOWED_ORIGINS or TRACE_CORS_ALLOWED_ORIGIN_PATTERNS to be explicitly configured");
        }

        if (containsWildcardOnly(allowedOrigins) || containsWildcardOnly(allowedPatterns)) {
            throw new IllegalStateException(
                    "Production profile must not use wildcard '*' as the sole CORS allowed origin");
        }
    }

    /**
     * 判断逗号分隔的 CORS 列表是否仅由 {@code *} 项组成（视为完全开放）。
     * 空字符串与非通配 entry 都返回 false。
     */
    private static boolean containsWildcardOnly(String value) {
        if (!StringUtils.hasText(value)) {
            return false;
        }
        String[] entries = value.split(",");
        boolean sawWildcard = false;
        for (String entry : entries) {
            String trimmed = entry.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            if ("*".equals(trimmed)) {
                sawWildcard = true;
            } else {
                return false;
            }
        }
        return sawWildcard;
    }

    private static boolean isPlaceholder(String value) {
        String normalized = value.toLowerCase();
        return normalized.startsWith("<")
                || normalized.contains("replace-with")
                || normalized.contains("change_me")
                || normalized.equals("changeme");
    }

    private static String trim(String value) {
        return value == null ? null : value.trim();
    }
}