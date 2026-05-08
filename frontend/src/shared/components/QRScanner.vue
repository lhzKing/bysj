<script setup>
import { computed, onMounted, onUnmounted, ref, watch } from 'vue'
import { QrcodeStream } from 'vue-qrcode-reader'
import { AlertTriangle, Camera, Loader2, RefreshCw, X, Zap, ZapOff } from 'lucide-vue-next'
import KbdShortcut from '@/shared/components/ui/KbdShortcut.vue'

const props = defineProps({
  inline: { type: Boolean, default: false }
})

const emit = defineEmits(['scan', 'close', 'error'])

const STATUS = {
  Initializing: 'initializing',
  Streaming: 'streaming',
  Error: 'error'
}

const status = ref(STATUS.Initializing)
const errorState = ref(null)
const torchEnabled = ref(false)
const torchSupported = ref(false)
const devices = ref([])
const currentDeviceId = ref(null)
const cameraResolution = ref('')

const cameraGuideHref = '/CAMERA_SCAN_GUIDE.md'

const constraints = computed(() => {
  if (currentDeviceId.value) {
    return { deviceId: { exact: currentDeviceId.value } }
  }
  return { facingMode: { ideal: 'environment' } }
})

const onDetect = (codes) => {
  const value = codes?.[0]?.rawValue
  if (value) emit('scan', value)
}

const onCameraOn = (capabilities) => {
  status.value = STATUS.Streaming
  errorState.value = null
  torchSupported.value = Boolean(capabilities?.torch)
  if (!torchSupported.value) torchEnabled.value = false
  if (capabilities?.width?.max && capabilities?.height?.max) {
    cameraResolution.value = `${capabilities.width.max}×${capabilities.height.max}`
  }
  refreshDevices()
}

const onCameraOff = () => {
  if (status.value !== STATUS.Error) {
    status.value = STATUS.Initializing
  }
}

const mapError = (error) => {
  const name = error?.name || ''
  const msg = String(error?.message || '')
  if (name === 'NotAllowedError') {
    return {
      kind: 'permission',
      title: '无法访问摄像头',
      message: '浏览器拒绝了摄像头权限，请在地址栏左侧的站点设置中允许摄像头访问，然后刷新页面。',
      showGuide: true
    }
  }
  if (name === 'NotFoundError' || name === 'OverconstrainedError') {
    return {
      kind: 'no-device',
      title: '未找到可用摄像头',
      message: '请检查设备是否连接摄像头，或尝试切换到其它输入设备。',
      showGuide: false
    }
  }
  if (name === 'NotSupportedError' || /https/i.test(msg) || /secure/i.test(msg)) {
    return {
      kind: 'insecure',
      title: '需要 HTTPS 才能访问摄像头',
      message: '请通过 HTTPS 或 localhost 访问。如需在局域网访问，请按指引启用自签证书。',
      showGuide: true
    }
  }
  if (name === 'NotReadableError') {
    return {
      kind: 'in-use',
      title: '摄像头被占用',
      message: '请关闭其它使用摄像头的应用（视频会议、其它浏览器标签），然后重试。',
      showGuide: false
    }
  }
  return {
    kind: 'unknown',
    title: '摄像头初始化失败',
    message: msg || '请稍后重试，或切换到手动输入。',
    showGuide: false
  }
}

const onError = (error) => {
  console.error('[QRScanner]', error)
  status.value = STATUS.Error
  errorState.value = mapError(error)
  emit('error', error)
}

const refreshDevices = async () => {
  if (!navigator?.mediaDevices?.enumerateDevices) return
  try {
    const list = await navigator.mediaDevices.enumerateDevices()
    devices.value = list.filter((d) => d.kind === 'videoinput')
  } catch (e) {
    console.warn('[QRScanner] enumerateDevices failed', e)
  }
}

const switchCamera = async () => {
  if (devices.value.length <= 1) await refreshDevices()
  if (devices.value.length <= 1) return
  const idx = devices.value.findIndex((d) => d.deviceId === currentDeviceId.value)
  const nextIdx = (idx + 1) % devices.value.length
  currentDeviceId.value = devices.value[nextIdx].deviceId
  status.value = STATUS.Initializing
}

const toggleTorch = () => {
  if (!torchSupported.value) return
  torchEnabled.value = !torchEnabled.value
}

