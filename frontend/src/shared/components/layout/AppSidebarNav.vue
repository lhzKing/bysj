<script setup>
import { computed } from 'vue'
import { ChevronRight } from 'lucide-vue-next'
import KbdShortcut from '@/shared/components/ui/KbdShortcut.vue'
import { NAVIGATION_SECTIONS } from './layoutNavigation'

const props = defineProps({
  items: { type: Array, default: () => [] },
  activePath: { type: String, default: '/' },
  username: { type: String, default: '用户' },
  userRole: { type: String, default: '当前账号' }
})

const emit = defineEmits(['navigate', 'logout'])

const userInitial = computed(() => (props.username?.slice(0, 1) || 'U').toUpperCase())

const sections = computed(() =>
  NAVIGATION_SECTIONS
    .map((section) => ({
      ...section,
      items: props.items.filter((item) => (item.section || 'main') === section.key)
    }))
    .filter((section) => section.items.length > 0)
)
</script>

<template>
  <aside class="sidebar" data-test="app-sidebar">
    <div class="sidebar__brand">
      <span class="sidebar__logo" aria-hidden="true">
        <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="white" stroke-width="2.4">
          <path d="M3 9h18M3 15h18M9 3v18M15 3v18" />
        </svg>
      </span>
      <span class="sidebar__brand-text">trace.</span>
    </div>

    <nav class="sidebar__nav" aria-label="主导航">
      <div
        v-for="section in sections"
        :key="section.key"
        class="sidebar__section"
      >
        <div v-if="section.label" class="sidebar__section-label">{{ section.label }}</div>
        <button
          v-for="item in section.items"
          :key="item.path"
          type="button"
          class="sidebar__item"
          :class="{ 'is-active': item.path === activePath }"
          :data-nav-path="item.path"
          @click="emit('navigate', item.path)"
        >
          <span class="sidebar__item-icon" aria-hidden="true">
            <component :is="item.icon" :size="14" :stroke-width="2" />
          </span>
          <span class="sidebar__item-label">{{ item.label }}</span>
          <KbdShortcut v-if="item.kbd" :keys="item.kbd" class="sidebar__item-kbd" />
        </button>
      </div>
    </nav>

    <button
      type="button"
      class="sidebar__user"
      data-test="logout-action"
      :aria-label="'退出登录'"
      @click="emit('logout')"
    >
      <span class="sidebar__user-avatar" aria-hidden="true">{{ userInitial }}</span>
      <span class="sidebar__user-meta">
        <span class="sidebar__user-name">{{ username }}</span>
        <span class="sidebar__user-role">{{ userRole }}</span>
      </span>
      <ChevronRight class="sidebar__user-chevron" :size="14" :stroke-width="2" />
    </button>
  </aside>
</template>

<style scoped>
.sidebar {
  width: 240px;
  position: fixed;
  top: 0;
  bottom: 0;
  left: 0;
  background: var(--surface-1);
  border-right: 1px solid var(--hairline);
  display: flex;
  flex-direction: column;
  z-index: 40;
}

.sidebar__brand {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 16px;
  border-bottom: 1px solid var(--hairline);
}

.sidebar__logo {
  width: 28px;
  height: 28px;
  border-radius: 8px;
  background: var(--primary);
  display: grid;
  place-items: center;
}

.sidebar__brand-text {
  font-size: 16px;
  font-weight: 600;
  color: var(--ink);
  letter-spacing: -0.2px;
}

.sidebar__nav {
  flex: 1 1 auto;
  overflow-y: auto;
  padding: 12px;
  display: flex;
  flex-direction: column;
}

.sidebar__section + .sidebar__section {
  margin-top: 4px;
}

.sidebar__section-label {
  font-size: 11px;
  font-weight: 500;
  letter-spacing: 0.4px;
  text-transform: uppercase;
  color: var(--ink-tertiary);
  padding: 0 10px;
  margin: 14px 0 4px;
}

.sidebar__item {
  display: flex;
  align-items: center;
  gap: 9px;
  width: 100%;
  height: 30px;
  padding: 6px 10px;
  border-radius: 6px;
  border: 0;
  background: transparent;
  font-size: 13.5px;
  font-weight: 500;
  line-height: 1;
  color: var(--ink-muted);
  cursor: pointer;
  text-align: left;
  transition: background-color 0.12s, color 0.12s;
}

.sidebar__item + .sidebar__item {
  margin-top: 2px;
}

.sidebar__item:hover,
.sidebar__item:focus-visible {
  background: var(--surface-2);
  color: var(--ink);
  outline: none;
}

.sidebar__item.is-active {
  background: var(--surface-2);
  color: var(--ink);
}

.sidebar__item-icon {
  display: inline-flex;
  flex-shrink: 0;
  color: var(--ink-subtle);
}

.sidebar__item.is-active .sidebar__item-icon {
  color: var(--primary);
}

.sidebar__item-label {
  flex: 1 1 auto;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.sidebar__item-kbd {
  margin-left: auto;
  flex-shrink: 0;
}

.sidebar__user {
  display: flex;
  align-items: center;
  gap: 10px;
  width: 100%;
  padding: 12px;
  border: 0;
  border-top: 1px solid var(--hairline);
  background: transparent;
  cursor: pointer;
  text-align: left;
  transition: background-color 0.12s;
}

.sidebar__user:hover,
.sidebar__user:focus-visible {
  background: var(--surface-2);
  outline: none;
}

.sidebar__user-avatar {
  width: 28px;
  height: 28px;
  flex-shrink: 0;
  border-radius: 9999px;
  background: var(--surface-2);
  display: grid;
  place-items: center;
  font-size: 11.5px;
  font-weight: 600;
  color: var(--ink);
}

.sidebar__user-meta {
  flex: 1 1 auto;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.sidebar__user-name {
  font-size: 12.5px;
  font-weight: 600;
  color: var(--ink);
  line-height: 1;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.sidebar__user-role {
  font-size: 11px;
  color: var(--ink-tertiary);
  line-height: 1;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.sidebar__user-chevron {
  flex-shrink: 0;
  color: var(--ink-tertiary);
}
</style>
