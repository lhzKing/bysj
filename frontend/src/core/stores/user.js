import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import { getUserInfo as getUserInfoApi, login as loginApi, logout as logoutApi } from '@/core/api/auth'
import {
  clearAuthSession,
  readCachedUser,
  readToken,
  writeAuthSession,
  writeCachedUser,
  writeToken
} from '@/core/auth/authStorage'

/**
 * User auth store.
 * Keeps token, current user, and permissions in sync with backend auth endpoints.
 */
export const useUserStore = defineStore('user', () => {
  const token = ref(readToken())
  const user = ref(readCachedUser())

  const isLoggedIn = computed(() => !!token.value && !!user.value)
  const permissions = computed(() => user.value?.permissions || [])

  function setUser(userData, tokenValue) {
    user.value = userData
    token.value = tokenValue
    writeAuthSession({ user: userData, token: tokenValue })
  }

  function clearUser() {
    user.value = null
    token.value = ''
    clearAuthSession()
  }

  async function login(username, password, rememberMe = false) {
    try {
      const data = await loginApi(username, password, rememberMe)
      const tokenVal = data.token || data.access_token

      token.value = tokenVal
      writeToken(tokenVal)

      await fetchUserInfo()
      return true
    } catch (error) {
      clearUser()
      throw error
    }
  }

  async function fetchUserInfo() {
    if (!token.value) return

    try {
      const data = await getUserInfoApi()
      const roleCode = data.roleCode || data.role || ''

      user.value = {
        id: data.id,
        username: data.username,
        role: roleCode,
        roleCode,
        roleName: data.roleName || '',
        permissions: data.permissions || []
      }
      writeCachedUser(user.value)
    } catch (error) {
      console.error('Failed to fetch user info:', error)
      clearUser()
      throw error
    }
  }

  async function logout() {
    try {
      await logoutApi()
    } catch (error) {
      console.error('Logout error:', error)
    } finally {
      clearUser()
    }
  }

  function hasPermission(permission) {
    return permissions.value.includes(permission)
  }

  function hasAnyPermission(requiredPermissions) {
    if (!requiredPermissions || requiredPermissions.length === 0) {
      return true
    }
    return requiredPermissions.some((permission) => permissions.value.includes(permission))
  }

  return {
    token,
    user,
    isLoggedIn,
    permissions,
    login,
    logout,
    fetchUserInfo,
    hasPermission,
    hasAnyPermission,
    setUser,
    clearUser
  }
})
