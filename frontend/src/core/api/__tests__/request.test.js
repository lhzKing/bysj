import { beforeEach, describe, expect, it, vi } from 'vitest'

const { unauthorizedMock, toastErrorMock, requestUseMock, responseUseMock, requestInstance } = vi.hoisted(() => ({
  unauthorizedMock: vi.fn(),
  toastErrorMock: vi.fn(),
  requestUseMock: vi.fn(),
  responseUseMock: vi.fn(),
  requestInstance: {
    interceptors: {
      request: { use: vi.fn() },
      response: { use: vi.fn() }
    }
  }
}))

requestInstance.interceptors.request.use = requestUseMock
requestInstance.interceptors.response.use = responseUseMock

vi.mock('axios', () => ({
  default: {
    create: () => requestInstance
  }
}))

vi.mock('@/shared/composables/useToast', () => ({
  useToast: () => ({
    error: toastErrorMock
  })
}))

import request, { normalizeRequestConfig, setUnauthorizedHandler, unwrapBusinessResponse } from '@/core/api/request'

describe('request contract normalization', () => {
  beforeEach(() => {
    localStorage.clear()
    unauthorizedMock.mockReset()
    toastErrorMock.mockReset()
    setUnauthorizedHandler(null)
  })

  it('wires request and response interceptors to the normalization helpers', async () => {
    requestUseMock.mockClear()
    responseUseMock.mockClear()
    vi.resetModules()

    const reloadedModule = await import('@/core/api/request')

    expect(reloadedModule.default).toBe(requestInstance)
    expect(requestUseMock).toHaveBeenCalledTimes(1)
    expect(responseUseMock).toHaveBeenCalledTimes(1)

    const [requestSuccessHandler] = requestUseMock.mock.calls[0]
    const [responseSuccessHandler] = responseUseMock.mock.calls[0]

    const config = {
      params: { roleId: 2 },
      data: { partCode: 'P-001' },
      headers: {}
    }
    const response = {
      data: {
        code: 0,
        data: {
          trace_code: 'TRACE-001'
        }
      },
      config: {}
    }

    expect(requestSuccessHandler(config)).toEqual(reloadedModule.normalizeRequestConfig(config))
    expect(responseSuccessHandler(response)).toEqual(reloadedModule.unwrapBusinessResponse(response))
  })

  it('serializes params and body to snake_case while keeping token injection', () => {
    localStorage.setItem('token', 'token-123')

    const normalized = normalizeRequestConfig({
      params: { roleId: 2, pageSize: 20 },
      data: { partCode: 'P-001', manufacturerNode: 'Factory A' },
      headers: {}
    })

    expect(normalized.params).toEqual({ role_id: 2, page_size: 20 })
    expect(normalized.data).toEqual({ part_code: 'P-001', manufacturer_node: 'Factory A' })
    expect(normalized.headers.Authorization).toBe('Bearer token-123')
  })

  it('unwraps successful business responses to camelCase data', () => {
    const result = unwrapBusinessResponse({
      data: {
        code: 0,
        data: {
          trace_code: 'TRACE-001',
          total_logs: 4
        }
      },
      config: {}
    })

    expect(result).toEqual({
      traceCode: 'TRACE-001',
      totalLogs: 4
    })
  })

  it('throws readable business errors and keeps toast behavior', () => {
    expect(() =>
      unwrapBusinessResponse(
        {
          data: {
            code: 40001,
            message: 'Service error'
          },
          config: {}
        },
        {
          error: toastErrorMock
        }
      )
    ).toThrow('Service error')

    expect(toastErrorMock).toHaveBeenCalledWith('Service error')
  })

  it('skips toast when hideErrorToast is true', () => {
    expect(() =>
      unwrapBusinessResponse(
        {
          data: {
            code: 40001,
            message: 'Service error'
          },
          config: {
            hideErrorToast: true
          }
        },
        {
          error: toastErrorMock
        }
      )
    ).toThrow('Service error')

    expect(toastErrorMock).not.toHaveBeenCalled()
  })

  it('keeps original business error payload on error.response', () => {
    const response = {
      data: {
        code: 40001,
        message: 'Service error',
        data: {
          trace_code: 'TRACE-001'
        }
      },
      config: {}
    }

    expect(() =>
      unwrapBusinessResponse(response, {
        error: toastErrorMock
      })
    ).toThrowError(
      expect.objectContaining({
        response: response.data
      })
    )
  })

  it('delegates non-login 401 redirects to the injected unauthorized handler', async () => {
    responseUseMock.mockClear()
    vi.resetModules()
    const reloadedModule = await import('@/core/api/request')
    localStorage.setItem('token', 'expired-token')
    localStorage.setItem('user', JSON.stringify({ username: 'alice' }))
    reloadedModule.setUnauthorizedHandler(unauthorizedMock)

    const [, errorHandler] = responseUseMock.mock.calls[0]
    const error = {
      config: { url: '/users' },
      response: {
        status: 401,
        data: {
          code: 11004,
          message: '登录已过期'
        }
      }
    }

    await expect(errorHandler(error)).rejects.toMatchObject({
      message: '登录已过期'
    })

    expect(localStorage.getItem('token')).toBeNull()
    expect(localStorage.getItem('user')).toBeNull()
    expect(toastErrorMock).toHaveBeenCalledWith('登录已过期')
    expect(unauthorizedMock).toHaveBeenCalledTimes(1)
  })
})
