/**
 * 权限常量定义
 * 与后端当前权限模型保持一致
 */

export const DASHBOARD_VIEW = 'dashboard:view'

export const USER_VIEW = 'user:view'
export const USER_MANAGE = 'user:manage'

export const ROLE_VIEW = 'role:view'
export const ROLE_MANAGE = 'role:manage'

export const PART_VIEW = 'part:view'
export const PART_MANAGE = 'part:manage'

export const TRACE_VIEW = 'trace:view'
export const TRACE_CREATE = 'trace:create'
export const TRACE_SCAN = 'trace:scan'
export const TRACE_INBOUND = 'trace:inbound'
export const TRACE_OUTBOUND = 'trace:outbound'
export const TRACE_TRANSFER = 'trace:transfer'
export const TRACE_SCAN_HUB_ACCESS = [
  TRACE_CREATE,
  TRACE_SCAN,
  TRACE_INBOUND,
  TRACE_OUTBOUND,
  TRACE_TRANSFER
]

export const PERMISSIONS = {
  DASHBOARD: {
    VIEW: DASHBOARD_VIEW
  },
  USER: {
    VIEW: USER_VIEW,
    MANAGE: USER_MANAGE
  },
  ROLE: {
    VIEW: ROLE_VIEW,
    MANAGE: ROLE_MANAGE
  },
  PART: {
    VIEW: PART_VIEW,
    MANAGE: PART_MANAGE
  },
  TRACE: {
    VIEW: TRACE_VIEW,
    CREATE: TRACE_CREATE,
    SCAN: TRACE_SCAN,
    INBOUND: TRACE_INBOUND,
    OUTBOUND: TRACE_OUTBOUND,
    TRANSFER: TRACE_TRANSFER,
    SCAN_HUB_ACCESS: TRACE_SCAN_HUB_ACCESS
  }
}
