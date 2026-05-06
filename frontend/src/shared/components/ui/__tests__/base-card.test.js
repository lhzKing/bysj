import { describe, expect, it } from 'vitest'
import { renderWithPrime } from '@/test/renderWithPrime'
import PrimeCard from '@/shared/components/prime/PrimeCard.vue'
import BaseCard from '@/shared/components/ui/BaseCard.vue'

describe('BaseCard compatibility', () => {
  it('forwards noPadding and custom class to PrimeCard', () => {
    const wrapper = renderWithPrime(BaseCard, {
      props: {
        noPadding: true,
        class: 'custom-shell'
      },
      slots: {
        default: '<div>Content area</div>'
      }
    })

    const primeCard = wrapper.findComponent(PrimeCard)

    expect(primeCard.exists()).toBe(true)
    expect(primeCard.props('noPadding')).toBe(true)
    expect(primeCard.props('class')).toContain('custom-shell')
  })
})
