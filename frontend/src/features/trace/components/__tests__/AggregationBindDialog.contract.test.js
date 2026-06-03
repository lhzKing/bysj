import { beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import AggregationBindDialog from '@/features/trace/components/AggregationBindDialog.vue'

const bindAggregationBatchMock = vi.fn()
const toastSuccess = vi.fn()
const toastError = vi.fn()
const toastWarning = vi.fn()
const toastInfo = vi.fn()

vi.mock('@/features/trace/api', () => ({
  bindAggregationBatch: (...args) => bindAggregationBatchMock(...args)
}))

vi.mock('@/shared/composables/useToast', () => ({
  useToast: () => ({
    success: toastSuccess,
    error: toastError,
    warning: toastWarning,
    info: toastInfo
  })
}))

vi.mock('@/shared/utils/logger', () => ({
  logger: { error: vi.fn(), warn: vi.fn(), info: vi.fn() }
}))

function mountDialog(props = {}) {
  return mount(AggregationBindDialog, {
    props: { modelValue: true, defaultRelationType: 'CARTON', ...props },
    global: {
      stubs: { teleport: true, QRScanner: true }
    }
  })
}

async function addChild(wrapper, code) {
  await wrapper.find('[data-test="agg-dialog-child-input"]').setValue(code)
  await wrapper.find('[data-test="agg-dialog-add-child"]').trigger('click')
}

describe('AggregationBindDialog contract', () => {
  beforeEach(() => {
    bindAggregationBatchMock.mockReset()
    toastSuccess.mockReset()
    toastError.mockReset()
    toastWarning.mockReset()
    toastInfo.mockReset()
  })

  it('accumulates multiple child codes (uppercased + deduped) and submits one batch payload', async () => {
    bindAggregationBatchMock.mockResolvedValue({ successCount: 2, failureCount: 0, succeeded: [], failed: [] })
    const wrapper = mountDialog()
    await flushPromises()

    await wrapper.find('[data-test="agg-dialog-parent-input"]').setValue('carton-001')
    await addChild(wrapper, 'tc-001')
    await addChild(wrapper, 'tc-002')
    await addChild(wrapper, 'TC-001') // duplicate (case-insensitive) → ignored

    expect(wrapper.find('[data-test="agg-dialog-child-count"]').text()).toContain('2')
    expect(toastInfo).toHaveBeenCalled() // duplicate notice

    await wrapper.find('[data-test="agg-dialog-submit"]').trigger('click')
    await flushPromises()

    expect(bindAggregationBatchMock).toHaveBeenCalledWith({
      relationType: 'CARTON',
      parentCode: 'CARTON-001',
      childCodes: ['TC-001', 'TC-002']
    })
    expect(toastSuccess).toHaveBeenCalledWith('成功绑定 2 个子码')
    expect(wrapper.emitted('update:modelValue')?.some((e) => e[0] === false)).toBe(true)
    expect(wrapper.emitted('success')).toBeTruthy()
  })

  it('on partial failure keeps the dialog open and retains only the failed codes with their reason', async () => {
    bindAggregationBatchMock.mockResolvedValue({
      successCount: 1,
      failureCount: 1,
      succeeded: [],
      failed: [{ childCode: 'TC-002', code: 10007, message: '单品码已在箱内' }]
    })
    const wrapper = mountDialog()
    await flushPromises()

    await wrapper.find('[data-test="agg-dialog-parent-input"]').setValue('CARTON-001')
    await addChild(wrapper, 'TC-001')
    await addChild(wrapper, 'TC-002')

    await wrapper.find('[data-test="agg-dialog-submit"]').trigger('click')
    await flushPromises()

    expect(toastWarning).toHaveBeenCalled()
    // dialog NOT closed
    expect(wrapper.emitted('update:modelValue')?.filter((e) => e[0] === false).length ?? 0).toBe(0)
    // success still emitted so the workbench refreshes
    expect(wrapper.emitted('success')).toBeTruthy()
    // succeeded code dropped, failed code retained + message shown
    expect(wrapper.find('[data-test="agg-dialog-child-chip-TC-001"]').exists()).toBe(false)
    const failedChip = wrapper.find('[data-test="agg-dialog-child-chip-TC-002"]')
    expect(failedChip.exists()).toBe(true)
    expect(failedChip.text()).toContain('单品码已在箱内')
  })

  it('blocks submit until at least one child code is added', async () => {
    const wrapper = mountDialog()
    await flushPromises()

    const submit = wrapper.find('[data-test="agg-dialog-submit"]')
    expect(submit.attributes('disabled')).toBeDefined()

    await wrapper.find('[data-test="agg-dialog-parent-input"]').setValue('CARTON-001')
    await addChild(wrapper, 'TC-001')

    expect(wrapper.find('[data-test="agg-dialog-submit"]').attributes('disabled')).toBeUndefined()
  })

  it('add-member mode locks parent code and relation type', async () => {
    const wrapper = mountDialog({ defaultRelationType: 'PALLET', presetParentCode: 'PALLET-EXT-009' })
    await flushPromises()

    const parent = wrapper.find('[data-test="agg-dialog-parent-input"]')
    expect(parent.element.value).toBe('PALLET-EXT-009')
    expect(parent.attributes('disabled')).toBeDefined()

    const cartonRadio = wrapper.find('[data-test="agg-dialog-relation-carton"] input')
    expect(cartonRadio.attributes('disabled')).toBeDefined()
  })
})
