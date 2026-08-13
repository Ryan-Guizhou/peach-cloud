<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'

import { submitLogout } from '../../../api/auth'
import { fetchCurrentProfile } from '../../../api/profile'
import PeachCloudLogo from '../../../components/auth/PeachCloudLogo.vue'
import { buildMenuItems } from '../../../router/dynamic'
import { clearDynamicRoutes } from '../../../router'
import { useAuthStore } from '../../../stores/auth'
import type { UserProfile } from '../../../types/profile'

const router = useRouter()
const authStore = useAuthStore()
const isLoggingOut = ref(false)
const profile = ref<UserProfile | null>(null)
const isProfileMenuOpen = ref(false)

const session = computed(() => authStore.session)
const loginInfo = computed(() => authStore.loginInfo)
const roleCount = computed(() => loginInfo.value?.roleList.length ?? 0)
const menuCount = computed(() => loginInfo.value?.menuList.length ?? 0)
const routerCount = computed(() => loginInfo.value?.routerList.length ?? 0)
const orgCount = computed(() => loginInfo.value?.userOrgList.length ?? 1)
const workspaceMenuItems = computed(() => buildMenuItems(loginInfo.value).filter(item => item.path !== '/workspace' && item.path !== '/profile'))
const avatarUrl = computed(() => profile.value?.currentAvatar?.avatarUrl)
const userInitials = computed(() => {
  const name = profile.value?.userName || session.value?.userName || 'U'
  return name.slice(0, 2).toUpperCase()
})

const loadProfilePreview = async () => {
  try {
    profile.value = await fetchCurrentProfile()
  } catch {
    profile.value = null
  }
}

const handleLogout = async () => {
  isLoggingOut.value = true
  try {
    await submitLogout()
  } finally {
    authStore.clearSession()
    clearDynamicRoutes()
    isLoggingOut.value = false
    await router.replace({ name: 'login' })
  }
}

const closeProfileMenu = () => {
  isProfileMenuOpen.value = false
}

onMounted(loadProfilePreview)
</script>

