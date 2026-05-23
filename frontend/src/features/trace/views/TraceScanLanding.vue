<script setup>
/**
 * TraceScanLanding —— 普通 USER 角色登录后的个人扫码落地页。
 *
 * 设计目标：USER 不应该看到完整的「追溯查询」列表全表（隐私 + 数据量）；
 * 他们的合理动作只是「拿到一个码 → 看溯源详情」。本页面就是该流程的入口：
 *   1. 大按钮打开摄像头扫码（复用 QRScanner）
 *   2. 备用：手动粘贴追溯码 + 查询
 * 两条路径都跳 `/traces/{code}` 详情页（USER 有 `trace:view` 权限可看详情）。
 *
 * 公开溯源页 `/public/traces/<code>` 同样能扫，但需要重新输入。本页面把"登录态扫码"做成
 * 一键流程，让外部审计 / 巡检员之类的低权限角色有一个干净的工作区。
 */
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { ScanLine, Search } from 'lucide-vue-next'
import PageHeader from '@/shared/components/ui/PageHeader.vue'
import BaseButton from '@/shared/components/ui/BaseButton.vue'
import BaseInput from '@/shared/components/ui/BaseInput.vue'
import QRScanner from '@/shared/components/QRScanner.vue'
import { useToast } from '@/shared/composables/useToast'

const router = useRouter()
const toast = useToast()

const scannerOpen = ref(false)
const manualCode = ref('')

function gotoDetail(rawCode) {
  const code = String(rawCode || '').trim()
  if (!code) {
    toast.error('请输入追溯码')
    return
  }
  router.push(`/traces/${encodeURIComponent(code)}`)
}

function onScan(code) {
  scannerOpen.value = false
  if (!code) return
  gotoDetail(code)
}

function onManualSubmit() {
  gotoDetail(manualCode.value)
}
</script>

<template>
  <div class="trace-scan-landing">
    <PageHeader
      title="扫码查询追溯码"
      subtitle="对准产品标签上的二维码扫描，或手动粘贴追溯码，即可查看完整溯源详情。"
      data-testid="scan-landing-header"
    />

    <section class="trace-scan-landing__hero" data-testid="scan-landing-hero">
      <div class="trace-scan-landing__hero-text">
        <span class="trace-scan-landing__eyebrow">主操作</span>
        <h2 class="trace-scan-landing__hero-title">打开摄像头扫码</h2>
        <p class="trace-scan-landing__hero-desc">
          支持产品标签上的 QR 码、箱码、托盘码。识别成功后自动跳转到对应单品的溯源详情。
        </p>
        <BaseButton
          variant="primary"
          size="md"
          data-test="scan-landing-open-camera"
          @click="scannerOpen = true"
        >
          <template #icon><ScanLine :size="14" /></template>
          打开摄像头扫码
        </BaseButton>
      </div>
      <div class="trace-scan-landing__hero-illustration" aria-hidden="true">
        <ScanLine :size="96" />
      </div>
    </section>

    <section class="trace-scan-landing__manual" data-testid="scan-landing-manual">
      <header class="trace-scan-landing__manual-head">
        <span class="trace-scan-landing__eyebrow">备用</span>
        <h3 class="trace-scan-landing__manual-title">或手动输入追溯码</h3>
      </header>
      <form class="trace-scan-landing__manual-form" @submit.prevent="onManualSubmit">
        <BaseInput
          v-model="manualCode"
          input-id="scan-landing-manual-code"
          placeholder="例如 TC-260505-A8F3K2"
          data-test="scan-landing-manual-input"
        />
        <BaseButton
          type="submit"
          variant="secondary"
          size="md"
          data-test="scan-landing-manual-submit"
        >
          <template #icon><Search :size="13" /></template>
          查询
        </BaseButton>
      </form>
    </section>

    <QRScanner v-if="scannerOpen" @scan="onScan" @close="scannerOpen = false" />
  </div>
</template>

<style scoped>
.trace-scan-landing {
  max-width: 880px;
  margin: 0 auto;
  padding: 24px 24px 48px;
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.trace-scan-landing__hero {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 24px;
  align-items: center;
  padding: 28px 32px;
  background: var(--surface-1);
  border: 1px solid var(--hairline);
  border-radius: 12px;
}
.trace-scan-landing__hero-text {
  display: flex;
  flex-direction: column;
  gap: 8px;
  min-width: 0;
}
.trace-scan-landing__eyebrow {
  font-size: 11px;
  font-weight: 500;
  letter-spacing: 0.4px;
  text-transform: uppercase;
  color: var(--ink-tertiary);
}
.trace-scan-landing__hero-title {
  margin: 0;
  font-size: 22px;
  font-weight: 600;
  letter-spacing: -0.4px;
  color: var(--ink);
}
.trace-scan-landing__hero-desc {
  margin: 0 0 6px;
  font-size: 13px;
  color: var(--ink-subtle);
  line-height: 1.5;
}
.trace-scan-landing__hero-illustration {
  width: 132px;
  height: 132px;
  flex-shrink: 0;
  display: grid;
  place-items: center;
  border-radius: 16px;
  background: var(--primary-soft);
  color: var(--primary);
}

.trace-scan-landing__manual {
  padding: 24px 32px;
  background: var(--surface-1);
  border: 1px solid var(--hairline);
  border-radius: 12px;
  display: flex;
  flex-direction: column;
  gap: 14px;
}
.trace-scan-landing__manual-head {
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.trace-scan-landing__manual-title {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
  color: var(--ink);
}
.trace-scan-landing__manual-form {
  display: flex;
  align-items: flex-end;
  gap: 8px;
}
.trace-scan-landing__manual-form :deep(.base-input) {
  flex: 1 1 auto;
  min-width: 200px;
}

@media (max-width: 639.98px) {
  .trace-scan-landing {
    padding: 16px 12px 32px;
  }
  .trace-scan-landing__hero {
    grid-template-columns: 1fr;
    padding: 20px;
  }
  .trace-scan-landing__hero-illustration {
    width: 96px;
    height: 96px;
    justify-self: start;
  }
  .trace-scan-landing__manual {
    padding: 20px;
  }
  .trace-scan-landing__manual-form {
    flex-direction: column;
    align-items: stretch;
  }
}
</style>
