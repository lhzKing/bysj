<script setup>
import { Search } from 'lucide-vue-next'
import BaseInput from '@/shared/components/ui/BaseInput.vue'

const props = defineProps({
  modelValue: String
})
const emit = defineEmits(['update:modelValue', 'search'])
</script>
<template>
  <div class="relative max-w-2xl mb-8 premium-card rounded-full p-3 flex gap-3 items-center shadow-lg shadow-indigo-100/50">
    <BaseInput 
      :modelValue="modelValue" 
      @update:modelValue="emit('update:modelValue', $event)"
      placeholder="搜索配件名称或编码..."
      :icon="Search"
      class="flex-1 bg-transparent border-0 shadow-none text-lg font-mono placeholder:text-slate-400 focus:ring-0 px-4"
      @keyup.enter="emit('search')"
    />
    <button @click="emit('search')" class="h-12 px-8 bg-indigo-600 hover:bg-indigo-700 text-white font-bold rounded-full transition-all shadow-md shadow-indigo-200 hover:shadow-indigo-300">
      神经元检索
    </button>
  </div>
</template>
