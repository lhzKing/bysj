import { createRouter, createWebHistory } from 'vue-router'
import { setUnauthorizedHandler } from '@/core/api/request'
import { useUserStore } from '@/core/stores/user'
import { PERMISSIONS, TRACE_SCAN_HUB_ACCESS } from '@/shared/constants'

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
          path: 'traces',
          name: 'traces',
          component: () => import('@/features/trace/views/TraceList.vue'),
          meta: {
            title: '溯源管理',
            permissions: [PERMISSIONS.TRACE.VIEW]
          }
        },
        {
          path: 'scan',
          name: 'scan-hub',
          component: () => import('@/features/trace/views/ScanHub.vue'),
          meta: {
            title: '扫码中心',
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

router.beforeEach((to, from, next) => {
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
    next('/')
    return
  }

  if (to.meta.permissions && Array.isArray(to.meta.permissions) && to.meta.permissions.length > 0) {
    const hasPermission = store.hasAnyPermission(to.meta.permissions)

    if (!hasPermission) {
      console.warn(`Access denied: missing permissions [${to.meta.permissions.join(', ')}]`)
      next({ path: '/', replace: true })
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
