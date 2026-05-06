import axios from 'axios'
import { useToast } from '@/shared/composables/useToast'
import { clearAuthSession, readToken } from '@/core/auth/authStorage'
import { transformKeysToCamel, transformKeysToSnake } from '@/shared/utils/transform'

/**
 * 核心 Axios 请求实例
 * 包含请求/响应拦截器，统一错误处理
 */
const request = axios.create({
  baseURL: '/api',
  timeout: 10000,
})

const toast = useToast()
let unauthorizedHandler = null

export function setUnauthorizedHandler(handler) {
  unauthorizedHandler = typeof handler === 'function' ? handler : null
}

export function normalizeRequestConfig(config = {}) {
  const normalizedConfig = {
    ...config,
    headers: {
      ...(config.headers || {})
    }
  }

  const token = readToken()
  if (token) {
    normalizedConfig.headers.Authorization = `Bearer ${token}`
  }

  if (normalizedConfig.params) {
    normalizedConfig.params = transformKeysToSnake(normalizedConfig.params)
  }

  if (normalizedConfig.data) {
    normalizedConfig.data = transformKeysToSnake(normalizedConfig.data)
  }

  return normalizedConfig
}

export function unwrapBusinessResponse(response, toastApi = toast) {
  const res = response.data

  if (res.code !== 0) {
    const message = res.message || 'Request failed'
    const error = new Error(message)
    error.code = res.code
    error.response = res

    if (!response.config?.hideErrorToast) {
      toastApi.error(message)
    }

    throw error
  }

  return transformKeysToCamel(res.data)
}

// 请求拦截器 - 注入 JWT Token，并统一序列化参数
request.interceptors.request.use(
  (config) => normalizeRequestConfig(config),
  (error) => Promise.reject(error)
)

// 响应拦截器 - 统一处理业务错误和 HTTP 错误
request.interceptors.response.use(
  (response) => unwrapBusinessResponse(response),
  (error) => {
    if (error.response) {
      const { status, data } = error.response
      const serverMessage = data?.message

      if (status === 401) {
        const isLoginRequest = error.config?.url?.includes('/auth/login')
        const isPasswordError = data?.code === 11002
        const isUserNotFound = data?.code === 11001

        if (isLoginRequest || isPasswordError || isUserNotFound) {
          error.message = serverMessage || '用户名或密码错误'
          toast.error(error.message)
        } else {
          clearAuthSession()

          toast.error(serverMessage || '登录已过期，请重新登录')

          unauthorizedHandler?.()

          error.message = serverMessage || 'Token 已过期，请重新登录'
        }
      } else if (status === 403) {
        error.message = serverMessage || '没有权限访问该资源'
        toast.error(error.message)
      } else if (status === 404) {
        error.message = serverMessage || '请求的资源不存在'
        toast.error(error.message)
      } else if (status === 500) {
        error.message = serverMessage || '服务器内部错误'
        toast.error(error.message)
      } else {
        error.message = serverMessage || `请求失败 (${status})`
        toast.error(error.message)
      }
    } else if (error.request) {
      error.message = '网络连接失败，请检查网络'
      toast.error(error.message)
    } else {
      error.message = error.message || '请求配置错误'
      toast.error(error.message)
    }

    return Promise.reject(error)
  }
)

export default request
