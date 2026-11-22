import http, { unwrapApiResponse } from './http'
import type { ApiResponse } from '../types/auth'

export type TenantStatus = 'PENDING' | 'ACTIVE' | 'SUSPENDED' | 'CLOSED'

export interface TenantSave {
  tenantCode: string
  tenantName: string
  tenantType: string
  description?: string | null
  contactName?: string | null
  contactPhone?: string | null
  contactEmail?: string | null
}

export interface Tenant extends TenantSave {
  id: string
  tenantId: string
  status: TenantStatus
  version: number
  activatedTime?: string | null
  closedAt?: string | null
  createTime?: string | null
  modifyTime?: string | null
}

export interface TenantQuery {
  pageNum: number
  pageSize: number
  keyword?: string
  status?: TenantStatus
  tenantType?: string
}

export async function fetchTenants(query: TenantQuery, signal?: AbortSignal): Promise<{ list: Tenant[]; total: number }> {
  const { data } = await http.post<ApiResponse<{ list: Tenant[]; total: number }>>('/auth/tenant/pageList', query, { signal })
  return unwrapApiResponse(data, '加载租户失败', { requireData: true })
}

export async function fetchTenant(id: string): Promise<Tenant> {
  const { data } = await http.get<ApiResponse<Tenant>>('/auth/tenant/selectById', { params: { id } })
  return unwrapApiResponse(data, '加载租户详情失败', { requireData: true })
}

export async function saveTenant(payload: TenantSave, identity?: { id: string; version: number }): Promise<void> {
  const { data } = await http.post<ApiResponse<void>>(`/auth/tenant/${identity ? 'update' : 'add'}`, { ...payload, ...identity })
  unwrapApiResponse(data)
}

export async function changeTenantStatus(tenant: Tenant, status: TenantStatus): Promise<void> {
  const { data } = await http.post<ApiResponse<void>>('/auth/tenant/status', { id: tenant.id, version: tenant.version, status })
  unwrapApiResponse(data)
}

export async function deleteTenant(tenant: Tenant): Promise<void> {
  const { data } = await http.delete<ApiResponse<void>>('/auth/tenant/delete', { params: { id: tenant.id, version: tenant.version } })
  unwrapApiResponse(data)
}
