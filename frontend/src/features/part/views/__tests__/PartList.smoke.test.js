import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import PartList from '@/features/part/views/PartList.vue'

const {
  toastSuccess,
  toastError,
  confirmMock,
  getPartsMock,
  getPartTypesMock,
  getManufacturersMock,
  createPartMock,
  updatePartMock,
  deletePartMock,
  batchDeletePartsMock,
  enablePartMock,
  disablePartMock
} = vi.hoisted(() => ({
  toastSuccess: vi.fn(),
  toastError: vi.fn(),
  confirmMock: vi.fn(),
  getPartsMock: vi.fn(),
  getPartTypesMock: vi.fn(),
  getManufacturersMock: vi.fn(),
  createPartMock: vi.fn(),
  updatePartMock: vi.fn(),
  deletePartMock: vi.fn(),
  batchDeletePartsMock: vi.fn(),
  enablePartMock: vi.fn(),
  disablePartMock: vi.fn()
}))

vi.mock('@/shared/composables/useToast', () => ({
  useToast: () => ({ success: toastSuccess, error: toastError, warning: vi.fn(), info: vi.fn() })
}))

vi.mock('@/shared/composables/useConfirm', () => ({
  useConfirm: () => ({ confirm: confirmMock })
}))

vi.mock('@/features/part/api', () => ({
  getParts: getPartsMock,
  getPartTypes: getPartTypesMock,
  getManufacturers: getManufacturersMock,
  createPart: createPartMock,
  updatePart: updatePartMock,
  deletePart: deletePartMock,
  batchDeleteParts: batchDeletePartsMock,
  enablePart: enablePartMock,
  disablePart: disablePartMock
}))

function buildPart(overrides = {}) {
  return {
    id: 1,
    partCode: 'SPU-VALVE-001',
    partName: '工业高压阀门',
    partType: '阀门类',
    model: 'V-2024001',
    manufacturer: '示例制造商',
    unit: '件',
    remark: '演示数据',
    enabled: true,
    ...overrides
  }
}

const partsPage = (parts = []) => ({ list: parts, total: parts.length, page: 1, size: 10, totalPages: 1 })

