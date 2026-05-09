<script setup>
import { computed } from 'vue'
import BaseButton from '@/shared/components/ui/BaseButton.vue'
import EmptyState from '@/shared/components/ui/EmptyState.vue'
import LoadingSkeleton from '@/shared/components/ui/LoadingSkeleton.vue'
import StatusPill from '@/shared/components/ui/StatusPill.vue'
import { Edit, Package, Power, PowerOff, Trash2 } from 'lucide-vue-next'

/**
 * PartTable —— Linear-light dense 配件表 + 移动端卡片列表 + 多选批量删除工具条 + 启停状态。
 *
 * 视觉契约：与 trace-list / flow-task 表同源。
 *  - 32px 高 thead，11.5px uppercase 字号、ink-subtle、letter-spacing 0.04em
 *  - tbody 行 hover surface-2；最右列操作分散排列（编辑 / 启用 or 禁用 / 删除）
 *  - 选中复选框列：48px 宽，居中，全选放在 thead
 *  - 启停状态走 StatusPill（success "启用" / mute "禁用"）
 *  - <640px：表格隐藏，cards UL 显示；卡片头部含 选择 checkbox + 编码 + 启停 pill
 *  - 空态、加载态分别走 EmptyState / LoadingSkeleton 原子
 *  - 选中 N 条时显示 sticky toolbar（顶部，带"批量删除 / 取消选择"两枚按钮）
 *
 * 接口：
 *  - parts / loading / total / page / size / hasMore / selected (Array<id>)
 *  - @edit(part) / @delete(part) / @enable(part) / @disable(part) / @page-change(delta) / @create
 *  - @update:selected(Array<id>) / @batch-delete / @clear-selection
 */
const props = defineProps({
  parts: { type: Array, default: () => [] },
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
  'page-change',
  'create',
  'update:selected',
  'batch-delete',
  'clear-selection'
])

const selectedSet = computed(() => new Set(props.selected))

const allSelected = computed(
  () => props.parts.length > 0 && props.parts.every((p) => selectedSet.value.has(p.id))
)

const someSelected = computed(
  () => props.parts.some((p) => selectedSet.value.has(p.id)) && !allSelected.value
)

function isEnabled(part) {
  return part?.enabled !== false
}

function toggleRow(part, checked) {
  const next = new Set(props.selected)
  if (checked) next.add(part.id)
  else next.delete(part.id)
  emit('update:selected', [...next])
}

function toggleAll(checked) {
  if (checked) {
    const next = new Set(props.selected)
    props.parts.forEach((p) => next.add(p.id))
    emit('update:selected', [...next])
  } else {
    const pageIds = new Set(props.parts.map((p) => p.id))
    emit(
      'update:selected',
      props.selected.filter((id) => !pageIds.has(id))
    )
  }
}

