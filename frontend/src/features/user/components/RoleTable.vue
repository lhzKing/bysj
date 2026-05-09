<script setup>
import { computed } from 'vue'
import BaseButton from '@/shared/components/ui/BaseButton.vue'
import EmptyState from '@/shared/components/ui/EmptyState.vue'
import LoadingSkeleton from '@/shared/components/ui/LoadingSkeleton.vue'
import StatusPill from '@/shared/components/ui/StatusPill.vue'
import { Edit, Lock, Network, ShieldCheck, Trash2 } from 'lucide-vue-next'

/**
 * RoleTable —— Linear-light dense 角色表 + 移动端卡片列表 + 系统预置保护标签。
 *
 * 视觉契约：与 UserTable / PartTable / TraceList 同源。
 *  - 32px 高 thead，11.5px uppercase 字号、ink-subtle、letter-spacing 0.04em
 *  - tbody 行 hover surface-2；最右列操作分散排列（分配权限 / 编辑 / 删除）
 *  - 角色名走 28px 圆角 lavender icon + 双行（角色名 + 描述截断）
 *  - 编码走 22px 高 surface-2 mono 圆 pill
 *  - 授权节点列：lucide Lock + N 项 mono；点击可触发权限分配
 *  - 系统预置 / 当前用户无权限管理的角色：行尾 StatusPill primary "Protected" + 操作按钮按 __guard 隐藏
 *  - <640px：表格隐藏，cards UL 显示；卡片头部含 角色名 + Protected pill；底部 3-cell 操作行
 *
 * 接口：
 *  - roles (Array<{ id, roleCode, roleName, remark, permissionCount, __guard?: { canEdit, canAssignPermissions, canDelete, isProtected } }>)
 *  - loading / total / matched
 *  - @edit(role) / @delete(role) / @assign-permissions(role) / @create
 */
const props = defineProps({
  roles: { type: Array, default: () => [] },
  loading: { type: Boolean, default: false },
  total: { type: Number, default: 0 },
  matched: { type: Number, default: 0 }
})

const emit = defineEmits(['edit', 'delete', 'assign-permissions', 'create'])

function getGuard(role) {
  return (
    role?.__guard ?? {
      canEdit: true,
      canAssignPermissions: true,
      canDelete: true,
      isProtected: false
    }
  )
}

function getPermissionCount(role) {
  return role?.permissionCount ?? 0
}

function isSystemRole(role) {
  return role?.roleCode === 'SUPER_ADMIN' || role?.roleCode === 'ADMIN'
}

const visibleRoles = computed(() => props.roles)

function onEdit(role) {
  emit('edit', role)
}
function onDelete(role) {
  emit('delete', role)
}
function onAssignPermissions(role) {
  emit('assign-permissions', role)
}
function onCreate() {
  emit('create')
}
</script>

