import { computed } from 'vue'
import {
  cancelPrompt,
  confirmPrompt,
  openPrompt,
  promptState,
  updatePromptValue
} from './promptState'

export function usePrompt() {
  const visible = computed({
    get: () => promptState.visible,
    set: (value) => {
      if (!value && promptState.visible) {
        cancelPrompt()
      }
    }
  })

  return {
    visible,
    title: computed(() => promptState.title),
    message: computed(() => promptState.message),
    inputType: computed(() => promptState.type),
    placeholder: computed(() => promptState.placeholder),
    error: computed(() => promptState.error),
    confirmText: computed(() => promptState.confirmText),
    cancelText: computed(() => promptState.cancelText),
    inputValue: computed({
      get: () => promptState.value,
      set: (value) => updatePromptValue(value)
    }),
    prompt: openPrompt,
    confirm: confirmPrompt,
    cancel: cancelPrompt
  }
}
