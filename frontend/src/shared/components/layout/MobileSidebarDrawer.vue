<script setup>
import { computed, watch } from 'vue'
import { ChevronRight, X } from 'lucide-vue-next'
import KbdShortcut from '@/shared/components/ui/KbdShortcut.vue'
import { NAVIGATION_SECTIONS } from './layoutNavigation'

const props = defineProps({
  visible: { type: Boolean, default: false },
  items: { type: Array, default: () => [] },
  activePath: { type: String, default: '/' },
  username: { type: String, default: '用户' },
  userRole: { type: String, default: '当前账号' }
})

const emit = defineEmits(['close', 'navigate', 'logout'])

const userInitial = computed(() => (props.username?.slice(0, 1) || 'U').toUpperCase())

const sections = computed(() =>
  NAVIGATION_SECTIONS
    .map((section) => ({
      ...section,
      items: props.items.filter((item) => (item.section || 'main') === section.key)
    }))
    .filter((section) => section.items.length > 0)
)

watch(
  () => props.visible,
  (open) => {
    if (typeof document === 'undefined') return
    document.body.style.overflow = open ? 'hidden' : ''
  }
)
</script>

<template>
  <Teleport to="body">
    <Transition name="drawer-fade">
      <div
        v-if="visible"
        class="drawer-root"
        data-test="mobile-sidebar-drawer"
      >
        <div class="drawer-backdrop" @click="emit('close')" />

        <aside class="drawer" role="dialog" aria-label="导航菜单">
          <div class="drawer__brand">
            <span class="drawer__logo" aria-hidden="true">
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="white" stroke-width="2.4">
                <path d="M3 9h18M3 15h18M9 3v18M15 3v18" />
              </svg>
            </span>
            <span class="drawer__brand-text">trace.</span>
            <button
              type="button"
              class="drawer__close"
              :aria-label="'关闭导航菜单'"
              @click="emit('close')"
            >
              <X :size="16" :stroke-width="2" />
            </button>
          </div>

          <nav class="drawer__nav" aria-label="主导航">
            <div
              v-for="section in sections"
              :key="section.key"
              class="drawer__section"
            >
              <div v-if="section.label" class="drawer__section-label">{{ section.label }}</div>
              <button
                v-for="item in section.items"
                :key="item.path"
                type="button"
                class="drawer__item"
                :class="{ 'is-active': item.path === activePath }"
                :data-nav-path="item.path"
                @click="emit('navigate', item.path)"
              >
                <span class="drawer__item-icon" aria-hidden="true">
                  <component :is="item.icon" :size="16" :stroke-width="2" />
                </span>
                <span class="drawer__item-label">{{ item.label }}</span>
                <KbdShortcut v-if="item.kbd" :keys="item.kbd" class="drawer__item-kbd" />
              </button>
            </div>
          </nav>

          <button
            type="button"
            class="drawer__user"
            data-test="logout-action"
            :aria-label="'退出登录'"
            @click="emit('logout')"
          >
            <span class="drawer__user-avatar" aria-hidden="true">{{ userInitial }}</span>
            <span class="drawer__user-meta">
              <span class="drawer__user-name">{{ username }}</span>
              <span class="drawer__user-role">{{ userRole }}</span>
            </span>
            <ChevronRight class="drawer__user-chevron" :size="14" :stroke-width="2" />
          </button>
        </aside>
      </div>
    </Transition>
  </Teleport>
</template>

<style scoped>
.drawer-root {
  position: fixed;
  inset: 0;
  z-index: 1000;
  display: flex;
}

.drawer-backdrop {
  position: absolute;
  inset: 0;
  background: rgba(15, 23, 42, 0.45);
}

.drawer {
  position: relative;
  width: 280px;
  max-width: 86vw;
  height: 100%;
  background: var(--surface-1);
  border-right: 1px solid var(--hairline);
  display: flex;
  flex-direction: column;
  z-index: 1;
}

.drawer__brand {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 16px;
  border-bottom: 1px solid var(--hairline);
}

