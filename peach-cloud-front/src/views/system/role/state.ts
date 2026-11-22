import type { ResourceGrant } from '../../../api/role-workspace'
export function resourceKey(funcCode: string, type: string, code: string): string {
  return [funcCode, type, code].map(value => `${value.length}:${value}`).join('')
}
export function grantKey(grant: ResourceGrant): string { return resourceKey(grant.funcCode, grant.opType ?? '', grant.resourceCode) }
/** Keep all hidden selections; a visible filter is never a complete grant snapshot. */
export function mergeVisibleChecks(current: string[], visible: string[], checked: string[]): string[] {
  const visibleSet = new Set(visible)
  return [...new Set([...current.filter(key => !visibleSet.has(key)), ...checked])]
}
