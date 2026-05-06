<script setup>
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/core/stores/user'
import { useConfirm } from '@/shared/composables/useConfirm'
import { useToast } from '@/shared/composables/useToast'
import AppFloatingNav from './AppFloatingNav.vue'
import AppMobileNavDrawer from './AppMobileNavDrawer.vue'
import AppContentFrame from './AppContentFrame.vue'
import { layoutNavigation, resolveActivePath } from './layoutNavigation'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const { confirm } = useConfirm()
const toast = useToast()

const drawerVisible = ref(false)
const mediaQuery = typeof window !== 'undefined' && typeof window.matchMedia === 'function'
  ? window.matchMedia('(max-width: 1023px)')
  : null
const isMobile = ref(Boolean(mediaQuery?.matches))

const navigation = computed(() =>
  layoutNavigation.filter((item) =>
    typeof userStore.hasAnyPermission === 'function'
      ? userStore.hasAnyPermission(item.permissions)
      : false
  )
)

const activePath = computed(() => resolveActivePath(route.path))
const username = computed(() => userStore.user?.username || '\u7528\u6237')

const syncViewport = (event) => {
  const matches = typeof event?.matches === 'boolean' ? event.matches : Boolean(mediaQuery?.matches)
  isMobile.value = matches

  if (!matches) {
    drawerVisible.value = false
  }
}

const openMobileDrawer = () => {
  drawerVisible.value = true
}

const closeMobileDrawer = () => {
  drawerVisible.value = false
}

const navigateTo = async (path) => {
  if (!path) {
    drawerVisible.value = false
    return
  }

  drawerVisible.value = false

  if (path === route.path) {
    return
  }

  await router.push(path)
}

const logout = async () => {
  const accepted = await confirm({
    title: '\u9000\u51fa\u767b\u5f55',
    message: '\u786e\u5b9a\u8981\u9000\u51fa\u5f53\u524d\u8d26\u53f7\u5417\uff1f',
    confirmText: '\u9000\u51fa',
    cancelText: '\u53d6\u6d88',
    type: 'danger'
  })

  if (!accepted) {
    return
  }

  await userStore.logout()
  await router.replace('/login')
  toast.success('\u5df2\u9000\u51fa\u767b\u5f55')
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
  <div class="app-shell relative min-h-screen" data-test="app-shell">
    <div class="mesh-bg"></div>
    <div class="grid-accent"></div>

    <AppFloatingNav
      :items="navigation"
      :active-path="activePath"
      :username="username"
      :is-mobile="isMobile"
      @navigate="navigateTo"
      @logout="logout"
      @toggle-menu="openMobileDrawer"
    />

    <AppContentFrame>
      <RouterView v-slot="{ Component }">
        <Transition name="app-shell-fade" mode="out-in">
          <component :is="Component" />
        </Transition>
      </RouterView>
    </AppContentFrame>

    <AppMobileNavDrawer
      v-if="isMobile"
      :visible="drawerVisible"
      :items="navigation"
      :active-path="activePath"
      :username="username"
      @close="closeMobileDrawer"
      @navigate="navigateTo"
      @logout="logout"
    />
  </div>
</template>