function onEdit(part) {
  emit('edit', part)
}
function onDelete(part) {
  emit('delete', part)
}
function onEnable(part) {
  emit('enable', part)
}
function onDisable(part) {
  emit('disable', part)
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
  <section class="part-table" data-testid="part-table">
    <!-- Batch toolbar -->
    <div
      v-if="selected.length > 0"
      class="part-table__toolbar"
      data-testid="part-table-toolbar"
    >
      <span class="part-table__toolbar-count">
        已选 <strong>{{ selected.length }}</strong> 条
      </span>
      <div class="part-table__toolbar-actions">
        <BaseButton
          variant="text"
          size="sm"
          data-testid="part-table-toolbar-clear"
          @click="onClearSelection"
        >
          取消选择
        </BaseButton>
        <BaseButton
          variant="danger"
          size="sm"
          data-testid="part-table-toolbar-delete"
          @click="onBatchDelete"
        >
          <template #icon><Trash2 class="part-table__action-icon" /></template>
          批量删除
        </BaseButton>
      </div>
    </div>

    <!-- Loading -->
    <div v-if="loading" class="part-table__loading">
      <LoadingSkeleton v-for="i in 6" :key="i" height="44px" />
    </div>

    <!-- Empty -->
    <div v-else-if="!parts.length" class="part-table__empty">
      <EmptyState
        :icon="Package"
        title="暂无配件"
        subtitle="点击右上角“新建配件”创建第一条 SPU 元数据。"
        data-testid="part-table-empty"
      >
        <template #actions>
          <BaseButton variant="primary" size="sm" data-testid="part-table-empty-create" @click="emit('create')">
            新建配件
          </BaseButton>
        </template>
      </EmptyState>
    </div>

    <!-- Table -->
    <div v-else>
      <div class="part-table__wrapper">
        <table class="part-table__table">
          <thead>
            <tr>
              <th class="part-table__th part-table__th--check">
                <input
                  type="checkbox"
                  class="part-table__checkbox"
                  data-testid="part-table-check-all"
                  :checked="allSelected"
                  :indeterminate.prop="someSelected"
                  @change="toggleAll($event.target.checked)"
                />
              </th>
              <th class="part-table__th">配件编码</th>
              <th class="part-table__th">名称 / 型号</th>
              <th class="part-table__th">类型</th>
              <th class="part-table__th">厂商</th>
              <th class="part-table__th">单位</th>
              <th class="part-table__th">状态</th>
              <th class="part-table__th part-table__th--actions">操作</th>
            </tr>
          </thead>
          <tbody>
            <tr
              v-for="part in parts"
              :key="part.id"
              class="part-table__tr"
              :class="{ 'part-table__tr--selected': selectedSet.has(part.id) }"
              data-testid="part-table-row"
              :data-id="part.id"
            >
              <td class="part-table__td part-table__td--check" @click.stop>
                <input
                  type="checkbox"
                  class="part-table__checkbox"
                  :data-testid="`part-table-check-${part.id}`"
                  :checked="selectedSet.has(part.id)"
                  @change="toggleRow(part, $event.target.checked)"
                />
              </td>
              <td class="part-table__td part-table__td--code mono">{{ part.partCode }}</td>
              <td class="part-table__td">
                <div class="part-table__name">
                  <span class="part-table__name-main">{{ part.partName }}</span>
                  <span v-if="part.model" class="part-table__name-model mono">{{ part.model }}</span>
                </div>
              </td>
              <td class="part-table__td">
                <span v-if="part.partType" class="part-table__type">{{ part.partType }}</span>
                <span v-else class="part-table__td--muted">-</span>
              </td>
              <td class="part-table__td">{{ part.manufacturer || '-' }}</td>
              <td class="part-table__td part-table__td--muted">{{ part.unit || '-' }}</td>
              <td class="part-table__td">
                <StatusPill :tone="isEnabled(part) ? 'success' : 'mute'">
                  {{ isEnabled(part) ? '启用' : '禁用' }}
                </StatusPill>
              </td>
              <td class="part-table__td part-table__td--actions" @click.stop>
                <button
                  type="button"
                  class="part-table__row-link"
                  data-testid="part-table-row-edit"
                  @click="onEdit(part)"
                >
                  <Edit class="part-table__action-icon" />
                  编辑
                </button>
                <button
                  v-if="isEnabled(part)"
                  type="button"
                  class="part-table__row-link part-table__row-link--warn"
                  data-testid="part-table-row-disable"
                  @click="onDisable(part)"
                >
                  <PowerOff class="part-table__action-icon" />
                  禁用
                </button>
                <button
                  v-else
                  type="button"
                  class="part-table__row-link part-table__row-link--success"
                  data-testid="part-table-row-enable"
                  @click="onEnable(part)"
                >
                  <Power class="part-table__action-icon" />
                  启用
                </button>
                <button
                  type="button"
                  class="part-table__row-link part-table__row-link--danger"
                  data-testid="part-table-row-delete"
                  @click="onDelete(part)"
                >
                  <Trash2 class="part-table__action-icon" />
                  删除
                </button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <!-- Mobile cards -->
      <ul class="part-table__cards">
        <li
          v-for="part in parts"
          :key="part.id"
          class="part-table__card"
          :class="{ 'part-table__card--selected': selectedSet.has(part.id) }"
          data-testid="part-table-card"
        >
          <div class="part-table__card-row">
            <label class="part-table__card-check">
              <input
                type="checkbox"
                class="part-table__checkbox"
                :data-testid="`part-table-card-check-${part.id}`"
                :checked="selectedSet.has(part.id)"
                @change="toggleRow(part, $event.target.checked)"
              />
              <span class="part-table__card-code mono">{{ part.partCode }}</span>
            </label>
            <StatusPill :tone="isEnabled(part) ? 'success' : 'mute'">
              {{ isEnabled(part) ? '启用' : '禁用' }}
            </StatusPill>
          </div>
          <p class="part-table__card-name">{{ part.partName }}</p>
          <dl class="part-table__card-grid">
            <div>
              <dt>类型</dt>
              <dd>{{ part.partType || '-' }}</dd>
            </div>
            <div>
              <dt>型号</dt>
              <dd class="mono">{{ part.model || '-' }}</dd>
            </div>
            <div>
              <dt>厂商</dt>
              <dd>{{ part.manufacturer || '-' }}</dd>
            </div>
            <div>
              <dt>单位</dt>
              <dd>{{ part.unit || '-' }}</dd>
            </div>
          </dl>
          <div class="part-table__card-actions">
            <BaseButton variant="text" size="sm" data-testid="part-table-card-edit" @click="onEdit(part)">
              <template #icon><Edit class="part-table__action-icon" /></template>
              编辑
            </BaseButton>
            <BaseButton
              v-if="isEnabled(part)"
              variant="text"
              size="sm"
              data-testid="part-table-card-disable"
              @click="onDisable(part)"
            >
              <template #icon><PowerOff class="part-table__action-icon" /></template>
              禁用
            </BaseButton>
            <BaseButton
              v-else
              variant="text"
              size="sm"
              data-testid="part-table-card-enable"
              @click="onEnable(part)"
            >
              <template #icon><Power class="part-table__action-icon" /></template>
              启用
            </BaseButton>
            <BaseButton variant="text" size="sm" data-testid="part-table-card-delete" @click="onDelete(part)">
              <template #icon><Trash2 class="part-table__action-icon" /></template>
              删除
            </BaseButton>
          </div>
        </li>
      </ul>
    </div>

    <!-- Pagination -->
    <footer v-if="!loading && parts.length" class="part-table__pagination">
      <div class="part-table__page-summary">共 {{ total.toLocaleString() }} 条 · 第 {{ page }} 页</div>
      <div class="part-table__page-buttons">
        <button
          type="button"
          class="part-table__page-btn"
          :class="{ 'part-table__page-btn--disabled': page <= 1 }"
          :disabled="page <= 1"
          data-testid="part-table-page-prev"
          @click="onPrev"
        >
          上一页
        </button>
        <span class="part-table__page-current">{{ page }}</span>
        <button
          type="button"
          class="part-table__page-btn"
          :class="{ 'part-table__page-btn--disabled': !hasMore }"
          :disabled="!hasMore"
          data-testid="part-table-page-next"
          @click="onNext"
        >
          下一页
        </button>
      </div>
    </footer>
  </section>
</template>

<style scoped>
.part-table {
  background: var(--surface-1);
  border: 1px solid var(--hairline);
  border-radius: 12px;
  overflow: hidden;
}

.part-table__toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 10px 16px;
  background: var(--primary-soft);
  border-bottom: 1px solid var(--hairline);
}
.part-table__toolbar-count {
  font-size: 12.5px;
  color: var(--ink);
}
.part-table__toolbar-count strong {
  color: var(--primary);
  font-weight: 600;
  margin: 0 2px;
}
.part-table__toolbar-actions {
  display: flex;
  gap: 6px;
}

