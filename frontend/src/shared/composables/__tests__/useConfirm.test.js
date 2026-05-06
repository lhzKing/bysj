import { beforeEach, describe, expect, it } from 'vitest'
import { useConfirm } from '@/shared/composables/useConfirm'

describe('useConfirm', () => {
  beforeEach(() => {
    const { reject, isVisible } = useConfirm()
    if (isVisible.value) {
      reject()
    }
  })

  it('resolves true when accept callback is triggered', async () => {
    const { confirm, isVisible, options, accept } = useConfirm()

    const resultPromise = confirm({ title: 'Delete user', message: 'Confirm delete?', type: 'danger' })

    expect(isVisible.value).toBe(true)
    expect(options.value).toMatchObject({
      title: 'Delete user',
      message: 'Confirm delete?',
      confirmText: '确认',
      cancelText: '取消',
      type: 'danger'
    })

    accept()

    await expect(resultPromise).resolves.toBe(true)
    expect(isVisible.value).toBe(false)
  })

  it('resolves false when reject callback is triggered', async () => {
    const { confirm, isVisible, options, reject } = useConfirm()

    const resultPromise = confirm({ title: 'Delete user', message: 'Confirm delete?' })

    expect(isVisible.value).toBe(true)
    expect(options.value).toMatchObject({
      title: 'Delete user',
      message: 'Confirm delete?',
      type: 'warning'
    })

    reject()

    await expect(resultPromise).resolves.toBe(false)
    expect(isVisible.value).toBe(false)
  })
})
