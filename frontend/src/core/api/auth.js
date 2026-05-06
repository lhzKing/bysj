import request from '@/core/api/request'

/**
 * Auth API module.
 * Frontend programming interface uses camelCase; request.js serializes outgoing HTTP to snake_case.
 */

/**
 * 用户登录。
 * @param {string} username
 * @param {string} password
 * @param {boolean} rememberMe
 * @returns {Promise<{token: string, user: Object}>}
 */
export function login(username, password, rememberMe = false) {
  return request.post('/auth/login', { username, password, rememberMe })
}

/**
 * 用户注册。
 * @param {string} username
 * @param {string} password
 * @returns {Promise<{token: string, username: string, role: string}>}
 */
export function register(username, password) {
  return request.post('/auth/register', { username, password })
}

/**
 * 获取当前用户信息。
 * @returns {Promise<Object>}
 */
export function getUserInfo() {
  return request.get('/auth/me')
}

/**
 * 用户登出。
 * @returns {Promise<void>}
 */
export function logout() {
  return request.post('/auth/logout')
}