.drawer__logo {
  width: 28px;
  height: 28px;
  border-radius: 8px;
  background: var(--primary);
  display: grid;
  place-items: center;
}

.drawer__brand-text {
  flex: 1;
  font-size: 16px;
  font-weight: 600;
  color: var(--ink);
  letter-spacing: -0.2px;
}

.drawer__close {
  width: 32px;
  height: 32px;
  display: grid;
  place-items: center;
  border-radius: 8px;
  border: 1px solid var(--hairline);
  background: var(--surface-1);
  color: var(--ink);
  cursor: pointer;
  transition: border-color 0.12s, background 0.12s;
}

.drawer__close:hover,
.drawer__close:focus-visible {
  border-color: var(--ink-subtle);
  background: var(--surface-2);
  outline: none;
}

.drawer__nav {
  flex: 1 1 auto;
  overflow-y: auto;
  padding: 12px;
  display: flex;
  flex-direction: column;
}

.drawer__section + .drawer__section {
  margin-top: 4px;
}

.drawer__section-label {
  font-size: 11px;
  font-weight: 500;
  letter-spacing: 0.4px;
  text-transform: uppercase;
  color: var(--ink-tertiary);
  padding: 0 12px;
  margin: 14px 0 4px;
}

.drawer__item {
  display: flex;
  align-items: center;
  gap: 12px;
  width: 100%;
  height: 40px;
  padding: 8px 12px;
  border-radius: 8px;
  border: 0;
  background: transparent;
  font-size: 14px;
  font-weight: 500;
  line-height: 1;
  color: var(--ink-muted);
  cursor: pointer;
  text-align: left;
  transition: background-color 0.12s, color 0.12s;
}

.drawer__item + .drawer__item {
  margin-top: 2px;
}

.drawer__item:hover,
.drawer__item:focus-visible {
  background: var(--surface-2);
  color: var(--ink);
  outline: none;
}

.drawer__item.is-active {
  background: var(--surface-2);
  color: var(--ink);
}

.drawer__item-icon {
  display: inline-flex;
  flex-shrink: 0;
  color: var(--ink-subtle);
}

.drawer__item.is-active .drawer__item-icon {
  color: var(--primary);
}

.drawer__item-label {
  flex: 1 1 auto;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.drawer__item-kbd {
  margin-left: auto;
  flex-shrink: 0;
}

.drawer__user {
  display: flex;
  align-items: center;
  gap: 10px;
  width: 100%;
  padding: 14px 16px;
  border: 0;
  border-top: 1px solid var(--hairline);
  background: transparent;
  cursor: pointer;
  text-align: left;
  transition: background-color 0.12s;
}

.drawer__user:hover,
.drawer__user:focus-visible {
  background: var(--surface-2);
  outline: none;
}

.drawer__user-avatar {
  width: 32px;
  height: 32px;
  flex-shrink: 0;
  border-radius: 9999px;
  background: var(--surface-2);
  display: grid;
  place-items: center;
  font-size: 12px;
  font-weight: 600;
  color: var(--ink);
}

.drawer__user-meta {
  flex: 1 1 auto;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.drawer__user-name {
  font-size: 13px;
  font-weight: 600;
  color: var(--ink);
  line-height: 1;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.drawer__user-role {
  font-size: 11.5px;
  color: var(--ink-tertiary);
  line-height: 1;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.drawer__user-chevron {
  flex-shrink: 0;
  color: var(--ink-tertiary);
}

.drawer-fade-enter-active,
.drawer-fade-leave-active {
  transition: opacity 0.18s ease;
}

.drawer-fade-enter-active .drawer,
.drawer-fade-leave-active .drawer {
  transition: transform 0.22s cubic-bezier(0.32, 0.72, 0, 1);
}

.drawer-fade-enter-from,
.drawer-fade-leave-to {
  opacity: 0;
}

.drawer-fade-enter-from .drawer,
.drawer-fade-leave-to .drawer {
  transform: translateX(-100%);
}
</style>
