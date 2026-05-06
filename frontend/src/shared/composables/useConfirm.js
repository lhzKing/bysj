import { ref } from 'vue'

const isVisible = ref(false)
const options = ref({})
let resolvePromise = null

export function useConfirm() {
  function confirm(opts = {}) {
    options.value = {
      title: '确认操作',
      message: '确定要执行此操作吗？',
      confirmText: '确认',
      cancelText: '取消',
      type: 'warning',
      ...opts
    }
    
    isVisible.value = true

    return new Promise((resolve) => {
      resolvePromise = resolve
    })
  }

  const accept = () => {
    isVisible.value = false
    if (resolvePromise) {
      resolvePromise(true)
      resolvePromise = null
    }
  }

  const reject = () => {
    isVisible.value = false
    if (resolvePromise) {
      resolvePromise(false)
      resolvePromise = null
    }
  }

  return { confirm, isVisible, options, accept, reject }
}