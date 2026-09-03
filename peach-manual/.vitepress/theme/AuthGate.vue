<script setup lang="ts">
import { onMounted, ref } from 'vue'

import { verifyManualAccess } from './auth-gate'

const ready = ref(false)
const blocked = ref(false)
const statusMessage = ref('正在验证登录状态…')

onMounted(async () => {
  const result = await verifyManualAccess()
  if (result.ok) {
    ready.value = true
    return
  }
  if (result.redirectUrl) {
    window.location.replace(result.redirectUrl)
    return
  }
  blocked.value = true
  statusMessage.value = result.message ?? '当前无法访问用户手册'
})
</script>

<template>
  <div v-if="!ready" class="manual-auth-gate">
    <div class="manual-auth-gate__panel">
      <div class="manual-auth-gate__brand">
        <span class="manual-auth-gate__mark">P</span>
        <div>
          <strong>Peach Cloud</strong>
          <small>CONTROL PLANE · MANUAL</small>
        </div>
      </div>
      <p>{{ statusMessage }}</p>
      <a v-if="blocked" class="manual-auth-gate__link" href="/login">前往登录</a>
    </div>
  </div>
  <slot v-else />
</template>
