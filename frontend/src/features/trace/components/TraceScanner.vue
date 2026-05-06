<template>
  <div class="scanner-fullscreen">
    <!-- 头部 -->
    <div class="scanner-header">
      <h2>{{ scanModeText }}</h2>
      <button @click="emit('close')" class="close-btn">
        <X class="w-5 h-5" />
      </button>
    </div>
    
    <!-- 摄像头预览 -->
    <div class="camera-container">
      <div v-if="cameraError" class="absolute inset-0 flex flex-col items-center justify-center bg-slate-900/90 text-white z-20 p-8 text-center backdrop-blur-md">
        <h3 class="text-2xl font-black mb-2 tracking-tight text-white">无法唤醒光学模块</h3>
        <p class="text-slate-400 font-medium mb-8 max-w-sm">{{ cameraError }}</p>
        <button @click="emit('close')" class="px-8 py-3 bg-white text-slate-900 font-black rounded-2xl hover:bg-slate-200 transition-colors shadow-xl">
          返回控制台
        </button>
      </div>

      <qrcode-stream
        v-if="!cameraError"
        @detect="onDetect"
        @error="onError"
        @camera-on="onCameraReady"
        :track="paintBoundingBox"
        camera="auto"
        class="qr-stream"
      >
        <div v-if="cameraLoading" class="camera-loading">
          <div class="spinner"></div>
          <p>摄像头加载中...</p>
        </div>
      </qrcode-stream>
      
      <!-- 扫描框 -->
      <div class="scanner-overlay">
        <div class="scanner-frame">
          <div class="corner tl"></div>
          <div class="corner tr"></div>
          <div class="corner bl"></div>
          <div class="corner br"></div>
          <div class="scan-line" :style="{ background: scanLineColor }"></div>
        </div>
        <p class="scanner-hint">请将二维码置于框内</p>
      </div>
    </div>
    
    <!-- 底部工具栏 -->
    <div class="scanner-footer">
      <button @click="toggleFlashlight" class="tool-btn">
        <component :is="flashlightOn ? 'Flashlight' : 'FlashlightOff'" class="w-6 h-6" />
        <span>{{ flashlightOn ? '关闭闪光灯' : '打开闪光灯' }}</span>
      </button>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { QrcodeStream } from 'vue-qrcode-reader'
import { X, Flashlight, FlashlightOff } from 'lucide-vue-next'
import { useToast } from '@/shared/composables/useToast'

const props = defineProps({
  currentAction: {
    type: String,
    default: ''
  }
})

const emit = defineEmits(['close', 'scan'])
const toast = useToast()

const cameraLoading = ref(true)
const flashlightOn = ref(false)
const cameraError = ref('')

const scanModeText = computed(() => {
  if (props.currentAction === 'inbound') return '入库扫描'
  if (props.currentAction === 'outbound') return '出库扫描'
  if (props.currentAction === 'transfer') return '流转扫描'
  return '扫描溯源码'
})

const scanLineColor = computed(() => {
  if (props.currentAction === 'inbound') return 'linear-gradient(90deg, transparent, #84A98C, transparent)'
  if (props.currentAction === 'outbound') return 'linear-gradient(90deg, transparent, #8ECAE6, transparent)'
  if (props.currentAction === 'transfer') return 'linear-gradient(90deg, transparent, #E9C46A, transparent)'
  return 'linear-gradient(90deg, transparent, #374151, transparent)'
})

const onCameraReady = () => {
  cameraLoading.value = false
}

const toggleFlashlight = async () => {
  try {
    const stream = document.querySelector('video')?.srcObject
    if (stream) {
      const track = stream.getVideoTracks()[0]
      const capabilities = track.getCapabilities()
      
      if (capabilities.torch) {
        await track.applyConstraints({
          advanced: [{ torch: !flashlightOn.value }]
        })
        flashlightOn.value = !flashlightOn.value
      } else {
        toast.error('您的设备不支持闪光灯功能')
      }
    }
  } catch (error) {
    console.error('Toggle flashlight error:', error)
    toast.error('闪光灯功能启动失败')
  }
}

