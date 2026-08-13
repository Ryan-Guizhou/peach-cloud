<script setup lang="ts">
import { onMounted, watch } from 'vue'

import { useAuthStore } from '../../stores/auth'
import { useWebSocketStore } from '../../stores/websocket'

const authStore = useAuthStore()
const webSocketStore = useWebSocketStore()

onMounted(() => {
  webSocketStore.connect(authStore.session)
})

watch(
  () => authStore.session?.token,
  () => {
    webSocketStore.connect(authStore.session)
  },
)
</script>

<template>
  <span class="ws-status" :class="{ 'ws-status--connected': webSocketStore.connected }">
    {{ webSocketStore.connected ? '实时通知已连接' : '实时通知未连接' }}
    <a-badge :count="webSocketStore.unreadCount" />
  </span>
</template>
