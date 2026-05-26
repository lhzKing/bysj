import request from '@/core/api/request'

/**
 * User API module.
 * Frontend programming interface uses camelCase; request.js serializes outgoing HTTP to snake_case.
 */

/**
 * Get a paginated user list.
 * @param {{ page?: number, size?: number, username?: string, roleId?: number | string, status?: number | string }} params
 * @returns {Promise<{list: Array, total: number}>}
 */
export function getUsers(params) {
  return request.get('/users', { params })
}

/**
 * Get user detail by ID.
 * @param {number} id
 * @returns {Promise<Object>}
 */
export function getUser(id) {
  return request.get(`/users/${id}`)
}

/**
 * Create a user from the management console.
 * @param {{ username: string, password: string, roleId: number | string, status?: number }} data
 * @returns {Promise<Object>}
 */
export function createUser(data) {
  return request.post('/users', data)
}

/**
 * Update user profile, role, password, or status fields.
 * @param {number} id
 * @param {{ username?: string, password?: string, roleId?: number | string, status?: number }} data
 * @returns {Promise<Object>}
 */
export function updateUser(id, data) {
  return request.put(`/users/${id}`, data)
}

/**
 * Change a user's role.
 * @param {number} id
 * @param {number | string} roleId
 * @returns {Promise<Object>}
 */
export function updateUserRole(id, roleId) {
  return request.patch(`/users/${id}/role`, null, { params: { roleId } })
}

/**
 * Update a user's enabled/disabled status.
 * @param {number} id
 * @param {boolean | number} status
 * @returns {Promise<Object>}
 */
export function updateUserStatus(id, status) {
  return request.patch(`/users/${id}/status`, null, { params: { status } })
}

/**
 * Delete a user by ID.
 * @param {number} id
 * @returns {Promise<Object>}
 */
export function deleteUser(id) {
  return request.delete(`/users/${id}`)
}

/**
 * Delete multiple users by ID.
 * @param {Array<number>} ids
 * @returns {Promise<Object>}
 */
export function batchDeleteUsers(ids) {
  return request.delete('/users/batch', { data: { ids } })
}

/**
 * Reset a user's password.
 * @param {number} id
 * @param {string} newPassword
 * @returns {Promise<Object>}
 */
export function resetUserPassword(id, newPassword) {
  return request.post(`/users/${id}/reset-password`, { newPassword })
}

/**
 * Get a user's currently bound trace nodes (operable scan locations).
 * Returns rows from trace_user_node_binding joined with trace_node — fields per
 * TraceUserNodeBindingResponse: bindingId, nodeId, nodeCode, nodeName, nodeType,
 * province, city, defaultNode, bindingEnabled, nodeEnabled.
 * @param {number} id
 * @returns {Promise<Array>}
 */
export function getUserTraceNodes(id) {
  return request.get(`/users/${id}/trace-nodes`)
}

/**
 * Replace a user's full set of operable trace-node bindings. Backend swaps the
 * binding rows transactionally.
 * @param {number} id
 * @param {{ nodeIds: Array<number>, defaultNodeId?: number | null }} payload
 * @returns {Promise<Array>}
 */
export function replaceUserTraceNodes(id, payload) {
  return request.put(`/users/${id}/trace-nodes`, payload)
}

/**
 * List every enabled trace_node — used as the selectable pool when editing
 * a user's bindings. Reuses the public /trace-nodes/selectable endpoint.
 * @returns {Promise<Array>}
 */
export function getSelectableTraceNodes() {
  return request.get('/trace-nodes/selectable')
}
