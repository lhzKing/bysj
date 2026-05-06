<script setup>
import { computed } from 'vue'
import LoadingSkeleton from '@/shared/components/ui/LoadingSkeleton.vue'
import DataTable from 'primevue/datatable'
import Column from 'primevue/column'
import Tag from 'primevue/tag'
import { Edit, RefreshCw, Power, Trash2 } from 'lucide-vue-next'

const props = defineProps({
  users: {
    type: Array,
    required: true
  },
  loading: {
    type: Boolean,
    default: false
  },
  total: {
    type: Number,
    required: true
  },
  query: {
    type: Object,
    required: true
  }
})

const emit = defineEmits(['edit', 'resetPassword', 'statusChange', 'delete', 'pageChange'])

const getUserRoleName = (user) => user.role?.roleName || user.roleName || user.role || '-'

const handlePageChange = (event) => {
  // PrimeVue Paginator event: event.page is 0-based
  emit('pageChange', event.page + 1)
}

const onEdit = (user) => emit('edit', user)
const onResetPassword = (id) => emit('resetPassword', id)
const onStatusChange = (user) => emit('statusChange', user)
const onDelete = (id) => emit('delete', id)
</script>

<template>
  <div class="premium-card rounded-[40px] overflow-hidden p-6 md:p-10 hidden md:block">
    <div class="flex justify-between items-end mb-8">
      <div>
          <h3 class="text-2xl font-extrabold text-slate-900 tracking-tight">操作员矩阵</h3>
          <p class="text-slate-400 text-sm mt-1 font-medium italic">Neural Operators Database</p>
      </div>
    </div>
    
    <DataTable 
      :value="users" 
      :loading="loading"
      :paginator="false"
      :lazy="true"
      dataKey="id"
      responsiveLayout="scroll"
      class="neural-table"
      :pt="{
        table: { class: 'w-full text-left' },
        thead: { class: 'border-b border-slate-100' },
        headerrow: { class: 'text-[10px] font-black text-slate-400 uppercase tracking-[0.2em]' },
        headercell: { class: 'px-6 py-4 bg-transparent border-0' },
        bodyrow: { class: 'hover:bg-slate-50/50 group transition-colors border-b border-slate-50/50' },
        bodycell: { class: 'px-6 py-6 bg-transparent border-0 text-sm font-medium text-slate-700' }
      }"
    >
      <template #empty>
        <div class="p-8 text-center text-slate-400 font-bold">暂无操作员数据，请点击右上角"配置新操作员"添加</div>
      </template>

      <Column field="username" header="IDENTITY">
        <template #body="{ data }">
          <div class="flex items-center gap-4">
            <div class="size-10 rounded-xl bg-indigo-50 flex items-center justify-center text-indigo-600 font-black shadow-inner">
              {{ data.username.charAt(0).toUpperCase() }}
            </div>
            <div class="flex flex-col">
                <span class="text-slate-900 font-bold text-base">{{ data.username }}</span>
                <span class="font-mono text-[10px] text-slate-400 font-bold mt-0.5">UID-{{ data.id }}</span>
            </div>
          </div>
        </template>
      </Column>

      <Column field="role" header="ACCESS LEVEL">
        <template #body="{ data }">
          <span class="inline-flex items-center px-3 py-1 rounded-lg bg-slate-100 text-slate-600 font-bold text-xs border border-slate-200 shadow-sm">
              {{ getUserRoleName(data) }}
          </span>
        </template>
      </Column>

      <Column field="status" header="NODE STATUS">
        <template #body="{ data }">
          <span class="flex items-center font-bold text-xs" :class="data.status === 1 ? 'text-emerald-600' : 'text-rose-500'">
              <span class="size-1.5 rounded-full mr-2" :class="data.status === 1 ? 'bg-emerald-500' : 'bg-rose-500'"></span>
              {{ data.status === 1 ? 'ACTIVE' : 'SUSPENDED' }}
          </span>
        </template>
      </Column>

      <Column field="createTime" header="TIMESTAMP">
        <template #body="{ data }">
          <span class="font-bold text-slate-500 text-xs">{{ data.createTime || '-' }}</span>
        </template>
      </Column>

      <Column header="ACTION" alignFrozen="right" :exportable="false" style="min-width:12rem">
        <template #body="{ data }">
          <div class="flex gap-2 justify-end pr-4">
            <button @click="onEdit(data)" class="size-10 rounded-xl bg-slate-50 text-slate-400 hover:bg-indigo-600 hover:text-white transition-all flex items-center justify-center shadow-sm hover:shadow-md hover:shadow-indigo-200" title="编辑属性">
              <Edit class="w-4 h-4" />
            </button>
            <button @click="onResetPassword(data.id)" class="size-10 rounded-xl bg-slate-50 text-slate-400 hover:bg-amber-500 hover:text-white transition-all flex items-center justify-center shadow-sm hover:shadow-md hover:shadow-amber-200" title="重置验证密钥">
              <RefreshCw class="w-4 h-4" />
            </button>
            <button @click="onStatusChange(data)" class="size-10 rounded-xl bg-slate-50 text-slate-400 hover:bg-emerald-500 hover:text-white transition-all flex items-center justify-center shadow-sm hover:shadow-md hover:shadow-emerald-200" title="切换挂起状态">
              <Power class="w-4 h-4" />
            </button>
            <button @click="onDelete(data.id)" class="size-10 rounded-xl bg-slate-50 text-slate-400 hover:bg-rose-500 hover:text-white transition-all flex items-center justify-center shadow-sm hover:shadow-md hover:shadow-rose-200" title="移除操作员">
              <Trash2 class="w-4 h-4" />
            </button>
          </div>
        </template>
      </Column>
    </DataTable>

    <!-- Pagination -->
    <div class="mt-8 pt-6 border-t border-slate-100/50 flex justify-between items-center text-sm font-bold text-slate-400">
      <span>共 {{ total }} 位操作员</span>
      <div class="flex gap-4 items-center">
        <button 
          class="px-6 py-2.5 rounded-xl transition-all"
          :class="query.page <= 1 ? 'bg-slate-50 text-slate-300 cursor-not-allowed' : 'bg-slate-100 hover:bg-indigo-50 hover:text-indigo-600 text-slate-500 shadow-sm'"
          :disabled="query.page <= 1" 
          @click="emit('pageChange', query.page - 1)"
        >
          上一页
        </button>
        <span class="text-slate-900 bg-slate-50 px-4 py-1.5 rounded-lg border border-slate-200 shadow-inner">{{ query.page }}</span>
        <button 
          class="px-6 py-2.5 rounded-xl transition-all"
          :class="(query.page * query.size) >= total ? 'bg-slate-50 text-slate-300 cursor-not-allowed' : 'bg-slate-100 hover:bg-indigo-50 hover:text-indigo-600 text-slate-500 shadow-sm'"
          :disabled="(query.page * query.size) >= total" 
          @click="emit('pageChange', query.page + 1)"
        >
          下一页
        </button>
      </div>
    </div>
  </div>

  <!-- Mobile Cards (移动端) -->
  <div class="md:hidden space-y-4 relative z-10">
    <LoadingSkeleton v-if="loading" type="card" :count="3" />
    
    <div 
      v-else
      v-for="user in users" 
      :key="user.id"
      class="premium-card rounded-[32px] p-6"
    >
      <div class="flex items-start justify-between mb-4">
        <div class="flex items-center gap-4">
            <div class="size-12 rounded-2xl bg-indigo-50 flex items-center justify-center text-indigo-600 font-black shadow-inner shrink-0">
              {{ user.username.charAt(0).toUpperCase() }}
            </div>
            <div class="flex flex-col">
              <div class="flex items-center gap-2 mb-1">
                <span class="text-xl font-black text-slate-900 tracking-tight">{{ user.username }}</span>
              </div>
              <div class="font-mono text-[10px] text-slate-400 font-bold uppercase tracking-widest">UID-{{ user.id }}</div>
            </div>
        </div>
      </div>

      <div class="space-y-3 text-sm mb-6 bg-slate-50/50 rounded-[20px] p-4 border border-slate-100">
        <div class="flex items-center justify-between">
          <span class="text-[10px] font-black text-slate-400 uppercase tracking-widest">Access Level</span>
          <span class="inline-flex items-center px-3 py-1 rounded-lg bg-white text-slate-600 font-bold text-xs border border-slate-200 shadow-sm">{{ getUserRoleName(user) }}</span>
        </div>
        <div class="flex items-center justify-between">
          <span class="text-[10px] font-black text-slate-400 uppercase tracking-widest">Status</span>
          <span class="flex items-center font-bold text-xs" :class="user.status === 1 ? 'text-emerald-600' : 'text-rose-500'">
              <span class="size-1.5 rounded-full mr-2" :class="user.status === 1 ? 'bg-emerald-500' : 'bg-rose-500'"></span>
              {{ user.status === 1 ? 'ACTIVE' : 'SUSPENDED' }}
          </span>
        </div>
      </div>

      <div class="grid grid-cols-4 gap-2">
        <button @click="onEdit(user)" class="flex flex-col items-center justify-center py-3 bg-slate-100 rounded-2xl text-slate-500 active:bg-indigo-600 active:text-white transition-colors">
          <Edit class="w-5 h-5 mb-1" />
          <span class="text-[10px] font-bold">属性</span>
        </button>
        <button @click="onResetPassword(user.id)" class="flex flex-col items-center justify-center py-3 bg-slate-100 rounded-2xl text-slate-500 active:bg-amber-500 active:text-white transition-colors">
          <RefreshCw class="w-5 h-5 mb-1" />
          <span class="text-[10px] font-bold">重置</span>
        </button>
        <button @click="onStatusChange(user)" class="flex flex-col items-center justify-center py-3 bg-slate-100 rounded-2xl text-slate-500 active:bg-emerald-500 active:text-white transition-colors">
          <Power class="w-5 h-5 mb-1" />
          <span class="text-[10px] font-bold">状态</span>
        </button>
        <button @click="onDelete(user.id)" class="flex flex-col items-center justify-center py-3 bg-slate-100 rounded-2xl text-slate-500 active:bg-rose-500 active:text-white transition-colors">
          <Trash2 class="w-5 h-5 mb-1" />
          <span class="text-[10px] font-bold">移除</span>
        </button>
      </div>
    </div>

    <!-- Mobile Pagination -->
    <div v-if="total > 0" class="premium-card rounded-full flex justify-between items-center text-sm p-2 mt-6">
      <button 
        :disabled="query.page <= 1" 
        @click="emit('pageChange', query.page - 1)" 
        class="size-10 flex items-center justify-center bg-slate-100 rounded-full disabled:opacity-50 transition-colors active:bg-slate-200"
      >
        <svg class="w-5 h-5 text-slate-600" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 19l-7-7 7-7"></path></svg>
      </button>
      <span class="text-xs font-black text-slate-500 uppercase tracking-widest">Page {{ query.page }} of {{ Math.ceil(total / query.size) }}</span>
      <button 
        :disabled="query.page >= Math.ceil(total / query.size)" 
        @click="emit('pageChange', query.page + 1)" 
        class="size-10 flex items-center justify-center bg-slate-100 rounded-full disabled:opacity-50 transition-colors active:bg-slate-200"
      >
        <svg class="w-5 h-5 text-slate-600" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5l7 7-7 7"></path></svg>
      </button>
    </div>
    
    <div v-if="!loading && users.length === 0" class="text-center py-12 text-slate-400 font-bold">
      暂无网络节点接入
    </div>
  </div>
</template>
