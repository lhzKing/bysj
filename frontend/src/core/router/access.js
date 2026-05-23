import { PERMISSIONS, TRACE_SCAN_HUB_ACCESS } from '@/shared/constants'

/**
 * 按用户权限挑一个能进的首页。
 *
 * 用途：
 * - 登录成功后没有 redirect query 时，决定落到哪里
 * - router beforeEach 里某条路由权限不足时，跳到 fallback 而不是死磕 `/`
 *
 * 否则会出现这种死循环：USER 角色没 `dashboard:view` → router.push('/')
 * → 守卫拒绝 → next('/') → 又被同一守卫拒绝 → vue-router 静默 abort，
 * 用户卡在 login 页（外观像登录失败，实际 token 已发）。
 *
 * 候选顺序：业务最常见 → 管理类。命中第一条 `hasAnyPermission` 即返回。
 * 全都不命中返回 null —— 调用方应当 logout + 留在 /login 并提示无权限。
 */
const ACCESSIBLE_ROUTES = [
  { path: '/', perms: [PERMISSIONS.DASHBOARD.VIEW] },
  // 纯 trace:view 用户（USER 角色）落到这里——他们不该看全表列表
  { path: '/scan-trace', perms: [PERMISSIONS.TRACE.VIEW] },
  { path: '/traces', perms: PERMISSIONS.TRACE.LIST_ACCESS },
  { path: '/scan', perms: TRACE_SCAN_HUB_ACCESS },
  { path: '/trace-flow-tasks', perms: PERMISSIONS.TRACE.FLOW_TASK_ACCESS },
  { path: '/trace-assignment', perms: PERMISSIONS.TRACE.ASSIGNMENT_ACCESS },
  { path: '/parts', perms: [PERMISSIONS.PART.VIEW] },
  { path: '/users', perms: [PERMISSIONS.USER.VIEW] },
  { path: '/roles', perms: [PERMISSIONS.ROLE.VIEW] }
]

/**
 * @param {{ hasAnyPermission: (perms: string[]) => boolean }} userStore
 * @returns {string | null}
 */
export function resolveAccessibleRoute(userStore) {
  if (!userStore || typeof userStore.hasAnyPermission !== 'function') return null
  for (const candidate of ACCESSIBLE_ROUTES) {
    if (userStore.hasAnyPermission(candidate.perms)) {
      return candidate.path
    }
  }
  return null
}
