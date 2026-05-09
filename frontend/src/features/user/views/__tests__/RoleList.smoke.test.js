import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import RoleList from '@/features/user/views/RoleList.vue'

const {
  toastSuccess,
  toastError,
  confirmMock,
  getRolesMock,
  getRoleMock,
  getPermissionsMock,
  createRoleMock,
  updateRoleMock,
  deleteRoleMock,
  assignPermissionsMock,
  userStoreState
} = vi.hoisted(() => ({
  toastSuccess: vi.fn(),
  toastError: vi.fn(),
  confirmMock: vi.fn(),
  getRolesMock: vi.fn(),
  getRoleMock: vi.fn(),
  getPermissionsMock: vi.fn(),
  createRoleMock: vi.fn(),
  updateRoleMock: vi.fn(),
  deleteRoleMock: vi.fn(),
  assignPermissionsMock: vi.fn(),
  userStoreState: {
    user: { roleCode: 'SUPER_ADMIN', role: 'SUPER_ADMIN', permissions: ['role:manage'] },
    permissions: ['role:manage']
  }
}))

vi.mock('@/shared/composables/useToast', () => ({
  useToast: () => ({ success: toastSuccess, error: toastError, warning: vi.fn(), info: vi.fn() })
}))

vi.mock('@/shared/composables/useConfirm', () => ({
  useConfirm: () => ({ confirm: confirmMock })
}))

vi.mock('@/core/stores/user', () => ({
  useUserStore: () => userStoreState
}))

vi.mock('@/features/user/api', () => ({
  getRoles: getRolesMock,
  getRole: getRoleMock,
  getPermissions: getPermissionsMock,
  createRole: createRoleMock,
  updateRole: updateRoleMock,
  deleteRole: deleteRoleMock,
  assignPermissions: assignPermissionsMock
}))

function setCurrentUser({ roleCode = 'SUPER_ADMIN', permissions = ['role:manage'] } = {}) {
  userStoreState.user = { roleCode, role: roleCode, permissions: [...permissions] }
  userStoreState.permissions = [...permissions]
}

const ROLES_FIXTURE = [
  { id: 1, roleCode: 'SUPER_ADMIN', roleName: '超级管理员', remark: '内置最高权限', permissionCount: 12 },
  { id: 2, roleCode: 'ADMIN', roleName: '管理员', remark: '业务管理员', permissionCount: 9 },
  { id: 3, roleCode: 'INSPECTOR', roleName: '质检员', remark: '负责质检', permissionCount: 4 },
  { id: 4, roleCode: 'WAREHOUSE', roleName: '仓管员', remark: '仓库岗位', permissionCount: 5 }
]

const PERMISSIONS_FIXTURE = [
  { id: 11, permCode: 'trace:view', permName: '溯源查看' },
  { id: 12, permCode: 'trace:scan', permName: '扫码登记' },
  { id: 13, permCode: 'trace:create', permName: '生产赋码' },
  { id: 21, permCode: 'part:view', permName: '配件查看' },
  { id: 22, permCode: 'part:manage', permName: '配件管理' },
  { id: 31, permCode: 'user:view', permName: '用户查看' },
  { id: 32, permCode: 'user:manage', permName: '用户管理' },
  { id: 41, permCode: 'role:view', permName: '角色查看' },
  { id: 42, permCode: 'role:manage', permName: '角色管理' }
]

const unwrap = (value) => value?.value ?? value

