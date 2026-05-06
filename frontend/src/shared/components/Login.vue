<script setup>
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Lock, User, Eye, EyeOff } from 'lucide-vue-next'
import { register as registerUser } from '@/core/api/auth'
import { useUserStore } from '@/core/stores/user'
import { useToast } from '@/shared/composables/useToast'

const COPY = {
  badge: '\u6bd5\u4e1a\u8bbe\u8ba1\u5c55\u793a',
  title: '\u5de5\u4e1a\u914d\u4ef6\u4f9b\u5e94\u94fe\u6eaf\u6e90\u7cfb\u7edf',
  subtitle: 'Neural Industrial Traceability System. 数字生命周期监控中枢。',
  help: '\u6f14\u793a\u8d26\u53f7\u8bf7\u8054\u7cfb\u7ba1\u7406\u5458\u6216\u67e5\u770b\u9879\u76ee\u8bf4\u660e',
  usernameLabel: '\u7528\u6237\u540d',
  usernamePlaceholder: '\u8bf7\u8f93\u5165\u7528\u6237\u540d',
  passwordLabel: '\u5bc6\u7801',
  passwordPlaceholder: '\u8bf7\u8f93\u5165\u5bc6\u7801',
  rememberMe: '\u8bb0\u4f4f\u767b\u5f55\u72b6\u6001',
  submitLogin: '\u767b\u5f55\u7cfb\u7edf',
  submitRegister: '注册新账号',
  successLogin: '\u767b\u5f55\u6210\u529f',
  successRegister: '注册成功，请登录',
  fallbackError: '\u767b\u5f55\u5931\u8d25\uff0c\u8bf7\u68c0\u67e5\u7528\u6237\u540d\u548c\u5bc6\u7801\u540e\u91cd\u8bd5\u3002',
  footer: '\u00a9 2026 \u5de5\u4e1a\u914d\u4ef6\u4f9b\u5e94\u94fe\u6eaf\u6e90\u7cfb\u7edf'
}

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const toast = useToast()

const isRegister = ref(false)
const username = ref('')
const password = ref('')
const confirmPassword = ref('')
const rememberMe = ref(false)
const errorMessage = ref('')
const loading = ref(false)
const showPassword = ref(false)

const canSubmit = computed(() => {
  if (isRegister.value) {
    return username.value.trim().length > 0 && password.value.trim().length > 0 && password.value === confirmPassword.value && !loading.value
  }
  return username.value.trim().length > 0 && password.value.trim().length > 0 && !loading.value
})

const toggleMode = () => {
  isRegister.value = !isRegister.value
  errorMessage.value = ''
  password.value = ''
  confirmPassword.value = ''
}

