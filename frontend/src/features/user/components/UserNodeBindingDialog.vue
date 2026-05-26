<script setup>
import { computed, ref, watch } from 'vue'
import { MapPin, Star } from 'lucide-vue-next'
import BaseButton from '@/shared/components/ui/BaseButton.vue'
import BaseDialog from '@/shared/components/ui/BaseDialog.vue'
import EmptyState from '@/shared/components/ui/EmptyState.vue'
import LoadingSkeleton from '@/shared/components/ui/LoadingSkeleton.vue'

/**
 * UserNodeBindingDialog —— 管理某个用户可操作的 trace_node 列表。
 *
 * 数据流：父组件传入 user / loading / saving / allNodes / currentBindings；
 * 用户在对话框里勾选/取消、选择"默认节点"，提交时 emit('save', { nodeIds, defaultNodeId })。
 * 父组件负责调 PUT /api/users/{id}/trace-nodes 并重载状态。
 *
 * UI 契约：
 *  - 节点按 nodeType 分组（FACTORY / WAREHOUSE / LOGISTICS / CUSTOMER / SERVICE），
 *    每组一个 section；组内每行：复选框 + node_code（mono 11.5px）+ node_name +
 *    省市 + 一颗五角星 radio（默认节点单选）
 *  - 已选节点数实时显示在 footer 左侧
 *  - 提交校验：default_node_id 必须在 node_ids 内；node_ids 可以为空（清空绑定）
 *  - persistent + 加载/保存时 disable 交互
 */

const props = defineProps({
  visible: { type: Boolean, default: false },
  user: { type: Object, default: null },
  allNodes: { type: Array, default: () => [] },
  currentBindings: { type: Array, default: () => [] },
  loading: { type: Boolean, default: false },
  saving: { type: Boolean, default: false }
})

const emit = defineEmits(['update:visible', 'save'])

const localVisible = computed({
  get: () => props.visible,
  set: (val) => emit('update:visible', val)
})

const selectedIds = ref(new Set())
const defaultId = ref(null)

const NODE_TYPE_ORDER = ['FACTORY', 'WAREHOUSE', 'LOGISTICS', 'CUSTOMER', 'SERVICE']
const NODE_TYPE_LABEL = {
  FACTORY: '生产工厂',
  WAREHOUSE: '仓储节点',
  LOGISTICS: '物流转运',
  CUSTOMER: '客户节点',
  SERVICE: '售后/服务'
}

// Sync internal state from props whenever the dialog reopens or props change.
watch(
  () => [props.visible, props.currentBindings],
  ([nowVisible]) => {
    if (!nowVisible) return
    const next = new Set()
    let nextDefault = null
    for (const b of props.currentBindings) {
      const nodeId = b?.nodeId ?? b?.node_id
      if (nodeId == null) continue
      next.add(nodeId)
      if (b?.defaultNode || b?.default_node) {
        nextDefault = nodeId
      }
    }
    selectedIds.value = next
    defaultId.value = nextDefault
  },
  { immediate: true, deep: true }
)

const groupedNodes = computed(() => {
  const groups = {}
  for (const node of props.allNodes) {
    const type = node?.nodeType || node?.node_type || 'SERVICE'
    if (!groups[type]) groups[type] = []
    groups[type].push(node)
  }
  return NODE_TYPE_ORDER
    .filter((t) => groups[t]?.length)
    .map((t) => ({ type: t, label: NODE_TYPE_LABEL[t] || t, nodes: groups[t] }))
})

const selectedCount = computed(() => selectedIds.value.size)

function isChecked(nodeId) {
  return selectedIds.value.has(nodeId)
}

function toggleNode(nodeId, checked) {
  const next = new Set(selectedIds.value)
  if (checked) {
    next.add(nodeId)
  } else {
    next.delete(nodeId)
    if (defaultId.value === nodeId) defaultId.value = null
  }
  selectedIds.value = next
}