describe('RoleList smoke', () => {
  beforeEach(() => {
    setCurrentUser()
    getRolesMock.mockResolvedValue([...ROLES_FIXTURE])
    getPermissionsMock.mockResolvedValue([...PERMISSIONS_FIXTURE])
    getRoleMock.mockResolvedValue({
      id: 3,
      roleCode: 'INSPECTOR',
      roleName: '质检员',
      permissions: [{ id: 11, permCode: 'trace:view' }]
    })
    createRoleMock.mockResolvedValue(undefined)
    updateRoleMock.mockResolvedValue(undefined)
    deleteRoleMock.mockResolvedValue(undefined)
    assignPermissionsMock.mockResolvedValue(undefined)
    confirmMock.mockResolvedValue(true)
  })

  afterEach(() => {
    vi.clearAllMocks()
  })

  it('renders PageHeader title with refresh / create actions and dense rows including roleName / permissionCount', async () => {
    const wrapper = mount(RoleList)
    await flushPromises()

    expect(wrapper.text()).toContain('角色管理')
    expect(wrapper.find('[data-testid="role-list-refresh"]').exists()).toBe(true)
    expect(wrapper.find('[data-testid="role-list-create"]').exists()).toBe(true)

    const rows = wrapper.findAll('[data-testid="role-table-row"]')
    expect(rows).toHaveLength(4)
    expect(rows[0].text()).toContain('超级管理员')
    expect(rows[0].text()).toContain('SUPER_ADMIN')
    expect(rows[0].text()).toContain('12')
    expect(rows[2].text()).toContain('质检员')
    expect(rows[2].text()).toContain('INSPECTOR')
  })

  it('hides edit / delete / permission actions for SUPER_ADMIN row when current user is ADMIN and shows Protected pill', async () => {
    setCurrentUser({ roleCode: 'ADMIN', permissions: ['role:manage'] })
    const wrapper = mount(RoleList)
    await flushPromises()

    const setupState = wrapper.vm.$.setupState
    const roleRows = unwrap(setupState.roleRows)

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
    expect(roleRows[2].__guard.canDelete).toBe(true)
    expect(wrapper.text()).toContain('Protected')
  })

  it('filters protected permissions out of ADMIN-visible permission options via assignablePermissions', async () => {
    setCurrentUser({ roleCode: 'ADMIN', permissions: ['role:manage'] })
    const wrapper = mount(RoleList)
    await flushPromises()

    const visible = unwrap(wrapper.vm.$.setupState.assignablePermissions)
    const codes = visible.map((p) => p.permCode)
    expect(codes).toContain('trace:view')
    expect(codes).toContain('part:view')
    expect(codes).not.toContain('user:view')
    expect(codes).not.toContain('user:manage')
    expect(codes).not.toContain('role:view')
    expect(codes).not.toContain('role:manage')
  })

  it('opens permission dialog with selectedPermissions populated from getRole detail', async () => {
    getRoleMock.mockResolvedValueOnce({
      id: 3,
      roleCode: 'INSPECTOR',
      roleName: '质检员',
      permissions: [
        { id: 11, permCode: 'trace:view' },
        { id: 12, permCode: 'trace:scan' }
      ]
    })

    const wrapper = mount(RoleList)
    await flushPromises()

    const setupState = wrapper.vm.$.setupState
    await setupState.handleAssignPermissions({
      id: 3,
      roleCode: 'INSPECTOR',
      roleName: '质检员',
      permissionCount: 2
    })
    await flushPromises()

    expect(getRoleMock).toHaveBeenCalledWith(3)
    expect(unwrap(setupState.selectedPermissions)).toEqual([11, 12])
    expect(unwrap(setupState.showPermissionDialog)).toBe(true)
  })

  it('refuses to open permission dialog for ADMIN when target role contains protected permissions', async () => {
    setCurrentUser({ roleCode: 'ADMIN', permissions: ['role:manage'] })
    getRoleMock.mockResolvedValueOnce({
      id: 3,
      permissions: [
        { id: 11, permCode: 'trace:view' },
        { id: 41, permCode: 'role:view' }
      ]
    })

    const wrapper = mount(RoleList)
    await flushPromises()

    const setupState = wrapper.vm.$.setupState
    await setupState.handleAssignPermissions({
      id: 3,
      roleCode: 'INSPECTOR',
      roleName: '质检员'
    })
    await flushPromises()

    expect(toastError).toHaveBeenCalled()
    expect(String(toastError.mock.calls.at(-1)?.[0] || '')).toContain('SUPER_ADMIN')
    expect(unwrap(setupState.showPermissionDialog)).toBe(false)
  })

  it('opens edit dialog when clicking row edit and submits trimmed payload via createRole on new role', async () => {
    const wrapper = mount(RoleList)
    await flushPromises()

    await wrapper.find('[data-testid="role-list-create"]').trigger('click')
    await flushPromises()

    expect(wrapper.find('[data-testid="role-edit-dialog"]').exists()).toBe(true)
    await wrapper.find('[data-testid="role-form-code"]').setValue('  CUSTOM_QC  ')
    await wrapper.find('[data-testid="role-form-name"]').setValue('  自定义质检  ')
    await wrapper.find('[data-testid="role-form-remark"]').setValue(' 仅用于演示 ')

    await wrapper.find('[data-testid="role-form-submit"]').trigger('click')
    await flushPromises()

    expect(createRoleMock).toHaveBeenCalledTimes(1)
    expect(createRoleMock.mock.calls[0][0]).toEqual({
      roleCode: 'CUSTOM_QC',
      roleName: '自定义质检',
      remark: '仅用于演示'
    })
    expect(toastSuccess).toHaveBeenCalledWith(expect.stringContaining('已创建'))
  })

  it('cancels delete when confirm rejects and never calls deleteRole', async () => {
    confirmMock.mockResolvedValueOnce(false)
    const wrapper = mount(RoleList)
    await flushPromises()

    const deleteBtns = wrapper.findAll('[data-testid="role-table-row-delete"]')
    expect(deleteBtns.length).toBeGreaterThanOrEqual(1)
    await deleteBtns[0].trigger('click')
    await flushPromises()

    expect(confirmMock).toHaveBeenCalledTimes(1)
    expect(deleteRoleMock).not.toHaveBeenCalled()
  })

  it('filters role list by keyword on filter chips and updates count', async () => {
    const wrapper = mount(RoleList)
    await flushPromises()

    await wrapper.find('[data-testid="role-filter-keyword"]').setValue('仓管')
    await flushPromises()

    const rows = wrapper.findAll('[data-testid="role-table-row"]')
    expect(rows).toHaveLength(1)
    expect(rows[0].text()).toContain('仓管员')
    expect(wrapper.find('[data-testid="role-filter-count"]').text()).toContain('1')
  })

  it('switches scope chip to system / custom and reduces visible roles', async () => {
    const wrapper = mount(RoleList)
    await flushPromises()

    await wrapper.find('[data-testid="role-filter-scope"]').setValue('system')
    await flushPromises()
    let rows = wrapper.findAll('[data-testid="role-table-row"]')
    const allSystem = rows.every((r) => /SUPER_ADMIN|ADMIN|PRODUCER|WAREHOUSE|LOGISTICS|USER/.test(r.text()))
    expect(allSystem).toBe(true)
    expect(rows.length).toBeGreaterThanOrEqual(2)

    await wrapper.find('[data-testid="role-filter-scope"]').setValue('custom')
    await flushPromises()
    rows = wrapper.findAll('[data-testid="role-table-row"]')
    expect(rows).toHaveLength(1)
    expect(rows[0].text()).toContain('INSPECTOR')
  })

  it('groups permissions by prefix in permission dialog and exposes select-all per group', async () => {
    const wrapper = mount(RoleList, { attachTo: document.body })
    await flushPromises()

    const setupState = wrapper.vm.$.setupState
    await setupState.handleAssignPermissions({
      id: 3,
      roleCode: 'INSPECTOR',
      roleName: '质检员'
    })
    await flushPromises()

    expect(wrapper.find('[data-testid="permission-assign-dialog"]').exists()).toBe(true)
    expect(wrapper.find('[data-testid="permission-group-trace"]').exists()).toBe(true)
    expect(wrapper.find('[data-testid="permission-group-part"]').exists()).toBe(true)
    expect(wrapper.find('[data-testid="permission-group-user"]').exists()).toBe(true)
    expect(wrapper.find('[data-testid="permission-group-role"]').exists()).toBe(true)
    expect(wrapper.find('[data-testid="permission-group-toggle-trace"]').exists()).toBe(true)

    wrapper.unmount()
  })

  it('auto-selects matching :view when checking a :manage permission via togglePermission', async () => {
    const wrapper = mount(RoleList, { attachTo: document.body })
    await flushPromises()

    const setupState = wrapper.vm.$.setupState
    await setupState.handleAssignPermissions({
      id: 3,
      roleCode: 'INSPECTOR',
      roleName: '质检员'
    })
    await flushPromises()

    const partManageCheckbox = wrapper.find('[data-testid="permission-item-check-22"]')
    expect(partManageCheckbox.exists()).toBe(true)
    await partManageCheckbox.setValue(true)
    await flushPromises()

    const ids = unwrap(setupState.selectedPermissions)
    expect(ids).toContain(22)
    expect(ids).toContain(21)

    wrapper.unmount()
  })
})
