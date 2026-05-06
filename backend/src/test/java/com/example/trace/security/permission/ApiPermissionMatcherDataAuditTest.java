package com.example.trace.security.permission;

import com.example.trace.entity.SysPermission;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Data audit for {@code sys_permission}: locks down the (api_method, api_pattern)
 * rows shipped in {@code init_schema.sql} against the real controller routes.
 *
 * <p>Why this test exists (T-P1-06):
 * <ul>
 *   <li>{@link ApiPermissionMatcher#toAntPattern(String)} expands every {@code *}
 *       in a stored pattern to {@code **}, so a row authored as a single-segment
 *       wildcard implicitly becomes a multi-segment match. The audit confirms
 *       that this is intentional and that no row escalates to an unintended
 *       endpoint.</li>
 *   <li>Path-based matching is a fallback — every controller method currently
 *       carries {@code @RequirePermission}, so the {@code api_pattern} column is
 *       a defense-in-depth safety net rather than the primary check. This
 *       audit is therefore about future-proofing: if an annotation is ever
 *       removed by accident, the fallback must still grant the right perm.</li>
 * </ul>
 */
class ApiPermissionMatcherDataAuditTest {

    private final ApiPermissionMatcher matcher = new ApiPermissionMatcher();

    /** Mirrors INSERTs in init_schema.sql. Keep in lock-step with that file. */
    private static List<SysPermission> seededPermissions() {
        List<SysPermission> rows = new ArrayList<>();
        rows.add(perm("trace:create", "POST", "/api/traces"));
        rows.add(perm("trace:scan", "POST", "/api/traces/*/events"));
        rows.add(perm("trace:view", "GET", "/api/traces/*"));
        rows.add(perm("dashboard:view", "GET", "/api/dashboard/*"));
        rows.add(perm("user:view", "GET", "/api/users/*"));
        rows.add(perm("user:manage", "*", "/api/users/*"));
        rows.add(perm("role:view", "GET", "/api/roles/*"));
        rows.add(perm("role:manage", "*", "/api/roles/*"));
        rows.add(perm("part:view", "GET", "/api/parts/*"));
        rows.add(perm("part:manage", "*", "/api/parts/*"));
        rows.add(perm("trace:data:generate", "POST", "/api/admin/generate-sample-data"));
        rows.add(perm("trace:data:clear", "DELETE", "/api/admin/clear-trace-data"));
        rows.add(perm("trace:inbound", "POST", "/api/traces/*/events"));
        rows.add(perm("trace:outbound", "POST", "/api/traces/*/events"));
        rows.add(perm("trace:transfer", "POST", "/api/traces/*/events"));
        rows.add(perm("trace:audit:view", null, null));
        return rows;
    }

    private static SysPermission perm(String code, String method, String pattern) {
        SysPermission p = new SysPermission();
        p.setPermCode(code);
        p.setApiMethod(method);
        p.setApiPattern(pattern);
        return p;
    }

    private List<String> matchingCodes(String method, String path) {
        List<String> hits = new ArrayList<>();
        for (SysPermission p : seededPermissions()) {
            if (matcher.matches(p, method, path)) {
                hits.add(p.getPermCode());
            }
        }
        return hits;
    }

    @Nested
    @DisplayName("UserController routes")
    class UserRoutes {

        @Test
        void getUsersListMatchesUserViewAndUserManage() {
            // Both user:view (GET) and user:manage (* method) match. Either grants access — by design.
            assertThat(matchingCodes("GET", "/api/users")).containsExactlyInAnyOrder("user:view", "user:manage");
        }

        @Test
        void getUserByIdMatchesUserViewAndUserManage() {
            assertThat(matchingCodes("GET", "/api/users/123")).containsExactlyInAnyOrder("user:view", "user:manage");
        }

        @Test
        void postUserCreateMatchesOnlyUserManage() {
            assertThat(matchingCodes("POST", "/api/users")).containsExactly("user:manage");
        }

        @Test
        void putUserUpdateMatchesOnlyUserManage() {
            assertThat(matchingCodes("PUT", "/api/users/123")).containsExactly("user:manage");
        }

        @Test
        void multiSegmentUserRoutesAllMatchUserManage() {
            // The audit's headline: confirm /api/users/* expands to multi-segment via toAntPattern().
            assertThat(matchingCodes("PATCH", "/api/users/123/role")).containsExactly("user:manage");
            assertThat(matchingCodes("PATCH", "/api/users/123/status")).containsExactly("user:manage");
            assertThat(matchingCodes("POST", "/api/users/123/reset-password")).containsExactly("user:manage");
            assertThat(matchingCodes("DELETE", "/api/users/batch")).containsExactly("user:manage");
            assertThat(matchingCodes("DELETE", "/api/users/123")).containsExactly("user:manage");
        }
    }

    @Nested
    @DisplayName("RoleController routes")
    class RoleRoutes {

        @Test
        void roleListsAndDetailsMatchViewAndManage() {
            assertThat(matchingCodes("GET", "/api/roles")).containsExactlyInAnyOrder("role:view", "role:manage");
            assertThat(matchingCodes("GET", "/api/roles/3")).containsExactlyInAnyOrder("role:view", "role:manage");
            assertThat(matchingCodes("GET", "/api/roles/permissions")).containsExactlyInAnyOrder("role:view", "role:manage");
        }

        @Test
        void writeRoutesMatchOnlyRoleManage() {
            assertThat(matchingCodes("POST", "/api/roles")).containsExactly("role:manage");
            assertThat(matchingCodes("PUT", "/api/roles/3")).containsExactly("role:manage");
            assertThat(matchingCodes("DELETE", "/api/roles/3")).containsExactly("role:manage");
            assertThat(matchingCodes("PUT", "/api/roles/3/permissions")).containsExactly("role:manage");
        }
    }

    @Nested
    @DisplayName("PartController routes")
    class PartRoutes {

        @Test
        void partReadsMatchPartViewAndPartManage() {
            assertThat(matchingCodes("GET", "/api/parts")).containsExactlyInAnyOrder("part:view", "part:manage");
            assertThat(matchingCodes("GET", "/api/parts/9")).containsExactlyInAnyOrder("part:view", "part:manage");
            assertThat(matchingCodes("GET", "/api/parts/code/P-001")).containsExactlyInAnyOrder("part:view", "part:manage");
            assertThat(matchingCodes("GET", "/api/parts/types")).containsExactlyInAnyOrder("part:view", "part:manage");
            assertThat(matchingCodes("GET", "/api/parts/manufacturers")).containsExactlyInAnyOrder("part:view", "part:manage");
        }

        @Test
        void partWritesMatchOnlyPartManage() {
            assertThat(matchingCodes("POST", "/api/parts")).containsExactly("part:manage");
            assertThat(matchingCodes("PUT", "/api/parts/9")).containsExactly("part:manage");
            assertThat(matchingCodes("DELETE", "/api/parts/9")).containsExactly("part:manage");
            assertThat(matchingCodes("DELETE", "/api/parts/batch")).containsExactly("part:manage");
        }
    }

    @Nested
    @DisplayName("TraceController routes")
    class TraceRoutes {

        @Test
        void traceCreateMatchesOnlyTraceCreate() {
            // /api/traces is exact (no wildcard), so only trace:create matches.
            assertThat(matchingCodes("POST", "/api/traces")).containsExactly("trace:create");
        }

        @Test
        void traceDetailAndVerifyMatchOnlyTraceView() {
            assertThat(matchingCodes("GET", "/api/traces/TRACE-001")).containsExactly("trace:view");
            assertThat(matchingCodes("GET", "/api/traces/TRACE-001/verify")).containsExactly("trace:view");
        }

        @Test
        void traceEventsMatchScanAndAllFineGrainedActions() {
            // Path-based fallback grants access if ANY of these perms is held by the role.
            // The fine-grained ActionType filter lives in TraceController.hasPermissionForAction()
            // and must remain there — the path matcher cannot enforce action-level rules.
            assertThat(matchingCodes("POST", "/api/traces/TRACE-001/events"))
                .containsExactlyInAnyOrder("trace:scan", "trace:inbound", "trace:outbound", "trace:transfer");
        }

        @Test
        void getPublicKeyIsExcludedAtInterceptorLevelButPathMatcherWouldGrantTraceView() {
            // GET /api/traces/public-key is excluded in WebMvcConfig (both LoginInterceptor
            // and PermissionInterceptor skip it) so the path matcher never runs in production.
            // Still — defensively — trace:view's pattern would grant it, which is consistent
            // with the endpoint's "publicly readable" intent. Lock it in so a future change
            // to either side surfaces here.
            assertThat(matchingCodes("GET", "/api/traces/public-key")).containsExactly("trace:view");
        }
    }

    @Nested
    @DisplayName("Dashboard and Admin routes")
    class DashboardAndAdminRoutes {

        @Test
        void dashboardSubRoutesMatchOnlyDashboardView() {
            assertThat(matchingCodes("GET", "/api/dashboard/kpi")).containsExactly("dashboard:view");
            assertThat(matchingCodes("GET", "/api/dashboard/map")).containsExactly("dashboard:view");
            assertThat(matchingCodes("GET", "/api/dashboard/trend")).containsExactly("dashboard:view");
            assertThat(matchingCodes("GET", "/api/dashboard/topology")).containsExactly("dashboard:view");
        }

        @Test
        void adminGenerateSampleDataMatchesOnlyDataGenerate() {
            assertThat(matchingCodes("POST", "/api/admin/generate-sample-data"))
                .containsExactly("trace:data:generate");
        }

        @Test
        void adminClearTraceDataMatchesOnlyDataClear() {
            assertThat(matchingCodes("DELETE", "/api/admin/clear-trace-data"))
                .containsExactly("trace:data:clear");
        }
    }

    @Nested
    @DisplayName("Cross-resource leakage probes (no escalation between users / roles / parts / traces)")
    class CrossResourceLeakageProbes {

        @Test
        void userPermissionsDoNotMatchRoleOrPartOrTracePaths() {
            for (SysPermission p : seededPermissions()) {
                if (!p.getPermCode().startsWith("user:")) continue;
                assertThat(matcher.matches(p, "GET", "/api/roles")).isFalse();
                assertThat(matcher.matches(p, "GET", "/api/parts")).isFalse();
                assertThat(matcher.matches(p, "GET", "/api/traces/TRACE-1")).isFalse();
                assertThat(matcher.matches(p, "GET", "/api/dashboard/kpi")).isFalse();
                assertThat(matcher.matches(p, "POST", "/api/admin/generate-sample-data")).isFalse();
            }
        }

        @Test
        void rolePermissionsDoNotMatchUserOrPartOrTracePaths() {
            for (SysPermission p : seededPermissions()) {
                if (!p.getPermCode().startsWith("role:")) continue;
                assertThat(matcher.matches(p, "GET", "/api/users")).isFalse();
                assertThat(matcher.matches(p, "GET", "/api/parts")).isFalse();
                assertThat(matcher.matches(p, "POST", "/api/traces")).isFalse();
            }
        }

        @Test
        void partPermissionsDoNotMatchTraceOrUserOrRolePaths() {
            for (SysPermission p : seededPermissions()) {
                if (!p.getPermCode().startsWith("part:")) continue;
                assertThat(matcher.matches(p, "POST", "/api/traces")).isFalse();
                assertThat(matcher.matches(p, "GET", "/api/users")).isFalse();
                assertThat(matcher.matches(p, "GET", "/api/roles")).isFalse();
            }
        }

        @Test
        void traceViewDoesNotMatchPostEndpoints() {
            // trace:view is GET-only; even though /api/traces/* would expand to /api/traces/**,
            // the method check must reject POST /api/traces/{code}/events.
            SysPermission traceView = perm("trace:view", "GET", "/api/traces/*");
            assertThat(matcher.matches(traceView, "POST", "/api/traces/TRACE-1/events")).isFalse();
            assertThat(matcher.matches(traceView, "DELETE", "/api/traces/TRACE-1")).isFalse();
        }

        @Test
        void traceCreateDoesNotMatchEventsEndpoint() {
            // /api/traces is exact; must NOT bleed into multi-segment paths even though
            // toAntPattern would replace * with **. There's no * here so there's no expansion,
            // but lock the behavior in.
            SysPermission traceCreate = perm("trace:create", "POST", "/api/traces");
            assertThat(matcher.matches(traceCreate, "POST", "/api/traces/TRACE-1/events")).isFalse();
        }

        @Test
        void traceFineGrainedActionPermsDoNotMatchTopLevelTraceCreate() {
            // trace:inbound/outbound/transfer all carry pattern /api/traces/*/events; they must
            // NOT match POST /api/traces (which is the trace:create endpoint).
            for (SysPermission p : seededPermissions()) {
                if (!p.getPermCode().equals("trace:inbound")
                        && !p.getPermCode().equals("trace:outbound")
                        && !p.getPermCode().equals("trace:transfer")) {
                    continue;
                }
                assertThat(matcher.matches(p, "POST", "/api/traces")).isFalse();
            }
        }

        @Test
        void adminEndpointsDoNotMatchAnyOtherPermission() {
            // /api/admin/* is NOT covered by any seeded pattern other than the two exact rows.
            for (SysPermission p : seededPermissions()) {
                if (p.getPermCode().equals("trace:data:generate")
                        || p.getPermCode().equals("trace:data:clear")) {
                    continue;
                }
                assertThat(matcher.matches(p, "POST", "/api/admin/generate-sample-data")).isFalse();
                assertThat(matcher.matches(p, "DELETE", "/api/admin/clear-trace-data")).isFalse();
            }
        }
    }
}
