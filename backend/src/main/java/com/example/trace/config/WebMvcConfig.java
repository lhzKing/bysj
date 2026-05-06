package com.example.trace.config;

import com.example.trace.security.LoginInterceptor;
import com.example.trace.security.PermissionInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC 配置 - 仅注册业务拦截器。
 *
 * <p>CORS 统一由 {@link CorsFilter} 在 Servlet 过滤器层处理，保证预检请求、
 * 拦截器拒绝响应和控制器异常响应使用同一套跨域规则，避免 MVC CORS 与 Filter
 * CORS 双实现漂移。</p>
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final LoginInterceptor loginInterceptor;
    private final PermissionInterceptor permissionInterceptor;

    public WebMvcConfig(
            LoginInterceptor loginInterceptor,
            PermissionInterceptor permissionInterceptor
    ) {
        this.loginInterceptor = loginInterceptor;
        this.permissionInterceptor = permissionInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 1. 登录拦截器 - 验证 JWT Token
        registry.addInterceptor(loginInterceptor)
                .addPathPatterns("/api/**")
                // 放行认证接口
                .excludePathPatterns(
                        "/api/auth/login",
                        "/api/auth/register",
                        "/api/traces/public-key"  // 公钥接口无需认证
                )
                .order(1);  // 先执行登录验证

        // 2. 权限拦截器 - 校验用户权限
        registry.addInterceptor(permissionInterceptor)
                .addPathPatterns("/api/**")
                // 放行认证接口（登录后即可访问，无需额外权限）
                .excludePathPatterns(
                        "/api/auth/**",
                        "/api/traces/public-key"  // 公钥接口无需权限
                )
                .order(2);  // 登录验证通过后再校验权限
    }
}
