package com.example.trace.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 单品码二维码载荷（qr_payload）生成参数。
 *
 * <p>设计动机：前端"打印标签"演示要让评委用手机原生扫码即跳转到溯源详情页，
 * 所以新生成的单品码 qrPayload 不再写裸 traceCode，而是写完整 URL，例如：
 * {@code http://localhost:5173/public/traces/<code>}。
 *
 * <p>{@link #publicBaseUrl} 为空字符串时，生成逻辑回退为裸 traceCode，
 * 保留与历史数据一致的行为。
 */
@ConfigurationProperties(prefix = "trace.qr")
public class TraceQrProperties {

    /**
     * 前端可访问的公开基址（不含尾部斜杠）。空值=不启用 URL 载荷，回退为裸 traceCode。
     */
    private String publicBaseUrl = "";

    /**
     * 公开溯源页相对路径模板，{@code {code}} 占位符在生成时替换为 traceCode。
     * 默认对齐前端路由 {@code /public/traces/:code}。
     */
    private String pathTemplate = "/public/traces/{code}";

    public String getPublicBaseUrl() {
        return publicBaseUrl;
    }

    public void setPublicBaseUrl(String publicBaseUrl) {
        this.publicBaseUrl = publicBaseUrl == null ? "" : publicBaseUrl.trim();
    }

    public String getPathTemplate() {
        return pathTemplate;
    }

    public void setPathTemplate(String pathTemplate) {
        if (pathTemplate == null || pathTemplate.isBlank()) {
            throw new IllegalArgumentException("trace.qr.path-template 不能为空");
        }
        if (!pathTemplate.contains("{code}")) {
            throw new IllegalArgumentException("trace.qr.path-template 必须包含 {code} 占位符");
        }
        this.pathTemplate = pathTemplate;
    }

    /**
     * 按当前配置渲染单个 traceCode 的 qrPayload。空 baseUrl 时直接返回 traceCode。
     */
    public String renderQrPayload(String traceCode) {
        if (traceCode == null || traceCode.isBlank()) {
            return traceCode;
        }
        if (publicBaseUrl.isEmpty()) {
            return traceCode;
        }
        String base = publicBaseUrl.endsWith("/")
                ? publicBaseUrl.substring(0, publicBaseUrl.length() - 1)
                : publicBaseUrl;
        return base + pathTemplate.replace("{code}", traceCode);
    }
}
