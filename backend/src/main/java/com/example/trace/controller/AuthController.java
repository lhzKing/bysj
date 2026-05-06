package com.example.trace.controller;

import com.example.trace.common.ApiResponse;
import com.example.trace.dto.ChangePasswordRequest;
import com.example.trace.dto.LoginRequest;
import com.example.trace.dto.LoginResponse;
import com.example.trace.dto.RegisterRequest;
import com.example.trace.dto.UserInfoResponse;
import com.example.trace.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 认证控制器 - RESTful API (JWT + BCrypt)
 * 路径前缀: /api/auth
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /**
     * 用户登录
     * POST /api/auth/login
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@RequestBody @Valid LoginRequest req) {
        return ResponseEntity.ok(ApiResponse.success(authService.login(req), "登录成功"));
    }

    /**
     * 用户注册
     * POST /api/auth/register
     */
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<LoginResponse>> register(@RequestBody @Valid RegisterRequest req) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.created(
                        authService.register(req),
                        "注册成功"
                ));
    }

    /**
     * 用户登出
     * POST /api/auth/logout
     */
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(HttpServletRequest request) {
        authService.logout(request.getHeader("Authorization"));
        return ResponseEntity.ok(ApiResponse.success(null, "登出成功"));
    }

    /**
     * 刷新 Token
     * POST /api/auth/refresh
     * 
     * 用于在 Token 即将过期时获取新 Token
     */
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<LoginResponse>> refreshToken(
            HttpServletRequest request,
            @RequestParam(name = "remember_me", required = false, defaultValue = "false") Boolean rememberMe
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                authService.refreshToken(request.getHeader("Authorization"), Boolean.TRUE.equals(rememberMe)),
                "Token 刷新成功"
        ));
    }

    /**
     * 获取当前用户信息
     * GET /api/auth/me
     */
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserInfoResponse>> getCurrentUser(HttpServletRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                authService.getCurrentUser(
                        (String) request.getAttribute("username"),
                        (Long) request.getAttribute("roleId")
                ),
                "获取用户信息成功"
        ));
    }

    /**
     * 修改密码
     * PUT /api/auth/password
     */
    @PutMapping("/password")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            HttpServletRequest request,
            @RequestBody @Valid ChangePasswordRequest req
    ) {
        authService.changePassword(
                (String) request.getAttribute("username"),
                request.getHeader("Authorization"),
                req
        );
        return ResponseEntity.ok(ApiResponse.success(null, "密码修改成功，请重新登录"));
    }
}
