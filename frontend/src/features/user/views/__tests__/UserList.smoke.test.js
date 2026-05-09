import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import UserList from '@/features/user/views/UserList.vue'

const {
  toastSuccess,
  toastError,
  confirmMock,
  promptMock,
  getUsersMock,
  getRolesMock,
  createUserMock,
  updateUserMock,
  deleteUserMock,
  batchDeleteUsersMock,
  updateUserStatusMock,
  resetUserPasswordMock
} = vi.hoisted(() => ({
  toastSuccess: vi.fn(),
  toastError: vi.fn(),
  confirmMock: vi.fn(),
  promptMock: vi.fn(),
  getUsersMock: vi.fn(),
  getRolesMock: vi.fn(),
  createUserMock: vi.fn(),
  updateUserMock: vi.fn(),
  deleteUserMock: vi.fn(),
  batchDeleteUsersMock: vi.fn(),
  updateUserStatusMock: vi.fn(),
  resetUserPasswordMock: vi.fn()
}))

vi.mock('@/shared/composables/useToast', () => ({
  useToast: () => ({ success: toastSuccess, error: toastError, warning: vi.fn(), info: vi.fn() })
}))

vi.mock('@/shared/composables/useConfirm', () => ({
  useConfirm: () => ({ confirm: confirmMock })
}))

vi.mock('@/shared/composables/usePrompt', () => ({
  usePrompt: () => ({ prompt: promptMock })
}))

vi.mock('@/features/user/api', () => ({
  getUsers: getUsersMock,
  getRoles: getRolesMock,
  createUser: createUserMock,
  updateUser: updateUserMock,
  deleteUser: deleteUserMock,
  batchDeleteUsers: batchDeleteUsersMock,
  updateUserStatus: updateUserStatusMock,
  resetUserPassword: resetUserPasswordMock
}))

function buildUser(overrides = {}) {
  return {
    id: 5,
    username: 'producer01',
    roleId: 4,
    roleCode: 'PRODUCER',
    roleName: '生产员',
    status: 1,
    createTime: '2026-05-01 10:30:00',
    ...overrides
  }
}

const usersPage = (users = []) => ({ list: users, total: users.length, page: 1, size: 10, totalPages: 1 })

