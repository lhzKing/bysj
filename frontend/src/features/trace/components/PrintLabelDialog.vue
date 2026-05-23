<script setup>
import { computed } from 'vue'
import QrcodeVue from 'qrcode.vue'
import { X, Printer } from 'lucide-vue-next'
import BaseButton from '@/shared/components/ui/BaseButton.vue'

const props = defineProps({
  modelValue: { type: Boolean, default: false },
  codes: { type: Array, default: () => [] },
  batch: { type: Object, default: null },
  partLabel: { type: String, default: '' },
  title: { type: String, default: '打印标签' },
  confirmText: { type: String, default: '打印' },
  // 'print'   = 首次打印（confirm 后父组件调 printTraceCode 上链）
  // 'reprint' = 重打（标签角标加 RP 提示；confirm 后父组件调 reprintTraceCode 上链）
  // 'view'    = 仅预览（任何登录用户都能看 QR，但不上链——给无 trace:code:print 权限的角色 + 终态码用）
  mode: { type: String, default: 'print' }
})

const emit = defineEmits(['update:modelValue', 'confirm', 'cancel'])

const isViewMode = computed(() => props.mode === 'view')
const resolvedConfirmText = computed(() => {
  if (props.confirmText && props.confirmText !== '打印') return props.confirmText
  return isViewMode.value ? '打印（仅预览，不上链）' : '打印'
})

// 渲染单张标签的 QR 内容。新数据 qrPayload 已是完整 URL，扫码直跳 /public/traces/<code>；
// 老数据 qrPayload=traceCode（裸码），这里用当前页面 origin 补齐 URL，让演示效果一致。
function qrValueOf(code) {
  const payload = String(code?.qrPayload || '').trim()
  if (/^https?:\/\//i.test(payload)) return payload
  const tc = code?.traceCode || payload
  return `${window.location.origin}/public/traces/${tc}`
}

const visibleCodes = computed(() => (props.codes || []).filter((c) => c?.traceCode))

function close() {
  emit('update:modelValue', false)
  emit('cancel')
}

function doPrint() {
  // 顺序很关键：先调 window.print()（同步阻塞，直到用户关掉浏览器打印对话框），
  // 再 emit('confirm') 让父组件关 dialog。
  // 反过来 emit 先发就坏了——父组件会立刻把 modelValue 置 false 销毁 dialog，
  // 之后 window.print() 抓到的 DOM 已经没有标签内容，打印预览全白。
  window.print()
  // view 模式（无打印权限或终态码）只允许浏览器本地打印，绝不触发父组件的链上事件回调；
  // 这是公开页 + 详情页双入口的"仅预览"安全门——把"看 QR"与"写链"在 UI 层就分离。
  if (!isViewMode.value) {
    emit('confirm', visibleCodes.value)
  }
}
</script>

<template>
  <Teleport to="body">
    <div v-if="modelValue" class="print-dialog" role="dialog" aria-label="标签打印预览">
      <div class="print-dialog__backdrop" @click="close" />

      <article class="print-dialog__card">
        <header class="print-dialog__head">
          <div>
            <h2 class="print-dialog__title">{{ title }}</h2>
            <p class="print-dialog__subtitle">
              共 {{ visibleCodes.length }} 张标签
              <span v-if="batch?.batchNo">· 批次 {{ batch.batchNo }}</span>
              <span v-if="partLabel"> · {{ partLabel }}</span>
            </p>
          </div>
          <button type="button" class="print-dialog__close" aria-label="关闭" @click="close">
            <X :size="16" />
          </button>
        </header>

        <div class="print-dialog__hint">
          <template v-if="isViewMode">
            <strong>仅预览模式：</strong>点击"{{ resolvedConfirmText }}"会调用浏览器本地打印 / 另存为 PDF，
            <strong>不会</strong>记录链上打印事件。如需正式打印请在生产赋码工作台进行。
          </template>
          <template v-else>
            预览正在显示打印效果，下面网格中的每一张就是一张实物标签。
            点击"打印"会弹出浏览器打印对话框，可选打印机或"另存为 PDF"。
          </template>
        </div>

        <div class="print-dialog__sheet" data-test="print-label-sheet">
          <article
            v-for="code in visibleCodes"
            :key="code.traceCode"
            class="print-label"
            :data-testid="`print-label-${code.traceCode}`"
          >
            <header class="print-label__head">
              <span class="print-label__brand">工业配件溯源</span>
              <span v-if="mode === 'reprint'" class="print-label__rp">RP</span>
            </header>

            <div class="print-label__qr">
              <QrcodeVue :value="qrValueOf(code)" :size="160" level="M" :margin="1" render-as="svg" />
            </div>

            <dl class="print-label__meta">
              <div class="print-label__meta-row">
                <dt>追溯码</dt>
                <dd class="mono">{{ code.traceCode }}</dd>
              </div>
              <div class="print-label__meta-row">
                <dt>批内序号</dt>
                <dd class="mono">#{{ code.serialNo || '-' }}</dd>
              </div>
              <div v-if="batch?.batchNo" class="print-label__meta-row">
                <dt>批次</dt>
                <dd class="mono">{{ batch.batchNo }}</dd>
              </div>
              <div v-if="partLabel" class="print-label__meta-row">
                <dt>产品</dt>
                <dd>{{ partLabel }}</dd>
              </div>
            </dl>

            <footer class="print-label__foot">
              扫码进入溯源详情 · {{ new Date().toLocaleDateString('zh-CN') }}
            </footer>
          </article>
        </div>

        <footer class="print-dialog__foot">
          <BaseButton variant="text" @click="close">取消</BaseButton>
          <BaseButton
            variant="primary"
            data-test="print-label-confirm"
            :data-mode="mode"
            :disabled="visibleCodes.length === 0"
            @click="doPrint"
          >
            <template #icon><Printer :size="13" /></template>
            {{ resolvedConfirmText }}
          </BaseButton>
        </footer>
      </article>
    </div>
  </Teleport>
</template>

<style scoped>
.print-dialog {
  position: fixed;
  inset: 0;
  z-index: 1000;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
}

.print-dialog__backdrop {
  position: absolute;
  inset: 0;
  background: rgba(15, 23, 42, 0.55);
}

.print-dialog__card {
  position: relative;
  width: 100%;
  max-width: 880px;
  max-height: calc(100vh - 48px);
  background: var(--surface-1);
  border: 1px solid var(--hairline);
  border-radius: 12px;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  box-shadow: 0 18px 40px -16px rgba(15, 23, 42, 0.35);
}

.print-dialog__head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  padding: 16px 20px;
  border-bottom: 1px solid var(--hairline);
}

