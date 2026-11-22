import http, { unwrapApiResponse } from './http'
import type { ApiResponse } from '../types/auth'
import type { PageResult } from './admin'

export interface LoginLog {
  logId: string
  occurredAt: string
  eventType: string
  result: string
  reasonCode: string
  account?: string
  userId?: string
  tenantId?: string
  orgId?: string
  ipAddress: string
  browser: string
  durationMs: number
  traceId: string
}
export interface LoginLogQuery {
  pageNum: number
  pageSize: number
  scope: 'TENANT' | 'UNASSIGNED'
  account: string
  ipAddress: string
  result?: string
  eventType?: string
  startTime?: string
  endTime?: string
}
export async function fetchLoginLogs(query: LoginLogQuery, signal?: AbortSignal): Promise<PageResult<LoginLog>> {
  const payload = { ...query, ipAddress: query.ipAddress || undefined }
  const { data } = await http.post<ApiResponse<PageResult<LoginLog>>>('/auth/loginLog/pageList', payload, { signal })
  return unwrapApiResponse(data, '加载登录日志失败', { requireData: true })
}
export async function fetchLoginLog(logId: string, signal?: AbortSignal): Promise<LoginLog> {
  const { data } = await http.get<ApiResponse<LoginLog>>('/auth/loginLog/selectById', { params: { logId }, signal })
  return unwrapApiResponse(data, '加载日志详情失败', { requireData: true })
}
