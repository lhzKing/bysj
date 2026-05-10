<script setup>
import { computed, onMounted, onUnmounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/core/stores/user'
import { useConfirm } from '@/shared/composables/useConfirm'
import { useToast } from '@/shared/composables/useToast'
import AppSidebarNav from './AppSidebarNav.vue'
import AppTopbar from './AppTopbar.vue'
import MobileSidebarDrawer from './MobileSidebarDrawer.vue'
import { layoutNavigation, resolveActivePath } from './layoutNavigation'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const { confirm } = useConfirm()
const toast = useToast()

const SIDEBAR_COLLAPSE_KEY = 'app_sidebar_collapsed'

const drawerVisible = ref(false)
const mediaQuery = typeof window !== 'undefined' && typeof window.matchMedia === 'function'
  ? window.matchMedia('(max-width: 1023.98px)')
  : null
const isCompact = ref(Boolean(mediaQuery?.matches))

const readSavedCollapse = () => {
  if (typeof window === 'undefined' || !window.localStorage) return false
  try {
    return window.localStorage.getItem(SIDEBAR_COLLAPSE_KEY) === '1'
  } catch (err) {
    return false
  }
}
const isCollapsed = ref(readSavedCollapse())

const toggleCollapsed = () => {
  isCollapsed.value = !isCollapsed.value
}

watch(isCollapsed, (next) => {
  if (typeof window === 'undefined' || !window.localStorage) return
  try {
    window.localStorage.setItem(SIDEBAR_COLLAPSE_KEY, next ? '1' : '0')
  } catch (err) {
    // localStorage 不可用时静默忽略
  }
})

const navigation = computed(() =>
  layoutNavigation.filter((item) =>
    typeof userStore.hasAnyPermission === 'function'
      ? userStore.hasAnyPermission(item.permissions)
      : false
  )
)

const activePath = computed(() => resolveActivePath(route.path))
const username = computed(() => userStore.user?.username || '用户')
const userRole = computed(() => userStore.user?.roleName || '当前账号')
const pageTitle = computed(() => route.meta?.title || '')

const syncViewport = () => {
  isCompact.value = Boolean(mediaQuery?.matches)
  if (!isCompact.value) {
    drawerVisible.value = false
  }
}

const openDrawer = () => {
  drawerVisible.value = true
}

const closeDrawer = () => {
  drawerVisible.value = false
}

const navigateTo = async (path) => {
  drawerVisible.value = false
  if (!path || path === route.path) {
    return
  }
  await router.push(path)
}

// Top-bar ⌘K + click — both jump to /traces and ask the page to focus
// its search input (TraceList reads route.query.focus on mount/route-update).
const goSearch = async () => {
  drawerVisible.value = false
  if (route.path === '/traces') {
    // Already on TraceList — bump query so its watcher re-fires the focus.
    await router.replace({ path: '/traces', query: { ...route.query, focus: `search-${Date.now()}` } })
  } else {
    await router.push({ path: '/traces', query: { focus: 'search' } })
  }
}

const onGlobalKeydown = (event) => {
  // Cmd/Ctrl + K → open the trace search. Skip when the user is already
  // typing in an input/textarea/contenteditable so we don't hijack typing.
  if ((event.metaKey || event.ctrlKey) && (event.key === 'k' || event.key === 'K')) {
    const target = event.target
    const tag = target?.tagName
    const isEditable = tag === 'INPUT' || tag === 'TEXTAREA' || target?.isContentEditable
    if (isEditable) return
    event.preventDefault()
    goSearch()
  }
}

const logout = async () => {
  const accepted = await confirm({
    title: '退出登录',
    message: '确定要退出当前账号吗？',
    confirmText: '退出',
    cancelText: '取消',
    type: 'danger'
  })

  if (!accepted) {
    return
  }

  await userStore.logout()
  await router.replace('/login')
  toast.success('已退出登录')
  drawerVisible.value = false
}

onMounted(() => {
  window.addEventListener('keydown', onGlobalKeydown)
  if (!mediaQuery) {
    return
  }

  syncViewport()

  if (typeof mediaQuery.addEventListener === 'function') {
    mediaQuery.addEventListener('change', syncViewport)
    return
  }

  if (typeof mediaQuery.addListener === 'function') {
    mediaQuery.addListener(syncViewport)
  }
})

onUnmounted(() => {
  window.removeEventListener('keydown', onGlobalKeydown)
  if (!mediaQuery) {
    return
  }

  if (typeof mediaQuery.removeEventListener === 'function') {
    mediaQuery.removeEventListener('change', syncViewport)
    return
  }

  if (typeof mediaQuery.removeListener === 'function') {
    mediaQuery.removeListener(syncViewport)
  }
})
</script>

<template>
  <div class="app-shell" data-test="app-shell">
    <AppSidebarNav
      v-if="!isCompact"
      :items="navigation"
      :active-path="activePath"
      :username="username"
      :user-role="userRole"
      :collapsed="isCollapsed"
      @navigate="navigateTo"
      @toggle-collapsed="toggleCollapsed"
      @logout="logout"
    />

    <div
      class="app-shell__main"
      :class="{
        'app-shell__main--compact': isCompact,
        'app-shell__main--collapsed': !isCompact && isCollapsed
      }"
    >
      <AppTopbar
        :page-title="pageTitle"
        :is-compact="isCompact"
        @toggle-menu="openDrawer"
        @open-search="goSearch"
      />

      <main class="app-shell__content" data-test="app-content">
        <RouterView v-slot="{ Component }">
          <component :is="Component" />
        </RouterView>
      </main>
    </div>

    <MobileSidebarDrawer
      v-if="isCompact"
      :visible="drawerVisible"
      :items="navigation"
      :active-path="activePath"
      :username="username"
      :user-role="userRole"
      @close="closeDrawer"
      @navigate="navigateTo"
      @logout="logout"
    />
  </div>
</template>

<style scoped>
.app-shell {
  min-height: 100vh;
  background: var(--canvas);
  color: var(--ink-muted);
}

.app-shell__main {
  margin-left: 240px;
  display: flex;
  flex-direction: column;
  min-height: 100vh;
  transition: margin-left 0.18s ease;
}

.app-shell__main--collapsed {
  margin-left: 64px;
}

.app-shell__main--compact {
  margin-left: 0;
}

.app-shell__content {
  flex: 1 1 auto;
}
</style>
