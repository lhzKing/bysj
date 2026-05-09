<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import {
  ArrowLeft,
  Camera,
  CameraOff,
  Image as ImageIcon,
  RefreshCw,
  ShieldAlert,
  Trash2
} from 'lucide-vue-next'
import BaseButton from '@/shared/components/ui/BaseButton.vue'
import StatusPill from '@/shared/components/ui/StatusPill.vue'

const router = useRouter()

const videoElement = ref(null)
const isCameraActive = ref(false)
const currentFacingMode = ref('environment')
const availableCameras = ref([])
const cameraResolution = ref('')
const errorMessage = ref('')
const capturedImage = ref(null)
let currentStream = null

const facingLabel = computed(() => (currentFacingMode.value === 'user' ? '前置摄像头' : '后置摄像头'))

const canSwitch = computed(() => isCameraActive.value && availableCameras.value.length > 1)

async function getCameraDevices() {
  try {
    const devices = await navigator.mediaDevices.enumerateDevices()
    availableCameras.value = devices.filter((device) => device.kind === 'videoinput')
  } catch (error) {
    errorMessage.value = `获取摄像头列表失败：${error.message}`
  }
}

async function startCamera() {
  errorMessage.value = ''
  try {
    if (!navigator.mediaDevices || !navigator.mediaDevices.getUserMedia) {
      throw new Error('当前浏览器不支持 getUserMedia')
    }
    const constraints = {
      video: {
        facingMode: currentFacingMode.value,
        width: { ideal: 1920 },
        height: { ideal: 1080 }
      },
      audio: false
    }
    currentStream = await navigator.mediaDevices.getUserMedia(constraints)
    if (videoElement.value) {
      videoElement.value.srcObject = currentStream
      isCameraActive.value = true
      videoElement.value.onloadedmetadata = () => {
        cameraResolution.value = `${videoElement.value.videoWidth}x${videoElement.value.videoHeight}`
      }
    }
    await getCameraDevices()
  } catch (error) {
    if (error.name === 'NotAllowedError') {
      errorMessage.value = '摄像头权限被拒绝，请在浏览器站点设置中重新授权'
    } else if (error.name === 'NotFoundError') {
      errorMessage.value = '未检测到可用的摄像头设备'
    } else if (error.name === 'NotReadableError') {
      errorMessage.value = '摄像头被占用，请先关闭其他正在使用摄像头的应用'
    } else {
      errorMessage.value = `开启摄像头失败：${error.message}`
    }
  }
}

function stopCamera() {
  if (currentStream) {
    currentStream.getTracks().forEach((track) => track.stop())
    currentStream = null
  }
  if (videoElement.value) {
    videoElement.value.srcObject = null
  }
  isCameraActive.value = false
  cameraResolution.value = ''
  errorMessage.value = ''
}

async function switchCamera() {
  stopCamera()
  currentFacingMode.value = currentFacingMode.value === 'user' ? 'environment' : 'user'
  await startCamera()
}

function capturePhoto() {
  if (!videoElement.value) return
  const canvas = document.createElement('canvas')
  canvas.width = videoElement.value.videoWidth
  canvas.height = videoElement.value.videoHeight
  const context = canvas.getContext('2d')
  context.drawImage(videoElement.value, 0, 0, canvas.width, canvas.height)
  capturedImage.value = canvas.toDataURL('image/jpeg', 0.95)
}

function clearPhoto() {
  capturedImage.value = null
}

function goBack() {
  if (window.history.length > 1) {
    router.back()
  } else {
    router.push('/')
  }
}

onMounted(() => {
  getCameraDevices()
})

onBeforeUnmount(() => {
  stopCamera()
})
</script>

