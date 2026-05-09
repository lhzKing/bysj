<script setup>
import { computed } from 'vue'
import BaseButton from '@/shared/components/ui/BaseButton.vue'
import BaseDialog from '@/shared/components/ui/BaseDialog.vue'
import { Cpu } from 'lucide-vue-next'

/**
 * PartEditDialog —— Linear-light 配件创建/编辑对话框。
 *
 * 视觉契约：
 *  - 走 BaseDialog md size；header icon = Cpu；title 根据 editingPart 切换
 *  - 6 个原生 input/select/textarea 字段：partCode（编辑时禁用）/ partName / partType / manufacturer / model / unit / remark
 *  - 必填字段红色 *：partCode / partName / partType
 *  - 表单 control 复用 .part-form__control 32px / 8px 圆角 / 1px hairline / focus 切 lavender
 *  - footer：取消（text）+ 提交（primary，loading 态显示 spinner）
 *  - <640px：BaseDialog 已自动占满全屏
 *
 * 接口：
 *  - v-model:visible 双向显隐 / editingPart Object 或 null / formData reactive / types / manufacturers / saving
 *  - @save 提交时触发，父组件负责调用 createPart/updatePart
 */
const props = defineProps({
  visible: { type: Boolean, default: false },
  editingPart: { type: Object, default: null },
  formData: { type: Object, required: true },
  types: { type: Array, default: () => [] },
  manufacturers: { type: Array, default: () => [] },
  saving: { type: Boolean, default: false }
})

const emit = defineEmits(['update:visible', 'save'])

const localVisible = computed({
  get: () => props.visible,
  set: (val) => emit('update:visible', val)
})

const titleText = computed(() => (props.editingPart ? '编辑配件' : '新建配件'))
const subtitleText = computed(() =>
  props.editingPart
    ? `配件编码 ${props.editingPart.partCode} 不可修改；其余字段可更新。`
    : '填写配件基础信息，partCode 创建后不可修改。'
)

function onCancel() {
  localVisible.value = false
}
function onSave() {
  emit('save')
}
</script>

<template>
  <BaseDialog
    v-model="localVisible"
    :title="titleText"
    :subtitle="subtitleText"
    :icon="Cpu"
    size="md"
    persistent
    data-testid="part-edit-dialog"
  >
    <form class="part-form" data-testid="part-form" @submit.prevent="onSave">
      <div class="part-form__row">
        <label class="part-form__label">
          配件编码
          <span class="part-form__required">*</span>
        </label>
        <input
          v-model="formData.partCode"
          type="text"
          class="part-form__control mono"
          placeholder="例如 SPU-VALVE-001"
          spellcheck="false"
          autocomplete="off"
          :disabled="!!editingPart"
          data-testid="part-form-code"
        />
      </div>

      <div class="part-form__row">
        <label class="part-form__label">
          配件名称
          <span class="part-form__required">*</span>
        </label>
        <input
          v-model="formData.partName"
          type="text"
          class="part-form__control"
          placeholder="例如 工业高压阀门"
          spellcheck="false"
          autocomplete="off"
          data-testid="part-form-name"
        />
      </div>

      <div class="part-form__grid">
        <div class="part-form__row">
          <label class="part-form__label">
            配件类型
            <span class="part-form__required">*</span>
          </label>
          <input
            v-model="formData.partType"
            type="text"
            class="part-form__control"
            list="part-form-type-options"
            placeholder="选择或输入类型"
            data-testid="part-form-type"
          />
          <datalist id="part-form-type-options">
            <option v-for="t in types" :key="t" :value="t" />
          </datalist>
        </div>

        <div class="part-form__row">
          <label class="part-form__label">生产厂商</label>
          <input
            v-model="formData.manufacturer"
            type="text"
            class="part-form__control"
            list="part-form-manufacturer-options"
            placeholder="选择或输入厂商"
            data-testid="part-form-manufacturer"
          />
          <datalist id="part-form-manufacturer-options">
            <option v-for="m in manufacturers" :key="m" :value="m" />
          </datalist>
        </div>
      </div>

      <div class="part-form__grid">
        <div class="part-form__row">
          <label class="part-form__label">规格型号</label>
          <input
            v-model="formData.model"
            type="text"
            class="part-form__control mono"
            placeholder="例如 V-2024001"
            spellcheck="false"
            autocomplete="off"
            data-testid="part-form-model"
          />
        </div>

        <div class="part-form__row">
          <label class="part-form__label">计量单位</label>
          <input
            v-model="formData.unit"
            type="text"
            class="part-form__control"
            placeholder="例如 件 / 台 / 米"
            spellcheck="false"
            autocomplete="off"
            data-testid="part-form-unit"
          />
        </div>
      </div>

      <div class="part-form__row">
        <label class="part-form__label">备注</label>
        <textarea
          v-model="formData.remark"
          class="part-form__control part-form__control--textarea"
          rows="2"
          placeholder="可选，记录工艺、规格变更等信息"
          data-testid="part-form-remark"
        />
      </div>
    </form>

    <template #footer>
      <BaseButton variant="text" size="sm" data-testid="part-form-cancel" @click="onCancel">
        取消
      </BaseButton>
      <BaseButton
        variant="primary"
        size="sm"
        :loading="saving"
        data-testid="part-form-submit"
        @click="onSave"
      >
        {{ editingPart ? '保存' : '创建' }}
      </BaseButton>
    </template>
  </BaseDialog>
</template>

<style scoped>
.part-form {
  display: flex;
  flex-direction: column;
  gap: 14px;
}
.part-form__grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
}
.part-form__row {
  display: flex;
  flex-direction: column;
  gap: 6px;
  min-width: 0;
}
.part-form__label {
  font-size: 12px;
  font-weight: 500;
  color: var(--ink-muted);
}
.part-form__required {
  color: var(--error);
  margin-left: 2px;
}
.part-form__control {
  height: 32px;
  padding: 0 10px;
  border-radius: 8px;
  background: var(--surface-1);
  border: 1px solid var(--hairline);
  font: inherit;
  font-size: 13px;
  color: var(--ink);
  outline: none;
  transition: border-color 0.15s, box-shadow 0.15s;
}
.part-form__control::placeholder {
  color: var(--ink-tertiary);
}
.part-form__control:focus {
  border-color: var(--primary-focus, #5e69d1);
  box-shadow: 0 0 0 3px rgba(94, 106, 210, 0.15);
}
.part-form__control:disabled {
  background: var(--surface-2);
  color: var(--ink-subtle);
  cursor: not-allowed;
}
.part-form__control--textarea {
  height: auto;
  padding: 8px 10px;
  resize: vertical;
  min-height: 60px;
}

.mono {
  font-family: 'JetBrains Mono', ui-monospace, 'SF Mono', Menlo, monospace;
}

@media (max-width: 640px) {
  .part-form__grid {
    grid-template-columns: 1fr;
  }
}
</style>
