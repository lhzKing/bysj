<script setup>
import BaseButton from '@/shared/components/ui/BaseButton.vue'
import EmptyState from '@/shared/components/ui/EmptyState.vue'
import LoadingSkeleton from '@/shared/components/ui/LoadingSkeleton.vue'
import { Edit, Package, Trash2 } from 'lucide-vue-next'

/**
 * PartTable —— Linear-light dense 配件表 + 移动端卡片列表。
 *
 * 视觉契约：与 trace-list / flow-task 表同源。
 *  - 32px 高 thead，11.5px uppercase 字号、ink-subtle、letter-spacing 0.04em
 *  - tbody 行 hover surface-2；最右列操作 BaseButton variant="text" 的 size sm
 *  - 编码列 mono、ink；名称列含名称 + 灰副 model；类型列 StatusPill mute
 *  - <640px：表格隐藏，cards UL 显示；每条卡含 编码 mono + 名称 + 类型/厂商 grid + 操作行
 *  - 空态、加载态分别走 EmptyState / LoadingSkeleton 原子
 *
 * 接口：
 *  - parts / loading / total / page / size / hasMore / hasPrev
 *  - @edit(part) / @delete(part) / @page-change(delta) / @create
 */
defineProps({
  parts: { type: Array, default: () => [] },
  loading: { type: Boolean, default: false },
  total: { type: Number, default: 0 },
  page: { type: Number, default: 1 },
  size: { type: Number, default: 10 },
  hasMore: { type: Boolean, default: false }
})

const emit = defineEmits(['edit', 'delete', 'page-change', 'create'])

function onEdit(part) {
  emit('edit', part)
}
function onDelete(part) {
  emit('delete', part)
}
function onPrev() {
  emit('page-change', -1)
}
function onNext() {
  emit('page-change', 1)
}
</script>

<template>
  <section class="part-table" data-testid="part-table">
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
              <th class="part-table__th">配件编码</th>
              <th class="part-table__th">名称 / 型号</th>
              <th class="part-table__th">类型</th>
              <th class="part-table__th">厂商</th>
              <th class="part-table__th">单位</th>
              <th class="part-table__th part-table__th--actions">操作</th>
            </tr>
          </thead>
          <tbody>
            <tr
              v-for="part in parts"
              :key="part.id"
              class="part-table__tr"
              data-testid="part-table-row"
              :data-id="part.id"
            >
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
          data-testid="part-table-card"
        >
          <div class="part-table__card-row">
            <span class="part-table__card-code mono">{{ part.partCode }}</span>
            <span v-if="part.partType" class="part-table__type">{{ part.partType }}</span>
          </div>
          <p class="part-table__card-name">{{ part.partName }}</p>
          <dl class="part-table__card-grid">
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
.part-table__th--actions {
  width: 160px;
  padding-right: 16px;
  text-align: right;
}

.part-table__tr {
  transition: background 0.15s;
}
.part-table__tr:hover .part-table__td {
  background: var(--surface-2);
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
}
.part-table__row-link:hover {
  background: var(--primary-soft);
  color: var(--primary-hover);
}
.part-table__row-link--danger {
  color: var(--error);
  margin-left: 4px;
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
  .part-table__card-row {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 8px;
    margin-bottom: 6px;
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
  }
  .part-table__pagination {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
