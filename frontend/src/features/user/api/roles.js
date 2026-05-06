import request from '@/core/api/request'

/**
 * Role API module.
 * Frontend programming interface uses camelCase; request.js serializes outgoing HTTP to snake_case.
 */

/**
 * 获取角色轻量列表。
 * 仅返回列表展示所需的基础字段与 permissionCount，不返回完整 permissions。
 * @returns {Promise<Array<{ id: number, roleCode: string, roleName: string, remark?: string, permissionCount: number }>>}
 */
export function getRoles() {
  return request.get('/roles')
}

/**
 * 获取角色详情。
 * 返回完整角色信息与 permissions，用于按需加载详情场景。
 * @param {number} id
 * @returns {Promise<{ id: number, roleCode: string, roleName: string, remark?: string, permissionCount?: number, permissions: Array }>}
 */
export function getRole(id) {
  return request.get(`/roles/${id}`)
}

/**
 * 创建角色。
 * @param {{ roleCode: string, roleName: string, remark?: string }} data
 * @returns {Promise<Object>}
 */
export function createRole(data) {
  return request.post('/roles', data)
}

/**
 * 更新角色。
 * @param {number} id
 * @param {{ roleCode?: string, roleName?: string, remark?: string }} data
 * @returns {Promise<Object>}
 */
export function updateRole(id, data) {
  return request.put(`/roles/${id}`, data)
}

/**
 * 删除角色。
 * @param {number} id
 * @returns {Promise<Object>}
 */
export function deleteRole(id) {
  return request.delete(`/roles/${id}`)
}

/**
 * 给角色分配权限。
 * @param {number} id
 * @param {Array<number>} permissionIds
 * @returns {Promise<Object>}
 */
export function assignPermissions(id, permissionIds) {
  return request.put(`/roles/${id}/permissions`, { permissionIds })
}

/**
 * 获取全部权限。
 * @returns {Promise<Array>}
 */
export function getPermissions() {
  return request.get('/roles/permissions')
}
