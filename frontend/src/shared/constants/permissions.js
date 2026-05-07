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
export const TRACE_BATCH_CREATE = 'trace:batch:create'
export const TRACE_CODE_PRINT = 'trace:code:print'
export const TRACE_CODE_ACTIVATE = 'trace:code:activate'
export const TRACE_SCAN = 'trace:scan'
export const TRACE_INBOUND = 'trace:inbound'
export const TRACE_OUTBOUND = 'trace:outbound'
export const TRACE_TRANSFER = 'trace:transfer'
export const TRACE_TASK_CREATE = 'trace:task:create'
export const TRACE_TASK_SCAN = 'trace:task:scan'
export const TRACE_TASK_COMPLETE = 'trace:task:complete'
export const TRACE_EXCEPTION_HANDLE = 'trace:exception:handle'
export const TRACE_AUDIT_VIEW = 'trace:audit:view'
export const TRACE_ASSIGNMENT_ACCESS = [
  TRACE_BATCH_CREATE,
  TRACE_CODE_PRINT,
  TRACE_CODE_ACTIVATE,
  TRACE_CREATE
]
export const TRACE_FLOW_TASK_ACCESS = [
  TRACE_TASK_CREATE,
  TRACE_TASK_SCAN,
  TRACE_TASK_COMPLETE,
  TRACE_SCAN,
  TRACE_INBOUND,
  TRACE_OUTBOUND,
  TRACE_TRANSFER
]
export const TRACE_SCAN_HUB_ACCESS = [
  TRACE_CREATE,
  TRACE_BATCH_CREATE,
  TRACE_SCAN,
  TRACE_INBOUND,
  TRACE_OUTBOUND,
  TRACE_TRANSFER,
  TRACE_EXCEPTION_HANDLE
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
    BATCH_CREATE: TRACE_BATCH_CREATE,
    CODE_PRINT: TRACE_CODE_PRINT,
    CODE_ACTIVATE: TRACE_CODE_ACTIVATE,
    SCAN: TRACE_SCAN,
    INBOUND: TRACE_INBOUND,
    OUTBOUND: TRACE_OUTBOUND,
    TRANSFER: TRACE_TRANSFER,
    TASK_CREATE: TRACE_TASK_CREATE,
    TASK_SCAN: TRACE_TASK_SCAN,
    TASK_COMPLETE: TRACE_TASK_COMPLETE,
    EXCEPTION_HANDLE: TRACE_EXCEPTION_HANDLE,
    AUDIT_VIEW: TRACE_AUDIT_VIEW,
    ASSIGNMENT_ACCESS: TRACE_ASSIGNMENT_ACCESS,
    FLOW_TASK_ACCESS: TRACE_FLOW_TASK_ACCESS,
    SCAN_HUB_ACCESS: TRACE_SCAN_HUB_ACCESS
  }
}
