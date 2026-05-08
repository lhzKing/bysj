<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { Camera, ChevronDown, Plus, Search, X } from 'lucide-vue-next'
import dayjs from 'dayjs'
import BaseButton from '@/shared/components/ui/BaseButton.vue'
import EmptyState from '@/shared/components/ui/EmptyState.vue'
import KbdShortcut from '@/shared/components/ui/KbdShortcut.vue'
import LoadingSkeleton from '@/shared/components/ui/LoadingSkeleton.vue'
import PageHeader from '@/shared/components/ui/PageHeader.vue'
import StatusPill from '@/shared/components/ui/StatusPill.vue'
import TraceCodeChip from '@/shared/components/ui/TraceCodeChip.vue'
import QRScanner from '@/shared/components/QRScanner.vue'
import CreateTraceDialog from '@/features/trace/components/CreateTraceDialog.vue'
import { listTraces } from '@/features/trace/api/trace'
import { useToast } from '@/shared/composables/useToast'
import { logger } from '@/shared/utils/logger'

const RECENT_KEY = 'recent_traces'
const RECENT_MAX = 20
const PAGE_SIZE = 20

const STATUS_OPTIONS = [
  { value: '', label: '全部', tone: 'mute' },
  { value: 'INIT', label: '已初始化', tone: 'mute' },
  { value: 'IN_STOCK', label: '在库', tone: 'success' },
  { value: 'IN_TRANSIT', label: '流转中', tone: 'warn' },
  { value: 'TRANSFERRED', label: '已交接', tone: 'mute' },
  { value: 'EXCEPTION', label: '异常', tone: 'error' }
]

const DATE_PRESETS = [
  { value: '', label: '全部时间' },
  { value: '1d', label: '近 24 小时' },
  { value: '7d', label: '近 7 天' },
  { value: '30d', label: '近 30 天' }
]

const SORT_OPTIONS = [
  { value: 'last_event_time', label: '最近事件' },
  { value: 'trace_code', label: '追溯码' },
  { value: 'update_time', label: '更新时间' }
]

const router = useRouter()
const toast = useToast()

const filters = reactive({
  keyword: '',
  status: '',
  spuId: '',
  batchNo: '',
  currentOwner: '',
  datePreset: ''
})

const sort = ref('last_event_time')
const order = ref('desc')

const page = ref(1)
const total = ref(0)
const totalPages = ref(0)
const rows = ref([])
const loading = ref(false)
const errorMessage = ref('')

const recentTraces = ref([])
const showRecents = ref(true)
const showScanner = ref(false)
const showCreateModal = ref(false)

const sortDropdownOpen = ref(false)

let searchDebounce = null

const datePreset = computed(() => filters.datePreset)
const eventTimeFrom = computed(() => {
  const preset = datePreset.value
  if (!preset) return ''
  const now = dayjs()
  if (preset === '1d') return now.subtract(1, 'day').format('YYYY-MM-DDTHH:mm:ss')
  if (preset === '7d') return now.subtract(7, 'day').startOf('day').format('YYYY-MM-DDTHH:mm:ss')
  if (preset === '30d') return now.subtract(30, 'day').startOf('day').format('YYYY-MM-DDTHH:mm:ss')
  return ''
})

const fetchPage = async () => {
  loading.value = true
  errorMessage.value = ''
  try {
    const params = {
      page: page.value,
      size: PAGE_SIZE,
      sort: sort.value,
      order: order.value
    }
    if (filters.keyword.trim()) params.keyword = filters.keyword.trim()
    if (filters.status) params.status = filters.status
    if (filters.spuId) {
      const id = Number(filters.spuId)
      if (!Number.isNaN(id)) params.spuId = id
    }
    if (filters.batchNo.trim()) params.batchNo = filters.batchNo.trim()
    if (filters.currentOwner.trim()) params.currentOwner = filters.currentOwner.trim()
    if (eventTimeFrom.value) params.eventTimeFrom = eventTimeFrom.value

    const data = await listTraces(params)
    rows.value = data?.list ?? []
    total.value = data?.total ?? 0
    totalPages.value = data?.totalPages ?? 0
  } catch (err) {
    logger.error('追溯列表加载失败:', err)
    errorMessage.value = err?.message || '加载失败'
    rows.value = []
    total.value = 0
    totalPages.value = 0
  } finally {
    loading.value = false
  }
}

