<script setup>
import { computed, watch } from 'vue'
import BaseInput from '@/shared/components/ui/BaseInput.vue'
import { REGIONS, getRegionByNode } from '@/shared/data/regions'

/**
 * BaseFlowForm —— ScanFlowDialog 共享的入库 / 出库 / 流转表单。
 *
 * 视觉契约（Linear-light）：
 *   - 字段栈纵向 14px gap；label 13/500/ink + 6px gap + control（h36）
 *   - input / select / textarea / datetime 全部 8px 圆角 + 1px hairline + var(--surface-1) 背景
 *   - focus 转 var(--primary-focus) 边 + 3px lavender 15% 光晕
 *   - 错误必填 *：var(--error)；不再用 rose-500 / indigo-500 旧色阶
 *
 * 接口：modelValue（form data 对象，由父持有 reactive）+ actionType=inbound|outbound|transfer 决定 label / placeholder / 流转特殊 hint。
 */
const props = defineProps({
  modelValue: { type: Object, required: true },
  actionType: {
    type: String,
    required: true,
    validator: (v) => ['inbound', 'outbound', 'transfer'].includes(v)
  }
})

const emit = defineEmits(['update:modelValue'])

const regions = REGIONS

const formData = computed({
  get: () => props.modelValue,
  set: (value) => emit('update:modelValue', value)
})

const labelsMap = {
  inbound: { fromNode: '来源节点', toNode: '目标仓库', province: '省份', city: '城市', eventTime: '入库时间' },
  outbound: { fromNode: '来源仓库', toNode: '目标节点', province: '省份', city: '城市', eventTime: '出库时间' },
  transfer: { fromNode: '起点', toNode: '终点', province: '当前位置-省份', city: '当前位置-城市', eventTime: '流转时间' }
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

const availableCities = computed(() => {
  const region = regions.find((r) => r.value === formData.value.province)
  return region ? region.cities : []
})

function handleProvinceChange() {
  formData.value.city = ''
}

function handleFromNodeBlur() {
  const region = getRegionByNode(formData.value.fromNode)
  if (region && !formData.value.province) {
    formData.value.province = region.province
    formData.value.city = region.city
  }
}

function handleToNodeBlur() {
  const region = getRegionByNode(formData.value.toNode)
  if (region && !formData.value.province) {
    formData.value.province = region.province
    formData.value.city = region.city
  }
}

watch(
  () => formData.value.province,
  (newProvince) => {
    if (newProvince && formData.value.city) {
      const cities = regions.find((r) => r.value === newProvince)?.cities || []
      if (!cities.includes(formData.value.city)) {
        formData.value.city = ''
      }
    }
  }
)
</script>

<template>
  <div class="flow-form">
    <p class="flow-form__intro">
      下列节点 / 地点字段留空时，后端会按你的节点绑定 + 当前快照自动补齐；填了则以你填的为准。
    </p>

    <BaseInput
      v-model="formData.fromNode"
      :label="labels.fromNode"
      :placeholder="placeholders.fromNode"
      @blur="handleFromNodeBlur"
    />

    <BaseInput
      v-model="formData.toNode"
      :label="labels.toNode"
      :placeholder="placeholders.toNode"
      @blur="handleToNodeBlur"
    />

    <div class="flow-form__row">
      <div class="flow-form__field">
        <label class="flow-form__label">{{ labels.province }}</label>
        <select
          v-model="formData.province"
          class="flow-form__select"
          @change="handleProvinceChange"
        >
          <option value="">请选择省份（可留空，由后端补齐）</option>
          <option v-for="region in regions" :key="region.value" :value="region.value">
            {{ region.label }}
          </option>
        </select>
      </div>

      <div class="flow-form__field">
        <label class="flow-form__label">{{ labels.city }}</label>
        <select
          v-model="formData.city"
          :disabled="!formData.province"
          class="flow-form__select"
        >
          <option value="">请选择城市（可留空，由后端补齐）</option>
          <option v-for="city in availableCities" :key="city" :value="city">
            {{ city }}
          </option>
        </select>
      </div>
    </div>

    <p v-if="actionType === 'transfer'" class="flow-form__hint">
      填写货物当前所在位置（通常是起点或中转站位置）
    </p>

    <div class="flow-form__field">
      <label class="flow-form__label">{{ labels.eventTime }} <span class="flow-form__star">*</span></label>
      <input
        v-model="formData.eventTime"
        type="datetime-local"
        class="flow-form__input flow-form__input--mono"
      />
    </div>

    <div class="flow-form__field">
      <label class="flow-form__label">备注说明</label>
      <textarea
        v-model="formData.remark"
        rows="3"
        maxlength="255"
        class="flow-form__textarea"
        :placeholder="placeholders.remark"
      />
    </div>
  </div>
</template>

<style scoped>
.flow-form {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.flow-form__intro {
  margin: 0 0 -2px;
  padding: 8px 12px;
  font-size: 12px;
  color: var(--ink-muted);
  background: var(--surface-2);
  border-left: 3px solid var(--primary);
  border-radius: 4px;
  line-height: 1.55;
}

.flow-form__row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
}

.flow-form__field {
  display: flex;
  flex-direction: column;
  gap: 6px;
  min-width: 0;
}

.flow-form__label {
  font-size: 13px;
  font-weight: 500;
  color: var(--ink);
  line-height: 1.3;
}

.flow-form__star {
  color: var(--error);
  margin-left: 2px;
}

.flow-form__hint {
  margin: -4px 0 0;
  font-size: 12px;
  color: var(--ink-subtle);
  line-height: 1.5;
}

.flow-form__select,
.flow-form__input {
  height: 36px;
  padding: 0 12px;
  border: 1px solid var(--hairline);
  border-radius: 8px;
  background: var(--surface-1);
  color: var(--ink);
  font-size: 14px;
  font-family: inherit;
  outline: none;
  transition: border-color 0.15s, box-shadow 0.15s;
  appearance: none;
  -webkit-appearance: none;
  width: 100%;
}
.flow-form__select {
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24' fill='none' stroke='%2371717a' stroke-width='2'%3E%3Cpolyline points='6 9 12 15 18 9'/%3E%3C/svg%3E");
  background-repeat: no-repeat;
  background-position: right 10px center;
  background-size: 14px 14px;
  padding-right: 32px;
}
.flow-form__input--mono {
  font-family: 'JetBrains Mono', ui-monospace, monospace;
  font-size: 13px;
}

.flow-form__select:focus,
.flow-form__input:focus {
  border-color: var(--primary-focus);
  box-shadow: 0 0 0 3px var(--primary-ring);
}
.flow-form__select:disabled {
  background-color: var(--surface-2);
  color: var(--ink-tertiary);
  cursor: not-allowed;
}

.flow-form__textarea {
  min-height: 84px;
  padding: 10px 12px;
  border: 1px solid var(--hairline);
  border-radius: 8px;
  background: var(--surface-1);
  color: var(--ink);
  font-size: 14px;
  font-family: inherit;
  outline: none;
  resize: vertical;
  transition: border-color 0.15s, box-shadow 0.15s;
  width: 100%;
  line-height: 1.55;
}
.flow-form__textarea:focus {
  border-color: var(--primary-focus);
  box-shadow: 0 0 0 3px var(--primary-ring);
}

@media (max-width: 639.98px) {
  .flow-form__row {
    grid-template-columns: 1fr;
  }
}
</style>