const retry = () => {
  status.value = STATUS.Initializing
  errorState.value = null
  torchEnabled.value = false
  currentDeviceId.value = null
}

const handleClose = () => {
  emit('close')
}

const handleKeydown = (event) => {
  if (props.inline) return
  if (event.key === 'Escape') {
    event.stopPropagation()
    handleClose()
  }
}

const showCornerGuides = computed(() => status.value === STATUS.Streaming)

watch(
  () => props.inline,
  (inline) => {
    if (inline) document.body.style.overflow = ''
  }
)

onMounted(() => {
  if (!props.inline) {
    document.body.style.overflow = 'hidden'
  }
  document.addEventListener('keydown', handleKeydown)
})

onUnmounted(() => {
  if (!props.inline) {
    document.body.style.overflow = ''
  }
  document.removeEventListener('keydown', handleKeydown)
})
</script>

<template>
  <Teleport to="body" :disabled="props.inline">
    <div
      :class="['qr-scanner', props.inline ? 'qr-scanner--inline' : 'qr-scanner--modal']"
      data-test="qr-scanner"
    >
      <div
        v-if="!props.inline"
        class="qr-scanner__backdrop"
        data-test="qr-scanner-backdrop"
        @click="handleClose"
      />

      <article class="qr-card" role="dialog" aria-label="二维码扫描">
        <header class="qr-card__top">
          <div class="qr-card__top-left">
            <span class="qr-card__live">
              <span
                :class="['qr-card__live-dot', { 'is-streaming': status === STATUS.Streaming }]"
              />
              <span class="qr-card__live-text">{{ status === STATUS.Streaming ? '实时' : '准备中' }}</span>
            </span>
            <span v-if="status === STATUS.Streaming && cameraResolution" class="qr-card__cam-info mono">
              {{ cameraResolution }}
            </span>
          </div>
          <div class="qr-card__top-right">
            <KbdShortcut v-if="!props.inline" tone="inverse" keys="Esc" />
            <button
              v-if="!props.inline"
              type="button"
              class="qr-card__icon-btn"
              data-test="qr-scanner-close"
              aria-label="关闭"
              @click="handleClose"
            >
              <X :size="14" />
            </button>
          </div>
        </header>

        <div class="qr-card__viewport">
          <template v-if="status === STATUS.Error && errorState">
            <div class="qr-card__state" data-test="qr-scanner-error">
              <span class="qr-card__state-icon">
                <AlertTriangle :size="20" />
              </span>
              <h4 class="qr-card__state-title">{{ errorState.title }}</h4>
              <p class="qr-card__state-message">{{ errorState.message }}</p>
              <div class="qr-card__state-actions">
                <button
                  type="button"
                  class="qr-card__state-btn qr-card__state-btn--primary"
                  data-test="qr-scanner-retry"
                  @click="retry"
                >
                  <RefreshCw :size="13" />
                  重试
                </button>
                <a
                  v-if="errorState.showGuide"
                  class="qr-card__state-btn qr-card__state-btn--ghost"
                  :href="cameraGuideHref"
                  target="_blank"
                  rel="noopener"
                >
                  查看摄像头排障指南
                </a>
              </div>
            </div>
          </template>

          <template v-else>
            <qrcode-stream
              :constraints="constraints"
              :torch="torchEnabled"
              class="qr-card__stream"
              @detect="onDetect"
              @camera-on="onCameraOn"
              @camera-off="onCameraOff"
              @error="onError"
            />

            <div v-if="status === STATUS.Initializing" class="qr-card__overlay" data-test="qr-scanner-loading">
              <Loader2 class="qr-card__spinner" :size="20" />
              <span class="qr-card__overlay-text">正在连接摄像头…</span>
            </div>

            <div v-if="showCornerGuides" class="qr-card__guides" aria-hidden="true">
              <span class="qr-card__guide qr-card__guide--tl" />
              <span class="qr-card__guide qr-card__guide--tr" />
              <span class="qr-card__guide qr-card__guide--bl" />
              <span class="qr-card__guide qr-card__guide--br" />
            </div>
          </template>
        </div>

        <footer class="qr-card__bottom">
          <span class="qr-card__hint mono">
            {{ status === STATUS.Streaming ? '就绪 · 等待识别' : status === STATUS.Initializing ? '握手中…' : '已暂停' }}
          </span>
          <div class="qr-card__actions">
            <button
              type="button"
              class="qr-card__action"
              :disabled="devices.length <= 1 || status !== STATUS.Streaming"
              data-test="qr-scanner-switch"
              @click="switchCamera"
            >
              <Camera :size="12" />
              切换设备
            </button>
            <button
              type="button"
              :class="['qr-card__action', { 'is-active': torchEnabled }]"
              :disabled="!torchSupported || status !== STATUS.Streaming"
              data-test="qr-scanner-torch"
              @click="toggleTorch"
            >
              <component :is="torchEnabled ? Zap : ZapOff" :size="12" />
              闪光灯
            </button>
          </div>
        </footer>
      </article>
    </div>
  </Teleport>