<template>
  <div class="workspace-shell">
    <aside class="workspace-sidebar">
      <PeachCloudLogo />
      <nav class="workspace-nav" aria-label="主导航">
        <a class="workspace-nav__item is-active" href="#overview">
          <span class="workspace-nav__icon" aria-hidden="true"><i /><i /><i /></span>
          数据总览
        </a>
        <RouterLink class="workspace-nav__item" to="/profile">
          <span class="workspace-nav__marker" aria-hidden="true" />
          个人中心
        </RouterLink>
        <RouterLink
          v-for="menu in workspaceMenuItems"
          :key="menu.name"
          class="workspace-nav__item"
          :to="menu.path"
        >
          <span class="workspace-nav__marker" aria-hidden="true" />
          {{ menu.meta.title }}
        </RouterLink>
      </nav>
      <div class="workspace-sidebar__context">
        <span>当前数据域</span>
        <strong>{{ session?.tenantName || '默认租户' }}</strong>
        <small>{{ session?.orgName || '默认机构' }}</small>
      </div>
    </aside>

    <main class="workspace-main" id="overview">
      <header class="workspace-header">
        <div>
          <span class="workspace-kicker">DATA CONTROL CENTER · {{ session?.fiscal }}</span>
          <h1>数据运行总览</h1>
          <p>你好，{{ session?.userName }}。你的租户、机构与权限上下文已完成装配。</p>
        </div>
        <div class="workspace-header__actions">
          <span class="live-indicator"><i /> 实时连接</span>
          <div
            class="profile-menu"
            :class="{ 'is-open': isProfileMenuOpen }"
            @mouseenter="isProfileMenuOpen = true"
            @mouseleave="closeProfileMenu"
            @focusin="isProfileMenuOpen = true"
            @focusout="closeProfileMenu"
          >
            <button
              class="profile-menu__trigger"
              type="button"
              :aria-expanded="isProfileMenuOpen"
              aria-haspopup="menu"
              aria-label="打开个人菜单"
              @click="isProfileMenuOpen = !isProfileMenuOpen"
            >
              <span class="profile-menu__avatar">
                <img v-if="avatarUrl" :src="avatarUrl" :alt="`${profile?.userName || session?.userName || '用户'}的头像`" />
                <strong v-else>{{ userInitials }}</strong>
              </span>
              <span class="profile-menu__meta">
                <strong>{{ profile?.userName || session?.userName || '当前用户' }}</strong>
                <small>{{ session?.orgName || session?.orgId || '默认机构' }}</small>
              </span>
              <span class="profile-menu__chevron" aria-hidden="true" />
            </button>
            <div class="profile-menu__panel" role="menu">
              <RouterLink class="profile-menu__item" to="/profile" role="menuitem" @click="closeProfileMenu">
                <span class="profile-menu__item-icon profile-menu__item-icon--user" aria-hidden="true" />
                个人中心
              </RouterLink>
              <button class="profile-menu__item" type="button" role="menuitem" :disabled="isLoggingOut" @click="handleLogout">
                <span class="profile-menu__item-icon profile-menu__item-icon--logout" aria-hidden="true" />
                {{ isLoggingOut ? '正在退出' : '退出登录' }}
              </button>
            </div>
          </div>
        </div>
      </header>

      <section class="metric-grid" aria-label="登录上下文指标">
        <article class="metric-card metric-card--accent">
          <span>可用菜单</span><strong>{{ menuCount }}</strong><small>由后端权限动态下发</small>
        </article>
        <article class="metric-card">
          <span>授权角色</span><strong>{{ roleCount }}</strong><small>当前机构权限集合</small>
        </article>
        <article class="metric-card">
          <span>业务路由</span><strong>{{ routerCount }}</strong><small>已通过白名单解析</small>
        </article>
        <article class="metric-card">
          <span>可管理机构</span><strong>{{ orgCount }}</strong><small>已返回全部绑定关系</small>
        </article>
      </section>

      <section class="workspace-grid" id="assets">
        <article class="data-card data-card--chart">
          <header><div><span>数据活跃度</span><h2>实时流量趋势</h2></div><small>过去 12 小时</small></header>
          <div class="trend-chart" aria-label="示意数据趋势图">
            <div class="trend-chart__grid" />
            <svg viewBox="0 0 720 230" role="img" aria-label="数据活跃度上升趋势">
              <defs><linearGradient id="trendArea" x1="0" y1="0" x2="0" y2="1"><stop offset="0" stop-color="currentColor" stop-opacity=".32"/><stop offset="1" stop-color="currentColor" stop-opacity="0"/></linearGradient></defs>
              <path class="trend-chart__area" d="M0 190 C90 205 118 142 176 158 S292 198 352 116 S468 66 526 104 S628 142 720 34 L720 230 L0 230 Z" />
              <path class="trend-chart__line" d="M0 190 C90 205 118 142 176 158 S292 198 352 116 S468 66 526 104 S628 142 720 34" />
            </svg>
          </div>
          <footer><span>00:00</span><span>04:00</span><span>08:00</span><span>12:00</span></footer>
        </article>

        <article class="data-card context-card">
          <header><div><span>身份上下文</span><h2>当前作用域</h2></div><i class="context-card__status" /></header>
          <dl>
            <div><dt>租户</dt><dd>{{ session?.tenantName || session?.tenantId }}</dd></div>
            <div><dt>机构</dt><dd>{{ session?.orgName || session?.orgId }}</dd></div>
            <div><dt>年度</dt><dd>{{ session?.fiscal }}</dd></div>
            <div><dt>会话状态</dt><dd><span class="status-chip">已验证</span></dd></div>
          </dl>
        </article>

        <article class="data-card permission-card">
          <header><div><span>权限图谱</span><h2>当前角色</h2></div></header>
          <div class="permission-list">
            <div v-for="role in loginInfo?.roleList" :key="role.roleCode" class="permission-row">
              <span class="permission-row__node" /><div><strong>{{ role.roleName || role.roleCode }}</strong><small>{{ role.roleScope || '机构作用域' }}</small></div>
            </div>
            <p v-if="!loginInfo?.roleList.length" class="empty-state">当前会话暂未加载角色明细。</p>
          </div>
        </article>
      </section>
    </main>
  </div>
</template>
