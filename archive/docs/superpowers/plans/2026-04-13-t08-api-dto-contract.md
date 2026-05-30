# T08 API / DTO Contract Cleanup Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在不改 UI、不改业务流程的前提下，收敛前端 API 注释、后端 DTO 注解与 Controller 参数语义，让 camelCase / snake_case 契约边界只保留一套主规则和少量白名单兼容。

**Architecture:** 先用后端 Jackson / Controller 契约测试锁定 body 与 query 的真实绑定行为，再清理冗余 `@JsonProperty` / `@JsonAlias` 和 query DTO 假契约；随后补强前端 API 契约测试并统一 JSDoc，让页面层只面向 camelCase 编程接口。最后执行前后端最小回归验证，并更新任务表。

**Tech Stack:** Spring Boot 3、Jackson、JUnit 5、Mockito、MockMvc、Vue 3、Axios、Vitest、Vite

---

## File Map

**Create:**
- `backend/src/test/java/com/example/trace/dto/RequestDtoContractTest.java` - 锁定 body DTO 的 snake_case 主契约与 camelCase 白名单兼容
- `backend/src/test/java/com/example/trace/controller/ControllerQueryBindingTest.java` - 锁定 dashboard / user / part / auth 的 query 绑定语义

**Modify:**
- `backend/src/main/java/com/example/trace/dto/ProduceAssignRequest.java`
- `backend/src/main/java/com/example/trace/dto/ScanTraceRequest.java`
- `backend/src/main/java/com/example/trace/dto/PartCreateRequest.java`
- `backend/src/main/java/com/example/trace/dto/PartUpdateRequest.java`
- `backend/src/main/java/com/example/trace/dto/RoleCreateRequest.java`
- `backend/src/main/java/com/example/trace/dto/RoleUpdateRequest.java`
- `backend/src/main/java/com/example/trace/dto/UserCreateRequest.java`
- `backend/src/main/java/com/example/trace/dto/UserUpdateRequest.java`
- `backend/src/main/java/com/example/trace/dto/AssignPermissionsRequest.java`
- `backend/src/main/java/com/example/trace/dto/ResetPasswordRequest.java`
- `backend/src/main/java/com/example/trace/dto/ChangePasswordRequest.java`
- `backend/src/main/java/com/example/trace/dto/LoginRequest.java`
- `backend/src/main/java/com/example/trace/dto/PageRequest.java`
- `backend/src/main/java/com/example/trace/dto/UserListRequest.java`
- `backend/src/main/java/com/example/trace/dto/PartListRequest.java`
- `backend/src/main/java/com/example/trace/controller/DashboardController.java`
- `backend/src/main/java/com/example/trace/controller/UserController.java`
- `backend/src/main/java/com/example/trace/controller/PartController.java`
- `backend/src/main/java/com/example/trace/controller/AuthController.java`
- `backend/src/main/java/com/example/trace/controller/RoleController.java`
- `backend/src/main/java/com/example/trace/controller/TraceController.java`
- `frontend/src/features/__tests__/api-contracts.test.js`
- `frontend/src/core/api/auth.js`
- `frontend/src/features/user/api/users.js`
- `frontend/src/features/user/api/roles.js`
- `frontend/src/features/part/api/parts.js`
- `frontend/src/features/trace/api/trace.js`
- `frontend/src/features/dashboard/api/dashboard.js`
- `项目整改执行任务表.md`

**Verify:**
- `backend/src/test/java/com/example/trace/dto/RequestDtoContractTest.java`
- `backend/src/test/java/com/example/trace/controller/ControllerQueryBindingTest.java`
- `backend/src/test/java/com/example/trace/controller/AuthControllerTest.java`
- `frontend/src/features/__tests__/api-contracts.test.js`
- `frontend/src/core/api/__tests__/request.test.js`

**Workspace note:** 当前工作区未检测到 `.git`；本计划中的“Commit”统一替换为“更新任务表并保存检查点”。

