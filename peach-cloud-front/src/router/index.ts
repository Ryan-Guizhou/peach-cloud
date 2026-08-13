import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'

import { buildAdminRoutes } from './dynamic'
import pinia from '../stores'
import { useAuthStore } from '../stores/auth'

const dynamicRouteNames = new Set<string>()

const staticRoutes: RouteRecordRaw[] = [
  { path: '/', redirect: '/login' },
  {
    path: '/login',
    name: 'login',
    component: () => import('../views/auth/login/index.vue'),
  },
  {
    path: '/register',
    name: 'register',
    component: () => import('../views/auth/register/index.vue'),
  },
  {
    path: '/forgot-password',
    name: 'forgot-password',
    component: () => import('../views/auth/forgot-password/index.vue'),
  },
  {
    path: '/:pathMatch(.*)*',
    name: 'not-found',
    component: () => import('../views/error/not-found/index.vue'),
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes: staticRoutes,
})

export function syncDynamicRoutes(): void {
  const authStore = useAuthStore(pinia)
  clearDynamicRoutes()
  buildAdminRoutes(authStore.loginInfo).forEach((route) => {
    const name = String(route.name)
    if (!router.hasRoute(name)) {
      router.addRoute(route)
      dynamicRouteNames.add(name)
    }
  })
}

export function clearDynamicRoutes(): void {
  dynamicRouteNames.forEach((name) => {
    if (router.hasRoute(name)) {
      router.removeRoute(name)
    }
  })
  dynamicRouteNames.clear()
}

router.beforeEach((to) => {
  const authStore = useAuthStore(pinia)
  const hasValidSession = authStore.ensureValidSession()
  if (hasValidSession && authStore.loginInfo && dynamicRouteNames.size === 0) {
    syncDynamicRoutes()
    if (to.name === 'not-found') {
      return to.fullPath
    }
  }
  if (to.meta.requiresAuth && !hasValidSession) {
    return { name: 'login', query: { redirect: to.fullPath } }
  }
  if (to.name === 'login' && hasValidSession) {
    return { name: 'workspace-overview' }
  }
  return true
})

export default router