<template>
  <div class="camera-test" data-test="camera-test-page">
    <header class="camera-test__topbar">
      <button
        type="button"
        class="camera-test__back"
        data-test="camera-test-back"
        @click="goBack"
      >
        <ArrowLeft :size="14" :stroke-width="2" />
        <span>返回</span>
      </button>
      <div class="camera-test__brand">
        <span class="camera-test__brand-logo" aria-hidden="true">
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="white" stroke-width="2.4" stroke-linecap="round">
            <path d="M3 9h18M3 15h18M9 3v18M15 3v18" />
          </svg>
        </span>
        <span class="camera-test__brand-text">trace.</span>
      </div>
    </header>

    <main class="camera-test__main">
      <section class="camera-test__head">
        <p class="camera-test__eyebrow mono">CAMERA · DEVICE TEST</p>
        <h1 class="camera-test__title">摄像头探测</h1>
        <p class="camera-test__subtitle">
          用于排查扫码工位摄像头授权与画质问题。建议在 HTTPS 环境下访问；首次开启会提示授权。
        </p>
      </section>

      <div class="camera-test__layout">
        <article class="camera-test__viewer-card" data-test="camera-test-viewer">
          <header class="camera-test__viewer-bar">
            <div class="camera-test__live">
              <span
                class="camera-test__live-dot"
                :class="{ 'is-streaming': isCameraActive }"
                aria-hidden="true"
              />
              <span class="camera-test__live-text">{{ isCameraActive ? '取景中' : '未启动' }}</span>
              <span v-if="cameraResolution" class="camera-test__live-meta mono">{{ cameraResolution }}</span>
            </div>
            <span class="camera-test__live-meta mono">{{ facingLabel }}</span>
          </header>
          <div class="camera-test__stage">
            <video
              ref="videoElement"
              autoplay
              playsinline
              class="camera-test__video"
            />
            <div v-if="!isCameraActive" class="camera-test__placeholder">
              <span class="camera-test__placeholder-icon" aria-hidden="true">
                <Camera :size="20" :stroke-width="1.6" />
              </span>
              <p class="camera-test__placeholder-text">点击右侧『开启摄像头』开始取景</p>
            </div>
          </div>
        </article>

        <aside class="camera-test__side">
          <section class="camera-test__panel" data-test="camera-test-controls">
            <header class="camera-test__panel-head">
              <h2 class="camera-test__panel-title">控制台</h2>
              <p class="camera-test__panel-sub">开启 / 关闭 / 切换 / 拍照</p>
            </header>
            <div class="camera-test__panel-body camera-test__actions">
              <BaseButton
                variant="primary"
                size="md"
                block
                :disabled="isCameraActive"
                data-test="camera-test-start"
                @click="startCamera"
              >
                <template #icon><Camera :size="14" :stroke-width="2" /></template>
                {{ isCameraActive ? '摄像头已开启' : '开启摄像头' }}
              </BaseButton>
              <BaseButton
                variant="secondary"
                size="md"
                block
                :disabled="!isCameraActive"
                data-test="camera-test-stop"
                @click="stopCamera"
              >
                <template #icon><CameraOff :size="14" :stroke-width="2" /></template>
                关闭摄像头
              </BaseButton>
              <BaseButton
                variant="secondary"
                size="md"
                block
                :disabled="!canSwitch"
                data-test="camera-test-switch"
                @click="switchCamera"
              >
                <template #icon><RefreshCw :size="14" :stroke-width="2" /></template>
                切换前/后摄像头
              </BaseButton>
              <BaseButton
                variant="secondary"
                size="md"
                block
                :disabled="!isCameraActive"
                data-test="camera-test-capture"
                @click="capturePhoto"
              >
                <template #icon><ImageIcon :size="14" :stroke-width="2" /></template>
                拍照
              </BaseButton>
            </div>
          </section>

          <section class="camera-test__panel" data-test="camera-test-info">
            <header class="camera-test__panel-head">
              <h2 class="camera-test__panel-title">设备信息</h2>
              <p class="camera-test__panel-sub">实时检测当前摄像头能力</p>
            </header>
            <dl class="camera-test__info">
              <div class="camera-test__info-row">
                <dt>摄像头状态</dt>
                <dd>
                  <StatusPill :tone="isCameraActive ? 'success' : 'mute'">
                    {{ isCameraActive ? '运行中' : '未启动' }}
                  </StatusPill>
                </dd>
              </div>
              <div class="camera-test__info-row">
                <dt>可用摄像头数</dt>
                <dd class="mono" data-test="camera-test-camera-count">{{ availableCameras.length }}</dd>
              </div>
              <div class="camera-test__info-row">
                <dt>当前摄像头</dt>
                <dd>{{ facingLabel }}</dd>
              </div>
              <div v-if="cameraResolution" class="camera-test__info-row">
                <dt>视频分辨率</dt>
                <dd class="mono">{{ cameraResolution }}</dd>
              </div>
            </dl>
            <div
              v-if="errorMessage"
              class="camera-test__error"
              data-test="camera-test-error"
              role="alert"
            >
              <ShieldAlert :size="14" :stroke-width="2" />
              <span>{{ errorMessage }}</span>
            </div>
          </section>
        </aside>
      </div>

      <section
        v-if="capturedImage"
        class="camera-test__capture-card"
        data-test="camera-test-capture-card"
      >
        <header class="camera-test__capture-bar">
          <h2 class="camera-test__panel-title">拍摄结果</h2>
          <BaseButton
            variant="text"
            size="sm"
            data-test="camera-test-clear"
            @click="clearPhoto"
          >
            <template #icon><Trash2 :size="13" :stroke-width="2" /></template>
            清除照片
          </BaseButton>
        </header>
        <div class="camera-test__stage camera-test__stage--still">
          <img :src="capturedImage" class="camera-test__video" alt="Captured" />
        </div>
      </section>
    </main>
  </div>
