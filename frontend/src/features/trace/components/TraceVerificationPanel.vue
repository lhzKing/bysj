<script setup>
import { computed, ref } from 'vue'
import VerifyBadge from '@/shared/components/ui/VerifyBadge.vue'

const props = defineProps({
  verification: {
    type: Object,
    required: true
  },
  verifiedAt: {
    type: [String, Date, null],
    default: null
  }
})

const showErrorDetails = ref(false)

const validNodes = computed(() => {
  const v = props.verification || {}
  if (typeof v.hashVerifiedCount === 'number' && typeof v.signatureVerifiedCount === 'number') {
    return Math.min(v.hashVerifiedCount, v.signatureVerifiedCount)
  }
  return v.hashVerifiedCount ?? v.signatureVerifiedCount ?? 0
})

const totalNodes = computed(() => props.verification?.totalLogs ?? 0)

const errorCount = computed(() => props.verification?.errors?.length || 0)

const isValid = computed(() => Boolean(props.verification?.valid))

const toggleErrors = () => {
  showErrorDetails.value = !showErrorDetails.value
}
</script>

<template>
  <div class="trace-verification">
    <VerifyBadge
      :valid="isValid"
      :valid-nodes="validNodes"
      :total-nodes="totalNodes"
      :verified-at="verifiedAt"
    >
      <template v-if="!isValid && errorCount" #actions>
        <button
          type="button"
          class="trace-verification__link"
          :aria-expanded="showErrorDetails"
          data-testid="trace-verification-toggle-errors"
          @click="toggleErrors"
        >
          {{ showErrorDetails ? '收起出错节点' : `查看 ${errorCount} 个出错节点` }}
        </button>
      </template>
    </VerifyBadge>

    <transition name="trace-verification-slide">
      <div v-if="!isValid && showErrorDetails && errorCount" class="trace-verification__errors" data-testid="trace-verification-errors">
        <p class="trace-verification__errors-title">证据链校验失败 · {{ errorCount }} 个节点</p>

        <ul class="trace-verification__errors-list">
          <li
            v-for="(err, idx) in verification.errors"
            :key="`${err.logId || idx}-${idx}`"
            class="trace-verification__error"
          >
            <div class="trace-verification__error-row">
              <span class="trace-verification__error-id mono">节点 #{{ err.logId || idx + 1 }}</span>
              <span class="trace-verification__error-tag">{{ err.errorType || '校验失败' }}</span>
            </div>
            <p class="trace-verification__error-msg">{{ err.message || '哈希或签名校验未通过' }}</p>
            <dl class="trace-verification__error-fields">
              <template v-if="err.expectedHash">
                <dt>期望 Hash</dt>
                <dd class="mono">{{ err.expectedHash }}</dd>
              </template>
              <template v-if="err.actualHash">
                <dt>实际 Hash</dt>
                <dd class="mono">{{ err.actualHash }}</dd>
              </template>
              <template v-if="err.traceCode">
                <dt>溯源码</dt>
                <dd class="mono">{{ err.traceCode }}</dd>
              </template>
            </dl>
          </li>
        </ul>

        <p class="trace-verification__hint">
          检测到的 Hash 不匹配或签名失败表明该溯源记录可能已被篡改。建议立即联系管理员暂停该批次流转并启动审计。
        </p>
      </div>
    </transition>
  </div>
</template>

<style scoped>
.trace-verification {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.trace-verification__link {
  background: transparent;
  border: 0;
  padding: 0;
  font-family: inherit;
  font-size: 12px;
  font-weight: 500;
  color: var(--error);
  cursor: pointer;
}
.trace-verification__link:hover { color: var(--ink); }

.trace-verification__errors {
  border: 1px solid #f8c8ca;
  background: var(--error-soft);
  border-radius: 8px;
  padding: 14px 16px;
}
.trace-verification__errors-title {
  font-size: 13px;
  font-weight: 600;
  color: var(--error);
  margin: 0 0 10px 0;
}

.trace-verification__errors-list {
  list-style: none;
  padding: 0;
  margin: 0;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.trace-verification__error {
  background: var(--surface-1);
  border: 1px solid #f8c8ca;
  border-radius: 6px;
  padding: 10px 12px;
}
.trace-verification__error-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 6px;
}
.trace-verification__error-id {
  font-size: 12px;
  font-weight: 500;
  color: var(--ink);
}
.trace-verification__error-tag {
  font-size: 11px;
  color: var(--error);
  background: var(--error-soft);
  border: 1px solid #f8c8ca;
  border-radius: 9999px;
  padding: 1px 8px;
}
.trace-verification__error-msg {
  font-size: 12px;
  color: var(--ink-muted);
  margin: 0 0 8px 0;
}

.trace-verification__error-fields {
  display: grid;
  grid-template-columns: 96px 1fr;
  font-size: 11.5px;
  margin: 0;
  gap: 4px 8px;
}
.trace-verification__error-fields dt {
  color: var(--ink-subtle);
}
.trace-verification__error-fields dd {
  margin: 0;
  color: var(--ink-muted);
  word-break: break-all;
}

.trace-verification__hint {
  margin: 12px 0 0 0;
  font-size: 12px;
  color: var(--ink-muted);
  background: var(--warn-soft);
  border: 1px solid #f9d7a6;
  border-radius: 6px;
  padding: 8px 10px;
}

.trace-verification-slide-enter-active,
.trace-verification-slide-leave-active {
  transition: all 0.2s ease;
  overflow: hidden;
}
.trace-verification-slide-enter-from,
.trace-verification-slide-leave-to {
  opacity: 0;
  max-height: 0;
}
.trace-verification-slide-enter-to,
.trace-verification-slide-leave-from {
  opacity: 1;
  max-height: 600px;
}

.mono {
  font-family: 'JetBrains Mono', ui-monospace, 'SF Mono', Menlo, monospace;
}
</style>