.print-dialog__title {
  margin: 0 0 4px;
  font-size: 16px;
  font-weight: 600;
  color: var(--ink);
}

.print-dialog__subtitle {
  margin: 0;
  font-size: 12.5px;
  color: var(--ink-subtle);
}

.print-dialog__close {
  width: 28px;
  height: 28px;
  border-radius: 6px;
  border: 1px solid transparent;
  background: transparent;
  color: var(--ink-subtle);
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}
.print-dialog__close:hover {
  background: var(--surface-2);
  color: var(--ink);
}

.print-dialog__hint {
  padding: 10px 20px;
  font-size: 12px;
  color: var(--ink-muted);
  background: var(--surface-2);
  border-bottom: 1px solid var(--hairline);
  line-height: 1.5;
}

.print-dialog__sheet {
  flex: 1;
  overflow-y: auto;
  padding: 20px;
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
  gap: 14px;
  background: var(--surface-2);
}

.print-label {
  background: #fff;
  border: 1px solid #d4d4d8;
  border-radius: 6px;
  padding: 12px;
  display: flex;
  flex-direction: column;
  gap: 8px;
  page-break-inside: avoid;
  break-inside: avoid;
}

.print-label__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 10px;
  font-weight: 600;
  letter-spacing: 0.6px;
  color: #52525b;
  text-transform: uppercase;
}
.print-label__rp {
  padding: 1px 6px;
  border: 1px solid #e54855;
  color: #e54855;
  border-radius: 9999px;
  font-size: 9px;
}

.print-label__qr {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 4px;
  background: #fff;
}
.print-label__qr :deep(svg) {
  width: 160px;
  height: 160px;
}

.print-label__meta {
  margin: 0;
  display: grid;
  gap: 3px;
  font-size: 11px;
  color: #18181b;
}
.print-label__meta-row {
  display: grid;
  grid-template-columns: 56px 1fr;
  gap: 6px;
  align-items: baseline;
}
.print-label__meta-row dt {
  color: #71717a;
  font-size: 10px;
}
.print-label__meta-row dd {
  margin: 0;
  word-break: break-all;
  font-size: 11px;
  line-height: 1.35;
}

.print-label__foot {
  margin-top: 2px;
  padding-top: 6px;
  border-top: 1px dashed #d4d4d8;
  font-size: 9.5px;
  color: #71717a;
  text-align: center;
}

.print-dialog__foot {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  padding: 12px 20px;
  border-top: 1px solid var(--hairline);
  background: var(--surface-1);
}

.mono {
  font-family: 'JetBrains Mono', ui-monospace, monospace;
}
</style>

<!--
  打印样式必须放在非 scoped 块里。原因：scoped style 会给所有选择器加 [data-v-hash]，
  导致 `.print-dialog *` 匹配不到 qrcode.vue 等子组件渲染的 SVG/path 内部节点，
  打印时这些节点继承 :global(body *) 的 visibility: hidden，结果就是 QR 全白。
-->
<style>
@media print {
  /* 整页空白：把 body 直系子节点全藏掉，再把 teleport 出去的对话框单独露出来 */
  body > *:not(.print-dialog) {
    display: none !important;
  }
  html, body {
    background: #fff !important;
    margin: 0 !important;
    padding: 0 !important;
  }
  .print-dialog {
    position: static !important;
    inset: auto !important;
    padding: 0 !important;
    display: block !important;
    background: #fff !important;
  }
  .print-dialog__backdrop,
  .print-dialog__head,
  .print-dialog__hint,
  .print-dialog__foot,
  .print-dialog__close {
    display: none !important;
  }
  .print-dialog__card {
    box-shadow: none !important;
    border: 0 !important;
    max-width: 100% !important;
    max-height: none !important;
    overflow: visible !important;
    background: #fff !important;
  }
  .print-dialog__sheet {
    background: #fff !important;
    grid-template-columns: repeat(3, 1fr) !important;
    gap: 6mm !important;
    padding: 8mm !important;
    overflow: visible !important;
  }
  .print-label {
    border: 1px dashed #d4d4d8 !important;
    background: #fff !important;
    page-break-inside: avoid !important;
    break-inside: avoid !important;
    color-adjust: exact !important;
    -webkit-print-color-adjust: exact !important;
    print-color-adjust: exact !important;
  }
  .print-label__qr svg {
    width: 36mm !important;
    height: 36mm !important;
    display: block !important;
  }
}
</style>
