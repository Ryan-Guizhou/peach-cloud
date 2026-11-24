import type { RouteRecordRaw } from 'vue-router'
import { groupMenuItems } from './menu-groups'

import type { LoginInfo, MenuInfo, RouterInfo } from '../types/auth'

export interface AdminRouteMeta {
  title: string
  menuCode: string
  permissionCode?: string
}

export interface AdminRouteConfig {
  path: string
  name: string
  componentPath: string
  meta: AdminRouteMeta
}

export interface AdminMenuGroup {
  title: string
  items: AdminRouteConfig[]
}

const viewModules = import.meta.glob('../views/**/index.vue')

// 本地组件目录；是否可访问仍由后端下发的路由和权限决定。
export const schedulerRoutes: AdminRouteConfig[] = [
  { path: '/scheduler/jobs', name: 'scheduler-jobs', componentPath: 'scheduler/job', meta: { title: '任务管理', menuCode: 'schedulerJob' } },
  { path: '/scheduler/executions', name: 'scheduler-executions', componentPath: 'scheduler/execution', meta: { title: '执行记录', menuCode: 'schedulerExecution' } },
  { path: '/scheduler/handlers', name: 'scheduler-handlers', componentPath: 'scheduler/handler', meta: { title: 'Handler 列表', menuCode: 'schedulerHandler' } },
]

export const fallbackAdminRoutes: AdminRouteConfig[] = [
  ...schedulerRoutes,
  { path: '/workspace', name: 'workspace-overview', componentPath: 'workspace/overview', meta: { title: '工作台', menuCode: 'workspace' } },
  { path: '/user', name: 'user-manage', componentPath: 'system/user', meta: { title: '用户管理', menuCode: 'user', permissionCode: 'user:view' } },
  { path: '/tenant', name: 'tenant-manage', componentPath: 'system/tenant', meta: { title: '租户管理', menuCode: 'tenant', permissionCode: 'tenant:view' } },
  { path: '/role', name: 'role-manage', componentPath: 'system/role', meta: { title: '角色管理', menuCode: 'role', permissionCode: 'role:view' } },
  { path: '/menu', name: 'menu-manage', componentPath: 'system/menu', meta: { title: '菜单管理', menuCode: 'menu', permissionCode: 'menu:view' } },
  { path: '/router', name: 'router-manage', componentPath: 'system/router', meta: { title: '路由管理', menuCode: 'router', permissionCode: 'router:view' } },
  { path: '/organization', name: 'organization-manage', componentPath: 'system/organization', meta: { title: '机构管理', menuCode: 'organization', permissionCode: 'organization:view' } },
  { path: '/function', name: 'function-manage', componentPath: 'system/function', meta: { title: '功能定义', menuCode: 'function', permissionCode: 'function:view' } },
  { path: '/resource', name: 'resource-manage', componentPath: 'system/resource', meta: { title: '资源管理', menuCode: 'resource', permissionCode: 'resource:view' } },
  { path: '/notice', name: 'notice-manage', componentPath: 'setting/notice', meta: { title: '公告管理', menuCode: 'notice', permissionCode: 'notice:view' } },
  { path: '/message-center', name: 'message-center', componentPath: 'message/center', meta: { title: '消息中心', menuCode: 'messageCenter', permissionCode: 'messageCenter:view' } },
  { path: '/dict', name: 'dict-manage', componentPath: 'setting/dict', meta: { title: '字典管理', menuCode: 'dict', permissionCode: 'dict:view' } },
  { path: '/value-set', name: 'value-set-manage', componentPath: 'setting/value-set', meta: { title: '值集管理', menuCode: 'valueSet', permissionCode: 'valueSet:view' } },
  { path: '/multi-message', name: 'multi-message-manage', componentPath: 'setting/multi-message', meta: { title: '多语言管理', menuCode: 'multiMessage', permissionCode: 'multiMessage:view' } },
  { path: '/ip-whitelist', name: 'ip-whitelist-manage', componentPath: 'setting/ip-whitelist', meta: { title: 'IP 白名单', menuCode: 'ipWhitelist', permissionCode: 'ipWhitelist:view' } },
  { path: '/file-record', name: 'file-record', componentPath: 'file/record', meta: { title: '文件管理', menuCode: 'fileRecord', permissionCode: 'fileRecord:view' } },
  { path: '/storage-instance', name: 'storage-instance', componentPath: 'file/storage-instance', meta: { title: '存储实例', menuCode: 'storageInstance', permissionCode: 'storageInstance:view' } },
  { path: '/object-browser', name: 'object-browser', componentPath: 'file/object-browser', meta: { title: '对象浏览', menuCode: 'objectBrowser', permissionCode: 'objectBrowser:view' } },
  { path: '/runtime-monitor', name: 'runtime-monitor', componentPath: 'monitor/runtime', meta: { title: '运行监控', menuCode: 'runtimeMonitor', permissionCode: 'runtimeMonitor:view' } },
  { path: '/authorization', name: 'authorization-manage', componentPath: 'system/authorization', meta: { title: '角色授权', menuCode: 'authorization', permissionCode: 'authorization:view' } },
  { path: '/login-log', name: 'login-log', componentPath: 'log/login', meta: { title: '登录日志', menuCode: 'loginLog', permissionCode: 'loginLog:view' } },
  { path: '/auth-log', name: 'auth-log', componentPath: 'log/auth', meta: { title: '授权日志', menuCode: 'authLog', permissionCode: 'authLog:view' } },
  { path: '/operation-log', name: 'operation-log', componentPath: 'log/operation', meta: { title: '操作日志', menuCode: 'operationLog', permissionCode: 'operationLog:view' } },
  { path: '/profile', name: 'user-profile', componentPath: 'user/profile', meta: { title: '个人中心', menuCode: 'profile' } },
]

