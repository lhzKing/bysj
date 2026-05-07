import { describe, expect, it } from 'vitest'
import { mount } from '@vue/test-utils'
import KbdShortcut from '@/shared/components/ui/KbdShortcut.vue'

describe('KbdShortcut', () => {
  it('renders keys prop content', () => {
    const wrapper = mount(KbdShortcut, { props: { keys: 'F1' } })
    expect(wrapper.text()).toBe('F1')
    expect(wrapper.find('.kbd-shortcut').exists()).toBe(true)
  })

  it('renders default slot over keys prop', () => {
    const wrapper = mount(KbdShortcut, {
      props: { keys: 'fallback' },
      slots: { default: '⌘ K' }
    })
    expect(wrapper.text()).toBe('⌘ K')
  })

  it('applies inverse tone class for primary buttons', () => {
    const wrapper = mount(KbdShortcut, {
      props: { keys: 'F1', tone: 'inverse' }
    })
    expect(wrapper.find('.kbd-shortcut').classes()).toContain('kbd-shortcut--inverse')
  })

  it('defaults to default tone', () => {
    const wrapper = mount(KbdShortcut, { props: { keys: 'Esc' } })
    expect(wrapper.find('.kbd-shortcut').classes()).toContain('kbd-shortcut--default')
  })
})
