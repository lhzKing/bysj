import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import PrintLabelDialog from '@/features/trace/components/PrintLabelDialog.vue'

describe('PrintLabelDialog (mode safety door)', () => {
  let printSpy

  beforeEach(() => {
    // window.print 在 jsdom 里是 noop——这里 spy 一下方便断言"是否触发了浏览器打印"
    printSpy = vi.spyOn(window, 'print').mockImplementation(() => {})
  })

  afterEach(() => {
    printSpy.mockRestore()
  })

  it('print 模式：点击确认后调用 window.print 并 emit confirm（父组件随后会调上链 API）', async () => {
    const wrapper = mount(PrintLabelDialog, {
      props: {
        modelValue: true,
        codes: [{ traceCode: 'TRACE-1', qrPayload: 'TRACE-1' }],
        mode: 'print'
      },
      attachTo: document.body
    })

    const btn = document.querySelector('[data-test="print-label-confirm"]')
    expect(btn).not.toBeNull()
    btn.click()

    expect(printSpy).toHaveBeenCalledTimes(1)
    expect(wrapper.emitted('confirm')).toBeTruthy()
    expect(wrapper.emitted('confirm')[0][0].map((c) => c.traceCode)).toEqual(['TRACE-1'])

    wrapper.unmount()
  })

  it('reprint 模式：仍 emit confirm（重打也要上链）+ 标签角标加 RP', async () => {
    const wrapper = mount(PrintLabelDialog, {
      props: {
        modelValue: true,
        codes: [{ traceCode: 'TRACE-1', qrPayload: 'TRACE-1' }],
        mode: 'reprint'
      },
      attachTo: document.body
    })

    expect(document.body.textContent).toContain('RP')

    document.querySelector('[data-test="print-label-confirm"]').click()
    expect(printSpy).toHaveBeenCalledTimes(1)
    expect(wrapper.emitted('confirm')).toBeTruthy()

    wrapper.unmount()
  })

  it('view 模式：点击确认仅调 window.print，绝不 emit confirm（安全门——避免无权限角色误触上链）', async () => {
    const wrapper = mount(PrintLabelDialog, {
      props: {
        modelValue: true,
        codes: [{ traceCode: 'TRACE-1', qrPayload: 'TRACE-1' }],
        mode: 'view'
      },
      attachTo: document.body
    })

    expect(document.body.textContent).toContain('仅预览模式')
    const btn = document.querySelector('[data-test="print-label-confirm"]')
    expect(btn.getAttribute('data-mode')).toBe('view')

    btn.click()

    expect(printSpy).toHaveBeenCalledTimes(1)
    // 关键断言：view 模式下无论怎么点确认，都不会 emit('confirm')——这就是 UI 层的安全门
    expect(wrapper.emitted('confirm')).toBeFalsy()

    wrapper.unmount()
  })
})
