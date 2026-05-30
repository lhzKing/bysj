# T24 User / Role Query Optimization Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 消除 `UserServiceImpl.listUsers()` 与 `RoleServiceImpl.listRoles()` 的 N+1 查询，同时把角色列表收敛为轻量 `permissionCount` 契约，并仅在获准范围内调整角色列表前端链路。

**Architecture:** 先用后端 service 测试锁定“用户列表批量补角色”“角色列表仅返回权限数量、角色详情仍返回完整权限”的行为，再在 service / mapper / DTO 中实现批量查询与权限计数。最后在获准的 `roles.js` + `RoleList.vue` 链路上切换到 `permissionCount` 和按需 `getRole(id)` 详情加载，并以 Vitest 锁定前端契约。

**Tech Stack:** Spring Boot 3、MyBatis-Plus、JUnit 5、Mockito、Vue 3、Vitest、Vite

---

## File Map

**Create:**
- `backend/src/test/java/com/example/trace/service/impl/UserServiceImplTest.java` - 锁定用户列表批量加载角色、避免逐条 `selectById`
- `backend/src/test/java/com/example/trace/service/impl/RoleServiceImplTest.java` - 锁定角色列表 `permissionCount` 契约与角色详情完整权限契约
- `frontend/src/features/user/views/__tests__/RoleList.contract.test.js` - 锁定角色列表使用 `permissionCount` 展示，并在权限配置时按需请求 `getRole(id)`

**Modify:**
- `backend/src/main/java/com/example/trace/service/impl/UserServiceImpl.java`
- `backend/src/main/java/com/example/trace/service/impl/RoleServiceImpl.java`
- `backend/src/main/java/com/example/trace/mapper/SysRolePermissionMapper.java`
- `backend/src/main/java/com/example/trace/dto/RoleResponse.java`
- `frontend/src/features/user/api/roles.js`
- `frontend/src/features/user/views/RoleList.vue`
- `frontend/src/features/__tests__/api-contracts.test.js`
- `项目整改执行任务表.md`

**Verify:**
- `backend/src/test/java/com/example/trace/service/impl/UserServiceImplTest.java`
- `backend/src/test/java/com/example/trace/service/impl/RoleServiceImplTest.java`
- `frontend/src/features/user/views/__tests__/RoleList.contract.test.js`
- `frontend/src/features/__tests__/api-contracts.test.js`

**Workspace note:** 当前工作区未检测到 `.git`；本计划中的“Commit”统一替换为“更新任务表并保存检查点”。

---

### Task 1: 先用测试锁定 `UserServiceImpl.listUsers()` 的批量角色加载行为

**Files:**
- Create: `backend/src/test/java/com/example/trace/service/impl/UserServiceImplTest.java`
- Modify: `backend/src/main/java/com/example/trace/service/impl/UserServiceImpl.java`
- Test: `backend/src/test/java/com/example/trace/service/impl/UserServiceImplTest.java`

- [ ] **Step 1: 写失败测试**

```java
package com.example.trace.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.trace.dto.PageResponse;
import com.example.trace.dto.UserListRequest;
import com.example.trace.dto.UserResponse;
import com.example.trace.entity.SysRole;
import com.example.trace.entity.SysUser;
import com.example.trace.mapper.SysRoleMapper;
import com.example.trace.mapper.SysUserMapper;
import com.example.trace.security.PasswordEncoder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock private SysUserMapper userMapper;
    @Mock private SysRoleMapper roleMapper;
    @Mock private PasswordEncoder passwordEncoder;

    @Test
    void listUsers_shouldBatchLoadRolesWithoutPerUserSelectById() {
        UserServiceImpl service = new UserServiceImpl(userMapper, roleMapper, passwordEncoder);

        SysUser alice = new SysUser();
        alice.setId(1L);
        alice.setUsername("alice");
        alice.setRoleId(11L);
        alice.setStatus(1);

        SysUser bob = new SysUser();
        bob.setId(2L);
        bob.setUsername("bob");
        bob.setRoleId(12L);
        bob.setStatus(1);

        Page<SysUser> page = new Page<>(1, 10);
        page.setRecords(List.of(alice, bob));
        page.setTotal(2);

        SysRole admin = new SysRole();
        admin.setId(11L);
        admin.setRoleCode("ADMIN");
        admin.setRoleName("Administrator");

        SysRole user = new SysRole();
        user.setId(12L);
        user.setRoleCode("USER");
        user.setRoleName("Standard User");

        when(userMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(page);
        when(roleMapper.selectBatchIds(any())).thenReturn(List.of(admin, user));

        PageResponse<UserResponse> response = service.listUsers(new UserListRequest(), "SUPER_ADMIN");

        assertThat(response.getList()).extracting(UserResponse::getRoleCode).containsExactly("ADMIN", "USER");
        assertThat(response.getList()).extracting(UserResponse::getRoleName).containsExactly("Administrator", "Standard User");
        verify(roleMapper).selectBatchIds(any());
        verify(roleMapper, never()).selectById(anyLong());
    }
}
```

