<script setup>
/**
 * ScanHub —— 扫码工位（Linear-light）
 *
 * 视觉契约：frontend/preview/linear-scan.html 三状态 1:1
 *   - 默认 · 等待扫码：左 2/3 主 CTA 卡 + 右 1/3 今日 KPI 三联
 *   - 取景中 · 等待识别：左 2/3 嵌入 <QRScanner inline /> dark surface + 右 1/3 设备健康 + 本次会话
 *   - 已识别 · 选择登记动作：顶部追溯码 hero 条 + 下方 5/4/3 三栏（配件 / 动作 / 流转记录）
 *
 * 接口契约：api-doc.md 2.5 / 2.6
 *   - 扫码后 GET /api/traces/{code}/available-actions → 渲染 availableActions[]
 *   - 选定动作后由对应 Dialog 调 POST /api/traces/{code}/events，请求体含 idempotencyKey
 *   - idempotencyKey 在本视图于动作选定瞬间用 crypto.randomUUID() 生成，作为 prop 透传
 *
 * 键盘 / 触屏：
 *   - 默认态：Space 启动摄像头
 *   - 取景态：Esc 取消（QRScanner inline 模式自带 Esc 提示和 × 关闭按钮，与 modal 视觉一致；
 *     本视图额外监听页面级 Esc 作为兜底）
 *   - 已识别态：F1-F4 绑定前 4 个可执行动作；Esc 重置回默认态
 */
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import {
  ArrowLeft,
  Camera,
  Factory,
  Keyboard,
  PackageOpen,
  PackagePlus,
  ShieldAlert,
  Truck,
  ScanLine
} from 'lucide-vue-next'
import { useUserStore } from '@/core/stores/user'
import { useToast } from '@/shared/composables/useToast'
import { logger } from '@/shared/utils/logger'
import { PERMISSIONS } from '@/shared/constants'
import { getTraceAvailableActions } from '@/features/trace/api'
import BaseButton from '@/shared/components/ui/BaseButton.vue'
import BaseCard from '@/shared/components/ui/BaseCard.vue'
import BaseInput from '@/shared/components/ui/BaseInput.vue'
import KbdShortcut from '@/shared/components/ui/KbdShortcut.vue'
import StatusPill from '@/shared/components/ui/StatusPill.vue'
import TraceCodeChip from '@/shared/components/ui/TraceCodeChip.vue'
import PageHeader from '@/shared/components/ui/PageHeader.vue'
import EmptyState from '@/shared/components/ui/EmptyState.vue'
import LoadingSkeleton from '@/shared/components/ui/LoadingSkeleton.vue'
import QRScanner from '@/shared/components/QRScanner.vue'
import ScanFlowDialog from '../components/ScanFlowDialog.vue'
import ScanExceptionDialog from '../components/ScanExceptionDialog.vue'
import CreateTraceDialog from '../components/CreateTraceDialog.vue'

const router = useRouter()
const userStore = useUserStore()
const toast = useToast()

const STATE = {
  Idle: 'idle',
  Scanning: 'scanning',
  Identified: 'identified'
}

const stage = ref(STATE.Idle)
const scannedCode = ref('')
const manualCode = ref('')
const availableActionsLoading = ref(false)
const actionDecision = ref(null)
const availableActionsError = ref('')

const showFlowDialog = ref(false)
const showExceptionDialog = ref(false)
const showCreateModal = ref(false)
const flowAction = ref('')
const pendingIdempotencyKey = ref('')

const sessionStartedAt = ref(null)

const permissions = computed(() => userStore.user?.permissions || [])
const hasPermission = (perm) => permissions.value.includes(perm)
const hasAnyPermission = (required = []) => required.some((perm) => permissions.value.includes(perm))

const actionTypeToDialog = {
  INBOUND: 'inbound',
  OUTBOUND: 'outbound',
  TRANSFER: 'transfer',
  DELIVER: 'deliver'
}

const labelMap = {
  INBOUND: '入库登记',
  OUTBOUND: '出库登记',
  TRANSFER: '中转流转',
  DELIVER: '最终交付',
  EXCEPTION: '异常上报',
  EXCEPTION_OPEN: '异常上报',
  EXCEPTION_CLOSE: '解除冻结',
  CORRECTION: '审计纠错'
}

