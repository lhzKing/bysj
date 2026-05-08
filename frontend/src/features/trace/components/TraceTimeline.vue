<script setup>
import { computed, ref } from 'vue'
import dayjs from 'dayjs'
import StatusPill from '@/shared/components/ui/StatusPill.vue'

const props = defineProps({
  history: {
    type: Array,
    required: true
  },
  view: {
    type: String,
    default: 'effective'
  }
})

const ACTION_LABELS = {
  INIT: '生产赋码',
  INBOUND: '入库登记',
  OUTBOUND: '出库登记',
  TRANSFER: '物流流转',
  PRINT_CODE: '打印标签',
  REPRINT_CODE: '重打标签',
  VOID_CODE: '作废标签',
  ACTIVATE_CODE: '扫码激活',
  PACK: '装箱',
  UNPACK: '拆箱',
  PALLETIZE: '托盘绑定',
  UNPALLETIZE: '托盘解绑',
  EXCEPTION: '异常冻结',
  EXCEPTION_OPEN: '异常冻结',
  EXCEPTION_CLOSE: '异常解除',
  CORRECTION: '审计纠错'
}

const ACTION_TONES = {
  INIT: 'success',
  INBOUND: 'primary',
  OUTBOUND: 'primary',
  TRANSFER: 'primary',
  PRINT_CODE: 'mute',
  REPRINT_CODE: 'warn',
  VOID_CODE: 'error',
  ACTIVATE_CODE: 'success',
  PACK: 'mute',
  UNPACK: 'mute',
  PALLETIZE: 'mute',
  UNPALLETIZE: 'mute',
  EXCEPTION: 'error',
  EXCEPTION_OPEN: 'error',
  EXCEPTION_CLOSE: 'success',
  CORRECTION: 'warn'
}

const expanded = ref(new Set())

const formatDate = (date) => (date ? dayjs(date).format('YYYY-MM-DD HH:mm:ss') : '-')

const formatHash = (hash) => {
  if (!hash) return ''
  if (hash.length <= 24) return hash
  return `${hash.slice(0, 12)} … ${hash.slice(-12)}`
}

const correctedLogIds = computed(() => new Set(
  props.history
    .map((log) => log.correctionOf)
    .filter((id) => id !== null && id !== undefined)
    .map((id) => String(id))
))

const isAuditView = computed(() => props.view === 'audit')

const isCorrectedOriginal = (log) => isAuditView.value && correctedLogIds.value.has(String(log.id))

const isCorrectionRecord = (log) => log.correctionOf !== null && log.correctionOf !== undefined

const getActionLabel = (type) => ACTION_LABELS[type] || type
const getActionTone = (type) => ACTION_TONES[type] || 'mute'

const isExpanded = (id) => expanded.value.has(String(id))

const toggleExpand = (id) => {
  const key = String(id)
  if (expanded.value.has(key)) {
    expanded.value.delete(key)
  } else {
    expanded.value.add(key)
  }
  expanded.value = new Set(expanded.value)
}

const hasHashRow = (log) => Boolean(log.currentHash || log.prevHash || log.signatureKeyId)

const formatLocation = (log) => {
  const parts = [log.province, log.city].filter(Boolean)
  return parts.length ? parts.join(' · ') : ''
}

const formatRoute = (log) => {
  if (!log.fromNode && !log.toNode) return ''
  return `${log.fromNode || '起点'} → ${log.toNode || '目的'}`
}
</script>

