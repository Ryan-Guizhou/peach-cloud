import http, { unwrapApiResponse } from './http'
import type { ApiResponse } from '../types/auth'

/**
 * SaaS 受保护管理 API：身份、租户、应用与自然年度统一由 Auth 的 Token 决定。
 * 这里的参数仅承载目标物理主键和操作内容，不接受浏览器自报的授权身份。
 */
const PHYSICAL_ID = /^[a-fA-F0-9]{32}$/
const ROLE_CODE = /^ROLE_[A-Z0-9_]{1,15}$/
const RESOURCE_CODE = /^[A-Z][A-Z0-9_.-]{0,63}$/

export type ScopeType = 'TENANT' | 'ORG' | 'ORG_TREE'
export type DataScopeType = 'SELF' | 'ORG' | 'ORG_TREE' | 'TENANT_ALL'
export type ReadEffect = 'ALLOW' | 'MASK' | 'DENY'
export type WriteEffect = 'ALLOW' | 'DENY'

export interface CreateTenantRoleRequest {
  roleCode: string
  roleName: string
}

export interface GrantMemberRoleRequest {
  targetMemberRefId: string
  roleRefId: string
  scopeType: ScopeType
  scopeRefId?: string
}

export interface DataScopeRule {
  dataScope: DataScopeType
  orgRefId?: string
}

export interface FieldPolicyRule {
  fieldRefId: string
  readEffect: ReadEffect
  writeEffect: WriteEffect
}

function physicalId(value: string, field: string): string {
  if (!PHYSICAL_ID.test(value)) {
    throw new Error(`${field}必须为有效的32位物理主键`)
  }
  return value
}

function validateUniqueIds(ids: string[], label: string, maxCount: number): string[] {
  if (ids.length > maxCount) {
    throw new Error(`${label}不能超过${maxCount}条`)
  }
  const checked = ids.map(id => physicalId(id, label))
  if (new Set(checked).size !== checked.length) {
    throw new Error(`${label}不能重复`)
  }
  return checked
}

function requireResourceCode(code: string): string {
  if (!RESOURCE_CODE.test(code)) {
    throw new Error('资源类型编码不符合安全规则')
  }
  return code
}

/** 角色业务码可由租户管理员定义，物理主键仍由 Auth 生成并返回。 */
export async function createTenantRole(request: CreateTenantRoleRequest): Promise<string> {
  const roleCode = request.roleCode.trim().toUpperCase()
  const roleName = request.roleName.trim()
  if (!ROLE_CODE.test(roleCode) || !roleName || roleName.length > 64) {
    throw new Error('角色代码或名称不符合规则')
  }
  const { data } = await http.post<ApiResponse<string>>('/auth/saas/roles', { roleCode, roleName })
  return physicalId(unwrapApiResponse(data, '创建角色失败', { requireData: true }), '创建的角色ID')
}

/** 全量替换角色权限。空数组是明确撤销全部权限，不允许隐式 null。 */
export async function replaceTenantRolePermissions(
  roleRefId: string,
  permissionRefIds: string[],
): Promise<void> {
  const role = physicalId(roleRefId, '角色ID')
  const ids = validateUniqueIds(permissionRefIds, '权限ID', 200)
  const { data } = await http.put<ApiResponse<unknown>>(
    `/auth/saas/roles/${role}/permissions`,
    { permissionRefIds: ids },
  )
  unwrapApiResponse(data, '更新角色权限失败')
}

/** 每次仅建立一个成员、应用、年度下的独立范围绑定。 */
export async function grantMemberRole(request: GrantMemberRoleRequest): Promise<string> {
  const targetMemberRefId = physicalId(request.targetMemberRefId, '成员ID')
  const roleRefId = physicalId(request.roleRefId, '角色ID')
  if (!(['TENANT', 'ORG', 'ORG_TREE'] as const).includes(request.scopeType)) {
    throw new Error('角色授权范围无效')
  }
  const scopeRefId = request.scopeType === 'TENANT'
    ? undefined
    : physicalId(request.scopeRefId ?? '', '授权机构ID')
  if (request.scopeType === 'TENANT' && request.scopeRefId) {
    throw new Error('租户级授权不得传入机构ID')
  }
  const { data } = await http.post<ApiResponse<string>>('/auth/saas/roles/bindings', {
    targetMemberRefId,
    roleRefId,
    scopeType: request.scopeType,
    scopeRefId,
  })
  return physicalId(unwrapApiResponse(data, '成员角色授权失败', { requireData: true }), '授权绑定ID')
}

/** 撤销指定物理绑定，不用角色 CODE 或机构业务 ID 猜测目标。 */
export async function revokeMemberRole(bindingRefId: string): Promise<void> {
  const binding = physicalId(bindingRefId, '授权绑定ID')
  const { data } = await http.delete<ApiResponse<unknown>>(
    `/auth/saas/roles/bindings/${binding}`,
  )
  unwrapApiResponse(data, '撤销成员角色失败')
}