const handleSubmit = async () => {
  if (!canSubmit.value) return

  loading.value = true
  errorMessage.value = ''

  if (isRegister.value) {
    if (password.value.length < 6 || !/[a-zA-Z]/.test(password.value) || !/\d/.test(password.value)) {
      errorMessage.value = '密码必须包含字母和数字，长度6-100个字符'
      loading.value = false
      return
    }
    try {
      await registerUser(username.value, password.value)
      toast.success(COPY.successRegister)
      isRegister.value = false
      password.value = ''
      confirmPassword.value = ''
    } catch (error) {
      errorMessage.value = error?.message || '注册失败，用户名可能已存在'
      toast.error(errorMessage.value)
    } finally {
      loading.value = false
    }
    return
  }

  try {
    await userStore.login(username.value, password.value, rememberMe.value)
  } catch (error) {
    errorMessage.value = error?.message || COPY.fallbackError
    if (!error?.response && !error?.request && !error?.toastShown) {
      toast.error(errorMessage.value)
    }
    loading.value = false
    return
  }

  try {
    const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : '/'
    await router.push(redirect)
    toast.success(COPY.successLogin)
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="relative min-h-screen flex items-center justify-center overflow-hidden bg-[#fdfdff] p-4 md:p-8">
    <div class="mesh-bg"></div>
    <div class="grid-accent"></div>
    
    <div class="absolute top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2 size-[600px] bg-indigo-300 rounded-full blur-[120px] opacity-20 pointer-events-none"></div>

    <div class="premium-card relative z-10 w-full max-w-lg rounded-[40px] md:rounded-[56px] p-8 md:p-12 shadow-2xl flex flex-col gap-8 border-t border-white overflow-hidden group transition-all duration-500">
      
      <div class="absolute -right-20 -top-20 size-60 bg-emerald-100 rounded-full blur-[60px] opacity-30 group-hover:scale-150 transition-transform duration-1000 pointer-events-none"></div>

      <header class="space-y-4 relative z-10">
        <div class="inline-flex items-center px-4 py-1.5 rounded-full bg-indigo-50 border border-indigo-100/50 text-indigo-600 text-xs font-black uppercase tracking-widest mb-2 shadow-sm">
          {{ COPY.badge }}
        </div>
        <h1 class="text-3xl md:text-4xl font-black text-slate-900 tracking-tight leading-tight">
          TRACE<span class="text-indigo-600">.CORE</span>
        </h1>
        <p class="text-sm font-bold text-slate-400">{{ COPY.subtitle }}</p>
      </header>

      <form class="space-y-6 relative z-10" @submit.prevent="handleSubmit">
        <div class="space-y-5">
          <div data-test="login-username" class="group/input">
            <label class="block text-[10px] font-black text-slate-400 uppercase tracking-widest mb-2 ml-1 group-focus-within/input:text-indigo-600 transition-colors">{{ COPY.usernameLabel }}</label>
            <div class="relative">
              <div class="absolute inset-y-0 left-0 pl-4 flex items-center pointer-events-none">
                <User class="h-5 w-5 text-slate-400 group-focus-within/input:text-indigo-600 transition-colors" />
              </div>
              <input
                v-model="username"
                id="login-username"
                type="text"
                :placeholder="COPY.usernamePlaceholder"
                :disabled="loading"
                class="block w-full pl-11 pr-4 py-4 border-0 bg-slate-50/50 text-slate-900 rounded-2xl ring-1 ring-inset ring-slate-200 focus:ring-2 focus:ring-inset focus:ring-indigo-600 sm:text-sm font-bold transition-shadow shadow-inner placeholder:text-slate-400/70"
              />
            </div>
          </div>

          <div data-test="login-password" class="group/input">
            <label class="block text-[10px] font-black text-slate-400 uppercase tracking-widest mb-2 ml-1 group-focus-within/input:text-indigo-600 transition-colors">{{ COPY.passwordLabel }}</label>
            <div class="relative">
              <div class="absolute inset-y-0 left-0 pl-4 flex items-center pointer-events-none">
                <Lock class="h-5 w-5 text-slate-400 group-focus-within/input:text-indigo-600 transition-colors" />
              </div>
              <input
                v-model="password"
                id="login-password"
                :type="showPassword ? 'text' : 'password'"
                :placeholder="COPY.passwordPlaceholder"
                :disabled="loading"
                class="block w-full pl-11 pr-12 py-4 border-0 bg-slate-50/50 text-slate-900 rounded-2xl ring-1 ring-inset ring-slate-200 focus:ring-2 focus:ring-inset focus:ring-indigo-600 sm:text-sm font-bold transition-shadow shadow-inner placeholder:text-slate-400/70"
              />
              <button type="button" @click="showPassword = !showPassword" class="absolute inset-y-0 right-0 pr-4 flex items-center text-slate-400 hover:text-indigo-600 transition-colors focus:outline-none">
                <EyeOff v-if="showPassword" class="h-5 w-5" />
                <Eye v-else class="h-5 w-5" />
              </button>
            </div>
          </div>

          <div v-if="isRegister" data-test="register-confirm-password" class="group/input animate-[fadeIn_0.3s_ease-out]">
            <label class="block text-[10px] font-black text-slate-400 uppercase tracking-widest mb-2 ml-1 group-focus-within/input:text-indigo-600 transition-colors">确认密码</label>
            <div class="relative">
              <div class="absolute inset-y-0 left-0 pl-4 flex items-center pointer-events-none">
                <Lock class="h-5 w-5 text-slate-400 group-focus-within/input:text-indigo-600 transition-colors" />
              </div>
              <input
                v-model="confirmPassword"
                type="password"
                placeholder="请再次输入密码"
                :disabled="loading"
                class="block w-full pl-11 pr-4 py-4 border-0 bg-slate-50/50 text-slate-900 rounded-2xl ring-1 ring-inset ring-slate-200 focus:ring-2 focus:ring-inset focus:ring-indigo-600 sm:text-sm font-bold transition-shadow shadow-inner placeholder:text-slate-400/70"
              />
            </div>
          </div>
        </div>

        <div class="flex items-center justify-between pt-2">
          <label v-if="!isRegister" class="flex items-center gap-3 cursor-pointer group/check">
            <div class="relative flex items-center justify-center">
              <input v-model="rememberMe" data-test="remember-me" type="checkbox" :disabled="loading" class="peer sr-only" />
              <div class="w-5 h-5 border-2 border-slate-300 rounded bg-white peer-checked:bg-indigo-600 peer-checked:border-indigo-600 transition-all shadow-sm"></div>
              <svg class="absolute w-3 h-3 text-white opacity-0 peer-checked:opacity-100 transition-opacity pointer-events-none" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="3">
                <path stroke-linecap="round" stroke-linejoin="round" d="M5 13l4 4L19 7"/>
              </svg>
            </div>
            <span class="text-sm font-bold text-slate-500 group-hover/check:text-slate-700 transition-colors">{{ COPY.rememberMe }}</span>
          </label>
          <div v-else></div>
          
          <button type="button" @click="toggleMode" class="text-sm font-bold text-indigo-600 hover:text-indigo-800 transition-colors">
            {{ isRegister ? '已有账号？去登录' : '没有账号？去注册' }}
          </button>
        </div>

        <div v-if="errorMessage" class="p-4 bg-rose-50 border border-rose-100 rounded-2xl animate-[fadeIn_0.3s_ease-out]" data-test="login-error">
          <p class="text-sm font-bold text-rose-600 flex items-center gap-2">
            <svg class="w-4 h-4 shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2.5" d="M12 8v4m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"></path></svg>
            {{ errorMessage }}
          </p>
        </div>

        <button
          type="submit"
          :disabled="!canSubmit"
          data-test="login-submit"
          class="w-full py-4 mt-4 bg-indigo-600 text-white rounded-2xl font-black text-lg shadow-lg shadow-indigo-200 transition-all hover:bg-indigo-700 hover:shadow-xl hover:-translate-y-0.5 active:scale-95 disabled:opacity-50 disabled:cursor-not-allowed flex justify-center items-center gap-2"
        >
          <span v-if="loading" class="w-5 h-5 border-2 border-white/30 border-t-white rounded-full animate-spin"></span>
          {{ loading ? '连接网络中...' : (isRegister ? COPY.submitRegister : COPY.submitLogin) }}
        </button>
      </form>

      <footer class="relative z-10 pt-6 border-t border-slate-200/50 text-center">
        <p class="text-xs font-bold text-slate-400 mb-2">{{ COPY.help }}</p>
        <p class="text-[10px] font-black text-slate-300 uppercase tracking-widest">{{ COPY.footer }}</p>
      </footer>
    </div>
  </div>
</template>
