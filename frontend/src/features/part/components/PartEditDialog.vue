<script setup>
import { computed } from 'vue'
import BaseButton from '@/shared/components/ui/BaseButton.vue'
import BaseInput from '@/shared/components/ui/BaseInput.vue'
import { X, Cpu } from 'lucide-vue-next'

const props = defineProps({
  visible: Boolean,
  editingPart: Object,
  formData: Object,
  types: Array,
  manufacturers: Array
})

const emit = defineEmits(['update:visible', 'save'])

const localVisible = computed({
  get: () => props.visible,
  set: (val) => emit('update:visible', val)
})

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
        <div class="relative premium-card rounded-[40px] w-full max-w-lg transform transition-all p-8 max-h-[90vh] overflow-y-auto">
          <!-- Header -->
          <div class="flex items-center justify-between mb-8">
            <h3 class="text-2xl font-black text-slate-900 tracking-tight flex items-center gap-3">
              <Cpu class="w-6 h-6 text-indigo-600" />
              {{ editingPart ? '编辑节点属性' : '注入新节点' }}
            </h3>
            <button @click="handleCancel" class="size-10 rounded-full bg-slate-100 hover:bg-slate-200 text-slate-500 flex items-center justify-center transition-colors">
              <X class="w-5 h-5" />
            </button>
          </div>
          
          <!-- Body -->
          <div class="space-y-6">
            <div>
              <label class="block text-[10px] font-black text-slate-400 uppercase tracking-widest mb-2 ml-1">
                配件代码 / Identifier <span class="text-rose-500">*</span>
              </label>
              <BaseInput 
                v-model="formData.partCode" 
                placeholder="请输入配件代码"
                :disabled="!!editingPart"
                class="bg-slate-50/50 rounded-2xl border-slate-200 font-mono text-indigo-600 font-bold"
              />
            </div>

            <div>
              <label class="block text-[10px] font-black text-slate-400 uppercase tracking-widest mb-2 ml-1">
                配件名称 / Nomenclature <span class="text-rose-500">*</span>
              </label>
              <BaseInput 
                v-model="formData.partName" 
                placeholder="请输入配件名称"
                class="bg-slate-50/50 rounded-2xl border-slate-200 font-bold text-slate-700"
              />
            </div>

            <div>
              <label class="block text-[10px] font-black text-slate-400 uppercase tracking-widest mb-2 ml-1">
                配件类型 / Type <span class="text-rose-500">*</span>
              </label>
              <select 
                v-model="formData.partType"
                class="w-full px-4 py-3 bg-slate-50/50 rounded-2xl border border-slate-200 text-sm font-bold text-slate-700 focus:outline-none focus:ring-2 focus:ring-indigo-500 transition-shadow appearance-none cursor-pointer"
              >
                <option value="" disabled selected class="bg-white text-slate-900">请选择类型</option>
                <option v-for="type in types" :key="type" :value="type" class="bg-white text-slate-900">
                  {{ type }}
                </option>
              </select>
            </div>

            <div>
              <label class="block text-[10px] font-black text-slate-400 uppercase tracking-widest mb-2 ml-1">
                生产商 / Manufacturer
              </label>
              <select 
                v-model="formData.manufacturer"
                class="w-full px-4 py-3 bg-slate-50/50 rounded-2xl border border-slate-200 text-sm font-bold text-slate-700 focus:outline-none focus:ring-2 focus:ring-indigo-500 transition-shadow appearance-none cursor-pointer"
              >
                <option value="" disabled selected class="bg-white text-slate-900">请选择生产商</option>
                <option v-for="manu in manufacturers" :key="manu" :value="manu" class="bg-white text-slate-900">
                  {{ manu }}
                </option>
              </select>
            </div>

            <div>
              <label class="block text-[10px] font-black text-slate-400 uppercase tracking-widest mb-2 ml-1">
                规格型号 / Model
              </label>
              <BaseInput 
                v-model="formData.model" 
                placeholder="请输入规格型号"
                class="bg-slate-50/50 rounded-2xl border-slate-200 font-bold text-slate-700"
              />
            </div>
          </div>
          
          <!-- Footer -->
          <div class="mt-10 flex justify-end gap-4">
            <button @click="handleCancel" class="px-6 py-3 rounded-xl font-bold text-slate-500 hover:bg-slate-100 transition-colors">取消</button>
            <button @click="emit('save')" class="px-8 py-3 rounded-xl font-bold bg-indigo-600 text-white shadow-lg shadow-indigo-200 hover:bg-indigo-700 hover:shadow-xl hover:shadow-indigo-300 transition-all flex items-center active:scale-95">
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
