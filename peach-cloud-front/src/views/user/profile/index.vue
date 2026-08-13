<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { RouterLink } from 'vue-router'

import { fetchCurrentProfile, selectProfileAvatar, updateCurrentProfile, uploadProfileAvatar } from '../../../api/profile'
import PeachCloudLogo from '../../../components/auth/PeachCloudLogo.vue'
import { useAuthStore } from '../../../stores/auth'
import type { AvatarHistoryItem, UserProfile, UserProfileUpdateRequest } from '../../../types/profile'

type ErrorWithExtras = Error & { cause?: unknown; response?: unknown }

const MAX_FILE_SIZE = 5 * 1024 * 1024
const ALLOWED_TYPES = new Set(['image/jpeg', 'image/png'])
const LOADING_TIP_MS = 3000
const DEV_DEBUG = true

const authStore = useAuthStore()
const fileInput = ref<HTMLInputElement | null>(null)
const profile = ref<UserProfile | null>(null)
const isLoading = ref(true)
const isLoadingSlow = ref(false)
const isUploading = ref(false)
const isSaving = ref(false)
const selectingId = ref('')
const errorMessage = ref('')
const errorDetail = ref('')
const successMessage = ref('')
let loadingTipTimer: ReturnType<typeof setTimeout> | null = null
let loadCount = 0

const safeGet = <T, K extends keyof T>(obj: T | null | undefined, key: K, fallback: T[K]): T[K] => {
  if (obj === null || obj === undefined) return fallback
  const value = obj[key]
  return value === null || value === undefined ? fallback : value
}

const safeGetString = (obj: unknown, key: string, fallback = ''): string => {
  if (obj === null || obj === undefined) return fallback
  const o = obj as Record<string, unknown>
  const value = o[key]
  if (typeof value === 'string') return value
  if (typeof value === 'number') return String(value)
  return fallback
}

const safeGetNumber = (obj: unknown, key: string, fallback = 0): number => {
  if (obj === null || obj === undefined) return fallback
  const o = obj as Record<string, unknown>
  const value = o[key]
  if (typeof value === 'number') return value
  if (typeof value === 'string') {
    const parsed = Number(value)
    return Number.isFinite(parsed) ? parsed : fallback
  }
  return fallback
}

const normalizeAvatarHistoryItem = (raw: unknown): AvatarHistoryItem | null => {
  if (!raw || typeof raw !== 'object') return null
  const avatarHistoryId = safeGetString(raw, 'avatarHistoryId')
  if (!avatarHistoryId) return null
  return {
    avatarHistoryId,
    fileId: safeGetString(raw, 'fileId'),
    avatarUrl: safeGetString(raw, 'avatarUrl'),
    sortNo: safeGetNumber(raw, 'sortNo', 0),
    isCurrent: safeGetNumber(raw, 'isCurrent', 0),
    createdTime: safeGetString(raw, 'createdTime'),
  }
}

const normalizeUserProfile = (raw: unknown, fallback: UserProfile | null): UserProfile => {
  const obj = (raw && typeof raw === 'object') ? (raw as Record<string, unknown>) : null
  const session = authStore.session
  const historyRaw = obj && Array.isArray(obj.avatarHistory) ? obj.avatarHistory : []
  const avatarHistory: AvatarHistoryItem[] = []
  for (const item of historyRaw) {
    const normalized = normalizeAvatarHistoryItem(item)
    if (normalized) avatarHistory.push(normalized)
  }
  let currentAvatar: AvatarHistoryItem | undefined
  const currentRaw = obj?.currentAvatar
  if (currentRaw && typeof currentRaw === 'object') {
    currentAvatar = normalizeAvatarHistoryItem(currentRaw) ?? undefined
  }
  if (!currentAvatar) {
    currentAvatar = avatarHistory.find(a => a.isCurrent === 1)
  }
  return {
    userId: safeGetString(obj, 'userId') || fallback?.userId || session?.userId || 'unknown',
    userCode: safeGetString(obj, 'userCode') || fallback?.userCode || session?.userName || 'unknown',
    userName: safeGetString(obj, 'userName') || fallback?.userName || session?.userName || '未知用户',
    mobilePhone: safeGetString(obj, 'mobilePhone') || fallback?.mobilePhone || undefined,
    email: safeGetString(obj, 'email') || fallback?.email || undefined,
    status: safeGetString(obj, 'status') || fallback?.status || '正常',
    lastestLogin: safeGetString(obj, 'lastestLogin') || fallback?.lastestLogin || undefined,
    defaultOrgId: safeGetString(obj, 'defaultOrgId') || fallback?.defaultOrgId || session?.orgId || undefined,
    currentAvatar,
    avatarHistory,
  }
}

