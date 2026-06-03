<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import {
  Boxes,
  ExternalLink,
  Layers,
  Package,
  PackageMinus,
  PackagePlus,
  Plus,
  RefreshCw,
  Search
} from 'lucide-vue-next'
import dayjs from 'dayjs'
import PageHeader from '@/shared/components/ui/PageHeader.vue'
import BaseButton from '@/shared/components/ui/BaseButton.vue'
import StatusPill from '@/shared/components/ui/StatusPill.vue'
import EmptyState from '@/shared/components/ui/EmptyState.vue'
import LoadingSkeleton from '@/shared/components/ui/LoadingSkeleton.vue'
import AggregationBindDialog from '@/features/trace/components/AggregationBindDialog.vue'
import { useUserStore } from '@/core/stores/user'
import { useConfirm } from '@/shared/composables/useConfirm'
import { useToast } from '@/shared/composables/useToast'
import { logger } from '@/shared/utils/logger'
import { PERMISSIONS } from '@/shared/constants'
import {
  listActiveAggregations,
  releaseAggregation
} from '@/features/trace/api'

/**
 * TraceAggregationWorkbench —— 箱码 / 托盘码聚合关系工作台（Linear-light）。
 *
 * 视觉契约：
 *   - 单 accent lavender；Inter 正文 + JetBrains Mono 码字段
 *   - 顶部 PageHeader（标题 + 刷新 + 新建装箱 + 新建装托）
 *   - 三 KPI 卡（active 总数 / CARTON / PALLET）
 *   - 过滤区：类型 chips（全部 / CARTON / PALLET）+ 父码模糊搜索
 *   - 按 parent_code 分组的表格，每行有「解除」按钮
 *
 * 接口契约：
 *   - GET /api/trace-aggregations[?relation_type=...] 拉全量 active
 *   - POST /api/trace-aggregations 经 AggregationBindDialog 绑定
 *   - POST /api/trace-aggregations/{id}/release 解除
 */

const router = useRouter()
const toast = useToast()
const { confirm } = useConfirm()
const userStore = useUserStore()

const BIND_PERMISSIONS = PERMISSIONS.TRACE.AGGREGATION_ACCESS
const canBind = computed(() => userStore.hasAnyPermission(BIND_PERMISSIONS))

const aggregations = ref([])
const loading = ref(false)
const loadError = ref('')
const releasingId = ref(null)
const dialogOpen = ref(false)
const dialogRelationType = ref('CARTON')
// 非空 → 弹窗进入「添加成员」模式（父码 + 类型锁定）；新建装箱/装托时清空。
const dialogPresetParent = ref('')

const activeRelationFilter = ref('ALL')
const parentKeyword = ref('')

const RELATION_FILTERS = [
  { value: 'ALL', label: '全部' },
  { value: 'CARTON', label: '箱码' },
  { value: 'PALLET', label: '托盘码' }
]

const RELATION_LABEL = { CARTON: '箱码', PALLET: '托盘码', BATCH: '批量' }
const RELATION_TONE = { CARTON: 'primary', PALLET: 'success', BATCH: 'warn' }

const totalActive = computed(() => aggregations.value.length)
const cartonActive = computed(
  () => aggregations.value.filter((row) => row.relationType === 'CARTON').length
)
const palletActive = computed(
  () => aggregations.value.filter((row) => row.relationType === 'PALLET').length
)

const filteredAggregations = computed(() => {
  const keyword = parentKeyword.value.trim().toLowerCase()
  return aggregations.value.filter((row) => {
    if (activeRelationFilter.value !== 'ALL' && row.relationType !== activeRelationFilter.value) {
      return false
    }
    if (!keyword) return true
    return [row.parentCode, row.childCode, row.createByUsername]
      .filter(Boolean)
      .some((value) => String(value).toLowerCase().includes(keyword))
  })
})

/**
 * 把扁平的聚合行按 parent_code 分组。返回有序数组，便于模板按出现顺序渲染。
 * 后端已 ORDER BY parent_code，所以前端只需保留进入顺序即可。
 */