const onDetect = (detectedCodes) => {
  const code = detectedCodes[0]?.rawValue
  if (code) {
    emit('scan', code)
  }
}

const onError = (error) => {
  console.error('摄像头错误:', error)
  let message = '摄像头访问失败'
  
  if (error.name === 'NotAllowedError') {
    message = '您拒绝了摄像头权限，请在浏览器设置中允许访问'
  } else if (error.name === 'NotFoundError') {
    message = '未找到摄像头设备'
  } else if (error.name === 'NotReadableError') {
    message = '摄像头被其他程序占用'
  } else if (error.name === 'NotSupportedError') {
    message = '浏览器不支持摄像头访问，请使用 HTTPS 或 localhost'
  }
  
  cameraError.value = message
  emit('camera-error', message)
}

const paintBoundingBox = (detectedCodes, ctx) => {
  for (const detectedCode of detectedCodes) {
    const { boundingBox } = detectedCode
    ctx.strokeStyle = '#84A98C'
    ctx.lineWidth = 4
    ctx.strokeRect(boundingBox.x, boundingBox.y, boundingBox.width, boundingBox.height)
    
    const cornerLength = 20
    ctx.lineWidth = 6
    
    ctx.beginPath()
    ctx.moveTo(boundingBox.x, boundingBox.y + cornerLength)
    ctx.lineTo(boundingBox.x, boundingBox.y)
    ctx.lineTo(boundingBox.x + cornerLength, boundingBox.y)
    ctx.stroke()
    
    ctx.beginPath()
    ctx.moveTo(boundingBox.x + boundingBox.width - cornerLength, boundingBox.y)
    ctx.lineTo(boundingBox.x + boundingBox.width, boundingBox.y)
    ctx.lineTo(boundingBox.x + boundingBox.width, boundingBox.y + cornerLength)
    ctx.stroke()
    
    ctx.beginPath()
    ctx.moveTo(boundingBox.x, boundingBox.y + boundingBox.height - cornerLength)
    ctx.lineTo(boundingBox.x, boundingBox.y + boundingBox.height)
    ctx.lineTo(boundingBox.x + cornerLength, boundingBox.y + boundingBox.height)
    ctx.stroke()
    
    ctx.beginPath()
    ctx.moveTo(boundingBox.x + boundingBox.width - cornerLength, boundingBox.y + boundingBox.height)
    ctx.lineTo(boundingBox.x + boundingBox.width, boundingBox.y + boundingBox.height)
    ctx.lineTo(boundingBox.x + boundingBox.width, boundingBox.y + boundingBox.height - cornerLength)
    ctx.stroke()
  }
}
</script>

<style scoped>

/* 变量 */
:root {
  --bg: #F9FAFB;
  --surface: #FFFFFF;
  --text-main: #374151;
  --text-sub: #9CA3AF;
  --primary: #3B82F6;
  --success: #10B981;
  --error: #EF4444;
}

.scan-hub {
  min-height: 100vh;
  background-color: var(--bg);
  display: flex;
  flex-direction: column;
}

/* ========== 欢迎界面 ========== */
.welcome-screen {
  flex: 1;
  display: flex;
  flex-direction: column;
  max-width: 600px;
  margin: 0 auto;
  width: 100%;
}

.welcome-header {
  padding: 24px 32px;
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
}

.welcome-text {
  display: flex;
  flex-direction: column;
}

.welcome-label {
  font-size: 13px;
  color: var(--text-sub);
  margin-bottom: 6px;
}

.app-name {
  font-size: 24px;
  font-weight: 700;
  color: var(--text-main);
  letter-spacing: -0.5px;
}