const buildFallbackProfile = (): UserProfile | null => {
  const session = authStore.session
  if (!session) return null
  return {
    userId: session.userId,
    userCode: session.userName,
    userName: session.userName,
    mobilePhone: undefined,
    email: undefined,
    status: '正常',
    lastestLogin: undefined,
    defaultOrgId: session.orgId,
    currentAvatar: undefined,
    avatarHistory: [],
  }
}

const fallbackProfile = computed<UserProfile | null>(buildFallbackProfile)

const profileForm = reactive<UserProfileUpdateRequest>({
  userName: fallbackProfile.value?.userName || '',
  mobilePhone: fallbackProfile.value?.mobilePhone || '',
  email: fallbackProfile.value?.email || '',
})

const displayProfile = computed<UserProfile | null>(() => profile.value || fallbackProfile.value)

const initials = computed(() => {
  const name = displayProfile.value?.userName || authStore.session?.userName || 'U'
  return name.slice(0, 2).toUpperCase()
})

const clearLoadingTipTimer = () => {
  if (loadingTipTimer) {
    try { clearTimeout(loadingTipTimer) } catch { /* ignore */ }
    loadingTipTimer = null
  }
}

const startLoadingTipTimer = () => {
  clearLoadingTipTimer()
  isLoadingSlow.value = false
  try {
    loadingTipTimer = setTimeout(() => {
      isLoadingSlow.value = true
    }, LOADING_TIP_MS)
  } catch { /* ignore */ }
}

const stopLoading = () => {
  clearLoadingTipTimer()
  isLoading.value = false
  isLoadingSlow.value = false
}

const applyProfileToForm = (p: UserProfile) => {
  profileForm.userName = safeGet(p, 'userName', '')
  profileForm.mobilePhone = safeGet(p, 'mobilePhone', '')
  profileForm.email = safeGet(p, 'email', '')
}

const getErrorExtras = (error: unknown): string => {
  if (error instanceof Error) {
    const withExtra = error as ErrorWithExtras
    const parts: string[] = []
    if (withExtra.cause instanceof Error) {
      parts.push(`[底层错误] ${withExtra.cause.message}`)
    }
    if (withExtra.response && typeof withExtra.response === 'object') {
      try {
        parts.push(`[响应体] ${JSON.stringify(withExtra.response, null, 2)}`)
      } catch {
        parts.push(`[响应体] ${String(withExtra.response)}`)
      }
    }
    if (parts.length > 0) return parts.join('\n\n')
    if (error.stack) return error.stack
  }
  return ''
}

const loadProfile = async (): Promise<void> => {
  loadCount += 1
  const runId = loadCount
  isLoading.value = true
  errorMessage.value = ''
  errorDetail.value = ''
  successMessage.value = ''
  startLoadingTipTimer()
  if (DEV_DEBUG) {
    // eslint-disable-next-line no-console
    console.debug(`[profile#${runId}] start load`)
  }

  try {
    const raw = await fetchCurrentProfile()
    if (DEV_DEBUG) {
      // eslint-disable-next-line no-console
      console.debug(`[profile#${runId}] raw result:`, raw)
    }
    const normalized = normalizeUserProfile(raw, fallbackProfile.value)
    profile.value = normalized
    applyProfileToForm(normalized)
    if (DEV_DEBUG) {
      // eslint-disable-next-line no-console
      console.debug(`[profile#${runId}] assigned profile=`, profile.value, 'avatarHistory=', normalized.avatarHistory)
    }
  } catch (error: unknown) {
    if (DEV_DEBUG) {
      // eslint-disable-next-line no-console
      console.error(`[profile#${runId}] load error`, error)
    }
    const msg = error instanceof Error ? error.message : '个人资料加载失败'
    errorMessage.value = msg
    errorDetail.value = getErrorExtras(error)
    const fb = fallbackProfile.value
    if (fb) {
      try {
        applyProfileToForm(fb)
      } catch { /* ignore */ }
    }
  } finally {
    stopLoading()
    if (DEV_DEBUG) {
      // eslint-disable-next-line no-console
      console.debug(`[profile#${runId}] finally, isLoading=`, isLoading.value, 'profile=', profile.value, 'displayProfile=', displayProfile.value)
    }
  }
}

