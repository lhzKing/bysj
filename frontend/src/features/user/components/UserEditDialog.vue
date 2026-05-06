<script setup>
import { ref, reactive, watch, computed } from 'vue'
import BaseInput from '@/shared/components/ui/BaseInput.vue'
import BaseButton from '@/shared/components/ui/BaseButton.vue'
import Select from 'primevue/select'
import { useToast } from '@/shared/composables/useToast'
import { X, User } from 'lucide-vue-next'

const props = defineProps({
  visible: {
    type: Boolean,
    required: true
  },
  editingUser: {
    type: Object,
    default: null
  },
  roles: {
    type: Array,
    required: true
  }
})

const emit = defineEmits(['update:visible', 'save'])

const toast = useToast()

const localVisible = computed({
  get: () => props.visible,
  set: (val) => emit('update:visible', val)
})

const formData = reactive({
  username: '',
  password: '',
  roleId: '',
  status: 1
})

const statusOptions = [
  { label: '启用', value: 1 },
  { label: '禁用', value: 0 }
]

watch(
  () => props.visible,
  (newVal) => {
    if (newVal) {
      if (props.editingUser) {
        Object.assign(formData, {
          username: props.editingUser.username,
          password: '', // 编辑时不填密码
          roleId: props.editingUser.roleId,
          status: props.editingUser.status
        })
      } else {
        Object.assign(formData, {
          username: '',
          password: '',
          roleId: '',
          status: 1
        })
      }
    }
  }
)

const handleSave = () => {
  // 表单验证
  if (!formData.username) {
    toast.error('请输入用户名')
    return
  }
  if (!props.editingUser && !formData.password) {
    toast.error('创建用户时必须输入密码')
    return
  }
  if (!formData.roleId) {
    toast.error('请选择角色')
    return
  }

  // 密码验证（如果填写了密码）
  if (formData.password) {
    if (formData.password.length < 6 || formData.password.length > 100) {
      toast.error('密码长度必须在6-100个字符之间')
      return
    }
    if (!/[a-zA-Z]/.test(formData.password) || !/\d/.test(formData.password)) {
      toast.error('密码必须包含字母和数字')
      return
    }
  }

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
              <User class="w-6 h-6 text-indigo-600" />
              {{ editingUser ? '编辑操作员' : '配置新操作员' }}
            </h3>
            <button @click="handleCancel" class="size-10 rounded-full bg-slate-100 hover:bg-slate-200 text-slate-500 flex items-center justify-center transition-colors">
              <X class="w-5 h-5" />
            </button>
          </div>
          
          <!-- Body -->
          <div class="space-y-6">
            <div>
              <label class="block text-[10px] font-black text-slate-400 uppercase tracking-widest mb-2 ml-1">
                操作员名称 / Identity <span class="text-rose-500">*</span>
              </label>
              <BaseInput 
                v-model="formData.username" 
                placeholder="请输入用户名"
                :disabled="!!editingUser"
                class="bg-slate-50/50 rounded-2xl border-slate-200 font-bold text-slate-700"
              />
            </div>

            <div>
              <label class="block text-[10px] font-black text-slate-400 uppercase tracking-widest mb-2 ml-1">
                安全密钥 / Password <span v-if="!editingUser" class="text-rose-500">*</span>
                <span v-if="editingUser" class="text-indigo-400 text-[10px]">（留空表示不修改）</span>
              </label>
              <BaseInput 
                v-model="formData.password" 
                type="text"
                placeholder="字母和数字，6-100个字符"
                class="bg-slate-50/50 rounded-2xl border-slate-200 font-mono"
              />
            </div>

            <div>
              <label class="block text-[10px] font-black text-slate-400 uppercase tracking-widest mb-2 ml-1">
                权限域 / Access Level <span class="text-rose-500">*</span>
              </label>
              <select 
                v-model="formData.roleId"
                class="w-full px-4 py-3 bg-slate-50/50 rounded-2xl border border-slate-200 text-sm font-bold text-slate-700 focus:outline-none focus:ring-2 focus:ring-indigo-500 transition-shadow appearance-none cursor-pointer"
              >
                <option value="" disabled selected class="bg-white text-slate-900">请选择角色</option>
                <option v-for="role in roles" :key="role.id" :value="role.id" class="bg-white text-slate-900">
                  {{ role.roleName }}
                </option>
              </select>
            </div>

            <div>
              <label class="block text-[10px] font-black text-slate-400 uppercase tracking-widest mb-2 ml-1">
                节点状态 / Status
              </label>
              <select 
                v-model="formData.status"
                class="w-full px-4 py-3 bg-slate-50/50 rounded-2xl border border-slate-200 text-sm font-bold text-slate-700 focus:outline-none focus:ring-2 focus:ring-indigo-500 transition-shadow appearance-none cursor-pointer"
              >
                <option v-for="opt in statusOptions" :key="opt.value" :value="opt.value" class="bg-white text-slate-900">
                  {{ opt.label }}
                </option>
              </select>
            </div>
          </div>
          
          <!-- Footer -->
          <div class="mt-10 flex justify-end gap-4">
            <button @click="handleCancel" class="px-6 py-3 rounded-xl font-bold text-slate-500 hover:bg-slate-100 transition-colors">取消</button>
            <button @click="handleSave" class="px-8 py-3 rounded-xl font-bold bg-indigo-600 text-white shadow-lg shadow-indigo-200 hover:bg-indigo-700 hover:shadow-xl hover:shadow-indigo-300 transition-all flex items-center active:scale-95">
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
