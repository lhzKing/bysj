package com.example.trace.common;

import com.example.trace.security.TokenStoreException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.util.stream.Collectors;

/**
 * 全局异常处理器 - 符合 RESTful API 规范
 * 确保所有错误响应结构统一：{ code, status, message, data: null }
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * 业务异常（自定义）
     */
    @ExceptionHandler(BizException.class)
    public ResponseEntity<ApiResponse<Void>> handleBizException(BizException e) {
        log.warn("业务异常: code={}, status={}, message={}", e.getCode(), e.getHttpStatus(), e.getMessage());
        ApiResponse<Void> errorResponse = ApiResponse.fail(e.getCode(), e.getHttpStatus(), e.getMessage());
        return ResponseEntity
                .status(e.getHttpStatus())
                .body(errorResponse);
    }

    /**
     * Token 黑名单存储异常。
     *
     * <p>Redis 黑名单是认证安全依赖；不可用时返回 503，避免认证链路 fail-open。</p>
     */
    @ExceptionHandler(TokenStoreException.class)
    public ResponseEntity<ApiResponse<Void>> handleTokenStoreException(TokenStoreException e) {
        log.error("认证状态存储异常: operation={}, cause={}",
                e.getOperation(),
                e.getCause() != null ? e.getCause().getClass().getSimpleName() : "none");
        ApiResponse<Void> errorResponse = ApiResponse.fail(
                BizCode.SERVER_ERROR,
                503,
                "认证状态存储暂不可用，请稍后重试"
        );
        return ResponseEntity
                .status(503)
                .body(errorResponse);
    }

    /**
     * 参数校验失败
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException e) {
        String msg = e.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .collect(Collectors.joining("; "));
        log.warn("参数校验失败: {}", msg);
        return buildErrorResponse(BizCode.PARAM_ERROR, msg.isBlank() ? "参数校验失败" : msg);
    }

    /**
     * 非法参数
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalArgument(IllegalArgumentException e) {
        log.warn("非法参数: {}", e.getMessage());
        return buildErrorResponse(BizCode.PARAM_ERROR, e.getMessage());
    }

    /**
     * JSON 解析错误（包括枚举反序列化失败）
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleHttpMessageNotReadable(HttpMessageNotReadableException e) {
        String message = "请求体格式错误";
        Throwable cause = e.getCause();
        
        // 提取更友好的错误信息
        if (cause != null) {
            String causeMsg = cause.getMessage();
            if (causeMsg != null) {
                // 检查是否是枚举解析错误
                if (causeMsg.contains("ActionType") || causeMsg.contains("非法的 ActionType")) {
                    message = "非法的 ActionType 值，允许的值: INIT, PRINT_CODE, REPRINT_CODE, ACTIVATE_CODE, VOID_CODE, PACK, UNPACK, PALLETIZE, UNPALLETIZE, INBOUND, OUTBOUND, TRANSFER, EXCEPTION, EXCEPTION_OPEN, EXCEPTION_CLOSE, CORRECTION";
                } else if (causeMsg.contains("Cannot deserialize")) {
                    // 提取枚举相关的错误信息
                    int fromIndex = causeMsg.indexOf("from String");
                    if (fromIndex > 0) {
                        int valueStart = causeMsg.indexOf("\"", fromIndex);
                        int valueEnd = causeMsg.indexOf("\"", valueStart + 1);
                        if (valueStart > 0 && valueEnd > valueStart) {
                            String invalidValue = causeMsg.substring(valueStart + 1, valueEnd);
                            message = "非法的参数值: '" + invalidValue + "'";
                        }
                    }
                }
            }
        }
        
        log.warn("请求体解析错误: {}", e.getMessage());
        return buildErrorResponse(BizCode.PARAM_ERROR, message);
    }

    /**
     * 资源不存在（404）
     */
    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNotFound(NoHandlerFoundException e) {
        log.warn("资源不存在: {}", e.getRequestURL());
        return buildErrorResponse(BizCode.NOT_FOUND, "资源不存在: " + e.getRequestURL());
    }

    /**
     * 其它未知异常
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleOther(Exception e) {
        log.error("服务器异常: {} - {}", e.getClass().getSimpleName(), e.getMessage(), e);
        return buildErrorResponse(BizCode.SERVER_ERROR, "服务器异常: " + e.getClass().getSimpleName());
    }

    private ResponseEntity<ApiResponse<Void>> buildErrorResponse(int code, String message) {
        ApiResponse<Void> errorResponse = ApiResponse.fail(code, message);
        return ResponseEntity
                .status(errorResponse.getStatus())
                .body(errorResponse);
    }
}