const openFilePicker = () => fileInput.value?.click()

const handleSaveProfile = async () => {
  const userName = profileForm.userName.trim()
  const mobilePhone = profileForm.mobilePhone?.trim() || undefined
  const email = profileForm.email?.trim() || undefined
  errorMessage.value = ''
  errorDetail.value = ''
  successMessage.value = ''
  if (!userName) {
    errorMessage.value = '用户名称不能为空'
    return
  }

  isSaving.value = true
  try {
    const raw = await updateCurrentProfile({ userName, mobilePhone, email })
    const normalized = normalizeUserProfile(raw, fallbackProfile.value)
    profile.value = normalized
    applyProfileToForm(normalized)
    try { authStore.updateProfileName(normalized.userName) } catch { /* ignore */ }
    successMessage.value = '个人信息已更新'
  } catch (error: unknown) {
    errorMessage.value = error instanceof Error ? error.message : '个人信息保存失败'
    errorDetail.value = getErrorExtras(error)
  } finally {
    isSaving.value = false
  }
}

const handleFileChange = async (event: Event) => {
  const target = event.target
  if (!(target instanceof HTMLInputElement) || !target.files?.length) {
    return
  }
  const file = target.files[0]
  target.value = ''
  errorMessage.value = ''
  errorDetail.value = ''
  successMessage.value = ''
  if (!ALLOWED_TYPES.has(file.type)) {
    errorMessage.value = '仅支持 JPG 和 PNG 图片'
    return
  }
  if (file.size > MAX_FILE_SIZE) {
    errorMessage.value = '头像文件不能超过 5MB'
    return
  }

  isUploading.value = true
  try {
    await uploadProfileAvatar(file)
    successMessage.value = '新头像已上传并设为当前头像'
    await loadProfile()
  } catch (error: unknown) {
    errorMessage.value = error instanceof Error ? error.message : '头像上传失败'
    errorDetail.value = getErrorExtras(error)
  } finally {
    isUploading.value = false
  }
}

const handleSelectAvatar = async (avatarHistoryId: string) => {
  if (selectingId.value) return
  if (!avatarHistoryId) return
  selectingId.value = avatarHistoryId
  errorMessage.value = ''
  errorDetail.value = ''
  successMessage.value = ''
  try {
    await selectProfileAvatar(avatarHistoryId)
    successMessage.value = '历史头像已切换并移动到首位'
    await loadProfile()
  } catch (error: unknown) {
    errorMessage.value = error instanceof Error ? error.message : '头像切换失败'
    errorDetail.value = getErrorExtras(error)
  } finally {
    selectingId.value = ''
  }
}

onMounted(() => {
  Promise.resolve()
    .then(() => loadProfile())
    .catch((e) => {
      if (DEV_DEBUG) {
        // eslint-disable-next-line no-console
        console.error('[profile] onMounted uncaught error:', e)
      }
      errorMessage.value = e instanceof Error ? e.message : '初始化失败'
      errorDetail.value = getErrorExtras(e)
      stopLoading()
    })
})

onBeforeUnmount(clearLoadingTipTimer)
</script>