- [ ] **Step 2: 运行测试并确认先失败**

Run:

```bash
cd backend && mvn test -Dtest=UserServiceImplTest
```

Expected: FAIL，当前 `listUsers()` 仍通过 `convertToResponse()` 逐条调用 `roleMapper.selectById(...)`。

- [ ] **Step 3: 用批量角色映射替换逐条查询**

```java
// backend/src/main/java/com/example/trace/service/impl/UserServiceImpl.java
@Override
public PageResponse<UserResponse> listUsers(UserListRequest request, String operatorRoleCode) {
    Page<SysUser> page = new Page<>(request.getPage(), request.getSize());
    LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
    // ... 保留现有筛选 / 排序 / 可见性逻辑 ...

    Page<SysUser> result = userMapper.selectPage(page, wrapper);
    List<SysUser> users = result.getRecords();
    Map<Long, SysRole> roleMap = buildRoleMap(users);

    List<UserResponse> userList = users.stream()
            .map(user -> convertToResponse(user, roleMap.get(user.getRoleId())))
            .collect(Collectors.toList());

    return PageResponse.of(userList, result.getTotal(), request.getPage(), request.getSize());
}

private Map<Long, SysRole> buildRoleMap(List<SysUser> users) {
    List<Long> roleIds = users.stream()
            .map(SysUser::getRoleId)
            .filter(Objects::nonNull)
            .distinct()
            .collect(Collectors.toList());
    if (roleIds.isEmpty()) {
        return Map.of();
    }
    return roleMapper.selectBatchIds(roleIds).stream()
            .collect(Collectors.toMap(SysRole::getId, role -> role));
}

private UserResponse convertToResponse(SysUser user, SysRole role) {
    UserResponse response = new UserResponse();
    response.setId(user.getId());
    response.setUsername(user.getUsername());
    response.setRoleId(user.getRoleId());
    response.setStatus(user.getStatus());
    response.setCreateTime(user.getCreateTime());
    response.setUpdateTime(user.getUpdateTime());
    if (role != null) {
        response.setRoleCode(role.getRoleCode());
        response.setRoleName(role.getRoleName());
    }
    return response;
}
```

补充说明：
- `getUserById()` 继续走 `selectUserWithRoleById()` + `convertToResponseWithRole()`，不扩散到详情链路。
- 旧的 `convertToResponse(SysUser user)` 如无其他调用，直接删除，避免未来误回到逐条查询。

- [ ] **Step 4: 重新运行用户列表批量查询测试**

Run:

```bash
cd backend && mvn test -Dtest=UserServiceImplTest
```

Expected: PASS，列表结果中的 `roleCode / roleName` 保持不变，且不会再逐条 `selectById`。

- [ ] **Step 5: 更新任务表中的 Task 1 检查点**

```md
在 `项目整改执行任务表.md` 追加一条 “T24 Task 1 用户列表批量角色检查点” 更新记录，注明：
- 已新增 `UserServiceImplTest`
- `listUsers()` 已改为批量 `selectBatchIds`
- 用户列表返回契约保持不变
```

---

### Task 2: 先用测试锁定角色列表轻量化与角色详情完整权限契约

**Files:**
- Create: `backend/src/test/java/com/example/trace/service/impl/RoleServiceImplTest.java`
- Modify: `backend/src/main/java/com/example/trace/service/impl/RoleServiceImpl.java`
- Modify: `backend/src/main/java/com/example/trace/mapper/SysRolePermissionMapper.java`
- Modify: `backend/src/main/java/com/example/trace/dto/RoleResponse.java`
- Test: `backend/src/test/java/com/example/trace/service/impl/RoleServiceImplTest.java`

