<script setup>
/**
 * TracePublicView —— 匿名（无需登录）追溯码自助查验视图。
 *
 * 路由：/public/traces/:code
 * 数据源：GET /api/public/traces/{code}（PublicTraceController，已脱敏）
 *
 * 视觉契约：与 Login 同源的独立全屏页（无 MainLayout 侧栏 / 顶栏），
 * 顶部 trace. 品牌 + 「← 返回登录」， 主区域单卡居中：
 *   1. Header：trace 码 + 状态徽章 + 最近更新
 *   2. 链上完整性大徽章（valid 时绿色 / invalid 时红色，是匿名用户最关心的「这个码是真的吗」）
 *   3. 简化时间轴：动作中文名 + 时间 + 省份/城市（无操作员、无内部节点 ID）
 *   4. CTA：「下载 RSA 公钥独立验签」 + 「登录工业溯源系统」
 */
import { onMounted, ref, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ChevronLeft, Shield, ShieldAlert, Download, LogIn, Clock, MapPin } from 'lucide-vue-next'
import { getPublicTrace } from '@/core/api/publicTrace'
import { logger } from '@/shared/utils/logger'

const route = useRoute()
const router = useRouter()

const loading = ref(true)
const error = ref('')
const data = ref(null)

const traceCode = computed(() => String(route.params.code || '').trim())

const chainValid = computed(() => data.value?.chainVerify?.valid === true)
const chainSummary = computed(() => {
  const v = data.value?.chainVerify
  if (!v) return ''
  return `${v.hashVerifiedCount} / ${v.totalLogs} 条哈希链 · ${v.signatureVerifiedCount} / ${v.totalLogs} 条 RSA 签名`
})

const statusLabel = (status) => {
  const map = {
    INIT: '已初始化',
    IN_STOCK: '在库',
    IN_TRANSIT: '运输中',
    TRANSFERRED: '已交付',
    EXCEPTION: '异常'
  }
  return map[status] || status || '-'
}

const formatTime = (iso) => {
  if (!iso) return '-'
  // Backend returns ISO-8601 like "2026-05-10T01:07:55"
  const t = new Date(iso)
  if (Number.isNaN(t.getTime())) return iso
  const pad = (n) => String(n).padStart(2, '0')
  return `${t.getFullYear()}-${pad(t.getMonth() + 1)}-${pad(t.getDate())} ${pad(t.getHours())}:${pad(t.getMinutes())}`
}

const goLogin = () => router.push('/login')
const goBack = () => {
  if (window.history.length > 1) router.back()
  else router.push('/login')
}