const iconMap = {
  INBOUND: PackagePlus,
  OUTBOUND: PackageOpen,
  TRANSFER: Truck,
  DELIVER: ScanLine,
  EXCEPTION: ShieldAlert,
  EXCEPTION_OPEN: ShieldAlert,
  EXCEPTION_CLOSE: ShieldAlert,
  CORRECTION: ShieldAlert
}

const executableActions = computed(() => actionDecision.value?.availableActions || [])

const recommendedActionType = computed(() => actionDecision.value?.recommendedAction || '')

const orderedActions = computed(() => {
  const items = [...executableActions.value]
  if (!recommendedActionType.value) return items
  items.sort((a, b) => {
    if (a.actionType === recommendedActionType.value) return -1
    if (b.actionType === recommendedActionType.value) return 1
    return 0
  })
  return items
})

const noActionMessage = computed(
  () =>
    availableActionsError.value ||
    actionDecision.value?.noActionReason ||
    '当前状态、权限或节点不允许执行扫码动作。'
)

/**
 * 把后端 noActionReason 翻译成更直白的解释 + 推荐演示账号。
 * 后端 reason 来自 TraceAvailableActionService.buildNoActionReason，4 种 pattern：
 *   - "当前用户未绑定当前可操作节点…"          → 维度 5 节点绑定 / 维度 4 角色权限
 *   - "当前角色没有该状态下的扫码动作权限…"     → 维度 4 角色权限
 *   - "当前状态 X 无常规可执行动作…"            → 维度 3 状态机
 *   - "当前请求缺少角色上下文…"                 → 鉴权/会话异常
 *
 * 前端不重复后端原句，而是给一段 "为什么 + 怎么做" 的解读 + 角色建议。
 */
const noActionGuidance = computed(() => {
  if (availableActionsError.value) return null
  const reason = actionDecision.value?.noActionReason || ''
  const status = actionDecision.value?.currentStatusLabel || actionDecision.value?.currentStatus || ''
  const username = userStore.user?.username || ''

  if (reason.includes('未绑定') || reason.includes('节点扫码动作')) {
    return {
      kind: 'role-or-node',
      hint: `当前账号 ${username || '此用户'} 在「${status || '当前状态'}」下没有可在该节点执行的扫码动作（角色权限或节点绑定限制）。`,
      tip: '不同业务角色负责不同动作：入库 / 出库由 warehouse、中转流转 / 最终交付由 logistics、生产赋码由 producer。请按业务职责切换演示账号后重试。'
    }
  }
  if (reason.includes('角色没有') || reason.includes('扫码动作权限')) {
    return {
      kind: 'role',
      hint: `当前角色在「${status || '此状态'}」下没有任何扫码动作权限。`,
      tip: '入库 / 出库需 warehouse 账号；中转流转 / 最终交付需 logistics 账号；生产赋码需 producer 账号。'
    }
  }
  if (reason.includes('无常规可执行动作') || reason.includes('状态')) {
    return {
      kind: 'state',
      hint: `「${status || '当前状态'}」是终点状态或异常冻结态，状态机不允许常规扫码动作。`,
      tip: '如需纠正历史记录请进入溯源详情的审计纠错流程；异常冻结请用相应账号执行解冻。'
    }
  }
  if (reason.includes('角色上下文')) {
    return {
      kind: 'auth',
      hint: '会话异常：未识别到角色信息。',
      tip: '请退出后重新登录，或联系管理员检查权限分配。'
    }
  }
  return null
})

const showAssignmentEntry = computed(() => hasAnyPermission(PERMISSIONS.TRACE.ASSIGNMENT_ACCESS))

const isRecommended = (action) => Boolean(action?.actionType) && action.actionType === recommendedActionType.value

function actionLabel(action) {
  if (!action) return ''
  return action.label || labelMap[action.actionType] || action.actionType || '未知动作'
}

function startCamera() {
  stage.value = STATE.Scanning
  sessionStartedAt.value = new Date()
  availableActionsError.value = ''
  actionDecision.value = null
}

function stopCamera() {
  stage.value = STATE.Idle
  sessionStartedAt.value = null
}