.part-table__loading {
  padding: 16px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.part-table__empty {
  padding: 32px 0;
}

.part-table__wrapper {
  width: 100%;
  overflow-x: auto;
}

.part-table__table {
  width: 100%;
  border-collapse: collapse;
  font-size: 13px;
}

.part-table__th {
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
.part-table__th--check {
  width: 40px;
  padding-right: 0;
}
.part-table__th--actions {
  width: 220px;
  padding-right: 16px;
  text-align: right;
}

.part-table__tr {
  transition: background 0.15s;
}
.part-table__tr:hover .part-table__td {
  background: var(--surface-2);
}
.part-table__tr--selected .part-table__td {
  background: var(--primary-soft);
}

.part-table__td {
  padding: 10px 14px;
  border-bottom: 1px solid var(--hairline);
  color: var(--ink);
  vertical-align: middle;
  background: var(--surface-1);
  transition: background 0.15s;
}
.part-table__tr:last-child .part-table__td {
  border-bottom: 0;
}
.part-table__td--check {
  width: 40px;
  padding-right: 0;
}
.part-table__td--code {
  color: var(--ink);
  white-space: nowrap;
}
.part-table__td--muted {
  color: var(--ink-subtle);
}
.part-table__td--actions {
  padding-right: 16px;
  text-align: right;
  white-space: nowrap;
}

.part-table__checkbox {
  width: 14px;
  height: 14px;
  cursor: pointer;
  accent-color: var(--primary);
}

.part-table__name {
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
}
.part-table__name-main {
  color: var(--ink);
}
.part-table__name-model {
  color: var(--ink-tertiary);
  font-size: 11.5px;
}

.part-table__type {
  display: inline-flex;
  align-items: center;
  height: 22px;
  padding: 0 8px;
  border-radius: 9999px;
  background: var(--surface-2);
  color: var(--ink-muted);
  font-size: 11.5px;
  font-weight: 500;
  white-space: nowrap;
}

.part-table__row-link {
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
.part-table__row-link:first-child {
  margin-left: 0;
}
.part-table__row-link:hover {
  background: var(--primary-soft);
  color: var(--primary-hover);
}
.part-table__row-link--warn {
  color: var(--warn);
}
.part-table__row-link--warn:hover {
  background: var(--warn-soft);
  color: var(--warn);
}
.part-table__row-link--success {
  color: var(--success);
}
.part-table__row-link--success:hover {
  background: var(--success-soft);
  color: var(--success);
}
.part-table__row-link--danger {
  color: var(--error);
}
.part-table__row-link--danger:hover {
  background: var(--error-soft);
  color: var(--error);
}
.part-table__action-icon {
  width: 12px;
  height: 12px;
}

.part-table__cards {
  display: none;
  list-style: none;
  margin: 0;
  padding: 0;
}

.part-table__pagination {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 12px 16px;
  border-top: 1px solid var(--hairline);
  background: var(--surface-1);
}
.part-table__page-summary {
  font-size: 12.5px;
  color: var(--ink-subtle);
}
.part-table__page-buttons {
  display: flex;
  align-items: center;
  gap: 6px;
}
.part-table__page-btn {
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
.part-table__page-btn:hover:not(.part-table__page-btn--disabled) {
  background: var(--surface-2);
  color: var(--ink);
  border-color: var(--hairline-strong);
}
.part-table__page-btn--disabled {
  color: var(--ink-tertiary);
  cursor: not-allowed;
}
.part-table__page-current {
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
  .part-table__wrapper {
    display: none;
  }
  .part-table__cards {
    display: flex;
    flex-direction: column;
  }
  .part-table__card {
    padding: 14px 16px;
    border-bottom: 1px solid var(--hairline);
  }
  .part-table__card:last-child {
    border-bottom: 0;
  }
  .part-table__card--selected {
    background: var(--primary-soft);
  }
  .part-table__card-row {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 8px;
    margin-bottom: 6px;
  }
  .part-table__card-check {
    display: inline-flex;
    align-items: center;
    gap: 8px;
    cursor: pointer;
    user-select: none;
  }
  .part-table__card-code {
    color: var(--ink);
    font-size: 13px;
  }
  .part-table__card-name {
    margin: 0 0 8px;
    color: var(--ink);
    font-size: 13.5px;
    font-weight: 500;
  }
  .part-table__card-grid {
    display: grid;
    grid-template-columns: 1fr 1fr;
    gap: 8px 12px;
    margin: 0 0 10px;
  }
  .part-table__card-grid > div {
    display: flex;
    flex-direction: column;
    gap: 2px;
  }
  .part-table__card-grid dt {
    font-size: 10.5px;
    text-transform: uppercase;
    letter-spacing: 0.04em;
    color: var(--ink-tertiary);
    margin: 0;
  }
  .part-table__card-grid dd {
    margin: 0;
    color: var(--ink);
    font-size: 12.5px;
  }
  .part-table__card-actions {
    display: flex;
    gap: 4px;
    justify-content: flex-end;
    flex-wrap: wrap;
  }
  .part-table__pagination {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
