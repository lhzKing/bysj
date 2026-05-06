package com.example.trace.security;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.example.trace.entity.SysUser;
import com.example.trace.mapper.SysUserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LoginInterceptorTest {

    @Mock private JwtUtil jwtUtil;
    @Mock private TokenStore tokenStore;
    @Mock private PermissionService permissionService;
    @Mock private SysUserMapper userMapper;

    private LoginInterceptor interceptor;

    @BeforeEach
    void setUp() {
        interceptor = new LoginInterceptor(jwtUtil, tokenStore, permissionService, userMapper);
    }

    @Test
    void preHandle_shouldResolveRoleIdAndPopulateRequestAttributes() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer jwt-token");
        MockHttpServletResponse response = new MockHttpServletResponse();

        SysUser user = new SysUser();
        user.setId(7L);
        user.setUsername("alice");
        user.setStatus(1);
        user.setTokenVersion(2);

        when(tokenStore.isBlacklisted("jwt-token")).thenReturn(false);
        when(jwtUtil.validateToken("jwt-token")).thenReturn(true);
        when(jwtUtil.getUsernameFromToken("jwt-token")).thenReturn("alice");
        when(jwtUtil.getRoleFromToken("jwt-token")).thenReturn("ADMIN");
        when(jwtUtil.getTokenVersionFromToken("jwt-token")).thenReturn(2);
        when(userMapper.selectUserWithRole("alice")).thenReturn(user);
        when(permissionService.getRoleIdByCode("ADMIN")).thenReturn(8L);

        assertThat(interceptor.preHandle(request, response, new Object())).isTrue();
        assertThat(request.getAttribute("username")).isEqualTo("alice");
        assertThat(request.getAttribute("userId")).isEqualTo(7L);
        assertThat(request.getAttribute("role")).isEqualTo("ADMIN");
        assertThat(request.getAttribute("roleId")).isEqualTo(8L);
    }

    @Test
    void preHandle_shouldNotLogRawAuthorizationHeaderOrBearerToken() throws Exception {
        Logger logger = (Logger) LoggerFactory.getLogger(LoginInterceptor.class);
        Level originalLevel = logger.getLevel();
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        logger.setLevel(Level.DEBUG);

        try {
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.addHeader("Authorization", "Bearer raw-secret-token");
            MockHttpServletResponse response = new MockHttpServletResponse();

            when(jwtUtil.validateToken("raw-secret-token")).thenReturn(false);

            assertThat(interceptor.preHandle(request, response, new Object())).isFalse();

            String logs = appender.list.stream()
                    .map(ILoggingEvent::getFormattedMessage)
                    .collect(Collectors.joining("\n"));

            assertThat(logs).contains("Auth header metadata: present=true, bearerScheme=true");
            assertThat(logs).doesNotContain("raw-secret-token");
            assertThat(logs).doesNotContain("Bearer raw-secret-token");
            assertThat(logs).doesNotContain("Authorization header: [");
        } finally {
            logger.detachAppender(appender);
            logger.setLevel(originalLevel);
            appender.stop();
        }
    }

    @Test
    void preHandle_shouldFailClosedWhenTokenStoreUnavailable() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer jwt-token");
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(jwtUtil.validateToken("jwt-token")).thenReturn(true);
        when(tokenStore.isBlacklisted("jwt-token"))
                .thenThrow(new TokenStoreException("blacklist-check", "Redis unavailable"));

        assertThat(interceptor.preHandle(request, response, new Object())).isFalse();
        assertThat(response.getStatus()).isEqualTo(503);
        assertThat(response.getContentAsString()).contains("认证状态存储暂不可用");

        verify(jwtUtil).validateToken("jwt-token");
        verifyNoInteractions(userMapper);
        verifyNoInteractions(permissionService);
    }
}
