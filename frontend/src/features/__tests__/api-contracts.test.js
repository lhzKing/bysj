import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'
import { beforeEach, describe, expect, it, vi } from 'vitest'

const requestMock = vi.hoisted(() => ({
  get: vi.fn(),
  post: vi.fn(),
  put: vi.fn(),
  patch: vi.fn(),
  delete: vi.fn()
}))

vi.mock('@/core/api/request', () => ({
  default: requestMock
}))

import request from '@/core/api/request'
import { login, register, getUserInfo, logout } from '@/core/api/auth'
import { getUsers, createUser, resetUserPassword } from '@/features/user/api/users'
import { getRoles, getRole, createRole, assignPermissions } from '@/features/user/api/roles'
import { createPart } from '@/features/part/api/parts'
import {
  activateTraceCode,
  completeTraceFlowTask,
  createTrace,
  createTraceFlowTask,
  getTraceAvailableActions,
  getTraceDetail,
  getTraceBatch,
  getTraceBatchCodes,
  getTraceFlowTask,
  getTraceFlowTasks,
  getTraceNodes,
  printTraceCode,
  reprintTraceCode,
  scanTraceFlowTask,
  voidTraceCode
} from '@/features/trace/api/trace'
import { getTopology } from '@/features/dashboard/api/dashboard'

const apiSourceFiles = [
  'src/core/api/auth.js',
  'src/features/user/api/users.js',
  'src/features/user/api/roles.js',
  'src/features/part/api/parts.js',
  'src/features/trace/api/trace.js',
  'src/features/dashboard/api/dashboard.js'
]

