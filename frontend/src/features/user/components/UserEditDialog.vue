<script setup>
import { computed } from 'vue'
import BaseButton from '@/shared/components/ui/BaseButton.vue'
import BaseDialog from '@/shared/components/ui/BaseDialog.vue'
import StatusPill from '@/shared/components/ui/StatusPill.vue'
import { UserCog } from 'lucide-vue-next'

/**
 * UserEditDialog —— Linear-light 用户创建/编辑对话框。
 *
 * 视觉契约：
 *  - 走 BaseDialog md size；header icon = UserCog；title 根据 editingUser 切换
 *  - 4 字段表单：username（编辑时禁用，3-50 字符）/ password（创建必填，编辑留空表示不改，6-100 字符且含字母数字）/ roleId（必填）/ status（启用/禁用）
 *  - 编辑模式下额外渲染只读 StatusPill 显示当前启停状态（提示走列表行按钮）
 *  - 必填字段红色 *：username / password (创建模式) / roleId
 *  - 表单 control 复用 .user-form__control 32px / 8px 圆角 / 1px hairline / focus 切 lavender
 *  - footer：取消（text）+ 提交（primary，loading 态显示 spinner）
 *  - <640px：BaseDialog 已自动占满全屏
 *
 * 接口：
 *  - v-model:visible 双向显隐 / editingUser Object 或 null / formData reactive / roles / saving
 *  - @save 提交时触发，父组件负责调用 createUser/updateUser
 */
const props = defineProps({
  visible: { type: Boolean, default: false },
  editingUser: { type: Object, default: null },
  formData: { type: Object, required: true },
  roles: { type: Array, default: () => [] },
  saving: { type: Boolean, default: false }
})

const emit = defineEmits(['update:visible', 'save'])

const localVisible = computed({
  get: () => props.visible,
  set: (val) => emit('update:visible', val)
})

const titleText = computed(() => (props.editingUser ? '编辑用户' : '新建用户'))
const subtitleText = computed(() =>
  props.editingUser
    ? `用户名 ${props.editingUser.username} 不可修改；密码留空表示不重置。启停请使用列表行按钮切换。`
    : '创建后用户即可使用用户名 + 密码登录。只能创建优先级低于自己的角色用户。'
)
const editingEnabled = computed(
  () =>
    props.editingUser &&
    (props.editingUser.status === 1 || props.editingUser.status === '1')
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
    :icon="UserCog"
    size="md"
    persistent
    data-testid="user-edit-dialog"
  >
    <form class="user-form" data-testid="user-form" @submit.prevent="onSave">
      <div v-if="editingUser" class="user-form__status-row" data-testid="user-form-status">
        <span class="user-form__status-label">当前状态</span>
        <StatusPill :tone="editingEnabled ? 'success' : 'mute'">
          {{ editingEnabled ? '启用' : '禁用' }}
        </StatusPill>
        <span class="user-form__status-hint">
          状态变更请使用列表行的“启用 / 禁用”按钮，本对话框只编辑账户元数据。
        </span>
      </div>

      <div class="user-form__row">
        <label class="user-form__label">
          用户名
          <span class="user-form__required">*</span>
        </label>
        <input
          v-model="formData.username"
          type="text"
          class="user-form__control"
          placeholder="3-50 字符"
          spellcheck="false"
          autocomplete="off"
          :disabled="!!editingUser"
          data-testid="user-form-username"
        />
      </div>

      <div class="user-form__row">
        <label class="user-form__label">
          密码
          <span v-if="!editingUser" class="user-form__required">*</span>
          <span v-else class="user-form__label-hint">（留空表示不修改）</span>
        </label>
        <input
          v-model="formData.password"
          type="password"
          class="user-form__control mono"
          placeholder="6-100 字符，必须包含字母和数字"
          spellcheck="false"
          autocomplete="new-password"
          data-testid="user-form-password"
        />
      </div>

      <div class="user-form__grid">
        <div class="user-form__row">
          <label class="user-form__label">
            角色
            <span class="user-form__required">*</span>
          </label>
          <select
            v-model="formData.roleId"
            class="user-form__control user-form__control--select"
            data-testid="user-form-role"
          >
            <option value="" disabled>请选择角色</option>
            <option v-for="role in roles" :key="role.id" :value="role.id">
              {{ role.roleName || role.role_name }}{{ role.roleCode || role.role_code ? ` (${role.roleCode || role.role_code})` : '' }}
            </option>
          </select>
        </div>

        <div class="user-form__row">
          <label class="user-form__label">状态</label>
          <select
            v-model.number="formData.status"
            class="user-form__control user-form__control--select"
            data-testid="user-form-status-select"
          >
            <option :value="1">启用</option>
            <option :value="0">禁用</option>
          </select>
        </div>
      </div>
    </form>

    <template #footer>
      <BaseButton variant="text" size="sm" data-testid="user-form-cancel" @click="onCancel">
        取消
      </BaseButton>
      <BaseButton
        variant="primary"
        size="sm"
        :loading="saving"
        data-testid="user-form-submit"
        @click="onSave"
      >
        {{ editingUser ? '保存' : '创建' }}
      </BaseButton>
    </template>
  </BaseDialog>
</template>

<style scoped>
.user-form {
  display: flex;
  flex-direction: column;
  gap: 14px;
}
.user-form__status-row {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px 10px;
  padding: 10px 12px;
  background: var(--surface-2);
  border: 1px solid var(--hairline);
  border-radius: 8px;
}
.user-form__status-label {
  font-size: 11.5px;
  font-weight: 500;
  text-transform: uppercase;
  letter-spacing: 0.04em;
  color: var(--ink-subtle);
}
.user-form__status-hint {
  flex: 1 1 100%;
  font-size: 12px;
  color: var(--ink-tertiary);
  line-height: 1.5;
}
.user-form__grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
}
.user-form__row {
  display: flex;
  flex-direction: column;
  gap: 6px;
  min-width: 0;
}
.user-form__label {
  font-size: 12px;
  font-weight: 500;
  color: var(--ink-muted);
}
.user-form__label-hint {
  margin-left: 4px;
  font-size: 11px;
  font-weight: 400;
  color: var(--ink-tertiary);
}
.user-form__required {
  color: var(--error);
  margin-left: 2px;
}
.user-form__control {
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
.user-form__control::placeholder {
  color: var(--ink-tertiary);
}
.user-form__control:focus {
  border-color: var(--primary-focus, #5e69d1);
  box-shadow: 0 0 0 3px rgba(94, 106, 210, 0.15);
}
.user-form__control:disabled {
  background: var(--surface-2);
  color: var(--ink-subtle);
  cursor: not-allowed;
}
.user-form__control--select {
  appearance: none;
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24' fill='none' stroke='%2371717a' stroke-width='2' stroke-linecap='round' stroke-linejoin='round'%3E%3Cpath d='M6 9l6 6 6-6'/%3E%3C/svg%3E");
  background-repeat: no-repeat;
  background-position: right 8px center;
  background-size: 14px 14px;
  padding-right: 28px;
  cursor: pointer;
}

.mono {
  font-family: 'JetBrains Mono', ui-monospace, 'SF Mono', Menlo, monospace;
}

@media (max-width: 640px) {
  .user-form__grid {
    grid-template-columns: 1fr;
  }
}
</style>
