<script setup>
import DataTable from 'primevue/datatable'
import Column from 'primevue/column'
import BaseButton from '@/shared/components/ui/BaseButton.vue'
import { Edit, Trash2 } from 'lucide-vue-next'

const props = defineProps({
  parts: Array,
  loading: Boolean,
  total: Number,
  query: Object,
  hasMore: Boolean
})

const emit = defineEmits(['edit', 'delete', 'page-change'])
</script>
<template>
  <div class="premium-card rounded-[40px] overflow-hidden p-6 md:p-10">
    <div class="flex justify-between items-end mb-8">
      <div>
          <h3 class="text-2xl font-extrabold text-slate-900 tracking-tight">智能配件矩阵</h3>
          <p class="text-slate-400 text-sm mt-1 font-medium italic">Integrated Inventory System</p>
      </div>
    </div>
    <DataTable :value="parts" :loading="loading" dataKey="id" responsiveLayout="scroll"
      class="neural-table" :pt="{
        table: { class: 'w-full text-left' },
        thead: { class: 'border-b border-slate-100' },
        headerrow: { class: 'text-[10px] font-black text-slate-400 uppercase tracking-[0.2em]' },
        headercell: { class: 'px-6 py-4 bg-transparent border-0' },
        bodyrow: { class: 'hover:bg-slate-50/50 group transition-colors border-b border-slate-50/50' },
        bodycell: { class: 'px-6 py-6 bg-transparent border-0 text-sm font-medium text-slate-700' }
      }"
    >
      <template #empty>
        <div class="p-8 text-center text-slate-400 font-bold">暂无配件数据，请点击右上角"注入新节点"添加</div>
      </template>
      <Column field="partCode" header="IDENTIFIER">
        <template #body="{ data }">
          <span class="font-mono text-indigo-600 font-bold text-base">{{ data.partCode }}</span>
        </template>
      </Column>
      <Column field="partName" header="NOMENCLATURE">
        <template #body="{ data }">
          <div class="flex flex-col">
              <span class="text-slate-900 font-bold text-base">{{ data.partName }}</span>
              <span class="text-[10px] text-slate-400 font-bold uppercase mt-0.5">{{ data.model || 'Standard' }}</span>
          </div>
        </template>
      </Column>
      <Column field="partType" header="TYPE">
        <template #body="{ data }">
          <span class="flex items-center text-emerald-600 font-bold">
              <span class="size-1.5 bg-emerald-500 rounded-full mr-2"></span>
              {{ data.partType }}
          </span>
        </template>
      </Column>
      <Column field="manufacturer" header="MANUFACTURER">
        <template #body="{ data }">
          <span class="font-bold text-slate-600">{{ data.manufacturer }}</span>
        </template>
      </Column>
      <Column header="ACTION" alignFrozen="right" :exportable="false" style="min-width:8rem">
        <template #body="{ data }">
          <div class="flex gap-3 justify-end pr-4">
            <button @click="emit('edit', data)" class="size-10 rounded-xl bg-slate-50 text-slate-400 hover:bg-indigo-600 hover:text-white transition-all flex items-center justify-center shadow-sm hover:shadow-md hover:shadow-indigo-200" title="编辑">
              <Edit class="w-4 h-4" />
            </button>
            <button @click="emit('delete', data)" class="size-10 rounded-xl bg-slate-50 text-slate-400 hover:bg-rose-500 hover:text-white transition-all flex items-center justify-center shadow-sm hover:shadow-md hover:shadow-rose-200" title="删除">
              <Trash2 class="w-4 h-4" />
            </button>
          </div>
        </template>
      </Column>
    </DataTable>
    <!-- Pagination -->
    <div class="mt-8 pt-6 border-t border-slate-100/50 flex justify-between items-center text-sm font-bold text-slate-400">
      <span>共 {{ total }} 条节点记录</span>
      <div class="flex gap-4 items-center">
        <button 
          class="px-6 py-2.5 rounded-xl transition-all"
          :class="query.page <= 1 ? 'bg-slate-50 text-slate-300 cursor-not-allowed' : 'bg-slate-100 hover:bg-indigo-50 hover:text-indigo-600 text-slate-500'"
          :disabled="query.page <= 1" 
          @click="emit('page-change', -1)"
        >
          上一页
        </button>
        <span class="text-slate-900 bg-slate-50 px-4 py-1.5 rounded-lg">{{ query.page }}</span>
        <button 
          class="px-6 py-2.5 rounded-xl transition-all"
          :class="!hasMore ? 'bg-slate-50 text-slate-300 cursor-not-allowed' : 'bg-slate-100 hover:bg-indigo-50 hover:text-indigo-600 text-slate-500'"
          :disabled="!hasMore" 
          @click="emit('page-change', 1)"
        >
          下一页
        </button>
      </div>
    </div>
  </div>
</template>
