import { describe, expect, it } from 'vitest'
import { mount } from '@vue/test-utils'
import PageHeader from '@/shared/components/ui/PageHeader.vue'

describe('PageHeader', () => {
  it('renders title and subtitle from props', () => {
    const wrapper = mount(PageHeader, {
      props: { title: '总览', subtitle: '2026 年 5 月 5 日 · 周三 · 14:32' }
    })
    expect(wrapper.find('.page-header__title').text()).toBe('总览')
    expect(wrapper.find('.page-header__subtitle').text()).toBe('2026 年 5 月 5 日 · 周三 · 14:32')
  })

  it('overrides title via slot', () => {
    const wrapper = mount(PageHeader, {
      props: { title: 'Fallback' },
      slots: { title: '<span data-test="custom">自定义</span>' }
    })
    expect(wrapper.find('[data-test="custom"]').exists()).toBe(true)
    expect(wrapper.find('.page-header__title').text()).toBe('自定义')
  })

  it('renders right actions slot', () => {
    const wrapper = mount(PageHeader, {
      props: { title: '追溯查询' },
      slots: { actions: '<button data-test="export">导出 CSV</button>' }
    })
    expect(wrapper.find('[data-test="export"]').exists()).toBe(true)
    expect(wrapper.find('.page-header__actions').exists()).toBe(true)
  })

  it('omits subtitle node when neither slot nor prop is provided', () => {
    const wrapper = mount(PageHeader, { props: { title: '仅标题' } })
    expect(wrapper.find('.page-header__subtitle').exists()).toBe(false)
  })
})
