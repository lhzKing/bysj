package com.example.trace.controller;

import com.example.trace.common.GlobalExceptionHandler;
import com.example.trace.config.JacksonConfig;
import com.example.trace.dto.LoginResponse;
import com.example.trace.dto.PageResponse;
import com.example.trace.dto.PartListRequest;
import com.example.trace.dto.PartResponse;
import com.example.trace.dto.UserListRequest;
import com.example.trace.dto.UserResponse;
import com.example.trace.security.TokenStoreException;
import com.example.trace.service.AuthService;
import com.example.trace.service.DashboardService;
import com.example.trace.service.PartService;
import com.example.trace.service.UserService;
import com.example.trace.service.TraceUserNodeBindingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ControllerQueryBindingTest {

    @Mock
    private DashboardService dashboardService;
    @Mock
    private UserService userService;
    @Mock
    private PartService partService;
    @Mock
    private AuthService authService;
    @Mock
    private TraceUserNodeBindingService traceUserNodeBindingService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(
                        new DashboardController(dashboardService),
                        new UserController(userService, traceUserNodeBindingService),
                        new PartController(partService),
                        new AuthController(authService)
                )
                .setControllerAdvice(new GlobalExceptionHandler())
                .setMessageConverters(new MappingJackson2HttpMessageConverter(new JacksonConfig().objectMapper()))
                .build();
    }

    @Test
    void listUsers_shouldMapSnakeCaseFiltersIntoUserListRequest() throws Exception {
        when(userService.listUsers(any(UserListRequest.class), eq("ADMIN")))
                .thenReturn(PageResponse.of(List.<UserResponse>of(), 0L, 1, 10));

        mockMvc.perform(get("/api/users")
                        .param("username", "alice")
                        .param("role_id", "2")
                        .param("status", "1")
                        .param("page", "1")
                        .param("size", "10")
                        .requestAttr("role", "ADMIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        ArgumentCaptor<UserListRequest> captor = ArgumentCaptor.forClass(UserListRequest.class);
        verify(userService).listUsers(captor.capture(), eq("ADMIN"));
        assertThat(captor.getValue().getUsername()).isEqualTo("alice");
        assertThat(captor.getValue().getRoleId()).isEqualTo(2L);
        assertThat(captor.getValue().getStatus()).isEqualTo(1);
        assertThat(captor.getValue().getPage()).isEqualTo(1);
        assertThat(captor.getValue().getSize()).isEqualTo(10);
    }

    @Test
    void listParts_shouldMapSnakeCaseFiltersIntoPartListRequest() throws Exception {
        when(partService.listParts(any(PartListRequest.class)))
                .thenReturn(PageResponse.of(List.<PartResponse>of(), 0L, 1, 10));

        mockMvc.perform(get("/api/parts")
                        .param("keyword", "bearing")
                        .param("part_type", "Mechanical")
                        .param("manufacturer", "Factory-A")
                        .param("page", "1")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        ArgumentCaptor<PartListRequest> captor = ArgumentCaptor.forClass(PartListRequest.class);
        verify(partService).listParts(captor.capture());
        assertThat(captor.getValue().getKeyword()).isEqualTo("bearing");
        assertThat(captor.getValue().getPartType()).isEqualTo("Mechanical");
        assertThat(captor.getValue().getManufacturer()).isEqualTo("Factory-A");
        assertThat(captor.getValue().getPage()).isEqualTo(1);
        assertThat(captor.getValue().getSize()).isEqualTo(10);
    }

    @Test
    void dashboardTopology_shouldAcceptTraceCodeSnakeCaseQuery() throws Exception {
        when(dashboardService.topology("TRACE-001", "30d"))
                .thenReturn(Map.of("nodes", List.of(), "links", List.of(), "range", "30d"));

        mockMvc.perform(get("/api/dashboard/topology")
                        .param("trace_code", "TRACE-001")
                        .param("range", "30d"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.range").value("30d"));

        verify(dashboardService).topology("TRACE-001", "30d");
    }

    @Test
    void changeUserRole_shouldAcceptRoleIdSnakeCaseQuery() throws Exception {
        when(userService.changeUserRole(7L, 3L, "ADMIN")).thenReturn(new UserResponse());

        mockMvc.perform(patch("/api/users/7/role")
                        .param("role_id", "3")
                        .requestAttr("role", "ADMIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        verify(userService).changeUserRole(7L, 3L, "ADMIN");
    }

    @Test
    void refreshToken_shouldAcceptRememberMeSnakeCaseQuery() throws Exception {
        when(authService.refreshToken("Bearer old-token", true))
                .thenReturn(new LoginResponse("new-token", "alice", "ADMIN", List.of()));

        mockMvc.perform(post("/api/auth/refresh")
                        .header("Authorization", "Bearer old-token")
                        .param("remember_me", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.token").value("new-token"));

        verify(authService).refreshToken("Bearer old-token", true);
    }

    @Test
    void refreshToken_shouldReturnServiceUnavailableWhenBlacklistWriteFails() throws Exception {
        doThrow(new TokenStoreException("blacklist-add", "Redis unavailable"))
                .when(authService)
                .refreshToken("Bearer old-token", true);

        mockMvc.perform(post("/api/auth/refresh")
                        .header("Authorization", "Bearer old-token")
                        .param("remember_me", "true"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value(10005))
                .andExpect(jsonPath("$.message").value("认证状态存储暂不可用，请稍后重试"));
    }
}