const groupedAggregations = computed(() => {
  const buckets = new Map()
  for (const row of filteredAggregations.value) {
    const key = `${row.parentCode}::${row.relationType}`
    if (!buckets.has(key)) {
      buckets.set(key, {
        key,
        parentCode: row.parentCode,
        relationType: row.relationType,
        rows: []
      })
    }
    buckets.get(key).rows.push(row)
  }
  return Array.from(buckets.values())
})

onMounted(() => {
  loadAggregations()
})

async function loadAggregations() {
  loading.value = true
  loadError.value = ''
  try {
    const params =
      activeRelationFilter.value === 'ALL'
        ? {}
        : { relationType: activeRelationFilter.value }
    const list = await listActiveAggregations(params)
    aggregations.value = Array.isArray(list) ? list : []
  } catch (error) {
    logger.error('加载聚合关系失败', error)
    loadError.value = error?.message || '加载聚合关系失败'
    aggregations.value = []
  } finally {
    loading.value = false
  }
}

function handleFilterChange(value) {
  if (activeRelationFilter.value === value) return
  activeRelationFilter.value = value
  loadAggregations()
}

function openBindDialog(relationType) {
  dialogPresetParent.value = ''
  dialogRelationType.value = relationType
  dialogOpen.value = true
}

/**
 * 向某个已有箱 / 托盘追加成员：预填并锁定父码 + 类型，复用同一个批量绑定弹窗。
 * 「移除成员」走每行的「解除」，二者合起来就是对已建聚合的增/减能力。
 */
function openAddMember(group) {
  if (!group?.parentCode) return
  dialogPresetParent.value = group.parentCode
  dialogRelationType.value = group.relationType
  dialogOpen.value = true
}

function handleBindSuccess() {
  loadAggregations()
}

async function handleRelease(row) {
  if (!row?.id) return
  const accepted = await confirm({
    title: '解除聚合关系',
    message: `确认解除 ${row.childCode} 与父码 ${row.parentCode} 的聚合？解除后会写入 ${
      row.relationType === 'PALLET' ? 'UNPALLETIZE' : 'UNPACK'
    } 上链事件。`,
    confirmText: '确认解除',
    cancelText: '取消',
    type: 'danger'
  })
  if (!accepted) return
  releasingId.value = row.id
  try {
    await releaseAggregation(row.id)
    toast.success('聚合关系已解除')
    await loadAggregations()
  } catch (error) {
    logger.error('解除聚合关系失败', error)
    // request.js 已 toast 错误，这里不重复
  } finally {
    releasingId.value = null
  }
}

function goTraceDetail(code) {
  if (!code) return
  // 只有单品码（trace_code）能跳详情；箱码/托盘父码不是 trace_code，跳过去会 404
  if (isParentAggregationCode(code)) return
  router.push(`/traces/${encodeURIComponent(code)}`)
}

function isParentAggregationCode(code) {
  if (!code) return false
  const upper = String(code).toUpperCase()
  return upper.startsWith('CARTON-') || upper.startsWith('PALLET-') || upper.startsWith('CTN-')
}

function formatBindTime(value) {
  return value ? dayjs(value).format('YYYY-MM-DD HH:mm') : '-'
}

function relationLabel(value) {
  return RELATION_LABEL[value] || value || '-'
}
function relationTone(value) {
  return RELATION_TONE[value] || 'mute'
}
</script>