- [ ] **Step 1: 写失败测试**

```java
package com.example.trace.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.trace.dto.RoleResponse;
import com.example.trace.entity.SysPermission;
import com.example.trace.entity.SysRole;
import com.example.trace.mapper.SysPermissionMapper;
import com.example.trace.mapper.SysRoleMapper;
import com.example.trace.mapper.SysRolePermissionMapper;
import com.example.trace.mapper.SysUserMapper;
import com.example.trace.security.PermissionService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RoleServiceImplTest {

    @Mock private SysRoleMapper roleMapper;
    @Mock private SysPermissionMapper permissionMapper;
    @Mock private SysRolePermissionMapper rolePermissionMapper;
    @Mock private SysUserMapper userMapper;
    @Mock private PermissionService permissionService;

    @Test
    void listRoles_shouldReturnPermissionCountWithoutLoadingPermissionDetails() {
        RoleServiceImpl service = new RoleServiceImpl(
                roleMapper, permissionMapper, rolePermissionMapper, userMapper, permissionService
        );

        SysRole admin = new SysRole();
        admin.setId(1L);
        admin.setRoleCode("ADMIN");
        admin.setRoleName("Administrator");

        SysRole viewer = new SysRole();
        viewer.setId(2L);
        viewer.setRoleCode("USER");
        viewer.setRoleName("Viewer");

        when(roleMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(admin, viewer));
        when(rolePermissionMapper.countPermissionByRoleIds(List.of(1L, 2L))).thenReturn(List.of(
                Map.of("roleId", 1L, "permissionCount", 5L)
        ));

        List<RoleResponse> response = service.listRoles();

        assertThat(response).extracting(RoleResponse::getPermissionCount).containsExactly(5, 0);
        assertThat(response).allSatisfy(role -> assertThat(role.getPermissions()).isNull());
        verify(permissionMapper, never()).selectByRoleId(anyLong());
    }

    @Test
    void getRoleById_shouldKeepReturningFullPermissions() {
        RoleServiceImpl service = new RoleServiceImpl(
                roleMapper, permissionMapper, rolePermissionMapper, userMapper, permissionService
        );

        SysRole role = new SysRole();
        role.setId(7L);
        role.setRoleCode("MANAGER");
        role.setRoleName("Manager");

        SysPermission permission = new SysPermission();
        permission.setId(101L);
        permission.setPermCode("role:view");
        permission.setPermName("Role View");

        when(roleMapper.selectById(7L)).thenReturn(role);
        when(permissionMapper.selectByRoleId(7L)).thenReturn(List.of(permission));

        RoleResponse response = service.getRoleById(7L);

        assertThat(response.getPermissions()).hasSize(1);
        assertThat(response.getPermissions().get(0).getPermCode()).isEqualTo("role:view");
    }
}
```

- [ ] **Step 2: 运行测试并确认先失败**

Run:

```bash
cd backend && mvn test -Dtest=RoleServiceImplTest
```

Expected: FAIL，当前 `listRoles()` 仍对每个角色逐条调用 `permissionMapper.selectByRoleId(...)`，且 `RoleResponse` 还没有 `permissionCount`。

- [ ] **Step 3: 实现角色列表轻量化与按角色聚合计数**

```java
// backend/src/main/java/com/example/trace/mapper/SysRolePermissionMapper.java
@Select("""
    <script>
    SELECT role_id AS roleId, COUNT(*) AS permissionCount
    FROM sys_role_permission
    WHERE role_id IN
    <foreach collection="roleIds" item="roleId" open="(" separator="," close=")">
        #{roleId}
    </foreach>
    GROUP BY role_id
    </script>
    """)
List<Map<String, Object>> countPermissionByRoleIds(@Param("roleIds") List<Long> roleIds);
```

```java
// backend/src/main/java/com/example/trace/dto/RoleResponse.java
@Data
public class RoleResponse {
    private Long id;
    private String roleCode;
    private String roleName;
    private String remark;
    private Integer permissionCount;
    private List<PermissionResponse> permissions;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
```

