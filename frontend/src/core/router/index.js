import { createRouter, createWebHistory } from 'vue-router'
import { setUnauthorizedHandler } from '@/core/api/request'
import { useUserStore } from '@/core/stores/user'
import { PERMISSIONS, TRACE_SCAN_HUB_ACCESS } from '@/shared/constants'
import { resolveAccessibleRoute } from '@/core/router/access'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/login',
      name: 'login',
      component: () => import('@/shared/components/Login.vue'),
      meta: { title: '登录' }
    },
    {
      path: '/public/traces/:code',
      name: 'public-trace-verify',
      component: () => import('@/features/trace/views/TracePublicView.vue'),
      meta: { title: '追溯码自助验签' }
    },
    {
      path: '/camera-test',
      name: 'camera-test',
      component: () => import('@/views/CameraTest.vue'),
      meta: { title: '摄像头测试' }
    },
    {
      path: '/',
      component: () => import('@/shared/components/layout/MainLayout.vue'),
      meta: { requiresAuth: true },
      children: [
        {
          path: '',
          name: 'dashboard',
          component: () => import('@/features/dashboard/views/Dashboard.vue'),
          meta: {
            title: '仪表盘',
            permissions: [PERMISSIONS.DASHBOARD.VIEW]
          }
        },

        {
          path: 'trace-assignment',
          name: 'trace-assignment-workbench',
          component: () => import('@/features/trace/views/TraceAssignmentWorkbench.vue'),
          meta: {
            title: '生产赋码工作台',
            permissions: PERMISSIONS.TRACE.ASSIGNMENT_ACCESS
          }
        },
        {
          path: 'trace-flow-tasks',
          name: 'trace-flow-task-workbench',
          component: () => import('@/features/trace/views/TraceFlowTaskWorkbench.vue'),
          meta: {
            title: '仓库/物流任务工作台',
            permissions: PERMISSIONS.TRACE.FLOW_TASK_ACCESS
          }
        },
        {
          path: 'trace-aggregations',
          name: 'trace-aggregation-workbench',
          component: () => import('@/features/trace/views/TraceAggregationWorkbench.vue'),
          meta: {
            title: '箱码 / 托盘码聚合',
            // 仅能做装箱/装托的业务角色可进；纯只读 USER（仅 trace:view）被挡
            permissions: PERMISSIONS.TRACE.AGGREGATION_ACCESS
          }
        },
        {
          path: 'traces',
          name: 'traces',
          component: () => import('@/features/trace/views/TraceList.vue'),
          meta: {
            title: '追溯查询',
            // 仅业务/审计/管理角色能进全表列表；纯 trace:view 的 USER 角色走 /scan-trace
            permissions: PERMISSIONS.TRACE.LIST_ACCESS
          }
        },
        {
          path: 'scan-trace',
          name: 'scan-trace-landing',
          component: () => import('@/features/trace/views/TraceScanLanding.vue'),
          meta: {
            title: '扫码查询',
            permissions: [PERMISSIONS.TRACE.VIEW]
          }
        },
        {
          path: 'scan',
          name: 'scan-hub',
          component: () => import('@/features/trace/views/ScanHub.vue'),
          meta: {
            title: '扫码工位',
            permissions: TRACE_SCAN_HUB_ACCESS
          }
        },
        {
          path: 'traces/:code',
          name: 'trace-detail',
          component: () => import('@/features/trace/views/TraceDetail.vue'),
          meta: {
            title: '溯源详情',
            permissions: [PERMISSIONS.TRACE.VIEW]
          }
        },
        {
          path: 'users',
          name: 'users',
          component: () => import('@/features/user/views/UserList.vue'),
          meta: {
            title: '用户管理',
            permissions: [PERMISSIONS.USER.VIEW]
          }
        },
        {
          path: 'roles',
          name: 'roles',
          component: () => import('@/features/user/views/RoleList.vue'),
          meta: {
            title: '角色管理',
            permissions: [PERMISSIONS.ROLE.VIEW]
          }
        },
        {
          path: 'parts',
          name: 'parts',
          component: () => import('@/features/part/views/PartList.vue'),
          meta: {
            title: '配件管理',
            permissions: [PERMISSIONS.PART.VIEW]
          }
        }
      ]
    },
    {
      path: '/:pathMatch(.*)*',
      name: 'not-found',
      component: () => import('@/shared/components/NotFound.vue'),
      meta: { title: '页面不存在' }
    }
  ]
})

router.beforeEach(async (to, from, next) => {
  const store = useUserStore()

  if (to.meta.title) {
    document.title = `${to.meta.title} - 工业溯源系统`
  }

  if (to.meta.requiresAuth && !store.isLoggedIn) {
    next({
      path: '/login',
      query: { redirect: to.fullPath }
    })
    return
  }

  if (to.path === '/login' && store.isLoggedIn) {
    // 已登录但又访问 /login —— 找一个用户能进的页面跳过去
    const fallback = resolveAccessibleRoute(store)
    if (fallback) {
      next(fallback)
    } else {
      // 登录态但无任何可访问页面（极端配置或权限被全撤）—— 退出登录，留在 login 并提示
      await store.logout()
      next({ path: '/login', query: { error: 'no-access' }, replace: true })
    }
    return
  }

  if (to.meta.permissions && Array.isArray(to.meta.permissions) && to.meta.permissions.length > 0) {
    const hasPermission = store.hasAnyPermission(to.meta.permissions)

    if (!hasPermission) {
      console.warn(`Access denied: missing permissions [${to.meta.permissions.join(', ')}]`)
      const fallback = resolveAccessibleRoute(store)
      if (fallback && fallback !== to.path) {
        next({ path: fallback, replace: true })
      } else if (!fallback) {
        // 用户登录但无任何可访问页面 —— 退出 + 回 login 带 error 标记
        await store.logout()
        next({ path: '/login', query: { error: 'no-access' }, replace: true })
      } else {
        // fallback === to.path：理论上不会进来（同一守卫已 hasPermission=true 才到这），保险走 false
        next(false)
      }
      return
    }
  }

  next()
})

setUnauthorizedHandler(() => {
  router.push({
    path: '/login',
    query: { redirect: router.currentRoute.value.fullPath }
  }).catch(() => {})
})

export default router
