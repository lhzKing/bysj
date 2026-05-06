import request from '@/core/api/request'

/**
 * Dashboard API module.
 * Frontend programming interface uses camelCase; request.js serializes outgoing HTTP to snake_case.
 */

/**
 * Get KPI data for the dashboard.
 * @param {string} range
 * @returns {Promise<{totalTraces: number, todayNew: number, totalLogs: number, exceptionCount: number, range: string}>}
 */
export function getKPI(range = '30d') {
  return request.get('/dashboard/kpi', { params: { range } })
}

/**
 * Get map aggregation data for the dashboard.
 * @param {string} range
 * @returns {Promise<{items: Array<{province: string, count: number}>, total: number, range: string}>}
 */
export function getMapData(range = '30d') {
  return request.get('/dashboard/map', { params: { range } })
}

/**
 * Get trend data for the dashboard.
 * @param {string} range
 * @returns {Promise<{items: Array<{date?: string, hour?: string, label?: string, count: number}>, total: number, range: string}>}
 */
export function getTrend(range = '30d') {
  return request.get('/dashboard/trend', { params: { range } })
}

/**
 * Get topology graph data for the dashboard.
 * @param {string | null} traceCode
 * @param {string} range
 * @returns {Promise<{nodes: Array, links: Array, range: string}>}
 */
export function getTopology(traceCode, range = '30d') {
  return request.get('/dashboard/topology', { params: { traceCode, range } })
}