describe('feature api contracts', () => {
  beforeEach(() => {
    request.get.mockReset()
    request.post.mockReset()
    request.put.mockReset()
    request.patch.mockReset()
    request.delete.mockReset()
  })

  it('documents camelCase frontend contracts and snake_case wire serialization at api boundaries', () => {
    const boundaryNote =
      'Frontend programming interface uses camelCase; request.js serializes outgoing HTTP to snake_case.'

    apiSourceFiles.forEach((relativePath) => {
      const source = readFileSync(resolve(process.cwd(), relativePath), 'utf8')
      expect(source).toContain(boundaryNote)
    })
  })

  it('keeps auth and management inputs in camelCase before request serialization', async () => {
    request.get.mockResolvedValue({})
    request.post.mockResolvedValue({})
    request.put.mockResolvedValue({})

    await login('alice', 'abc123', true)
    await register('new_user', 'abc123')
    await getUserInfo()
    await logout()
    await getUsers({ username: 'alice', roleId: 2, page: 1, size: 10 })
    await createUser({ username: 'alice', password: 'abc123', roleId: 2, status: 1 })
    await resetUserPassword(8, 'newPass123')
    await createRole({ roleCode: 'MANAGER', roleName: 'Manager' })
    await assignPermissions(3, [1, 2, 3])

    expect(request.post).toHaveBeenNthCalledWith(1, '/auth/login', {
      username: 'alice',
      password: 'abc123',
      rememberMe: true
    })
    expect(request.post).toHaveBeenNthCalledWith(2, '/auth/register', {
      username: 'new_user',
      password: 'abc123'
    })
    expect(request.get).toHaveBeenNthCalledWith(1, '/auth/me')
    expect(request.post).toHaveBeenNthCalledWith(3, '/auth/logout')
    expect(request.get).toHaveBeenNthCalledWith(2, '/users', {
      params: { username: 'alice', roleId: 2, page: 1, size: 10 }
    })
    expect(request.post).toHaveBeenNthCalledWith(4, '/users', {
      username: 'alice',
      password: 'abc123',
      roleId: 2,
      status: 1
    })
    expect(request.post).toHaveBeenNthCalledWith(5, '/users/8/reset-password', {
      newPassword: 'newPass123'
    })
    expect(request.post).toHaveBeenNthCalledWith(6, '/roles', {
      roleCode: 'MANAGER',
      roleName: 'Manager'
    })
    expect(request.put).toHaveBeenNthCalledWith(1, '/roles/3/permissions', {
      permissionIds: [1, 2, 3]
    })
  })

  it('keeps role list/detail api boundaries explicit', async () => {
    request.get.mockResolvedValue({})
    request.post.mockResolvedValue({})
    request.put.mockResolvedValue({})

    await getRoles()
    await getRole(3)
    await createRole({ roleCode: 'MANAGER', roleName: 'Manager' })
    await assignPermissions(3, [1, 2, 3])

    expect(request.get).toHaveBeenNthCalledWith(1, '/roles')
    expect(request.get).toHaveBeenNthCalledWith(2, '/roles/3')
    expect(request.post).toHaveBeenCalledWith('/roles', {
      roleCode: 'MANAGER',
      roleName: 'Manager'
    })
    expect(request.put).toHaveBeenCalledWith('/roles/3/permissions', {
      permissionIds: [1, 2, 3]
    })
  })

  it('keeps part, trace and dashboard inputs in camelCase before request serialization', async () => {
    request.post.mockResolvedValue({})
    request.get.mockResolvedValue({})

    await createPart({ partCode: 'P-001', partName: 'Bearing', partType: 'Mechanical' })
    await createTrace({ partCode: 'P-001', manufacturerNode: 'Factory-A', quantity: 3 })
    await getTraceDetail('TRACE-001')
    await getTraceAvailableActions('TRACE-001')
    await getTopology('TRACE-001', '30d')

    expect(request.post).toHaveBeenNthCalledWith(1, '/parts', {
      partCode: 'P-001',
      partName: 'Bearing',
      partType: 'Mechanical'
    })
    expect(request.post).toHaveBeenNthCalledWith(2, '/traces', {
      partCode: 'P-001',
      manufacturerNode: 'Factory-A',
      quantity: 3
    })
    expect(request.get).toHaveBeenNthCalledWith(1, '/traces/TRACE-001', {
      params: { view: 'effective' }
    })
    expect(request.get).toHaveBeenNthCalledWith(2, '/traces/TRACE-001/available-actions')
    expect(request.get).toHaveBeenCalledWith('/dashboard/topology', {
      params: { traceCode: 'TRACE-001', range: '30d' }
    })
  })

  it('keeps production assignment workbench trace api endpoints explicit', async () => {
    request.post.mockResolvedValue({})
    request.get.mockResolvedValue({})

    await printTraceCode('TRACE-001', { remark: 'print' })
    await reprintTraceCode('TRACE-001', { remark: 'reprint' })
    await voidTraceCode('TRACE-001', { remark: 'void' })
    await activateTraceCode('TRACE-001', { activationNode: 'Factory-A', deviceId: 'PDA-01' })
    await getTraceBatch(9)
    await getTraceBatchCodes(9)
    await getTraceNodes()

    expect(request.post).toHaveBeenNthCalledWith(1, '/traces/TRACE-001/print', { remark: 'print' })
    expect(request.post).toHaveBeenNthCalledWith(2, '/traces/TRACE-001/reprint', { remark: 'reprint' })
    expect(request.post).toHaveBeenNthCalledWith(3, '/traces/TRACE-001/void', { remark: 'void' })
    expect(request.post).toHaveBeenNthCalledWith(4, '/trace-codes/TRACE-001/activate', {
      activationNode: 'Factory-A',
      deviceId: 'PDA-01'
    })
    expect(request.get).toHaveBeenNthCalledWith(1, '/trace-batches/9')
    expect(request.get).toHaveBeenNthCalledWith(2, '/trace-batches/9/codes')
    expect(request.get).toHaveBeenNthCalledWith(3, '/trace-nodes/selectable')
  })

  it('keeps warehouse logistics flow-task api endpoints explicit', async () => {
    request.post.mockResolvedValue({})
    request.get.mockResolvedValue({})

    await getTraceFlowTasks({ status: 'CREATED', taskType: 'OUTBOUND' })
    await getTraceFlowTask(18)
    await createTraceFlowTask({ taskType: 'OUTBOUND', sourceNodeId: 1, targetNodeId: 2, expectedQuantity: 20 })
    await scanTraceFlowTask(18, { traceCode: 'TRACE-001', eventTime: '2026-05-07T10:00:00' })
    await completeTraceFlowTask(18, { discrepancyReason: '少件待补扫' })

    expect(request.get).toHaveBeenNthCalledWith(1, '/trace-flow-tasks', {
      params: { status: 'CREATED', taskType: 'OUTBOUND' }
    })
    expect(request.get).toHaveBeenNthCalledWith(2, '/trace-flow-tasks/18')
    expect(request.post).toHaveBeenNthCalledWith(1, '/trace-flow-tasks', {
      taskType: 'OUTBOUND',
      sourceNodeId: 1,
      targetNodeId: 2,
      expectedQuantity: 20
    })
    expect(request.post).toHaveBeenNthCalledWith(2, '/trace-flow-tasks/18/scan', {
      traceCode: 'TRACE-001',
      eventTime: '2026-05-07T10:00:00'
    })
    expect(request.post).toHaveBeenNthCalledWith(3, '/trace-flow-tasks/18/complete', {
      discrepancyReason: '少件待补扫'
    })
  })

})