```java
// backend/src/main/java/com/example/trace/service/impl/RoleServiceImpl.java
@Override
public List<RoleResponse> listRoles() {
    List<SysRole> roles = roleMapper.selectList(
            new LambdaQueryWrapper<SysRole>().orderByAsc(SysRole::getId)
    );
    Map<Long, Integer> permissionCountMap = buildPermissionCountMap(roles);

    return roles.stream()
            .map(role -> convertToResponse(role, permissionCountMap.getOrDefault(role.getId(), 0)))
            .collect(Collectors.toList());
}

private Map<Long, Integer> buildPermissionCountMap(List<SysRole> roles) {
    List<Long> roleIds = roles.stream().map(SysRole::getId).collect(Collectors.toList());
    if (roleIds.isEmpty()) {
        return Map.of();
    }
    return rolePermissionMapper.countPermissionByRoleIds(roleIds).stream()
            .collect(Collectors.toMap(
                    row -> ((Number) row.get("roleId")).longValue(),
                    row -> ((Number) row.get("permissionCount")).intValue()
            ));
}

private RoleResponse convertToResponse(SysRole role, Integer permissionCount) {
    RoleResponse response = new RoleResponse();
    response.setId(role.getId());
    response.setRoleCode(role.getRoleCode());
    response.setRoleName(role.getRoleName());
    response.setRemark(role.getRemark());
    response.setCreateTime(role.getCreateTime());
    response.setPermissionCount(permissionCount);
    return response;
}
```

```java
// backend/src/main/java/com/example/trace/service/impl/RoleServiceImpl.java
@Override
public RoleResponse getRoleById(Long id) {
    SysRole role = roleMapper.selectById(id);
    if (role == null) {
        throw new BizException(BizCode.NOT_FOUND, "角色不存在");
    }

    List<PermissionResponse> permissionResponses = permissionMapper.selectByRoleId(id).stream()
            .map(this::convertToPermissionResponse)
            .collect(Collectors.toList());

    RoleResponse response = convertToResponse(role, permissionResponses.size());
    response.setPermissions(permissionResponses);
    return response;
}
```

补充说明：
- `listRoles()` 不再回填 `permissions`，只暴露 `permissionCount`。
- `getRoleById()` 继续返回完整 `permissions`；这里顺手把 `permissionCount` 设为详情权限数，便于共享 DTO 时保持字段自洽。

- [ ] **Step 4: 重新运行角色列表/详情契约测试**

Run:

```bash
cd backend && mvn test -Dtest=RoleServiceImplTest
```

Expected: PASS，`listRoles()` 只依赖聚合计数查询，`getRoleById()` 仍返回完整权限明细。

- [ ] **Step 5: 更新任务表中的 Task 2 检查点**

```md
在 `项目整改执行任务表.md` 追加一条 “T24 Task 2 角色列表轻量契约检查点” 更新记录，注明：
- 已新增 `RoleServiceImplTest`
- `GET /api/roles` 已改为返回 `permissionCount`
- `GET /api/roles/{id}` 仍返回完整 `permissions`
```

---

### Task 3: 在获准范围内更新前端角色链路，只改契约不扩散 UI

**Files:**
- Create: `frontend/src/features/user/views/__tests__/RoleList.contract.test.js`
- Modify: `frontend/src/features/user/api/roles.js`
- Modify: `frontend/src/features/user/views/RoleList.vue`
- Modify: `frontend/src/features/__tests__/api-contracts.test.js`
- Test: `frontend/src/features/user/views/__tests__/RoleList.contract.test.js`
- Test: `frontend/src/features/__tests__/api-contracts.test.js`

- [ ] **Step 1: 先写失败的前端契约测试**

