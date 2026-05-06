import request from '@/core/api/request'

/**
 * Part API module.
 * Frontend programming interface uses camelCase; request.js serializes outgoing HTTP to snake_case.
 */

/**
 * Get a paginated part list.
 * @param {{ page?: number, size?: number, keyword?: string, partCode?: string, partName?: string, partType?: string, manufacturer?: string }} params
 * @returns {Promise<{list: Array, total: number}>}
 */
export function getParts(params) {
  return request.get('/parts', { params })
}

/**
 * Get part detail by ID.
 * @param {number} id
 * @returns {Promise<Object>}
 */
export function getPart(id) {
  return request.get(`/parts/${id}`)
}

/**
 * Get part detail by part code.
 * @param {string} code
 * @returns {Promise<Object>}
 */
export function getPartByCode(code) {
  return request.get(`/parts/code/${code}`)
}

/**
 * Create a part.
 * @param {{ partCode: string, partName: string, partType: string, manufacturer?: string, model?: string }} data
 * @returns {Promise<Object>}
 */
export function createPart(data) {
  return request.post('/parts', data)
}

/**
 * Update a part.
 * @param {number} id
 * @param {{ partCode?: string, partName?: string, partType?: string, manufacturer?: string, model?: string }} data
 * @returns {Promise<Object>}
 */
export function updatePart(id, data) {
  return request.put(`/parts/${id}`, data)
}

/**
 * Delete a part by ID.
 * @param {number} id
 * @returns {Promise<Object>}
 */
export function deletePart(id) {
  return request.delete(`/parts/${id}`)
}

/**
 * Delete multiple parts by ID.
 * @param {Array<number>} ids
 * @returns {Promise<Object>}
 */
export function batchDeleteParts(ids) {
  return request.delete('/parts/batch', { data: { ids } })
}

/**
 * Get all part types.
 * @returns {Promise<Array<string>>}
 */
export function getPartTypes() {
  return request.get('/parts/types')
}

/**
 * Get all manufacturers.
 * @returns {Promise<Array<string>>}
 */
export function getManufacturers() {
  return request.get('/parts/manufacturers')
}
