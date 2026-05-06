import {
  FileText as Document,
  Camera,
  Factory,
  Truck,
  Search,
  User,
  Lock,
  Box
} from 'lucide-vue-next'
import { PERMISSIONS, TRACE_SCAN_HUB_ACCESS } from '@/shared/constants'

export const layoutNavigation = [
  {
    key: 'dashboard',
    label: '\u4eea\u8868\u76d8',
    eyebrow: '\u4e3b\u63a7\u9762\u677f',
    title: '\u4eea\u8868\u76d8',
    subtitle: '\u67e5\u770b\u7cfb\u7edf\u603b\u89c8\u4e0e\u5173\u952e\u6307\u6807',
    path: '/',
    icon: Document,
    permissions: [PERMISSIONS.DASHBOARD.VIEW]
  },
  {
    key: 'scan',
    label: '\u626b\u7801\u4e2d\u5fc3',
    eyebrow: '\u6267\u884c\u5165\u53e3',
    title: '\u626b\u7801\u4e2d\u5fc3',
    subtitle: '\u5feb\u901f\u5b8c\u6210\u626b\u7801\u3001\u51fa\u5165\u5e93\u4e0e\u6d41\u8f6c\u64cd\u4f5c',
    path: '/scan',
    icon: Camera,
    permissions: TRACE_SCAN_HUB_ACCESS
  },

  {
    key: 'trace-assignment',
    label: '\u751f\u4ea7\u8d4b\u7801',
    eyebrow: '\u751f\u4ea7\u5de5\u4f5c\u53f0',
    title: '\u751f\u4ea7\u8d4b\u7801\u5de5\u4f5c\u53f0',
    subtitle: '\u6309\u6279\u6b21\u751f\u6210\u3001\u6253\u5370\u3001\u6fc0\u6d3b\u548c\u5bf9\u8d26\u5355\u54c1\u7801',
    path: '/trace-assignment',
    icon: Factory,
    permissions: [PERMISSIONS.TRACE.CREATE]
  },
  {
    key: 'trace-flow-tasks',
    label: '\u4ed3\u5e93\u7269\u6d41',
    eyebrow: '\u4efb\u52a1\u5de5\u4f5c\u53f0',
    title: '\u4ed3\u5e93/\u7269\u6d41\u4efb\u52a1\u5de5\u4f5c\u53f0',
    subtitle: '\u6309\u5f85\u529e\u4efb\u52a1\u8fde\u7eed\u626b\u7801\uff0c\u81ea\u52a8\u7d2f\u8ba1\u5df2\u626b/\u5e94\u626b\u6570\u91cf',
    path: '/trace-flow-tasks',
    icon: Truck,
    permissions: [
      PERMISSIONS.TRACE.SCAN,
      PERMISSIONS.TRACE.INBOUND,
      PERMISSIONS.TRACE.OUTBOUND,
      PERMISSIONS.TRACE.TRANSFER
    ]
  },
  {
    key: 'traces',
    label: '\u6eaf\u6e90\u7ba1\u7406',
    eyebrow: '\u4e1a\u52a1\u7ba1\u7406',
    title: '\u6eaf\u6e90\u7ba1\u7406',
    subtitle: '\u7edf\u4e00\u7ef4\u62a4\u6eaf\u6e90\u94fe\u8def\u4e0e\u8bb0\u5f55',
    path: '/traces',
    icon: Search,
    permissions: [PERMISSIONS.TRACE.VIEW]
  },
  {
    key: 'users',
    label: '\u7528\u6237\u7ba1\u7406',
    eyebrow: '\u7cfb\u7edf\u7ba1\u7406',
    title: '\u7528\u6237\u7ba1\u7406',
    subtitle: '\u7ef4\u62a4\u7cfb\u7edf\u8d26\u53f7\u4e0e\u7528\u6237\u8d44\u6599',
    path: '/users',
    icon: User,
    permissions: [PERMISSIONS.USER.VIEW]
  },
  {
    key: 'roles',
    label: '\u89d2\u8272\u7ba1\u7406',
    eyebrow: '\u7cfb\u7edf\u7ba1\u7406',
    title: '\u89d2\u8272\u7ba1\u7406',
    subtitle: '\u7ba1\u7406\u89d2\u8272\u6743\u9650\u4e0e\u6388\u6743\u8303\u56f4',
    path: '/roles',
    icon: Lock,
    permissions: [PERMISSIONS.ROLE.VIEW]
  },
  {
    key: 'parts',
    label: '\u914d\u4ef6\u7ba1\u7406',
    eyebrow: '\u57fa\u7840\u8d44\u6599',
    title: '\u914d\u4ef6\u7ba1\u7406',
    subtitle: '\u7ef4\u62a4\u914d\u4ef6\u8d44\u6599\u4e0e\u53ef\u7528\u5e93\u5b58\u4fe1\u606f',
    path: '/parts',
    icon: Box,
    permissions: [PERMISSIONS.PART.VIEW]
  }
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