const refresh = () => {
  page.value = 1
  fetchPage()
}

watch(
  () => [filters.status, filters.spuId, filters.batchNo, filters.currentOwner, filters.datePreset, sort.value, order.value],
  () => refresh()
)

watch(
  () => filters.keyword,
  () => {
    if (searchDebounce) clearTimeout(searchDebounce)
    searchDebounce = setTimeout(() => refresh(), 300)
  }
)

onMounted(() => {
  try {
    const saved = localStorage.getItem(RECENT_KEY)
    if (saved) recentTraces.value = JSON.parse(saved)
  } catch (err) {
    logger.error('读取最近访问失败:', err)
    recentTraces.value = []
  }
  fetchPage()
})

const persistRecent = () => {
  try {
    localStorage.setItem(RECENT_KEY, JSON.stringify(recentTraces.value))
  } catch (err) {
    logger.error('写入最近访问失败:', err)
  }
}

const pushRecent = (code) => {
  const dedup = recentTraces.value.filter((item) => item.code !== code)
  recentTraces.value = [{ code, time: new Date().toISOString() }, ...dedup].slice(0, RECENT_MAX)
  persistRecent()
}

const handleScan = (traceCode) => {
  try {
    showScanner.value = false
    if (traceCode) {
      pushRecent(traceCode)
      router.push(`/traces/${traceCode}`)
    }
  } catch (err) {
    logger.error('扫码处理失败:', err)
    toast.error(`扫码处理失败: ${err.message}`)
  }
}

const handleRowClick = (item) => {
  if (!item?.traceCode) return
  pushRecent(item.traceCode)
  router.push(`/traces/${item.traceCode}`)
}

const handleSearchEnter = () => {
  const code = filters.keyword.trim()
  if (!code) {
    toast.warning('请输入追溯码或关键词')
    return
  }
  if (rows.value.length === 1 && rows.value[0]?.traceCode?.toUpperCase() === code.toUpperCase()) {
    pushRecent(rows.value[0].traceCode)
    router.push(`/traces/${rows.value[0].traceCode}`)
    return
  }
  refresh()
}

const removeRecent = (code, event) => {
  event?.stopPropagation()
  recentTraces.value = recentTraces.value.filter((item) => item.code !== code)
  persistRecent()
}

const clearRecents = () => {
  recentTraces.value = []
  persistRecent()
}

const onCreateSuccess = (traceCodes) => {
  if (!traceCodes || traceCodes.length === 0) return
  const newCode = traceCodes[0]
  pushRecent(newCode)
  refresh()
  router.push(`/traces/${newCode}`)
}

const goToPage = (target) => {
  if (target < 1 || target > totalPages.value || target === page.value) return
  page.value = target
  fetchPage()
}

const pageWindow = computed(() => {
  const last = totalPages.value || 1
  const current = page.value
  const arr = []
  if (last <= 7) {
    for (let i = 1; i <= last; i++) arr.push(i)
    return arr
  }
  arr.push(1)
  if (current > 3) arr.push('…')
  const start = Math.max(2, current - 1)
  const end = Math.min(last - 1, current + 1)
  for (let i = start; i <= end; i++) arr.push(i)
  if (current < last - 2) arr.push('…')
  arr.push(last)
  return arr
})

const showingFrom = computed(() => (rows.value.length ? (page.value - 1) * PAGE_SIZE + 1 : 0))
const showingTo = computed(() => (page.value - 1) * PAGE_SIZE + rows.value.length)

