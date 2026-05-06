<script setup>
import { computed } from 'vue'
import { LogOut as SwitchButton } from 'lucide-vue-next'

const COPY = {
  eyebrow: '\u6bd5\u4e1a\u8bbe\u8ba1\u5c55\u793a',
  title: '\u5de5\u4e1a\u914d\u4ef6\u4f9b\u5e94\u94fe\u6eaf\u6e90\u7cfb\u7edf',
  subtitle: '\u4f01\u4e1a\u7ea7\u4e1a\u52a1\u540e\u53f0\u539f\u578b',
  currentUser: '\u5f53\u524d\u767b\u5f55\u7528\u6237',
  logout: '\u9000\u51fa\u767b\u5f55'
}

const props = defineProps({
  items: {
    type: Array,
    default: () => []
  },
  activePath: {
    type: String,
    default: '/'
  },
  username: {
    type: String,
    default: '\u5f53\u524d\u7528\u6237'
  }
})

const emit = defineEmits(['navigate', 'logout'])

const userInitial = computed(() => props.username.slice(0, 1).toUpperCase())
</script>

<template>
  <aside class="app-sidebar-shell" data-test="desktop-sidebar">
    <div class="app-sidebar-shell__brand">
      <p class="app-sidebar-shell__eyebrow">{{ COPY.eyebrow }}</p>
      <h1 class="app-sidebar-shell__title">{{ COPY.title }}</h1>
      <p class="app-sidebar-shell__subtitle">{{ COPY.subtitle }}</p>
    </div>

    <nav class="app-sidebar-shell__nav" aria-label="Primary">
      <button
        v-for="item in items"
        :key="item.path"
        type="button"
        class="app-nav-item"
        :class="{ 'is-active': item.path === activePath }"
        :data-nav-path="item.path"
        @click="emit('navigate', item.path)"
      >
        <span class="app-nav-item__icon" aria-hidden="true">
          <component :is="item.icon" />
        </span>
        <span class="app-nav-item__content">
          <span class="app-nav-item__label">{{ item.label }}</span>
        </span>
      </button>
    </nav>

    <footer class="app-sidebar-shell__footer">
      <div class="app-user-chip">
          <span class="app-user-chip__avatar" aria-hidden="true">{{ userInitial }}</span>
          <span class="app-user-chip__body">
            <span class="app-user-chip__name">{{ username }}</span>
            <span class="app-user-chip__meta">{{ COPY.currentUser }}</span>
          </span>
        </div>

      <button
        type="button"
        class="app-secondary-action"
        data-test="logout-action"
        @click="emit('logout')"
      >
        <span class="app-nav-item__icon" aria-hidden="true">
          <LogOut class="w-5 h-5" />
        </span>
        <span>{{ COPY.logout }}</span>
      </button>
    </footer>
  </aside>
</template>
