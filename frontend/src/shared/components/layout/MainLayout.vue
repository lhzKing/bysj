<script setup>
import { computed, onMounted, onUnmounted, ref } from 'vue'
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

const drawerVisible = ref(false)
const mediaQuery = typeof window !== 'undefined' && typeof window.matchMedia === 'function'
  ? window.matchMedia('(max-width: 1023.98px)')
  : null
const isCompact = ref(Boolean(mediaQuery?.matches))

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
      @navigate="navigateTo"
      @logout="logout"
    />

    <div class="app-shell__main" :class="{ 'app-shell__main--compact': isCompact }">
      <AppTopbar
        :page-title="pageTitle"
        :is-compact="isCompact"
        @toggle-menu="openDrawer"
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
}

.app-shell__main--compact {
  margin-left: 0;
}

.app-shell__content {
  flex: 1 1 auto;
}
</style>