function resetToIdle() {
  stage.value = STATE.Idle
  scannedCode.value = ''
  actionDecision.value = null
  availableActionsError.value = ''
  flowAction.value = ''
  pendingIdempotencyKey.value = ''
}

async function handleScan(code) {
  const trimmed = String(code || '').trim()
  if (!trimmed) return
  scannedCode.value = trimmed
  stage.value = STATE.Identified
  await loadAvailableActions(trimmed)
}

function submitManual() {
  const trimmed = manualCode.value.trim()
  if (!trimmed) {
    toast.error('请输入溯源码')
    return
  }
  manualCode.value = ''
  handleScan(trimmed)
}

async function loadAvailableActions(traceCode) {
  availableActionsLoading.value = true
  availableActionsError.value = ''
  actionDecision.value = null
  try {
    actionDecision.value = await getTraceAvailableActions(traceCode)
  } catch (error) {
    logger.error('加载扫码可执行动作失败', error)
    availableActionsError.value = error?.message || '加载扫码可执行动作失败，请稍后重试'
    toast.error(availableActionsError.value)
  } finally {
    availableActionsLoading.value = false
  }
}

function generateIdempotencyKey() {
  if (typeof crypto !== 'undefined' && typeof crypto.randomUUID === 'function') {
    return crypto.randomUUID()
  }
  return `scn-${Date.now().toString(36)}-${Math.random().toString(36).slice(2, 10)}`
}

function pickAction(action) {
  if (!action?.actionType) return
  pendingIdempotencyKey.value = generateIdempotencyKey()
  const actionType = action.actionType
  if (actionType === 'EXCEPTION' || actionType === 'EXCEPTION_OPEN') {
    showExceptionDialog.value = true
    return
  }
  if (actionType === 'EXCEPTION_CLOSE') {
    toast.error('解除异常冻结请进入溯源详情的异常处理流程')
    return
  }
  if (actionType === 'CORRECTION') {
    toast.error('审计纠错请进入溯源详情的审计流程')
    return
  }
  const dialogAction = actionTypeToDialog[actionType]
  if (!dialogAction) {
    toast.error(`当前前端暂不支持动作: ${actionType}`)
    return
  }
  flowAction.value = dialogAction
  showFlowDialog.value = true
}

function onFlowSuccess() {
  showFlowDialog.value = false
  showExceptionDialog.value = false
  scannedCode.value = ''
  actionDecision.value = null
  flowAction.value = ''
  pendingIdempotencyKey.value = ''
  // 流水线扫码模式：800ms 后自动重新启动摄像头方便连续扫下一个零件。
  // 这里 toast 提示一下，避免用户误以为提交失败回到扫码态。
  toast.success('已提交并上链，准备扫描下一个零件…按 Esc 可取消连扫')
  setTimeout(() => {
    startCamera()
  }, 800)
}

function handleViewDetail() {
  if (!scannedCode.value) return
  router.push(`/traces/${scannedCode.value}`)
}

function openCreateTrace() {
  showCreateModal.value = true
}

function onCreateTraceSuccess(traceCodes) {
  showCreateModal.value = false
  const firstCode = Array.isArray(traceCodes) ? traceCodes[0] : null
  if (firstCode) {
    router.push(`/traces/${firstCode}`)
  }
}

function handleKeydown(event) {
  if (event.defaultPrevented) return
  const tag = event.target?.tagName
  if (tag === 'INPUT' || tag === 'TEXTAREA' || tag === 'SELECT') return
  if (showFlowDialog.value || showExceptionDialog.value || showCreateModal.value) return

  if (stage.value === STATE.Idle && event.code === 'Space' && !event.repeat) {
    event.preventDefault()
    startCamera()
    return
  }
  if (stage.value === STATE.Scanning && event.key === 'Escape') {
    event.preventDefault()
    stopCamera()
    return
  }
  if (stage.value === STATE.Identified) {
    if (event.key === 'Escape') {
      event.preventDefault()
      resetToIdle()
      return
    }
    const fnMatch = /^F([1-4])$/.exec(event.key)
    if (fnMatch && !availableActionsLoading.value) {
      const idx = Number(fnMatch[1]) - 1
      const target = orderedActions.value[idx]
      if (target) {
        event.preventDefault()
        pickAction(target)
      }
    }
  }
}

onMounted(() => {
  document.addEventListener('keydown', handleKeydown)
})