<template>
  <div v-if="!history.length" class="trace-timeline trace-timeline--empty">
    <p class="trace-timeline__empty-title">暂无生命周期事件</p>
    <p class="trace-timeline__empty-subtitle">扫码登记后，所有动作会以哈希链方式追加到此处。</p>
  </div>

  <ol v-else class="trace-timeline">
    <li
      v-for="(log, idx) in history"
      :key="log.id"
      class="trace-timeline__node"
      :class="[
        idx === 0 ? 'trace-timeline__node--current' : 'trace-timeline__node--past',
        isCorrectedOriginal(log) && 'trace-timeline__node--corrected-original',
        isCorrectionRecord(log) && 'trace-timeline__node--correction'
      ]"
      :data-corrected-original="isCorrectedOriginal(log) ? 'true' : undefined"
      :data-correction-record="isCorrectionRecord(log) ? 'true' : undefined"
    >
      <div class="trace-timeline__row">
        <span class="trace-timeline__title">{{ getActionLabel(log.actionType) }}</span>
        <StatusPill v-if="idx === 0" tone="primary">当前</StatusPill>
        <StatusPill v-if="isCorrectedOriginal(log)" tone="warn">已被纠错覆盖</StatusPill>
        <StatusPill v-else-if="isCorrectionRecord(log)" tone="primary">修正 #{{ log.correctionOf }}</StatusPill>
        <StatusPill v-else-if="getActionTone(log.actionType) === 'error'" tone="error">{{ getActionLabel(log.actionType) }}</StatusPill>
        <span class="trace-timeline__time mono">{{ formatDate(log.eventTime) }}</span>
      </div>

      <div class="trace-timeline__meta">
        <template v-if="formatLocation(log)">
          <span>{{ formatLocation(log) }}</span>
          <span class="trace-timeline__sep">·</span>
        </template>
        <template v-if="formatRoute(log)">
          <span>{{ formatRoute(log) }}</span>
          <span class="trace-timeline__sep">·</span>
        </template>
        <span>操作员 {{ log.operator || '-' }}</span>
      </div>

      <p v-if="log.remark" class="trace-timeline__remark">{{ log.remark }}</p>

      <button
        v-if="hasHashRow(log)"
        type="button"
        class="trace-timeline__toggle"
        :aria-expanded="isExpanded(log.id)"
        :data-testid="`trace-hash-toggle-${log.id}`"
        @click="toggleExpand(log.id)"
      >
        <svg width="11" height="11" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.4" :class="{ 'trace-timeline__toggle-icon--open': isExpanded(log.id) }">
          <path d="M6 9l6 6 6-6" />
        </svg>
        {{ isExpanded(log.id) ? '收起哈希凭证' : '查看哈希凭证' }}
      </button>

      <div v-if="hasHashRow(log) && isExpanded(log.id)" class="trace-timeline__hash" data-testid="trace-hash-row">
        <div v-if="log.currentHash" class="hash-row">
          <span class="hash-row__label">currentHash</span>
          <span class="hash-row__val mono">{{ formatHash(log.currentHash) }}</span>
        </div>
        <div v-if="log.prevHash" class="hash-row">
          <span class="hash-row__label">prevHash</span>
          <span class="hash-row__val mono">{{ formatHash(log.prevHash) }}</span>
        </div>
        <div v-if="log.signatureKeyId" class="hash-row">
          <span class="hash-row__label">RSA signature</span>
          <span class="hash-row__val mono">
            <span class="hash-row__dot" />
            verified · key {{ log.signatureKeyId }}<span v-if="log.signatureKeyVersion"> · v{{ log.signatureKeyVersion }}</span>
          </span>
        </div>
      </div>

      <div
        v-if="isAuditView && (isCorrectedOriginal(log) || isCorrectionRecord(log))"
        class="trace-timeline__audit"
      >
        <span class="trace-timeline__audit-label">Audit</span>
        <span v-if="isCorrectedOriginal(log)">原始日志 #{{ log.id }} 已被后续 CORRECTION 覆盖，业务有效视图默认隐藏</span>
        <span v-else>本记录修正原始日志 #{{ log.correctionOf }}</span>
      </div>
    </li>
  </ol>
</template>

<style scoped>
.trace-timeline {
  position: relative;
  padding: 0 0 0 28px;
  margin: 0;
  list-style: none;
}
.trace-timeline::before {
  content: '';
  position: absolute;
  left: 9px;
  top: 8px;
  bottom: 4px;
  width: 1px;
  background: var(--hairline);
}