.role-pill {
  font-size: 12px;
  font-weight: 600;
  padding: 6px 16px;
  border-radius: 20px;
  background-color: #E5E7EB;
  color: var(--text-main);
}

.welcome-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px 24px;
}

.welcome-icon {
  color: #E5E7EB;
  margin-bottom: 24px;
}

.welcome-title {
  font-size: 28px;
  font-weight: 700;
  color: var(--text-main);
  margin-bottom: 12px;
}

.welcome-desc {
  font-size: 15px;
  color: var(--text-sub);
  text-align: center;
  margin-bottom: 32px;
}

.start-btn {
  padding: 16px 48px;
  font-size: 16px;
  font-weight: 600;
  background: var(--primary);
  color: white;
  border: none;
  border-radius: 16px;
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 12px;
  box-shadow: 0 4px 16px rgba(59, 130, 246, 0.3);
  transition: all 0.2s;
}

.start-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(59, 130, 246, 0.4);
}

.start-btn:active {
  transform: scale(0.98);
}

.feature-list {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  justify-content: center;
  margin-top: 40px;
  max-width: 400px;
}

.feature-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 16px;
  background: white;
  border-radius: 20px;
  font-size: 14px;
  color: var(--text-main);
  box-shadow: 0 2px 8px rgba(0,0,0,0.05);
}

.feature-icon {
  font-size: 16px;
}

/* ========== 扫描器界面 ========== */
.scanner-fullscreen {
  position: fixed;
  inset: 0;
  z-index: 1000;
  background: black;
  display: flex;
  flex-direction: column;
}

.scanner-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px;
  background: rgba(0, 0, 0, 0.8);
  color: white;
  backdrop-filter: blur(10px);
}

.scanner-header h2 {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
}

.close-btn {
  width: 40px;
  height: 40px;
  border: none;
  background: rgba(255, 255, 255, 0.1);
  color: white;
  border-radius: 50%;
  font-size: 20px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: background 0.2s;
}

.close-btn:hover {
  background: rgba(255, 255, 255, 0.2);
}

.camera-container {
  flex: 1;
  position: relative;
  overflow: hidden;
}

