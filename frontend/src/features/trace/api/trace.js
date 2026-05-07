import request from '@/core/api/request'

/**
 * Trace API module.
 * Frontend programming interface uses camelCase; request.js serializes outgoing HTTP to snake_case.
 */

/**
 * Create one or more trace codes for a part.
 * @param {{ partCode?: string, spuId?: number, manufacturerNode: string, province?: string, city?: string, quantity: number }} data
 * @returns {Promise<{traceCodes: string[]}>}
 */
export function createTrace(data) {
  return request.post('/traces', data)
}

/**
 * Create a trace lifecycle event.
 * @param {string} traceCode
 * @param {{ actionType: string, fromNode?: string, toNode?: string, province?: string, city?: string, eventTime?: string, correctionOf?: number | null, remark?: string }} data
 * @returns {Promise<Object>}
 */
export function createEvent(traceCode, data) {
  return request.post(`/traces/${traceCode}/events`, data)
}

/**
 * Get trace detail and event history.
 * @param {string} traceCode
 * @param {'effective'|'audit'} view
 * @returns {Promise<{snapshot: Object, history: Array, view: string}>}
 */
export function getTraceDetail(traceCode, view = 'effective') {
  return request.get(`/traces/${traceCode}`, {
    params: { view }
  })
}

/**
 * Get scan-time executable actions and recommended action for the current user.
 * @param {string} traceCode
 * @returns {Promise<{traceCode: string, currentStatus: string, currentStatusLabel?: string, currentNode?: string, recommendedAction?: string, availableActions: Array, noActionReason?: string}>}
 */
export function getTraceAvailableActions(traceCode) {
  return request.get(`/traces/${traceCode}/available-actions`)
}

/**
 * Verify the hash and signature chain for a trace code.
 * @param {string} traceCode
 * @returns {Promise<{valid: boolean, totalLogs: number, hashVerifiedCount: number, signatureVerifiedCount: number, errors: Array}>}
 */
export function verifyTraceChain(traceCode) {
  return request.get(`/traces/${traceCode}/verify`)
}


/**
 * Print a generated trace-code label.
 * @param {string} traceCode
 * @param {{ eventTime?: string, remark?: string }} data
 * @returns {Promise<{traceCode: string, actionType: string, codeStatus: string, printCount: number}>}
 */
export function printTraceCode(traceCode, data = {}) {
  return request.post(`/traces/${traceCode}/print`, data)
}

/**
 * Reprint / supplement-print a trace-code label.
 * @param {string} traceCode
 * @param {{ eventTime?: string, remark?: string }} data
 * @returns {Promise<{traceCode: string, actionType: string, codeStatus: string, printCount: number}>}
 */
export function reprintTraceCode(traceCode, data = {}) {
  return request.post(`/traces/${traceCode}/reprint`, data)
}

/**
 * Void an unactivated trace-code label.
 * @param {string} traceCode
 * @param {{ eventTime?: string, remark?: string }} data
 * @returns {Promise<{traceCode: string, actionType: string, codeStatus: string, printCount: number}>}
 */
export function voidTraceCode(traceCode, data = {}) {
  return request.post(`/traces/${traceCode}/void`, data)
}

/**
 * Activate / verify a single trace-code after physical labelling.
 * @param {string} traceCode
 * @param {{ eventTime?: string, activationNode?: string, deviceId?: string, remark?: string }} data
 * @returns {Promise<{traceCode: string, actionType: string, codeStatus: string, activationNode?: string}>}
 */
export function activateTraceCode(traceCode, data = {}) {
  return request.post(`/trace-codes/${traceCode}/activate`, data)
}

/**
 * Get assignment-batch reconciliation detail.
 * @param {number|string} batchId
 * @returns {Promise<Object>}
 */
export function getTraceBatch(batchId) {
  return request.get(`/trace-batches/${batchId}`)
}

/**
 * Get generated code list for an assignment batch.
 * @param {number|string} batchId
 * @returns {Promise<Array>}
 */
export function getTraceBatchCodes(batchId) {
  return request.get(`/trace-batches/${batchId}/codes`)
}

/**
 * Get selectable structured trace nodes.
 * @returns {Promise<Array>}
 */
export function getTraceNodes() {
  return request.get('/trace-nodes/selectable')
}

/**
 * List warehouse/logistics flow tasks.
 * @param {{ taskType?: string, status?: string }} params
 * @returns {Promise<Array>}
 */
export function getTraceFlowTasks(params = {}) {
  return request.get('/trace-flow-tasks', { params })
}

/**
 * Get one warehouse/logistics flow task by numeric id.
 * @param {number|string} taskId
 * @returns {Promise<Object>}
 */
export function getTraceFlowTask(taskId) {
  return request.get(`/trace-flow-tasks/${taskId}`)
}

/**
 * Create a warehouse/logistics flow task.
 * @param {{ taskNo?: string, taskType: string, sourceNodeId: number, targetNodeId: number, expectedQuantity: number, remark?: string }} data
 * @returns {Promise<Object>}
 */
export function createTraceFlowTask(data) {
  return request.post('/trace-flow-tasks', data)
}

/**
 * Scan one trace code inside a warehouse/logistics flow task.
 * @param {number|string} taskId
 * @param {{ traceCode: string, eventTime?: string, idempotencyKey?: string, remark?: string }} data
 * @returns {Promise<Object>}
 */
export function scanTraceFlowTask(taskId, data) {
  return request.post(`/trace-flow-tasks/${taskId}/scan`, data)
}

/**
 * Complete a warehouse/logistics flow task, optionally with discrepancy reason.
 * @param {number|string} taskId
 * @param {{ actualQuantity?: number, remark?: string, discrepancyReason?: string }} data
 * @returns {Promise<Object>}
 */
export function completeTraceFlowTask(taskId, data = {}) {
  return request.post(`/trace-flow-tasks/${taskId}/complete`, data)
}

/**
 * Cancel an open warehouse/logistics flow task.
 * @param {number|string} taskId
 * @returns {Promise<Object>}
 */
export function cancelTraceFlowTask(taskId) {
  return request.post(`/trace-flow-tasks/${taskId}/cancel`)
}

