<script setup>
import { computed } from 'vue'
import { X as Close, LogOut as SwitchButton, ArrowRight } from 'lucide-vue-next'

const props = defineProps({
  visible: {
    type: Boolean,
    default: false
  },
  items: {
    type: Array,
    default: () => []
  },
  activePath: {
    type: String,
    default: '/'
  },
  username: {
    type: String,
    default: 'User'
  }
})

const emit = defineEmits(['close', 'navigate', 'logout'])

const userInitial = computed(() => props.username.slice(0, 1).toUpperCase())
</script>

<template>
  <Teleport to="body">
    <Transition name="menu-fade">
      <div v-if="visible" class="fixed inset-0 z-[1000] flex flex-col bg-slate-900/95 backdrop-blur-xl" data-test="mobile-drawer">
        
        <header class="p-6 flex items-center justify-between">
          <div class="flex items-center space-x-3">
            <div class="size-10 bg-indigo-600 rounded-xl flex items-center justify-center shadow-lg shadow-indigo-500/50">
              <svg class="size-6 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2.5" d="M13 10V3L4 14h7v7l9-11h-7z"></path></svg>
            </div>
            <span class="text-xl font-black tracking-tighter italic text-white">TRACE<span class="text-indigo-400">.CORE</span></span>
          </div>
          <button
            class="size-10 rounded-full bg-white/10 text-white flex items-center justify-center hover:bg-white/20 transition-colors"
            @click="emit('close')"
          >
            <Close class="w-5 h-5" />
          </button>
        </header>

        <nav class="flex-1 overflow-y-auto px-6 py-4 space-y-4" aria-label="Mobile primary navigation">
          <button
            v-for="item in items"
            :key="item.path"
            type="button"
            class="w-full flex items-center justify-between p-5 rounded-[24px] transition-all"
            :class="item.path === activePath ? 'bg-indigo-600 text-white shadow-xl shadow-indigo-600/30' : 'bg-white/5 text-white/70 hover:bg-white/10 hover:text-white'"
            :data-nav-path="item.path"
            @click="emit('navigate', item.path)"
          >
            <div class="flex items-center gap-5">
              <div class="size-12 rounded-2xl flex items-center justify-center" :class="item.path === activePath ? 'bg-white/20' : 'bg-white/10'">
                <component :is="item.icon" class="w-6 h-6" />
              </div>
              <span class="font-black text-xl tracking-tight">{{ item.label }}</span>
            </div>
            <ArrowRight class="w-6 h-6 opacity-50" v-if="item.path !== activePath" />
          </button>
        </nav>

        <footer class="p-6 pb-8">
          <div class="bg-white/10 rounded-[32px] p-4 flex items-center justify-between border border-white/10 backdrop-blur-md">
            <div class="flex items-center gap-4">
              <div class="size-12 rounded-2xl overflow-hidden ring-[2px] ring-white/20">
                <img :src="'https://ui-avatars.com/api/?name=' + username + '&background=6366f1&color=fff'" :alt="username" class="w-full h-full object-cover">
              </div>
              <div class="flex flex-col text-left">
                <span class="font-black text-white text-lg leading-tight">{{ username }}</span>
                <span class="text-[10px] font-bold text-indigo-300 uppercase tracking-widest">Active Node</span>
              </div>
            </div>
            <button class="size-12 rounded-2xl bg-rose-500/20 text-rose-400 flex items-center justify-center hover:bg-rose-500 hover:text-white transition-colors" @click="emit('logout')">
              <SwitchButton class="w-6 h-6" />
            </button>
          </div>
        </footer>
      </div>
    </Transition>
  </Teleport>
</template>

<style scoped>
.menu-fade-enter-active,
.menu-fade-leave-active {
  transition: all 0.3s cubic-bezier(0.23, 1, 0.32, 1);
}
.menu-fade-enter-from,
.menu-fade-leave-to {
  opacity: 0;
  transform: translateY(20px) scale(0.98);
}
</style>
