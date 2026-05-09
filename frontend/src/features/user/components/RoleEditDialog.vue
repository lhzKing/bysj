<script setup>
import { computed, reactive, watch } from 'vue'
import BaseButton from '@/shared/components/ui/BaseButton.vue'
import BaseDialog from '@/shared/components/ui/BaseDialog.vue'
import { ShieldCheck } from 'lucide-vue-next'

/**
 * RoleEditDialog —— Linear-light 角色创建/编辑对话框。
 *
 * 视觉契约：与 UserEditDialog / PartEditDialog 同源。
 *  - 走 BaseDialog md size；header icon = ShieldCheck；title 根据 editingRole 切换
 *  - 3 字段表单：roleCode（编辑禁用，1-50 字符 mono）/ roleName（必填，1-50 字符）/ remark（textarea）
 *  - 必填字段红色 *：roleCode（创建模式）/ roleName
 *  - 表单 control 复用 .role-form__control 32px / 8px 圆角 / 1px hairline / focus 切 lavender
 *  - footer：取消（text）+ 提交（primary，loading 态显示 spinner）
 *  - <640px：BaseDialog 自动占满全屏
 *
 * 接口：
 *  - v-model:visible 双向显隐 / editingRole Object 或 null / saving Boolean
 *  - @save({ roleCode, roleName, remark }) 提交时触发，父组件负责调用 createRole/updateRole
 */
const props = defineProps({
  visible: { type: Boolean, default: false },
  editingRole: { type: Object, default: null },
  saving: { type: Boolean, default: false }
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

const titleText = computed(() => (props.editingRole ? '编辑角色' : '新建角色'))
const subtitleText = computed(() =>
  props.editingRole
    ? `角色编码 ${props.editingRole.roleCode} 不可修改；权限分配请使用列表行『分配权限』按钮。`
    : '创建后角色默认不带任何业务权限；创建完成可在列表中点击『分配权限』勾选具体节点。'
)

watch(
  () => props.visible,
  (newVal) => {
    if (!newVal) return
    if (props.editingRole) {
      Object.assign(formData, {
        roleCode: props.editingRole.roleCode || '',
        roleName: props.editingRole.roleName || '',
        remark: props.editingRole.remark || ''
      })
    } else {
      Object.assign(formData, { roleCode: '', roleName: '', remark: '' })
    }
  }
)

function onCancel() {
  localVisible.value = false
}

function onSave() {
  emit('save', {
    roleCode: formData.roleCode.trim(),
    roleName: formData.roleName.trim(),
    remark: formData.remark.trim()
  })
}
</script>

<template>
  <BaseDialog
    v-model="localVisible"
    :title="titleText"
    :subtitle="subtitleText"
    :icon="ShieldCheck"
    size="md"
    persistent
    data-testid="role-edit-dialog"
  >
    <form class="role-form" data-testid="role-form" @submit.prevent="onSave">
      <div class="role-form__row">
        <label class="role-form__label">
          角色编码
          <span v-if="!editingRole" class="role-form__required">*</span>
          <span v-else class="role-form__label-hint">（不可修改）</span>
        </label>
        <input
          v-model="formData.roleCode"
          type="text"
          class="role-form__control mono"
          placeholder="如 INSPECTOR / WAREHOUSE / CUSTOM_ROLE"
          spellcheck="false"
          autocomplete="off"
          :disabled="!!editingRole"
          data-testid="role-form-code"
        />
      </div>

      <div class="role-form__row">
        <label class="role-form__label">
          角色名称
          <span class="role-form__required">*</span>
        </label>
        <input
          v-model="formData.roleName"
          type="text"
          class="role-form__control"
          placeholder="例如：质检员 / 出入库员"
          spellcheck="false"
          autocomplete="off"
          data-testid="role-form-name"
        />
      </div>

      <div class="role-form__row">
        <label class="role-form__label">描述</label>
        <textarea
          v-model="formData.remark"
          class="role-form__control role-form__control--textarea"
          rows="3"
          placeholder="选填，用于说明该角色的业务范围与边界"
          data-testid="role-form-remark"
        />
      </div>
    </form>

    <template #footer>
      <BaseButton variant="text" size="sm" data-testid="role-form-cancel" @click="onCancel">
        取消
      </BaseButton>
      <BaseButton
        variant="primary"
        size="sm"
        :loading="saving"
        data-testid="role-form-submit"
        @click="onSave"
      >
        {{ editingRole ? '保存' : '创建' }}
      </BaseButton>
    </template>
  </BaseDialog>
</template>

<style scoped>
.role-form {
  display: flex;
  flex-direction: column;
  gap: 14px;
}
.role-form__row {
  display: flex;
  flex-direction: column;
  gap: 6px;
  min-width: 0;
}
.role-form__label {
  font-size: 12px;
  font-weight: 500;
  color: var(--ink-muted);
}
.role-form__label-hint {
  margin-left: 4px;
  font-size: 11px;
  font-weight: 400;
  color: var(--ink-tertiary);
}
.role-form__required {
  color: var(--error);
  margin-left: 2px;
}
.role-form__control {
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
.role-form__control::placeholder {
  color: var(--ink-tertiary);
}
.role-form__control:focus {
  border-color: var(--primary-focus, #5e69d1);
  box-shadow: 0 0 0 3px rgba(94, 106, 210, 0.15);
}
.role-form__control:disabled {
  background: var(--surface-2);
  color: var(--ink-subtle);
  cursor: not-allowed;
}
.role-form__control--textarea {
  height: auto;
  min-height: 76px;
  padding: 8px 10px;
  resize: vertical;
  line-height: 1.5;
}

.mono {
  font-family: 'JetBrains Mono', ui-monospace, 'SF Mono', Menlo, monospace;
}
</style>