function setDefault(nodeId) {
  if (!selectedIds.value.has(nodeId)) {
    // Auto-select the node when user clicks its "default" radio.
    toggleNode(nodeId, true)
  }
  defaultId.value = nodeId
}

function onCancel() {
  if (props.saving) return
  localVisible.value = false
}

function onSave() {
  const nodeIds = [...selectedIds.value]
  // If default is set but no longer in selection, drop it; if not set and only one node, auto-promote.
  let resolvedDefault = defaultId.value
  if (resolvedDefault != null && !selectedIds.value.has(resolvedDefault)) {
    resolvedDefault = null
  }
  if (resolvedDefault == null && nodeIds.length === 1) {
    resolvedDefault = nodeIds[0]
  }
  emit('save', { nodeIds, defaultNodeId: resolvedDefault })
}

function nodeRegion(node) {
  const province = node?.province || ''
  const city = node?.city || ''
  if (province && city) return `${province} · ${city}`
  return province || city || '-'
}
</script>

<template>
  <BaseDialog
    v-model="localVisible"
    title="管理节点绑定"
    :subtitle="user
      ? `为「${user.username}」分配可操作的溯源业务节点。仅勾选的节点上能扫码 / 出入库 / 解除冻结。`
      : '为指定用户分配可操作的溯源业务节点。'"
    :icon="MapPin"
    size="md"
    persistent
    data-testid="user-node-binding-dialog"
  >
    <div class="binding-form" data-testid="user-node-binding-form">
      <div v-if="loading" class="binding-form__loading">
        <LoadingSkeleton v-for="i in 4" :key="i" height="32px" />
      </div>

      <EmptyState
        v-else-if="!allNodes.length"
        :icon="MapPin"
        title="暂无可绑定的节点"
        subtitle="请先在「节点管理」创建并启用 trace_node 主数据。"
      />

      <template v-else>
        <p class="binding-form__hint">
          ✓ 勾选启用节点（每行最左侧）
          <span class="binding-form__hint-divider">·</span>
          ★ 点击星标设为默认节点（扫码时未指定节点会用这个）
        </p>

        <section
          v-for="group in groupedNodes"
          :key="group.type"
          class="binding-group"
          :data-type="group.type"
        >
          <h4 class="binding-group__title">
            <span class="binding-group__badge">{{ group.label }}</span>
            <span class="binding-group__count">{{ group.nodes.length }} 个</span>
          </h4>
          <ul class="binding-group__list">
            <li
              v-for="node in group.nodes"
              :key="node.id"
              class="binding-row"
              :class="{
                'binding-row--checked': isChecked(node.id),
                'binding-row--default': defaultId === node.id
              }"
              :data-testid="`binding-row-${node.nodeCode || node.node_code}`"
            >
              <label class="binding-row__main">
                <input
                  type="checkbox"
                  class="binding-row__check"
                  :checked="isChecked(node.id)"
                  :disabled="saving"
                  @change="toggleNode(node.id, $event.target.checked)"
                />
                <span class="binding-row__code mono">{{ node.nodeCode || node.node_code }}</span>
                <span class="binding-row__name">{{ node.nodeName || node.node_name }}</span>
                <span class="binding-row__region">{{ nodeRegion(node) }}</span>
              </label>
              <button
                type="button"
                class="binding-row__default-btn"
                :class="{ 'binding-row__default-btn--active': defaultId === node.id }"
                :disabled="saving"
                :title="defaultId === node.id ? '当前默认节点' : '设为默认节点'"
                @click="setDefault(node.id)"
              >
                <Star class="binding-row__default-icon" :fill="defaultId === node.id ? 'currentColor' : 'none'" />
              </button>
            </li>
          </ul>
        </section>
      </template>
    </div>

    <template #footer>
      <span class="binding-form__summary" data-testid="binding-form-summary">
        已勾选 <strong>{{ selectedCount }}</strong> 个节点<template v-if="defaultId !== null"> · 默认节点已设置</template>
      </span>
      <BaseButton variant="secondary" size="md" :disabled="saving" @click="onCancel">
        取消
      </BaseButton>
      <BaseButton variant="primary" size="md" :loading="saving" :disabled="loading || saving" @click="onSave">
        保存绑定
      </BaseButton>
    </template>
  </BaseDialog>
