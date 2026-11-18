<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { message } from 'ant-design-vue'

import { switchContext } from '../../api/admin'
import { submitLogout } from '../../api/auth'
import WebSocketNotifier from '../../components/admin/WebSocketNotifier.vue'
import { buildMenuGroups } from '../../router/dynamic'
import { clearDynamicRoutes, syncDynamicRoutes } from '../../router'
import { useAuthStore } from '../../stores/auth'
import { useWebSocketStore } from '../../stores/websocket'
import { isManualFeatureEnabled, openPeachManual } from '../../utils/manual'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const webSocketStore = useWebSocketStore()
const isSwitching = ref(false)
const selectedTenantKey = ref(`${authStore.session?.tenantRefId ?? ''}:${authStore.session?.appRefId ?? ''}`)

const menuGroups = computed(() => buildMenuGroups(authStore.loginInfo))
const tenantOptions = computed(() => (authStore.loginInfo?.tenantOptions ?? [])
  .filter(item => Boolean(item.tenantRefId && item.appRefId))
  .map(item => ({
    label: `${item.tenantName || item.tenantId} / ${item.appName || item.appId}`,
    value: `${item.tenantRefId}:${item.appRefId}`,
    tenantRefId: item.tenantRefId,
    appRefId: item.appRefId,
  })))

const title = computed(() => String(route.meta.title ?? 'Peach Cloud'))
const activePath = computed(() => route.path)
const roleLabel = computed(() => authStore.loginInfo?.roleList?.[0]?.roleName ?? '控制台用户')
const manualEnabled = isManualFeatureEnabled()
const avatarText = computed(() => {
  const name = authStore.session?.userName ?? 'U'
  return name.slice(0, 2).toUpperCase()
})

async function handleMenuClick(path: string) {
  await router.push(path)
}

async function handleSwitchContext() {
  if (isSwitching.value) {
    return
  }
  const option = tenantOptions.value.find(item => item.value === selectedTenantKey.value)
  if (!option || !authStore.session) {
    message.warning('请选择有效租户和应用')
    return
  }
  isSwitching.value = true
  let remoteSwitchCommitted = false
  try {
    const loginInfo = await switchContext({
      tenantRefId: option.tenantRefId,
      appRefId: option.appRefId,
    })
    remoteSwitchCommitted = true
    webSocketStore.disconnect()
    clearDynamicRoutes()
    authStore.replaceLoginInfo(loginInfo)
    syncDynamicRoutes()
    webSocketStore.connect(authStore.session)
    selectedTenantKey.value = `${authStore.session?.tenantRefId ?? ''}:${authStore.session?.appRefId ?? ''}`
    message.success('已切换租户及应用上下文')
    await router.replace(router.hasRoute('workspace-overview') ? '/workspace' : '/no-access')
  } catch (error: unknown) {
    if (remoteSwitchCommitted) {
      // Auth 已切换 Token 上下文，但前端应用状态更新不完整；禁止继续使用旧租户视图。
      webSocketStore.disconnect()
      authStore.clearSession()
      clearDynamicRoutes()
      message.error('后端切换已提交，但页面状态未能同步，请重新登录')
      await router.replace('/login')
      return
    }
    message.error(error instanceof Error ? error.message : '切换上下文失败')
    const session = authStore.session
    if (session) {
      selectedTenantKey.value = `${session.tenantRefId}:${session.appRefId}`
    }
  } finally {
    isSwitching.value = false
  }
}

async function logout() {
  if (isSwitching.value) {
    return
  }
  try {
    // 先让服务端撤销当前 Token，再清理本地会话；网络异常时也不得继续持有本地 Token。
    await submitLogout()
  } catch {
    message.warning('服务器未确认退出，但本地登录信息已清除')
  } finally {
    webSocketStore.disconnect()
    authStore.clearSession()
    clearDynamicRoutes()
    await router.replace('/login')
  }
}

function handleOpenManual() {
  openPeachManual('/')
}
</script>

<template>
  <div class="console-shell">
    <aside class="console-sidebar">
      <div class="console-sidebar__head">
        <div class="console-brand">
          <span class="console-brand__mark">P</span>
          <div class="console-brand__meta">
            <strong>Peach Cloud</strong>
            <small>CONTROL PLANE</small>
          </div>
        </div>
      </div>

      <nav class="console-nav">
        <section v-for="group in menuGroups" :key="group.title" class="console-nav__section">
          <div class="console-nav__title">{{ group.title }}</div>
          <button
            v-for="item in group.items"
            :key="item.path"
            type="button"
            class="console-nav__item"
            :class="{ 'is-active': activePath === item.path }"
            @click="handleMenuClick(item.path)"
          >
            <span>{{ item.meta.title }}</span>
          </button>
        </section>
      </nav>

      <div class="console-sidebar__foot">
        <div class="console-status">
          <div class="console-status__line">
            <span class="console-status__dot" />
            平台运行正常
          </div>
          <div class="console-status__sub">权限由后端统一装配</div>
        </div>
      </div>
    </aside>

    <header class="console-topbar">
      <div class="console-crumbs">
        <span>Peach Cloud</span>
        <span class="console-crumbs__sep">/</span>
        <strong>{{ title }}</strong>
      </div>
      <div class="console-topbar__actions">
        <WebSocketNotifier />
        <button
          v-if="manualEnabled"
          type="button"
          class="console-manual"
          title="在新标签页打开用户手册"
          @click="handleOpenManual"
        >
          用户手册
        </button>
        <a-select
          v-model:value="selectedTenantKey"
          class="context-select"
          :loading="isSwitching"
          :options="tenantOptions"
          :disabled="isSwitching || tenantOptions.length === 0"
          placeholder="选择租户与应用"
        />
        <button
          type="button"
          class="console-manual"
          :disabled="isSwitching || tenantOptions.length === 0"
          @click="handleSwitchContext"
        >
          {{ isSwitching ? '切换中' : '切换' }}
        </button>
        <button type="button" class="console-user" @click="router.push('/profile')">
          <span class="console-user__avatar">{{ avatarText }}</span>
          <span class="console-user__meta">
            <strong>{{ authStore.session?.userName }}</strong>
            <span>{{ roleLabel }}</span>
          </span>
        </button>
        <button type="button" class="console-logout" @click="logout">退出</button>
      </div>
    </header>

    <main class="console-main">
      <div class="console-page">
        <slot />
      </div>
    </main>
  </div>
</template>
