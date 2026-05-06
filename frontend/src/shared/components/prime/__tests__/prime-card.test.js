import { describe, expect, it } from 'vitest'
import { renderWithPrime } from '@/test/renderWithPrime'
import PrimeCard from '@/shared/components/prime/PrimeCard.vue'

describe('PrimeCard', () => {
  it('renders title, content and footer slot', () => {
    const wrapper = renderWithPrime(PrimeCard, {
      props: {
        title: 'Inventory Overview',
        subtitle: 'Updated today'
      },
      slots: {
        default: '<div>Main content</div>',
        footer: '<div>Footer actions</div>'
      }
    })

    expect(wrapper.text()).toContain('Inventory Overview')
    expect(wrapper.text()).toContain('Updated today')
    expect(wrapper.text()).toContain('Main content')
    expect(wrapper.text()).toContain('Footer actions')
  })
})