</template>

<style scoped>
.qr-scanner--modal {
  position: fixed;
  inset: 0;
  z-index: 1000;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
}

.qr-scanner--inline {
  display: block;
  width: 100%;
}

.qr-scanner__backdrop {
  position: absolute;
  inset: 0;
  background: rgba(15, 23, 42, 0.6);
}

.qr-card {
  position: relative;
  display: flex;
  flex-direction: column;
  width: 100%;
  background: var(--dark-surface-1);
  border: 1px solid var(--dark-hairline);
  border-radius: 16px;
  color: var(--dark-ink);
  overflow: hidden;
  box-shadow: 0 18px 40px -16px rgba(0, 0, 0, 0.45);
}

.qr-scanner--modal .qr-card {
  max-width: 640px;
}

.qr-card__top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 44px;
  padding: 0 16px;
  border-bottom: 1px solid var(--dark-hairline);
  font-size: 12.5px;
  flex-shrink: 0;
}

.qr-card__top-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.qr-card__top-right {
  display: flex;
  align-items: center;
  gap: 8px;
}

.qr-card__live {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.qr-card__live-dot {
  width: 6px;
  height: 6px;
  border-radius: 9999px;
  background: var(--dark-ink-subtle);
}

.qr-card__live-dot.is-streaming {
  background: var(--error);
  animation: qr-pulse 1.4s ease-in-out infinite;
}

@keyframes qr-pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.45; }
}

.qr-card__live-text {
  font-weight: 500;
  color: var(--dark-ink);
}

.qr-card__cam-info {
  font-size: 11.5px;
  color: var(--dark-ink-subtle);
}

.qr-card__icon-btn {
  width: 28px;
  height: 28px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 6px;
  background: transparent;
  color: var(--dark-ink-subtle);
  border: 1px solid transparent;
  cursor: pointer;
  transition: background 0.15s ease, color 0.15s ease;
}

.qr-card__icon-btn:hover,
.qr-card__icon-btn:focus-visible {
  background: var(--dark-surface-2);
  color: var(--dark-ink);
  outline: none;
}

.qr-card__viewport {
  position: relative;
  width: 100%;
  aspect-ratio: 16 / 9;
  background: var(--dark-canvas);
  overflow: hidden;
}

.qr-card__stream {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  object-fit: cover;
}

:deep(.qr-card__stream video) {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.qr-card__overlay {
  position: absolute;
  inset: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 10px;
  background: rgba(1, 1, 2, 0.85);
  color: var(--dark-ink-subtle);
  font-size: 11.5px;
}

.qr-card__overlay-text {
  letter-spacing: 0.2px;
}

.qr-card__spinner {
  color: var(--dark-ink);
  animation: qr-spin 0.9s linear infinite;
}

@keyframes qr-spin {
  to { transform: rotate(360deg); }
}

.qr-card__guides {
  position: absolute;
  top: 50%;
  left: 50%;
  width: 34%;
  aspect-ratio: 1 / 1;
  transform: translate(-50%, -50%);
  pointer-events: none;
}

.qr-card__guide {
  position: absolute;
  width: 20px;
  height: 20px;
}

.qr-card__guide--tl {
  top: 0;
  left: 0;
  border-top: 1px solid rgba(255, 255, 255, 0.85);
  border-left: 1px solid rgba(255, 255, 255, 0.85);
}

.qr-card__guide--tr {
  top: 0;
  right: 0;
  border-top: 1px solid rgba(255, 255, 255, 0.85);
  border-right: 1px solid rgba(255, 255, 255, 0.85);
}

.qr-card__guide--bl {
  bottom: 0;
  left: 0;
  border-bottom: 1px solid rgba(255, 255, 255, 0.85);
  border-left: 1px solid rgba(255, 255, 255, 0.85);
}

.qr-card__guide--br {
  bottom: 0;
  right: 0;
  border-bottom: 1px solid rgba(255, 255, 255, 0.85);
  border-right: 1px solid rgba(255, 255, 255, 0.85);
}

.qr-card__state {
  position: absolute;
  inset: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 10px;
  padding: 32px 24px;
  text-align: center;
  background: var(--dark-canvas);
}

.qr-card__state-icon {
  width: 36px;
  height: 36px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 9999px;
  background: rgba(229, 72, 77, 0.12);
  border: 1px solid rgba(229, 72, 77, 0.4);
  color: var(--error);
  margin-bottom: 6px;
}

.qr-card__state-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--dark-ink);
  letter-spacing: -0.1px;
  margin: 0;
}

