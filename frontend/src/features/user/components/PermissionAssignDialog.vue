<script setup>
import { computed } from 'vue'
import BaseButton from '@/shared/components/ui/BaseButton.vue'
import BaseDialog from '@/shared/components/ui/BaseDialog.vue'
import EmptyState from '@/shared/components/ui/EmptyState.vue'
import StatusPill from '@/shared/components/ui/StatusPill.vue'
import { Lock, ShieldOff } from 'lucide-vue-next'

/**
 * PermissionAssignDialog —— Linear-light 角色权限分配对话框。
 *
 * 视觉契约：
 *  - 走 BaseDialog lg size；header icon = Lock；title 含目标角色名
 *  - 上方提示条：sticky，左 ink "已选 N / 共 M 项" + 右 toolbar（重置 / 全选）
 *  - 权限按 permCode 前缀分组：trace / part / user / role / system / 其他
 *  - 每组卡片 surface-1 + 1px hairline + 12px 圆角，header 含组名 + 当前组选中数 + 全选按钮
 *  - 每个权限渲染为 .permission-item：原生 checkbox + permName + permCode mono；hover surface-2，已选 primary-soft
 *  - 自动联动：勾选 :manage 自动勾上同模块 :view；勾选 trace:* 业务权限自动勾上 trace:view
 *  - footer：取消 + 保存 primary（loading 态）
 *  - 空 allPermissions：渲染 EmptyState 占位
 *
 * 接口：
 *  - v-model:visible / v-model:selectedPermissions(Array<id>)
 *  - role Object（含 roleName / roleCode）/ allPermissions Array / saving Boolean
 *  - @save 提交
 */
const props = defineProps({
  visible: { type: Boolean, default: false },
  role: { type: Object, default: null },
  allPermissions: { type: Array, default: () => [] },
  selectedPermissions: { type: Array, default: () => [] },
  saving: { type: Boolean, default: false }
})

const emit = defineEmits(['update:visible', 'update:selectedPermissions', 'save'])

const localVisible = computed({
  get: () => props.visible,
  set: (val) => emit('update:visible', val)
})

const innerSelected = computed({
  get: () => props.selectedPermissions,
  set: (val) => emit('update:selectedPermissions', val)
})

const selectedSet = computed(() => new Set(innerSelected.value))

const GROUP_LABEL = {
  trace: { label: '溯源 · Trace', tone: 'primary' },
  part: { label: '配件 · Part', tone: 'mute' },
  user: { label: '用户 · User', tone: 'mute' },
  role: { label: '角色 · Role', tone: 'mute' },
  system: { label: '系统 · System', tone: 'mute' },
  dashboard: { label: '仪表盘 · Dashboard', tone: 'mute' }
}

const groupedPermissions = computed(() => {
  const groups = new Map()
  for (const perm of props.allPermissions) {
    const code = perm?.permCode || perm?.perm_code || ''
    const prefix = code.includes(':') ? code.split(':')[0] : 'other'
    if (!groups.has(prefix)) groups.set(prefix, [])
    groups.get(prefix).push(perm)
  }
  const ordered = []
  for (const key of Object.keys(GROUP_LABEL)) {
    if (groups.has(key)) {
      ordered.push({ key, ...GROUP_LABEL[key], items: groups.get(key) })
      groups.delete(key)
    }
  }
  for (const [key, items] of groups.entries()) {
    ordered.push({ key, label: key, tone: 'mute', items })
  }
  return ordered
})

const totalCount = computed(() => props.allPermissions.length)

const dialogTitle = computed(() => {
  const name = props.role?.roleName || ''
  return name ? `分配权限 · ${name}` : '分配权限'
})

const dialogSubtitle = computed(() => {
  const code = props.role?.roleCode
  if (code) {
    return `角色编码 ${code} · 勾选 :manage 会自动勾上同模块 :view；勾选 trace:* 业务权限会自动勾上 trace:view。`
  }
  return '勾选权限项后点击『保存』提交。'
})

