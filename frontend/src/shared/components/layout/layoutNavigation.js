import {
  LayoutDashboard,
  ScanLine,
  Factory,
  Truck,
  Search,
  Users,
  Shield,
  Package,
  Boxes
} from 'lucide-vue-next'
import { PERMISSIONS, TRACE_SCAN_HUB_ACCESS } from '@/shared/constants'

/**
 * Linear-style sidebar navigation model.
 *
 * Each item carries:
 *  - key:         stable identifier used by tests / activePath resolver
 *  - section:     'main' | 'manage' — controls grouping in sidebar
 *  - label:       sidebar nav label (also rendered in mobile drawer)
 *  - path:        router path used by RouterLink / push
 *  - icon:        lucide-vue-next component rendered as 14×14 stroke icon
 *  - kbd:         optional keyboard hint shown right-aligned in the row
 *  - permissions: list of permission codes; user with ANY of these can see the entry
 */
export const layoutNavigation = [
  {
    key: 'dashboard',
    section: 'main',
    label: '仪表盘',
    path: '/',
    icon: LayoutDashboard,
    kbd: 'G D',
    permissions: [PERMISSIONS.DASHBOARD.VIEW]
  },
  {
    key: 'scan',
    section: 'main',
    label: '扫码工位',
    path: '/scan',
    icon: ScanLine,
    kbd: 'G S',
    permissions: TRACE_SCAN_HUB_ACCESS
  },
  {
    key: 'trace-assignment',
    section: 'main',
    label: '生产赋码',
    path: '/trace-assignment',
    icon: Factory,
    permissions: PERMISSIONS.TRACE.ASSIGNMENT_ACCESS
  },
  {
    key: 'trace-flow-tasks',
    section: 'main',
    label: '仓库物流',
    path: '/trace-flow-tasks',
    icon: Truck,
    permissions: PERMISSIONS.TRACE.FLOW_TASK_ACCESS
  },
  {
    key: 'trace-aggregations',
    section: 'main',
    label: '聚合关系',
    path: '/trace-aggregations',
    icon: Boxes,
    permissions: PERMISSIONS.TRACE.AGGREGATION_ACCESS
  },
  {
    key: 'scan-trace-landing',
    section: 'main',
    label: '扫码查询',
    path: '/scan-trace',
    icon: ScanLine,
    permissions: [PERMISSIONS.TRACE.VIEW]
  },
  {
    key: 'traces',
    section: 'main',
    label: '追溯查询',
    path: '/traces',
    icon: Search,
    kbd: 'G T',
    permissions: PERMISSIONS.TRACE.LIST_ACCESS
  },
  {
    key: 'parts',
    section: 'main',
    label: '配件管理',
    path: '/parts',
    icon: Package,
    permissions: [PERMISSIONS.PART.VIEW]
  },
  {
    key: 'users',
    section: 'manage',
    label: '用户管理',
    path: '/users',
    icon: Users,
    permissions: [PERMISSIONS.USER.VIEW]
  },
  {
    key: 'roles',
    section: 'manage',
    label: '角色管理',
    path: '/roles',
    icon: Shield,
    permissions: [PERMISSIONS.ROLE.VIEW]
  }
]

/**
 * Section header copy. `label === null` means render no header (default group).
 */
export const NAVIGATION_SECTIONS = [
  { key: 'main', label: null },
  { key: 'manage', label: '管理' }
]

export function resolveActivePath(currentPath = '/') {
  if (!currentPath || currentPath === '/') {
    return '/'
  }

  const matchedItem = [...layoutNavigation]
    .sort((left, right) => right.path.length - left.path.length)
    .find((item) => currentPath === item.path || currentPath.startsWith(item.path + '/'))

  return matchedItem?.path || '/'
}