describe('UserList smoke', () => {
  beforeEach(() => {
    getUsersMock.mockResolvedValue(
      usersPage([
        buildUser({ id: 1, username: 'superadmin', roleId: 1, roleCode: 'SUPER_ADMIN', roleName: '超级管理员' }),
        buildUser(),
        buildUser({ id: 6, username: 'warehouse01', roleId: 5, roleCode: 'WAREHOUSE', roleName: '仓管员', status: 0 })
      ])
    )
    getRolesMock.mockResolvedValue([
      { id: 4, roleCode: 'PRODUCER', roleName: '生产员', permissionCount: 3 },
      { id: 5, roleCode: 'WAREHOUSE', roleName: '仓管员', permissionCount: 4 }
    ])
    createUserMock.mockResolvedValue(buildUser({ id: 9 }))
    updateUserMock.mockResolvedValue(buildUser())
    deleteUserMock.mockResolvedValue(undefined)
    batchDeleteUsersMock.mockResolvedValue(2)
    updateUserStatusMock.mockResolvedValue(buildUser())
    resetUserPasswordMock.mockResolvedValue(undefined)
    confirmMock.mockResolvedValue(true)
    promptMock.mockResolvedValue('Newpass123')
  })

  afterEach(() => {
    vi.clearAllMocks()
  })

  it('renders PageHeader title with refresh / create actions and dense rows including role and status', async () => {
    const wrapper = mount(UserList)
    await flushPromises()

    expect(wrapper.text()).toContain('用户管理')
    expect(wrapper.find('[data-testid="user-list-refresh"]').exists()).toBe(true)
    expect(wrapper.find('[data-testid="user-list-create"]').exists()).toBe(true)

    const rows = wrapper.findAll('[data-testid="user-table-row"]')
    expect(rows).toHaveLength(3)
    expect(rows[0].text()).toContain('superadmin')
    expect(rows[0].text()).toContain('SUPER_ADMIN')
    expect(rows[0].text()).toContain('启用')
    expect(rows[1].text()).toContain('producer01')
    expect(rows[1].text()).toContain('生产员')
    expect(rows[2].text()).toContain('warehouse01')
    expect(rows[2].text()).toContain('禁用')
  })

  it('disables superadmin row checkbox so it cannot be batch-selected', async () => {
    const wrapper = mount(UserList)
    await flushPromises()

    const superCheckbox = wrapper.find('[data-testid="user-table-check-1"]')
    expect(superCheckbox.exists()).toBe(true)
    expect(superCheckbox.attributes('disabled')).toBeDefined()

    const producerCheckbox = wrapper.find('[data-testid="user-table-check-5"]')
    expect(producerCheckbox.attributes('disabled')).toBeUndefined()
  })

  it('shows "禁用" action on enabled rows and toggles disable through confirm', async () => {
    const wrapper = mount(UserList)
    await flushPromises()

    const disableBtns = wrapper.findAll('[data-testid="user-table-row-disable"]')
    expect(disableBtns.length).toBeGreaterThanOrEqual(1)
    await disableBtns[0].trigger('click')
    await flushPromises()

    expect(confirmMock).toHaveBeenCalledTimes(1)
    expect(confirmMock.mock.calls[0][0].title).toBe('禁用用户')
    expect(updateUserStatusMock).toHaveBeenCalledWith(1, 0)
    expect(toastSuccess).toHaveBeenCalledWith(expect.stringContaining('已禁用'))
  })

  it('shows "启用" action on disabled rows and enables without confirm prompt', async () => {
    const wrapper = mount(UserList)
    await flushPromises()

    const enableBtn = wrapper.find('[data-testid="user-table-row-enable"]')
    expect(enableBtn.exists()).toBe(true)
    await enableBtn.trigger('click')
    await flushPromises()

    expect(confirmMock).not.toHaveBeenCalled()
    expect(updateUserStatusMock).toHaveBeenCalledWith(6, 1)
    expect(toastSuccess).toHaveBeenCalledWith(expect.stringContaining('已启用'))
  })

  it('selects multiple selectable rows and runs batch delete with confirmation', async () => {
    const wrapper = mount(UserList)
    await flushPromises()

    await wrapper.find('[data-testid="user-table-check-5"]').setValue(true)
    await wrapper.find('[data-testid="user-table-check-6"]').setValue(true)

    expect(wrapper.find('[data-testid="user-table-toolbar"]').text()).toContain('已选')
    expect(wrapper.find('[data-testid="user-table-toolbar"]').text()).toContain('2')

    await wrapper.find('[data-testid="user-table-toolbar-delete"]').trigger('click')
    await flushPromises()

    expect(confirmMock).toHaveBeenCalledTimes(1)
    expect(confirmMock.mock.calls[0][0].title).toContain('批量删除')
    expect(batchDeleteUsersMock).toHaveBeenCalledWith([5, 6])
    expect(toastSuccess).toHaveBeenCalledWith(expect.stringContaining('已删除 2 位用户'))
    expect(wrapper.find('[data-testid="user-table-toolbar"]').exists()).toBe(false)
  })

  it('cancels batch delete when confirm rejects and never calls API', async () => {
    confirmMock.mockResolvedValueOnce(false)
    const wrapper = mount(UserList)
    await flushPromises()

    await wrapper.find('[data-testid="user-table-check-5"]').setValue(true)
    await wrapper.find('[data-testid="user-table-toolbar-delete"]').trigger('click')
    await flushPromises()

    expect(batchDeleteUsersMock).not.toHaveBeenCalled()
  })

  it('passes status=1 / status=0 / roleId=Number when filter chips switch', async () => {
    const wrapper = mount(UserList)
    await flushPromises()
    getUsersMock.mockClear()

    await wrapper.find('[data-testid="user-filter-status"]').setValue('1')
    await flushPromises()
    expect(getUsersMock.mock.calls[0][0]).toMatchObject({ status: 1, page: 1 })

    await wrapper.find('[data-testid="user-filter-status"]').setValue('0')
    await flushPromises()
    expect(getUsersMock.mock.calls[1][0]).toMatchObject({ status: 0, page: 1 })

    await wrapper.find('[data-testid="user-filter-role"]').setValue('4')
    await flushPromises()
    expect(getUsersMock.mock.calls[2][0]).toMatchObject({ roleId: 4, page: 1 })
  })

  it('renders 4-field create form via dialog and submits with trimmed payload', async () => {
    const wrapper = mount(UserList)
    await flushPromises()

    await wrapper.find('[data-testid="user-list-create"]').trigger('click')
    await flushPromises()

    expect(wrapper.find('[data-testid="user-edit-dialog"]').exists()).toBe(true)
    await wrapper.find('[data-testid="user-form-username"]').setValue('  newuser  ')
    await wrapper.find('[data-testid="user-form-password"]').setValue('Pass1234')
    await wrapper.find('[data-testid="user-form-role"]').setValue('4')

    await wrapper.find('[data-testid="user-form-submit"]').trigger('click')
    await flushPromises()

    expect(createUserMock).toHaveBeenCalledTimes(1)
    expect(createUserMock.mock.calls[0][0]).toMatchObject({
      username: 'newuser',
      password: 'Pass1234',
      roleId: 4,
      status: 1
    })
  })

  it('shows readonly status pill in edit dialog reflecting current enabled state', async () => {
    const wrapper = mount(UserList)
    await flushPromises()

    // edit the disabled user (id=6, warehouse01)
    const editButtons = wrapper.findAll('[data-testid="user-table-row-edit"]')
    await editButtons[2].trigger('click')
    await flushPromises()

    const statusRow = wrapper.find('[data-testid="user-form-status"]')
    expect(statusRow.exists()).toBe(true)
    expect(statusRow.text()).toContain('禁用')
  })

  it('reset password button opens prompt with password validator and calls resetUserPassword on confirm', async () => {
    const wrapper = mount(UserList)
    await flushPromises()

    const resetBtns = wrapper.findAll('[data-testid="user-table-row-reset"]')
    await resetBtns[1].trigger('click')
    await flushPromises()

    expect(promptMock).toHaveBeenCalledTimes(1)
    expect(promptMock.mock.calls[0][0].title).toBe('重置密码')
    expect(promptMock.mock.calls[0][0].type).toBe('password')
    expect(typeof promptMock.mock.calls[0][0].validator).toBe('function')

    expect(resetUserPasswordMock).toHaveBeenCalledWith(5, 'Newpass123')
    expect(toastSuccess).toHaveBeenCalledWith(expect.stringContaining('密码已重置'))
  })

  it('does not call resetUserPassword when prompt resolves to null', async () => {
    promptMock.mockResolvedValueOnce(null)
    const wrapper = mount(UserList)
    await flushPromises()

    const resetBtns = wrapper.findAll('[data-testid="user-table-row-reset"]')
    await resetBtns[1].trigger('click')
    await flushPromises()

    expect(resetUserPasswordMock).not.toHaveBeenCalled()
  })
})
