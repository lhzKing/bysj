import { afterEach, describe, expect, it, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import { h } from 'vue'
import BaseDialog from '@/shared/components/ui/BaseDialog.vue'

const closeIconStub = { template: '<span class="x-stub" />' }

function mountDialog(props = {}, slots = {}) {
  return mount(BaseDialog, {
    props: { modelValue: true, title: 'Title', ...props },
    slots,
    global: {
      stubs: {
        X: closeIconStub
      }
    },
    attachTo: document.body
  })
}

afterEach(() => {
  document.body.innerHTML = ''
  document.body.style.overflow = ''
})

describe('BaseDialog', () => {
  it('does not render anything when modelValue is false', () => {
    const wrapper = mountDialog({ modelValue: false })
    expect(document.querySelector('[data-test="base-dialog"]')).toBeNull()
    wrapper.unmount()
  })

  it('renders title / subtitle / icon and body slot when open', () => {
    const wrapper = mountDialog(
      { title: '入库登记', subtitle: '当前节点：北京仓库', icon: { template: '<i class="i-stub" />' } },
      { default: () => h('p', { class: 'body-content' }, 'BODY') }
    )

    expect(document.querySelector('[data-test="base-dialog-title"]').textContent).toContain('入库登记')
    expect(document.querySelector('[data-test="base-dialog-subtitle"]').textContent).toContain('北京仓库')
    expect(document.querySelector('.body-content').textContent).toBe('BODY')
    expect(document.querySelector('.i-stub')).not.toBeNull()
    wrapper.unmount()
  })

  it('emits update:modelValue=false when backdrop is clicked', async () => {
    const wrapper = mountDialog()
    const backdrop = document.querySelector('[data-test="base-dialog-backdrop"]')
    backdrop.dispatchEvent(new MouseEvent('click', { bubbles: true }))
    await wrapper.vm.$nextTick()

    const updates = wrapper.emitted('update:modelValue') || []
    expect(updates.at(-1)).toEqual([false])
    expect(wrapper.emitted('close')).toBeTruthy()
    wrapper.unmount()
  })

  it('does NOT close on backdrop click when persistent', async () => {
    const wrapper = mountDialog({ persistent: true })
    const backdrop = document.querySelector('[data-test="base-dialog-backdrop"]')
    backdrop.dispatchEvent(new MouseEvent('click', { bubbles: true }))
    await wrapper.vm.$nextTick()

    expect(wrapper.emitted('update:modelValue')).toBeFalsy()
    wrapper.unmount()
  })

  it('emits update:modelValue=false when X close button is clicked', async () => {
    const wrapper = mountDialog()
    const closeBtn = document.querySelector('[data-test="base-dialog-close"]')
    closeBtn.dispatchEvent(new MouseEvent('click', { bubbles: true }))
    await wrapper.vm.$nextTick()

    const updates = wrapper.emitted('update:modelValue') || []
    expect(updates.at(-1)).toEqual([false])
    wrapper.unmount()
  })

  it('hides X close button when closable=false', () => {
    const wrapper = mountDialog({ closable: false })
    expect(document.querySelector('[data-test="base-dialog-close"]')).toBeNull()
    wrapper.unmount()
  })

  it('closes on Escape by default', async () => {
    const wrapper = mountDialog()
    const root = document.querySelector('[data-test="base-dialog"]')
    root.dispatchEvent(new KeyboardEvent('keydown', { key: 'Escape', bubbles: true }))
    await wrapper.vm.$nextTick()

    const updates = wrapper.emitted('update:modelValue') || []
    expect(updates.at(-1)).toEqual([false])
    wrapper.unmount()
  })

  it('does NOT close on Escape when dismissOnEsc=false', async () => {
    const wrapper = mountDialog({ dismissOnEsc: false })
    const root = document.querySelector('[data-test="base-dialog"]')
    root.dispatchEvent(new KeyboardEvent('keydown', { key: 'Escape', bubbles: true }))
    await wrapper.vm.$nextTick()

    expect(wrapper.emitted('update:modelValue')).toBeFalsy()
    wrapper.unmount()
  })

  it('renders footer slot inside the sticky footer', () => {
    const wrapper = mountDialog(
      {},
      { footer: () => h('button', { class: 'footer-btn' }, '提交') }
    )

    const footer = document.querySelector('[data-test="base-dialog-footer"]')
    expect(footer).not.toBeNull()
    expect(footer.querySelector('.footer-btn').textContent).toBe('提交')
    wrapper.unmount()
  })

  it('locks body overflow while open and restores on close', async () => {
    const wrapper = mountDialog()
    expect(document.body.style.overflow).toBe('hidden')

    await wrapper.setProps({ modelValue: false })
    expect(document.body.style.overflow).toBe('')
    wrapper.unmount()
  })

  it.each([
    ['sm', 'base-dialog__card--sm'],
    ['md', 'base-dialog__card--md'],
    ['lg', 'base-dialog__card--lg']
  ])('applies size class for size=%s', (size, expected) => {
    const wrapper = mountDialog({ size })
    expect(document.querySelector(`.${expected}`)).not.toBeNull()
    wrapper.unmount()
  })
})
