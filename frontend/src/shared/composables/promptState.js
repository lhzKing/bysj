import { reactive } from 'vue'

export const promptState = reactive({
  visible: false,
  title: '请输入',
  message: '',
  value: '',
  type: 'text',
  placeholder: '',
  confirmText: '确定',
  cancelText: '取消',
  error: '',
  validator: null,
  resolver: null
})

export function resetPromptState() {
  promptState.visible = false
  promptState.title = '请输入'
  promptState.message = ''
  promptState.value = ''
  promptState.type = 'text'
  promptState.placeholder = ''
  promptState.confirmText = '确定'
  promptState.cancelText = '取消'
  promptState.error = ''
  promptState.validator = null
  promptState.resolver = null
}

export function openPrompt(options = {}) {
  resetPromptState()
  promptState.visible = true
  promptState.title = options.title ?? '请输入'
  promptState.message = options.message ?? ''
  promptState.value = options.defaultValue ?? ''
  promptState.type = options.type === 'password' ? 'password' : 'text'
  promptState.placeholder = options.placeholder ?? ''
  promptState.confirmText = options.confirmText ?? '确定'
  promptState.cancelText = options.cancelText ?? '取消'
  promptState.validator = options.validator ?? null

  return new Promise((resolve) => {
    promptState.resolver = resolve
  })
}

export function updatePromptValue(value) {
  promptState.value = value

  if (promptState.error) {
    promptState.error = ''
  }
}

export function confirmPrompt() {
  const validationResult = typeof promptState.validator === 'function'
    ? promptState.validator(promptState.value)
    : true

  if (validationResult !== true) {
    promptState.error = typeof validationResult === 'string' ? validationResult : '输入校验未通过'

    return false
  }

  const resolve = promptState.resolver
  const result = promptState.value

  resetPromptState()
  resolve?.(result)

  return true
}

export function cancelPrompt() {
  const resolve = promptState.resolver

  resetPromptState()
  resolve?.(null)
}
