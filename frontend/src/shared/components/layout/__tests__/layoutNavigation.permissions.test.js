import { describe, expect, it } from 'vitest'
import router from '@/core/router'
import { layoutNavigation } from '@/shared/components/layout/layoutNavigation'
import { PERMISSIONS, TRACE_SCAN_HUB_ACCESS } from '@/shared/constants'

describe('scan permission navigation model', () => {
  it('keeps scan-center route and navigation gated by operational trace permissions only', () => {
    const scanNav = layoutNavigation.find((item) => item.key === 'scan')
    const scanRoute = router.getRoutes().find((route) => route.name === 'scan-hub')

    expect(scanNav).toBeTruthy()
    expect(scanRoute).toBeTruthy()
    expect(scanNav.permissions).toEqual(TRACE_SCAN_HUB_ACCESS)
    expect(scanRoute.meta.permissions).toEqual(TRACE_SCAN_HUB_ACCESS)
    expect(scanNav.permissions).not.toContain(PERMISSIONS.TRACE.VIEW)
    expect(scanRoute.meta.permissions).not.toContain(PERMISSIONS.TRACE.VIEW)
  })

  it('allows fine-grained or super scan permissions into the scan hub, but not trace:view alone', () => {
    const hasAnyPermission = (grantedPermissions = [], requiredPermissions = []) =>
      requiredPermissions.some((permission) => grantedPermissions.includes(permission))

    expect(hasAnyPermission(['trace:view'], TRACE_SCAN_HUB_ACCESS)).toBe(false)
    expect(hasAnyPermission(['trace:inbound', 'trace:view'], TRACE_SCAN_HUB_ACCESS)).toBe(true)
    expect(hasAnyPermission(['trace:exception:handle', 'trace:view'], TRACE_SCAN_HUB_ACCESS)).toBe(true)
    expect(hasAnyPermission(['trace:scan', 'trace:view'], TRACE_SCAN_HUB_ACCESS)).toBe(true)
  })

  it('gates production assignment workbench by assignment business permissions plus legacy trace:create', () => {
    const assignmentNav = layoutNavigation.find((item) => item.key === 'trace-assignment')
    const assignmentRoute = router.getRoutes().find((route) => route.name === 'trace-assignment-workbench')

    expect(assignmentNav).toBeTruthy()
    expect(assignmentRoute).toBeTruthy()
    expect(assignmentNav.permissions).toEqual(PERMISSIONS.TRACE.ASSIGNMENT_ACCESS)
    expect(assignmentRoute.meta.permissions).toEqual(PERMISSIONS.TRACE.ASSIGNMENT_ACCESS)
    expect(assignmentNav.permissions).toEqual([
      PERMISSIONS.TRACE.BATCH_CREATE,
      PERMISSIONS.TRACE.CODE_PRINT,
      PERMISSIONS.TRACE.CODE_ACTIVATE,
      PERMISSIONS.TRACE.CREATE
    ])
  })

  it('gates warehouse logistics task workbench by task business permissions plus legacy scan permissions', () => {
    const taskNav = layoutNavigation.find((item) => item.key === 'trace-flow-tasks')
    const taskRoute = router.getRoutes().find((route) => route.name === 'trace-flow-task-workbench')

    expect(taskNav).toBeTruthy()
    expect(taskRoute).toBeTruthy()
    expect(taskNav.permissions).toEqual(PERMISSIONS.TRACE.FLOW_TASK_ACCESS)
    expect(taskRoute.meta.permissions).toEqual(PERMISSIONS.TRACE.FLOW_TASK_ACCESS)
    expect(taskNav.permissions).toContain(PERMISSIONS.TRACE.TASK_CREATE)
    expect(taskNav.permissions).toContain(PERMISSIONS.TRACE.TASK_SCAN)
    expect(taskNav.permissions).toContain(PERMISSIONS.TRACE.TASK_COMPLETE)
    expect(taskNav.permissions).not.toContain(PERMISSIONS.TRACE.VIEW)
    expect(taskRoute.meta.permissions).not.toContain(PERMISSIONS.TRACE.VIEW)
  })

  it('exposes /scan-trace as the trace:view landing for read-only users (USER role)', () => {
    const landingNav = layoutNavigation.find((item) => item.key === 'scan-trace-landing')
    const landingRoute = router.getRoutes().find((route) => route.name === 'scan-trace-landing')

    expect(landingNav).toBeTruthy()
    expect(landingRoute).toBeTruthy()
    expect(landingNav.permissions).toEqual([PERMISSIONS.TRACE.VIEW])
    expect(landingRoute.meta.permissions).toEqual([PERMISSIONS.TRACE.VIEW])
  })

  it('gates /traces full-table list by business/audit permissions, not bare trace:view', () => {
    // 关键安全门：纯只读用户（仅有 trace:view）不能进 /traces 列表全表，
    // 防止低权限角色查看所有产品 / 客户 / 节点的隐私数据
    const tracesNav = layoutNavigation.find((item) => item.key === 'traces')
    const tracesRoute = router.getRoutes().find((route) => route.name === 'traces')

    expect(tracesNav).toBeTruthy()
    expect(tracesRoute).toBeTruthy()
    expect(tracesNav.permissions).toEqual(PERMISSIONS.TRACE.LIST_ACCESS)
    expect(tracesRoute.meta.permissions).toEqual(PERMISSIONS.TRACE.LIST_ACCESS)
    expect(tracesNav.permissions).not.toContain(PERMISSIONS.TRACE.VIEW)

    // 业务角色至少有一个能命中
    const hasAny = (granted, required) => required.some((p) => granted.includes(p))
    expect(hasAny(['trace:create'], tracesNav.permissions)).toBe(true)        // PRODUCER
    expect(hasAny(['trace:inbound'], tracesNav.permissions)).toBe(true)       // WAREHOUSE
    expect(hasAny(['trace:transfer'], tracesNav.permissions)).toBe(true)      // LOGISTICS
    expect(hasAny(['trace:audit:view'], tracesNav.permissions)).toBe(true)    // ADMIN / SUPER_ADMIN
    // USER 角色（仅 trace:view + dashboard:view）应被挡在外面
    expect(hasAny(['trace:view', 'dashboard:view'], tracesNav.permissions)).toBe(false)
  })

})
