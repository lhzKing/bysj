<script setup>
import { computed } from 'vue'
import BaseButton from '@/shared/components/ui/BaseButton.vue'
import EmptyState from '@/shared/components/ui/EmptyState.vue'
import LoadingSkeleton from '@/shared/components/ui/LoadingSkeleton.vue'
import StatusPill from '@/shared/components/ui/StatusPill.vue'
import { Edit, KeyRound, Power, PowerOff, Trash2, Users } from 'lucide-vue-next'

/**
 * UserTable —— Linear-light dense 用户表 + 移动端卡片列表 + 多选批量删除工具条 + 启停状态 + 角色 chip。
 *
 * 视觉契约：与 PartTable / TraceList / FlowTask 表同源。
 *  - 32px 高 thead，11.5px uppercase 字号、ink-subtle、letter-spacing 0.04em
 *  - tbody 行 hover surface-2；最右列操作分散排列（编辑 / 重置密码 / 启用 or 禁用 / 删除）
 *  - 选中复选框列：48px 宽，居中，全选放在 thead；superadmin 行不允许选中（disabled）
 *  - 角色走 22px 高 surface-2 圆 pill（含 role_name + role_code 双行 chip）
 *  - 启停状态走 StatusPill（success "启用" / mute "禁用"）
 *  - <640px：表格隐藏，cards UL 显示；卡片头部含 选择 checkbox + 用户名 + 启停 pill
 *  - 空态、加载态分别走 EmptyState / LoadingSkeleton 原子
 *  - 选中 N 条时显示 sticky toolbar（顶部，带 "批量删除 / 取消选择" 两枚按钮）
 *
 * 接口：
 *  - users / loading / total / page / size / hasMore / selected (Array<id>)
 *  - @edit(user) / @delete(user) / @enable(user) / @disable(user) / @reset-password(user) / @page-change(delta) / @create
 *  - @update:selected(Array<id>) / @batch-delete / @clear-selection
 */
const props = defineProps({
  users: { type: Array, default: () => [] },
  loading: { type: Boolean, default: false },
  total: { type: Number, default: 0 },
  page: { type: Number, default: 1 },
  size: { type: Number, default: 10 },
  hasMore: { type: Boolean, default: false },
  selected: { type: Array, default: () => [] }
})

const emit = defineEmits([
  'edit',
  'delete',
  'enable',
  'disable',
  'reset-password',
  'page-change',
  'create',
  'update:selected',
  'batch-delete',
  'clear-selection'
])

const selectedSet = computed(() => new Set(props.selected))

function isSelectable(user) {
  return user?.username !== 'superadmin' && user?.roleCode !== 'SUPER_ADMIN' && user?.role_code !== 'SUPER_ADMIN'
}

const selectableUsers = computed(() => props.users.filter(isSelectable))

const allSelected = computed(
  () =>
    selectableUsers.value.length > 0 &&
    selectableUsers.value.every((u) => selectedSet.value.has(u.id))
)

const someSelected = computed(
  () =>
    selectableUsers.value.some((u) => selectedSet.value.has(u.id)) && !allSelected.value
)

function isEnabled(user) {
  return user?.status === 1 || user?.status === '1'
}

function getRoleName(user) {
  return user?.roleName || user?.role_name || user?.role || '-'
}

function getRoleCode(user) {
  return user?.roleCode || user?.role_code || ''
}

function getCreateTime(user) {
  return user?.createTime || user?.create_time || '-'
}

function toggleRow(user, checked) {
  if (!isSelectable(user)) return
  const next = new Set(props.selected)
  if (checked) next.add(user.id)
  else next.delete(user.id)
  emit('update:selected', [...next])
}

function toggleAll(checked) {
  if (checked) {
    const next = new Set(props.selected)
    selectableUsers.value.forEach((u) => next.add(u.id))
    emit('update:selected', [...next])
  } else {
    const pageIds = new Set(selectableUsers.value.map((u) => u.id))
    emit(
      'update:selected',
      props.selected.filter((id) => !pageIds.has(id))
    )
  }
}