const statusTone = (status) => {
  switch (status) {
    case 'IN_STOCK':
      return 'success'
    case 'IN_TRANSIT':
      return 'warn'
    case 'EXCEPTION':
      return 'error'
    case 'TRANSFERRED':
      return 'mute'
    case 'INIT':
      return 'primary'
    default:
      return 'mute'
  }
}

const statusLabel = (status) => {
  const m = STATUS_OPTIONS.find((s) => s.value === status)
  return m?.label || status || '-'
}

const formatRelative = (iso) => {
  if (!iso) return '-'
  const t = dayjs(iso)
  if (!t.isValid()) return '-'
  const now = dayjs()
  const diffMin = now.diff(t, 'minute')
  if (diffMin < 1) return '刚刚'
  if (diffMin < 60) return `${diffMin} 分钟前`
  const diffHour = now.diff(t, 'hour')
  if (diffHour < 24) return `今天 ${t.format('HH:mm')}`
  const diffDay = now.diff(t, 'day')
  if (diffDay === 1) return `昨日 ${t.format('HH:mm')}`
  if (diffDay < 7) return `${diffDay} 天前`
  return t.format('YYYY-MM-DD')
}

const lastActionLabel = (action) => {
  const m = {
    INIT: '初始化',
    PRINT_CODE: '打印',
    REPRINT_CODE: '重打',
    ACTIVATE_CODE: '激活',
    VOID_CODE: '作废',
    INBOUND: '入库',
    OUTBOUND: '出库',
    TRANSFER: '交接',
    PACK: '装箱',
    UNPACK: '拆箱',
    PALLETIZE: '上托',
    UNPALLETIZE: '下托',
    EXCEPTION: '异常',
    EXCEPTION_OPEN: '异常',
    EXCEPTION_CLOSE: '解除异常',
    CORRECTION: '纠错'
  }
  return action ? (m[action] || action) : '-'
}

const onSortSelect = (col) => {
  if (sort.value === col) {
    order.value = order.value === 'asc' ? 'desc' : 'asc'
  } else {
    sort.value = col
    order.value = 'desc'
  }
  sortDropdownOpen.value = false
}

const sortLabel = computed(() => {
  const m = SORT_OPTIONS.find((s) => s.value === sort.value)
  return m ? m.label : '最近事件'
})
</script>

