package com.example.trace.common;

import lombok.Getter;

/**
 * 业务异常类 - 用于抛出带业务码的异常
 */
@Getter
public class BizException extends RuntimeException {

    private final int code;
    private final int httpStatus;

    public BizException(int code, int httpStatus, String message) {
        super(message);
        this.code = code;
        this.httpStatus = httpStatus;
    }

    public BizException(int code, int httpStatus, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
        this.httpStatus = httpStatus;
    }

    /**
     * 便捷构造函数 - 根据业务码自动推断 HTTP 状态码
     */
    public BizException(int code, String message) {
        super(message);
        this.code = code;
        this.httpStatus = BizCode.httpStatusOf(code);
    }

    public BizException(int code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
        this.httpStatus = BizCode.httpStatusOf(code);
    }

    // ========== 便捷工厂方法 ==========

    public static BizException badRequest(String message) {
        return new BizException(BizCode.PARAM_ERROR, message);
    }

    public static BizException badRequest(int code, String message) {
        return new BizException(code, 400, message);
    }

    public static BizException unauthorized(String message) {
        return new BizException(BizCode.UNAUTHORIZED, message);
    }

    public static BizException forbidden(String message) {
        return new BizException(BizCode.FORBIDDEN, message);
    }

    public static BizException notFound(String message) {
        return new BizException(BizCode.NOT_FOUND, message);
    }

    public static BizException notFound(int code, String message) {
        return new BizException(code, 404, message);
    }

    public static BizException serverError(String message) {
        return new BizException(BizCode.SERVER_ERROR, message);
    }
}
