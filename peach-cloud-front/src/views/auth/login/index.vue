<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'

import { initLogin, submitLogin } from '../../../api/auth'
import AuthShell from '../../../components/auth/AuthShell.vue'
import BlockPuzzleCaptcha from '../../../components/auth/BlockPuzzleCaptcha.vue'
import { syncDynamicRoutes } from '../../../router'
import { useAuthStore } from '../../../stores/auth'
import type { LoginInitInfo } from '../../../types/auth'
import { encryptLoginPassword } from '../../../utils/rsa'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()
const loginUsername = ref('')
const loginPassword = ref('')
const isEmailFocused = ref(false)
const showPassword = ref(false)
const remember = ref(true)
const isInitializing = ref(true)
const isSubmitting = ref(false)
const initInfo = ref<LoginInitInfo | null>(null)
const formError = ref('')
const captchaVerification = ref('')
const captchaPassed = ref(false)

const passwordLength = computed(() => loginPassword.value.length)
const captchaRequired = computed(() => initInfo.value?.captchaRequired !== false)
const captchaType = computed(() => initInfo.value?.captchaType ?? 'BLOCKPUZZLE')
const canSubmit = computed(() => {
  if (!initInfo.value || isInitializing.value || isSubmitting.value) {
    return false
  }
  if (captchaRequired.value && !captchaPassed.value) {
    return false
  }
  return true
})

const initializeLogin = async () => {
  isInitializing.value = true
  formError.value = ''
  captchaVerification.value = ''
  captchaPassed.value = false
  try {
    initInfo.value = await initLogin()
    if (initInfo.value.captchaRequired === false) {
      captchaPassed.value = true
    }
  } catch (error: unknown) {
    formError.value = error instanceof Error ? error.message : '初始化登录环境失败，请重试'
  } finally {
    isInitializing.value = false
  }
}

const onCaptchaVerified = (verification: string) => {
  captchaVerification.value = verification
  captchaPassed.value = true
  formError.value = ''
}

const onCaptchaReset = () => {
  captchaVerification.value = ''
  captchaPassed.value = false
}

const handleSubmit = async () => {
  if (!initInfo.value) {
    formError.value = '登录环境尚未初始化，请先重试初始化'
    return
  }
  if (!loginUsername.value.trim() || !loginPassword.value) {
    formError.value = '请输入账号和密码'
    return
  }
  if (captchaRequired.value && !captchaPassed.value) {
    formError.value = '请先完成滑块验证'
    return
  }

  isSubmitting.value = true
  formError.value = ''
  try {
    const encryptedPassword = await encryptLoginPassword(loginPassword.value, initInfo.value.publicKey)
    const loginInfo = await submitLogin({
      username: loginUsername.value.trim(),
      password: encryptedPassword,
      fiscal: initInfo.value.fiscal,
      captchaVerification: captchaRequired.value ? captchaVerification.value : undefined,
    })
    authStore.establishSession(loginInfo, remember.value)
    syncDynamicRoutes()
    const redirect = typeof route.query.redirect === 'string'
      && route.query.redirect.startsWith('/')
      && !route.query.redirect.startsWith('//')
      ? route.query.redirect
      : '/workspace'
    await router.replace(redirect)
  } catch (error: unknown) {
    formError.value = error instanceof Error ? error.message : '登录失败，请稍后重试'
    captchaVerification.value = ''
    captchaPassed.value = !captchaRequired.value
  } finally {
    isSubmitting.value = false
    loginPassword.value = ''
  }
}

onMounted(initializeLogin)
</script>

<template>
  <AuthShell
    :system-name="initInfo?.systemName"
    :system-description="initInfo?.systemDescription"
    :is-typing="isEmailFocused"
    :show-password="showPassword"
    :password-length="passwordLength"
  >
    <div class="segment">
      <RouterLink to="/login" class="segment__item is-active">Login</RouterLink>
      <RouterLink to="/register" class="segment__item">Register</RouterLink>
    </div>

    <header class="form-header form-header--left">
      <div class="system-status" :class="{ 'system-status--error': formError && !initInfo }">
        <span class="system-status__dot" />
        {{ isInitializing ? '正在建立安全上下文' : initInfo ? '安全上下文已就绪' : '初始化失败' }}
      </div>
      <h2>进入数据中枢</h2>
      <p>认证成功后，将按当前租户与机构装配你的数据视图、菜单和权限。</p>
    </header>

    <form class="login-form" @submit.prevent="handleSubmit">
      <label class="field">
        <span>账号</span>
        <div class="field__control">
          <input
            v-model="loginUsername"
            type="text"
            autocomplete="username"
            placeholder="账号 / 手机号 / 邮箱"
            @focus="isEmailFocused = true"
            @blur="isEmailFocused = false"
          />
        </div>
      </label>

      <label class="field">
        <span>登录密码</span>
        <div class="field__control">
          <input
            v-model="loginPassword"
            :type="showPassword ? 'text' : 'password'"
            :placeholder="'•'.repeat(8)"
            autocomplete="current-password"
            :disabled="isInitializing"
          />
          <button
            type="button"
            class="password-toggle"
            :aria-label="showPassword ? '隐藏密码' : '显示密码'"
            @click="showPassword = !showPassword"
          >
            {{ showPassword ? '隐藏' : '显示' }}
          </button>
        </div>
      </label>

      <BlockPuzzleCaptcha
        v-if="initInfo && captchaRequired"
        :captcha-type="captchaType"
        :disabled="isInitializing || isSubmitting"
        @verified="onCaptchaVerified"
        @reset="onCaptchaReset"
      />

      <div class="form-row">
        <label class="remember-row">
          <button
            type="button"
            role="checkbox"
            :aria-checked="remember"
            :class="`remember-checkbox${remember ? ' is-checked' : ''}`"
            @click="remember = !remember"
          >
            <span v-if="remember" class="remember-indicator" />
          </button>
          <span>在此设备保持登录 7 天</span>
        </label>
        <RouterLink to="/forgot-password" class="forgot-link">忘记密码？</RouterLink>
      </div>

      <div v-if="formError" class="form-alert" role="alert">
        <span>{{ formError }}</span>
        <button v-if="!initInfo" type="button" @click="initializeLogin">重试初始化</button>
      </div>

      <button
        type="submit"
        class="primary-button"
        :disabled="!canSubmit"
        :aria-busy="isSubmitting"
      >
        <span v-if="isSubmitting" class="button-spinner" aria-hidden="true" />
        {{ isSubmitting ? '正在验证数据身份' : '安全登录' }}
      </button>
    </form>
  </AuthShell>
</template>