<template>
  <div class="trace-list">
    <PageHeader
      title="追溯查询"
      :subtitle="loading
        ? '加载中…'
        : `${total.toLocaleString()} 条记录 · 当前第 ${page} / ${Math.max(totalPages, 1)} 页 · 链上完整性 100%`"
    >
      <template #actions>
        <BaseButton variant="secondary" size="sm" @click="showScanner = true">
          <template #icon><Camera class="trace-list__btn-icon" /></template>
          扫码
        </BaseButton>
        <BaseButton variant="primary" size="sm" @click="showCreateModal = true">
          <template #icon><Plus class="trace-list__btn-icon" /></template>
          生产赋码
        </BaseButton>
      </template>
    </PageHeader>

    <!-- Filter bar -->
    <section class="trace-list__filter-bar">
      <div class="trace-list__search-box" data-testid="trace-list-search-box">
        <Search class="trace-list__search-icon" />
        <input
          v-model="filters.keyword"
          class="trace-list__search-input"
          type="text"
          placeholder="搜索追溯码 / SPU 名称 / 批次 / 持有方"
          spellcheck="false"
          autocomplete="off"
          data-testid="trace-list-search-input"
          @keydown.enter.prevent="handleSearchEnter"
        />
        <KbdShortcut keys="Enter" />
      </div>

      <select
        v-model="filters.status"
        class="trace-list__filter-chip"
        :class="{ 'trace-list__filter-chip--has-val': filters.status }"
        data-testid="trace-list-status"
      >
        <option v-for="opt in STATUS_OPTIONS" :key="opt.value" :value="opt.value">
          状态 · {{ opt.label }}
        </option>
      </select>

      <input
        v-model.number="filters.spuId"
        type="number"
        min="1"
        placeholder="SPU ID"
        class="trace-list__filter-input"
        data-testid="trace-list-spu-id"
      />

      <input
        v-model="filters.batchNo"
        type="text"
        placeholder="批次号"
        class="trace-list__filter-input"
        spellcheck="false"
        autocomplete="off"
        data-testid="trace-list-batch-no"
      />

      <input
        v-model="filters.currentOwner"
        type="text"
        placeholder="持有方"
        class="trace-list__filter-input"
        spellcheck="false"
        autocomplete="off"
        data-testid="trace-list-owner"
      />

      <select
        v-model="filters.datePreset"
        class="trace-list__filter-chip"
        :class="{ 'trace-list__filter-chip--has-val': filters.datePreset }"
        data-testid="trace-list-date-preset"
      >
        <option v-for="opt in DATE_PRESETS" :key="opt.value" :value="opt.value">
          日期 · {{ opt.label }}
        </option>
      </select>

      <div class="trace-list__sort-wrap">
        <button
          class="trace-list__filter-chip"
          type="button"
          data-testid="trace-list-sort-toggle"
          @click="sortDropdownOpen = !sortDropdownOpen"
        >
          排序 · {{ sortLabel }} {{ order === 'asc' ? '↑' : '↓' }}
          <ChevronDown class="trace-list__chip-icon" />
        </button>
        <div v-if="sortDropdownOpen" class="trace-list__sort-menu">
          <button
            v-for="opt in SORT_OPTIONS"
            :key="opt.value"
            type="button"
            class="trace-list__sort-item"
            :class="{ 'trace-list__sort-item--active': sort === opt.value }"
            @click="onSortSelect(opt.value)"
          >
            {{ opt.label }}
            <span v-if="sort === opt.value" class="trace-list__sort-arrow">{{ order === 'asc' ? '↑' : '↓' }}</span>
          </button>
        </div>
      </div>

      <span class="trace-list__match-count" data-testid="trace-list-match-count">
        <strong>{{ total.toLocaleString() }}</strong> 条匹配
      </span>
    </section>

    <!-- Recents (collapsible) -->
    <section v-if="recentTraces.length > 0" class="trace-list__recents">
      <header class="trace-list__recents-header">
        <button
          type="button"
          class="trace-list__recents-toggle"
          data-testid="trace-list-recents-toggle"
          @click="showRecents = !showRecents"
        >
          <span class="trace-list__eyebrow">最近访问 · {{ recentTraces.length }}</span>
          <ChevronDown
            class="trace-list__chip-icon"
            :style="{ transform: showRecents ? 'rotate(180deg)' : 'none' }"
          />
        </button>
        <BaseButton
          v-if="showRecents"
          variant="text"
          size="sm"
          data-testid="trace-list-clear-recent"
          @click="clearRecents"
        >
          清空
        </BaseButton>
      </header>
      <ul v-if="showRecents" class="trace-list__recents-list">
        <li
          v-for="item in recentTraces.slice(0, 8)"
          :key="item.code"
          class="trace-list__recent-chip"
          data-testid="trace-list-recent-chip"
          :data-code="item.code"
          @click="handleRowClick({ traceCode: item.code })"
        >
          <span class="mono">{{ item.code }}</span>
          <button
            type="button"
            class="trace-list__recent-remove"
            :aria-label="`移除 ${item.code}`"
            data-testid="trace-list-recent-remove"
            @click="removeRecent(item.code, $event)"
          >
            <X class="trace-list__btn-icon" />
          </button>
        </li>
      </ul>
    </section>

    <!-- Table card -->
    <section class="trace-list__table-card">
      <!-- Loading -->
      <div v-if="loading" class="trace-list__table-loading">
        <LoadingSkeleton v-for="i in 6" :key="i" height="44px" />
      </div>

      <!-- Error -->
      <div v-else-if="errorMessage" class="trace-list__error" data-testid="trace-list-error">
        <EmptyState
          :icon="Search"
          title="加载失败"
          :subtitle="errorMessage"
        >
          <template #actions>
            <BaseButton variant="secondary" size="sm" @click="fetchPage">重试</BaseButton>
          </template>
        </EmptyState>
      </div>

      <!-- Empty -->
      <div v-else-if="rows.length === 0" class="trace-list__empty" data-testid="trace-list-empty">
        <EmptyState
          :icon="Search"
          title="未匹配到任何追溯码"
          subtitle="清空筛选条件或更换关键词后重试。"
        />
      </div>

      <!-- Table -->
      <div v-else>
        <div class="trace-list__table-wrapper">
          <table class="trace-list__table">
            <thead>
              <tr>
                <th class="trace-list__th">追溯码</th>
                <th class="trace-list__th">SPU</th>
                <th class="trace-list__th">批次</th>
                <th class="trace-list__th">状态</th>
                <th class="trace-list__th">持有方</th>
                <th class="trace-list__th">最近动作</th>
                <th class="trace-list__th trace-list__th--actions">操作</th>
              </tr>
            </thead>
            <tbody>
              <tr
                v-for="row in rows"
                :key="row.traceCode"
                class="trace-list__tr"
                data-testid="trace-list-row"
                :data-code="row.traceCode"
                @click="handleRowClick(row)"
              >
                <td class="trace-list__td trace-list__td--code">
                  <TraceCodeChip :code="row.traceCode" :copyable="false" size="md" />
                </td>
                <td class="trace-list__td">
                  <div class="trace-list__spu">
                    <span class="trace-list__spu-name">{{ row.spuPartName || '-' }}</span>
                    <span v-if="row.spuPartCode" class="trace-list__spu-code mono">{{ row.spuPartCode }}</span>
                  </div>
                </td>
                <td class="trace-list__td mono trace-list__td--batch">{{ row.batchNo || '-' }}</td>
                <td class="trace-list__td">
                  <StatusPill :tone="statusTone(row.currentStatus)">
                    {{ statusLabel(row.currentStatus) }}
                  </StatusPill>
                </td>
                <td class="trace-list__td">
                  <div class="trace-list__owner">
                    <span>{{ row.currentOwner || '-' }}</span>
                    <span v-if="row.currentNode" class="trace-list__owner-node">· {{ row.currentNode }}</span>
                  </div>
                </td>
                <td class="trace-list__td trace-list__td--action">
                  <span class="trace-list__action">{{ lastActionLabel(row.lastActionType) }}</span>
                  <span class="trace-list__action-time">· {{ formatRelative(row.lastEventTime) }}</span>
                </td>
                <td class="trace-list__td trace-list__td--actions" @click.stop>
                  <button
                    type="button"
                    class="trace-list__row-link"
                    data-testid="trace-list-row-view"
                    @click="handleRowClick(row)"
                  >
                    查看
                  </button>
                </td>
              </tr>
            </tbody>
          </table>
        </div>

        <!-- Mobile cards (hidden on desktop) -->
        <ul class="trace-list__cards">
          <li
            v-for="row in rows"
            :key="row.traceCode"
            class="trace-list__card-item"
            data-testid="trace-list-card"
            @click="handleRowClick(row)"
          >
            <div class="trace-list__card-row">
              <TraceCodeChip :code="row.traceCode" :copyable="false" size="md" />
              <StatusPill :tone="statusTone(row.currentStatus)">
                {{ statusLabel(row.currentStatus) }}
              </StatusPill>
            </div>
            <p class="trace-list__card-spu">{{ row.spuPartName || '-' }}</p>
            <dl class="trace-list__card-grid">
              <div>
                <dt>批次</dt>
                <dd class="mono">{{ row.batchNo || '-' }}</dd>
              </div>
              <div>
                <dt>持有方</dt>
                <dd>{{ row.currentOwner || '-' }}</dd>
              </div>
              <div>
                <dt>最近动作</dt>
                <dd>{{ lastActionLabel(row.lastActionType) }} · {{ formatRelative(row.lastEventTime) }}</dd>
              </div>
            </dl>
          </li>
        </ul>
      </div>
    </section>

    <!-- Pagination -->
    <section v-if="!loading && !errorMessage && rows.length > 0" class="trace-list__pagination">
      <div class="trace-list__page-summary">
        显示第 {{ showingFrom }}–{{ showingTo }} 条 · 共 {{ total.toLocaleString() }} 条匹配
      </div>
      <div class="trace-list__page-buttons">
        <button
          class="trace-list__page-btn"
          :class="{ 'trace-list__page-btn--disabled': page <= 1 }"
          type="button"
          data-testid="trace-list-page-prev"
          @click="goToPage(page - 1)"
        >‹</button>
        <button
          v-for="(p, idx) in pageWindow"
          :key="`${p}-${idx}`"
          type="button"
          class="trace-list__page-btn"
          :class="{
            'trace-list__page-btn--active': p === page,
            'trace-list__page-btn--ellipsis': p === '…'
          }"
          :disabled="p === '…'"
          :data-testid="`trace-list-page-${p}`"
          @click="typeof p === 'number' && goToPage(p)"
        >{{ p }}</button>
        <button
          class="trace-list__page-btn"
          :class="{ 'trace-list__page-btn--disabled': page >= totalPages }"
          type="button"
          data-testid="trace-list-page-next"
          @click="goToPage(page + 1)"
        >›</button>
      </div>
    </section>

    <QRScanner
      v-if="showScanner"
      @scan="handleScan"
      @close="showScanner = false"
    />

    <CreateTraceDialog
      v-model="showCreateModal"
      @success="onCreateSuccess"
    />
  </div>