function normalizePath(path: string | undefined): string {
  if (!path) {
    return ''
  }
  const trimmed = path.trim()
  if (!trimmed) {
    return ''
  }
  const normalized = trimmed.startsWith('/') ? trimmed : `/${trimmed}`
  if (!/^\/[A-Za-z0-9_/-]+$/.test(normalized) || normalized.includes('//')) {
    return ''
  }
  return normalized.replace(/\/$/, '')
}

function normalizeComponentPath(filePath: string | undefined, routePath: string): string {
  const candidate = filePath && filePath.trim() ? filePath.trim() : routePath
  return candidate
    .replace(/^\/+/, '')
    .replace(/\/index\.vue$/, '')
    .replace(/^views\//, '')
}

function findView(componentPath: string) {
  if (!/^[A-Za-z0-9_-]+(?:\/[A-Za-z0-9_-]+)+$/.test(componentPath)
    || componentPath.startsWith('auth/') || componentPath.startsWith('error/')) {
    return undefined
  }
  return viewModules[`../views/${componentPath}/index.vue`]
}

function fallbackByPath(path: string): AdminRouteConfig | undefined {
  return fallbackAdminRoutes.find(route => route.path === path)
}

function normalizeCode(code: string | undefined): string {
  return (code ?? '').trim().toLowerCase().replace(/[^a-z0-9]/g, '')
}

function buildRouterMaps(routerList: RouterInfo[]) {
  const pathMap = new Map<string, RouterInfo>()
  const codeMap = new Map<string, RouterInfo>()
  const funcMap = new Map<string, RouterInfo>()
  routerList.forEach((routerInfo) => {
    const path = normalizePath(routerInfo.routerUrl)
    if (path) {
      pathMap.set(path, routerInfo)
    }
    const routerCode = normalizeCode(routerInfo.routerCode)
    if (routerCode) {
      codeMap.set(routerCode, routerInfo)
    }
    const funcCode = normalizeCode(routerInfo.funcCode)
    if (funcCode) {
      funcMap.set(funcCode, routerInfo)
    }
  })
  return { pathMap, codeMap, funcMap }
}

function buildRouteConfigs(loginInfo: LoginInfo | null): AdminRouteConfig[] {
  const routerList = loginInfo?.routerList ?? []
  const dynamicRoutes: AdminRouteConfig[] = routerList
    .map((routerInfo: RouterInfo): AdminRouteConfig | null => {
      const path = normalizePath(routerInfo.routerUrl)
      if (!path) {
        return null
      }
      const fallback = fallbackByPath(path)
      const componentPath = normalizeComponentPath(routerInfo.filePath, path)
      // 已有页面 URL 使用稳定组件映射，避免历史错误配置将日志等页面映射为工作台。
      const matchedView = fallback?.componentPath ?? (findView(componentPath) ? componentPath : undefined)
      if (!matchedView) {
        return null
      }
      return {
        path,
        name: fallback?.name ?? `admin:${path}`,
        componentPath: matchedView,
        meta: {
          title: routerInfo.routerName || fallback?.meta.title || path,
          menuCode: fallback?.meta.menuCode || routerInfo.routerCode || path,
          permissionCode: fallback?.meta.permissionCode,
        },
      }
    })
    .filter((route): route is AdminRouteConfig => route !== null)

  const routeByPath = new Map<string, AdminRouteConfig>()
  dynamicRoutes.forEach((route) => {
    if (!routeByPath.has(route.path)) {
      routeByPath.set(route.path, route)
    }
  })

  // 登录后的基础入口，不代表业务功能授权。
  for (const path of ['/workspace', '/profile']) {
    const route = fallbackByPath(path)
    if (route && !routeByPath.has(path)) routeByPath.set(path, route)
  }
  return Array.from(routeByPath.values())
}

export function buildAdminRoutes(loginInfo: LoginInfo | null): RouteRecordRaw[] {
  return buildRouteConfigs(loginInfo).flatMap((route): RouteRecordRaw[] => {
    const component = findView(route.componentPath)
    if (!component) return []
    return [{
      path: route.path,
      name: route.name,
      component,
      meta: {
        requiresAuth: true,
        title: route.meta.title,
        menuCode: route.meta.menuCode,
        permissionCode: route.meta.permissionCode,
      },
    }]
  })
}

export function buildMenuItems(loginInfo: LoginInfo | null): AdminRouteConfig[] {
  const menuList = loginInfo?.menuList ?? []
  const registered = new Map(buildRouteConfigs(loginInfo).map(route => [route.path, route]))
  const permissions = new Set(loginInfo?.permissionList ?? [])
  const routerMaps = buildRouterMaps(loginInfo?.routerList ?? [])
  const items = menuList
    .filter(menu => menu.isLeaf !== 0)
    .map((menuInfo: MenuInfo): AdminRouteConfig | null => {
      const key = menuInfo.menuCode ?? ''
      const menuPath = normalizePath(menuInfo.menuUrl)
      // 有明确菜单 URL 时不允许错误的功能编码将其重定向到另一个页面。
      const matchedRouter = menuPath ? routerMaps.pathMap.get(menuPath)
        : routerMaps.funcMap.get(normalizeCode(menuInfo.funcCode))
          ?? routerMaps.codeMap.get(normalizeCode(menuInfo.menuCode))
      const path = menuPath || normalizePath(matchedRouter?.routerUrl)
      const route = registered.get(path)
      if (!route || (route.meta.permissionCode && !permissions.has(route.meta.permissionCode))) {
        return null
      }
      return {
        path,
        name: route.name,
        componentPath: route.componentPath,
        meta: {
          title: menuInfo.menuName || route.meta.title,
          menuCode: key || route.meta.menuCode,
          permissionCode: route.meta.permissionCode,
        },
      }
    })
    .filter((item): item is AdminRouteConfig => item !== null)
  const byPath = new Map(items.map(item => [item.path, item]))
  for (const path of ['/workspace', '/profile']) {
    const route = registered.get(path)
    if (route && !byPath.has(path)) byPath.set(path, route)
  }
  return Array.from(byPath.values())
}

export function buildMenuGroups(loginInfo: LoginInfo | null): AdminMenuGroup[] {
  return groupMenuItems(buildMenuItems(loginInfo), loginInfo?.menuList ?? [])
}
