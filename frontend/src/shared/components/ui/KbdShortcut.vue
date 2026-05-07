<script setup>
/**
 * KbdShortcut —— 键盘快捷键提示徽章。
 *
 * 视觉契约（与 frontend/preview/linear-*.html 中 .kbd 1:1）：
 *   - 字体：JetBrains Mono / 11px / ink-tertiary
 *   - 背景：var(--surface-2)；描边 1px var(--hairline)
 *   - 圆角：4px；padding 1px 5px
 *   - tone="inverse"：用于 primary 按钮内（白色 15% 透明 + 白边），与 BaseButton kbd 槽位等价
 *
 * 用法：
 *   <KbdShortcut keys="F1" />
 *   <KbdShortcut keys="⌘ K" />
 *   <KbdShortcut><span>Esc</span></KbdShortcut>
 */
defineProps({
  keys: {
    type: String,
    default: ''
  },
  tone: {
    type: String,
    default: 'default',
    validator: (v) => ['default', 'inverse'].includes(v)
  }
})
</script>

<template>
  <span class="kbd-shortcut" :class="`kbd-shortcut--${tone}`">
    <slot>{{ keys }}</slot>
  </span>
</template>

<style scoped>
.kbd-shortcut {
  display: inline-flex;
  align-items: center;
  font-family: 'JetBrains Mono', ui-monospace, 'SF Mono', Menlo, monospace;
  font-size: 11px;
  line-height: 1;
  padding: 1px 5px;
  border-radius: 4px;
  border: 1px solid var(--hairline);
  background: var(--surface-2);
  color: var(--ink-tertiary);
  white-space: nowrap;
}
.kbd-shortcut--inverse {
  background: rgba(255, 255, 255, 0.15);
  border-color: rgba(255, 255, 255, 0.2);
  color: rgba(255, 255, 255, 0.85);
}
</style>