<template>
  <div class="profile-page">
    <header class="profile-topbar">
      <PeachCloudLogo />
      <RouterLink class="profile-back" to="/workspace">返回数据总览</RouterLink>
    </header>

    <main class="profile-content">
      <section class="profile-heading">
        <div>
          <span>IDENTITY CONTROL CENTER</span>
          <h1>个人中心</h1>
          <p>管理个人资料、当前身份上下文与头像历史。</p>
        </div>
        <span class="profile-status"><i /> 账户已验证</span>
      </section>

      <!-- 加载中 -->
      <div v-if="isLoading" class="profile-state profile-state--loading" aria-live="polite">
        <div class="profile-spinner" aria-hidden="true" />
        <div class="profile-state__body">
          <strong>正在同步个人资料…</strong>
          <small v-if="isLoadingSlow">请求耗时较长，请确认网关 (18080)、Nacos 和 peach-auth 服务是否已启动</small>
        </div>
        <button type="button" class="profile-state__retry" @click="loadProfile">重新加载</button>
      </div>

      <!-- 加载完成 -->
      <template v-else>
        <!-- 错误提示：接口异常（但有 fallback 兜底资料也可编辑） -->
        <div
          v-if="errorMessage && displayProfile"
          class="profile-feedback profile-feedback--block is-error"
          role="alert"
        >
          <div class="profile-feedback__main">
            <strong>接口异常（已显示本地缓存身份）</strong>
            <p>{{ errorMessage }}</p>
            <pre v-if="errorDetail" class="profile-feedback__detail">{{ errorDetail }}</pre>
          </div>
          <button type="button" class="profile-retry-btn" @click="loadProfile">重新加载</button>
        </div>

        <!-- 中性提示：接口没出错，但 profile 没加载（可能是代码异常），这里显示 fallback 兜底 -->
        <div v-else-if="!profile && displayProfile" class="profile-feedback profile-feedback--block" role="status">
          <div class="profile-feedback__main">
            <strong>已加载本地会话信息</strong>
            <p>当前使用登录会话中的最小身份信息，部分功能可能受限。请启动后端服务后点击重新加载。</p>
          </div>
          <button type="button" class="profile-retry-btn" @click="loadProfile">重新加载</button>
        </div>

        <!-- 成功提示 -->
        <div v-if="successMessage && !errorMessage" class="profile-feedback" role="status">
          {{ successMessage }}
        </div>

        <!-- 身份卡片 + 个人信息卡片（只要 displayProfile 就渲染，不管其它状态） -->
        <section class="profile-grid" v-if="displayProfile">
          <article class="profile-card profile-identity">
            <span class="profile-card__eyebrow">当前身份</span>
            <div class="profile-avatar profile-avatar--large">
              <img v-if="displayProfile.currentAvatar?.avatarUrl" :src="displayProfile.currentAvatar.avatarUrl" :alt="`${displayProfile.userName}的头像`" />
              <strong v-else>{{ initials }}</strong>
              <span class="profile-avatar__live" aria-hidden="true" />
            </div>
            <h2>{{ displayProfile.userName }}</h2>
            <p>@{{ displayProfile.userCode }}</p>
            <div class="profile-context">
              <div><span>租户</span><strong>{{ authStore.session?.tenantName || authStore.session?.tenantId }}</strong></div>
              <div><span>机构</span><strong>{{ authStore.session?.orgName || authStore.session?.orgId }}</strong></div>
            </div>
          </article>

          <article class="profile-card profile-details">
            <header>
              <div>
                <span class="profile-card__eyebrow">PROFILE DATA</span>
                <h2>个人信息</h2>
              </div>
              <small>可编辑</small>
            </header>
            <form class="profile-form" @submit.prevent="handleSaveProfile">
              <label class="profile-field">
                <span>用户名称</span>
                <input v-model="profileForm.userName" type="text" maxlength="50" autocomplete="name" />
              </label>
              <label class="profile-field">
                <span>手机号码</span>
                <input v-model="profileForm.mobilePhone" type="tel" maxlength="20" autocomplete="tel" placeholder="未设置" />
              </label>
              <label class="profile-field">
                <span>邮箱地址</span>
                <input v-model="profileForm.email" type="email" maxlength="100" autocomplete="email" placeholder="未设置" />
              </label>
              <div class="profile-readonly-grid">
                <div>
                  <span>用户编号</span>
                  <strong>{{ displayProfile.userId }}</strong>
                </div>
                <div>
                  <span>登录账号</span>
                  <strong>{{ displayProfile.userCode }}</strong>
                </div>
                <div>
                  <span>账户状态</span>
                  <strong><em>{{ displayProfile.status || '正常' }}</em></strong>
                </div>
                <div>
                  <span>默认机构</span>
                  <strong>{{ displayProfile.defaultOrgId || '未设置' }}</strong>
                </div>
              </div>
              <button class="profile-save" type="submit" :disabled="isSaving">
                {{ isSaving ? '正在保存…' : '保存个人信息' }}
              </button>
            </form>
          </article>
        </section>

        <!-- 头像管理卡片 -->
        <section class="profile-card avatar-manager" v-if="displayProfile">
          <header>
            <div>
              <span class="profile-card__eyebrow">AVATAR TIMELINE</span>
              <h2>头像管理</h2>
              <p>保留最近 10 张。选择历史头像后，该头像会成为当前头像并移动到首位。</p>
            </div>
            <div>
              <input ref="fileInput" class="visually-hidden" type="file" accept="image/jpeg,image/png" @change="handleFileChange" />
              <button class="profile-upload" type="button" :disabled="isUploading" @click="openFilePicker">
                {{ isUploading ? '正在上传…' : '上传新头像' }}
              </button>
              <small>JPG / PNG，最大 5MB</small>
            </div>
          </header>

          <div v-if="!displayProfile.avatarHistory || displayProfile.avatarHistory.length === 0" class="avatar-empty">
            <p>还没有头像记录，上传第一张头像来建立你的身份视觉。</p>
            <small>提示：若此处一直为空，可能是头像历史接口未返回数据，请检查后端 peach-fileservice 服务是否启动。</small>
          </div>

          <div v-else class="avatar-history" aria-label="历史头像">
            <button
              v-for="avatar in displayProfile.avatarHistory"
              :key="avatar.avatarHistoryId"
              class="avatar-history__item"
              :class="{ 'is-current': avatar.isCurrent === 1 }"
              type="button"
              :disabled="avatar.isCurrent === 1 || Boolean(selectingId)"
              :aria-label="avatar.isCurrent === 1 ? '当前头像' : '选择此历史头像'"
              @click="handleSelectAvatar(avatar.avatarHistoryId)"
            >
              <span class="avatar-history__image">
                <img v-if="avatar.avatarUrl" :src="avatar.avatarUrl" alt="历史头像" loading="lazy" />
                <strong v-else>{{ initials }}</strong>
              </span>
              <span>
                {{ avatar.isCurrent === 1 ? '当前使用' : selectingId === avatar.avatarHistoryId ? '切换中…' : `历史 #${avatar.sortNo}` }}
              </span>
            </button>
          </div>
        </section>

        <!-- 完全没资料的兜底：引导返回登录页 -->
        <div v-if="!displayProfile" class="profile-state profile-state--error">
          <span>无法加载任何身份信息，请返回登录页重新登录。</span>
          <RouterLink class="profile-back profile-back--inline" to="/login">返回登录</RouterLink>
        </div>
      </template>
    </main>
  </div>