.trace-timeline--empty {
  padding: 32px 24px;
  border: 1px dashed var(--hairline);
  border-radius: 8px;
  background: var(--surface-2);
  text-align: center;
}
.trace-timeline--empty::before { display: none; }
.trace-timeline__empty-title {
  color: var(--ink);
  font-weight: 500;
  font-size: 14px;
  margin: 0 0 4px 0;
}
.trace-timeline__empty-subtitle {
  color: var(--ink-subtle);
  font-size: 12px;
  margin: 0;
}

.trace-timeline__node {
  position: relative;
  padding-bottom: 24px;
}
.trace-timeline__node:last-child {
  padding-bottom: 0;
}
.trace-timeline__node::before {
  content: '';
  position: absolute;
  left: -24px;
  top: 6px;
  width: 14px;
  height: 14px;
  border-radius: 9999px;
  background: var(--surface-1);
  border: 2px solid var(--ink-tertiary);
  box-sizing: border-box;
}
.trace-timeline__node--current::before {
  border-color: var(--primary);
  background: var(--primary);
  box-shadow: 0 0 0 4px rgba(94, 106, 210, 0.12);
}
.trace-timeline__node--past::before {
  border-color: var(--success);
  background: var(--success);
}
.trace-timeline__node--corrected-original::before {
  border-color: var(--warn);
  background: var(--warn);
}
.trace-timeline__node--correction::before {
  border-color: var(--warn);
  background: var(--surface-1);
}

.trace-timeline__row {
  display: flex;
  align-items: baseline;
  gap: 8px;
  flex-wrap: wrap;
  margin-bottom: 4px;
}
.trace-timeline__title {
  font-size: 14px;
  font-weight: 600;
  color: var(--ink);
}
.trace-timeline__time {
  margin-left: auto;
  font-size: 12px;
  color: var(--ink-subtle);
}

.trace-timeline__meta {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: var(--ink-muted);
  margin-bottom: 8px;
}
.trace-timeline__sep {
  color: var(--ink-tertiary);
}

.trace-timeline__remark {
  font-size: 13px;
  color: var(--ink-muted);
  background: var(--surface-2);
  border: 1px solid var(--hairline);
  border-radius: 6px;
  padding: 6px 10px;
  margin: 0 0 8px 0;
}

.trace-timeline__toggle {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: var(--ink-subtle);
  background: transparent;
  border: 0;
  padding: 4px 0;
  cursor: pointer;
  transition: color 0.15s;
}
.trace-timeline__toggle:hover { color: var(--ink); }
.trace-timeline__toggle-icon--open { transform: rotate(180deg); }
.trace-timeline__toggle svg { transition: transform 0.15s; }

.trace-timeline__hash {
  margin-top: 6px;
  display: flex;
  flex-direction: column;
  gap: 6px;
}
.hash-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 8px;
  padding: 4px 8px;
  border-radius: 6px;
  background: var(--surface-2);
  border: 1px solid var(--hairline);
  font-size: 11.5px;
}
.hash-row__label {
  color: var(--ink-subtle);
  font-weight: 500;
  flex-shrink: 0;
}
.hash-row__val {
  color: var(--ink-muted);
  display: inline-flex;
  align-items: center;
  gap: 6px;
  word-break: break-all;
}
.hash-row__dot {
  display: inline-block;
  width: 6px;
  height: 6px;
  border-radius: 9999px;
  background: var(--success);
  flex-shrink: 0;
}

.trace-timeline__audit {
  margin-top: 8px;
  display: flex;
  align-items: baseline;
  gap: 8px;
  font-size: 12px;
  color: var(--warn);
  background: var(--warn-soft);
  border: 1px solid #f9d7a6;
  border-radius: 6px;
  padding: 6px 10px;
}
.trace-timeline__audit-label {
  font-weight: 500;
  font-family: 'JetBrains Mono', ui-monospace, monospace;
  font-size: 10.5px;
  letter-spacing: 0.4px;
  text-transform: uppercase;
}

@media (max-width: 640px) {
  .trace-timeline__time { margin-left: 0; flex-basis: 100%; }
  .hash-row { flex-direction: column; align-items: flex-start; }
}
</style>