---

### Task 1: 用 DTO 契约测试锁定 body 命名规则，再清理冗余注解

**Files:**
- Create: `backend/src/test/java/com/example/trace/dto/RequestDtoContractTest.java`
- Modify: `backend/src/main/java/com/example/trace/dto/ProduceAssignRequest.java`
- Modify: `backend/src/main/java/com/example/trace/dto/ScanTraceRequest.java`
- Modify: `backend/src/main/java/com/example/trace/dto/PartCreateRequest.java`
- Modify: `backend/src/main/java/com/example/trace/dto/PartUpdateRequest.java`
- Modify: `backend/src/main/java/com/example/trace/dto/RoleCreateRequest.java`
- Modify: `backend/src/main/java/com/example/trace/dto/RoleUpdateRequest.java`
- Modify: `backend/src/main/java/com/example/trace/dto/UserCreateRequest.java`
- Modify: `backend/src/main/java/com/example/trace/dto/UserUpdateRequest.java`
- Modify: `backend/src/main/java/com/example/trace/dto/AssignPermissionsRequest.java`
- Modify: `backend/src/main/java/com/example/trace/dto/ResetPasswordRequest.java`
- Modify: `backend/src/main/java/com/example/trace/dto/ChangePasswordRequest.java`
- Modify: `backend/src/main/java/com/example/trace/dto/LoginRequest.java`
- Test: `backend/src/test/java/com/example/trace/dto/RequestDtoContractTest.java`

- [ ] **Step 1: 写失败测试**

```java
// backend/src/test/java/com/example/trace/dto/RequestDtoContractTest.java
class RequestDtoContractTest {
    private final ObjectMapper mapper = new JacksonConfig().objectMapper();

    @Test
    void snakeCaseBodies_shouldDeserializeThroughGlobalNamingStrategy() throws Exception {
        ProduceAssignRequest produce = mapper.readValue("""
                {"part_code":"P-001","quantity":3,"manufacturer_node":"Factory-A"}
                """, ProduceAssignRequest.class);
        ScanTraceRequest scan = mapper.readValue("""
                {"action_type":"INBOUND","from_node":"A","to_node":"B","event_time":"2026-04-13T10:00:00","correction_of":8}
                """, ScanTraceRequest.class);

        assertThat(produce.getPartCode()).isEqualTo("P-001");
        assertThat(produce.getManufacturerNode()).isEqualTo("Factory-A");
        assertThat(scan.getFromNode()).isEqualTo("A");
        assertThat(scan.getToNode()).isEqualTo("B");
        assertThat(scan.getCorrectionOf()).isEqualTo(8L);
    }

    @Test
    void camelCaseWhitelistBodies_shouldStillDeserializeAfterJsonPropertyCleanup() throws Exception {
        UserCreateRequest user = mapper.readValue("""
                {"username":"alice","password":"abc123","roleId":2,"status":1}
                """, UserCreateRequest.class);
        PartCreateRequest part = mapper.readValue("""
                {"partCode":"P-001","partName":"Bearing","partType":"Mechanical"}
                """, PartCreateRequest.class);
        LoginRequest login = mapper.readValue("""
                {"username":"alice","password":"abc123","rememberMe":true}
                """, LoginRequest.class);

        assertThat(user.getRoleId()).isEqualTo(2L);
        assertThat(part.getPartCode()).isEqualTo("P-001");
        assertThat(login.isRememberMe()).isTrue();
    }
}
```

- [ ] **Step 2: 运行测试并确认先失败**

Run:

```bash
cd backend && mvn test -Dtest=RequestDtoContractTest
```

Expected: FAIL，当前 DTO 仍依赖显式 `@JsonProperty` / 冗余 alias，或测试类尚不存在。

- [ ] **Step 3: 清理 body DTO 注解，只保留白名单 alias**

