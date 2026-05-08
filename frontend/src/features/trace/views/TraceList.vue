<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { Camera, Plus, Search, Trash2, X } from 'lucide-vue-next'
import dayjs from 'dayjs'
import BaseButton from '@/shared/components/ui/BaseButton.vue'
import EmptyState from '@/shared/components/ui/EmptyState.vue'
import KbdShortcut from '@/shared/components/ui/KbdShortcut.vue'
import PageHeader from '@/shared/components/ui/PageHeader.vue'
import TraceCodeChip from '@/shared/components/ui/TraceCodeChip.vue'
import QRScanner from '@/shared/components/QRScanner.vue'
import CreateTraceDialog from '@/features/trace/components/CreateTraceDialog.vue'
import { useToast } from '@/shared/composables/useToast'
import { logger } from '@/shared/utils/logger'

const RECENT_KEY = 'recent_traces'
const RECENT_MAX = 20

const router = useRouter()
const toast = useToast()

const searchQuery = ref('')
const recentTraces = ref([])
const showScanner = ref(false)
const showCreateModal = ref(false)

onMounted(() => {
  try {
    const saved = localStorage.getItem(RECENT_KEY)
    if (saved) recentTraces.value = JSON.parse(saved)
  } catch (err) {
    logger.error('读取最近访问失败:', err)
    recentTraces.value = []
  }
})