<template>
  <div class="agg-workbench">
    <PageHeader
      title="箱码 / 托盘码聚合"
      subtitle="按父码分组查看所有 active 聚合关系；新建装箱/装托会自动写入 PACK / PALLETIZE 上链事件。"
      data-testid="aggregation-page-header"
    >
      <template #actions>
        <BaseButton
          variant="secondary"
          :loading="loading"
          :disabled="loading"
          data-test="aggregation-refresh"
          @click="loadAggregations"
        >
          <template #icon><RefreshCw :size="13" /></template>
          刷新
        </BaseButton>
        <BaseButton
          v-if="canBind"
          variant="secondary"
          :disabled="loading"
          data-test="aggregation-create-carton"
          @click="openBindDialog('CARTON')"
        >
          <template #icon><PackagePlus :size="13" /></template>
          新建装箱
        </BaseButton>
        <BaseButton
          v-if="canBind"
          variant="primary"
          :disabled="loading"
          data-test="aggregation-create-pallet"
          @click="openBindDialog('PALLET')"
        >
          <template #icon><Layers :size="13" /></template>
          新建装托
        </BaseButton>
      </template>
    </PageHeader>

    <section class="agg-workbench__kpis">
      <article class="agg-workbench__kpi" data-testid="aggregation-kpi-total">
        <span class="agg-workbench__kpi-label">Active 聚合总数</span>
        <span class="agg-workbench__kpi-value mono">{{ totalActive }}</span>
        <span class="agg-workbench__kpi-hint">所有未解除的 parent → child 关系</span>
      </article>
      <article class="agg-workbench__kpi" data-testid="aggregation-kpi-carton">
        <span class="agg-workbench__kpi-label">箱码（CARTON）</span>
        <span class="agg-workbench__kpi-value mono">{{ cartonActive }}</span>
        <span class="agg-workbench__kpi-hint">单品码 → 箱码</span>
      </article>
      <article class="agg-workbench__kpi" data-testid="aggregation-kpi-pallet">
        <span class="agg-workbench__kpi-label">托盘码（PALLET）</span>
        <span class="agg-workbench__kpi-value mono">{{ palletActive }}</span>
        <span class="agg-workbench__kpi-hint">箱码 / 单品码 → 托盘码</span>
      </article>
    </section>

    <section class="agg-workbench__filter-row">
      <div class="agg-workbench__chips" role="tablist">
        <button
          v-for="option in RELATION_FILTERS"
          :key="option.value"
          type="button"
          class="agg-workbench__chip"
          :class="activeRelationFilter === option.value && 'agg-workbench__chip--active'"
          :data-test="`aggregation-filter-${option.value}`"
          @click="handleFilterChange(option.value)"
        >
          {{ option.label }}
        </button>
      </div>
      <div class="agg-workbench__search">
        <Search :size="13" />
        <input
          v-model="parentKeyword"
          type="text"
          placeholder="按父码 / 子码 / 创建人模糊筛选"
          data-test="aggregation-search"
        />
      </div>
    </section>

    <section class="agg-workbench__panel" data-test="aggregation-table-panel">
      <div v-if="loadError" class="agg-workbench__error" role="alert">{{ loadError }}</div>

      <div v-if="loading && aggregations.length === 0" class="agg-workbench__loading">
        <LoadingSkeleton type="table" :rows="6" />
      </div>

      <div
        v-else-if="filteredAggregations.length === 0"
        class="agg-workbench__empty"
        data-test="aggregation-empty"
      >
        <EmptyState
          :icon="Boxes"
          title="当前筛选下暂无聚合关系"
          subtitle="切换类型 / 清空关键字、刷新、或新建一个装箱 / 装托关系。"
        />
      </div>

      <div
        v-else
        class="agg-workbench__groups"
        data-test="aggregation-groups"
      >
        <div
          v-for="group in groupedAggregations"
          :key="group.key"
          class="agg-workbench__group"
          :data-test="`aggregation-group-${group.parentCode}`"
        >
          <header class="agg-workbench__group-head">
            <div class="agg-workbench__group-title">
              <component
                :is="group.relationType === 'PALLET' ? Layers : Package"
                :size="14"
                class="agg-workbench__group-icon"
              />
              <span class="mono agg-workbench__group-parent">{{ group.parentCode }}</span>
              <StatusPill :tone="relationTone(group.relationType)" size="xs">
                {{ relationLabel(group.relationType) }}
              </StatusPill>
            </div>
            <div class="agg-workbench__group-meta">
              <span>共 {{ group.rows.length }} 个子码</span>
              <BaseButton
                v-if="canBind"
                variant="text"
                size="sm"
                :disabled="loading"
                :data-test="`aggregation-add-member-${group.parentCode}`"
                @click="openAddMember(group)"
              >
                <template #icon><Plus :size="13" /></template>
                添加成员
              </BaseButton>
            </div>
          </header>

          <div class="agg-workbench__list-wrap">
            <table class="agg-workbench__list">
              <thead>
                <tr>
                  <th class="agg-workbench__col-child">子码</th>
                  <th class="agg-workbench__col-bind">绑定时间</th>
                  <th class="agg-workbench__col-by">创建人</th>
                  <th class="agg-workbench__col-remark">备注</th>
                  <th class="agg-workbench__col-actions">操作</th>
                </tr>
              </thead>
              <tbody>
                <tr
                  v-for="row in group.rows"
                  :key="row.id"
                  class="agg-workbench__row"
                  :data-test="`aggregation-row-${row.id}`"
                >
                  <td>
                    <button
                      v-if="!isParentAggregationCode(row.childCode)"
                      type="button"
                      class="agg-workbench__child-link mono"
                      :data-test="`aggregation-child-link-${row.id}`"
                      @click="goTraceDetail(row.childCode)"
                    >
                      <span>{{ row.childCode }}</span>
                      <ExternalLink :size="11" />
                    </button>
                    <span v-else class="mono agg-workbench__child-code">{{ row.childCode }}</span>
                  </td>
                  <td class="agg-workbench__cell-mute mono">{{ formatBindTime(row.bindTime) }}</td>
                  <td class="agg-workbench__cell-mute">{{ row.createByUsername || '-' }}</td>
                  <td class="agg-workbench__cell-remark">{{ row.remark || '—' }}</td>
                  <td class="agg-workbench__cell-actions">
                    <BaseButton
                      v-if="canBind"
                      variant="text"
                      size="sm"
                      :loading="releasingId === row.id"
                      :disabled="releasingId === row.id"
                      :data-test="`aggregation-release-${row.id}`"
                      @click="handleRelease(row)"
                    >
                      <template #icon><PackageMinus :size="13" /></template>
                      解除
                    </BaseButton>
                    <span v-else class="agg-workbench__cell-mute">—</span>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>
      </div>
    </section>

    <AggregationBindDialog
      v-model="dialogOpen"
      :default-relation-type="dialogRelationType"
      :preset-parent-code="dialogPresetParent"
      @success="handleBindSuccess"
    />
  </div>
