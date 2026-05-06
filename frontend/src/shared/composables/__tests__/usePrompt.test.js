import { beforeEach, describe, expect, it } from 'vitest'
import { nextTick } from 'vue'
import { renderWithPrime } from '@/test/renderWithPrime'
import PrimePromptDialog from '@/shared/components/prime/PrimePromptDialog.vue'
import { usePrompt } from '@/shared/composables/usePrompt'
import { resetPromptState } from '@/shared/composables/promptState'

describe('usePrompt', () => {
  beforeEach(() => {
    resetPromptState()
  })

  it('resolves entered value after confirm', async () => {
    const wrapper = renderWithPrime(PrimePromptDialog)
    const { prompt } = usePrompt()

    const resultPromise = prompt({
      title: 'Reset password',
      message: 'Please enter a new password',
      placeholder: 'Please enter a new password',
      type: 'password'
    })

    await nextTick()
    await wrapper.find('input').setValue('new-password')
    await wrapper.find('[data-test="prompt-confirm"]').trigger('click')

    await expect(resultPromise).resolves.toBe('new-password')
  })

  it('returns null when prompt is cancelled', async () => {
    const wrapper = renderWithPrime(PrimePromptDialog)
    const { prompt } = usePrompt()

    const resultPromise = prompt({ title: 'Please enter notes' })

    await nextTick()
    await wrapper.find('[data-test="prompt-cancel"]').trigger('click')

    await expect(resultPromise).resolves.toBe(null)
  })
})