```java
// ProduceAssignRequest.java
@Data
public class ProduceAssignRequest {
    private Long spuId;
    private String partCode;
    @Min(1) private int quantity;
    private String manufacturerNode;
    private String province;
    private String city;
}
```

```java
// ScanTraceRequest.java
@Data
public class ScanTraceRequest {
    @JsonIgnore private String traceCode;
    @NotNull(message = "actionType 不能为空")
    private ActionType actionType;
    private String fromNode;
    private String toNode;
    private String province;
    private String city;
    private String eventTime;
    private Long correctionOf;
}
```

```java
// LoginRequest.java
@Data
public class LoginRequest {
    @NotBlank private String username;
    @NotBlank private String password;
    @JsonAlias("rememberMe")
    private Boolean rememberMe;

    public boolean isRememberMe() {
        return Boolean.TRUE.equals(rememberMe);
    }
}
```

```java
// UserCreateRequest.java / UserUpdateRequest.java / PartCreateRequest.java / PartUpdateRequest.java / RoleCreateRequest.java / RoleUpdateRequest.java / AssignPermissionsRequest.java / ResetPasswordRequest.java / ChangePasswordRequest.java
// 分别删除字段上的 @JsonProperty("snake_case")；保留现有 @JsonAlias("camelCase")；Java 字段名不变，继续使用 camelCase。
```
- [ ] **Step 4: 重新运行 DTO 契约测试**

Run:

```bash
cd backend && mvn test -Dtest=RequestDtoContractTest
```

Expected: PASS，snake_case body 可通过 Jackson 全局策略反序列化，白名单内 camelCase body 仍可兼容。

- [ ] **Step 5: 更新任务表中的 Task 1 检查点**

```md
在 `项目整改执行任务表.md` 追加一条 “T08 Task 1 DTO 契约检查点” 更新记录，注明：
- 已新增 DTO 契约测试
- `ProduceAssignRequest` / `ScanTraceRequest` 的冗余 snake_case alias 已清理
- 其余 body DTO 已去掉冗余 `@JsonProperty`，只保留必要 camelCase 兼容 alias
```

---

### Task 2: 用 Controller 绑定测试锁定 query 语义，再把 query 契约显式化

**Files:**
- Create: `backend/src/test/java/com/example/trace/controller/ControllerQueryBindingTest.java`
- Modify: `backend/src/main/java/com/example/trace/controller/DashboardController.java`
- Modify: `backend/src/main/java/com/example/trace/controller/UserController.java`
- Modify: `backend/src/main/java/com/example/trace/controller/PartController.java`
- Modify: `backend/src/main/java/com/example/trace/controller/AuthController.java`
- Modify: `backend/src/main/java/com/example/trace/dto/PageRequest.java`
- Modify: `backend/src/main/java/com/example/trace/dto/UserListRequest.java`
- Modify: `backend/src/main/java/com/example/trace/dto/PartListRequest.java`
- Modify: `backend/src/main/java/com/example/trace/controller/RoleController.java`
- Modify: `backend/src/main/java/com/example/trace/controller/TraceController.java`
- Test: `backend/src/test/java/com/example/trace/controller/ControllerQueryBindingTest.java`

- [ ] **Step 1: 写失败测试**