</template>

<style scoped>
.camera-test {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background: var(--canvas);
  color: var(--ink-muted);
}

.camera-test__topbar {
  height: 48px;
  padding: 0 24px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  border-bottom: 1px solid var(--hairline);
  background: var(--surface-1);
  flex-shrink: 0;
}

.camera-test__back {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  height: 30px;
  padding: 0 10px;
  border-radius: 6px;
  background: transparent;
  border: 0;
  font-size: 13px;
  font-weight: 500;
  color: var(--ink-muted);
  cursor: pointer;
  font-family: inherit;
  transition: background 0.12s, color 0.12s;
}

.camera-test__back:hover {
  background: var(--surface-2);
  color: var(--ink);
}

.camera-test__brand {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  font-weight: 600;
  color: var(--ink);
  letter-spacing: -0.4px;
}

.camera-test__brand-logo {
  width: 22px;
  height: 22px;
  border-radius: 6px;
  background: var(--primary);
  display: grid;
  place-items: center;
}

.camera-test__main {
  flex: 1 1 auto;
  width: 100%;
  max-width: 1200px;
  margin: 0 auto;
  padding: 24px 24px 48px;
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.camera-test__head {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.camera-test__eyebrow {
  font-size: 11.5px;
  font-weight: 500;
  letter-spacing: 1.2px;
  color: var(--ink-tertiary);
  margin: 0;
  text-transform: uppercase;
}

.camera-test__title {
  font-size: 28px;
  font-weight: 600;
  letter-spacing: -0.6px;
  line-height: 1.2;
  color: var(--ink);
  margin: 0;
}

.camera-test__subtitle {
  font-size: 13px;
  line-height: 1.5;
  color: var(--ink-subtle);
  margin: 0;
  max-width: 640px;
}

.camera-test__layout {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 320px;
  gap: 16px;
  align-items: start;
}

.camera-test__viewer-card {
  background: var(--dark-surface-1);
  border: 1px solid var(--dark-hairline);
  border-radius: 12px;
  overflow: hidden;
  color: var(--dark-ink);
  display: flex;
  flex-direction: column;
}

.camera-test__viewer-bar {
  height: 40px;
  padding: 0 14px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  border-bottom: 1px solid var(--dark-hairline);
  font-size: 12.5px;
  flex-shrink: 0;
}

.camera-test__live {
  display: inline-flex;
  align-items: center;
  gap: 8px;
}

.camera-test__live-dot {
  width: 6px;
  height: 6px;
  border-radius: 9999px;
  background: var(--dark-ink-subtle);
}

.camera-test__live-dot.is-streaming {
  background: var(--error);
  animation: camera-test-pulse 1.4s ease-in-out infinite;
}

@keyframes camera-test-pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.45; }
}

