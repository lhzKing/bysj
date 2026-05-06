import { beforeEach, describe, expect, it, vi } from 'vitest'
import { reactive } from 'vue'
import { flushPromises } from '@vue/test-utils'
import { renderWithPrime } from '@/test/renderWithPrime'
import RoleList from '@/features/user/views/RoleList.vue'

const getRolesMock = vi.fn()
const getRoleMock = vi.fn()
const getPermissionsMock = vi.fn()
const assignPermissionsMock = vi.fn()
const deleteRoleMock = vi.fn()
const createRoleMock = vi.fn()
const updateRoleMock = vi.fn()
const confirmMock = vi.fn()
const toastSuccessMock = vi.fn()
const toastErrorMock = vi.fn()

const currentUser = reactive({
  roleCode: 'SUPER_ADMIN',
  role: 'SUPER_ADMIN',
  permissions: ['role:manage']
})

const currentUserStore = reactive({
  user: currentUser,
  permissions: currentUser.permissions
})

vi.mock('@/features/user/api', () => ({
  getRoles: (...args) => getRolesMock(...args),
  getRole: (...args) => getRoleMock(...args),
  getPermissions: (...args) => getPermissionsMock(...args),
  assignPermissions: (...args) => assignPermissionsMock(...args),
  deleteRole: (...args) => deleteRoleMock(...args),
  createRole: (...args) => createRoleMock(...args),
  updateRole: (...args) => updateRoleMock(...args)
}))

vi.mock('@/shared/composables/useConfirm', () => ({
  useConfirm: () => ({ confirm: confirmMock })
}))

vi.mock('@/shared/composables/useToast', () => ({
  useToast: () => ({
    success: toastSuccessMock,
    error: toastErrorMock
  })
}))

vi.mock('@/core/stores/user', () => ({
  useUserStore: () => currentUserStore
}))

const unwrap = (value) => value?.value ?? value

function setCurrentUser(options = {}) {
  const { roleCode = 'SUPER_ADMIN', permissions = ['role:manage'] } = options
  currentUser.roleCode = roleCode
  currentUser.role = roleCode
  currentUser.permissions = [...permissions]
  currentUserStore.permissions = currentUser.permissions
}

function mountRoleList() {
  return renderWithPrime(RoleList)
}

describe('RoleList contract', () => {
  beforeEach(() => {
    getRolesMock.mockReset()
    getRoleMock.mockReset()
    getPermissionsMock.mockReset()
    assignPermissionsMock.mockReset()
    deleteRoleMock.mockReset()
    createRoleMock.mockReset()
    updateRoleMock.mockReset()
    confirmMock.mockReset()
    toastSuccessMock.mockReset()
    toastErrorMock.mockReset()
    setCurrentUser()
  })

  it('displays lightweight permission counts from role.permissionCount', async () => {
    getRolesMock.mockResolvedValue([{ id: 1, roleName: 'Admin', roleCode: 'ADMIN', permissionCount: 7 }])
    getPermissionsMock.mockResolvedValue([])

    const wrapper = mountRoleList()
    await flushPromises()

    expect(getRolesMock).toHaveBeenCalledTimes(1)
    expect(wrapper.text()).toContain('7')
  })

  it('fetches full role detail before initializing the permission dialog', async () => {
    getRolesMock.mockResolvedValue([{ id: 3, roleName: 'Manager', roleCode: 'MANAGER', permissionCount: 2 }])
    getPermissionsMock.mockResolvedValue([
      { id: 11, permCode: 'trace:view', permName: 'Trace View' },
      { id: 12, permCode: 'trace:scan', permName: 'Trace Scan' }
    ])
    getRoleMock.mockResolvedValue({
      id: 3,
      permissions: [
        { id: 11, permCode: 'trace:view' },
        { id: 12, permCode: 'trace:scan' }
      ]
    })

    const wrapper = mountRoleList()
    await flushPromises()

    const setupState = wrapper.vm.$.setupState
    await setupState.handleAssignPermissions({
      id: 3,
      roleName: 'Manager',
      roleCode: 'MANAGER',
      permissionCount: 2
    })
    await flushPromises()

    expect(getRoleMock).toHaveBeenCalledWith(3)
    expect(unwrap(setupState.selectedPermissions)).toEqual([11, 12])
    expect(unwrap(setupState.showPermissionDialog)).toBe(true)
  })

  it('hides SUPER_ADMIN and same-level mutation actions for ADMIN users', async () => {
    setCurrentUser({ roleCode: 'ADMIN', permissions: ['role:manage'] })
    getRolesMock.mockResolvedValue([
      { id: 1, roleName: 'Super Admin', roleCode: 'SUPER_ADMIN', permissionCount: 10 },
      { id: 2, roleName: 'Admin', roleCode: 'ADMIN', permissionCount: 8 },
      { id: 3, roleName: 'Manager', roleCode: 'MANAGER', permissionCount: 2 }
    ])
    getPermissionsMock.mockResolvedValue([])

    const wrapper = mountRoleList()
    await flushPromises()

    const roleRows = unwrap(wrapper.vm.$.setupState.roleRows)
    expect(roleRows[0].__guard).toEqual({
      canEdit: false,
      canAssignPermissions: false,
      canDelete: false,
      isProtected: true
    })
    expect(roleRows[1].__guard).toEqual({
      canEdit: false,
      canAssignPermissions: false,
      canDelete: false,
      isProtected: true
    })
    expect(roleRows[2].__guard.canAssignPermissions).toBe(true)
    expect(roleRows[2].__guard.canEdit).toBe(true)
    expect(wrapper.text()).toContain('Protected')
  })

  it('filters protected permissions out of ADMIN-visible permission options', async () => {
    setCurrentUser({ roleCode: 'ADMIN', permissions: ['role:manage'] })
    getRolesMock.mockResolvedValue([])
    getPermissionsMock.mockResolvedValue([
      { id: 3, permCode: 'trace:view', permName: 'Trace View' },
      { id: 5, permCode: 'user:view', permName: 'User View' },
      { id: 8, permCode: 'role:manage', permName: 'Role Manage' }
    ])

    const wrapper = mountRoleList()
    await flushPromises()

    expect(unwrap(wrapper.vm.$.setupState.assignablePermissions)).toEqual([
      { id: 3, permCode: 'trace:view', permName: 'Trace View' }
    ])
  })

  it('refuses to open the permission dialog for ADMIN when the target role already contains protected permissions', async () => {
    setCurrentUser({ roleCode: 'ADMIN', permissions: ['role:manage'] })
    getRolesMock.mockResolvedValue([{ id: 3, roleName: 'Manager', roleCode: 'MANAGER', permissionCount: 2 }])
    getPermissionsMock.mockResolvedValue([
      { id: 3, permCode: 'trace:view', permName: 'Trace View' },
      { id: 8, permCode: 'role:manage', permName: 'Role Manage' }
    ])
    getRoleMock.mockResolvedValue({
      id: 3,
      permissions: [
        { id: 3, permCode: 'trace:view' },
        { id: 8, permCode: 'role:manage' }
      ]
    })

    const wrapper = mountRoleList()
    await flushPromises()

    await wrapper.vm.$.setupState.handleAssignPermissions({
      id: 3,
      roleName: 'Manager',
      roleCode: 'MANAGER',
      permissionCount: 2
    })
    await flushPromises()

    expect(toastErrorMock).toHaveBeenCalled()
    expect(String(toastErrorMock.mock.calls.at(-1)?.[0] || '')).toContain('SUPER_ADMIN')
    expect(unwrap(wrapper.vm.$.setupState.showPermissionDialog)).toBe(false)
  })
})
