<script setup>
import { ref, onMounted, onBeforeUnmount } from 'vue'
import { onToastMessage } from '@/shared/composables/useToast'
import { CheckCircle2, AlertCircle, Info, XCircle, X } from 'lucide-vue-next'

const toasts = ref([])
let stopListening = null
let idCounter = 0

const iconMap = {
  success: CheckCircle2,
  error: XCircle,
  warn: AlertCircle,
  info: Info
}

const colorMap = {
  success: 'bg-emerald-50 text-emerald-600 border-emerald-200',
  error: 'bg-rose-50 text-rose-500 border-rose-200',
  warn: 'bg-amber-50 text-amber-500 border-amber-200',
  info: 'bg-indigo-50 text-indigo-600 border-indigo-200'
}

onMounted(() => {
  stopListening = onToastMessage((message) => {
    const id = idCounter++
    toasts.value.push({
      ...message,
      id
    })
    
    if (message.life !== 0) {
      setTimeout(() => {
        removeToast(id)
      }, message.life || 3000)
    }
  })
})

onBeforeUnmount(() => {
  stopListening?.()
})

const removeToast = (id) => {
  toasts.value = toasts.value.filter(t => t.id !== id)
}
</script>

<template>
  <Teleport to="body">
    <div class="fixed top-6 left-1/2 -translate-x-1/2 z-[2000] flex flex-col items-center gap-3 w-full max-w-sm pointer-events-none px-4">
      <TransitionGroup name="toast-list">
        <div 
          v-for="toast in toasts" 
          :key="toast.id"
          class="w-full flex items-start gap-4 p-4 rounded-3xl bg-white/80 backdrop-blur-3xl shadow-2xl border border-white/60 pointer-events-auto transform transition-all duration-300"
        >
          <!-- Custom Icon removed as requested, but keeping a subtle color dot -->
          <div class="mt-1 flex-shrink-0 size-2.5 rounded-full shadow-sm" :class="colorMap[toast.severity].split(' ')[0].replace('50', '500')"></div>
          
          <div class="flex-1">
            <h4 class="text-[14px] font-black tracking-tight text-slate-900">{{ toast.summary }}</h4>
            <p class="text-xs font-bold text-slate-500 mt-1 leading-relaxed">{{ toast.detail }}</p>
          </div>
          
          <button @click="removeToast(toast.id)" class="flex-shrink-0 p-2 -mr-2 -mt-2 text-slate-400 hover:text-slate-600 rounded-full hover:bg-slate-100 transition-colors">
            <X class="w-4 h-4" />
          </button>
        </div>
      </TransitionGroup>
    </div>
  </Teleport>
</template>

<style scoped>
.toast-list-enter-active,
.toast-list-leave-active {
  transition: all 0.4s cubic-bezier(0.23, 1, 0.32, 1);
}
.toast-list-enter-from {
  opacity: 0;
  transform: translateY(-20px) scale(0.95);
}
.toast-list-leave-to {
  opacity: 0;
  transform: translateY(-20px) scale(0.95);
}
</style>
