import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import { nextTick } from 'vue'
import TraceList from '@/features/trace/views/TraceList.vue'

const { routerPushMock, toastWarning, toastError } = vi.hoisted(() => ({
  routerPushMock: vi.fn(),
  toastWarning: vi.fn(),
  toastError: vi.fn()
}))

vi.mock('vue-router', () => ({
  useRouter: () => ({ push: routerPushMock }),
  RouterLink: {
    props: ['to'],
    template: '<a :data-to="to"><slot /></a>'
  }
}))

vi.mock('@/shared/composables/useToast', () => ({
  useToast: () => ({
    warning: toastWarning,
    error: toastError,
    success: vi.fn(),
    info: vi.fn()
  })
}))

vi.mock('@/shared/utils/logger', () => ({
  logger: { error: vi.fn(), info: vi.fn(), warn: vi.fn() }
}))

const mountTraceList = () => mount(TraceList, {
  global: {
    stubs: {
      QRScanner: true,
      CreateTraceDialog: {
        props: ['modelValue'],
        template: '<div data-testid="create-trace-dialog" :data-open="modelValue"></div>'
      },
      'router-link': {
        props: ['to'],
        template: '<a :data-to="to"><slot /></a>'
      }
    }
  }
})

describe('TraceList — search-by-code + localStorage recents', () => {
  beforeEach(() => {
    routerPushMock.mockReset()
    toastWarning.mockReset()
    toastError.mockReset()
    localStorage.clear()
  })

  afterEach(() => {
    localStorage.clear()
  })

  it('renders page header, search box and empty state when no recents', async () => {
    const wrapper = mountTraceList()
    await flushPromises()

    expect(wrapper.text()).toContain('追溯查询')
    expect(wrapper.find('[data-testid="trace-list-search-input"]').exists()).toBe(true)
    expect(wrapper.find('[data-testid="trace-list-search-submit"]').exists()).toBe(true)
    expect(wrapper.text()).toContain('还没有访问过任何追溯码')
    expect(wrapper.findAll('[data-testid="trace-list-recent-row"]').length).toBe(0)
  })

  it('warns when search submitted with empty input and does not navigate', async () => {
    const wrapper = mountTraceList()
    await flushPromises()

    await wrapper.find('[data-testid="trace-list-search-submit"]').trigger('click')

    expect(toastWarning).toHaveBeenCalledWith('请输入追溯码')
    expect(routerPushMock).not.toHaveBeenCalled()
  })

  it('pushes a new recent record to localStorage and navigates on Enter', async () => {
    const wrapper = mountTraceList()
    await flushPromises()

    const input = wrapper.find('[data-testid="trace-list-search-input"]')
    await input.setValue('TC-260505-A8F3K2')
    await input.trigger('keydown', { key: 'Enter' })

    expect(routerPushMock).toHaveBeenCalledWith('/traces/TC-260505-A8F3K2')

    const stored = JSON.parse(localStorage.getItem('recent_traces'))
    expect(stored).toHaveLength(1)
    expect(stored[0].code).toBe('TC-260505-A8F3K2')
  })

  it('renders previously saved recents and jumps to detail on row click', async () => {
    localStorage.setItem('recent_traces', JSON.stringify([
      { code: 'TC-A', time: '2026-05-08T10:00:00.000Z' },
      { code: 'TC-B', time: '2026-05-08T09:00:00.000Z' }
    ]))

    const wrapper = mountTraceList()
    await flushPromises()

    const rows = wrapper.findAll('[data-testid="trace-list-recent-row"]')
    expect(rows).toHaveLength(2)
    expect(rows[0].attributes('data-code')).toBe('TC-A')

    await rows[1].trigger('click')
    expect(routerPushMock).toHaveBeenCalledWith('/traces/TC-B')

    const stored = JSON.parse(localStorage.getItem('recent_traces'))
    expect(stored[0].code).toBe('TC-B')
  })

  it('removes a single recent without navigating away', async () => {
    localStorage.setItem('recent_traces', JSON.stringify([
      { code: 'TC-A', time: '2026-05-08T10:00:00.000Z' },
      { code: 'TC-B', time: '2026-05-08T09:00:00.000Z' }
    ]))

    const wrapper = mountTraceList()
    await flushPromises()

    const removeBtn = wrapper.findAll('[data-testid="trace-list-recent-remove"]')[0]
    await removeBtn.trigger('click')

    expect(routerPushMock).not.toHaveBeenCalled()
    const stored = JSON.parse(localStorage.getItem('recent_traces'))
    expect(stored).toHaveLength(1)
    expect(stored[0].code).toBe('TC-B')
  })

  it('clears all recents via the clear button', async () => {
    localStorage.setItem('recent_traces', JSON.stringify([
      { code: 'TC-A', time: '2026-05-08T10:00:00.000Z' }
    ]))

    const wrapper = mountTraceList()
    await flushPromises()

    expect(wrapper.find('[data-testid="trace-list-clear-recent"]').exists()).toBe(true)
    await wrapper.find('[data-testid="trace-list-clear-recent"]').trigger('click')
    await nextTick()

    expect(JSON.parse(localStorage.getItem('recent_traces'))).toEqual([])
    expect(wrapper.text()).toContain('还没有访问过任何追溯码')
  })

  it('filters recents by typed code without navigating', async () => {
    localStorage.setItem('recent_traces', JSON.stringify([
      { code: 'TC-AAA-001', time: '2026-05-08T10:00:00.000Z' },
      { code: 'TC-BBB-002', time: '2026-05-08T09:00:00.000Z' }
    ]))

    const wrapper = mountTraceList()
    await flushPromises()

    await wrapper.find('[data-testid="trace-list-search-input"]').setValue('aaa')
    await nextTick()

    const rows = wrapper.findAll('[data-testid="trace-list-recent-row"]')
    expect(rows).toHaveLength(1)
    expect(rows[0].attributes('data-code')).toBe('TC-AAA-001')
    expect(routerPushMock).not.toHaveBeenCalled()
  })
})