function isTraceBusinessPermission(permission) {
  const code = permission?.permCode || permission?.perm_code || ''
  return code.startsWith('trace:') && code !== 'trace:view'
}

function ensureViewPermission(current, viewCode) {
  const view = props.allPermissions.find(
    (p) => (p.permCode || p.perm_code) === viewCode
  )
  if (view && !current.includes(view.id)) {
    current.push(view.id)
  }
}

function togglePermission(permission) {
  const current = [...innerSelected.value]
  const idx = current.indexOf(permission.id)
  if (idx > -1) {
    current.splice(idx, 1)
  } else {
    current.push(permission.id)
    const code = permission.permCode || permission.perm_code || ''
    if (code.endsWith(':manage')) {
      const viewCode = code.replace(':manage', ':view')
      ensureViewPermission(current, viewCode)
    } else if (isTraceBusinessPermission(permission)) {
      ensureViewPermission(current, 'trace:view')
    }
  }
  innerSelected.value = current
}

function toggleGroup(group, selectAll) {
  const current = new Set(innerSelected.value)
  if (selectAll) {
    for (const perm of group.items) {
      current.add(perm.id)
      const code = perm.permCode || perm.perm_code || ''
      if (code.endsWith(':manage')) {
        const viewCode = code.replace(':manage', ':view')
        const view = props.allPermissions.find(
          (p) => (p.permCode || p.perm_code) === viewCode
        )
        if (view) current.add(view.id)
      }
    }
  } else {
    for (const perm of group.items) current.delete(perm.id)
  }
  innerSelected.value = [...current]
}

function groupSelectedCount(group) {
  return group.items.filter((p) => selectedSet.value.has(p.id)).length
}

function isGroupAllSelected(group) {
  return group.items.length > 0 && group.items.every((p) => selectedSet.value.has(p.id))
}

function selectAll() {
  innerSelected.value = props.allPermissions.map((p) => p.id)
}

function clearAll() {
  innerSelected.value = []
}

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
    :title="dialogTitle"
    :subtitle="dialogSubtitle"
    :icon="Lock"
    size="lg"
    persistent
    data-testid="permission-assign-dialog"
  >
    <div class="permission-toolbar">
      <span class="permission-toolbar__count">
        已选 <strong>{{ innerSelected.length }}</strong> / {{ totalCount }} 项
      </span>
      <div class="permission-toolbar__actions">
        <BaseButton
          variant="text"
          size="sm"
          data-testid="permission-clear-all"
          :disabled="!innerSelected.length"
          @click="clearAll"
        >
          清空
        </BaseButton>
        <BaseButton
          variant="secondary"
          size="sm"
          data-testid="permission-select-all"
          :disabled="!totalCount"
          @click="selectAll"
        >
          全选
        </BaseButton>
      </div>
    </div>

    <div v-if="!totalCount" class="permission-empty">
      <EmptyState
        :icon="ShieldOff"
        title="暂无可分配权限"
        subtitle="当前账号没有可下发的权限节点，或后端尚未注入权限数据。"
        data-testid="permission-empty"
      />
    </div>

    <div v-else class="permission-groups" data-testid="permission-groups">
      <section
        v-for="group in groupedPermissions"
        :key="group.key"
        class="permission-group"
        :data-testid="`permission-group-${group.key}`"
      >
        <header class="permission-group__header">
          <div class="permission-group__lead">
            <h3 class="permission-group__title">{{ group.label }}</h3>
            <StatusPill tone="mute" size="xs" :dot="false">
              {{ groupSelectedCount(group) }}/{{ group.items.length }}
            </StatusPill>
          </div>
          <button
            type="button"
            class="permission-group__toggle"
            :data-testid="`permission-group-toggle-${group.key}`"
            @click="toggleGroup(group, !isGroupAllSelected(group))"
          >
            {{ isGroupAllSelected(group) ? '取消本组' : '勾选本组' }}
          </button>
        </header>
        <div class="permission-group__grid">
          <label
            v-for="permission in group.items"
            :key="permission.id"
            class="permission-item"
            :class="{ 'permission-item--checked': selectedSet.has(permission.id) }"
            :data-testid="`permission-item-${permission.id}`"
          >
            <input
              type="checkbox"
              class="permission-item__checkbox"
              :data-testid="`permission-item-check-${permission.id}`"
              :checked="selectedSet.has(permission.id)"
              @change="togglePermission(permission)"
            />
            <span class="permission-item__body">
              <span class="permission-item__name">{{ permission.permName || permission.perm_name }}</span>
              <span class="permission-item__code mono">{{ permission.permCode || permission.perm_code }}</span>
            </span>
          </label>
        </div>
      </section>
    </div>

    <template #footer>
      <BaseButton variant="text" size="sm" data-testid="permission-cancel" @click="onCancel">
        取消
      </BaseButton>
      <BaseButton
        variant="primary"
        size="sm"
        :loading="saving"
        data-testid="permission-submit"
        @click="onSave"
      >
        保存
      </BaseButton>
    </template>
  </BaseDialog>
