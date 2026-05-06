<template>
  <div class="min-h-screen bg-gray-50 p-4">
    <div class="max-w-2xl mx-auto">
      <!-- Header -->
      <div class="bg-white rounded-lg shadow-sm p-6 mb-4">
        <h1 class="text-2xl font-bold text-gray-900 mb-2">📷 摄像头测试页面</h1>
        <p class="text-sm text-gray-500">测试手机摄像头调用和预览功能</p>
      </div>

      <!-- Controls -->
      <div class="bg-white rounded-lg shadow-sm p-6 mb-4">
        <div class="flex flex-col gap-3">
          <button
            @click="startCamera"
            :disabled="isCameraActive"
            class="w-full py-3 px-4 rounded-lg font-medium transition-all duration-200 disabled:opacity-50 disabled:cursor-not-allowed"
            :class="isCameraActive ? 'bg-gray-200 text-gray-500' : 'bg-green-600 text-white hover:bg-green-700 active:scale-[0.98]'"
          >
            {{ isCameraActive ? '✅ 摄像头已开启' : '🎥 开启摄像头' }}
          </button>

          <button
            @click="stopCamera"
            :disabled="!isCameraActive"
            class="w-full py-3 px-4 rounded-lg font-medium transition-all duration-200 disabled:opacity-50 disabled:cursor-not-allowed"
            :class="!isCameraActive ? 'bg-gray-200 text-gray-500' : 'bg-red-600 text-white hover:bg-red-700 active:scale-[0.98]'"
          >
            ⏹️ 关闭摄像头
          </button>

          <button
            @click="switchCamera"
            :disabled="!isCameraActive || availableCameras.length <= 1"
            class="w-full py-3 px-4 rounded-lg font-medium transition-all duration-200 disabled:opacity-50 disabled:cursor-not-allowed bg-blue-600 text-white hover:bg-blue-700 active:scale-[0.98]"
          >
            🔄 切换摄像头 ({{ currentFacingMode === 'user' ? '前置' : '后置' }})
          </button>

          <button
            @click="capturePhoto"
            :disabled="!isCameraActive"
            class="w-full py-3 px-4 rounded-lg font-medium transition-all duration-200 disabled:opacity-50 disabled:cursor-not-allowed bg-purple-600 text-white hover:bg-purple-700 active:scale-[0.98]"
          >
            📸 拍照
          </button>
        </div>
      </div>

      <!-- Camera Preview -->
      <div class="bg-white rounded-lg shadow-sm p-6 mb-4">
        <h2 class="text-lg font-semibold text-gray-900 mb-4">摄像头预览</h2>
        <div class="relative bg-black rounded-lg overflow-hidden" style="aspect-ratio: 4/3;">
          <video
            ref="videoElement"
            autoplay
            playsinline
            class="w-full h-full object-cover"
          ></video>
          <div v-if="!isCameraActive" class="absolute inset-0 flex items-center justify-center text-gray-400">
            <div class="text-center">
              <div class="text-6xl mb-2">📷</div>
              <div class="text-sm">点击"开启摄像头"开始</div>
            </div>
          </div>
        </div>
      </div>

      <!-- Captured Photo -->
      <div v-if="capturedImage" class="bg-white rounded-lg shadow-sm p-6 mb-4">
        <h2 class="text-lg font-semibold text-gray-900 mb-4">拍摄照片</h2>
        <div class="relative bg-black rounded-lg overflow-hidden" style="aspect-ratio: 4/3;">
          <img :src="capturedImage" class="w-full h-full object-cover" alt="Captured" />
        </div>
        <button
          @click="capturedImage = null"
          class="w-full mt-4 py-2 px-4 rounded-lg font-medium bg-gray-200 text-gray-700 hover:bg-gray-300 transition-colors"
        >
          清除照片
        </button>
      </div>

      <!-- Info Panel -->
      <div class="bg-white rounded-lg shadow-sm p-6">
        <h2 class="text-lg font-semibold text-gray-900 mb-4">设备信息</h2>
        <div class="space-y-2 text-sm">
          <div class="flex justify-between py-2 border-b border-gray-100">
            <span class="text-gray-500">摄像头状态:</span>
            <span :class="isCameraActive ? 'text-green-600 font-medium' : 'text-gray-400'">
              {{ isCameraActive ? '运行中' : '未启动' }}
            </span>
          </div>
          <div class="flex justify-between py-2 border-b border-gray-100">
            <span class="text-gray-500">可用摄像头数:</span>
            <span class="text-gray-900 font-medium">{{ availableCameras.length }}</span>
          </div>
          <div class="flex justify-between py-2 border-b border-gray-100">
            <span class="text-gray-500">当前摄像头:</span>
            <span class="text-gray-900 font-medium">{{ currentFacingMode === 'user' ? '前置摄像头' : '后置摄像头' }}</span>
          </div>
          <div v-if="cameraResolution" class="flex justify-between py-2 border-b border-gray-100">
            <span class="text-gray-500">视频分辨率:</span>
            <span class="text-gray-900 font-medium">{{ cameraResolution }}</span>
          </div>
          <div v-if="errorMessage" class="mt-4 p-3 bg-red-50 border border-red-200 rounded-lg">
            <div class="text-red-600 text-sm">
              <strong>错误:</strong> {{ errorMessage }}
            </div>
          </div>
        </div>
      </div>

      <!-- Back Button -->
      <div class="mt-6">
        <button
          @click="$router.back()"
          class="w-full py-3 px-4 rounded-lg font-medium bg-white border border-gray-300 text-gray-700 hover:bg-gray-50 transition-colors"
        >
          ← 返回
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount } from 'vue'