describe('PartList smoke', () => {
  beforeEach(() => {
    getPartsMock.mockResolvedValue(
      partsPage([
        buildPart(),
        buildPart({ id: 2, partCode: 'SPU-MOTOR-002', partName: '工业电机', partType: '动力设备', enabled: false })
      ])
    )
    getPartTypesMock.mockResolvedValue(['阀门类', '动力设备'])
    getManufacturersMock.mockResolvedValue(['示例制造商', '电机制造厂'])
    createPartMock.mockResolvedValue(buildPart())
    updatePartMock.mockResolvedValue(buildPart())
    deletePartMock.mockResolvedValue(undefined)
    batchDeletePartsMock.mockResolvedValue(2)
    enablePartMock.mockResolvedValue(buildPart({ enabled: true }))
    disablePartMock.mockResolvedValue(buildPart({ enabled: false }))
    confirmMock.mockResolvedValue(true)
  })

  afterEach(() => {
    vi.clearAllMocks()
  })

  it('renders PageHeader title with refresh / create actions and dense rows including enabled status', async () => {
    const wrapper = mount(PartList)
    await flushPromises()

    expect(wrapper.text()).toContain('配件管理')
    expect(wrapper.find('[data-testid="part-list-refresh"]').exists()).toBe(true)
    expect(wrapper.find('[data-testid="part-list-create"]').exists()).toBe(true)

    const rows = wrapper.findAll('[data-testid="part-table-row"]')
    expect(rows).toHaveLength(2)
    expect(rows[0].text()).toContain('SPU-VALVE-001')
    expect(rows[0].text()).toContain('启用')
    expect(rows[1].text()).toContain('SPU-MOTOR-002')
    expect(rows[1].text()).toContain('禁用')
  })

  it('shows "禁用" action on enabled rows and toggles disable through confirm', async () => {
    const wrapper = mount(PartList)
    await flushPromises()

    const disableBtn = wrapper.find('[data-testid="part-table-row-disable"]')
    expect(disableBtn.exists()).toBe(true)
    await disableBtn.trigger('click')
    await flushPromises()

    expect(confirmMock).toHaveBeenCalledTimes(1)
    expect(confirmMock.mock.calls[0][0].title).toBe('禁用配件')
    expect(disablePartMock).toHaveBeenCalledWith(1)
    expect(toastSuccess).toHaveBeenCalledWith(expect.stringContaining('已禁用'))
  })

  it('shows "启用" action on disabled rows and enables without confirm prompt', async () => {
    const wrapper = mount(PartList)
    await flushPromises()

    const enableBtn = wrapper.find('[data-testid="part-table-row-enable"]')
    expect(enableBtn.exists()).toBe(true)
    await enableBtn.trigger('click')
    await flushPromises()

    expect(confirmMock).not.toHaveBeenCalled()
    expect(enablePartMock).toHaveBeenCalledWith(2)
    expect(toastSuccess).toHaveBeenCalledWith(expect.stringContaining('已启用'))
  })

  it('selects multiple rows and runs batch delete with confirmation', async () => {
    const wrapper = mount(PartList)
    await flushPromises()

    const checkboxes = wrapper.findAll('[data-testid^="part-table-check-"]').filter(
      (c) => c.attributes('data-testid') !== 'part-table-check-all'
    )
    expect(checkboxes.length).toBe(2)
    await checkboxes[0].setValue(true)
    await checkboxes[1].setValue(true)

    expect(wrapper.find('[data-testid="part-table-toolbar"]').text()).toContain('已选')
    expect(wrapper.find('[data-testid="part-table-toolbar"]').text()).toContain('2')

    await wrapper.find('[data-testid="part-table-toolbar-delete"]').trigger('click')
    await flushPromises()

    expect(confirmMock).toHaveBeenCalledTimes(1)
    expect(confirmMock.mock.calls[0][0].title).toContain('批量删除')
    expect(batchDeletePartsMock).toHaveBeenCalledWith([1, 2])
    expect(toastSuccess).toHaveBeenCalledWith(expect.stringContaining('已删除 2 条配件'))
    // After batch delete, selectedIds should be cleared and toolbar gone
    expect(wrapper.find('[data-testid="part-table-toolbar"]').exists()).toBe(false)
  })

  it('cancels batch delete when confirm rejects and never calls API', async () => {
    confirmMock.mockResolvedValueOnce(false)
    const wrapper = mount(PartList)
    await flushPromises()

    await wrapper.find('[data-testid="part-table-check-1"]').setValue(true)
    await wrapper.find('[data-testid="part-table-toolbar-delete"]').trigger('click')
    await flushPromises()

    expect(batchDeletePartsMock).not.toHaveBeenCalled()
  })

  it('passes enabled=true when filter chip switches to "状态 · 启用"', async () => {
    const wrapper = mount(PartList)
    await flushPromises()
    getPartsMock.mockClear()

    await wrapper.find('[data-testid="part-filter-enabled"]').setValue('true')
    await flushPromises()

    expect(getPartsMock).toHaveBeenCalledTimes(1)
    expect(getPartsMock.mock.calls[0][0]).toMatchObject({ enabled: true, page: 1 })
  })

  it('passes enabled=false when filter chip switches to "状态 · 禁用"', async () => {
    const wrapper = mount(PartList)
    await flushPromises()
    getPartsMock.mockClear()

    await wrapper.find('[data-testid="part-filter-enabled"]').setValue('false')
    await flushPromises()

    expect(getPartsMock.mock.calls[0][0]).toMatchObject({ enabled: false, page: 1 })
  })

  it('renders 7-field create form via dialog and submits with trimmed payload', async () => {
    const wrapper = mount(PartList)
    await flushPromises()

    await wrapper.find('[data-testid="part-list-create"]').trigger('click')
    await flushPromises()

    expect(wrapper.find('[data-testid="part-edit-dialog"]').exists()).toBe(true)
    await wrapper.find('[data-testid="part-form-code"]').setValue('  SPU-NEW-001  ')
    await wrapper.find('[data-testid="part-form-name"]').setValue(' 新建测试件 ')
    await wrapper.find('[data-testid="part-form-type"]').setValue(' 阀门类 ')
    await wrapper.find('[data-testid="part-form-unit"]').setValue('件')

    await wrapper.find('[data-testid="part-form-submit"]').trigger('click')
    await flushPromises()

    expect(createPartMock).toHaveBeenCalledTimes(1)
    expect(createPartMock.mock.calls[0][0]).toMatchObject({
      partCode: 'SPU-NEW-001',
      partName: '新建测试件',
      partType: '阀门类',
      unit: '件'
    })
  })

  it('shows readonly status pill in edit dialog reflecting current enabled state', async () => {
    const wrapper = mount(PartList)
    await flushPromises()

    // edit the disabled part (id=2)
    const editButtons = wrapper.findAll('[data-testid="part-table-row-edit"]')
    await editButtons[1].trigger('click')
    await flushPromises()

    const statusRow = wrapper.find('[data-testid="part-form-status"]')
    expect(statusRow.exists()).toBe(true)
    expect(statusRow.text()).toContain('禁用')
  })
})