</template>

<style scoped>
.agg-workbench {
  width: 100%;
  max-width: 1280px;
  margin: 0 auto;
  padding: 24px 24px 48px;
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.agg-workbench__kpis {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
}
.agg-workbench__kpi {
  display: flex;
  flex-direction: column;
  gap: 4px;
  padding: 14px 16px;
  border: 1px solid var(--hairline);
  border-radius: 12px;
  background: var(--surface-1);
}
.agg-workbench__kpi-label {
  font-size: 11px;
  font-weight: 500;
  letter-spacing: 0.4px;
  color: var(--ink-tertiary);
  text-transform: uppercase;
}
.agg-workbench__kpi-value {
  font-size: 22px;
  font-weight: 600;
  letter-spacing: -0.4px;
  color: var(--ink);
  line-height: 1.1;
}
.agg-workbench__kpi-hint {
  font-size: 11.5px;
  color: var(--ink-subtle);
}

.agg-workbench__filter-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
}
.agg-workbench__chips {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  background: var(--surface-2);
  border-radius: 8px;
  padding: 3px;
}
.agg-workbench__chip {
  height: 26px;
  padding: 0 10px;
  border-radius: 6px;
  background: transparent;
  border: 0;
  font-size: 12.5px;
  font-weight: 500;
  color: var(--ink-subtle);
  cursor: pointer;
  transition: background 0.15s, color 0.15s;
}
.agg-workbench__chip:hover {
  color: var(--ink);
}
.agg-workbench__chip--active {
  background: var(--surface-1);
  color: var(--ink);
  box-shadow: 0 1px 2px rgba(15, 23, 42, 0.05);
}
.agg-workbench__search {
  display: flex;
  align-items: center;
  gap: 8px;
  height: 32px;
  padding: 0 10px;
  background: var(--surface-1);
  border: 1px solid var(--hairline);
  border-radius: 8px;
  min-width: 240px;
  flex: 1 1 280px;
}
.agg-workbench__search svg {
  color: var(--ink-tertiary);
  flex-shrink: 0;
}
.agg-workbench__search input {
  flex: 1;
  border: 0;
  background: transparent;
  font-size: 13px;
  color: var(--ink);
  outline: none;
}
.agg-workbench__search input::placeholder {
  color: var(--ink-tertiary);
}

