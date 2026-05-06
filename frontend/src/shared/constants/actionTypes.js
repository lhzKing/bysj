/**
 * 动作类型常量
 * 与后端 ActionType 枚举保持一致
 */

export const ACTION_TYPES = {
  INIT: 'INIT',
  INBOUND: 'INBOUND',
  OUTBOUND: 'OUTBOUND',
  TRANSFER: 'TRANSFER',
  EXCEPTION: 'EXCEPTION',
  CORRECTION: 'CORRECTION'
}

export const ACTION_TYPE_LABELS = {
  [ACTION_TYPES.INIT]: '生产赋码',
  [ACTION_TYPES.INBOUND]: '入库',
  [ACTION_TYPES.OUTBOUND]: '出库',
  [ACTION_TYPES.TRANSFER]: '流转',
  [ACTION_TYPES.EXCEPTION]: '异常',
  [ACTION_TYPES.CORRECTION]: '修正'
}

export const ACTION_TYPE_COLORS = {
  [ACTION_TYPES.INIT]: 'green',
  [ACTION_TYPES.INBOUND]: 'orange',
  [ACTION_TYPES.OUTBOUND]: 'orange',
  [ACTION_TYPES.TRANSFER]: 'blue',
  [ACTION_TYPES.EXCEPTION]: 'red',
  [ACTION_TYPES.CORRECTION]: 'purple'
}

export const TIME_RANGES = [
  { value: 'today', label: '今天' },
  { value: '7d', label: '近7天' },
  { value: '30d', label: '近30天' },
  { value: '180d', label: '近半年' },
  { value: 'all', label: '全部' }
]

export const USER_STATUS = {
  ACTIVE: true,
  INACTIVE: false
}

export const USER_STATUS_LABELS = {
  [USER_STATUS.ACTIVE]: '启用',
  [USER_STATUS.INACTIVE]: '禁用'
}
