<script setup>
import { ref, onUnmounted } from 'vue'
import { QrcodeStream } from 'vue-qrcode-reader'
import { X, Camera, Loader2 } from 'lucide-vue-next'

const emit = defineEmits(['scan', 'close'])

const cameraLoading = ref(true)
const cameraError = ref('')

const onDetect = (detectedCodes) => {
  const code = detectedCodes[0]?.rawValue
  if (code) {
    emit('scan', code)
  }
}

const onCameraReady = () => {
  cameraLoading.value = false
}

const onError = (error) => {
  console.error('QR Scanner Error:', error)
  let message = '摄像头访问失败'
  if (error.name === 'NotAllowedError') {
    message = '请在浏览器设置中允许摄像头访问权限'
  } else if (error.name === 'NotFoundError') {
    message = '未找到可用的摄像头设备'
  } else if (error.name === 'NotSupportedError') {
    message = '当前环境不支持摄像头，请确保使用 HTTPS 或 localhost'
  }
  cameraError.value = message
  cameraLoading.value = false
}

const paintBoundingBox = (detectedCodes, ctx) => {
  for (const detectedCode of detectedCodes) {
    const { boundingBox } = detectedCode
    ctx.strokeStyle = '#6366f1'
    ctx.lineWidth = 4
    ctx.strokeRect(boundingBox.x, boundingBox.y, boundingBox.width, boundingBox.height)
  }
}
</script>

<template>
  <Teleport to="body">
    <Transition name="dialog-fade">
      <div class="fixed inset-0 z-[2000] flex items-center justify-center p-4">
        <!-- Backdrop -->
        <div class="absolute inset-0 bg-slate-900/80 backdrop-blur-md" @click="emit('close')"></div>

        <!-- Scanner Card -->
        <div class="relative premium-card rounded-[48px] w-full max-w-lg overflow-hidden shadow-2xl border-none">
          
          <header class="p-8 flex items-center justify-between relative z-10">
            <div>
              <h3 class="text-2xl font-black text-slate-900 tracking-tight">光学脉冲扫描</h3>
              <p class="text-xs font-bold text-slate-400 uppercase tracking-widest mt-1">Neural Optical Scanner</p>
            </div>
            <button @click="emit('close')" class="size-12 rounded-full bg-slate-100 text-slate-500 flex items-center justify-center hover:bg-rose-50 hover:text-rose-500 transition-colors">
              <X class="w-6 h-6" />
            </button>
          </header>

          <div class="aspect-square w-full relative bg-black">
            <div v-if="cameraError" class="absolute inset-0 flex flex-col items-center justify-center p-10 text-center z-20">
               <div class="size-16 rounded-3xl bg-rose-500/20 text-rose-500 flex items-center justify-center mb-6">
                 <X class="w-8 h-8" />
               </div>
               <h4 class="text-xl font-black text-white mb-2">初始化失败</h4>
               <p class="text-slate-400 text-sm font-medium leading-relaxed">{{ cameraError }}</p>
               <button @click="emit('close')" class="mt-8 px-8 py-3 bg-white text-slate-900 font-black rounded-2xl active:scale-95 transition-all">返回</button>
            </div>

            <div v-if="cameraLoading && !cameraError" class="absolute inset-0 flex flex-col items-center justify-center z-20 bg-slate-900">
               <Loader2 class="w-10 h-10 text-indigo-500 animate-spin mb-4" />
               <p class="text-white font-black text-xs uppercase tracking-[0.2em]">Link Establishing...</p>
            </div>

            <qrcode-stream
              v-if="!cameraError"
              @detect="onDetect"
              @error="onError"
              @camera-on="onCameraReady"
              :track="paintBoundingBox"
              class="w-full h-full object-cover"
            />
            
            <!-- Scan Overlay Decorations -->
            <div class="absolute inset-0 pointer-events-none border-[40px] border-black/40"></div>
            <div class="absolute inset-[60px] pointer-events-none">
                <div class="absolute top-0 left-0 size-8 border-l-4 border-t-4 border-indigo-500 rounded-tl-2xl"></div>
                <div class="absolute top-0 right-0 size-8 border-r-4 border-t-4 border-indigo-500 rounded-tr-2xl"></div>
                <div class="absolute bottom-0 left-0 size-8 border-l-4 border-b-4 border-indigo-500 rounded-bl-2xl"></div>
                <div class="absolute bottom-0 right-0 size-8 border-r-4 border-b-4 border-indigo-500 rounded-br-2xl"></div>
                
                <div class="absolute top-0 left-0 w-full h-1 bg-gradient-to-r from-transparent via-indigo-500/50 to-transparent animate-[scan_3s_infinite_ease-in-out]"></div>
            </div>
          </div>

          <footer class="p-8 bg-slate-50 text-center">
            <p class="text-xs font-bold text-slate-400">请将溯源二维码/光学纹理置于中心感应区</p>
          </footer>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<style scoped>
@keyframes scan {
  0% { transform: translateY(0); opacity: 0; }
  50% { opacity: 1; }
  100% { transform: translateY(280px); opacity: 0; }
}

.dialog-fade-enter-active,
.dialog-fade-leave-active {
  transition: opacity 0.4s ease;
}
.dialog-fade-enter-from,
.dialog-fade-leave-to {
  opacity: 0;
}
.dialog-fade-enter-active .premium-card,
.dialog-fade-leave-active .premium-card {
  transition: all 0.5s cubic-bezier(0.23, 1, 0.32, 1);
}
.dialog-fade-enter-from .premium-card,
.dialog-fade-leave-to .premium-card {
  transform: scale(0.9) translateY(40px);
  filter: blur(10px);
}
</style>
