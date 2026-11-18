<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'

import { submitLogout } from '../../../api/auth'
import { clearDynamicRoutes } from '../../../router'
import { useAuthStore } from '../../../stores/auth'
import { useWebSocketStore } from '../../../stores/websocket'

const router = useRouter()
const authStore = useAuthStore()
const websocketStore = useWebSocketStore()
const isLoggingOut = ref(false)

async function handleLogout() {
  if (isLoggingOut.value) {
    return
  }
  isLoggingOut.value = true
  try {
    await submitLogout()
  } catch {
    // 注销请求失败也不保留本地凭证，避免用户误认为已经撤销远端会话。
  } finally {
    websocketStore.disconnect()
    authStore.clearSession()
    clearDynamicRoutes()
    isLoggingOut.value = false
    await router.replace('/login')
  }
}
</script>

<template>
  <main class="no-access-shell">
    <section class="no-access-panel" aria-labelledby="no-access-title">
      <span class="no-access-eyebrow">PEACH CLOUD · IAM</span>
      <h1 id="no-access-title">当前会话暂无已授权的业务页面</h1>
      <p>
        账号已经登录，但服务端尚未下发可访问的菜单与路由。
        请联系当前租户管理员配置年度角色和资源权限，或退出后选择其他租户及应用。
      </p>
      <button type="button" :disabled="isLoggingOut" @click="handleLogout">
        {{ isLoggingOut ? '正在退出' : '安全退出登录' }}
      </button>
    </section>
  </main>
</template>

<style scoped>
.no-access-shell {
  min-height: 100vh;
  display: grid;
  place-items: center;
  padding: 2rem;
  background: var(--background-color, #f6f8fc);
}
.no-access-panel {
  width: min(100%, 36rem);
  border: 1px solid #dce4ed;
  border-radius: 1.5rem;
  background: #fff;
  padding: clamp(1.5rem, 5vw, 3.25rem);
  box-shadow: 0 24px 70px rgb(31 49 85 / 7%);
}
.no-access-eyebrow {
  color: #47668d;
  font-size: .75rem;
  font-weight: 700;
  letter-spacing: .14em;
}
h1 {
  margin: 1.2rem 0 1rem;
  color: #182b44;
  font-size: clamp(1.45rem, 3vw, 2rem);
}
p {
  color: #52637a;
  line-height: 1.8;
}
button {
  margin-top: 1rem;
  border: 0;
  border-radius: .7rem;
  padding: .8rem 1.4rem;
  font: inherit;
  font-weight: 600;
  background: #244c80;
  color: #fff;
  cursor: pointer;
}
button:focus-visible {
  outline: 3px solid #9cc8ff;
  outline-offset: 3px;
}
button:disabled {
  opacity: .6;
  cursor: wait;
}
</style>