const filteredRecent = computed(() => {
  const q = searchQuery.value.trim().toUpperCase()
  if (!q) return recentTraces.value
  return recentTraces.value.filter((item) => item.code.toUpperCase().includes(q))
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

const handleSearch = () => {
  const code = searchQuery.value.trim()
  if (!code) {
    toast.warning('请输入追溯码')
    return
  }
  pushRecent(code)
  router.push(`/traces/${code}`)
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
  pushRecent(item.code)
  router.push(`/traces/${item.code}`)
}

const handleRemoveRecent = (code, event) => {
  event?.stopPropagation()
  recentTraces.value = recentTraces.value.filter((item) => item.code !== code)
  persistRecent()
}

const handleClearRecent = () => {
  recentTraces.value = []
  persistRecent()
}

const onCreateSuccess = (traceCodes) => {
  if (!traceCodes || traceCodes.length === 0) return
  const newCode = traceCodes[0]
  pushRecent(newCode)
  router.push(`/traces/${newCode}`)
}

const formatTime = (iso) => (iso ? dayjs(iso).format('YYYY-MM-DD HH:mm') : '-')
const formatRelative = (iso) => {
  if (!iso) return '-'
  const now = dayjs()
  const t = dayjs(iso)
  const diffMin = now.diff(t, 'minute')
  if (diffMin < 1) return '刚刚'
  if (diffMin < 60) return `${diffMin} 分钟前`
  const diffHour = now.diff(t, 'hour')
  if (diffHour < 24) return `${diffHour} 小时前`
  const diffDay = now.diff(t, 'day')
  if (diffDay < 7) return `${diffDay} 天前`
  return t.format('YYYY-MM-DD')
}
</script>

<template>
  <div class="trace-list">
    <PageHeader
      title="追溯查询"
      :subtitle="`已访问 ${recentTraces.length} 条追溯码 · 通过码或扫码进入详情，凭证哈希链上自动校验`"
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

    <section class="trace-list__search-card" data-testid="trace-list-search-card">
      <div class="trace-list__search-box">
        <Search class="trace-list__search-icon" />
        <input
          v-model="searchQuery"
          class="trace-list__search-input"
          type="text"
          placeholder="输入追溯码（TC-... / TRC-...）回车进入详情，或在最近访问中过滤"
          spellcheck="false"
          autocomplete="off"
          data-testid="trace-list-search-input"
          @keydown.enter.prevent="handleSearch"
        />
        <KbdShortcut keys="Enter" />
      </div>
      <BaseButton variant="primary" size="sm" data-testid="trace-list-search-submit" @click="handleSearch">
        进入详情
      </BaseButton>
    </section>

    <section class="trace-list__hint">
      <span class="trace-list__hint-eyebrow">说明</span>
      <span class="trace-list__hint-body">
        当前后端尚未开放追溯码分页列表 / 多条件筛选接口，本页用于「按追溯码精确进入详情 + 浏览本机最近访问记录」。
        若需批量筛选，请前往
        <router-link class="trace-list__hint-link" to="/dashboard">仪表盘</router-link>
        查看异常 / 流转中等聚合 KPI，或前往生产赋码工作台按批次查询。
      </span>
    </section>

    <section class="trace-list__recents-card">
      <header class="trace-list__recents-header">
        <div>
          <p class="trace-list__eyebrow">最近访问</p>
          <p class="trace-list__caption">仅存于本设备 localStorage，最多保留 {{ RECENT_MAX }} 条；切换浏览器或清空缓存后重置。</p>
        </div>
        <BaseButton
          v-if="recentTraces.length > 0"
          variant="text"
          size="sm"
          data-testid="trace-list-clear-recent"
          @click="handleClearRecent"
        >
          <template #icon><Trash2 class="trace-list__btn-icon" /></template>
          清空
        </BaseButton>
      </header>

      <div v-if="filteredRecent.length === 0" class="trace-list__empty">
        <EmptyState
          v-if="recentTraces.length === 0"
          :icon="Search"
          title="还没有访问过任何追溯码"
          subtitle="在上方输入框敲入码字符串回车进入详情，访问过的码会自动出现在此处。"
        />
        <EmptyState
          v-else
          :icon="Search"
          title="未匹配到任何追溯码"
          :subtitle="`本机最近访问 ${recentTraces.length} 条中没有包含「${searchQuery}」的码`"
        />
      </div>

      <div v-else class="trace-list__table-wrapper">
        <table class="trace-list__table">
          <thead>
            <tr>
              <th class="trace-list__th">追溯码</th>
              <th class="trace-list__th">访问时间</th>
              <th class="trace-list__th trace-list__th--rel">距今</th>
              <th class="trace-list__th trace-list__th--actions">操作</th>
            </tr>
          </thead>
          <tbody>
            <tr
              v-for="item in filteredRecent"
              :key="item.code"
              class="trace-list__tr"
              data-testid="trace-list-recent-row"
              :data-code="item.code"
              @click="handleRowClick(item)"
            >
              <td class="trace-list__td trace-list__td--code">
                <TraceCodeChip :code="item.code" :copyable="false" size="md" />
              </td>
              <td class="trace-list__td mono trace-list__td--time">{{ formatTime(item.time) }}</td>
              <td class="trace-list__td trace-list__td--rel">{{ formatRelative(item.time) }}</td>
              <td class="trace-list__td trace-list__td--actions" @click.stop>
                <button
                  type="button"
                  class="trace-list__row-action"
                  data-testid="trace-list-recent-remove"
                  :aria-label="`移除 ${item.code}`"
                  @click="handleRemoveRecent(item.code, $event)"
                >
                  <X class="trace-list__btn-icon" />
                </button>
              </td>
            </tr>
          </tbody>
        </table>

        <ul class="trace-list__cards">
          <li
            v-for="item in filteredRecent"
            :key="item.code"
            class="trace-list__card-item"
            data-testid="trace-list-recent-card"
            @click="handleRowClick(item)"
          >
            <div class="trace-list__card-row">
              <TraceCodeChip :code="item.code" :copyable="false" size="md" />
              <button
                type="button"
                class="trace-list__row-action"
                @click="handleRemoveRecent(item.code, $event)"
              >
                <X class="trace-list__btn-icon" />
              </button>
            </div>
            <p class="trace-list__card-meta">
              <span class="mono">{{ formatTime(item.time) }}</span>
              <span class="trace-list__sep">·</span>
              <span>{{ formatRelative(item.time) }}</span>
            </p>
          </li>
        </ul>
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

.trace-list__search-card {
  display: flex;
  align-items: center;
  gap: 8px;
  background: var(--surface-1);
  border: 1px solid var(--hairline);
  border-radius: 12px;
  padding: 12px;
}

.trace-list__search-box {
  flex: 1 1 auto;
  display: flex;
  align-items: center;
  gap: 8px;
  height: 36px;
  padding: 0 12px;
  background: var(--surface-1);
  border: 1px solid var(--hairline);
  border-radius: 8px;
  transition: border-color 0.15s, box-shadow 0.15s;
}
.trace-list__search-box:focus-within {
  border-color: var(--primary-focus);
  box-shadow: 0 0 0 3px rgba(94, 106, 210, 0.15);
}

.trace-list__search-icon {
  width: 14px;
  height: 14px;
  color: var(--ink-tertiary);
  flex-shrink: 0;
}

.trace-list__search-input {
  flex: 1 1 auto;
  border: 0;
  outline: none;
  background: transparent;
  font: inherit;
  font-family: 'JetBrains Mono', ui-monospace, 'SF Mono', Menlo, monospace;
  font-size: 13px;
  color: var(--ink);
  min-width: 0;
}
.trace-list__search-input::placeholder {
  font-family: 'Inter', -apple-system, sans-serif;
  color: var(--ink-tertiary);
}

.trace-list__hint {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  background: var(--primary-soft);
  border: 1px solid #d9def5;
  border-radius: 8px;
  padding: 10px 14px;
  font-size: 12.5px;
  color: var(--ink-muted);
  line-height: 1.5;
}
.trace-list__hint-eyebrow {
  font-size: 10.5px;
  font-weight: 500;
  letter-spacing: 0.4px;
  text-transform: uppercase;
  color: var(--primary);
  background: var(--surface-1);
  border: 1px solid #d9def5;
  border-radius: 4px;
  padding: 1px 6px;
  flex-shrink: 0;
  margin-top: 1px;
}
.trace-list__hint-body {
  flex: 1 1 auto;
}
.trace-list__hint-link {
  color: var(--primary);
  font-weight: 500;
  text-decoration: none;
}
.trace-list__hint-link:hover {
  color: var(--primary-hover);
  text-decoration: underline;
}

.trace-list__recents-card {
  background: var(--surface-1);
  border: 1px solid var(--hairline);
  border-radius: 12px;
  overflow: hidden;
}

.trace-list__recents-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  padding: 16px 20px;
  border-bottom: 1px solid var(--hairline);
}

.trace-list__eyebrow {
  font-size: 11px;
  font-weight: 500;
  letter-spacing: 0.4px;
  text-transform: uppercase;
  color: var(--ink-subtle);
  margin: 0 0 4px 0;
}
.trace-list__caption {
  margin: 0;
  font-size: 12.5px;
  color: var(--ink-subtle);
}

.trace-list__empty {
  padding: 16px 0;
}

.trace-list__table-wrapper {
  width: 100%;
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
  padding: 10px 16px;
  border-bottom: 1px solid var(--hairline);
  background: var(--surface-2);
}
.trace-list__th--rel { width: 120px; }
.trace-list__th--actions { width: 56px; padding-right: 16px; }

.trace-list__tr {
  cursor: pointer;
  transition: background 0.15s;
}
.trace-list__tr:hover .trace-list__td {
  background: var(--surface-2);
}

.trace-list__td {
  padding: 10px 16px;
  border-bottom: 1px solid var(--hairline);
  color: var(--ink);
  vertical-align: middle;
  background: var(--surface-1);
  transition: background 0.15s;
}
.trace-list__tr:last-child .trace-list__td {
  border-bottom: 0;
}
.trace-list__td--time { color: var(--ink-muted); font-size: 12px; }
.trace-list__td--rel { color: var(--ink-subtle); font-size: 12.5px; }
.trace-list__td--actions { padding-right: 16px; text-align: right; }

.trace-list__row-action {
  width: 26px;
  height: 26px;
  display: inline-grid;
  place-items: center;
  background: transparent;
  border: 1px solid transparent;
  border-radius: 6px;
  color: var(--ink-tertiary);
  cursor: pointer;
  transition: background 0.15s, color 0.15s, border-color 0.15s;
}
.trace-list__row-action:hover {
  color: var(--ink);
  background: var(--surface-1);
  border-color: var(--hairline);
}

.trace-list__cards {
  display: none;
}

.mono {
  font-family: 'JetBrains Mono', ui-monospace, 'SF Mono', Menlo, monospace;
}

.trace-list__sep {
  color: var(--ink-tertiary);
  margin: 0 4px;
}

@media (max-width: 768px) {
  .trace-list__search-card {
    flex-direction: column;
    align-items: stretch;
    gap: 8px;
  }
}

@media (max-width: 640px) {
  .trace-list {
    padding: 16px 12px 32px;
  }
  .trace-list__table {
    display: none;
  }
  .trace-list__cards {
    display: flex;
    flex-direction: column;
    gap: 0;
  }
  .trace-list__card-item {
    list-style: none;
    padding: 14px 16px;
    border-bottom: 1px solid var(--hairline);
    cursor: pointer;
    transition: background 0.15s;
  }
  .trace-list__card-item:last-child {
    border-bottom: 0;
  }
  .trace-list__card-item:active {
    background: var(--surface-2);
  }
  .trace-list__card-row {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 8px;
    margin-bottom: 4px;
  }
  .trace-list__card-meta {
    margin: 0;
    font-size: 12px;
    color: var(--ink-subtle);
  }
  .trace-list__hint {
    flex-direction: column;
    gap: 6px;
  }
}
</style>