.agg-workbench__panel {
  display: flex;
  flex-direction: column;
  gap: 16px;
  background: var(--surface-1);
  border: 1px solid var(--hairline);
  border-radius: 12px;
  padding: 16px;
}

.agg-workbench__error {
  background: var(--error-soft);
  color: var(--error);
  border: 1px solid #f8c8ca;
  border-radius: 8px;
  padding: 8px 12px;
  font-size: 12.5px;
  line-height: 1.45;
}

.agg-workbench__loading,
.agg-workbench__empty {
  padding: 4px 0;
}

.agg-workbench__groups {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.agg-workbench__group {
  border: 1px solid var(--hairline);
  border-radius: 8px;
  background: var(--surface-1);
  overflow: hidden;
}
.agg-workbench__group-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 10px 14px;
  background: var(--surface-2);
  border-bottom: 1px solid var(--hairline);
}
.agg-workbench__group-title {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
}
.agg-workbench__group-icon {
  color: var(--primary);
  flex-shrink: 0;
}
.agg-workbench__group-parent {
  font-size: 13px;
  font-weight: 600;
  color: var(--ink);
  letter-spacing: -0.1px;
  word-break: break-all;
}
.agg-workbench__group-meta {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 12px;
  color: var(--ink-subtle);
  white-space: nowrap;
}

.agg-workbench__list-wrap {
  overflow-x: auto;
}
.agg-workbench__list {
  width: 100%;
  border-collapse: collapse;
  font-size: 13px;
}
.agg-workbench__list th {
  text-align: left;
  font-weight: 500;
  font-size: 11.5px;
  letter-spacing: 0.04em;
  text-transform: uppercase;
  color: var(--ink-subtle);
  padding: 8px 12px;
  border-bottom: 1px solid var(--hairline);
  background: var(--surface-1);
}
.agg-workbench__list td {
  padding: 9px 12px;
  border-bottom: 1px solid var(--hairline);
  color: var(--ink);
  vertical-align: middle;
}
.agg-workbench__row:last-child td {
  border-bottom: 0;
}
.agg-workbench__cell-mute {
  color: var(--ink-muted);
  white-space: nowrap;
}
.agg-workbench__cell-remark {
  color: var(--ink-muted);
  max-width: 320px;
  word-break: break-word;
}
.agg-workbench__cell-actions {
  text-align: right;
  width: 1%;
  white-space: nowrap;
}
.agg-workbench__col-child {
  width: 32%;
}
.agg-workbench__col-bind {
  width: 18%;
}
.agg-workbench__col-by {
  width: 14%;
}
.agg-workbench__col-remark {
  width: 28%;
}
.agg-workbench__col-actions {
  width: 8%;
}
.agg-workbench__child-link {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  background: transparent;
  border: 0;
  padding: 0;
  font-size: 13px;
  font-weight: 500;
  color: var(--primary);
  cursor: pointer;
  font-family: 'JetBrains Mono', ui-monospace, monospace;
}
.agg-workbench__child-link:hover {
  text-decoration: underline;
  text-underline-offset: 3px;
}
.agg-workbench__child-code {
  font-size: 13px;
  color: var(--ink);
}

.mono {
  font-family: 'JetBrains Mono', ui-monospace, monospace;
}

@media (max-width: 1023.98px) {
  .agg-workbench__kpis {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
  .agg-workbench__col-remark {
    display: none;
  }
  .agg-workbench__list th:nth-child(4),
  .agg-workbench__list td:nth-child(4) {
    display: none;
  }
}

@media (max-width: 639.98px) {
  .agg-workbench {
    padding: 16px 12px 32px;
  }
  .agg-workbench__kpis {
    grid-template-columns: minmax(0, 1fr);
  }
  .agg-workbench__filter-row {
    flex-direction: column;
    align-items: stretch;
  }
  .agg-workbench__search {
    min-width: 0;
  }
  .agg-workbench__col-by {
    display: none;
  }
  .agg-workbench__list th:nth-child(3),
  .agg-workbench__list td:nth-child(3) {
    display: none;
  }
}
</style>
