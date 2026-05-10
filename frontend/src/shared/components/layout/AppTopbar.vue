<script setup>
import { Menu, Search } from 'lucide-vue-next'
import KbdShortcut from '@/shared/components/ui/KbdShortcut.vue'

const props = defineProps({
  pageTitle: { type: String, default: '' },
  isCompact: { type: Boolean, default: false },
  brand: { type: String, default: 'trace.' }
})

const emit = defineEmits(['toggle-menu', 'open-search'])
</script>

<template>
  <header class="topbar" data-test="app-topbar">
    <div class="topbar__lead">
      <button
        v-if="isCompact"
        type="button"
        class="topbar__icon-btn"
        data-test="mobile-nav-toggle"
        :aria-label="'打开导航菜单'"
        @click="emit('toggle-menu')"
      >
        <Menu :size="16" :stroke-width="2" />
      </button>

      <nav v-if="!isCompact" class="topbar__trail" aria-label="面包屑">
        <span class="topbar__seg">{{ brand }}</span>
        <span class="topbar__sep">/</span>
        <span class="topbar__now" data-test="page-title">{{ pageTitle }}</span>
      </nav>

      <span v-else class="topbar__compact-title" data-test="page-title">{{ pageTitle }}</span>
    </div>

    <div class="topbar__actions">
      <button
        v-if="!isCompact"
        type="button"
        class="topbar__search"
        data-test="topbar-search"
        @click="emit('open-search')"
      >
        <Search :size="13" :stroke-width="2" />
        <span class="topbar__search-text">搜索追溯码</span>
        <KbdShortcut keys="⌘ K" class="topbar__search-kbd" />
      </button>
    </div>
  </header>
</template>

<style scoped>
.topbar {
  height: 48px;
  background: var(--surface-1);
  border-bottom: 1px solid var(--hairline);
  position: sticky;
  top: 0;
  z-index: 30;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
  gap: 16px;
}

.topbar__lead {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
}

.topbar__trail {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
}

.topbar__seg {
  color: var(--ink-subtle);
}

.topbar__sep {
  color: var(--ink-tertiary);
}

.topbar__now {
  color: var(--ink);
  font-weight: 500;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.topbar__compact-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--ink);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.topbar__actions {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
}

.topbar__search {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  height: 32px;
  padding: 0 12px;
  border-radius: 8px;
  background: var(--surface-1);
  border: 1px solid var(--hairline);
  color: var(--ink-muted);
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: border-color 0.15s, color 0.15s;
}

.topbar__search:hover,
.topbar__search:focus-visible {
  border-color: var(--ink-subtle);
  color: var(--ink);
  outline: none;
}

.topbar__search-text {
  color: var(--ink-muted);
}

.topbar__search-kbd {
  margin-left: 4px;
}

.topbar__icon-btn {
  height: 32px;
  width: 32px;
  display: grid;
  place-items: center;
  border-radius: 8px;
  background: var(--surface-1);
  border: 1px solid var(--hairline);
  color: var(--ink);
  cursor: pointer;
  transition: border-color 0.15s, background-color 0.15s;
}

.topbar__icon-btn:hover,
.topbar__icon-btn:focus-visible {
  border-color: var(--ink-subtle);
  background: var(--surface-2);
  outline: none;
}

@media (max-width: 767.98px) {
  .topbar {
    padding: 0 16px;
  }
}
</style>
