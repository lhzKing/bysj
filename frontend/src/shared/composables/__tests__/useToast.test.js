import { beforeEach, describe, expect, it } from 'vitest'
import { __resetToastBridge, onToastMessage, useToast } from '@/shared/composables/useToast'

describe('useToast bridge', () => {
  beforeEach(() => {
    __resetToastBridge()
  })

  it('emits readable Chinese summary labels', () => {
    const messages = []
    const stop = onToastMessage((payload) => messages.push(payload))
    const toast = useToast()

    toast.success('保存成功')
    toast.error('保存失败')

    stop()

    expect(messages[0]).toMatchObject({
      severity: 'success',
      summary: '成功',
      detail: '保存成功'
    })
    expect(messages[1]).toMatchObject({
      severity: 'error',
      summary: '错误',
      detail: '保存失败'
    })
  })
})