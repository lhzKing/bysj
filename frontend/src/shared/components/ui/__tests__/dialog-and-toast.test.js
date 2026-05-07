import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import { nextTick } from 'vue'
import Toast from '@/shared/components/ui/Toast.vue'
import ConfirmDialog from '@/shared/components/ui/ConfirmDialog.vue'
import PromptDialog from '@/shared/components/ui/PromptDialog.vue'
import { __resetToastBridge, useToast } from '@/shared/composables/useToast'
import { useConfirm } from '@/shared/composables/useConfirm'
import { usePrompt } from '@/shared/composables/usePrompt'
import { resetPromptState } from '@/shared/composables/promptState'

describe('Linear Toast / ConfirmDialog / PromptDialog (F06)', () => {
  beforeEach(() => {
    vi.useFakeTimers()
    __resetToastBridge()
    resetPromptState()
  })

  afterEach(() => {
    vi.useRealTimers()
    document.body.style.overflow = ''
  })

  it('Toast renders summary + severity dot and auto-dismisses after life ms', async () => {
    const wrapper = mount(Toast)
    await nextTick()

    const toast = useToast()
    toast.success('保存成功', 1200)
    await nextTick()

    const items = wrapper.findAll('[data-test="toast-item"]')
    expect(items).toHaveLength(1)
    expect(items[0].text()).toContain('成功')
    expect(items[0].text()).toContain('保存成功')
    expect(items[0].find('.toast__dot--success').exists()).toBe(true)

    vi.advanceTimersByTime(1300)
    await nextTick()
    await nextTick()
    expect(wrapper.vm.$.setupState.toasts).toHaveLength(0)
  })

  it('Toast caps visible stack at 3 entries', async () => {
    const wrapper = mount(Toast)
    await nextTick()

    const toast = useToast()
    toast.info('a')
    toast.info('b')
    toast.info('c')
    toast.info('d')
    toast.info('e')
    await nextTick()

    expect(wrapper.findAll('[data-test="toast-item"]')).toHaveLength(3)
  })

  it('ConfirmDialog resolves true on accept and false on cancel', async () => {
    const wrapper = mount(ConfirmDialog)
    const { confirm } = useConfirm()

    const acceptPromise = confirm({
      title: '退出登录',
      message: '确定要退出当前账号吗？',
      confirmText: '退出',
      cancelText: '取消',
      type: 'danger'
    })

    await nextTick()
    expect(wrapper.find('[data-test="confirm-title"]').text()).toBe('退出登录')
    expect(wrapper.find('[data-test="confirm-message"]').text()).toBe('确定要退出当前账号吗？')
    const acceptBtn = wrapper.find('[data-test="confirm-accept"]')
    expect(acceptBtn.classes()).toContain('base-btn--danger')

    await acceptBtn.trigger('click')
    await expect(acceptPromise).resolves.toBe(true)

    const rejectPromise = confirm({ title: 'X' })
    await nextTick()
    await wrapper.find('[data-test="confirm-cancel"]').trigger('click')
    await expect(rejectPromise).resolves.toBe(false)
  })

  it('ConfirmDialog backdrop click dismisses with false', async () => {
    const wrapper = mount(ConfirmDialog)
    const { confirm } = useConfirm()

    const result = confirm({ title: '提示' })
    await nextTick()

    await wrapper.find('[data-test="confirm-backdrop"]').trigger('click')
    await expect(result).resolves.toBe(false)
  })

  it('PromptDialog returns input value on confirm and null on cancel', async () => {
    const wrapper = mount(PromptDialog)
    const { prompt } = usePrompt()

    const submit = prompt({
      title: '追溯码自助验签',
      message: '输入追溯码以发起只读验签。',
      placeholder: 'TC-260505-A8F3K2'
    })

    await nextTick()
    expect(wrapper.find('[data-test="prompt-title"]').text()).toBe('追溯码自助验签')

    const input = wrapper.find('input')
    await input.setValue('TC-260505-A8F3K2')
    await wrapper.find('[data-test="prompt-confirm"]').trigger('click')
    await expect(submit).resolves.toBe('TC-260505-A8F3K2')

    const dismiss = prompt({ title: 'X' })
    await nextTick()
    await wrapper.find('[data-test="prompt-cancel"]').trigger('click')
    await expect(dismiss).resolves.toBe(null)
  })

  it('PromptDialog surfaces validator error inline without resolving', async () => {
    const wrapper = mount(PromptDialog)
    const { prompt } = usePrompt()

    let settled = false
    prompt({
      title: '请输入',
      validator: (value) => (value.length >= 3 ? true : '至少 3 个字符')
    }).then(() => {
      settled = true
    })

    await nextTick()
    await wrapper.find('input').setValue('ab')
    await wrapper.find('[data-test="prompt-confirm"]').trigger('click')
    await nextTick()

    expect(wrapper.text()).toContain('至少 3 个字符')
    expect(settled).toBe(false)
  })
})
