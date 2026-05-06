package com.example.trace.controller;

import com.example.trace.common.BizCode;
import com.example.trace.common.BizException;
import com.example.trace.dto.ChangePasswordRequest;
import com.example.trace.dto.LoginRequest;
import com.example.trace.dto.LoginResponse;
import com.example.trace.dto.RegisterRequest;
import com.example.trace.dto.UserInfoResponse;
import com.example.trace.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock private AuthService authService;

    @InjectMocks
    private AuthController authController;

    @Test
    void login_shouldDelegateToAuthService() {
        LoginRequest request = new LoginRequest();
        request.setUsername("alice");
        request.setPassword("secret");
        request.setRememberMe(true);

        LoginResponse serviceResponse = new LoginResponse("jwt-token", "alice", "ADMIN", java.util.List.of("trace:view"));
        when(authService.login(request)).thenReturn(serviceResponse);

        ResponseEntity<?> response = authController.login(request);
        Object body = response.getBody();
        assertThat(body).isNotNull();
        LoginResponse data = (LoginResponse) ((com.example.trace.common.ApiResponse<?>) body).getData();
        assertThat(data.getToken()).isEqualTo("jwt-token");
        assertThat(data.getPermissions()).containsExactly("trace:view");
        verify(authService).login(request);
    }

    @Test
    void register_shouldWrapCreatedResponseFromAuthService() {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("new_user");
        request.setPassword("abc123");

        LoginResponse serviceResponse = new LoginResponse("register-token", "new_user", "USER", java.util.List.of());
        when(authService.register(request)).thenReturn(serviceResponse);

        ResponseEntity<?> response = authController.register(request);

        assertThat(response.getStatusCode().value()).isEqualTo(201);
        Object body = response.getBody();
        assertThat(body).isNotNull();
        LoginResponse data = (LoginResponse) ((com.example.trace.common.ApiResponse<?>) body).getData();
        assertThat(data.getToken()).isEqualTo("register-token");
        assertThat(data.getRole()).isEqualTo("USER");
        verify(authService).register(request);
    }

    @Test
    void changePassword_shouldDelegateUsernameAndHeaderToAuthService() {
        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setOldPassword("old-pass");
        request.setNewPassword("new-pass");

        MockHttpServletRequest servletRequest = new MockHttpServletRequest();
        servletRequest.setAttribute("username", "alice");
        servletRequest.addHeader("Authorization", "Bearer token-123");

        authController.changePassword(servletRequest, request);

        verify(authService).changePassword("alice", "Bearer token-123", request);
    }

    @Test
    void logout_shouldDelegateAuthorizationHeaderToAuthService() {
        MockHttpServletRequest servletRequest = new MockHttpServletRequest();
        servletRequest.addHeader("Authorization", "Bearer token-123");

        ResponseEntity<?> response = authController.logout(servletRequest);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        verify(authService).logout("Bearer token-123");
    }

    @Test
    void refreshToken_shouldDelegateHeaderAndRememberMeToAuthService() {
        MockHttpServletRequest servletRequest = new MockHttpServletRequest();
        servletRequest.addHeader("Authorization", "Bearer old-token");

        LoginResponse serviceResponse = new LoginResponse("new-token", "alice", "ADMIN", java.util.List.of());
        when(authService.refreshToken("Bearer old-token", true)).thenReturn(serviceResponse);

        ResponseEntity<?> response = authController.refreshToken(servletRequest, true);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        Object body = response.getBody();
        assertThat(body).isNotNull();
        LoginResponse data = (LoginResponse) ((com.example.trace.common.ApiResponse<?>) body).getData();
        assertThat(data.getToken()).isEqualTo("new-token");
        verify(authService).refreshToken("Bearer old-token", true);
    }

    @Test
    void getCurrentUser_shouldDelegateRequestAttributesToAuthService() {
        MockHttpServletRequest servletRequest = new MockHttpServletRequest();
        servletRequest.setAttribute("username", "alice");
        servletRequest.setAttribute("roleId", 8L);

        UserInfoResponse serviceResponse = new UserInfoResponse();
        serviceResponse.setUsername("alice");
        serviceResponse.setRoleCode("ADMIN");
        when(authService.getCurrentUser("alice", 8L)).thenReturn(serviceResponse);

        ResponseEntity<?> response = authController.getCurrentUser(servletRequest);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        Object body = response.getBody();
        assertThat(body).isNotNull();
        UserInfoResponse data = (UserInfoResponse) ((com.example.trace.common.ApiResponse<?>) body).getData();
        assertThat(data.getUsername()).isEqualTo("alice");
        verify(authService).getCurrentUser("alice", 8L);
    }

    @Test
    void authController_shouldPropagateBizExceptionsFromAuthService() {
        LoginRequest request = new LoginRequest();
        request.setUsername("alice");
        request.setPassword("secret");
        request.setRememberMe(true);

        when(authService.login(request)).thenThrow(new BizException(BizCode.PASSWORD_ERROR, "用户名或密码错误"));

        assertThatThrownBy(() -> authController.login(request))
                .isInstanceOf(BizException.class)
                .hasMessage("用户名或密码错误");
    }
}