const videoElement = ref(null)
const isCameraActive = ref(false)
const currentFacingMode = ref('environment') // 'user' (前置) 或 'environment' (后置)
const availableCameras = ref([])
const cameraResolution = ref('')
const errorMessage = ref('')
const capturedImage = ref(null)
let currentStream = null

// 获取可用的摄像头列表
const getCameraDevices = async () => {
  try {
    const devices = await navigator.mediaDevices.enumerateDevices()
    availableCameras.value = devices.filter(device => device.kind === 'videoinput')
    console.log('可用摄像头:', availableCameras.value)
  } catch (error) {
    console.error('获取摄像头列表失败:', error)
    errorMessage.value = `获取摄像头列表失败: ${error.message}`
  }
}

// 开启摄像头
const startCamera = async () => {
  errorMessage.value = ''
  
  try {
    // 检查浏览器支持
    if (!navigator.mediaDevices || !navigator.mediaDevices.getUserMedia) {
      throw new Error('您的浏览器不支持摄像头访问')
    }

    const constraints = {
      video: {
        facingMode: currentFacingMode.value,
        width: { ideal: 1920 },
        height: { ideal: 1080 }
      },
      audio: false
    }

    // 请求摄像头权限
    currentStream = await navigator.mediaDevices.getUserMedia(constraints)
    
    // 绑定到 video 元素
    if (videoElement.value) {
      videoElement.value.srcObject = currentStream
      isCameraActive.value = true

      // 获取实际分辨率
      videoElement.value.onloadedmetadata = () => {
        cameraResolution.value = `${videoElement.value.videoWidth}x${videoElement.value.videoHeight}`
      }
    }

    // 更新摄像头列表
    await getCameraDevices()
  } catch (error) {
    console.error('开启摄像头失败:', error)
    if (error.name === 'NotAllowedError') {
      errorMessage.value = '您拒绝了摄像头权限，请在浏览器设置中允许访问'
    } else if (error.name === 'NotFoundError') {
      errorMessage.value = '未找到摄像头设备'
    } else {
      errorMessage.value = `开启摄像头失败: ${error.message}`
    }
  }
}

// 关闭摄像头
const stopCamera = () => {
  if (currentStream) {
    currentStream.getTracks().forEach(track => track.stop())
    currentStream = null
  }
  if (videoElement.value) {
    videoElement.value.srcObject = null
  }
  isCameraActive.value = false
  cameraResolution.value = ''
  errorMessage.value = ''
}

// 切换前后摄像头
const switchCamera = async () => {
  stopCamera()
  currentFacingMode.value = currentFacingMode.value === 'user' ? 'environment' : 'user'
  await startCamera()
}

// 拍照
const capturePhoto = () => {
  if (!videoElement.value) return

  const canvas = document.createElement('canvas')
  canvas.width = videoElement.value.videoWidth
  canvas.height = videoElement.value.videoHeight
  
  const context = canvas.getContext('2d')
  context.drawImage(videoElement.value, 0, 0, canvas.width, canvas.height)
  
  capturedImage.value = canvas.toDataURL('image/jpeg', 0.95)
  console.log('照片已拍摄，大小:', (capturedImage.value.length / 1024).toFixed(2), 'KB')
}

onMounted(() => {
  getCameraDevices()
})

onBeforeUnmount(() => {
  stopCamera()
})
</script>