.qr-stream {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.camera-loading {
  position: absolute;
  inset: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  background: rgba(0, 0, 0, 0.7);
  color: white;
  z-index: 10;
}

.spinner {
  width: 40px;
  height: 40px;
  border: 3px solid rgba(255, 255, 255, 0.3);
  border-top-color: white;
  border-radius: 50%;
  animation: spin 1s linear infinite;
  margin-bottom: 16px;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.scanner-overlay {
  position: absolute;
  inset: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  pointer-events: none;
}

.scanner-frame {
  width: 280px;
  height: 280px;
  position: relative;
}

.corner {
  position: absolute;
  width: 24px;
  height: 24px;
  border-color: white;
  border-style: solid;
  opacity: 0.9;
}

.tl { top: 0; left: 0; border-width: 3px 0 0 3px; }
.tr { top: 0; right: 0; border-width: 3px 3px 0 0; }
.bl { bottom: 0; left: 0; border-width: 0 0 3px 3px; }
.br { bottom: 0; right: 0; border-width: 0 3px 3px 0; }

.scan-line {
  position: absolute;
  top: 0;
  left: 5%;
  width: 90%;
  height: 3px;
  opacity: 0.7;
  filter: blur(2px);
  animation: scan 2.5s infinite ease-in-out;
}

@keyframes scan {
  0% { top: 0%; opacity: 0; }
  50% { opacity: 1; }
  100% { top: 100%; opacity: 0; }
}

.scanner-hint {
  margin-top: 30px;
  color: white;
  font-size: 14px;
  text-shadow: 0 2px 4px rgba(0,0,0,0.5);
}

.scanner-footer {
  padding: 20px;
  background: rgba(0, 0, 0, 0.8);
  display: flex;
  justify-content: center;
  gap: 16px;
  backdrop-filter: blur(10px);
}

.tool-btn {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  padding: 12px 20px;
  border: none;
  background: rgba(255, 255, 255, 0.1);
  color: white;
  border-radius: 12px;
  font-size: 12px;
  cursor: pointer;
  transition: background 0.2s;
}

.tool-btn:hover {
  background: rgba(255, 255, 255, 0.2);
}

.tool-btn span:first-child {
  font-size: 20px;
}

/* ========== 操作选择界面 ========== */
.action-selector {
  flex: 1;
  display: flex;
  flex-direction: column;
  max-width: 600px;
  margin: 0 auto;
  width: 100%;
  padding: 20px;
}

.action-header {
  display: flex;
  align-items: center;
  margin-bottom: 24px;
}

.back-btn {
  border: none;
  background: none;
  font-size: 16px;
  color: var(--primary);
  cursor: pointer;
  padding: 8px;
  margin-right: 16px;
}

.action-header h2 {
  flex: 1;
  text-align: center;
  margin: 0;
  font-size: 20px;
  font-weight: 600;
  color: var(--text-main);
}

.spacer {
  width: 60px;
}

.scan-result {
  background: white;
  border-radius: 16px;
  padding: 24px;
  text-align: center;
  margin-bottom: 24px;
  box-shadow: 0 2px 12px rgba(0,0,0,0.05);
}

.result-icon {
  width: 64px;
  height: 64px;
  background: linear-gradient(135deg, #10B981, #059669);
  color: white;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 32px;
  margin: 0 auto 16px;
}

.result-label {
  font-size: 14px;
  color: var(--text-sub);
  margin: 0 0 8px;
}

.result-code {
  font-size: 18px;
  font-weight: 600;
  color: var(--text-main);
  font-family: 'Monaco', 'Courier New', monospace;
  margin: 0;
}

.action-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px;
}

.action-card {
  background: white;
  border: 2px solid #E5E7EB;
  border-radius: 16px;
  padding: 20px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  transition: all 0.2s;
  text-align: center;
}

.action-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 16px rgba(0,0,0,0.1);
}

.action-card:active {
  transform: scale(0.98);
}

.action-card.primary {
  grid-column: 1 / -1;
  background: linear-gradient(135deg, #3B82F6, #2563EB);
  border-color: transparent;
  color: white;
}

.action-card.inbound {
  border-color: #84A98C;
}

.action-card.outbound {
  border-color: #8ECAE6;
}

.action-card.transfer {
  border-color: #E9C46A;
}

.action-card.alert {
  border-color: #E76F51;
}

.action-icon {
  font-size: 32px;
}

.action-title {
  font-size: 15px;
  font-weight: 600;
  color: inherit;
}

.action-desc {
  font-size: 12px;
  color: var(--text-sub);
}

.action-card.primary .action-desc {
  color: rgba(255, 255, 255, 0.8);
}

/* ========== Toast ========== */
.toast {
  position: fixed;
  bottom: 40px;
  left: 50%;
  transform: translateX(-50%);
  background: white;
  color: var(--text-main);
  padding: 12px 24px;
  border-radius: 30px;
  font-size: 14px;
  font-weight: 500;
  box-shadow: 0 10px 40px rgba(0,0,0,0.1);
  z-index: 200;
  display: flex;
  align-items: center;
  gap: 8px;
}

.toast-dot {
  width: 8px;
  height: 8px;
  background: var(--success);
  border-radius: 50%;
}

.toast.error .toast-dot {
  background: var(--error);
}

.toast-fade-enter-active,
.toast-fade-leave-active {
  transition: all 0.4s cubic-bezier(0.175, 0.885, 0.32, 1.275);
}

.toast-fade-enter-from {
  opacity: 0;
  transform: translateX(-50%) translateY(20px);
}

.toast-fade-leave-to {
  opacity: 0;
  transform: translateX(-50%) translateY(-20px);
}

</style>