```java
// backend/src/test/java/com/example/trace/controller/ControllerQueryBindingTest.java
@ExtendWith(MockitoExtension.class)
class ControllerQueryBindingTest {
    @Mock private DashboardService dashboardService;
    @Mock private UserService userService;
    @Mock private PartService partService;
    @Mock private SysUserMapper sysUserMapper;
    @Mock private SysRoleMapper sysRoleMapper;
    @Mock private JwtUtil jwtUtil;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private TokenStore tokenStore;
    @Mock private PermissionService permissionService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(
                new DashboardController(dashboardService),
                new UserController(userService),
                new PartController(partService),
                new AuthController(sysUserMapper, sysRoleMapper, jwtUtil, passwordEncoder, tokenStore, permissionService)
        ).setMessageConverters(new MappingJackson2HttpMessageConverter(new JacksonConfig().objectMapper())).build();
    }

    @Test
    void listUsers_shouldMapSnakeCaseFiltersIntoUserListRequest() throws Exception {
        when(userService.listUsers(any(UserListRequest.class), eq("ADMIN"))).thenReturn(PageResponse.of(List.of(), 0L, 1, 10));

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
        assertThat(captor.getValue().getRoleId()).isEqualTo(2L);
    }

    @Test
    void listParts_shouldMapSnakeCaseFiltersIntoPartListRequest() throws Exception {
        when(partService.listParts(any(PartListRequest.class))).thenReturn(PageResponse.of(List.of(), 0L, 1, 10));

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
        assertThat(captor.getValue().getPartType()).isEqualTo("Mechanical");
    }

    @Test
    void dashboardTopology_shouldAcceptTraceCodeSnakeCaseQuery() throws Exception {
        when(dashboardService.topology("TRACE-001", "30d")).thenReturn(Map.of("nodes", List.of(), "links", List.of(), "range", "30d"));

        mockMvc.perform(get("/api/dashboard/topology")
                        .param("trace_code", "TRACE-001")
                        .param("range", "30d"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.range").value("30d"));
    }

    @Test
    void changeUserRole_shouldAcceptRoleIdSnakeCaseQuery() throws Exception {
        when(userService.changeUserRole(7L, 3L, "ADMIN")).thenReturn(new UserResponse());

        mockMvc.perform(patch("/api/users/7/role").param("role_id", "3").requestAttr("role", "ADMIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }

    @Test
    void refreshToken_shouldAcceptRememberMeSnakeCaseQuery() throws Exception {
        when(tokenStore.isBlacklisted("old-token")).thenReturn(false);
        when(jwtUtil.validateToken("old-token")).thenReturn(true);
        when(jwtUtil.refreshToken("old-token", true)).thenReturn("new-token");
        when(jwtUtil.getUsernameFromToken("new-token")).thenReturn("alice");
        when(jwtUtil.getRoleFromToken("new-token")).thenReturn("ADMIN");

        mockMvc.perform(post("/api/auth/refresh")
                        .header("Authorization", "Bearer old-token")
                        .param("remember_me", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.token").value("new-token"));
    }
}
```

- [ ] **Step 2: 运行测试并确认先失败**

Run:

```bash
cd backend && mvn test -Dtest=ControllerQueryBindingTest
```

Expected: FAIL，至少 `role_id` / `remember_me` / list filter 绑定中的一部分会失败。

- [ ] **Step 3: 显式定义 query 绑定，并移除 query DTO 上的 Jackson 假契约**

```java
// UserController.java
@GetMapping
public ApiResponse<PageResponse<UserResponse>> listUsers(
        HttpServletRequest request,
        @RequestParam(required = false) String username,
        @RequestParam(name = "role_id", required = false) Long roleId,
        @RequestParam(required = false) Integer status,
        @RequestParam(defaultValue = "1") Integer page,
        @RequestParam(defaultValue = "10") Integer size,
        @RequestParam(required = false) String sort,
        @RequestParam(defaultValue = "desc") String order) {
    UserListRequest listRequest = new UserListRequest();
    listRequest.setUsername(username);
    listRequest.setRoleId(roleId);
    listRequest.setStatus(status);
    listRequest.setPage(page);
    listRequest.setSize(size);
    listRequest.setSort(sort);
    listRequest.setOrder(order);
    return ApiResponse.success(userService.listUsers(listRequest, (String) request.getAttribute("role")));
}

@PatchMapping("/{id}/role")
public ApiResponse<UserResponse> changeUserRole(HttpServletRequest request, @PathVariable Long id, @RequestParam(name = "role_id") Long roleId) {
    return ApiResponse.success(userService.changeUserRole(id, roleId, (String) request.getAttribute("role")));
}
```

