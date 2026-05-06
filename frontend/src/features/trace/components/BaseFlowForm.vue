<template>
  <div class="space-y-4">
    <!-- From Node -->
    <div>
      <label class="block text-[10px] font-black text-slate-400 uppercase tracking-widest mb-2 ml-1">
        {{ labels.fromNode }} <span class="text-rose-500">*</span>
      </label>
      <BaseInput
        v-model="formData.fromNode"
        @blur="handleFromNodeBlur"
        :placeholder="placeholders.fromNode"
        class="bg-slate-50/50 rounded-2xl border-slate-200 font-bold text-slate-700"
      />
    </div>

    <!-- To Node -->
    <div>
      <label class="block text-[10px] font-black text-slate-400 uppercase tracking-widest mb-2 ml-1">
        {{ labels.toNode }} <span class="text-rose-500">*</span>
      </label>
      <BaseInput
        v-model="formData.toNode"
        @blur="handleToNodeBlur"
        :placeholder="placeholders.toNode"
        class="bg-slate-50/50 rounded-2xl border-slate-200 font-bold text-slate-700"
      />
    </div>

    <!-- Province -->
    <div>
      <label class="block text-[10px] font-black text-slate-400 uppercase tracking-widest mb-2 ml-1">
        {{ labels.province }} <span class="text-rose-500">*</span>
      </label>
      <select
        v-model="formData.province"
        @change="handleProvinceChange"
        class="w-full px-4 py-3 bg-slate-50/50 rounded-2xl border border-slate-200 text-sm font-bold text-slate-700 focus:outline-none focus:ring-2 focus:ring-indigo-500 transition-shadow appearance-none"
      >
        <option value="" disabled selected class="bg-white text-slate-900">请选择省份</option>
        <option v-for="region in regions" :key="region.value" :value="region.value" class="bg-white text-slate-900">
          {{ region.label }}
        </option>
      </select>
    </div>

    <!-- City -->
    <div>
      <label class="block text-[10px] font-black text-slate-400 uppercase tracking-widest mb-2 ml-1">
        {{ labels.city }} <span class="text-rose-500">*</span>
      </label>
      <select
        v-model="formData.city"
        :disabled="!formData.province"
        class="w-full px-4 py-3 bg-slate-50/50 rounded-2xl border border-slate-200 text-sm font-bold text-slate-700 focus:outline-none focus:ring-2 focus:ring-indigo-500 transition-shadow appearance-none disabled:opacity-50 disabled:cursor-not-allowed"
      >
        <option value="" disabled selected class="bg-white text-slate-900">请选择城市</option>
        <option v-for="city in availableCities" :key="city" :value="city" class="bg-white text-slate-900">
          {{ city }}
        </option>
      </select>
      <p v-if="actionType === 'transfer'" class="text-[10px] text-indigo-400 font-bold mt-1 ml-1">填写货物当前所在位置（通常是起点或中转站位置）</p>
    </div>

    <!-- Event Time -->
    <div>
      <label class="block text-[10px] font-black text-slate-400 uppercase tracking-widest mb-2 ml-1">
        {{ labels.eventTime }} <span class="text-rose-500">*</span>
      </label>
      <input
        v-model="formData.eventTime"
        type="datetime-local"
        class="w-full px-4 py-3 bg-slate-50/50 rounded-2xl border border-slate-200 text-sm font-bold text-slate-700 focus:outline-none focus:ring-2 focus:ring-indigo-500 transition-shadow font-mono"
      />
    </div>

    <!-- Remark -->
    <div>
      <label class="block text-[10px] font-black text-slate-400 uppercase tracking-widest mb-2 ml-1">
        备注说明
      </label>
      <textarea
        v-model="formData.remark"
        :placeholder="placeholders.remark"
        rows="3"
        class="w-full px-4 py-3 bg-slate-50/50 rounded-2xl border border-slate-200 text-sm font-medium text-slate-700 focus:outline-none focus:ring-2 focus:ring-indigo-500 transition-shadow resize-none"
      ></textarea>
    </div>
  </div>
</template>

<script setup>
import { computed, watch } from 'vue'
import BaseInput from '@/shared/components/ui/BaseInput.vue'
import { REGIONS, getRegionByNode } from '@/shared/data/regions'

const props = defineProps({
  modelValue: {
    type: Object,
    required: true
  },
  actionType: {
    type: String,
    required: true,
    validator: (value) => ['inbound', 'outbound', 'transfer'].includes(value)
  }
})

const emit = defineEmits(['update:modelValue'])

const regions = REGIONS
const formData = computed({
  get: () => props.modelValue,
  set: (value) => emit('update:modelValue', value)
})

const labelsMap = {
  inbound: {
    fromNode: '来源节点',
    toNode: '目标仓库',
    province: '省份',
    city: '城市',
    eventTime: '入库时间'
  },
  outbound: {
    fromNode: '来源仓库',
    toNode: '目标节点',
    province: '省份',
    city: '城市',
    eventTime: '出库时间'
  },
  transfer: {
    fromNode: '起点',
    toNode: '终点',
    province: '当前位置-省份',
    city: '当前位置-城市',
    eventTime: '流转时间'
  }
}

const placeholdersMap = {
  inbound: {
    fromNode: '请输入来源节点，如"供应商A"、"生产线1"',
    toNode: '请输入目标仓库名称，如"北京仓库"、"华东仓储中心"',
    remark: '请输入备注信息（选填）'
  },
  outbound: {
    fromNode: '请输入来源仓库名称，如"北京仓库"、"华南仓储中心"',
    toNode: '请输入目标节点，如"物流中心"、"客户A"、"生产线2"',
    remark: '请输入备注信息（选填）'
  },
  transfer: {
    fromNode: '请输入起点，如"北京物流中心"、"顺丰速运-上海站"',
    toNode: '请输入终点，如"广州物流中心"、"中通-杭州转运站"',
    remark: '请输入备注信息（选填），如"途经南京中转"、"运输状态良好"'
  }
}

const labels = computed(() => labelsMap[props.actionType])
const placeholders = computed(() => placeholdersMap[props.actionType])

// 可选城市列表
const availableCities = computed(() => {
  const selectedRegion = regions.find(r => r.value === formData.value.province)
  return selectedRegion ? selectedRegion.cities : []
})

// 省份改变时清空城市
const handleProvinceChange = () => {
  formData.value.city = ''
}

// 失焦时尝试自动填充地区
const handleFromNodeBlur = () => {
  const region = getRegionByNode(formData.value.fromNode)
  if (region && !formData.value.province) {
    formData.value.province = region.province
    formData.value.city = region.city
  }
}

const handleToNodeBlur = () => {
  const region = getRegionByNode(formData.value.toNode)
  if (region && !formData.value.province) {
    formData.value.province = region.province
    formData.value.city = region.city
  }
}

// 监听province变化
watch(() => formData.value.province, (newProvince) => {
  if (newProvince && formData.value.city) {
    const cities = regions.find(r => r.value === newProvince)?.cities || []
    if (!cities.includes(formData.value.city)) {
      formData.value.city = ''
    }
  }
})
</script>