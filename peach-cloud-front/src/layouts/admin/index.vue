<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import type { MenuInfo } from 'ant-design-vue/es/menu/src/interface'

import { switchContext } from '../../api/admin'
import WebSocketNotifier from '../../components/admin/WebSocketNotifier.vue'
import { buildMenuItems } from '../../router/dynamic'
import { clearDynamicRoutes, syncDynamicRoutes } from '../../router'
import { useAuthStore } from '../../stores/auth'
import { useWebSocketStore } from '../../stores/websocket'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const webSocketStore = useWebSocketStore()
const isSwitching = ref(false)
const selectedOrgKey = ref(`${authStore.session?.tenantId ?? ''}:${authStore.session?.orgId ?? ''}`)

const menuItems = computed(() => buildMenuItems(authStore.loginInfo))
const userOrgOptions = computed(() => (authStore.loginInfo?.userOrgList ?? []).map(item => ({
  label: `${item.tenantName ?? item.tenantCode ?? '租户'} / ${item.orgName ?? item.orgCode ?? '机构'}`,
  value: `${item.tenantId ?? ''}:${item.orgId ?? ''}`,
  tenantId: item.tenantId ?? '',
  orgId: item.orgId ?? '',
})))

const title = computed(() => String(route.meta.title ?? 'Peach Cloud'))
const activePath = computed(() => route.path)

async function handleMenuClick(path: string) {
  await router.push(path)
}

function handleMenuEvent(event: MenuInfo) {
  void handleMenuClick(String(event.key))
}

async function handleSwitchContext(value: string) {
  const option = userOrgOptions.value.find(item => item.value === value)
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
</script>

<template>
  <a-layout class="admin-layout">
    <a-layout-sider class="admin-sider" :width="248" breakpoint="lg" collapsed-width="0">
      <div class="admin-brand">
        <strong>Peach Cloud</strong>
        <span>DataOS Console</span>
      </div>
      <a-menu
        class="admin-menu"
        mode="inline"
        theme="dark"
        :selected-keys="[activePath]"
        @click="handleMenuEvent"
      >
        <a-menu-item v-for="item in menuItems" :key="item.path">
          {{ item.meta.title }}
        </a-menu-item>
      </a-menu>
    </a-layout-sider>

    <a-layout>
      <a-layout-header class="admin-header">
        <div class="admin-header__title">
          <span class="admin-kicker">当前页面</span>
          <h1>{{ title }}</h1>
        </div>
        <div class="admin-header__actions">
          <WebSocketNotifier />
          <a-select
            v-model:value="selectedOrgKey"
            class="context-select"
            :loading="isSwitching"
            :options="userOrgOptions"
            placeholder="选择机构上下文"
            @change="handleSwitchContext"
          />
          <a-button @click="router.push('/profile')">个人中心</a-button>
          <a-button danger @click="logout">退出</a-button>
        </div>
      </a-layout-header>

      <a-layout-content class="admin-content">
        <slot />
      </a-layout-content>
    </a-layout>
  </a-layout>
</template>
