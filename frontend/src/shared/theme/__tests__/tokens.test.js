import { describe, expect, it } from 'vitest'
import { enterpriseTokens } from '@/shared/theme/tokens'
import { enterpriseTheme } from '@/shared/theme/primevue-theme'

describe('enterprise theme tokens', () => {
  it('exports semantic groups for brand, background and text', () => {
    expect(enterpriseTokens.semantic.primary).toBeDefined()
    expect(enterpriseTokens.semantic.bg).toBeDefined()
    expect(enterpriseTokens.semantic.text).toBeDefined()
  })

  it('exports a PrimeVue theme object', () => {
    expect(enterpriseTheme.semantic).toBeDefined()
  })
})