<template>
  <section class="role-table" data-testid="role-table">
    <!-- Loading -->
    <div v-if="loading && !roles.length" class="role-table__loading">
      <LoadingSkeleton v-for="i in 5" :key="i" height="44px" />
    </div>

    <!-- Empty -->
    <div v-else-if="!visibleRoles.length" class="role-table__empty">
      <EmptyState
        :icon="Network"
        title="暂无角色"
        subtitle="点击右上角『新建角色』创建第一个角色，再为它分配业务权限。"
        data-testid="role-table-empty"
      >
        <template #actions>
          <BaseButton variant="primary" size="sm" data-testid="role-table-empty-create" @click="onCreate">
            新建角色
          </BaseButton>
        </template>
      </EmptyState>
    </div>

    <!-- Table -->
    <div v-else>
      <div class="role-table__wrapper">
        <table class="role-table__table">
          <thead>
            <tr>
              <th class="role-table__th">角色</th>
              <th class="role-table__th">编码</th>
              <th class="role-table__th">授权节点</th>
              <th class="role-table__th">类型</th>
              <th class="role-table__th role-table__th--actions">操作</th>
            </tr>
          </thead>
          <tbody>
            <tr
              v-for="role in visibleRoles"
              :key="role.id"
              class="role-table__tr"
              data-testid="role-table-row"
              :data-id="role.id"
            >
              <td class="role-table__td role-table__td--lead">
                <div class="role-table__role">
                  <span class="role-table__icon" aria-hidden="true">
                    <ShieldCheck :size="14" />
                  </span>
                  <div class="role-table__role-info">
                    <span class="role-table__role-name">{{ role.roleName }}</span>
                    <span v-if="role.remark" class="role-table__role-remark">{{ role.remark }}</span>
                  </div>
                </div>
              </td>
              <td class="role-table__td">
                <span class="role-table__code mono">{{ role.roleCode }}</span>
              </td>
              <td class="role-table__td">
                <span class="role-table__perm">
                  <Lock class="role-table__perm-icon" />
                  <span class="role-table__perm-count mono">{{ getPermissionCount(role) }}</span>
                  <span class="role-table__perm-label">项</span>
                </span>
              </td>
              <td class="role-table__td">
                <StatusPill v-if="isSystemRole(role)" tone="primary">系统</StatusPill>
                <StatusPill v-else tone="mute">自定义</StatusPill>
              </td>
              <td class="role-table__td role-table__td--actions" @click.stop>
                <button
                  v-if="getGuard(role).canAssignPermissions"
                  type="button"
                  class="role-table__row-link"
                  data-testid="role-table-row-permissions"
                  @click="onAssignPermissions(role)"
                >
                  <Lock class="role-table__action-icon" />
                  分配权限
                </button>
                <button
                  v-if="getGuard(role).canEdit"
                  type="button"
                  class="role-table__row-link"
                  data-testid="role-table-row-edit"
                  @click="onEdit(role)"
                >
                  <Edit class="role-table__action-icon" />
                  编辑
                </button>
                <button
                  v-if="getGuard(role).canDelete"
                  type="button"
                  class="role-table__row-link role-table__row-link--danger"
                  data-testid="role-table-row-delete"
                  @click="onDelete(role)"
                >
                  <Trash2 class="role-table__action-icon" />
                  删除
                </button>
                <span
                  v-if="getGuard(role).isProtected && !getGuard(role).canEdit && !getGuard(role).canAssignPermissions && !getGuard(role).canDelete"
                  class="role-table__protected"
                  data-testid="role-table-row-protected"
                >
                  Protected
                </span>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <!-- Mobile cards -->
      <ul class="role-table__cards">
        <li
          v-for="role in visibleRoles"
          :key="role.id"
          class="role-table__card"
          data-testid="role-table-card"
        >
          <div class="role-table__card-row">
            <div class="role-table__card-lead">
              <span class="role-table__icon" aria-hidden="true">
                <ShieldCheck :size="14" />
              </span>
              <div class="role-table__role-info">
                <span class="role-table__role-name">{{ role.roleName }}</span>
                <span class="role-table__code mono">{{ role.roleCode }}</span>
              </div>
            </div>
            <StatusPill v-if="isSystemRole(role)" tone="primary">系统</StatusPill>
            <StatusPill v-else tone="mute">自定义</StatusPill>
          </div>
          <dl class="role-table__card-grid">
            <div>
              <dt>授权节点</dt>
              <dd class="mono">{{ getPermissionCount(role) }} 项</dd>
            </div>
            <div>
              <dt>描述</dt>
              <dd>{{ role.remark || '-' }}</dd>
            </div>
          </dl>
          <div class="role-table__card-actions">
            <BaseButton
              v-if="getGuard(role).canAssignPermissions"
              variant="text"
              size="sm"
              data-testid="role-table-card-permissions"
              @click="onAssignPermissions(role)"
            >
              <template #icon><Lock class="role-table__action-icon" /></template>
              分配权限
            </BaseButton>
            <BaseButton
              v-if="getGuard(role).canEdit"
              variant="text"
              size="sm"
              data-testid="role-table-card-edit"
              @click="onEdit(role)"
            >
              <template #icon><Edit class="role-table__action-icon" /></template>
              编辑
            </BaseButton>
            <BaseButton
              v-if="getGuard(role).canDelete"
              variant="text"
              size="sm"
              data-testid="role-table-card-delete"
              @click="onDelete(role)"
            >
              <template #icon><Trash2 class="role-table__action-icon" /></template>
              删除
            </BaseButton>
            <span
              v-if="getGuard(role).isProtected && !getGuard(role).canEdit && !getGuard(role).canAssignPermissions && !getGuard(role).canDelete"
              class="role-table__protected"
            >
              Protected
            </span>
          </div>
        </li>
      </ul>
    </div>

    <!-- Footer summary -->
    <footer v-if="!loading && visibleRoles.length" class="role-table__footer">
      <div class="role-table__page-summary">
        共 {{ total.toLocaleString() }} 个角色 · 显示 {{ matched.toLocaleString() }}
      </div>
    </footer>
  </section>
</template>

<style scoped>
.role-table {
  background: var(--surface-1);
  border: 1px solid var(--hairline);
  border-radius: 12px;
  overflow: hidden;
}

