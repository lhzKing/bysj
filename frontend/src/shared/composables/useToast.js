const listeners = new Set()

const summaryMap = {
  success: '\u6210\u529f',
  error: '\u9519\u8bef',
  warn: '\u8b66\u544a',
  info: '\u63d0\u793a'
}

function emitToast(payload) {
  listeners.forEach((listener) => listener(payload))
}

export function onToastMessage(listener) {
  listeners.add(listener)

  return () => listeners.delete(listener)
}

export function __resetToastBridge() {
  listeners.clear()
}

function createMessage(severity, message, duration) {
  return {
    group: 'app-toast',
    severity,
    summary: summaryMap[severity] || '\u63d0\u793a',
    detail: message,
    life: duration
  }
}

export function useToast() {
  return {
    success: (message, duration = 3000) => emitToast(createMessage('success', message, duration)),
    error: (message, duration = 3000) => emitToast(createMessage('error', message, duration)),
    warning: (message, duration = 3000) => emitToast(createMessage('warn', message, duration)),
    info: (message, duration = 3000) => emitToast(createMessage('info', message, duration))
  }
}