</template>

<style scoped>
.binding-form {
  display: flex;
  flex-direction: column;
  gap: 14px;
  max-height: 60vh;
  overflow-y: auto;
}
.binding-form__loading {
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.binding-form__hint {
  margin: 0;
  padding: 8px 12px;
  font-size: 12px;
  color: var(--ink-muted);
  background: var(--surface-2);
  border: 1px solid var(--hairline);
  border-radius: 8px;
}
.binding-form__hint-divider {
  margin: 0 8px;
  color: var(--ink-tertiary);
}
.binding-form__summary {
  flex: 1;
  font-size: 12.5px;
  color: var(--ink-subtle);
}
.binding-form__summary strong {
  margin: 0 2px;
  color: var(--primary);
  font-weight: 600;
}

.binding-group {
  display: flex;
  flex-direction: column;
  gap: 6px;
}
.binding-group__title {
  display: flex;
  align-items: center;
  gap: 8px;
  margin: 0;
  font-size: 11.5px;
  text-transform: uppercase;
  letter-spacing: 0.04em;
  color: var(--ink-subtle);
  font-weight: 500;
}
.binding-group__badge {
  color: var(--ink);
  font-weight: 600;
}
.binding-group__count {
  color: var(--ink-tertiary);
  font-size: 11px;
}
.binding-group__list {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.binding-row {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 10px;
  border-radius: 8px;
  border: 1px solid var(--hairline);
  background: var(--surface-1);
  transition: background 0.15s, border-color 0.15s;
}
.binding-row:hover {
  background: var(--surface-2);
}
.binding-row--checked {
  border-color: var(--primary-focus, #5e69d1);
  background: var(--primary-soft);
}
.binding-row--default {
  border-color: var(--primary);
}

.binding-row__main {
  flex: 1;
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
  cursor: pointer;
}
.binding-row__check {
  width: 14px;
  height: 14px;
  accent-color: var(--primary);
  cursor: pointer;
  flex-shrink: 0;
}
.binding-row__code {
  font-size: 11.5px;
  color: var(--ink-tertiary);
  flex-shrink: 0;
  min-width: 140px;
}
.binding-row__name {
  flex: 1;
  font-size: 13px;
  color: var(--ink);
  font-weight: 500;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.binding-row__region {
  font-size: 11.5px;
  color: var(--ink-subtle);
  flex-shrink: 0;
}

.binding-row__default-btn {
  flex-shrink: 0;
  display: inline-grid;
  place-items: center;
  width: 28px;
  height: 28px;
  border-radius: 6px;
  border: 1px solid transparent;
  background: transparent;
  color: var(--ink-tertiary);
  cursor: pointer;
  transition: color 0.15s, background 0.15s, border-color 0.15s;
}
.binding-row__default-btn:hover:not(:disabled) {
  color: var(--warn);
  background: var(--warn-soft);
}
.binding-row__default-btn:disabled {
  cursor: not-allowed;
  opacity: 0.5;
}
.binding-row__default-btn--active {
  color: var(--warn);
  background: var(--warn-soft);
  border-color: var(--warn);
}
.binding-row__default-icon {
  width: 14px;
  height: 14px;
}

.mono {
  font-family: 'JetBrains Mono', ui-monospace, 'SF Mono', Menlo, monospace;
}

@media (max-width: 640px) {
  .binding-row {
    flex-wrap: wrap;
  }
  .binding-row__main {
    flex-wrap: wrap;
    gap: 6px 10px;
  }
  .binding-row__code {
    min-width: 0;
  }
  .binding-row__region {
    width: 100%;
    padding-left: 24px;
  }
}
</style>