</template>

<style scoped>
.permission-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 8px 12px;
  background: var(--surface-2);
  border: 1px solid var(--hairline);
  border-radius: 8px;
  margin-bottom: 14px;
  position: sticky;
  top: 0;
  z-index: 1;
}
.permission-toolbar__count {
  font-size: 12.5px;
  color: var(--ink-muted);
}
.permission-toolbar__count strong {
  color: var(--primary);
  font-weight: 600;
  margin: 0 2px;
}
.permission-toolbar__actions {
  display: flex;
  gap: 6px;
}

.permission-empty {
  padding: 12px 0 24px;
}

.permission-groups {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.permission-group {
  background: var(--surface-1);
  border: 1px solid var(--hairline);
  border-radius: 12px;
  overflow: hidden;
}
.permission-group__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 10px 14px;
  background: var(--surface-1);
  border-bottom: 1px solid var(--hairline);
}
.permission-group__lead {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
}
.permission-group__title {
  margin: 0;
  font-size: 13px;
  font-weight: 600;
  color: var(--ink);
  letter-spacing: -0.1px;
}
.permission-group__toggle {
  background: transparent;
  border: 1px solid var(--hairline);
  color: var(--ink-muted);
  border-radius: 6px;
  height: 26px;
  padding: 0 10px;
  font-size: 12px;
  font-weight: 500;
  cursor: pointer;
  font-family: inherit;
  transition: background 0.15s, color 0.15s, border-color 0.15s;
}
.permission-group__toggle:hover {
  background: var(--surface-2);
  color: var(--ink);
  border-color: var(--hairline-strong);
}

.permission-group__grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
  gap: 0;
}

.permission-item {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  padding: 10px 14px;
  border-top: 1px solid var(--hairline);
  cursor: pointer;
  transition: background 0.15s;
  min-width: 0;
}
.permission-item:hover {
  background: var(--surface-2);
}
.permission-item--checked {
  background: var(--primary-soft);
}
.permission-item--checked:hover {
  background: var(--primary-soft);
}
.permission-item__checkbox {
  width: 14px;
  height: 14px;
  margin-top: 2px;
  cursor: pointer;
  accent-color: var(--primary);
  flex-shrink: 0;
}
.permission-item__body {
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
}
.permission-item__name {
  font-size: 13px;
  font-weight: 500;
  color: var(--ink);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.permission-item__code {
  font-size: 11.5px;
  color: var(--ink-tertiary);
}

.mono {
  font-family: 'JetBrains Mono', ui-monospace, 'SF Mono', Menlo, monospace;
}

@media (max-width: 640px) {
  .permission-group__grid {
    grid-template-columns: 1fr;
  }
}
</style>