```js
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import RoleList from '@/features/user/views/RoleList.vue'

const getRolesMock = vi.fn()
const getRoleMock = vi.fn()
const getPermissionsMock = vi.fn()
const assignPermissionsMock = vi.fn()
const deleteRoleMock = vi.fn()
const createRoleMock = vi.fn()
const updateRoleMock = vi.fn()

vi.mock('@/features/user/api', () => ({
  getRoles: (...args) => getRolesMock(...args),
  getRole: (...args) => getRoleMock(...args),
  getPermissions: (...args) => getPermissionsMock(...args),
  assignPermissions: (...args) => assignPermissionsMock(...args),
  deleteRole: (...args) => deleteRoleMock(...args),
  createRole: (...args) => createRoleMock(...args),
  updateRole: (...args) => updateRoleMock(...args)
}))

vi.mock('@/shared/composables/useConfirm', () => ({
  useConfirm: () => ({ confirm: vi.fn() })
}))

vi.mock('@/shared/composables/useToast', () => ({
  useToast: () => ({
    success: vi.fn(),
    error: vi.fn()
  })
}))

describe('RoleList contract', () => {
  beforeEach(() => {
    getRolesMock.mockReset()
    getRoleMock.mockReset()
    getPermissionsMock.mockReset()
    assignPermissionsMock.mockReset()
    deleteRoleMock.mockReset()
    createRoleMock.mockReset()
    updateRoleMock.mockReset()
  })

  it('reads permission count from permissionCount instead of permissions length', async () => {
    getRolesMock.mockResolvedValue([{ id: 1, roleName: 'Admin', roleCode: 'ADMIN', permissionCount: 7, permissions: [] }])
    getPermissionsMock.mockResolvedValue([])

    const wrapper = mount(RoleList, {
      global: {
        stubs: {
          teleport: true,
          BaseCard: { template: '<div><slot /></div>' },
          LoadingSkeleton: true,
          'el-icon': true,
          Plus: true,
          Edit: true,
          Delete: true,
          Setting: true,
          Lock: true,
          X: true
        }
      }
    })

    await flushPromises()
    const { roles, getRolePermissionCount } = wrapper.vm.$.setupState

    expect(roles).toHaveLength(1)
    expect(getRolePermissionCount(roles[0])).toBe(7)
  })

  it('fetches full role detail before opening permission dialog', async () => {
    getRolesMock.mockResolvedValue([{ id: 3, roleName: 'Manager', roleCode: 'MANAGER', permissionCount: 2 }])
    getPermissionsMock.mockResolvedValue([{ id: 11, permCode: 'role:view', permName: 'Role View' }])
    getRoleMock.mockResolvedValue({
      id: 3,
      permissions: [{ id: 11 }, { id: 12 }]
    })

    const wrapper = mount(RoleList, {
      global: {
        stubs: {
          teleport: true,
          BaseCard: { template: '<div><slot /></div>' },
          LoadingSkeleton: true,
          'el-icon': true,
          Plus: true,
          Edit: true,
          Delete: true,
          Setting: true,
          Lock: true,
          X: true
        }
      }
    })

    await flushPromises()
    await wrapper.vm.$.setupState.handleAssignPermissions({ id: 3, roleName: 'Manager', permissionCount: 2 })
    await flushPromises()

    expect(getRoleMock).toHaveBeenCalledWith(3)
    expect(wrapper.vm.$.setupState.selectedPermissions).toEqual([11, 12])
    expect(wrapper.vm.$.setupState.showPermissionDialog).toBe(true)
  })
})
```

- [ ] **Step 2: 运行测试并确认先失败**

Run:

```bash
cd frontend && npm run test -- --run --pool=threads src/features/user/views/__tests__/RoleList.contract.test.js
```

Expected: FAIL，当前 `RoleList.vue` 仍从 `role.permissions` 读数量，并直接用列表项里的 `permissions` 初始化权限弹窗。

- [ ] **Step 3: 切换到轻量列表 + 按需详情加载**

```js
// frontend/src/features/user/api/roles.js
/**
 * 获取角色轻量列表。
 * Frontend programming interface uses camelCase; request.js serializes outgoing HTTP to snake_case.
 * @returns {Promise<Array<{ id: number, roleCode: string, roleName: string, remark?: string, permissionCount: number }>>}
 */
export function getRoles() {
  return request.get('/roles')
}

/**
 * 获取角色详情（含完整 permissions）。
 * Frontend programming interface uses camelCase; request.js serializes outgoing HTTP to snake_case.
 * @param {number} id
 * @returns {Promise<{ id: number, roleCode: string, roleName: string, permissions: Array }>}
 */
export function getRole(id) {
  return request.get(`/roles/${id}`)
}
```

```js
// frontend/src/features/user/views/RoleList.vue
import { getRoles, getRole, deleteRole, createRole, updateRole, getPermissions, assignPermissions } from '@/features/user/api'

const getRolePermissionCount = (role) => role.permissionCount || 0

const handleAssignPermissions = async (role) => {
  try {
    const detail = await getRole(role.id)
    selectedRole.value = role
    selectedPermissions.value = (detail?.permissions || []).map((permission) => permission.id)
    showPermissionDialog.value = true
  } catch (error) {
    console.error('Load role detail error:', error)
    toast.error('角色详情加载失败，请稍后重试')
  }
}
```

