import type { RouteRecordRaw } from 'vue-router'

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

const viewModules = import.meta.glob('../views/**/index.vue')

export const fallbackAdminRoutes: AdminRouteConfig[] = [
  { path: '/workspace', name: 'workspace-overview', componentPath: 'workspace/overview', meta: { title: '工作台', menuCode: 'workspace' } },
  { path: '/user', name: 'user-manage', componentPath: 'system/user', meta: { title: '用户管理', menuCode: 'user', permissionCode: 'user:view' } },
  { path: '/role', name: 'role-manage', componentPath: 'system/role', meta: { title: '角色管理', menuCode: 'role', permissionCode: 'role:view' } },
  { path: '/menu', name: 'menu-manage', componentPath: 'system/menu', meta: { title: '菜单管理', menuCode: 'menu', permissionCode: 'menu:view' } },
  { path: '/router', name: 'router-manage', componentPath: 'system/router', meta: { title: '路由管理', menuCode: 'router', permissionCode: 'router:view' } },
  { path: '/organization', name: 'organization-manage', componentPath: 'system/organization', meta: { title: '机构管理', menuCode: 'organization', permissionCode: 'organization:view' } },
  { path: '/function', name: 'function-manage', componentPath: 'system/function', meta: { title: '功能定义字段', menuCode: 'function', permissionCode: 'function:view' } },
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
  return trimmed.startsWith('/') ? trimmed : `/${trimmed}`
}

function normalizeComponentPath(filePath: string | undefined, routePath: string): string {
  const candidate = filePath && filePath.trim() ? filePath.trim() : routePath
  return candidate
    .replace(/^\/+/, '')
    .replace(/\/index\.vue$/, '')
    .replace(/^views\//, '')
}

function findView(componentPath: string) {
  return viewModules[`../views/${componentPath}/index.vue`]
}

function fallbackByPath(path: string): AdminRouteConfig | undefined {
  return fallbackAdminRoutes.find(route => route.path === path)
}

function normalizeCode(code: string | undefined): string {
  return (code ?? '').trim().toLowerCase().replace(/[^a-z0-9]/g, '')
}

function fallbackByAnyCode(...codes: Array<string | undefined>): AdminRouteConfig | undefined {
  const normalizedCodes = codes.map(normalizeCode).filter(code => code.length > 0)
  if (normalizedCodes.length === 0) {
    return undefined
  }
  return fallbackAdminRoutes.find((route) => {
    const routeCode = normalizeCode(route.meta.menuCode)
    const routeName = normalizeCode(route.name)
    return normalizedCodes.some(code => routeCode === code || routeName === code || routeCode.includes(code) || code.includes(routeCode))
  })
}

function buildRouterMaps(routerList: RouterInfo[]) {
  const pathMap = new Map<string, RouterInfo>()
  const codeMap = new Map<string, RouterInfo>()
  routerList.forEach((routerInfo) => {
    const path = normalizePath(routerInfo.routerUrl)
    if (path) {
      pathMap.set(path, routerInfo)
    }
    const routerCode = normalizeCode(routerInfo.routerCode)
    if (routerCode) {
      codeMap.set(routerCode, routerInfo)
    }
  })
  return { pathMap, codeMap }
}

export function buildAdminRoutes(loginInfo: LoginInfo | null): RouteRecordRaw[] {
  const routerList = loginInfo?.routerList ?? []
  const dynamicRoutes: AdminRouteConfig[] = routerList
    .map((routerInfo: RouterInfo): AdminRouteConfig | null => {
      const path = normalizePath(routerInfo.routerUrl)
      if (!path) {
        return null
      }
      const fallback = fallbackByPath(path)
      const componentPath = normalizeComponentPath(routerInfo.filePath, path)
      const matchedView = findView(componentPath) ? componentPath : fallback?.componentPath
      if (!matchedView) {
        return null
      }
      return {
        path,
        name: routerInfo.routerCode || fallback?.name || path.replace(/\//g, '-').replace(/^-/, ''),
        componentPath: matchedView,
        meta: {
          title: routerInfo.routerName || fallback?.meta.title || path,
          menuCode: routerInfo.routerCode || fallback?.meta.menuCode || path,
          permissionCode: fallback?.meta.permissionCode,
        },
      }
    })
    .filter((route): route is AdminRouteConfig => route !== null)

  const menuFallbackRoutes = (loginInfo?.menuList ?? [])
    .map((menuInfo): AdminRouteConfig | null => {
      const menuPath = normalizePath(menuInfo.menuUrl)
      const fallback = fallbackByPath(menuPath) ?? fallbackByAnyCode(menuInfo.funcCode, menuInfo.menuCode)
      if (!fallback) {
        return null
      }
      return fallback
    })
    .filter((route): route is AdminRouteConfig => route !== null)

  const routeByPath = new Map<string, AdminRouteConfig>()
  const candidateRoutes = dynamicRoutes.length > 0
    ? [...dynamicRoutes, ...menuFallbackRoutes]
    : fallbackAdminRoutes
  candidateRoutes.forEach((route) => {
    if (!routeByPath.has(route.path)) {
      routeByPath.set(route.path, route)
    }
  })

  const effectiveRoutes = Array.from(routeByPath.values())

  return effectiveRoutes.map((route): RouteRecordRaw => ({
    path: route.path,
    name: route.name,
    component: findView(route.componentPath),
    meta: {
      requiresAuth: true,
      title: route.meta.title,
      menuCode: route.meta.menuCode,
      permissionCode: route.meta.permissionCode,
    },
  }))
}

export function buildMenuItems(loginInfo: LoginInfo | null): AdminRouteConfig[] {
  const menuList = loginInfo?.menuList ?? []
  if (menuList.length === 0) {
    return fallbackAdminRoutes
  }
  const routerMaps = buildRouterMaps(loginInfo?.routerList ?? [])
  const routeMap = new Map(fallbackAdminRoutes.map(route => [route.meta.menuCode, route]))
  const items = menuList
    .map((menuInfo: MenuInfo): AdminRouteConfig | null => {
      const key = menuInfo.menuCode ?? ''
      const menuPath = normalizePath(menuInfo.menuUrl)
      const matchedRouter = routerMaps.pathMap.get(menuPath)
        ?? routerMaps.codeMap.get(normalizeCode(menuInfo.funcCode))
        ?? routerMaps.codeMap.get(normalizeCode(menuInfo.menuCode))
      const matchedRouterPath = normalizePath(matchedRouter?.routerUrl)
      const fallback = routeMap.get(key)
        ?? fallbackByPath(menuPath)
        ?? fallbackByPath(matchedRouterPath)
        ?? fallbackByAnyCode(menuInfo.funcCode, menuInfo.menuCode, matchedRouter?.routerCode)
      const path = matchedRouterPath || menuPath || fallback?.path
      if (!path) {
        return null
      }
      return {
        path,
        name: matchedRouter?.routerCode || fallback?.name || key || path,
        componentPath: fallback?.componentPath || normalizeComponentPath(matchedRouter?.filePath, path),
        meta: {
          title: menuInfo.menuName || matchedRouter?.routerName || fallback?.meta.title || path,
          menuCode: key || fallback?.meta.menuCode || path,
          permissionCode: fallback?.meta.permissionCode,
        },
      }
    })
    .filter((item): item is AdminRouteConfig => item !== null)
  return items.length > 0 ? items : fallbackAdminRoutes
}
