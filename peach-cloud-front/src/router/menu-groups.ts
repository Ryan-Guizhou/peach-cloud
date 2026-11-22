import type { AdminMenuGroup, AdminRouteConfig } from './dynamic'
import type { MenuInfo } from '../types/auth'

const shortcuts = new Set(['workspace', 'profile', 'messageCenter'])
const fallbacks: Record<string, string> = {
  authLog: '日志审计', operationLog: '日志审计', loginLog: '日志审计',
  objectBrowser: '文件与存储', fileRecord: '文件与存储', storageInstance: '文件与存储',
  schedulerJob: '任务调度', schedulerExecution: '任务调度', schedulerHandler: '任务调度',
}

/** Group authorised leaves by their backend ancestor directories; never render directories as pages. */
export function groupMenuItems(items: AdminRouteConfig[], menus: MenuInfo[]): AdminMenuGroup[] {
  const byId = new Map(menus.filter(menu => menu.menuId).map(menu => [menu.menuId, menu]))
  const groups = new Map<string, AdminRouteConfig[]>()
  for (const item of items) {
    let title = shortcuts.has(item.meta.menuCode) ? '快捷入口' : ''
    const source = menus.find(menu => menu.menuCode === item.meta.menuCode || menu.menuUrl === item.path)
    let parent = source?.parentMenuId
    const seen = new Set<string>()
    while (!title && parent && !seen.has(parent)) {
      seen.add(parent)
      const catalog = byId.get(parent)
      if (!catalog) break
      if (catalog.menuName && catalog.menuName !== '应用功能列表') title = catalog.menuName
      parent = catalog.parentMenuId
    }
    title ||= fallbacks[item.meta.menuCode] ?? '系统设置'
    groups.set(title, [...(groups.get(title) ?? []), item])
  }
  const order = ['快捷入口', '系统设置', '日志审计', '文件与存储', '任务调度']
  return [...groups.entries()].sort(([a], [b]) => {
    const ai = order.indexOf(a), bi = order.indexOf(b)
    return (ai < 0 ? order.length : ai) - (bi < 0 ? order.length : bi)
  }).map(([title, entries]) => ({ title, items: entries }))
}
