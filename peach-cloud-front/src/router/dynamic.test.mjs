import assert from 'node:assert/strict'
import { after, before, test } from 'node:test'
import { createServer } from 'vite'
import { createMemoryHistory, createRouter } from 'vue-router'
import { readdirSync } from 'node:fs'

let server
let buildAdminRoutes
let buildMenuItems
let fallbackAdminRoutes

before(async () => {
  server = await createServer({ server: { middlewareMode: true }, appType: 'custom' })
  ;({ buildAdminRoutes, buildMenuItems, fallbackAdminRoutes } = await server.ssrLoadModule('/src/router/dynamic.ts'))
})

test('all implemented business pages have a canonical route and can appear in an authorized administrator menu', () => {
  const pages = readdirSync(new URL('../views/', import.meta.url), { recursive: true })
    .map(path => path.replaceAll('\\', '/'))
    .filter(path => path.endsWith('/index.vue') && !path.startsWith('auth/') && !path.startsWith('error/'))
    .map(path => path.replace('/index.vue', '')).sort()
  assert.deepEqual(fallbackAdminRoutes.map(route => route.componentPath).sort(), pages)
  const info = {
    routerList: fallbackAdminRoutes.map(route => ({ routerUrl: route.path, filePath: route.componentPath })),
    menuList: fallbackAdminRoutes.map((route, i) => ({ menuUrl: route.path, menuCode: `M${i}`, isLeaf: 1 })),
    permissionList: fallbackAdminRoutes.flatMap(route => route.meta.permissionCode ? [route.meta.permissionCode] : []),
  }
  assert.equal(buildMenuItems(info).length, pages.length)
  assert.equal(buildAdminRoutes(info).length, pages.length)
})

test('known page URL cannot resolve to the workspace component through stale backend data', () => {
  const expected = buildAdminRoutes({ routerList: [{ routerUrl: '/login-log', filePath: 'log/login' }] })
    .find(route => route.path === '/login-log')
  const actual = buildAdminRoutes({ routerList: [{ routerUrl: '/login-log', filePath: 'workspace/overview' }] })
    .find(route => route.path === '/login-log')
  assert.equal(actual.component, expected.component)
})

test('empty authorization never expands to the full business navigation', () => {
  const info = { routerList: [], menuList: [], permissionList: [] }
  assert.deepEqual(buildAdminRoutes(info).map(route => route.path).sort(), ['/profile', '/workspace'])
  assert.deepEqual(buildMenuItems(info).map(item => item.path).sort(), ['/profile', '/workspace'])
})

test('migrated menu/function/router codes resolve by real associations while permission identifiers stay unchanged', () => {
  const info = {
    routerList: [{ routerUrl: '/login-log', routerCode: 'RT0001', funcCode: 'F0001', filePath: 'log/login' }],
    menuList: [{ menuCode: 'M0001', menuName: '登录日志', funcCode: 'F0001', isLeaf: 1 }],
    permissionList: ['loginLog:view', 'POST:/api/auth/loginLog/pageList'],
  }
  const item = buildMenuItems(info).find(menu => menu.meta.menuCode === 'M0001')
  assert.ok(item)
  assert.equal(item.path, '/login-log')
  assert.equal(item.meta.permissionCode, 'loginLog:view')
  assert.equal(buildAdminRoutes(info).find(route => route.path === '/login-log').name, 'login-log')
})

test('menu URL wins over a stale function binding and only registered permitted pages appear', () => {
  const info = {
    routerList: [
      { routerUrl: '/workspace', filePath: 'workspace/overview', funcCode: 'OLD_FUNC' },
      { routerUrl: '/login-log', filePath: 'log/login', funcCode: 'SYS_LOGIN_LOG' },
      { routerUrl: '/tenant', filePath: 'system/tenant' },
    ],
    menuList: [
      { menuName: '登录日志', menuCode: 'L00001', menuUrl: '/login-log', funcCode: 'OLD_FUNC', isLeaf: 1 },
      { menuName: '租户', menuCode: 'T00001', menuUrl: '/tenant', isLeaf: 1 },
      { menuName: '未授权页面', menuCode: 'UNKNOWN', menuUrl: '/user', isLeaf: 1 },
    ],
    permissionList: ['loginLog:view'],
  }
  const menus = buildMenuItems(info)
  assert.equal(menus.find(menu => menu.meta.title === '登录日志').path, '/login-log')
  assert.equal(menus.some(menu => menu.path === '/tenant' || menu.path === '/user'), false)
})

test('business route names remain unique when backend router codes collide', () => {
  const routes = buildAdminRoutes({ routerList: [
    { routerUrl: '/custom-one', routerCode: 'SAME', filePath: 'log/login' },
    { routerUrl: '/custom-two', routerCode: 'SAME', filePath: 'log/auth' },
  ] })
  const router = createRouter({ history: createMemoryHistory(), routes })
  assert.equal(router.resolve('/custom-one').matched.length, 1)
  assert.equal(router.resolve('/custom-two').matched.length, 1)
})

test('external paths and authentication views cannot be registered as business pages', () => {
  const routes = buildAdminRoutes({ routerList: [
    { routerUrl: '//outside.example', filePath: 'log/login' },
    { routerUrl: '/login', filePath: 'auth/login' },
    { routerUrl: '/invalid', filePath: '../auth/login' },
  ] })
  assert.deepEqual(routes.map(route => route.path).sort(), ['/profile', '/workspace'])
})

after(async () => {
  await server?.close()
})

test('partial backend routes retain the login landing page without adding unrelated business pages', () => {
  const routes = buildAdminRoutes({ routerList: [
    { routerUrl: '/user', routerCode: 'user', filePath: 'system/user' },
  ] })
  const workspace = routes.find(route => route.path === '/workspace')
  assert.ok(workspace, 'login fallback /workspace must be registered')
  assert.equal(workspace.name, 'workspace-overview')
  assert.equal(workspace.meta.requiresAuth, true)
  assert.equal(typeof workspace.component, 'function')
  assert.equal(routes.some(route => route.path === '/tenant'), false)

  const router = createRouter({ history: createMemoryHistory(), routes })
  assert.equal(router.resolve('/workspace').name, 'workspace-overview')
})

test('backend workspace remains unique and keeps its title', () => {
  const routes = buildAdminRoutes({ routerList: [
    { routerUrl: '/workspace', routerName: '我的工作台', filePath: 'workspace/overview' },
  ] })
  assert.equal(routes.filter(route => route.path === '/workspace').length, 1)
  assert.equal(routes.find(route => route.path === '/workspace').meta.title, '我的工作台')
})