```js
// frontend/src/features/__tests__/api-contracts.test.js
import { getRoles, getRole, createRole, assignPermissions } from '@/features/user/api/roles'

it('keeps role list/detail api boundaries explicit', async () => {
  request.get.mockResolvedValue({})
  request.post.mockResolvedValue({})
  request.put.mockResolvedValue({})

  await getRoles()
  await getRole(3)
  await createRole({ roleCode: 'MANAGER', roleName: 'Manager' })
  await assignPermissions(3, [1, 2, 3])

  expect(request.get).toHaveBeenNthCalledWith(1, '/roles')
  expect(request.get).toHaveBeenNthCalledWith(2, '/roles/3')
  expect(request.post).toHaveBeenCalledWith('/roles', { roleCode: 'MANAGER', roleName: 'Manager' })
  expect(request.put).toHaveBeenCalledWith('/roles/3/permissions', { permissionIds: [1, 2, 3] })
})
```

补充说明：
- 本任务不做任何视觉重构；只允许调整 `roles.js` 契约说明和 `RoleList.vue` 的数据读取逻辑。
- 弹窗仍使用原有 UI；只是把“打开前初始化权限”的数据来源改成 `getRole(id)`。

- [ ] **Step 4: 重新运行前端契约测试**

Run:

```bash
cd frontend && npm run test -- --run --pool=threads src/features/user/views/__tests__/RoleList.contract.test.js src/features/__tests__/api-contracts.test.js
```

Expected: PASS，角色列表展示只依赖 `permissionCount`，权限配置弹窗会按需请求角色详情。

- [ ] **Step 5: 更新任务表中的 Task 3 检查点**

```md
在 `项目整改执行任务表.md` 追加一条 “T24 Task 3 角色前端契约检查点” 更新记录，注明：
- `roles.js` 已明确区分轻量列表与详情接口
- `RoleList.vue` 已切到 `permissionCount`
- 打开权限配置时会额外调用 `getRole(id)` 获取完整 `permissions`
- 本轮未扩散到其他前端界面文件
```

---

### Task 4: 执行整体验证，并把 T24 更新到任务表

**Files:**
- Modify: `项目整改执行任务表.md`
- Test: `backend/src/test/java/com/example/trace/service/impl/UserServiceImplTest.java`
- Test: `backend/src/test/java/com/example/trace/service/impl/RoleServiceImplTest.java`
- Test: `frontend/src/features/user/views/__tests__/RoleList.contract.test.js`
- Test: `frontend/src/features/__tests__/api-contracts.test.js`

- [ ] **Step 1: 运行后端 T24 相关测试**

Run:

```bash
cd backend && mvn test "-Dtest=UserServiceImplTest,RoleServiceImplTest"
```

Expected: PASS，用户列表与角色列表/详情的新查询模式都被自动化测试锁定。

- [ ] **Step 2: 运行前端角色契约测试**

Run:

```bash
cd frontend && npm run test -- --run --pool=threads src/features/user/views/__tests__/RoleList.contract.test.js src/features/__tests__/api-contracts.test.js
```

Expected: PASS，角色 API 边界与 `RoleList.vue` 的 permissionCount / detail-fetch 逻辑通过。

- [ ] **Step 3: 运行前端构建回归**

Run:

```bash
cd frontend && npm run build
```

Expected: PASS，允许保留既有 chunk warning，但不能出现新的模板、导入或类型错误。

- [ ] **Step 4: 更新任务表，将 T24 切到完成状态**

```md
同步更新 `项目整改执行任务表.md`：
- “当前进行中任务”改为“无”，或切到下一推荐任务
- “最近一次更新摘要”改为 T24 已完成
- “当前任务状态总览”里将 T24 改为 DONE
- “更新记录”补充 T24 最终完成记录
- “下一推荐任务”切到 T25
```

- [ ] **Step 5: 保存最终检查点**

```md
在 T24 最终更新记录里明确写出：
- `GET /api/users` 契约保持不变，但 `listUsers()` 已消除角色查询 N+1
- `GET /api/roles` 已收敛为轻量列表，返回 `permissionCount`
- `GET /api/roles/{id}` 仍返回完整 `permissions`
- 前端仅调整了获准范围内的角色链路，不涉及其他 UI 文件
```
