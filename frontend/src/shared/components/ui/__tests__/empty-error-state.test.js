import { describe, expect, it } from 'vitest'
import { mount } from '@vue/test-utils'
import EmptyState from '@/shared/components/ui/EmptyState.vue'
import ErrorState from '@/shared/components/ui/ErrorState.vue'

describe('EmptyState', () => {
  it('renders default title and skips subtitle/icon when not provided', () => {
    const wrapper = mount(EmptyState)
    expect(wrapper.find('[data-test="empty-state-title"]').text()).toBe('暂无数据')
    expect(wrapper.find('[data-test="empty-state-subtitle"]').exists()).toBe(false)
    expect(wrapper.find('[data-test="empty-state-icon"]').exists()).toBe(false)
  })

  it('renders props.title / subtitle and exposes actions slot', () => {
    const wrapper = mount(EmptyState, {
      props: { title: '尚未创建任何配件', subtitle: '点击右上角"新建配件"开始。' },
      slots: { actions: '<button data-test="cta">新建配件</button>' }
    })
    expect(wrapper.find('[data-test="empty-state-title"]').text()).toBe('尚未创建任何配件')
    expect(wrapper.find('[data-test="empty-state-subtitle"]').text()).toBe('点击右上角"新建配件"开始。')
    expect(wrapper.find('[data-test="cta"]').exists()).toBe(true)
  })

  it('renders icon component when icon prop is provided', () => {
    const FakeIcon = { template: '<svg data-test="custom-icon" />' }
    const wrapper = mount(EmptyState, { props: { icon: FakeIcon } })
    expect(wrapper.find('[data-test="custom-icon"]').exists()).toBe(true)
  })
})

describe('ErrorState', () => {
  it('renders default title / subtitle / retry button', () => {
    const wrapper = mount(ErrorState)
    expect(wrapper.find('[data-test="error-state-title"]').text()).toBe('加载失败')
    expect(wrapper.find('[data-test="error-state-subtitle"]').text()).toContain('请稍后重试')
    expect(wrapper.find('[data-test="error-state-retry"]').exists()).toBe(true)
  })

  it('emits retry event when retry button is clicked', async () => {
    const wrapper = mount(ErrorState)
    await wrapper.find('[data-test="error-state-retry"]').trigger('click')
    expect(wrapper.emitted('retry')).toHaveLength(1)
  })

  it('hides retry button when showRetry=false', () => {
    const wrapper = mount(ErrorState, { props: { showRetry: false } })
    expect(wrapper.find('[data-test="error-state-retry"]').exists()).toBe(false)
    expect(wrapper.find('[data-test="error-state-actions"]').exists()).toBe(false)
  })

  it('respects retryText prop', () => {
    const wrapper = mount(ErrorState, { props: { retryText: '重新加载' } })
    expect(wrapper.find('[data-test="error-state-retry"]').text()).toBe('重新加载')
  })
})
