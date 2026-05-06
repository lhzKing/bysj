<script setup>
import { reactive, watch, computed } from 'vue'
import BaseInput from '@/shared/components/ui/BaseInput.vue'
import { X, Network } from 'lucide-vue-next'

const props = defineProps({
  visible: Boolean,
  editingRole: Object
})
const emit = defineEmits(['update:visible', 'save'])

const localVisible = computed({
  get: () => props.visible,
  set: (val) => emit('update:visible', val)
})

const formData = reactive({
  roleCode: '',
  roleName: '',
  remark: ''
})

watch(() => props.visible, (newVal) => {
  if (newVal) {
    if (props.editingRole) {
      Object.assign(formData, {
        roleCode: props.editingRole.roleCode,
        roleName: props.editingRole.roleName,
        remark: props.editingRole.remark || ''
      })
    } else {
      Object.assign(formData, {
        roleCode: '',
        roleName: '',
        remark: ''
      })
    }
  }
})

const handleSave = () => {
  emit('save', { ...formData })
}

const handleCancel = () => {
  localVisible.value = false
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
        <div class="relative premium-card rounded-[40px] w-full max-w-md transform transition-all p-8 max-h-[90vh] overflow-y-auto">
          <!-- Header -->
          <div class="flex items-center justify-between mb-8">
            <h3 class="text-2xl font-black text-slate-900 tracking-tight flex items-center gap-3">
              <Network class="w-6 h-6 text-emerald-600" />
              {{ editingRole ? '编辑权限拓扑' : '配置新角色拓扑' }}
            </h3>
            <button @click="handleCancel" class="size-10 rounded-full bg-slate-100 hover:bg-slate-200 text-slate-500 flex items-center justify-center transition-colors">
              <X class="w-5 h-5" />
            </button>
          </div>
          
          <!-- Body -->
          <div class="space-y-6">
            <div>
              <label class="block text-[10px] font-black text-slate-400 uppercase tracking-widest mb-2 ml-1">
                访问代码 / Access Code <span class="text-rose-500">*</span>
              </label>
              <BaseInput 
                v-model="formData.roleCode" 
                placeholder="例如: ROLE_ADMIN"
                :disabled="!!editingRole"
                class="bg-slate-50/50 rounded-2xl border-slate-200 font-mono text-emerald-600 font-bold"
              />
            </div>

            <div>
              <label class="block text-[10px] font-black text-slate-400 uppercase tracking-widest mb-2 ml-1">
                角色标识 / Role Identity <span class="text-rose-500">*</span>
              </label>
              <BaseInput 
                v-model="formData.roleName" 
                placeholder="请输入角色名称"
                class="bg-slate-50/50 rounded-2xl border-slate-200 font-bold text-slate-700"
              />
            </div>

            <div>
              <label class="block text-[10px] font-black text-slate-400 uppercase tracking-widest mb-2 ml-1">
                拓扑描述 / Description
              </label>
              <textarea 
                v-model="formData.remark"
                placeholder="请输入节点访问权限说明..."
                rows="3"
                class="w-full px-4 py-3 bg-slate-50/50 rounded-2xl border border-slate-200 text-sm font-medium text-slate-700 focus:outline-none focus:ring-2 focus:ring-emerald-500 focus:border-transparent transition-shadow resize-none"
              ></textarea>
            </div>
          </div>
          
          <!-- Footer -->
          <div class="mt-10 flex justify-end gap-4">
            <button @click="handleCancel" class="px-6 py-3 rounded-xl font-bold text-slate-500 hover:bg-slate-100 transition-colors">取消</button>
            <button @click="handleSave" class="px-8 py-3 rounded-xl font-bold bg-emerald-500 text-white shadow-lg shadow-emerald-200 hover:bg-emerald-600 hover:shadow-xl hover:shadow-emerald-300 transition-all flex items-center active:scale-95">
              确认配置
            </button>
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
