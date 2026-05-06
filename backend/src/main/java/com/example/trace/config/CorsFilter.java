package com.example.trace.config;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * CORS 过滤器 - 统一处理跨域请求。
 *
 * <p>这是项目唯一的 CORS 写入点，优先级高于所有拦截器，确保 OPTIONS 预检请求、
 * 登录/权限拦截失败响应和控制器异常响应都能得到一致的 CORS Header。</p>
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorsFilter implements Filter {

    private static final Logger log = LoggerFactory.getLogger(CorsFilter.class);
    private final CorsProperties corsProperties;
    private final CorsOriginMatcher corsOriginMatcher;

    public CorsFilter(CorsProperties corsProperties, CorsOriginMatcher corsOriginMatcher) {
        this.corsProperties = corsProperties;
        this.corsOriginMatcher = corsOriginMatcher;
    }

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain) 
            throws IOException, ServletException {
        
        HttpServletRequest request = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) res;
        
        String origin = request.getHeader("Origin");
        String method = request.getMethod();
        
        log.debug("CORS Filter: method={}, origin={}, path={}", method, origin, request.getRequestURI());
        
        // 仅对携带 Origin 的跨域请求写入 CORS 头
        if (origin != null) {
            if (isOriginAllowed(origin)) {
                response.setHeader("Access-Control-Allow-Origin", origin);
                response.setHeader("Access-Control-Allow-Credentials", String.valueOf(corsProperties.isAllowCredentials()));
                response.setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, PATCH, DELETE, OPTIONS");
                response.setHeader("Access-Control-Allow-Headers", "Authorization, Content-Type, Accept, X-Requested-With");
                response.setHeader("Access-Control-Expose-Headers", "Authorization");
                response.setHeader("Access-Control-Max-Age", "3600");

                log.debug("CORS headers set for origin: {}", origin);
            } else {
                log.warn("Origin not allowed: {}", origin);
            }
        }
        
        // OPTIONS 预检请求直接返回 200
        if ("OPTIONS".equalsIgnoreCase(method)) {
            response.setStatus(HttpServletResponse.SC_OK);
            log.debug("OPTIONS request handled, returning 200");
            return;
        }
        
        chain.doFilter(request, response);
    }
    
    /**
     * 按外置化的 CORS 配置检查 Origin 是否允许访问。
     */
    private boolean isOriginAllowed(String origin) {
        return corsOriginMatcher.isAllowed(
            origin,
            corsProperties.getAllowedOrigins(),
            corsProperties.getAllowedOriginPatterns()
        );
    }
}
