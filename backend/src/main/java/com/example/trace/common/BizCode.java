package com.example.trace.common;

/**
 * 业务错误码常量
 * 规范：
 * - 0 表示成功
 * - 10xxx 表示通用错误（参数/认证/权限等）
 * - 20xxx 表示溯源业务错误
 * - 30xxx 表示Dashboard业务错误
 */
public final class BizCode {

    private BizCode() {}

    // ========== 成功 ==========
    public static final int SUCCESS = 0;

    // ========== 通用错误 10xxx ==========
    public static final int PARAM_ERROR = 10001;          // 参数校验失败
    public static final int UNAUTHORIZED = 10002;         // 未授权/登录失效
    public static final int FORBIDDEN = 10003;            // 操作被拒绝
    public static final int NOT_FOUND = 10004;            // 资源不存在
    public static final int SERVER_ERROR = 10005;         // 服务器内部错误
    public static final int BAD_REQUEST = 10006;          // 请求参数错误
    public static final int CONFLICT = 10007;             // 资源冲突（如重复创建）
    public static final int CONCURRENT_CONFLICT = 10008;  // 并发冲突（乐观锁失败）

    // ========== 认证相关 11xxx ==========
    public static final int USER_NOT_FOUND = 11001;       // 用户不存在
    public static final int PASSWORD_ERROR = 11002;       // 密码错误
    public static final int USER_EXISTS = 11003;          // 用户名已存在
    public static final int TOKEN_INVALID = 11004;        // Token无效

    // ========== 溯源业务 20xxx ==========
    public static final int TRACE_NOT_FOUND = 20001;      // 溯源码不存在
    public static final int TRACE_ALREADY_EXISTS = 20002; // 溯源码已存在
    public static final int INVALID_ACTION_TYPE = 20003;  // 无效的操作类型
    public static final int SPU_NOT_FOUND = 20004;        // 配件规格不存在
    public static final int HASH_CHAIN_BROKEN = 20005;    // 哈希链断裂
    public static final int CORRECTION_TARGET_NOT_FOUND = 20006; // 修正目标不存在

    // ========== Dashboard业务 30xxx ==========
    public static final int DASHBOARD_QUERY_ERROR = 30001; // Dashboard查询失败

    /**
     * 根据业务码推断默认 HTTP 状态码。
     */
    public static int httpStatusOf(int code) {
        return switch (code) {
            case SUCCESS -> 200;
            case PARAM_ERROR, BAD_REQUEST, INVALID_ACTION_TYPE -> 400;
            case UNAUTHORIZED, USER_NOT_FOUND, PASSWORD_ERROR, TOKEN_INVALID -> 401;
            case FORBIDDEN -> 403;
            case NOT_FOUND, TRACE_NOT_FOUND, SPU_NOT_FOUND, CORRECTION_TARGET_NOT_FOUND -> 404;
            case CONFLICT, CONCURRENT_CONFLICT, USER_EXISTS, TRACE_ALREADY_EXISTS -> 409;
            case SERVER_ERROR, DASHBOARD_QUERY_ERROR -> 500;
            default -> 400;
        };
    }
}
