import request from '@/core/api/request'

/**
 * Trace Aggregation API.
 *
 * 6 个端点对齐 backend/.../controller/TraceAggregationController：
 *   GET    /api/trace-aggregations[?relation_type=]    list all active（工作台一次性拉全部）
 *   POST   /api/trace-aggregations                     bind（装箱/装托）
 *   POST   /api/trace-aggregations/{relationId}/release  release
 *   GET    /api/trace-aggregations/children?parent_code=...
 *   GET    /api/trace-aggregations/parents?child_code=...
 *   GET    /api/trace-aggregations/history/by-parent?parent_code=...
 *   GET    /api/trace-aggregations/history/by-child?child_code=...
 *
 * request.js 已自动做 snake↔camel 转换，前端写 camelCase 即可。
 */

/**
 * List all active aggregations, optionally filtered by relation_type.
 * @param {{ relationType?: 'CARTON'|'PALLET'|'BATCH' }} [params]
 * @returns {Promise<Array>}
 */
export function listActiveAggregations(params = {}) {
  return request.get('/trace-aggregations', { params })
}

/**
 * Bind a child code to a parent carton/pallet.
 * @param {{ parentCode: string, childCode: string, relationType: 'CARTON'|'PALLET', remark?: string }} data
 * @returns {Promise<Object>}
 */
export function bindAggregation(data) {
  return request.post('/trace-aggregations', data)
}

/**
 * Release (unbind) an aggregation relation.
 * @param {number|string} relationId
 * @param {{ remark?: string }} [data]
 * @returns {Promise<Object>}
 */
export function releaseAggregation(relationId, data = {}) {
  return request.post(`/trace-aggregations/${relationId}/release`, data)
}

/**
 * List active children for one parent code.
 * @param {string} parentCode
 * @returns {Promise<Array>}
 */
export function listAggregationChildren(parentCode) {
  return request.get('/trace-aggregations/children', {
    params: { parentCode }
  })
}

/**
 * List active parents for one child code.
 * @param {string} childCode
 * @returns {Promise<Array>}
 */
export function listAggregationParents(childCode) {
  return request.get('/trace-aggregations/parents', {
    params: { childCode }
  })
}

/**
 * List the full history (active + released) under one parent code.
 * @param {string} parentCode
 * @returns {Promise<Array>}
 */
export function listAggregationHistoryByParent(parentCode) {
  return request.get('/trace-aggregations/history/by-parent', {
    params: { parentCode }
  })
}

/**
 * List the full history (active + released) for one child code.
 * @param {string} childCode
 * @returns {Promise<Array>}
 */
export function listAggregationHistoryByChild(childCode) {
  return request.get('/trace-aggregations/history/by-child', {
    params: { childCode }
  })
}
