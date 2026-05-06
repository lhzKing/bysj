import { describe, expect, it } from 'vitest'
import {
  toCamelCaseKey,
  toSnakeCaseKey,
  transformArray,
  transformKeysToCamel,
  transformKeysToSnake,
  transformSnapshot,
  transformTraceLog,
  transformUser
} from '@/shared/utils/transform'

describe('transform contract helpers', () => {
  it('converts snake_case keys to camelCase recursively', () => {
    const payload = {
      trace_code: 'TRACE-001',
      nested_data: {
        current_status: 'IN_TRANSIT'
      },
      history: [
        {
          event_time: '2026-04-11T10:00:00',
          from_node: '工厂A'
        }
      ]
    }

    expect(transformKeysToCamel(payload)).toEqual({
      traceCode: 'TRACE-001',
      nestedData: {
        currentStatus: 'IN_TRANSIT'
      },
      history: [
        {
          eventTime: '2026-04-11T10:00:00',
          fromNode: '工厂A'
        }
      ]
    })
  })

  it('converts camelCase keys to snake_case recursively and handles acronym boundaries', () => {
    const payload = {
      roleId: 7,
      traceCode: 'TRACE-001',
      nestedData: {
        manufacturerNode: '工厂A',
        apiURLValue: 'https://example.test'
      }
    }

    expect(toSnakeCaseKey('manufacturerNode')).toBe('manufacturer_node')
    expect(toSnakeCaseKey('apiURLValue')).toBe('api_url_value')
    expect(toSnakeCaseKey('URLValue')).toBe('url_value')

    expect(transformKeysToSnake(payload)).toEqual({
      role_id: 7,
      trace_code: 'TRACE-001',
      nested_data: {
        manufacturer_node: '工厂A',
        api_url_value: 'https://example.test'
      }
    })
  })

  it('keeps special values intact while transforming plain objects', () => {
    const formData = new FormData()
    formData.append('traceCode', 'TRACE-001')
    const now = new Date('2026-04-11T10:00:00')
    const blob = new Blob(['trace payload'], { type: 'text/plain' })

    const result = transformKeysToSnake({
      formData,
      now,
      blobValue: blob,
      list: [{ eventTime: '2026-04-11 10:00:00' }]
    })

    expect(result.form_data).toBe(formData)
    expect(result.now).toBe(now)
    expect(result.blob_value).toBe(blob)
    expect(result.list[0]).toEqual({ event_time: '2026-04-11 10:00:00' })
  })

  it('passes through special root values unchanged', () => {
    const rootBlob = new Blob(['root blob'], { type: 'text/plain' })
    const rootDate = new Date('2026-04-11T10:00:00')
    const rootFile = new File(['root file'], 'trace.txt', { type: 'text/plain' })

    expect(transformKeysToSnake(rootBlob)).toBe(rootBlob)
    expect(transformKeysToCamel(rootDate)).toBe(rootDate)
    expect(transformKeysToSnake(rootFile)).toBe(rootFile)
  })

  it('keeps helper key conversions stable', () => {
    expect(toCamelCaseKey('trace_code')).toBe('traceCode')
    expect(toSnakeCaseKey('manufacturerNode')).toBe('manufacturer_node')
  })

  it('keeps snapshot helper returning camelCase data', () => {
    expect(transformSnapshot({ trace_code: 'TRACE-001', current_status: 'INIT' })).toEqual({
      traceCode: 'TRACE-001',
      currentStatus: 'INIT'
    })
  })

  it('keeps trace log helper returning camelCase data', () => {
    expect(transformTraceLog({ action_type: 'INBOUND', from_node: '工厂A' })).toEqual({
      actionType: 'INBOUND',
      fromNode: '工厂A'
    })
  })

  it('keeps user helper returning camelCase data', () => {
    expect(transformUser({ role_id: 2, role_name: '管理员', token_version: 3 })).toEqual({
      roleId: 2,
      roleName: '管理员',
      tokenVersion: 3
    })
  })

  it('keeps transformArray compatible with existing helpers', () => {
    expect(transformArray([{ trace_code: 'TRACE-001' }], transformSnapshot)).toEqual([
      { traceCode: 'TRACE-001' }
    ])
  })
})