.role-table__loading {
  padding: 16px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.role-table__empty {
  padding: 32px 0;
}

.role-table__wrapper {
  width: 100%;
  overflow-x: auto;
}

.role-table__table {
  width: 100%;
  border-collapse: collapse;
  font-size: 13px;
}

.role-table__th {
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
.role-table__th--actions {
  width: 320px;
  padding-right: 16px;
  text-align: right;
}

.role-table__tr {
  transition: background 0.15s;
}
.role-table__tr:hover .role-table__td {
  background: var(--surface-2);
}

.role-table__td {
  padding: 10px 14px;
  border-bottom: 1px solid var(--hairline);
  color: var(--ink);
  vertical-align: middle;
  background: var(--surface-1);
  transition: background 0.15s;
}
.role-table__tr:last-child .role-table__td {
  border-bottom: 0;
}
.role-table__td--actions {
  padding-right: 16px;
  text-align: right;
  white-space: nowrap;
}
.role-table__td--lead {
  min-width: 240px;
}

.role-table__role {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
}
.role-table__icon {
  display: inline-grid;
  place-items: center;
  width: 28px;
  height: 28px;
  border-radius: 8px;
  background: var(--primary-soft);
  color: var(--primary);
  flex-shrink: 0;
}
.role-table__role-info {
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
}
.role-table__role-name {
  color: var(--ink);
  font-weight: 500;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.role-table__role-remark {
  color: var(--ink-tertiary);
  font-size: 11.5px;
  max-width: 280px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.role-table__code {
  display: inline-flex;
  align-items: center;
  height: 22px;
  padding: 0 8px;
  border-radius: 9999px;
  background: var(--surface-2);
  border: 1px solid var(--hairline);
  color: var(--ink-muted);
  font-size: 11.5px;
  letter-spacing: 0.02em;
  white-space: nowrap;
}

.role-table__perm {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  color: var(--ink);
  font-weight: 500;
}
.role-table__perm-icon {
  width: 12px;
  height: 12px;
  color: var(--primary);
}
.role-table__perm-count {
  font-weight: 600;
}
.role-table__perm-label {
  font-size: 11.5px;
  color: var(--ink-subtle);
}

.role-table__row-link {
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
.role-table__row-link:first-child {
  margin-left: 0;
}
.role-table__row-link:hover {
  background: var(--primary-soft);
  color: var(--primary-hover);
}
.role-table__row-link--danger {
  color: var(--error);
}
.role-table__row-link--danger:hover {
  background: var(--error-soft);
  color: var(--error);
}
.role-table__action-icon {
  width: 12px;
  height: 12px;
}

.role-table__protected {
  display: inline-flex;
  align-items: center;
  height: 22px;
  padding: 0 10px;
  border-radius: 9999px;
  background: var(--surface-2);
  border: 1px dashed var(--hairline-strong);
  color: var(--ink-tertiary);
  font-size: 11px;
  font-weight: 500;
  letter-spacing: 0.04em;
  text-transform: uppercase;
}

.role-table__cards {
  display: none;
  list-style: none;
  margin: 0;
  padding: 0;
}

.role-table__footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 12px 16px;
  border-top: 1px solid var(--hairline);
  background: var(--surface-1);
}
.role-table__page-summary {
  font-size: 12.5px;
  color: var(--ink-subtle);
}

.mono {
  font-family: 'JetBrains Mono', ui-monospace, 'SF Mono', Menlo, monospace;
}

@media (max-width: 640px) {
  .role-table__wrapper {
    display: none;
  }
  .role-table__cards {
    display: flex;
    flex-direction: column;
  }
  .role-table__card {
    padding: 14px 16px;
    border-bottom: 1px solid var(--hairline);
  }
  .role-table__card:last-child {
    border-bottom: 0;
  }
  .role-table__card-row {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 8px;
    margin-bottom: 8px;
  }
  .role-table__card-lead {
    display: flex;
    align-items: center;
    gap: 10px;
    min-width: 0;
    flex: 1 1 auto;
  }
  .role-table__card-grid {
    display: grid;
    grid-template-columns: 1fr 1fr;
    gap: 8px 12px;
    margin: 0 0 10px;
  }
  .role-table__card-grid > div {
    display: flex;
    flex-direction: column;
    gap: 2px;
  }
  .role-table__card-grid dt {
    font-size: 10.5px;
    text-transform: uppercase;
    letter-spacing: 0.04em;
    color: var(--ink-tertiary);
    margin: 0;
  }
  .role-table__card-grid dd {
    margin: 0;
    color: var(--ink);
    font-size: 12.5px;
  }
  .role-table__card-actions {
    display: flex;
    gap: 4px;
    justify-content: flex-end;
    flex-wrap: wrap;
  }
  .role-table__footer {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