.camera-test__live-text {
  color: var(--dark-ink);
  font-weight: 500;
}

.camera-test__live-meta {
  color: var(--dark-ink-subtle);
  font-size: 11.5px;
}

.camera-test__stage {
  position: relative;
  width: 100%;
  background: var(--dark-canvas);
  aspect-ratio: 16 / 10;
  display: flex;
  align-items: center;
  justify-content: center;
}

.camera-test__stage--still {
  background: var(--dark-canvas);
}

.camera-test__video {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.camera-test__placeholder {
  position: absolute;
  inset: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 12px;
  color: var(--dark-ink-subtle);
  pointer-events: none;
}

.camera-test__placeholder-icon {
  width: 36px;
  height: 36px;
  border-radius: 9999px;
  background: var(--dark-surface-2);
  border: 1px solid var(--dark-hairline);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  color: var(--dark-ink-subtle);
}

.camera-test__placeholder-text {
  margin: 0;
  font-size: 12.5px;
}

.camera-test__side {
  display: flex;
  flex-direction: column;
  gap: 16px;
  min-width: 0;
}

.camera-test__panel {
  background: var(--surface-1);
  border: 1px solid var(--hairline);
  border-radius: 12px;
  overflow: hidden;
}

.camera-test__panel-head {
  padding: 14px 16px;
  border-bottom: 1px solid var(--hairline);
}

.camera-test__panel-title {
  margin: 0;
  font-size: 14px;
  font-weight: 600;
  color: var(--ink);
  letter-spacing: -0.1px;
}

.camera-test__panel-sub {
  margin: 4px 0 0;
  font-size: 12px;
  color: var(--ink-subtle);
}

.camera-test__panel-body {
  padding: 14px 16px;
}

.camera-test__actions {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.camera-test__info {
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
}

.camera-test__info-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 10px 16px;
  border-bottom: 1px solid var(--hairline);
}

.camera-test__info-row:last-child {
  border-bottom: 0;
}

.camera-test__info-row dt {
  font-size: 12.5px;
  color: var(--ink-subtle);
}

.camera-test__info-row dd {
  margin: 0;
  font-size: 13px;
  font-weight: 500;
  color: var(--ink);
}

.camera-test__error {
  margin: 12px 16px 16px;
  padding: 10px 12px;
  border-radius: 8px;
  background: var(--error-soft);
  border: 1px solid #f8c8ca;
  color: var(--error);
  font-size: 12.5px;
  line-height: 1.5;
  display: flex;
  align-items: flex-start;
  gap: 8px;
}

.camera-test__error svg {
  flex-shrink: 0;
  margin-top: 2px;
}

.camera-test__capture-card {
  background: var(--surface-1);
  border: 1px solid var(--hairline);
  border-radius: 12px;
  overflow: hidden;
}

.camera-test__capture-bar {
  padding: 12px 16px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  border-bottom: 1px solid var(--hairline);
}

@media (max-width: 1023px) {
  .camera-test__layout {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 640px) {
  .camera-test__topbar {
    padding: 0 16px;
  }

  .camera-test__main {
    padding: 16px 16px 32px;
    gap: 16px;
  }

  .camera-test__title {
    font-size: 24px;
    letter-spacing: -0.4px;
  }

  .camera-test__stage {
    aspect-ratio: 4 / 3;
  }
}
</style>