```java
// PartController.java
@GetMapping
public ApiResponse<PageResponse<PartResponse>> listParts(
        @RequestParam(required = false) String keyword,
        @RequestParam(name = "part_code", required = false) String partCode,
        @RequestParam(name = "part_name", required = false) String partName,
        @RequestParam(name = "part_type", required = false) String partType,
        @RequestParam(required = false) String manufacturer,
        @RequestParam(defaultValue = "1") Integer page,
        @RequestParam(defaultValue = "10") Integer size,
        @RequestParam(required = false) String sort,
        @RequestParam(defaultValue = "desc") String order) {
    PartListRequest request = new PartListRequest();
    request.setKeyword(keyword);
    request.setPartCode(partCode);
    request.setPartName(partName);
    request.setPartType(partType);
    request.setManufacturer(manufacturer);
    request.setPage(page);
    request.setSize(size);
    request.setSort(sort);
    request.setOrder(order);
    return ApiResponse.success(partService.listParts(request));
}
```

```java
// AuthController.java
@PostMapping("/refresh")
public ResponseEntity<ApiResponse<LoginResponse>> refreshToken(
        HttpServletRequest request,
        @RequestParam(name = "remember_me", required = false, defaultValue = "false") Boolean rememberMe) {
    String authHeader = request.getHeader("Authorization");
    String oldToken = extractToken(authHeader);

    if (oldToken == null) {
        throw new BizException(BizCode.UNAUTHORIZED, 401, "未提供 Token");
    }
    if (tokenStore.isBlacklisted(oldToken)) {
        throw new BizException(BizCode.UNAUTHORIZED, 401, "Token 已失效");
    }
    if (!jwtUtil.validateToken(oldToken)) {
        throw new BizException(BizCode.UNAUTHORIZED, 401, "Token 无效或已过期");
    }

    String newToken = jwtUtil.refreshToken(oldToken, rememberMe);
    tokenStore.addToBlacklist(oldToken);

    return ResponseEntity.ok(ApiResponse.ok(
            new LoginResponse(newToken, jwtUtil.getUsernameFromToken(newToken), jwtUtil.getRoleFromToken(newToken)),
            "Token 刷新成功"
    ));
}
```

```java
// PageRequest.java / UserListRequest.java / PartListRequest.java
// 删除 Jackson 注解，让 query 契约只由显式 @RequestParam + 自动化测试定义。
```
- [ ] **Step 4: 重新运行 query 绑定测试**

Run:

```bash
cd backend && mvn test -Dtest=ControllerQueryBindingTest
```

Expected: PASS，`trace_code`、`role_id`、`part_type`、`remember_me` 等 query 语义都由测试明确锁定。

- [ ] **Step 5: 更新任务表中的 Task 2 检查点**

```md
在 `项目整改执行任务表.md` 追加一条 “T08 Task 2 Controller query 契约检查点” 更新记录，注明：
- 已新增 ControllerQueryBindingTest
- User / Part / Dashboard / Auth 的 query 参数契约已由显式绑定和自动化测试锁定
- PageRequest / UserListRequest / PartListRequest 已去掉不再充当文档本体的 Jackson 注解
```

---

### Task 3: 补强前端 API 契约测试，并统一 core / feature API 的 JSDoc 语义

**Files:**
- Modify: `frontend/src/features/__tests__/api-contracts.test.js`
- Modify: `frontend/src/core/api/auth.js`
- Modify: `frontend/src/features/user/api/users.js`
- Modify: `frontend/src/features/user/api/roles.js`
- Modify: `frontend/src/features/part/api/parts.js`
- Modify: `frontend/src/features/trace/api/trace.js`
- Modify: `frontend/src/features/dashboard/api/dashboard.js`
- Test: `frontend/src/features/__tests__/api-contracts.test.js`