</template>

<style scoped>
.profile-page { min-height: 100vh; background: radial-gradient(circle at 82% 0, rgba(36,117,255,.12), transparent 28%), var(--color-bg-canvas); color: var(--color-text-primary); }
.profile-topbar { display: flex; align-items: center; justify-content: space-between; min-height: 82px; padding: 0 clamp(20px,5vw,72px); border-bottom: 1px solid var(--color-border); background: rgba(7,17,31,.82); backdrop-filter: blur(18px); }
.profile-back { display: inline-flex; align-items: center; min-height: 44px; padding: 0 15px; border: 1px solid var(--color-border); border-radius: var(--control-radius); color: var(--color-text-secondary); font-size: 12px; }
.profile-back--inline { margin-left: 12px; padding: 10px 14px; color: white; background: var(--color-accent-strong); border-color: transparent; }
.profile-content { width: min(1160px, calc(100% - 40px)); margin: 0 auto; padding: 46px 0 70px; }
.profile-heading { display: flex; align-items: flex-start; justify-content: space-between; gap: 24px; margin-bottom: 28px; }
.profile-heading > div > span,.profile-card__eyebrow { color: var(--cyan-300); font-size: 10px; font-weight: 800; letter-spacing: .16em; }
.profile-heading h1 { margin: 9px 0 0; font-size: clamp(32px,4vw,48px); letter-spacing: -.05em; }.profile-heading p { margin: 9px 0 0; color: var(--color-text-muted); }
.profile-status { display: inline-flex; align-items: center; gap: 8px; color: var(--color-success); font-size: 11px; }.profile-status i { width: 7px; height: 7px; border-radius: 50%; background: currentColor; box-shadow: 0 0 12px currentColor; }
.profile-grid { display: grid; grid-template-columns: minmax(250px,.68fr) minmax(0,1.32fr); gap: 16px; margin-top: 16px; }.profile-card { border: 1px solid var(--color-border); border-radius: var(--card-radius); background: linear-gradient(145deg, rgba(16,32,54,.94), rgba(8,21,37,.86)); box-shadow: var(--shadow-raised); }
.profile-identity { display: flex; align-items: center; flex-direction: column; padding: 30px; text-align: center; }.profile-avatar { position: relative; display: grid; overflow: hidden; place-items: center; border: 1px solid var(--color-border-strong); border-radius: 50%; background: radial-gradient(circle, rgba(33,200,239,.22), rgba(7,17,31,.9)); }.profile-avatar--large { width: 128px; height: 128px; margin: 24px 0 16px; }.profile-avatar img,.avatar-history__image img { width: 100%; height: 100%; object-fit: cover; }.profile-avatar strong { font-size: 32px; }.profile-avatar__live { position: absolute; right: 8px; bottom: 12px; width: 13px; height: 13px; border: 3px solid var(--navy-900); border-radius: 50%; background: var(--color-success); }.profile-identity h2 { margin: 0; font-size: 24px; }.profile-identity > p { margin: 6px 0 22px; color: var(--color-text-muted); font-size: 12px; }
.profile-context { width: 100%; border-top: 1px solid var(--color-border); }.profile-context div { display: flex; justify-content: space-between; gap: 10px; padding: 13px 0; border-bottom: 1px solid var(--color-border); text-align: left; }.profile-context span,.profile-context strong { font-size: 11px; }.profile-context span { color: var(--color-text-muted); }.profile-context strong { overflow: hidden; max-width: 65%; text-overflow: ellipsis; white-space: nowrap; }
.profile-details { padding: 28px; }.profile-details header,.avatar-manager header { display: flex; align-items: flex-start; justify-content: space-between; gap: 20px; }.profile-details h2,.avatar-manager h2 { margin: 7px 0 0; font-size: 19px; }.profile-details header small { color: var(--color-text-muted); }.profile-form { display: grid; grid-template-columns: 1fr 1fr; gap: 14px; margin-top: 24px; }.profile-field { display: flex; min-width: 0; flex-direction: column; gap: 8px; }.profile-field:first-child { grid-column: 1 / -1; }.profile-field span,.profile-readonly-grid span { color: var(--color-text-muted); font-size: 10px; }.profile-field input { width: 100%; min-height: 44px; padding: 0 13px; border: 1px solid var(--color-border); border-radius: var(--control-radius); outline: none; background: rgba(7,17,31,.54); color: var(--color-text-primary); }.profile-field input:focus { border-color: var(--color-border-strong); box-shadow: 0 0 0 3px rgba(33,200,239,.13); }.profile-field input::placeholder { color: var(--color-text-muted); }.profile-readonly-grid { display: grid; grid-column: 1 / -1; grid-template-columns: 1fr 1fr; gap: 1px; overflow: hidden; border: 1px solid var(--color-border); border-radius: var(--radius-sm); background: var(--color-border); }.profile-readonly-grid div { min-width: 0; padding: 13px; background: rgba(7,17,31,.58); }.profile-readonly-grid strong { display: block; overflow: hidden; margin-top: 7px; color: var(--color-text-secondary); font-size: 12px; font-style: normal; text-overflow: ellipsis; white-space: nowrap; }.profile-readonly-grid em { padding: 4px 8px; border-radius: 999px; background: rgba(37,216,179,.1); color: var(--color-success); font-style: normal; }.profile-save { justify-self: start; min-height: 44px; padding: 0 17px; border-radius: var(--control-radius); background: linear-gradient(120deg,#087a99,#1a67d8); color: white; font-weight: 800; }
.avatar-manager { margin-top: 16px; padding: 28px; }.avatar-manager header p { max-width: 620px; margin: 8px 0 0; color: var(--color-text-muted); font-size: 12px; line-height: 1.6; }.avatar-manager header > div:last-child { display: flex; align-items: flex-end; flex-direction: column; gap: 7px; }.avatar-manager header small { color: var(--color-text-muted); font-size: 9px; }.profile-upload { min-height: 44px; padding: 0 17px; border-radius: var(--control-radius); background: linear-gradient(120deg,#087a99,#1a67d8); color: white; font-weight: 800; }.profile-upload:focus-visible,.avatar-history__item:focus-visible,.profile-back:focus-visible,.profile-save:focus-visible { outline: 3px solid rgba(102,227,255,.55); outline-offset: 3px; }
.avatar-history { display: grid; grid-template-columns: repeat(5,minmax(100px,1fr)); gap: 14px; margin-top: 26px; }.avatar-history__item { display: flex; align-items: center; flex-direction: column; gap: 9px; min-height: 144px; padding: 12px; border: 1px solid var(--color-border); border-radius: 13px; background: rgba(7,17,31,.6); color: var(--color-text-muted); font-size: 10px; }.avatar-history__item:not(:disabled):hover { border-color: var(--color-border-strong); transform: translateY(-2px); }.avatar-history__item.is-current { border-color: var(--color-border-strong); color: var(--cyan-300); box-shadow: inset 0 0 28px rgba(33,200,239,.08); }.avatar-history__image { display: grid; overflow: hidden; width: 84px; height: 84px; place-items: center; border-radius: 50%; background: var(--color-bg-raised); }.avatar-empty,.profile-state { margin-top: 20px; padding: 28px; border: 1px dashed var(--color-border); border-radius: var(--card-radius); color: var(--color-text-muted); text-align: center; }.avatar-empty p { margin: 0 0 8px; color: var(--color-text-secondary); }.avatar-empty small { font-size: 10px; line-height: 1.6; opacity: .85; }.profile-state--loading { display: flex; align-items: center; gap: 18px; text-align: left; }.profile-state__body { flex: 1; min-width: 0; }.profile-state--loading strong { display: block; color: var(--color-text-secondary); font-size: 13px; }.profile-state--loading small { display: block; margin-top: 4px; color: var(--color-danger); font-size: 11px; line-height: 1.6; }.profile-state--error { display: flex; align-items: center; justify-content: center; gap: 12px; flex-wrap: wrap; }.profile-state__retry { min-height: 40px; padding: 0 14px; border-radius: 8px; color: white; background: var(--color-accent-strong); font-size: 12px; flex: none; }.profile-spinner { width: 38px; height: 38px; flex: none; border: 3px solid var(--color-border); border-top-color: var(--cyan-400); border-radius: 50%; animation: profile-spin 1s linear infinite; }.profile-feedback { margin-bottom: 16px; padding: 12px 15px; border: 1px solid rgba(37,216,179,.25); border-radius: 10px; background: rgba(37,216,179,.08); color: var(--color-success); font-size: 12px; }.profile-feedback.is-error { border-color: rgba(255,110,124,.25); background: rgba(255,110,124,.08); color: var(--color-danger); }.profile-feedback--block { display: flex; align-items: flex-start; justify-content: space-between; gap: 16px; padding: 18px 20px; margin-top: 0; margin-bottom: 16px; border-style: solid; }.profile-feedback--block .profile-feedback__main strong { display: block; margin-bottom: 6px; font-size: 13px; }.profile-feedback--block .profile-feedback__main p { margin: 0; color: inherit; line-height: 1.6; font-size: 12px; opacity: .92; }.profile-feedback--block.is-error .profile-feedback__main strong { color: var(--color-danger); }.profile-feedback__detail { margin: 10px 0 0; padding: 10px 12px; max-height: 160px; overflow: auto; border-radius: 8px; background: rgba(0,0,0,.35); color: var(--color-text-secondary); font-size: 10px; line-height: 1.6; white-space: pre-wrap; word-break: break-all; }.profile-retry-btn { min-height: 36px; padding: 0 14px; border-radius: 8px; color: white; background: var(--color-accent-strong); font-size: 12px; flex: none; }.visually-hidden { position: absolute; overflow: hidden; width: 1px; height: 1px; clip: rect(0,0,0,0); white-space: nowrap; }
@keyframes profile-spin { to { transform: rotate(360deg); } }
@media (max-width: 800px) { .profile-grid { grid-template-columns: 1fr; }.avatar-history { grid-template-columns: repeat(3,1fr); }.profile-heading,.avatar-manager header,.profile-state--loading,.profile-feedback--block { flex-direction: column; }.avatar-manager header > div:last-child { align-items: flex-start; } }
@media (max-width: 520px) { .profile-content { width: min(100% - 24px,1160px); padding-top: 30px; }.profile-form,.profile-readonly-grid { grid-template-columns: 1fr; }.avatar-history { grid-template-columns: repeat(2,1fr); }.profile-topbar { min-height: 70px; padding: 0 12px; } }
@media (prefers-reduced-motion: reduce) { * { animation-duration: .01ms !important; animation-iteration-count: 1 !important; transition-duration: .01ms !important; } }
</style>