.qr-card__state-message {
  font-size: 13px;
  line-height: 1.55;
  color: var(--dark-ink-subtle);
  max-width: 360px;
  margin: 0;
}

.qr-card__state-actions {
  margin-top: 12px;
  display: inline-flex;
  align-items: center;
  gap: 8px;
}

.qr-card__state-btn {
  height: 30px;
  padding: 0 12px;
  border-radius: 6px;
  font-size: 12.5px;
  font-weight: 500;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  cursor: pointer;
  text-decoration: none;
  border: 1px solid transparent;
  transition: background 0.15s ease, color 0.15s ease, border-color 0.15s ease;
}

.qr-card__state-btn--primary {
  background: var(--dark-ink);
  color: var(--dark-canvas);
}

.qr-card__state-btn--primary:hover,
.qr-card__state-btn--primary:focus-visible {
  background: #ffffff;
  outline: none;
}

.qr-card__state-btn--ghost {
  background: transparent;
  color: var(--dark-ink-subtle);
  border-color: var(--dark-hairline);
}

.qr-card__state-btn--ghost:hover,
.qr-card__state-btn--ghost:focus-visible {
  color: var(--dark-ink);
  border-color: var(--dark-ink-subtle);
  outline: none;
}

.qr-card__bottom {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  height: 44px;
  padding: 0 16px;
  border-top: 1px solid var(--dark-hairline);
  flex-shrink: 0;
}

.qr-card__hint {
  font-size: 11px;
  color: rgba(247, 248, 248, 0.6);
}

.qr-card__actions {
  display: inline-flex;
  align-items: center;
  gap: 8px;
}

.qr-card__action {
  height: 26px;
  padding: 0 10px;
  border-radius: 6px;
  font-size: 11.5px;
  font-weight: 500;
  display: inline-flex;
  align-items: center;
  gap: 5px;
  background: rgba(255, 255, 255, 0.06);
  color: rgba(247, 248, 248, 0.8);
  border: 1px solid rgba(255, 255, 255, 0.08);
  cursor: pointer;
  transition: background 0.15s ease, color 0.15s ease, border-color 0.15s ease;
}

.qr-card__action:hover:not(:disabled),
.qr-card__action:focus-visible:not(:disabled) {
  background: rgba(255, 255, 255, 0.12);
  color: var(--dark-ink);
  outline: none;
}

.qr-card__action.is-active {
  background: rgba(94, 106, 210, 0.2);
  border-color: rgba(94, 106, 210, 0.45);
  color: var(--dark-ink);
}

.qr-card__action:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

@media (max-width: 767.98px) {
  .qr-scanner--modal {
    padding: 0;
    align-items: stretch;
  }

  .qr-scanner--modal .qr-card {
    width: 100%;
    max-width: 100%;
    height: 100%;
    border-radius: 0;
    border: 0;
    box-shadow: none;
  }

  .qr-card__viewport {
    aspect-ratio: auto;
    flex: 1 1 auto;
  }

  .qr-card__guides {
    width: 60%;
  }

  .qr-card__guide {
    width: 28px;
    height: 28px;
  }

  .qr-card__top {
    padding: 0 16px calc(env(safe-area-inset-top, 0px));
    padding-top: env(safe-area-inset-top, 0px);
    height: calc(44px + env(safe-area-inset-top, 0px));
  }

  .qr-card__bottom {
    padding-bottom: calc(env(safe-area-inset-bottom, 0px));
    height: calc(44px + env(safe-area-inset-bottom, 0px));
  }
}
</style>
