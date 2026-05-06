/**
 * 统一处理后端返回的 snake_case 字段转换。
 * 作为前端契约收口层，对外暴露通用 key 转换与领域 helper。
 */

export function toCamelCaseKey(key) {
  if (typeof key !== 'string') return key
  return key.replace(/_([a-zA-Z0-9])/g, (_, char) => char.toUpperCase())
}

export function toSnakeCaseKey(key) {
  if (typeof key !== 'string') return key
  return key
    .replace(/([A-Z]+)([A-Z][a-z0-9])/g, '$1_$2')
    .replace(/([a-z0-9])([A-Z])/g, '$1_$2')
    .toLowerCase()
}

function isSpecialValue(value) {
  if (!value || typeof value !== 'object') return false

  return (
    value instanceof Date ||
    (typeof FormData !== 'undefined' && value instanceof FormData) ||
    (typeof Blob !== 'undefined' && value instanceof Blob) ||
    (typeof File !== 'undefined' && value instanceof File)
  )
}

function isPlainObject(value) {
  if (!value || typeof value !== 'object') return false
  if (Array.isArray(value) || isSpecialValue(value)) return false

  const prototype = Object.getPrototypeOf(value)
  return prototype === Object.prototype || prototype === null
}

function transformKeys(value, transformKey) {
  if (Array.isArray(value)) {
    return value.map(item => transformKeys(item, transformKey))
  }

  if (!isPlainObject(value)) {
    return value
  }

  return Object.entries(value).reduce((result, [key, nestedValue]) => {
    result[transformKey(key)] = transformKeys(nestedValue, transformKey)
    return result
  }, {})
}

export function transformKeysToCamel(value) {
  return transformKeys(value, toCamelCaseKey)
}

export function transformKeysToSnake(value) {
  return transformKeys(value, toSnakeCaseKey)
}

/**
 * 转换溯源快照字段
 */
export function transformSnapshot(snap) {
  if (!snap) return null
  return transformKeysToCamel(snap)
}

/**
 * 转换溯源日志字段
 */
export function transformTraceLog(log) {
  if (!log) return null
  return transformKeysToCamel(log)
}

/**
 * 转换用户信息字段
 */
export function transformUser(user) {
  if (!user) return null
  return transformKeysToCamel(user)
}

/**
 * 批量转换数组
 */
export function transformArray(array, transformFn) {
  if (!Array.isArray(array)) return []
  return array.map(item => transformFn(item))
}
