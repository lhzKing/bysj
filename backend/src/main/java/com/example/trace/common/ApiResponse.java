package com.example.trace.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

/**
 * 统一响应结构 - 符合 RESTful API 规范
 * code: 业务码（0 表示成功，其它表示业务错误）
 * status: HTTP 状态码
 * message: 可读提示
 * data: 真实业务数据（失败时为 null）
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    private static final String DEFAULT_SUCCESS_MESSAGE = "success";
    private static final String DEFAULT_CREATED_MESSAGE = "创建成功";

    private int code;
    private int status;
    private String message;
    private T data;

    private ApiResponse() {}

    private ApiResponse(int code, int status, String message, T data) {
        this.code = code;
        this.status = status;
        this.message = message;
        this.data = data;
    }

    private static <T> ApiResponse<T> of(int code, int status, String message, T data) {
        return new ApiResponse<>(code, status, message, data);
    }

    private static <T> ApiResponse<T> successOf(int status, String message, T data) {
        return of(BizCode.SUCCESS, status, message, data);
    }

    private static <T> ApiResponse<T> failureOf(int code, int status, String message) {
        return of(code, status, message, null);
    }

    // ========== 成功响应 ==========

    public static <T> ApiResponse<T> success(T data) {
        return success(data, DEFAULT_SUCCESS_MESSAGE);
    }

    public static <T> ApiResponse<T> success(T data, String message) {
        return successOf(BizCode.httpStatusOf(BizCode.SUCCESS), message, data);
    }

    @Deprecated
    public static <T> ApiResponse<T> ok(T data) {
        return success(data);
    }

    @Deprecated
    public static <T> ApiResponse<T> ok(T data, String message) {
        return success(data, message);
    }

    public static <T> ApiResponse<T> created(T data) {
        return created(data, DEFAULT_CREATED_MESSAGE);
    }

    public static <T> ApiResponse<T> created(T data, String message) {
        return successOf(201, message, data);
    }

    // ========== 失败响应 ==========

    public static <T> ApiResponse<T> fail(int code, int status, String message) {
        return failureOf(code, status, message);
    }

    public static <T> ApiResponse<T> fail(int code, String message) {
        return failureOf(code, BizCode.httpStatusOf(code), message);
    }

    public static <T> ApiResponse<T> badRequest(String message) {
        return badRequest(BizCode.PARAM_ERROR, message);
    }

    public static <T> ApiResponse<T> badRequest(int code, String message) {
        return failureOf(code, 400, message);
    }

    public static <T> ApiResponse<T> unauthorized(String message) {
        return fail(BizCode.UNAUTHORIZED, message);
    }

    public static <T> ApiResponse<T> forbidden(String message) {
        return fail(BizCode.FORBIDDEN, message);
    }

    public static <T> ApiResponse<T> notFound(String message) {
        return fail(BizCode.NOT_FOUND, message);
    }

    public static <T> ApiResponse<T> serverError(String message) {
        return fail(BizCode.SERVER_ERROR, message);
    }

}