onUnmounted(() => {
  document.removeEventListener('keydown', handleKeydown)
})
</script>

<template>
  <div class="scan-hub">
    <PageHeader title="扫码工位" subtitle="将零件二维码对准摄像头取景框，每次扫码以 RSA 数字签名上链。">
      <template #actions>
        <span v-if="stage === STATE.Idle" class="scan-hub__hint">
          <KbdShortcut keys="空格" /> 启动摄像头 · USB 扫码枪可直接触发
        </span>
        <span v-else-if="stage === STATE.Scanning" class="scan-hub__hint">
          <StatusPill tone="error">
            <template #default>取景中</template>
          </StatusPill>
          <KbdShortcut keys="Esc" /> 取消
        </span>
        <span v-else class="scan-hub__hint">
          <StatusPill tone="primary">已识别</StatusPill>
          <KbdShortcut keys="F1-F4" /> 选择动作 · <KbdShortcut keys="Esc" /> 重置
        </span>
      </template>
    </PageHeader>

    <!-- ============ 默认 · 等待扫码 ============ -->
    <section v-if="stage === STATE.Idle" class="scan-hub__grid scan-hub__grid--default">
      <BaseCard padding="lg" class="scan-hub__cta">
        <div class="scan-hub__cta-eyebrow">默认 · 等待扫码</div>
        <h2 class="scan-hub__cta-title">扫一次</h2>
        <p class="scan-hub__cta-text">
          支持摄像头、USB 扫码枪与手动输入。每次扫码先调用 <span class="mono">/available-actions</span>，再由系统
          自动判断当前可执行动作并附带幂等键。
        </p>
        <div class="scan-hub__cta-row">
          <BaseButton variant="primary" size="md" data-test="scan-start" @click="startCamera">
            <template #icon><ScanLine :size="14" /></template>
            启动摄像头
            <template #kbd>空格</template>
          </BaseButton>
          <form class="scan-hub__manual" data-test="scan-manual-form" @submit.prevent="submitManual">
            <BaseInput
              v-model="manualCode"
              size="sm"
              placeholder="或粘贴 / 手动输入溯源码"
              autocomplete="off"
              :icon="Keyboard"
              data-test="scan-manual-input"
            />
            <BaseButton variant="secondary" size="sm" type="submit" data-test="scan-manual-submit">
              提交
            </BaseButton>
          </form>
        </div>
        <div v-if="showAssignmentEntry" class="scan-hub__cta-extra">
          <span class="scan-hub__cta-extra-label">生产线赋码、批量入库新批次：</span>
          <button
            type="button"
            class="scan-hub__link"
            data-test="scan-open-assignment"
            @click="openCreateTrace"
          >
            <Factory :size="12" />
            或者：直接生产赋码
          </button>
        </div>
      </BaseCard>

      <BaseCard padding="md" class="scan-hub__kpi">
        <div class="scan-hub__kpi-eyebrow">今日 · 本工位</div>
        <div class="scan-hub__kpi-grid">
          <div>
            <div class="scan-hub__kpi-num">0</div>
            <div class="scan-hub__kpi-label">入库</div>
          </div>
          <div>
            <div class="scan-hub__kpi-num">0</div>
            <div class="scan-hub__kpi-label">出库</div>
          </div>
          <div>
            <div class="scan-hub__kpi-num scan-hub__kpi-num--error">0</div>
            <div class="scan-hub__kpi-label scan-hub__kpi-label--error">异常</div>
          </div>
        </div>
        <p class="scan-hub__kpi-foot">数据来自 <span class="mono">/dashboard/kpi</span>，工作台实操后会即时刷新。</p>
      </BaseCard>
    </section>

    <!-- ============ 取景中 · 等待识别 ============ -->
    <section v-else-if="stage === STATE.Scanning" class="scan-hub__grid scan-hub__grid--scanning">
      <div class="scan-hub__viewport" data-test="scan-viewport">
        <QRScanner inline @scan="handleScan" @close="stopCamera" />
      </div>
      <div class="scan-hub__sidecar">
        <BaseCard padding="md">
          <div class="scan-hub__side-eyebrow">本次会话</div>
          <dl class="scan-hub__kv">
            <div class="scan-hub__kv-row">
              <dt>状态</dt>
              <dd><StatusPill tone="primary">取景中</StatusPill></dd>
            </div>
            <div class="scan-hub__kv-row">
              <dt>开始</dt>
              <dd v-if="sessionStartedAt" class="mono">
                {{
                  sessionStartedAt
                    .toTimeString()
                    .slice(0, 8)
                }}
              </dd>
              <dd v-else>—</dd>
            </div>
          </dl>
        </BaseCard>
        <BaseCard padding="md">
          <div class="scan-hub__side-eyebrow">扫码提示</div>
          <ul class="scan-hub__tips">
            <li>对准取景框 4 个白色角标，距离 15-30 cm。</li>
            <li>使用 USB 扫码枪时焦点会自动落在本页隐藏 input。</li>
            <li>权限不足或证书无效会显示降级提示并跳转排障指南。</li>
          </ul>
        </BaseCard>
      </div>
    </section>

    <!-- ============ 已识别 · 选择登记动作 ============ -->
    <section v-else class="scan-hub__identified">
      <BaseCard padding="md" class="scan-hub__hero">
        <div class="scan-hub__hero-left">
          <button
            type="button"
            class="scan-hub__hero-back"
            data-test="scan-reset"
            @click="resetToIdle"
          >
            <ArrowLeft :size="13" />
            重新扫描
          </button>
          <span class="scan-hub__hero-eyebrow">追溯码</span>
          <TraceCodeChip
            :code="scannedCode"
            size="xl"
            data-test="scanned-code"
          />
          <StatusPill v-if="actionDecision?.currentStatusLabel" tone="success">
            {{ actionDecision.currentStatusLabel }}
          </StatusPill>
          <StatusPill v-else-if="actionDecision?.currentStatus" tone="primary">
            {{ actionDecision.currentStatus }}
          </StatusPill>
        </div>
        <div class="scan-hub__hero-right">
          <BaseButton variant="secondary" size="sm" data-test="scan-view-detail" @click="handleViewDetail">
            查看溯源详情
          </BaseButton>
          <BaseButton
            variant="text"
            size="sm"
            data-test="available-actions-refresh"
            :disabled="availableActionsLoading"
            @click="loadAvailableActions(scannedCode)"
          >
            刷新判断
          </BaseButton>
        </div>
      </BaseCard>

      <div class="scan-hub__grid scan-hub__grid--identified">
        <!-- 配件信息（5/12） -->
        <BaseCard padding="md" class="scan-hub__panel scan-hub__panel--info">
          <div class="scan-hub__side-eyebrow">配件 · 当前节点</div>
          <dl class="scan-hub__kv">
            <div class="scan-hub__kv-row">
              <dt>追溯码</dt>
              <dd><TraceCodeChip :code="scannedCode" size="md" /></dd>
            </div>
            <div class="scan-hub__kv-row">
              <dt>当前状态</dt>
              <dd data-test="available-current-status">
                {{ actionDecision?.currentStatusLabel || actionDecision?.currentStatus || '—' }}
              </dd>
            </div>
            <div class="scan-hub__kv-row">
              <dt>当前节点</dt>
              <dd data-test="available-current-node">{{ actionDecision?.currentNode || '—' }}</dd>
            </div>
          </dl>
          <p class="scan-hub__panel-foot">
            完整配件资料、生产厂、批次、累计流转次数请进入
            <button type="button" class="scan-hub__link-inline" @click="handleViewDetail">溯源详情</button>。
          </p>
        </BaseCard>

        <!-- 动作面板（4/12） -->
        <BaseCard padding="md" class="scan-hub__panel scan-hub__panel--actions" data-test="scan-action-panel">
          <div class="scan-hub__side-eyebrow">登记动作</div>

          <LoadingSkeleton
            v-if="availableActionsLoading"
            type="card"
            :count="1"
            data-test="available-actions-loading"
          />

          <template v-else-if="orderedActions.length > 0">
            <div class="scan-hub__action-list" data-test="available-actions-list">
              <button
                v-for="(action, idx) in orderedActions"
                :key="action.actionType"
                type="button"
                :class="[
                  'scan-hub__action',
                  isRecommended(action) && 'scan-hub__action--primary',
                  ['EXCEPTION', 'EXCEPTION_OPEN'].includes(action.actionType) && 'scan-hub__action--danger'
                ]"
                :data-test="`available-action-${action.actionType}`"
                @click="pickAction(action)"
              >
                <span class="scan-hub__action-lead">
                  <component :is="iconMap[action.actionType] || ScanLine" :size="14" />
                  <span>{{ actionLabel(action) }}</span>
                  <StatusPill
                    v-if="isRecommended(action)"
                    tone="primary"
                    size="xs"
                    data-test="recommended-action-badge"
                  >
                    推荐
                  </StatusPill>
                </span>
                <span class="scan-hub__action-trail">
                  <span v-if="action.nextStatusLabel" class="scan-hub__action-next">
                    → {{ action.nextStatusLabel }}
                  </span>
                  <KbdShortcut
                    v-if="idx < 4"
                    :tone="isRecommended(action) ? 'inverse' : 'default'"
                    :keys="`F${idx + 1}`"
                  />
                </span>
              </button>
            </div>
            <p class="scan-hub__panel-foot">
              动作由 <span class="mono">/available-actions</span> 数据驱动，前端不再硬编码入库 / 出库 / 中转 / 交付 / 异常。
            </p>
          </template>

          <EmptyState
            v-else
            title="当前无可执行扫码动作"
            :subtitle="noActionMessage"
            data-test="no-available-actions"
          >
            <template v-if="noActionGuidance" #subtitle>
              <span class="scan-hub__no-action-hint" data-test="no-action-hint">
                {{ noActionGuidance.hint }}
              </span>
              <span class="scan-hub__no-action-tip" data-test="no-action-tip">
                {{ noActionGuidance.tip }}
              </span>
              <span class="scan-hub__no-action-raw" data-test="no-action-raw">
                后端原始判定：{{ noActionMessage }}
              </span>
            </template>
          </EmptyState>
        </BaseCard>

        <!-- 流转记录占位（3/12，详细数据由 F12 详情页提供） -->
        <BaseCard padding="md" class="scan-hub__panel scan-hub__panel--history">
          <div class="scan-hub__side-eyebrow">流转记录</div>
          <ol class="scan-hub__history">
            <li>
              <span class="scan-hub__history-dot scan-hub__history-dot--success" />
              <div>
                <div class="scan-hub__history-title">系统已校验链上完整性</div>
                <div class="scan-hub__history-meta mono">
                  current_status =
                  {{ actionDecision?.currentStatus || '—' }}
                </div>
              </div>
            </li>
            <li>
              <span class="scan-hub__history-dot" />
              <div>
                <div class="scan-hub__history-title">想看完整生命周期？</div>
                <button
                  type="button"
                  class="scan-hub__link-inline"
                  data-test="scan-open-history"
                  @click="handleViewDetail"
                >
                  打开溯源详情 →
                </button>
              </div>
            </li>
          </ol>
        </BaseCard>
      </div>
    </section>

    <ScanFlowDialog
      v-model="showFlowDialog"
      :trace-code="scannedCode"
      :action-type="flowAction"
      :idempotency-key="pendingIdempotencyKey"
      @success="onFlowSuccess"
    />

    <ScanExceptionDialog
      v-model="showExceptionDialog"
      :trace-code="scannedCode"
      :idempotency-key="pendingIdempotencyKey"
      @success="onFlowSuccess"
    />

    <CreateTraceDialog v-model="showCreateModal" @success="onCreateTraceSuccess" />
  </div>