</template>

<style scoped>
.trace-list {
  max-width: 1280px;
  margin: 0 auto;
  padding: 24px 24px 48px;
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.trace-list__btn-icon {
  width: 13px;
  height: 13px;
}
.trace-list__chip-icon {
  width: 11px;
  height: 11px;
  color: var(--ink-tertiary);
  transition: transform 0.15s;
}

/* Filter bar */
.trace-list__filter-bar {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.trace-list__search-box {
  display: flex;
  align-items: center;
  gap: 8px;
  height: 32px;
  padding: 0 10px;
  background: var(--surface-1);
  border: 1px solid var(--hairline);
  border-radius: 8px;
  min-width: 280px;
  flex: 1 1 280px;
  max-width: 380px;
  transition: border-color 0.15s, box-shadow 0.15s;
}
.trace-list__search-box:focus-within {
  border-color: var(--primary-focus, #5e69d1);
  box-shadow: 0 0 0 3px rgba(94, 106, 210, 0.15);
}

.trace-list__search-icon {
  width: 13px;
  height: 13px;
  color: var(--ink-tertiary);
  flex-shrink: 0;
}

.trace-list__search-input {
  flex: 1 1 auto;
  border: 0;
  outline: none;
  background: transparent;
  font: inherit;
  font-size: 13px;
  color: var(--ink);
  min-width: 0;
}
.trace-list__search-input::placeholder {
  color: var(--ink-tertiary);
}

.trace-list__filter-chip {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  height: 32px;
  padding: 0 10px;
  border-radius: 8px;
  background: var(--surface-1);
  border: 1px solid var(--hairline);
  font-size: 13px;
  color: var(--ink-muted);
  font-weight: 500;
  cursor: pointer;
  appearance: none;
  font-family: inherit;
}
.trace-list__filter-chip:hover {
  border-color: var(--ink-subtle);
}
.trace-list__filter-chip--has-val {
  color: var(--ink);
  background: var(--surface-2);
  border-color: var(--hairline-strong);
}

.trace-list__filter-input {
  height: 32px;
  padding: 0 10px;
  border-radius: 8px;
  background: var(--surface-1);
  border: 1px solid var(--hairline);
  font-size: 13px;
  color: var(--ink);
  font-family: inherit;
  outline: none;
  width: 120px;
  transition: border-color 0.15s;
}
.trace-list__filter-input:focus {
  border-color: var(--primary-focus, #5e69d1);
}
.trace-list__filter-input::placeholder {
  color: var(--ink-tertiary);
}

.trace-list__sort-wrap {
  position: relative;
}
.trace-list__sort-menu {
  position: absolute;
  top: 36px;
  right: 0;
  z-index: 10;
  background: var(--surface-1);
  border: 1px solid var(--hairline);
  border-radius: 8px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.06);
  min-width: 160px;
  padding: 4px;
}
.trace-list__sort-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
  height: 28px;
  padding: 0 8px;
  border-radius: 6px;
  background: transparent;
  border: 0;
  font-size: 13px;
  color: var(--ink-muted);
  cursor: pointer;
  font-family: inherit;
}
.trace-list__sort-item:hover {
  background: var(--surface-2);
  color: var(--ink);
}
.trace-list__sort-item--active {
  color: var(--ink);
  font-weight: 500;
}
.trace-list__sort-arrow {
  color: var(--primary);
  font-size: 11px;
}

.trace-list__match-count {
  margin-left: auto;
  font-size: 12.5px;
  color: var(--ink-subtle);
}
.trace-list__match-count strong {
  color: var(--ink);
  font-weight: 600;
}

/* Recents */
.trace-list__recents {
  background: var(--surface-1);
  border: 1px solid var(--hairline);
  border-radius: 12px;
  padding: 12px 16px;
}
.trace-list__recents-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}
.trace-list__recents-toggle {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  background: transparent;
  border: 0;
  cursor: pointer;
  padding: 4px 0;
  font-family: inherit;
}
.trace-list__eyebrow {
  font-size: 11px;
  font-weight: 500;
  letter-spacing: 0.4px;
  text-transform: uppercase;
  color: var(--ink-subtle);
}
.trace-list__recents-list {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 6px;
  margin: 8px 0 0 0;
  padding: 0;
  list-style: none;
}
.trace-list__recent-chip {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 3px 6px 3px 8px;
  background: var(--surface-2);
  border: 1px solid var(--hairline);
  border-radius: 6px;
  cursor: pointer;
  font-size: 12px;
  color: var(--ink-muted);
  transition: background 0.15s, border-color 0.15s;
}
.trace-list__recent-chip:hover {
  background: var(--primary-soft);
  color: var(--primary);
  border-color: var(--primary-focus, #5e69d1);
}
.trace-list__recent-remove {
  width: 16px;
  height: 16px;
  display: inline-grid;
  place-items: center;
  background: transparent;
  border: 0;
  border-radius: 4px;
  color: var(--ink-tertiary);
  cursor: pointer;
}
.trace-list__recent-remove:hover {
  color: var(--ink);
  background: rgba(0, 0, 0, 0.04);
}

/* Table card */
.trace-list__table-card {
  background: var(--surface-1);
  border: 1px solid var(--hairline);
  border-radius: 12px;
  overflow: hidden;
}
.trace-list__table-loading {
  padding: 16px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.trace-list__error,
.trace-list__empty {
  padding: 24px 0;
}

.trace-list__table-wrapper {
  width: 100%;
  overflow-x: auto;
}

.trace-list__table {
  width: 100%;
  border-collapse: collapse;
  font-size: 13px;
}
.trace-list__th {
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
.trace-list__th--actions { width: 80px; padding-right: 16px; text-align: right; }

.trace-list__tr {
  cursor: pointer;
  transition: background 0.15s;
}
.trace-list__tr:hover .trace-list__td {
  background: var(--surface-2);
}

.trace-list__td {
  padding: 10px 14px;
  border-bottom: 1px solid var(--hairline);
  color: var(--ink);
  vertical-align: middle;
  background: var(--surface-1);
  transition: background 0.15s;
  white-space: nowrap;
}
.trace-list__tr:last-child .trace-list__td {
  border-bottom: 0;
}
.trace-list__td--batch { color: var(--ink-muted); font-size: 12px; }
.trace-list__td--action { color: var(--ink-subtle); }
.trace-list__td--actions { padding-right: 16px; text-align: right; }

.trace-list__spu {
  display: flex;
  flex-direction: column;
  gap: 2px;
}
.trace-list__spu-name {
  color: var(--ink);
}
.trace-list__spu-code {
  color: var(--ink-tertiary);
  font-size: 11.5px;
}

.trace-list__owner {
  display: inline-flex;
  align-items: baseline;
  gap: 4px;
  color: var(--ink-muted);
}
.trace-list__owner-node {
  color: var(--ink-tertiary);
  font-size: 12px;
}

.trace-list__action {
  color: var(--ink-muted);
}
.trace-list__action-time {
  color: var(--ink-tertiary);
  font-size: 12px;
  margin-left: 4px;
}

.trace-list__row-link {
  background: transparent;
  border: 0;
  color: var(--primary);
  font-weight: 500;
  font-size: 13px;
  cursor: pointer;
  font-family: inherit;
}
.trace-list__row-link:hover {
  color: var(--primary-hover);
}

.trace-list__cards {
  display: none;
  list-style: none;
  margin: 0;
  padding: 0;
}

/* Pagination */
.trace-list__pagination {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}
.trace-list__page-summary {
  font-size: 12.5px;
  color: var(--ink-subtle);
}
.trace-list__page-buttons {
  display: flex;
  align-items: center;
  gap: 4px;
}
.trace-list__page-btn {
  min-width: 28px;
  height: 28px;
  padding: 0 6px;
  border-radius: 6px;
  display: grid;
  place-items: center;
  font-size: 12.5px;
  font-weight: 500;
  color: var(--ink-muted);
  cursor: pointer;
  background: transparent;
  border: 0;
  font-family: inherit;
}
.trace-list__page-btn:hover:not(.trace-list__page-btn--disabled):not(.trace-list__page-btn--ellipsis) {
  background: var(--surface-2);
  color: var(--ink);
}
.trace-list__page-btn--active {
  background: var(--surface-2);
  color: var(--ink);
}
.trace-list__page-btn--disabled,
.trace-list__page-btn--ellipsis {
  color: var(--ink-tertiary);
  cursor: not-allowed;
}

.mono {
  font-family: 'JetBrains Mono', ui-monospace, 'SF Mono', Menlo, monospace;
}

/* Tablet: cap field widths so chips wrap */
@media (max-width: 1023px) {
  .trace-list__filter-input { width: 100px; }
  .trace-list__search-box { min-width: 220px; flex-basis: 220px; }
}

/* Mobile: switch to card layout */
@media (max-width: 640px) {
  .trace-list { padding: 16px 12px 32px; }
  .trace-list__filter-bar { gap: 6px; }
  .trace-list__match-count { width: 100%; margin-left: 0; }
  .trace-list__table-wrapper { display: none; }
  .trace-list__cards {
    display: flex;
    flex-direction: column;
  }
  .trace-list__card-item {
    padding: 14px 16px;
    border-bottom: 1px solid var(--hairline);
    cursor: pointer;
    transition: background 0.15s;
  }
  .trace-list__card-item:last-child { border-bottom: 0; }
  .trace-list__card-item:active { background: var(--surface-2); }
  .trace-list__card-row {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 8px;
    margin-bottom: 6px;
  }
  .trace-list__card-spu {
    margin: 0 0 8px 0;
    color: var(--ink);
    font-size: 13.5px;
    font-weight: 500;
  }
  .trace-list__card-grid {
    display: grid;
    grid-template-columns: 1fr 1fr;
    gap: 8px 12px;
    margin: 0;
  }
  .trace-list__card-grid > div { display: flex; flex-direction: column; gap: 2px; }
  .trace-list__card-grid dt {
    font-size: 10.5px;
    text-transform: uppercase;
    letter-spacing: 0.04em;
    color: var(--ink-tertiary);
    margin: 0;
  }
  .trace-list__card-grid dd {
    margin: 0;
    color: var(--ink);
    font-size: 12.5px;
  }
  .trace-list__pagination {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
