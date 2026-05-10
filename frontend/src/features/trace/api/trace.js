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
 * Paged trace list with multi-criteria filters.
 *
 * @param {Object} params
 * @param {string=} params.keyword         模糊匹配 trace_code / SPU 名称 / SPU 编码 / current_owner
 * @param {string=} params.status          状态精确（多值用逗号），INIT/IN_STOCK/IN_TRANSIT/TRANSFERRED/EXCEPTION
 * @param {number=} params.spuId
 * @param {string=} params.batchNo
 * @param {string=} params.currentNode
 * @param {string=} params.currentOwner
 * @param {string=} params.province
 * @param {string=} params.eventTimeFrom   ISO-8601
 * @param {string=} params.eventTimeTo     ISO-8601
 * @param {number=} params.page            默认 1
 * @param {number=} params.size            默认 10，最大 200
 * @param {string=} params.sort            last_event_time(默认) / trace_code / update_time / current_status
 * @param {string=} params.order           asc / desc(默认)
 * @returns {Promise<{list: Array, total: number, page: number, size: number, totalPages: number}>}
 */
export function listTraces(params = {}) {
  return request.get('/traces', { params })
}

/**
 * Create a trace lifecycle event.
 * @param {string} traceCode
 * @param {{ actionType: string, fromNode?: string, toNode?: string, province?: string, city?: string, eventTime?: string, correctionOf?: number | null, remark?: string, idempotencyKey?: string }} data
 * @returns {Promise<Object>}
 */
export function createEvent(traceCode, data) {
  return request.post(`/traces/${traceCode}/events`, data)
}

/**
 * Close an exception hold and restore the pre-freeze snapshot state.
 * @param {string} traceCode
 * @param {{ remark: string, eventTime?: string, idempotencyKey?: string }} data
 * @returns {Promise<Object>}
 */
export function closeTraceException(traceCode, data) {
  return request.post(`/traces/${traceCode}/exception/close`, data)
}

/**
 * Append an auditable correction record for an existing lifecycle log.
 * @param {string} traceCode
 * @param {{ correctionOf: number|string, remark: string, fromNode?: string, toNode?: string, province?: string, city?: string, eventTime?: string, idempotencyKey?: string }} data
 * @returns {Promise<Object>}
 */
export function createTraceCorrection(traceCode, data) {
  return request.post(`/traces/${traceCode}/corrections`, data)
}

/**
 * Get trace detail and event history.
 * @param {string} traceCode
 * @param {'effective'|'audit'} view
 * @returns {Promise<{snapshot: Object, history: Array, view: string, aggregationHistory: Array}>}
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
 * 普通扫码弹窗运单驱动用：列出该追溯码当前可参与的开放运单（CREATED/PROCESSING）。
 * 选定后用 candidate.prefill* 字段自动填入弹窗，并在提交时改走 /trace-flow-tasks/{id}/scan
 * 以联动任务的 actualQuantity / status，把"普通扫码 + 任务扫码"两条链路接通。
 * @param {string} traceCode
 * @returns {Promise<Array<{id: number, taskNo: string, taskType: string, taskTypeLabel: string, status: string, statusLabel: string, sourceNodeName: string, targetNodeName: string, expectedQuantity: number, actualQuantity: number, remainingQuantity: number, compatibleActionType: string, prefillFromNode: string, prefillToNode: string, prefillProvince: string, prefillCity: string}>>}
 */
export function getTraceCandidateFlowTasks(traceCode) {
  return request.get(`/traces/${traceCode}/candidate-flow-tasks`)
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