</template>

<style scoped>
.scan-hub {
  max-width: 1280px;
  margin: 0 auto;
  padding: 24px 24px 48px;
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.scan-hub__hint {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  font-size: 12.5px;
  color: var(--ink-subtle);
}

.scan-hub__grid {
  display: grid;
  gap: 16px;
}
.scan-hub__grid--default {
  grid-template-columns: 2fr 1fr;
}
.scan-hub__grid--scanning {
  grid-template-columns: 2fr 1fr;
}
.scan-hub__grid--identified {
  grid-template-columns: 5fr 4fr 3fr;
}

.scan-hub__cta {
  min-height: 240px;
}
.scan-hub__cta-eyebrow {
  font-size: 11px;
  font-weight: 500;
  letter-spacing: 0.4px;
  text-transform: uppercase;
  color: var(--ink-tertiary);
  margin-bottom: 12px;
}
.scan-hub__cta-title {
  font-size: 28px;
  line-height: 1.2;
  letter-spacing: -0.6px;
  font-weight: 600;
  color: var(--ink);
  margin: 0 0 12px 0;
}
.scan-hub__cta-text {
  font-size: 14px;
  color: var(--ink-muted);
  max-width: 480px;
  margin: 0 0 24px 0;
  line-height: 1.55;
}
.scan-hub__cta-row {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 12px;
}
.scan-hub__manual {
  display: flex;
  align-items: center;
  gap: 8px;
  flex: 1 1 280px;
  min-width: 240px;
}
.scan-hub__manual > :first-child {
  flex: 1 1 auto;
}
.scan-hub__cta-extra {
  margin-top: 24px;
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
  font-size: 12.5px;
  color: var(--ink-subtle);
}
.scan-hub__cta-extra-label {
  color: var(--ink-tertiary);
}
.scan-hub__link {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 13px;
  font-weight: 500;
  color: var(--primary);
  background: transparent;
  border: 0;
  padding: 0;
  cursor: pointer;
}
.scan-hub__link:hover {
  color: var(--primary-hover);
  text-decoration: underline;
  text-underline-offset: 4px;
}
.scan-hub__link-inline {
  font-size: inherit;
  font-weight: 500;
  color: var(--primary);
  background: transparent;
  border: 0;
  padding: 0;
  cursor: pointer;
}
.scan-hub__link-inline:hover {
  color: var(--primary-hover);
  text-decoration: underline;
}

.scan-hub__kpi {
  display: flex;
  flex-direction: column;
}
.scan-hub__kpi-eyebrow {
  font-size: 11px;
  font-weight: 500;
  letter-spacing: 0.4px;
  text-transform: uppercase;
  color: var(--ink-tertiary);
  margin-bottom: 16px;
}
.scan-hub__kpi-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12px;
}
.scan-hub__kpi-num {
  font-size: 24px;
  line-height: 1;
  letter-spacing: -0.6px;
  font-weight: 600;
  color: var(--ink);
}
.scan-hub__kpi-num--error {
  color: var(--error);
}
.scan-hub__kpi-label {
  font-size: 12px;
  color: var(--ink-subtle);
  margin-top: 6px;
}
.scan-hub__kpi-label--error {
  color: var(--error);
}
.scan-hub__kpi-foot {
  margin: 16px 0 0 0;
  font-size: 12px;
  color: var(--ink-tertiary);
  line-height: 1.55;
}

