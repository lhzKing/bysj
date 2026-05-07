<script setup>
import { computed } from 'vue'
import { Lock, X } from 'lucide-vue-next'

const props = defineProps({
  visible: Boolean,
  role: Object,
  allPermissions: Array,
  selectedPermissions: Array
})
const emit = defineEmits(['update:visible', 'update:selectedPermissions', 'save'])

const innerSelectedPermissions = computed({
  get: () => props.selectedPermissions,
  set: (val) => emit('update:selectedPermissions', val)
})

const localVisible = computed({
  get: () => props.visible,
  set: (val) => emit('update:visible', val)
})

const handleCancel = () => {
  localVisible.value = false
}

const isTraceBusinessPermission = (permission) => {
  return permission?.permCode?.startsWith('trace:') && permission.permCode !== 'trace:view'
}

const ensureViewPermission = (current, viewCode) => {
  const viewPermission = props.allPermissions.find(p => p.permCode === viewCode)
  if (viewPermission && !current.includes(viewPermission.id)) {
    current.push(viewPermission.id)
  }
}

const togglePermission = (permissionId) => {
  const current = [...innerSelectedPermissions.value]
  const index = current.indexOf(permissionId)
  if (index > -1) {
    current.splice(index, 1)
    innerSelectedPermissions.value = current
  } else {
    current.push(permissionId)
    
    // Auto check logic
    const permission = props.allPermissions.find(p => p.id === permissionId)
    if (permission && permission.permCode.endsWith(':manage')) {
      const viewCode = permission.permCode.replace(':manage', ':view')
      ensureViewPermission(current, viewCode)
    } else if (isTraceBusinessPermission(permission)) {
      ensureViewPermission(current, 'trace:view')
    }
    
    innerSelectedPermissions.value = current
  }
}

const handleSave = () => {
  emit('save')
}
</script>

<template>
  <Teleport to="body">
    <Transition name="dialog-fade">
      <div v-if="localVisible" class="fixed inset-0 z-50 flex items-center justify-center p-4">
        <!-- Backdrop -->
        <div 
          class="absolute inset-0 bg-slate-900/40 backdrop-blur-sm"
          @click="handleCancel"
        ></div>
        
        <!-- Dialog -->
        <div class="relative premium-card rounded-[40px] w-full max-w-2xl transform transition-all p-8 max-h-[90vh] overflow-y-auto">
          <!-- Header -->
          <div class="flex items-center justify-between mb-8">
            <h3 class="text-2xl font-black text-slate-900 tracking-tight flex items-center gap-3">
              <Lock class="w-6 h-6 text-emerald-600" />
              配置授权节点 - {{ role?.roleName }}
            </h3>
            <button @click="handleCancel" class="size-10 rounded-full bg-slate-100 hover:bg-slate-200 text-slate-500 flex items-center justify-center transition-colors">
              <X class="w-5 h-5" />
            </button>
          </div>
          
          <!-- Body -->
          <div class="space-y-6">
            <div class="bg-emerald-50 border border-emerald-100 rounded-2xl p-4 flex items-start gap-4 shadow-inner">
              <Lock class="w-6 h-6 text-emerald-600 flex-shrink-0 mt-0.5" />
              <div class="text-sm">
                <p class="font-black text-slate-900 mb-1 tracking-tight">Access Control Matrix</p>
                <p class="text-slate-600 font-medium leading-relaxed">为该拓扑角色分配可访问的节点权限，未授权的节点将在数字层面上被隔离。</p>
              </div>
            </div>

            <div v-if="allPermissions.length === 0" class="text-center py-12 text-slate-400 font-bold bg-slate-50/50 rounded-[32px] border border-slate-200 border-dashed">
              暂无可用权限节点
            </div>
            
            <div v-else class="grid grid-cols-1 sm:grid-cols-2 gap-4">
              <label 
                v-for="permission in allPermissions" 
                :key="permission.id"
                class="flex items-center gap-4 p-4 rounded-[24px] border-2 cursor-pointer transition-all hover:-translate-y-0.5"
                :class="innerSelectedPermissions.includes(permission.id) ? 'bg-emerald-50/80 border-emerald-300 shadow-md shadow-emerald-100' : 'bg-white/60 border-transparent hover:border-slate-200 hover:shadow-sm'"
              >
                <div class="relative flex items-center justify-center">
                  <input 
                    type="checkbox"
                    class="peer sr-only"
                    :checked="innerSelectedPermissions.includes(permission.id)"
                    @change="togglePermission(permission.id)"
                  />
                  <div class="w-6 h-6 border-2 border-slate-300 rounded-lg bg-white peer-checked:bg-emerald-500 peer-checked:border-emerald-500 transition-all shadow-sm"></div>
                  <svg class="absolute w-4 h-4 text-white opacity-0 peer-checked:opacity-100 transition-opacity pointer-events-none" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="3">
                    <path stroke-linecap="round" stroke-linejoin="round" d="M5 13l4 4L19 7"/>
                  </svg>
                </div>
                <div class="flex flex-col">
                  <span class="text-sm font-black text-slate-900">{{ permission.permName }}</span>
                  <span class="text-[10px] font-mono text-slate-500 font-bold tracking-widest mt-0.5">{{ permission.permCode }}</span>
                </div>
              </label>
            </div>
          </div>
          
          <!-- Footer -->
          <div class="mt-10 flex justify-between items-center gap-4">
            <span class="text-[10px] font-black text-emerald-600 uppercase tracking-widest bg-emerald-50 px-4 py-2 rounded-xl border border-emerald-100">
              已授权 {{ innerSelectedPermissions.length }} 个节点
            </span>
            <div class="flex gap-4">
              <button @click="handleCancel" class="px-6 py-3 rounded-xl font-bold text-slate-500 hover:bg-slate-100 transition-colors">取消</button>
              <button @click="handleSave" class="px-8 py-3 rounded-xl font-bold bg-emerald-500 text-white shadow-lg shadow-emerald-200 hover:bg-emerald-600 hover:shadow-xl hover:shadow-emerald-300 transition-all flex items-center active:scale-95">
                部署权限
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
