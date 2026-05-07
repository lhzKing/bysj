import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import TraceCodeChip from '@/shared/components/ui/TraceCodeChip.vue'

describe('TraceCodeChip', () => {
  let writeTextMock

  beforeEach(() => {
    writeTextMock = vi.fn(() => Promise.resolve())
    Object.defineProperty(window, 'isSecureContext', {
      configurable: true,
      value: true
    })
    Object.defineProperty(navigator, 'clipboard', {
      configurable: true,
      value: { writeText: writeTextMock }
    })
    vi.useFakeTimers()
  })

  afterEach(() => {
    vi.useRealTimers()
  })

  it('renders the trace code in mono', () => {
    const wrapper = mount(TraceCodeChip, {
      props: { code: 'TC-260505-A8F3K2', copyable: false }
    })
    expect(wrapper.find('.trace-chip__code').text()).toBe('TC-260505-A8F3K2')
    expect(wrapper.find('.trace-chip').classes()).not.toContain('trace-chip--copyable')
  })

  it('marks chip as copyable and exposes role=button', () => {
    const wrapper = mount(TraceCodeChip, {
      props: { code: 'TC-260505-A8F3K2' }
    })
    const root = wrapper.find('.trace-chip')
    expect(root.classes()).toContain('trace-chip--copyable')
    expect(root.attributes('role')).toBe('button')
    expect(root.attributes('tabindex')).toBe('0')
  })

  it('copies code on click and emits copy event', async () => {
    const wrapper = mount(TraceCodeChip, {
      props: { code: 'TC-260505-A8F3K2' }
    })
    await wrapper.find('.trace-chip').trigger('click')
    await flushPromises()

    expect(writeTextMock).toHaveBeenCalledWith('TC-260505-A8F3K2')
    expect(wrapper.emitted('copy')).toEqual([['TC-260505-A8F3K2']])
    expect(wrapper.find('.trace-chip').classes()).toContain('trace-chip--copied')
  })

  it('resets copied state after 1.5s', async () => {
    const wrapper = mount(TraceCodeChip, {
      props: { code: 'TC-X' }
    })
    await wrapper.find('.trace-chip').trigger('click')
    await flushPromises()
    expect(wrapper.find('.trace-chip').classes()).toContain('trace-chip--copied')

    vi.advanceTimersByTime(1600)
    await flushPromises()
    expect(wrapper.find('.trace-chip').classes()).not.toContain('trace-chip--copied')
  })

  it('does nothing when copyable=false', async () => {
    const wrapper = mount(TraceCodeChip, {
      props: { code: 'TC-X', copyable: false }
    })
    await wrapper.find('.trace-chip').trigger('click')
    await flushPromises()
    expect(writeTextMock).not.toHaveBeenCalled()
    expect(wrapper.emitted('copy')).toBeUndefined()
  })
})