.scan-hub__viewport {
  min-height: 320px;
  display: flex;
}
.scan-hub__viewport > * {
  width: 100%;
}
.scan-hub__sidecar {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.scan-hub__side-eyebrow {
  font-size: 11px;
  font-weight: 500;
  letter-spacing: 0.4px;
  text-transform: uppercase;
  color: var(--ink-tertiary);
  margin-bottom: 12px;
}

.scan-hub__kv {
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.scan-hub__kv-row {
  display: grid;
  grid-template-columns: 88px 1fr;
  align-items: center;
  font-size: 13px;
}
.scan-hub__kv-row dt {
  color: var(--ink-subtle);
}
.scan-hub__kv-row dd {
  margin: 0;
  color: var(--ink);
  font-weight: 500;
  display: inline-flex;
  align-items: center;
  gap: 8px;
}

.scan-hub__tips {
  margin: 0;
  padding-left: 18px;
  list-style: disc;
  color: var(--ink-muted);
  font-size: 13px;
  line-height: 1.7;
}

.scan-hub__identified {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.scan-hub__hero {
  display: flex;
}
.scan-hub__hero :deep(.base-card__body) {
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  flex-wrap: wrap;
}
.scan-hub__hero-left {
  display: flex;
  align-items: center;
  gap: 14px;
  flex-wrap: wrap;
}
.scan-hub__hero-back {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 12.5px;
  font-weight: 500;
  color: var(--ink-subtle);
  background: transparent;
  border: 0;
  padding: 0;
  cursor: pointer;
}
.scan-hub__hero-back:hover {
  color: var(--ink);
}
.scan-hub__hero-eyebrow {
  font-size: 11px;
  font-weight: 500;
  letter-spacing: 0.4px;
  text-transform: uppercase;
  color: var(--ink-tertiary);
}
.scan-hub__hero-right {
  display: inline-flex;
  align-items: center;
  gap: 8px;
}

.scan-hub__panel {
  min-height: 240px;
}
.scan-hub__panel-foot {
  margin: 16px 0 0 0;
  font-size: 12px;
  color: var(--ink-tertiary);
  line-height: 1.55;
}

.scan-hub__no-action-hint,
.scan-hub__no-action-tip,
.scan-hub__no-action-raw {
  display: block;
  line-height: 1.55;
}
.scan-hub__no-action-hint {
  font-weight: 500;
  color: var(--ink);
  margin-bottom: 6px;
}
.scan-hub__no-action-tip {
  color: var(--ink-muted);
  margin-bottom: 8px;
}
.scan-hub__no-action-raw {
  font-size: 11.5px;
  color: var(--ink-tertiary);
  font-family: 'JetBrains Mono', ui-monospace, monospace;
  padding-top: 6px;
  border-top: 1px dashed var(--hairline);
}

.scan-hub__action-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.scan-hub__action {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  height: 44px;
  padding: 0 14px;
  border-radius: 8px;
  border: 1px solid var(--hairline);
  background: var(--surface-1);
  color: var(--ink);
  font-size: 13.5px;
  font-weight: 500;
  cursor: pointer;
  transition: border-color 0.15s, background 0.15s, box-shadow 0.15s;
  text-align: left;
  font-family: inherit;
}
.scan-hub__action:hover {
  border-color: var(--ink-subtle);
}
.scan-hub__action:focus-visible {
  outline: none;
  box-shadow: 0 0 0 3px var(--primary-ring);
}
.scan-hub__action--primary {
  background: var(--primary);
  border-color: var(--primary);
  color: #fff;
}
.scan-hub__action--primary:hover {
  background: var(--primary-hover);
  border-color: var(--primary-hover);
}
.scan-hub__action--danger:not(.scan-hub__action--primary) {
  border-color: var(--error-soft);
}
.scan-hub__action--danger:not(.scan-hub__action--primary):hover {
  border-color: var(--error);
}
.scan-hub__action-lead {
  display: inline-flex;
  align-items: center;
  gap: 8px;
}
.scan-hub__action-trail {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  font-size: 12px;
  color: var(--ink-subtle);
}
.scan-hub__action--primary .scan-hub__action-trail {
  color: rgba(255, 255, 255, 0.85);
}
.scan-hub__action-next {
  font-size: 12px;
}

.scan-hub__history {
  margin: 0;
  padding: 0;
  list-style: none;
  display: flex;
  flex-direction: column;
  gap: 14px;
}
.scan-hub__history li {
  display: flex;
  gap: 8px;
  align-items: flex-start;
}
.scan-hub__history-dot {
  width: 6px;
  height: 6px;
  border-radius: 9999px;
  background: var(--ink-tertiary);
  margin-top: 6px;
  flex-shrink: 0;
}
.scan-hub__history-dot--success {
  background: var(--success);
}
.scan-hub__history-title {
  font-size: 13px;
  font-weight: 500;
  color: var(--ink);
}
.scan-hub__history-meta {
  font-size: 11.5px;
  color: var(--ink-subtle);
  margin-top: 4px;
}

/* 平板：5/4 + 流转记录折到底部 */
@media (max-width: 1023.98px) {
  .scan-hub__grid--default,
  .scan-hub__grid--scanning {
    grid-template-columns: 1fr;
  }
  .scan-hub__grid--identified {
    grid-template-columns: 5fr 4fr;
  }
  .scan-hub__panel--history {
    grid-column: 1 / -1;
  }
}

/* 手机：单栏堆叠；动作面板粘性吸顶 */
@media (max-width: 767.98px) {
  .scan-hub__grid--identified {
    grid-template-columns: 1fr;
  }
  .scan-hub__panel--actions {
    position: sticky;
    top: 56px;
    z-index: 5;
  }
  .scan-hub__cta-row {
    flex-direction: column;
    align-items: stretch;
  }
  .scan-hub__manual {
    width: 100%;
  }
  .scan-hub__viewport {
    min-height: 60vh;
  }
}
</style>
