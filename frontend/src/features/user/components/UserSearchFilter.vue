<script setup>
import { computed } from 'vue'
import BaseButton from '@/shared/components/ui/BaseButton.vue'
import { Search } from 'lucide-vue-next'

const props = defineProps({
  query: {
    type: Object,
    required: true
  },
  roles: {
    type: Array,
    required: true
  }
})

const emit = defineEmits(['update:query', 'search'])

const localQuery = computed({
  get: () => props.query,
  set: (val) => emit('update:query', val)
})

const handleSearch = () => {
  emit('search')
}

const statusOptions = [
  { label: '所有状态', value: '' },
  { label: '启用', value: 1 },
  { label: '禁用', value: 0 }
]

const roleOptions = computed(() => {
  return [
    { roleName: '所有角色', id: '' },
    ...props.roles
  ]
})
</script>

<template>
  <div class="relative max-w-4xl mb-8 premium-card rounded-[32px] p-4 flex flex-col md:flex-row gap-4 items-center shadow-lg shadow-indigo-100/50 border border-slate-100 z-20">
    <div class="relative flex-1 w-full group/input">
      <div class="absolute inset-y-0 left-0 pl-4 flex items-center pointer-events-none">
        <Search class="h-5 w-5 text-slate-400 group-focus-within/input:text-indigo-600 transition-colors" />
      </div>
      <input
        v-model="localQuery.username"
        type="text"
        placeholder="搜索用户名..."
        class="block w-full pl-12 pr-4 py-4 border-0 bg-slate-50/50 text-slate-900 rounded-2xl ring-1 ring-inset ring-slate-200 focus:ring-2 focus:ring-inset focus:ring-indigo-600 sm:text-sm font-bold transition-shadow shadow-inner placeholder:text-slate-400/70"
        @keyup.enter="handleSearch"
      />
    </div>

    <div class="w-full md:w-48 shrink-0">
      <select 
        v-model="localQuery.roleId"
        class="w-full px-4 py-4 bg-slate-50/50 rounded-2xl ring-1 ring-inset ring-slate-200 text-sm font-bold text-slate-700 focus:outline-none focus:ring-2 focus:ring-inset focus:ring-indigo-600 transition-shadow appearance-none cursor-pointer"
      >
        <option v-for="opt in roleOptions" :key="opt.id" :value="opt.id" class="bg-white text-slate-900">
          {{ opt.roleName }}
        </option>
      </select>
    </div>

    <div class="w-full md:w-40 shrink-0">
      <select 
        v-model="localQuery.status"
        class="w-full px-4 py-4 bg-slate-50/50 rounded-2xl ring-1 ring-inset ring-slate-200 text-sm font-bold text-slate-700 focus:outline-none focus:ring-2 focus:ring-inset focus:ring-indigo-600 transition-shadow appearance-none cursor-pointer"
      >
        <option v-for="opt in statusOptions" :key="opt.value" :value="opt.value" class="bg-white text-slate-900">
          {{ opt.label }}
        </option>
      </select>
    </div>

    <button @click="handleSearch" class="w-full md:w-auto h-[52px] px-8 bg-indigo-600 hover:bg-indigo-700 text-white font-bold rounded-2xl transition-all shadow-md shadow-indigo-200 hover:shadow-indigo-300 flex items-center justify-center shrink-0 active:scale-95">
      <Search class="w-5 h-5 mr-2" />
      查询
    </button>
  </div>
</template>
