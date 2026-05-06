<script setup>
import { computed } from 'vue'

const props = defineProps({
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
  },
  isMobile: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits(['navigate', 'logout', 'toggle-menu'])
const userInitial = computed(() => props.username.slice(0, 1).toUpperCase())

</script>

<template>
  <nav
    class="fixed top-4 md:top-8 left-1/2 -translate-x-1/2 w-[95%] md:w-[92%] max-w-5xl h-16 md:h-20 nav-island rounded-[32px] md:rounded-[40px] z-50 px-4 md:px-10 flex items-center justify-between transition-all duration-300"
    data-test="floating-nav"
  >
    <div class="flex items-center space-x-3 md:space-x-12">
      <div class="flex items-center space-x-3 group cursor-pointer" @click="emit('navigate', '/')">
        <div class="size-8 md:size-12 bg-indigo-600 rounded-xl md:rounded-2xl flex items-center justify-center shadow-md md:shadow-xl shadow-indigo-200 transition-transform group-hover:rotate-12 shrink-0">
          <svg class="size-5 md:size-7 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2.5" d="M13 10V3L4 14h7v7l9-11h-7z"></path></svg>
        </div>
        <span class="text-lg md:text-2xl font-black tracking-tighter italic">TRACE<span class="text-indigo-600 hidden sm:inline">.CORE</span></span>
      </div>
      
      <div
        v-if="!isMobile"
        class="hidden lg:flex items-center space-x-8 text-sm font-bold tracking-tight"
        data-test="desktop-nav"
      >
        <button
          v-for="item in items"
          :key="item.path"
          class="nav-item transition-all"
          :class="{ 'nav-item-active': item.path === activePath }"
          :data-nav-path="item.path"
          @click="emit('navigate', item.path)"
        >
          {{ item.label }}
        </button>
      </div>
    </div>

    <div class="flex items-center space-x-3 md:space-x-6">
      <div v-if="!isMobile" class="hidden md:flex flex-col items-end">
        <span class="text-[10px] font-black text-slate-400 uppercase tracking-[0.2em] leading-none">Status</span>
        <span class="text-xs font-black text-emerald-500 mt-1.5 flex items-center">
          <span class="size-2 bg-emerald-500 rounded-full animate-ping mr-2 opacity-75"></span> 
          <span class="relative">SECURE NODE</span>
        </span>
      </div>
      
      <button
        v-if="isMobile"
        data-test="mobile-nav-toggle"
        @click="emit('toggle-menu')"
        class="size-8 md:size-10 bg-slate-100/50 rounded-xl flex items-center justify-center text-slate-600 border border-slate-200 hover:bg-indigo-50 hover:text-indigo-600 transition-colors"
      >
        <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 6h16M4 12h16M4 18h16"></path></svg>
      </button>

      <div
        class="size-8 md:size-12 rounded-full md:rounded-2xl overflow-hidden ring-[3px] md:ring-[6px] ring-white/50 shadow-md md:shadow-xl transition-transform hover:scale-110 cursor-pointer shrink-0"
        data-test="logout-action"
        @click="emit('logout')"
      >
        <img :src="'https://ui-avatars.com/api/?name=' + username + '&background=6366f1&color=fff'" :alt="username" class="w-full h-full object-cover">
      </div>
    </div>
  </nav>
</template>