function onEdit(user) {
  emit('edit', user)
}
function onDelete(user) {
  emit('delete', user)
}
function onEnable(user) {
  emit('enable', user)
}
function onDisable(user) {
  emit('disable', user)
}
function onResetPassword(user) {
  emit('reset-password', user)
}
function onPrev() {
  emit('page-change', -1)
}
function onNext() {
  emit('page-change', 1)
}
function onBatchDelete() {
  emit('batch-delete')
}
function onClearSelection() {
  emit('clear-selection')
}
</script>

<template>
  <section class="user-table" data-testid="user-table">
    <!-- Batch toolbar -->
    <div
      v-if="selected.length > 0"
      class="user-table__toolbar"
      data-testid="user-table-toolbar"
    >
      <span class="user-table__toolbar-count">
        已选 <strong>{{ selected.length }}</strong> 位
      </span>
      <div class="user-table__toolbar-actions">
        <BaseButton
          variant="text"
          size="sm"
          data-testid="user-table-toolbar-clear"
          @click="onClearSelection"
        >
          取消选择
        </BaseButton>
        <BaseButton
          variant="danger"
          size="sm"
          data-testid="user-table-toolbar-delete"
          @click="onBatchDelete"
        >
          <template #icon><Trash2 class="user-table__action-icon" /></template>
          批量删除
        </BaseButton>
      </div>
    </div>

    <!-- Loading -->
    <div v-if="loading" class="user-table__loading">
      <LoadingSkeleton v-for="i in 6" :key="i" height="44px" />
    </div>

    <!-- Empty -->
    <div v-else-if="!users.length" class="user-table__empty">
      <EmptyState
        :icon="Users"
        title="暂无用户"
        subtitle="点击右上角『新建用户』创建第一位操作员；列表会按当前账号角色优先级自动过滤。"
        data-testid="user-table-empty"
      >
        <template #actions>
          <BaseButton variant="primary" size="sm" data-testid="user-table-empty-create" @click="emit('create')">
            新建用户
          </BaseButton>
        </template>
      </EmptyState>
    </div>

    <!-- Table -->
    <div v-else>
      <div class="user-table__wrapper">
        <table class="user-table__table">
          <thead>
            <tr>
              <th class="user-table__th user-table__th--check">
                <input
                  type="checkbox"
                  class="user-table__checkbox"
                  data-testid="user-table-check-all"
                  :checked="allSelected"
                  :indeterminate.prop="someSelected"
                  :disabled="!selectableUsers.length"
                  @change="toggleAll($event.target.checked)"
                />
              </th>
              <th class="user-table__th">用户名</th>
              <th class="user-table__th">角色</th>
              <th class="user-table__th">状态</th>
              <th class="user-table__th">创建时间</th>
              <th class="user-table__th user-table__th--actions">操作</th>
            </tr>
          </thead>
          <tbody>
            <tr
              v-for="user in users"
              :key="user.id"
              class="user-table__tr"
              :class="{ 'user-table__tr--selected': selectedSet.has(user.id) }"
              data-testid="user-table-row"
              :data-id="user.id"
            >
              <td class="user-table__td user-table__td--check" @click.stop>
                <input
                  type="checkbox"
                  class="user-table__checkbox"
                  :data-testid="`user-table-check-${user.id}`"
                  :checked="selectedSet.has(user.id)"
                  :disabled="!isSelectable(user)"
                  :title="!isSelectable(user) ? 'superadmin 账号不可批量删除' : ''"
                  @change="toggleRow(user, $event.target.checked)"
                />
              </td>
              <td class="user-table__td user-table__td--user">
                <div class="user-table__user">
                  <span class="user-table__avatar" aria-hidden="true">{{ (user.username || '?').charAt(0).toUpperCase() }}</span>
                  <div class="user-table__user-info">
                    <span class="user-table__user-name">{{ user.username }}</span>
                    <span class="user-table__user-id mono">UID-{{ user.id }}</span>
                  </div>
                </div>
              </td>
              <td class="user-table__td">
                <span class="user-table__role">
                  <span class="user-table__role-name">{{ getRoleName(user) }}</span>
                  <span v-if="getRoleCode(user)" class="user-table__role-code mono">{{ getRoleCode(user) }}</span>
                </span>
              </td>
              <td class="user-table__td">
                <StatusPill :tone="isEnabled(user) ? 'success' : 'mute'">
                  {{ isEnabled(user) ? '启用' : '禁用' }}
                </StatusPill>
              </td>
              <td class="user-table__td user-table__td--muted mono">{{ getCreateTime(user) }}</td>
              <td class="user-table__td user-table__td--actions" @click.stop>
                <button
                  type="button"
                  class="user-table__row-link"
                  data-testid="user-table-row-edit"
                  @click="onEdit(user)"
                >
                  <Edit class="user-table__action-icon" />
                  编辑
                </button>
                <button
                  type="button"
                  class="user-table__row-link"
                  data-testid="user-table-row-reset"
                  @click="onResetPassword(user)"
                >
                  <KeyRound class="user-table__action-icon" />
                  重置密码
                </button>
                <button
                  v-if="isEnabled(user)"
                  type="button"
                  class="user-table__row-link user-table__row-link--warn"
                  data-testid="user-table-row-disable"
                  @click="onDisable(user)"
                >
                  <PowerOff class="user-table__action-icon" />
                  禁用
                </button>
                <button
                  v-else
                  type="button"
                  class="user-table__row-link user-table__row-link--success"
                  data-testid="user-table-row-enable"
                  @click="onEnable(user)"
                >
                  <Power class="user-table__action-icon" />
                  启用
                </button>
                <button
                  type="button"
                  class="user-table__row-link user-table__row-link--danger"
                  data-testid="user-table-row-delete"
                  @click="onDelete(user)"
                >
                  <Trash2 class="user-table__action-icon" />
                  删除
                </button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <!-- Mobile cards -->
      <ul class="user-table__cards">
        <li
          v-for="user in users"
          :key="user.id"
          class="user-table__card"
          :class="{ 'user-table__card--selected': selectedSet.has(user.id) }"
          data-testid="user-table-card"
        >
          <div class="user-table__card-row">
            <label class="user-table__card-check">
              <input
                type="checkbox"
                class="user-table__checkbox"
                :data-testid="`user-table-card-check-${user.id}`"
                :checked="selectedSet.has(user.id)"
                :disabled="!isSelectable(user)"
                @change="toggleRow(user, $event.target.checked)"
              />
              <span class="user-table__avatar" aria-hidden="true">{{ (user.username || '?').charAt(0).toUpperCase() }}</span>
              <span class="user-table__card-name">{{ user.username }}</span>
            </label>
            <StatusPill :tone="isEnabled(user) ? 'success' : 'mute'">
              {{ isEnabled(user) ? '启用' : '禁用' }}
            </StatusPill>
          </div>
          <dl class="user-table__card-grid">
            <div>
              <dt>角色</dt>
              <dd>{{ getRoleName(user) }}</dd>
            </div>
            <div>
              <dt>角色编码</dt>
              <dd class="mono">{{ getRoleCode(user) || '-' }}</dd>
            </div>
            <div>
              <dt>UID</dt>
              <dd class="mono">{{ user.id }}</dd>
            </div>
            <div>
              <dt>创建时间</dt>
              <dd class="mono">{{ getCreateTime(user) }}</dd>
            </div>
          </dl>
          <div class="user-table__card-actions">
            <BaseButton variant="text" size="sm" data-testid="user-table-card-edit" @click="onEdit(user)">
              <template #icon><Edit class="user-table__action-icon" /></template>
              编辑
            </BaseButton>
            <BaseButton variant="text" size="sm" data-testid="user-table-card-reset" @click="onResetPassword(user)">
              <template #icon><KeyRound class="user-table__action-icon" /></template>
              重置密码
            </BaseButton>
            <BaseButton
              v-if="isEnabled(user)"
              variant="text"
              size="sm"
              data-testid="user-table-card-disable"
              @click="onDisable(user)"
            >
              <template #icon><PowerOff class="user-table__action-icon" /></template>
              禁用
            </BaseButton>
            <BaseButton
              v-else
              variant="text"
              size="sm"
              data-testid="user-table-card-enable"
              @click="onEnable(user)"
            >
              <template #icon><Power class="user-table__action-icon" /></template>
              启用
            </BaseButton>
            <BaseButton variant="text" size="sm" data-testid="user-table-card-delete" @click="onDelete(user)">
              <template #icon><Trash2 class="user-table__action-icon" /></template>
              删除
            </BaseButton>
          </div>
        </li>
      </ul>
    </div>

    <!-- Pagination -->
    <footer v-if="!loading && users.length" class="user-table__pagination">
      <div class="user-table__page-summary">共 {{ total.toLocaleString() }} 位 · 第 {{ page }} 页</div>
      <div class="user-table__page-buttons">
        <button
          type="button"
          class="user-table__page-btn"
          :class="{ 'user-table__page-btn--disabled': page <= 1 }"
          :disabled="page <= 1"
          data-testid="user-table-page-prev"
          @click="onPrev"
        >
          上一页
        </button>
        <span class="user-table__page-current">{{ page }}</span>
        <button
          type="button"
          class="user-table__page-btn"
          :class="{ 'user-table__page-btn--disabled': !hasMore }"
          :disabled="!hasMore"
          data-testid="user-table-page-next"
          @click="onNext"
        >
          下一页
        </button>
      </div>
    </footer>
  </section>
