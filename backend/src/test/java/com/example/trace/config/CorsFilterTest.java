package com.example.trace.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CorsFilterTest {

    @Test
    void doFilter_shouldWriteCorsHeadersForAllowedOriginAndContinueActualRequest() throws Exception {
        CorsFilter filter = corsFilter(
                List.of("http://localhost:5173"),
                List.of(),
                true
        );
        MockHttpServletRequest request = request("GET", "/api/auth/me", "http://localhost:5173");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(response.getHeader("Access-Control-Allow-Origin")).isEqualTo("http://localhost:5173");
        assertThat(response.getHeader("Access-Control-Allow-Credentials")).isEqualTo("true");
        assertThat(response.getHeader("Access-Control-Allow-Methods")).isEqualTo("GET, POST, PUT, PATCH, DELETE, OPTIONS");
        assertThat(response.getHeader("Access-Control-Allow-Headers")).isEqualTo("Authorization, Content-Type, Accept, X-Requested-With");
        assertThat(response.getHeader("Access-Control-Expose-Headers")).isEqualTo("Authorization");
        assertThat(response.getHeader("Access-Control-Max-Age")).isEqualTo("3600");
        assertThat(chain.getRequest()).isSameAs(request);
    }

    @Test
    void doFilter_shouldShortCircuitOptionsPreflightBeforeInterceptors() throws Exception {
        CorsFilter filter = corsFilter(
                List.of(),
                List.of("https://192.168.*:5173"),
                true
        );
        MockHttpServletRequest request = request("OPTIONS", "/api/users", "https://192.168.2.10:5173");
        request.addHeader("Access-Control-Request-Method", "POST");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(response.getHeader("Access-Control-Allow-Origin")).isEqualTo("https://192.168.2.10:5173");
        assertThat(chain.getRequest()).isNull();
    }

    @Test
    void doFilter_shouldNotWriteCorsHeadersForRejectedOrigin() throws Exception {
        CorsFilter filter = corsFilter(
                List.of("http://localhost:5173"),
                List.of("https://192.168.*:5173"),
                true
        );
        MockHttpServletRequest request = request("GET", "/api/auth/me", "https://evil.example.com");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(response.getHeader("Access-Control-Allow-Origin")).isNull();
        assertThat(response.getHeader("Access-Control-Allow-Credentials")).isNull();
        assertThat(chain.getRequest()).isSameAs(request);
    }

    private CorsFilter corsFilter(List<String> allowedOrigins, List<String> allowedOriginPatterns, boolean allowCredentials) {
        CorsProperties properties = new CorsProperties();
        properties.setAllowedOrigins(allowedOrigins);
        properties.setAllowedOriginPatterns(allowedOriginPatterns);
        properties.setAllowCredentials(allowCredentials);
        return new CorsFilter(properties, new CorsOriginMatcher());
    }

    private MockHttpServletRequest request(String method, String path, String origin) {
        MockHttpServletRequest request = new MockHttpServletRequest(method, path);
        request.addHeader("Origin", origin);
        return request;
    }
}
