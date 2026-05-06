import { afterEach, beforeEach, describe, expect, it } from 'vitest'

// Import the REAL request instance — no vi.mock('@/core/api/request') here.
// We override the axios adapter per-test so the network is mocked but every other
// layer (request interceptor → transformKeysToSnake; response interceptor →
// transformKeysToCamel) actually runs. That is the gap the previous
// api-contracts.test.js intentionally left covered: it stubbed the entire
// request module and never executed the case-transform pipeline.
import request from '@/core/api/request'
import { login } from '@/core/api/auth'
import { createUser, getUsers } from '@/features/user/api/users'
import { createPart } from '@/features/part/api/parts'

// axios runs its default transformRequest (object → JSON string) AFTER the
// request interceptor and BEFORE the adapter, so a captured POST body shows up
// as a JSON string. Parse it back so assertions can compare structured shapes.
function captureWire(captured, mockResponseData) {
  return (config) => {
    let parsedData = config.data
    if (typeof parsedData === 'string' && parsedData.length > 0) {
      try {
        parsedData = JSON.parse(parsedData)
      } catch {
        // leave non-JSON bodies (FormData, Blob, …) untouched
      }
    }
    captured.push({
      url: config.url,
      method: config.method,
      params: config.params,
      data: parsedData,
      headers: config.headers
    })
    return Promise.resolve({
      config,
      status: 200,
      statusText: 'OK',
      headers: {},
      data: { code: 0, data: mockResponseData }
    })
  }
}

describe('api wire serialization (real request.js interceptors)', () => {
  let captured
  let mockResponseData

  beforeEach(() => {
    captured = []
    mockResponseData = null
    localStorage.clear()
    request.defaults.adapter = (config) => captureWire(captured, mockResponseData)(config)
  })

  afterEach(() => {
    delete request.defaults.adapter
  })

  it('POST /auth/login serializes camelCase rememberMe to snake_case remember_me on the wire and camelCases response', async () => {
    mockResponseData = {
      access_token: 'abc-123',
      user_info: { user_id: 1, role_code: 'USER', remember_me: true }
    }

    const result = await login('alice', 'secret', true)

    expect(captured).toHaveLength(1)
    const wire = captured[0]
    expect(wire.url).toBe('/auth/login')
    expect(wire.method).toBe('post')
    expect(wire.data).toEqual({
      username: 'alice',
      password: 'secret',
      remember_me: true
    })
    // Negative assertion: the camelCase key must NOT survive past the request interceptor.
    expect(wire.data).not.toHaveProperty('rememberMe')

    // Response is auto-camelCased by the response interceptor.
    expect(result).toEqual({
      accessToken: 'abc-123',
      userInfo: { userId: 1, roleCode: 'USER', rememberMe: true }
    })
  })

  it('POST /users serializes roleId to role_id on the wire and camelCases the response', async () => {
    mockResponseData = { user_id: 8, role_code: 'ADMIN', token_version: 0 }

    const result = await createUser({ username: 'bob', password: 'p', roleId: 2, status: 1 })

    expect(captured).toHaveLength(1)
    expect(captured[0].url).toBe('/users')
    expect(captured[0].method).toBe('post')
    expect(captured[0].data).toEqual({
      username: 'bob',
      password: 'p',
      role_id: 2,
      status: 1
    })
    expect(captured[0].data).not.toHaveProperty('roleId')

    expect(result).toEqual({ userId: 8, roleCode: 'ADMIN', tokenVersion: 0 })
  })

  it('POST /parts serializes partCode/partName/partType to snake_case on the wire and camelCases the response', async () => {
    mockResponseData = { part_id: 9, part_code: 'P-001', part_name: 'Bearing', created_by: 'alice' }

    const result = await createPart({
      partCode: 'P-001',
      partName: 'Bearing',
      partType: 'Mechanical'
    })

    expect(captured).toHaveLength(1)
    expect(captured[0].url).toBe('/parts')
    expect(captured[0].method).toBe('post')
    expect(captured[0].data).toEqual({
      part_code: 'P-001',
      part_name: 'Bearing',
      part_type: 'Mechanical'
    })
    expect(captured[0].data).not.toHaveProperty('partCode')
    expect(captured[0].data).not.toHaveProperty('partName')
    expect(captured[0].data).not.toHaveProperty('partType')

    expect(result).toEqual({
      partId: 9,
      partCode: 'P-001',
      partName: 'Bearing',
      createdBy: 'alice'
    })
  })

  it('GET /users serializes camelCase query params to snake_case on the wire and camelCases the response list', async () => {
    mockResponseData = {
      list: [
        { user_id: 1, role_id: 2, role_code: 'USER' },
        { user_id: 2, role_id: 3, role_code: 'ADMIN' }
      ],
      total: 2
    }

    const result = await getUsers({ username: 'alice', roleId: 2, page: 1, size: 10 })

    expect(captured).toHaveLength(1)
    expect(captured[0].url).toBe('/users')
    expect(captured[0].method).toBe('get')
    expect(captured[0].params).toEqual({
      username: 'alice',
      role_id: 2,
      page: 1,
      size: 10
    })
    expect(captured[0].params).not.toHaveProperty('roleId')

    // Nested arrays in the response also pass through transformKeysToCamel.
    expect(result).toEqual({
      list: [
        { userId: 1, roleId: 2, roleCode: 'USER' },
        { userId: 2, roleId: 3, roleCode: 'ADMIN' }
      ],
      total: 2
    })
  })
})