- [ ] **Step 1: 先扩展失败测试**

```js
// frontend/src/features/__tests__/api-contracts.test.js
import { beforeEach, describe, expect, it, vi } from 'vitest'

const requestMock = vi.hoisted(() => ({
  get: vi.fn(),
  post: vi.fn(),
  put: vi.fn(),
  patch: vi.fn(),
  delete: vi.fn()
}))

vi.mock('@/core/api/request', () => ({ default: requestMock }))

import request from '@/core/api/request'
import { login, getUserInfo, logout } from '@/core/api/auth'
import { getUsers, createUser, resetUserPassword } from '@/features/user/api/users'
import { createRole, assignPermissions } from '@/features/user/api/roles'
import { createPart } from '@/features/part/api/parts'
import { createTrace } from '@/features/trace/api/trace'
import { getTopology } from '@/features/dashboard/api/dashboard'

describe('feature api contracts', () => {
  beforeEach(() => {
    request.get.mockReset()
    request.post.mockReset()
    request.put.mockReset()
    request.patch.mockReset()
    request.delete.mockReset()
  })

  it('keeps auth and management inputs in camelCase before request serialization', async () => {
    request.get.mockResolvedValue({})
    request.post.mockResolvedValue({})
    request.put.mockResolvedValue({})

    await login('alice', 'abc123', true)
    await getUserInfo()
    await logout()
    await getUsers({ username: 'alice', roleId: 2, page: 1, size: 10 })
    await createUser({ username: 'alice', password: 'abc123', roleId: 2, status: 1 })
    await resetUserPassword(8, 'newPass123')
    await createRole({ roleCode: 'MANAGER', roleName: 'Manager' })
    await assignPermissions(3, [1, 2, 3])

    expect(request.post).toHaveBeenNthCalledWith(1, '/auth/login', { username: 'alice', password: 'abc123', rememberMe: true })
    expect(request.get).toHaveBeenNthCalledWith(1, '/auth/me')
    expect(request.post).toHaveBeenNthCalledWith(2, '/auth/logout')
    expect(request.post).toHaveBeenNthCalledWith(4, '/users/8/reset-password', { newPassword: 'newPass123' })
    expect(request.put).toHaveBeenNthCalledWith(1, '/roles/3/permissions', { permissionIds: [1, 2, 3] })
  })
})
```

- [ ] **Step 2: 运行测试并确认先失败**

Run:

```bash
cd frontend && npm run test -- --run --pool=threads src/features/__tests__/api-contracts.test.js
```

Expected: FAIL，当前测试尚未覆盖 `auth.js` / `resetUserPassword` / `assignPermissions`，或注释语义与断言还未完全对齐。

- [ ] **Step 3: 统一 API 注释与边界说明，只保留 camelCase 编程语义**

```js
// frontend/src/core/api/auth.js
/**
 * 认证 API。
 * 前端编程接口统一使用 camelCase；HTTP 请求发出前会由 request.js 统一序列化为 snake_case。
 */
export function login(username, password, rememberMe = false) {
  return request.post('/auth/login', { username, password, rememberMe })
}
```

```js
// frontend/src/features/user/api/users.js
/**
 * 获取用户列表。
 * 编程接口使用 camelCase；query 在 request 层统一转为 snake_case。
 */
export function getUsers(params) {
  return request.get('/users', { params })
}

/**
 * 重置用户密码。
 * @param {number} id
 * @param {string} newPassword
 */
export function resetUserPassword(id, newPassword) {
  return request.post(`/users/${id}/reset-password`, { newPassword })
}
```

```js
// frontend/src/features/user/api/roles.js
/**
 * 给角色分配权限。
 * @param {number} id
 * @param {Array<number>} permissionIds
 */
export function assignPermissions(id, permissionIds) {
  return request.put(`/roles/${id}/permissions`, { permissionIds })
}
```

