package com.example.trace.annotation;

import java.lang.annotation.*;

/**
 * 权限校验注解
 * 
 * 可用于类或方法级别：
 * - 类级别：该类所有方法都需要指定权限
 * - 方法级别：覆盖类级别配置
 * 
 * 使用示例：
 * <pre>
 * @RequirePermission("user:view")
 * public class UserController { }
 * 
 * @RequirePermission({"user:create", "user:update"})  // 满足任一即可
 * public void createOrUpdate() { }
 * </pre>
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequirePermission {

    /**
     * 需要的权限代码列表（满足任一即可）
     */
    String[] value() default {};

    /**
     * 是否需要全部匹配（默认 false，满足任一即可）
     */
    boolean matchAll() default false;

    /**
     * 是否允许匿名访问（用于放行特定方法）
     */
    boolean allowAnonymous() default false;
}
