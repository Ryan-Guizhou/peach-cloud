<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { message } from 'ant-design-vue'

import { switchContext } from '../../api/admin'
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
const selectedOrgKey = ref(`${authStore.session?.tenantId ?? ''}:${authStore.session?.orgId ?? ''}`)

const menuGroups = computed(() => buildMenuGroups(authStore.loginInfo))
const userOrgOptions = computed(() => (authStore.loginInfo?.userOrgList ?? []).map(item => ({
  label: `${item.tenantName ?? item.tenantCode ?? '租户'} / ${item.orgName ?? item.orgCode ?? '机构'}`,
  value: `${item.tenantId ?? ''}:${item.orgId ?? ''}`,
  tenantId: item.tenantId ?? '',
  orgId: item.orgId ?? '',
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

async function handleSwitchContext(value: unknown) {
  const orgKey = String(value ?? '')
  const option = userOrgOptions.value.find(item => item.value === orgKey)
  if (!option || !authStore.session) {
    return
  }
  isSwitching.value = true
  try {
    const loginInfo = await switchContext({
      tenantId: option.tenantId,
      orgId: option.orgId,
      fiscal: Number(authStore.session.fiscal),
    })
    authStore.replaceLoginInfo(loginInfo)
    syncDynamicRoutes()
    message.success('已切换当前机构上下文')
    await router.replace('/workspace')
  } catch (error: unknown) {
    message.error(error instanceof Error ? error.message : '切换机构失败')
    selectedOrgKey.value = `${authStore.session.tenantId}:${authStore.session.orgId}`
  } finally {
    isSwitching.value = false
  }
}

async function logout() {
  webSocketStore.disconnect()
  authStore.clearSession()
  clearDynamicRoutes()
  await router.replace('/login')
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
          v-model:value="selectedOrgKey"
          class="context-select"
          :loading="isSwitching"
          :options="userOrgOptions"
          placeholder="选择机构上下文"
          @change="handleSwitchContext"
        />
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