/** 行级策略的组织物理 ID 必须与所选择的数据范围一致。 */
export async function replaceDataPolicies(
  roleRefId: string,
  permissionRefId: string,
  policies: DataScopeRule[],
): Promise<void> {
  const role = physicalId(roleRefId, '角色ID')
  const permission = physicalId(permissionRefId, '权限ID')
  if (policies.length > 32) {
    throw new Error('行级策略最多32项')
  }
  const checked = policies.map(rule => {
    if (!(['SELF', 'ORG', 'ORG_TREE', 'TENANT_ALL'] as const).includes(rule.dataScope)) {
      throw new Error('不支持的数据范围')
    }
    const needsOrg = rule.dataScope === 'ORG' || rule.dataScope === 'ORG_TREE'
    if (!needsOrg && rule.orgRefId) {
      throw new Error('当前数据范围不得指定机构ID')
    }
    return {
      dataScope: rule.dataScope,
      orgRefId: needsOrg ? physicalId(rule.orgRefId ?? '', '机构ID') : undefined,
    }
  })
  const { data } = await http.put<ApiResponse<unknown>>(
    `/auth/saas/roles/${role}/permissions/${permission}/data-policies`,
    { policies: checked },
  )
  unwrapApiResponse(data, '更新行级权限失败')
}

/** 字段目录由后端强制校验，前端不允许自定义脱敏算法或绕过字段目录。 */
export async function replaceFieldPolicies(
  roleRefId: string,
  permissionRefId: string,
  resourceCode: string,
  policies: FieldPolicyRule[],
): Promise<void> {
  const role = physicalId(roleRefId, '角色ID')
  const permission = physicalId(permissionRefId, '权限ID')
  const resource = requireResourceCode(resourceCode)
  if (policies.length > 128) {
    throw new Error('字段策略最多128项')
  }
  const checked = policies.map(rule => {
    const fieldRefId = physicalId(rule.fieldRefId, '字段ID')
    if (!(['ALLOW', 'MASK', 'DENY'] as const).includes(rule.readEffect)
        || !(['ALLOW', 'DENY'] as const).includes(rule.writeEffect)) {
      throw new Error('无效的字段读写效果')
    }
    return { fieldRefId, readEffect: rule.readEffect, writeEffect: rule.writeEffect }
  })
  if (new Set(checked.map(rule => rule.fieldRefId)).size !== checked.length) {
    throw new Error('字段策略不得包含重复字段')
  }
  const { data } = await http.put<ApiResponse<unknown>>(
    `/auth/saas/roles/${role}/permissions/${permission}/field-policies`,
    { resourceCode: resource, policies: checked },
  )
  unwrapApiResponse(data, '更新字段权限失败')
}


/** 受信服务按当前 Token 限定租户、应用与年度，分页参数不包含任何浏览器自报身份。 */
export type CatalogKind = 'roles' | 'permissions' | 'members' | 'organizations'

export interface SaasCatalogEntry {
  id: string
  code?: string
  name?: string
  resourceType?: string
  roleRefId?: string
  scopeType?: ScopeType
  scopeRefId?: string
}

export interface SaasCatalogPage {
  page: number
  pageSize: number
  hasMore: boolean
  items: SaasCatalogEntry[]
}

export async function fetchSaasCatalog(
  kind: CatalogKind,
  page = 1,
  pageSize = 20,
  keyword = '',
): Promise<SaasCatalogPage> {
  if (!Number.isInteger(page) || page < 1 || page > 1000
    || !Number.isInteger(pageSize) || pageSize < 1 || pageSize > 50
    || keyword.length > 64) {
    throw new Error('目录分页或检索条件无效')
  }
  const { data } = await http.get<ApiResponse<SaasCatalogPage>>(
    `/auth/saas/roles/catalog/${kind}`,
    { params: { page, pageSize, keyword: keyword.trim() } },
  )
  return unwrapApiResponse(data, '读取受信目录失败', { requireData: true })
}

export async function fetchRolePermissionRefs(roleRefId: string): Promise<string[]> {
  const role = physicalId(roleRefId, '角色ID')
  const { data } = await http.get<ApiResponse<string[]>>(
    `/auth/saas/roles/catalog/roles/${role}/permissions`,
  )
  return validateUniqueIds(
    unwrapApiResponse(data, '读取当前角色权限失败', { requireData: true }), '角色权限ID', 200)
}

export async function fetchMemberBindings(memberRefId: string): Promise<SaasCatalogEntry[]> {
  const member = physicalId(memberRefId, '成员ID')
  const { data } = await http.get<ApiResponse<SaasCatalogEntry[]>>(
    `/auth/saas/roles/catalog/members/${member}/bindings`,
  )
  const bindings = unwrapApiResponse(data, '读取成员授权绑定失败', { requireData: true })
  if (bindings.length > 200 || bindings.some(item => !PHYSICAL_ID.test(item.id))) {
    throw new Error('成员授权绑定目录不完整')
  }
  return bindings
}
