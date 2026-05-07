<script setup>
import { computed } from 'vue'

/**
 * VerifyBadge —— 链验通过 / 失败横条。
 *
 * 视觉契约（与 frontend/preview/linear-trace-detail.html 中 verification stripe 1:1）：
 *   - valid=true：success-soft 底 + #bef0c7 描边 + ✓ 图标
 *   - valid=false：error-soft 底 + #f8c8ca 描边 + ✗ 图标
 *   - 左：✓/✗ 在白底圆角方块内（36×36，1px 描边同 stripe）
 *   - 中：13/12 双行，"链上完整 · N / N 节点验签通过" + "最后验证于 hh:mm:ss · RSA-2048 · SHA-256 链式哈希"
 *   - 右：actions slot，常放"查看公钥 / 第三方验签"的 text-link
 *
 * 用法：
 *   <VerifyBadge :valid="true" :validNodes="4" :totalNodes="4" verifiedAt="2026-05-05T14:32:08" />
 *   <VerifyBadge :valid="false" :validNodes="3" :totalNodes="4">
 *     <template #actions><a class="text-link">查看出错节点</a></template>
 *   </VerifyBadge>
 */
const props = defineProps({
  valid: {
    type: Boolean,
    default: true
  },
  validNodes: {
    type: Number,
    default: 0
  },
  totalNodes: {
    type: Number,
    default: 0
  },
  verifiedAt: {
    type: [String, Date, null],
    default: null
  },
  algo: {
    type: String,
    default: 'RSA-2048 · SHA-256 链式哈希'
  }
})

const formattedTime = computed(() => {
  if (!props.verifiedAt) return ''
  try {
    const d = props.verifiedAt instanceof Date ? props.verifiedAt : new Date(props.verifiedAt)
    if (Number.isNaN(d.getTime())) return ''
    const hh = String(d.getHours()).padStart(2, '0')
    const mm = String(d.getMinutes()).padStart(2, '0')
    const ss = String(d.getSeconds()).padStart(2, '0')
    return `${hh}:${mm}:${ss}`
  } catch {
    return ''
  }
})

const headline = computed(() => {
  const ratio = `${props.validNodes} / ${props.totalNodes}`
  return props.valid
    ? `链上完整 · ${ratio} 节点验签通过`
    : `链上不完整 · ${ratio} 节点验签通过`
})

const subline = computed(() => {
  const time = formattedTime.value
  return time ? `最后验证于 ${time} · ${props.algo}` : props.algo
})
</script>

<template>
  <div class="verify-badge" :class="valid ? 'verify-badge--ok' : 'verify-badge--fail'">
    <div class="verify-badge__icon">
      <svg v-if="valid" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.4">
        <path d="M5 12l5 5L20 7" />
      </svg>
      <svg v-else width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.4">
        <path d="M18 6L6 18M6 6l12 12" />
      </svg>
    </div>
    <div class="verify-badge__text">
      <div class="verify-badge__headline">{{ headline }}</div>
      <div class="verify-badge__subline">{{ subline }}</div>
    </div>
    <div v-if="$slots.actions" class="verify-badge__actions">
      <slot name="actions" />
    </div>
  </div>
</template>

<style scoped>
.verify-badge {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  border-radius: 12px;
  border: 1px solid transparent;
}
.verify-badge--ok {
  background: var(--success-soft);
  border-color: #bef0c7;
}
.verify-badge--fail {
  background: var(--error-soft);
  border-color: #f8c8ca;
}

.verify-badge__icon {
  display: grid;
  place-items: center;
  width: 36px;
  height: 36px;
  border-radius: 8px;
  background: #fff;
  flex-shrink: 0;
}
.verify-badge--ok .verify-badge__icon {
  color: var(--success);
  border: 1px solid #bef0c7;
}
.verify-badge--fail .verify-badge__icon {
  color: var(--error);
  border: 1px solid #f8c8ca;
}

.verify-badge__text {
  flex: 1 1 auto;
  min-width: 0;
}
.verify-badge__headline {
  font-size: 13px;
  font-weight: 600;
  color: var(--ink);
  line-height: 1.3;
}
.verify-badge__subline {
  font-size: 12px;
  color: var(--ink-muted);
  margin-top: 2px;
  line-height: 1.4;
}

.verify-badge__actions {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
}
</style>