onMounted(async () => {
  if (!traceCode.value) {
    loading.value = false
    error.value = '缺少追溯码'
    return
  }
  try {
    data.value = await getPublicTrace(traceCode.value)
  } catch (e) {
    logger.error('Public trace lookup failed:', e)
    if (e?.response?.status === 404 || e?.response?.data?.code === 30001) {
      error.value = '该追溯码不存在，或已被作废'
    } else if (e?.request && !e?.response) {
      error.value = '网络连接失败，请检查后重试'
    } else {
      error.value = e?.response?.data?.message || e?.message || '查询失败，请稍后重试'
    }
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <div class="trace-public-page">
    <header class="trace-public__topbar">
      <button class="trace-public__back" type="button" @click="goBack">
        <ChevronLeft :size="14" />
        返回登录
      </button>
      <a class="trace-public__brand">
        <span class="trace-public__brand-logo" aria-hidden="true">
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="white" stroke-width="2.4" stroke-linecap="round">
            <path d="M3 9h18M3 15h18M9 3v18M15 3v18" />
          </svg>
        </span>
        <span class="trace-public__brand-text">trace.</span>
      </a>
      <a class="trace-public__pubkey" href="/api/traces/public-key" target="_blank" rel="noopener">
        <Download :size="13" />
        下载 RSA 公钥
      </a>
    </header>

    <main class="trace-public__main">
      <div class="trace-public__shell">
        <p class="trace-public__eyebrow">外部审计 · 无需登录</p>
        <h1 class="trace-public__title">追溯码自助验签</h1>
        <p class="trace-public__subtitle">链上哈希 + RSA 数字签名实时校验，无需注册即可独立验证产品真实性。</p>

        <!-- Loading -->
        <div v-if="loading" class="trace-public__card trace-public__loading">
          <div class="trace-public__spinner" aria-hidden="true"></div>
          <p>正在查询并验签 {{ traceCode }} …</p>
        </div>

        <!-- Error -->
        <div v-else-if="error" class="trace-public__card trace-public__error">
          <ShieldAlert :size="32" class="trace-public__error-icon" />
          <h2>无法查验</h2>
          <p>{{ error }}</p>
          <p class="trace-public__error-code">追溯码：<code>{{ traceCode }}</code></p>
          <div class="trace-public__error-actions">
            <button type="button" class="trace-public__btn trace-public__btn--secondary" @click="goBack">返回</button>
            <button type="button" class="trace-public__btn trace-public__btn--primary" @click="goLogin">
              <LogIn :size="14" />
              登录后查询
            </button>
          </div>
        </div>

        <!-- Detail -->
        <template v-else>
          <!-- Chain integrity hero badge -->
          <section
            class="trace-public__hero"
            :class="chainValid ? 'trace-public__hero--ok' : 'trace-public__hero--bad'"
          >
            <component :is="chainValid ? Shield : ShieldAlert" :size="36" class="trace-public__hero-icon" />
            <div class="trace-public__hero-text">
              <h2 class="trace-public__hero-title">
                {{ chainValid ? '链上完整 · 验签通过' : '链上校验失败' }}
              </h2>
              <p class="trace-public__hero-summary">{{ chainSummary }}</p>
              <p class="trace-public__hero-algo">RSA-2048 · SHA-256 链式哈希 · 校验时间 {{ formatTime(data.chainVerify?.verifyTime) }}</p>
            </div>
          </section>

          <!-- Trace meta card -->
          <section class="trace-public__card trace-public__meta">
            <div class="trace-public__meta-row">
              <span class="trace-public__meta-label">追溯码</span>
              <code class="trace-public__meta-code">{{ data.traceCode }}</code>
            </div>
            <div class="trace-public__meta-grid">
              <div class="trace-public__meta-cell">
                <span class="trace-public__meta-label">配件名称</span>
                <span class="trace-public__meta-value">{{ data.spuName || '-' }}</span>
              </div>
              <div class="trace-public__meta-cell">
                <span class="trace-public__meta-label">当前状态</span>
                <span class="trace-public__meta-value">{{ statusLabel(data.currentStatus) }}</span>
              </div>
              <div class="trace-public__meta-cell">
                <span class="trace-public__meta-label">当前位置</span>
                <span class="trace-public__meta-value">
                  {{ data.currentProvince || data.currentCity ? `${data.currentProvince || ''} · ${data.currentCity || ''}` : '-' }}
                </span>
              </div>
              <div class="trace-public__meta-cell">
                <span class="trace-public__meta-label">最近更新</span>
                <span class="trace-public__meta-value">{{ formatTime(data.lastUpdateTime) }}</span>
              </div>
            </div>
          </section>

          <!-- Timeline -->
          <section class="trace-public__card trace-public__timeline-card">
            <header class="trace-public__timeline-header">
              <h3>流转时间轴</h3>
              <span class="trace-public__timeline-count">{{ data.totalEvents }} 条已验签事件</span>
            </header>
            <ol v-if="data.events?.length" class="trace-public__timeline">
              <li v-for="(ev, i) in data.events" :key="i" class="trace-public__event">
                <span class="trace-public__event-dot" aria-hidden="true"></span>
                <div class="trace-public__event-body">
                  <p class="trace-public__event-action">{{ ev.actionLabel }}</p>
                  <p class="trace-public__event-meta">
                    <Clock :size="12" />
                    {{ formatTime(ev.eventTime) }}
                    <span v-if="ev.province || ev.city" class="trace-public__event-loc">
                      <MapPin :size="12" />
                      {{ ev.province || '' }}{{ ev.city ? ' · ' + ev.city : '' }}
                    </span>
                  </p>
                </div>
              </li>
            </ol>
            <p v-else class="trace-public__empty">暂无可展示的流转事件</p>
          </section>

          <!-- CTA -->
          <div class="trace-public__cta-row">
            <button type="button" class="trace-public__btn trace-public__btn--secondary" @click="goBack">
              返回
            </button>
            <button type="button" class="trace-public__btn trace-public__btn--primary" @click="goLogin">
              <LogIn :size="14" />
              登录工业溯源系统
            </button>
          </div>
        </template>
      </div>
    </main>

    <footer class="trace-public__footer">
      <span>© 2026 工业零配件溯源 · 公开验签入口</span>
      <span>v2.4.1</span>
    </footer>
  </div>
</template>

<style scoped>
.trace-public-page {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background: var(--surface-bg, #f7f8fa);
  color: var(--ink, #1f2330);
}

.trace-public__topbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 16px 24px;
  border-bottom: 1px solid var(--hairline, #e6e6e8);
  background: var(--surface-1, #ffffff);
}
.trace-public__back {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 6px 10px;
  background: transparent;
  border: 1px solid var(--hairline, #e6e6e8);
  border-radius: 6px;
  color: var(--ink-subtle, #6b6f80);
  font-size: 12.5px;
  cursor: pointer;
}
.trace-public__back:hover { background: var(--surface-2, #f0f1f5); color: var(--ink); }
.trace-public__brand {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  font-weight: 600;
  color: var(--ink);
}
.trace-public__brand-logo {
  width: 24px; height: 24px;
  background: var(--primary, #5e6ad2);
  border-radius: 6px;
  display: inline-flex; align-items: center; justify-content: center;
}
.trace-public__pubkey {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 6px 10px;
  border: 1px solid var(--hairline);
  border-radius: 6px;
  font-size: 12.5px;
  color: var(--ink-subtle);
  text-decoration: none;
}
.trace-public__pubkey:hover { background: var(--surface-2); color: var(--ink); }

.trace-public__main { flex: 1 1 auto; padding: 32px 24px; }
.trace-public__shell { max-width: 720px; margin: 0 auto; }

.trace-public__eyebrow {
  font-size: 11.5px;
  letter-spacing: 0.06em;
  text-transform: uppercase;
  color: var(--primary, #5e6ad2);
  margin: 0 0 6px;
}
.trace-public__title {
  font-size: 28px;
  font-weight: 600;
  letter-spacing: -0.4px;
  margin: 0 0 6px;
  color: var(--ink);
}
.trace-public__subtitle {
  font-size: 13px;
  color: var(--ink-subtle);
  margin: 0 0 24px;
}

.trace-public__card {
  background: var(--surface-1, #fff);
  border: 1px solid var(--hairline, #e6e6e8);
  border-radius: 12px;
  padding: 20px 24px;
  margin-bottom: 16px;
}

.trace-public__loading {
  display: flex; align-items: center; gap: 12px;
  color: var(--ink-subtle);
}
.trace-public__spinner {
  width: 18px; height: 18px;
  border: 2px solid var(--hairline);
  border-top-color: var(--primary);
  border-radius: 50%;
  animation: trace-public-spin 0.8s linear infinite;
}
@keyframes trace-public-spin { to { transform: rotate(360deg); } }

.trace-public__error {
  text-align: center;
  padding: 32px 24px;
}
.trace-public__error-icon { color: #e5484d; margin-bottom: 12px; }
.trace-public__error h2 { margin: 0 0 4px; font-size: 18px; }
.trace-public__error p { margin: 4px 0; color: var(--ink-subtle); font-size: 13px; }
.trace-public__error-code code {
  font-family: 'JetBrains Mono', ui-monospace, monospace;
  font-size: 12px;
  background: var(--surface-2);
  padding: 2px 6px;
  border-radius: 4px;
}
.trace-public__error-actions { margin-top: 16px; display: flex; gap: 8px; justify-content: center; }

.trace-public__hero {
  border-radius: 12px;
  padding: 20px 24px;
  margin-bottom: 16px;
  display: flex;
  align-items: center;
  gap: 16px;
  border: 1px solid;
}
.trace-public__hero--ok {
  background: rgba(39, 166, 68, 0.06);
  border-color: rgba(39, 166, 68, 0.3);
}
.trace-public__hero--ok .trace-public__hero-icon { color: #27a644; }
.trace-public__hero--bad {
  background: rgba(229, 72, 77, 0.06);
  border-color: rgba(229, 72, 77, 0.3);
}
.trace-public__hero--bad .trace-public__hero-icon { color: #e5484d; }
.trace-public__hero-title { margin: 0 0 2px; font-size: 16px; font-weight: 600; }
.trace-public__hero-summary { margin: 0; font-size: 13px; color: var(--ink); }
.trace-public__hero-algo { margin: 4px 0 0; font-size: 11.5px; color: var(--ink-subtle); }

.trace-public__meta-row {
  display: flex;
  align-items: center;
  gap: 12px;
  padding-bottom: 16px;
  margin-bottom: 16px;
  border-bottom: 1px solid var(--hairline);
}
.trace-public__meta-code {
  font-family: 'JetBrains Mono', ui-monospace, monospace;
  font-size: 13px;
  background: var(--surface-2, #f0f1f5);
  padding: 4px 8px;
  border-radius: 6px;
  color: var(--ink);
}
.trace-public__meta-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}
.trace-public__meta-cell { display: flex; flex-direction: column; gap: 2px; }
.trace-public__meta-label {
  font-size: 11.5px;
  color: var(--ink-subtle);
}
.trace-public__meta-value {
  font-size: 14px;
  color: var(--ink);
  font-weight: 500;
}

.trace-public__timeline-header {
  display: flex; align-items: baseline; justify-content: space-between;
  margin-bottom: 16px;
}
.trace-public__timeline-header h3 { margin: 0; font-size: 14px; font-weight: 600; }
.trace-public__timeline-count { font-size: 12px; color: var(--ink-subtle); }

.trace-public__timeline {
  list-style: none;
  margin: 0;
  padding: 0 0 0 16px;
  border-left: 1px solid var(--hairline);
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.trace-public__event { position: relative; }
.trace-public__event-dot {
  position: absolute;
  left: -22px;
  top: 6px;
  width: 10px; height: 10px;
  background: var(--primary, #5e6ad2);
  border-radius: 50%;
  border: 2px solid var(--surface-1);
  box-shadow: 0 0 0 1px var(--primary);
}
.trace-public__event-action { margin: 0; font-size: 13.5px; font-weight: 500; color: var(--ink); }
.trace-public__event-meta {
  margin: 4px 0 0;
  font-size: 12px;
  color: var(--ink-subtle);
  display: flex;
  align-items: center;
  gap: 6px;
  flex-wrap: wrap;
}
.trace-public__event-loc { display: inline-flex; align-items: center; gap: 4px; }

.trace-public__empty { color: var(--ink-subtle); font-size: 13px; }

.trace-public__cta-row {
  display: flex;
  gap: 8px;
  justify-content: flex-end;
  margin-top: 24px;
}
.trace-public__btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 8px 14px;
  border-radius: 8px;
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  border: 1px solid transparent;
}
.trace-public__btn--secondary {
  background: var(--surface-1);
  border-color: var(--hairline);
  color: var(--ink);
}
.trace-public__btn--secondary:hover { background: var(--surface-2); }
.trace-public__btn--primary {
  background: var(--primary, #5e6ad2);
  color: #ffffff;
}
.trace-public__btn--primary:hover { background: var(--primary-strong, #4f59c2); }

.trace-public__footer {
  display: flex;
  justify-content: space-between;
  padding: 16px 24px;
  border-top: 1px solid var(--hairline);
  background: var(--surface-1);
  font-size: 11.5px;
  color: var(--ink-subtle);
}

@media (max-width: 639.98px) {
  .trace-public__topbar { padding: 12px 16px; }
  .trace-public__main { padding: 16px; }
  .trace-public__title { font-size: 22px; }
  .trace-public__hero { flex-direction: column; align-items: flex-start; padding: 16px; }
  .trace-public__meta-grid { grid-template-columns: 1fr; }
  .trace-public__cta-row { flex-direction: column-reverse; }
  .trace-public__cta-row .trace-public__btn { width: 100%; justify-content: center; }
  .trace-public__pubkey span { display: none; }
}
</style>
