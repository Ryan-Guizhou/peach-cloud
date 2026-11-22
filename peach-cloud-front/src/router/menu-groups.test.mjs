import { test } from 'node:test'
import assert from 'node:assert/strict'
import { groupMenuItems } from './menu-groups.ts'
const item = (menuCode, path) => ({ path, name: menuCode, componentPath: '', meta: { menuCode, title: menuCode } })
test('uses backend directories and keeps authorised tenant and log pages separate', () => {
  const result = groupMenuItems([item('tenant', '/tenant'), item('loginLog', '/login-log')], [
    { menuId: 'settings', menuName: '系统设置', isLeaf: 0 },
    { menuId: 'audit', menuName: '日志审计', isLeaf: 0 },
    { menuId: 'tenant', menuCode: 'tenant', menuUrl: '/tenant', parentMenuId: 'settings' },
    { menuId: 'logs', menuCode: 'loginLog', menuUrl: '/login-log', parentMenuId: 'audit' },
  ])
  assert.deepEqual(result.map(group => group.title), ['系统设置', '日志审计'])
  assert.equal(result[1].items[0].path, '/login-log')
})
test('preserves shortcuts, has a bounded fallback for broken/cyclic ancestry and no empty groups', () => {
  const result = groupMenuItems([item('profile', '/profile'), item('schedulerJob', '/scheduler/jobs')], [])
  assert.deepEqual(result.map(group => group.title), ['快捷入口', '任务调度'])
})