</template>

<style scoped>
.user-table {
  background: var(--surface-1);
  border: 1px solid var(--hairline);
  border-radius: 12px;
  overflow: hidden;
}

.user-table__toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 10px 16px;
  background: var(--primary-soft);
  border-bottom: 1px solid var(--hairline);
}
.user-table__toolbar-count {
  font-size: 12.5px;
  color: var(--ink);
}
.user-table__toolbar-count strong {
  color: var(--primary);
  font-weight: 600;
  margin: 0 2px;
}
.user-table__toolbar-actions {
  display: flex;
  gap: 6px;
}

.user-table__loading {
  padding: 16px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.user-table__empty {
  padding: 32px 0;
}

.user-table__wrapper {
  width: 100%;
  overflow-x: auto;
}

.user-table__table {
  width: 100%;
  border-collapse: collapse;
  font-size: 13px;
}

.user-table__th {
  text-align: left;
  font-weight: 500;
  color: var(--ink-subtle);
  font-size: 11.5px;
  text-transform: uppercase;
  letter-spacing: 0.04em;
  padding: 10px 14px;
  border-bottom: 1px solid var(--hairline);
  background: transparent;
  white-space: nowrap;
}
.user-table__th--check {
  width: 40px;
  padding-right: 0;
}
.user-table__th--actions {
  width: 280px;
  padding-right: 16px;
  text-align: right;
}

.user-table__tr {
  transition: background 0.15s;
}
.user-table__tr:hover .user-table__td {
  background: var(--surface-2);
}
.user-table__tr--selected .user-table__td {
  background: var(--primary-soft);
}

.user-table__td {
  padding: 10px 14px;
  border-bottom: 1px solid var(--hairline);
  color: var(--ink);
  vertical-align: middle;
  background: var(--surface-1);
  transition: background 0.15s;
}
.user-table__tr:last-child .user-table__td {
  border-bottom: 0;
}
.user-table__td--check {
  width: 40px;
  padding-right: 0;
}
.user-table__td--muted {
  color: var(--ink-subtle);
}
.user-table__td--actions {
  padding-right: 16px;
  text-align: right;
  white-space: nowrap;
}

.user-table__checkbox {
  width: 14px;
  height: 14px;
  cursor: pointer;
  accent-color: var(--primary);
}
.user-table__checkbox:disabled {
  cursor: not-allowed;
  opacity: 0.5;
}

.user-table__user {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
}
.user-table__avatar {
  display: inline-grid;
  place-items: center;
  width: 28px;
  height: 28px;
  border-radius: 8px;
  background: var(--primary-soft);
  color: var(--primary);
  font-weight: 600;
  font-size: 12.5px;
  flex-shrink: 0;
}
.user-table__user-info {
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
}
.user-table__user-name {
  color: var(--ink);
  font-weight: 500;
}
.user-table__user-id {
  color: var(--ink-tertiary);
  font-size: 11.5px;
}

.user-table__role {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  height: 22px;
  padding: 0 8px;
  border-radius: 9999px;
  background: var(--surface-2);
  color: var(--ink-muted);
  font-size: 11.5px;
  font-weight: 500;
  white-space: nowrap;
}
.user-table__role-name {
  color: var(--ink);
}
.user-table__role-code {
  color: var(--ink-tertiary);
  font-size: 10.5px;
}

.user-table__row-link {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  background: transparent;
  border: 0;
  color: var(--primary);
  font-weight: 500;
  font-size: 13px;
  cursor: pointer;
  font-family: inherit;
  padding: 4px 8px;
  border-radius: 6px;
  transition: background 0.15s, color 0.15s;
  margin-left: 4px;
}
.user-table__row-link:first-child {
  margin-left: 0;
}
.user-table__row-link:hover {
  background: var(--primary-soft);
  color: var(--primary-hover);
}
.user-table__row-link--warn {
  color: var(--warn);
}
.user-table__row-link--warn:hover {
  background: var(--warn-soft);
  color: var(--warn);
}
.user-table__row-link--success {
  color: var(--success);
}
.user-table__row-link--success:hover {
  background: var(--success-soft);
  color: var(--success);
}
.user-table__row-link--danger {
  color: var(--error);
}
.user-table__row-link--danger:hover {
  background: var(--error-soft);
  color: var(--error);
}
.user-table__action-icon {
  width: 12px;
  height: 12px;
}

.user-table__cards {
  display: none;
  list-style: none;
  margin: 0;
  padding: 0;
}

.user-table__pagination {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 12px 16px;
  border-top: 1px solid var(--hairline);
  background: var(--surface-1);
}
.user-table__page-summary {
  font-size: 12.5px;
  color: var(--ink-subtle);
}
.user-table__page-buttons {
  display: flex;
  align-items: center;
  gap: 6px;
}
.user-table__page-btn {
  height: 28px;
  padding: 0 10px;
  border-radius: 6px;
  background: transparent;
  border: 1px solid var(--hairline);
  font-size: 12.5px;
  font-weight: 500;
  color: var(--ink-muted);
  cursor: pointer;
  font-family: inherit;
  transition: background 0.15s, color 0.15s, border-color 0.15s;
}
.user-table__page-btn:hover:not(.user-table__page-btn--disabled) {
  background: var(--surface-2);
  color: var(--ink);
  border-color: var(--hairline-strong);
}
.user-table__page-btn--disabled {
  color: var(--ink-tertiary);
  cursor: not-allowed;
}
.user-table__page-current {
  min-width: 28px;
  height: 28px;
  padding: 0 10px;
  display: inline-grid;
  place-items: center;
  border-radius: 6px;
  background: var(--surface-2);
  color: var(--ink);
  font-size: 12.5px;
  font-weight: 600;
}

.mono {
  font-family: 'JetBrains Mono', ui-monospace, 'SF Mono', Menlo, monospace;
}

@media (max-width: 640px) {
  .user-table__wrapper {
    display: none;
  }
  .user-table__cards {
    display: flex;
    flex-direction: column;
  }
  .user-table__card {
    padding: 14px 16px;
    border-bottom: 1px solid var(--hairline);
  }
  .user-table__card:last-child {
    border-bottom: 0;
  }
  .user-table__card--selected {
    background: var(--primary-soft);
  }
  .user-table__card-row {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 8px;
    margin-bottom: 8px;
  }
  .user-table__card-check {
    display: inline-flex;
    align-items: center;
    gap: 8px;
    cursor: pointer;
    user-select: none;
    min-width: 0;
  }
  .user-table__card-name {
    color: var(--ink);
    font-size: 13.5px;
    font-weight: 500;
  }
  .user-table__card-grid {
    display: grid;
    grid-template-columns: 1fr 1fr;
    gap: 8px 12px;
    margin: 0 0 10px;
  }
  .user-table__card-grid > div {
    display: flex;
    flex-direction: column;
    gap: 2px;
  }
  .user-table__card-grid dt {
    font-size: 10.5px;
    text-transform: uppercase;
    letter-spacing: 0.04em;
    color: var(--ink-tertiary);
    margin: 0;
  }
  .user-table__card-grid dd {
    margin: 0;
    color: var(--ink);
    font-size: 12.5px;
  }
  .user-table__card-actions {
    display: flex;
    gap: 4px;
    justify-content: flex-end;
    flex-wrap: wrap;
  }
  .user-table__pagination {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
