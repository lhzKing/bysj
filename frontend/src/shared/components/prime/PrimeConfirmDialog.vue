<script setup>
import { AlertTriangle, Info, X } from 'lucide-vue-next'
import { useConfirm } from '@/shared/composables/useConfirm'

const { isVisible, options, accept, reject } = useConfirm()
</script>

<template>
  <Teleport to="body">
    <Transition name="dialog-fade">
      <div v-if="isVisible" class="fixed inset-0 z-[1000] flex items-center justify-center p-4">
        <!-- Backdrop -->
        <div class="absolute inset-0 bg-slate-900/60 backdrop-blur-sm" @click="reject"></div>
        
        <!-- Dialog -->
        <div class="relative premium-card rounded-[40px] w-full max-w-sm transform transition-all p-8 text-center overflow-hidden group">
          <div class="absolute -top-20 -left-20 size-60 rounded-full blur-[60px] opacity-30" :class="options.type === 'danger' ? 'bg-rose-200' : 'bg-indigo-200'"></div>
          
          <div class="relative z-10">
            <div class="size-16 rounded-3xl flex items-center justify-center mx-auto mb-6 shadow-xl" :class="options.type === 'danger' ? 'bg-rose-500 text-white shadow-rose-200' : 'bg-indigo-600 text-white shadow-indigo-200'">
              <AlertTriangle class="w-8 h-8" v-if="options.type === 'danger'" />
              <Info class="w-8 h-8" v-else />
            </div>
            
            <h3 class="text-2xl font-black text-slate-900 tracking-tight mb-2">{{ options.title }}</h3>
            <p class="text-slate-500 font-medium leading-relaxed">{{ options.message }}</p>
            
            <div class="mt-8 flex gap-4">
              <button @click="reject" class="flex-1 py-4 bg-slate-100 text-slate-600 hover:bg-slate-200 font-bold rounded-2xl transition-colors active:scale-95">
                {{ options.cancelText }}
              </button>
              <button @click="accept" class="flex-1 py-4 font-bold rounded-2xl text-white shadow-lg transition-all active:scale-95" :class="options.type === 'danger' ? 'bg-rose-600 hover:bg-rose-700 shadow-rose-200 hover:shadow-xl hover:shadow-rose-300' : 'bg-indigo-600 hover:bg-indigo-700 shadow-indigo-200 hover:shadow-xl hover:shadow-indigo-300'">
                {{ options.confirmText }}
              </button>
            </div>
          </div>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<style scoped>
.dialog-fade-enter-active,
.dialog-fade-leave-active {
  transition: opacity 0.3s ease;
}
.dialog-fade-enter-from,
.dialog-fade-leave-to {
  opacity: 0;
}
.dialog-fade-enter-active .premium-card,
.dialog-fade-leave-active .premium-card {
  transition: transform 0.3s cubic-bezier(0.23, 1, 0.32, 1);
}
.dialog-fade-enter-from .premium-card,
.dialog-fade-leave-to .premium-card {
  transform: scale(0.95) translateY(20px);
}
</style>
