<script setup>
const COPY = {
  workspace: '\u7cfb\u7edf\u5de5\u4f5c\u53f0',
  currentUser: '\u5f53\u524d\u7528\u6237\uff1a'
}

const props = defineProps({
  eyebrow: {
    type: String,
    default: '\u7cfb\u7edf\u5de5\u4f5c\u53f0'
  },
  title: {
    type: String,
    default: '\u4eea\u8868\u76d8'
  },
  username: {
    type: String,
    default: '\u7528\u6237'
  },
  isMobile: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits(['toggle-menu'])
</script>

<template>
  <header class="app-topbar" data-test="app-topbar">
    <div class="app-topbar__actions">
      <button
        v-if="isMobile"
        type="button"
        class="app-icon-button"
        data-test="mobile-nav-toggle"
        :aria-label="'\u6253\u5f00\u5bfc\u822a\u83dc\u5355'"
        @click="emit('toggle-menu')"
      >
        <span class="app-icon-button__line"></span>
        <span class="app-icon-button__line"></span>
        <span class="app-icon-button__line"></span>
      </button>
      <span v-else class="app-topbar__meta">{{ COPY.workspace }}</span>
    </div>

    <div class="app-topbar__content">
      <p class="app-topbar__eyebrow">{{ eyebrow }}</p>
      <h2 class="app-topbar__title" data-test="page-title">{{ title }}</h2>
    </div>

    <div class="app-user-chip app-topbar__user">
      <span class="app-user-chip__avatar" aria-hidden="true">{{ username.slice(0, 1).toUpperCase() }}</span>
      <span class="app-user-chip__body">
        <span class="app-user-chip__name">{{ username }}</span>
        <span class="app-user-chip__meta">{{ COPY.currentUser }}{{ username }}</span>
      </span>
    </div>
  </header>
</template>
