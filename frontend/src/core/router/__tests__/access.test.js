import { describe, expect, it } from 'vitest'
import { resolveAccessibleRoute } from '@/core/router/access'

/**
 * 路由 fallback 选择器是登录后死循环修复的核心 —— 测试一下：
 * - 没权限：返回 null
 * - 只有 dashboard:view：返回 /
 * - 没 dashboard:view 但有 trace:view：返回 /traces（避免普通用户卡在 login）
 * - 只有 part:view：跳到 /parts
 * - userStore 不规范（缺方法）：不抛错，返回 null
 */
function makeStore(perms) {
  return {
    hasAnyPermission(required) {
      if (!required || required.length === 0) return true
      return required.some((p) => perms.includes(p))
    }
  }
}

describe('resolveAccessibleRoute', () => {
  it('returns null for users with no permissions (caller should log them out)', () => {
    expect(resolveAccessibleRoute(makeStore([]))).toBeNull()
  })

  it('returns / when dashboard:view is granted (most common case)', () => {
    expect(resolveAccessibleRoute(makeStore(['dashboard:view']))).toBe('/')
  })

  it('falls back to /scan-trace for read-only users (trace:view only, no business writes)', () => {
    // USER 角色撤销 dashboard:view 后只剩 trace:view —— 不该看到全表列表 /traces，
    // 应当落到 /scan-trace 个人扫码页（只能扫码 + 输入码 → 跳详情）
    expect(resolveAccessibleRoute(makeStore(['trace:view']))).toBe('/scan-trace')
  })

  it('falls back to /traces for business users who have write/audit perms', () => {
    // PRODUCER 类用户（有业务写权限）才能进全表列表
    expect(resolveAccessibleRoute(makeStore(['trace:create']))).toBe('/traces')
  })

  it('falls back to /parts when only part:view is granted (admin-lite)', () => {
    expect(resolveAccessibleRoute(makeStore(['part:view']))).toBe('/parts')
  })

  it('returns null safely when userStore is null or missing hasAnyPermission', () => {
    expect(resolveAccessibleRoute(null)).toBeNull()
    expect(resolveAccessibleRoute({})).toBeNull()
    expect(resolveAccessibleRoute({ hasAnyPermission: 'not-a-function' })).toBeNull()
  })
})
