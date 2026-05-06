<script setup>
import DataTable from 'primevue/datatable'
import Column from 'primevue/column'
import { Edit, Lock, Settings as Setting, Trash2 as Delete } from 'lucide-vue-next'

defineProps({
  roles: { type: Array, required: true },
  loading: { type: Boolean, default: false }
})

const emit = defineEmits(['edit', 'delete', 'assign-permissions'])

const getRolePermissionCount = (role) => role.permissionCount ?? 0
const getRoleGuard = (role) => role?.__guard ?? {
  canEdit: true,
  canAssignPermissions: true,
  canDelete: true,
  isProtected: false
}
</script>

<template>
  <div>
    <div class="premium-card rounded-[40px] overflow-hidden p-6 md:p-10 hidden md:block">
      <div class="flex justify-between items-end mb-8">
        <div>
          <h3 class="text-2xl font-extrabold text-slate-900 tracking-tight">角色矩阵</h3>
          <p class="text-slate-400 text-sm mt-1 font-medium italic">仅展示当前登录角色可操作的入口。</p>
        </div>
      </div>

      <DataTable
        :value="roles"
        :loading="loading"
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
          <div class="p-8 text-center text-slate-400 font-bold">暂无角色数据</div>
        </template>

        <Column field="roleName" header="角色名称">
          <template #body="{ data }">
            <span class="font-black text-slate-900 text-base">{{ data.roleName }}</span>
          </template>
        </Column>

        <Column field="roleCode" header="角色编码">
          <template #body="{ data }">
            <span class="font-mono text-xs bg-indigo-50 text-indigo-600 px-3 py-1 rounded-lg border border-indigo-100 font-bold tracking-widest">
              {{ data.roleCode }}
            </span>
          </template>
        </Column>

        <Column field="remark" header="描述">
          <template #body="{ data }">
            <span class="text-slate-500 font-medium">{{ data.remark || '-' }}</span>
          </template>
        </Column>

        <Column header="授权节点">
          <template #body="{ data }">
            <div class="flex items-center gap-2 text-emerald-600 font-bold">
              <Lock class="w-4 h-4" />
              <span>{{ getRolePermissionCount(data) }} 项</span>
            </div>
          </template>
        </Column>

        <Column header="操作" alignFrozen="right" :exportable="false" style="min-width:12rem">
          <template #body="{ data }">
            <div class="flex items-center justify-end space-x-3 pr-4">
              <button
                v-if="getRoleGuard(data).canAssignPermissions"
                class="size-10 rounded-xl bg-slate-50 text-slate-400 hover:bg-emerald-500 hover:text-white transition-all flex items-center justify-center shadow-sm hover:shadow-md hover:shadow-emerald-200"
                title="分配权限"
                @click="emit('assign-permissions', data)"
              >
                <Setting class="w-4 h-4" />
              </button>

              <button
                v-if="getRoleGuard(data).canEdit"
                class="size-10 rounded-xl bg-slate-50 text-slate-400 hover:bg-indigo-600 hover:text-white transition-all flex items-center justify-center shadow-sm hover:shadow-md hover:shadow-indigo-200"
                title="编辑角色"
                @click="emit('edit', data)"
              >
                <Edit class="w-4 h-4" />
              </button>

              <button
                v-if="getRoleGuard(data).canDelete"
                class="size-10 rounded-xl bg-slate-50 text-slate-400 hover:bg-rose-500 hover:text-white transition-all flex items-center justify-center shadow-sm hover:shadow-md hover:shadow-rose-200"
                title="删除角色"
                @click="emit('delete', data)"
              >
                <Delete class="w-4 h-4" />
              </button>

              <span
                v-if="!getRoleGuard(data).canAssignPermissions && !getRoleGuard(data).canEdit && !getRoleGuard(data).canDelete"
                class="px-3 py-1 rounded-full bg-slate-100 text-slate-400 text-[10px] font-black uppercase tracking-widest whitespace-nowrap"
              >
                Protected
              </span>
            </div>
          </template>
        </Column>
      </DataTable>
    </div>

    <div v-if="!loading" class="md:hidden space-y-4">
      <div v-if="roles.length === 0" class="text-center py-12 text-slate-400 font-bold">
        暂无角色数据
      </div>

      <div v-for="role in roles" :key="role.id" class="premium-card rounded-[32px] p-6">
        <div class="flex justify-between items-start mb-4">
          <div class="flex-1">
            <h3 class="font-black text-slate-900 text-xl mb-2">{{ role.roleName }}</h3>
            <span class="inline-block font-mono text-[10px] uppercase tracking-widest bg-indigo-50 text-indigo-600 px-3 py-1.5 rounded-lg font-bold border border-indigo-100">
              {{ role.roleCode }}
            </span>
          </div>
        </div>

        <div class="space-y-3 text-sm mb-6 bg-slate-50/50 rounded-[20px] p-4 border border-slate-100">
          <div class="flex items-center justify-between font-bold text-emerald-600">
            <span class="flex items-center gap-2 text-[10px] text-slate-400 uppercase tracking-widest">
              <Lock class="w-4 h-4" />
              授权节点
            </span>
            <span>{{ getRolePermissionCount(role) }}</span>
          </div>
          <div v-if="role.remark" class="text-slate-500 text-xs">
            {{ role.remark }}
          </div>
        </div>

        <div
          v-if="getRoleGuard(role).canAssignPermissions || getRoleGuard(role).canEdit || getRoleGuard(role).canDelete"
          class="grid grid-cols-3 gap-2"
        >
          <button
            v-if="getRoleGuard(role).canAssignPermissions"
            @click="emit('assign-permissions', role)"
            class="flex flex-col items-center justify-center py-3 bg-slate-100 rounded-2xl text-slate-500 active:bg-emerald-500 active:text-white transition-colors"
          >
            <Setting class="w-5 h-5 mb-1" />
            <span class="text-[10px] font-bold">权限</span>
          </button>

          <button
            v-if="getRoleGuard(role).canEdit"
            @click="emit('edit', role)"
            class="flex flex-col items-center justify-center py-3 bg-slate-100 rounded-2xl text-slate-500 active:bg-indigo-600 active:text-white transition-colors"
          >
            <Edit class="w-5 h-5 mb-1" />
            <span class="text-[10px] font-bold">编辑</span>
          </button>

          <button
            v-if="getRoleGuard(role).canDelete"
            @click="emit('delete', role)"
            class="flex flex-col items-center justify-center py-3 bg-slate-100 rounded-2xl text-slate-500 active:bg-rose-500 active:text-white transition-colors"
          >
            <Delete class="w-5 h-5 mb-1" />
            <span class="text-[10px] font-bold">删除</span>
          </button>
        </div>

        <div
          v-else
          class="rounded-2xl border border-dashed border-slate-200 bg-slate-50 px-4 py-3 text-center text-xs font-black uppercase tracking-widest text-slate-400"
        >
          Protected
        </div>
      </div>
    </div>
  </div>
</template>