补充说明：
- `frontend/src/features/part/api/parts.js`：把 createPart / updatePart / getParts 的 JSDoc 参数、返回字段统一写成 partCode / partName / partType。
- `frontend/src/features/trace/api/trace.js`：把 createTrace / createEvent / getTraceDetail / verifyTraceChain 的 JSDoc 统一写成 manufacturerNode / actionType / eventTime / correctionOf / totalLogs。
- `frontend/src/features/dashboard/api/dashboard.js`：把 getKPI / getMapData / getTrend / getTopology 的 JSDoc 统一写成 totalTraces / todayNew / exceptionCount / traceCode。
- 上述 3 个文件的模块头注释都追加一句：前端编程接口使用 camelCase，HTTP 发出前由 request.js 统一转为 snake_case。

- [ ] **Step 4: 重新运行前端 API 契约测试**

Run:

```bash
cd frontend && npm run test -- --run --pool=threads src/features/__tests__/api-contracts.test.js
```

Expected: PASS，测试能证明 core / feature API 在 request 层序列化之前全部保持 camelCase 调用语义。

- [ ] **Step 5: 更新任务表中的 Task 3 检查点**

```md
在 `项目整改执行任务表.md` 追加一条 “T08 Task 3 前端 API 语义检查点” 更新记录，注明：
- `auth.js` 与 feature API 的 JSDoc 已统一成 camelCase 编程接口说明
- `resetUserPassword` / `assignPermissions` / `login` 等边界调用均已被契约测试覆盖
- 本轮只做注释与契约治理，不涉及 UI 改造
```

---

### Task 4: 执行整体验证，并把 T08 更新到任务表

**Files:**
- Modify: `项目整改执行任务表.md`
- Test: `backend/src/test/java/com/example/trace/dto/RequestDtoContractTest.java`
- Test: `backend/src/test/java/com/example/trace/controller/ControllerQueryBindingTest.java`
- Test: `backend/src/test/java/com/example/trace/controller/AuthControllerTest.java`
- Test: `frontend/src/features/__tests__/api-contracts.test.js`
- Test: `frontend/src/core/api/__tests__/request.test.js`

- [ ] **Step 1: 运行后端 T08 相关测试集合**

Run:

```bash
cd backend && mvn test -Dtest=RequestDtoContractTest,ControllerQueryBindingTest,AuthControllerTest
```

Expected: PASS，DTO body 契约、Controller query 绑定和认证相关回归全部通过。

- [ ] **Step 2: 运行前端契约测试**

Run:

```bash
cd frontend && npm run test -- --run --pool=threads src/core/api/__tests__/request.test.js src/features/__tests__/api-contracts.test.js
```

Expected: PASS，`request.js` 的 snake/camel 边界与 feature / core API 的 camelCase 编程接口都保持稳定。

- [ ] **Step 3: 运行前端构建**

Run:

```bash
cd frontend && npm run build
```

Expected: PASS，允许保留既有 chunk warning，但不能出现新的类型错误、模板错误或导入错误。

- [ ] **Step 4: 更新任务表，将 T08 切到完成状态**

```md
同步更新 `项目整改执行任务表.md`：
- “当前进行中任务”改为“无”，或切到下一推荐任务
- “最近一次更新摘要”改为 T08 已完成
- “当前任务状态总览”里将 T08 改为 DONE
- “更新记录”补充 T08 最终完成记录
- “下一推荐任务”切到 T12（按当前阶段顺序继续做逻辑/配置收口）
```

- [ ] **Step 5: 保存最终检查点**

```md
在 T08 最终更新记录里明确写出：
- body DTO 主契约已收敛为 snake_case wire contract + camelCase Java 字段
- query 参数契约已由显式 controller 绑定与自动化测试锁定
- 前端 API 注释统一为 camelCase 编程语义
- 本轮未做 UI 改造，只完成逻辑 / 契约 / 注释治理
```

