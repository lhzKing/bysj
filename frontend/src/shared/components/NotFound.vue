<script setup>
import { useRouter } from 'vue-router'
import { Home, ArrowLeft } from 'lucide-vue-next'
import BaseButton from '@/shared/components/ui/BaseButton.vue'

const router = useRouter()

const goHome = () => {
  router.push('/')
}

const goBack = () => {
  router.back()
}
</script>

<template>
  <div class="min-h-screen flex items-center justify-center bg-gray-50 p-4">
    <div class="text-center max-w-md">
      <div class="mb-8">
        <div class="text-9xl font-bold text-gray-200 mb-4">404</div>
        <h1 class="text-2xl font-semibold text-gray-800 mb-2">页面未找到</h1>
        <p class="text-gray-500">
          抱歉，您访问的页面不存在或已被移除
        </p>
      </div>

      <div class="flex gap-4 justify-center">
        <BaseButton @click="goBack" variant="secondary">
          <ArrowLeft class="w-4 h-4 mr-1" />
          返回上页
        </BaseButton>
        <BaseButton @click="goHome" variant="primary">
          <Home class="w-4 h-4 mr-1" />
          返回首页
        </BaseButton>
      </div>

      <div class="mt-12 text-sm text-gray-400">
        <p>如果您认为这是一个错误，请联系系统管理员</p>
      </div>
    </div>
  </div>
</template>