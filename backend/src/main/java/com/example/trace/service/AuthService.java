package com.example.trace.service;

import com.example.trace.dto.ChangePasswordRequest;
import com.example.trace.dto.LoginRequest;
import com.example.trace.dto.LoginResponse;
import com.example.trace.dto.RegisterRequest;
import com.example.trace.dto.UserInfoResponse;

/**
 * 认证服务接口。
 * 将登录、注册、刷新、登出、当前用户、改密等认证业务从 Controller 中下沉。
 */
public interface AuthService {

    LoginResponse login(LoginRequest request);

    LoginResponse register(RegisterRequest request);

    void logout(String authorizationHeader);

    LoginResponse refreshToken(String authorizationHeader, boolean rememberMe);

    UserInfoResponse getCurrentUser(String username, Long roleId);

    void changePassword(String username, String authorizationHeader, ChangePasswordRequest request);
}
