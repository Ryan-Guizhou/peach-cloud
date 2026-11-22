import http, { unwrapApiResponse } from './http'
import type { ApiResponse } from '../types/auth'
import type { DataRecord, PageResult } from './admin'
export interface RouterRecord extends DataRecord { routerId: string; moduleCode: string; routerCode: string; routerName: string; routerUrl: string; filePath: string; routerLevel?: number; funcCode?: string }
export async function fetchRouters(query: { pageNum: number; pageSize: number; moduleCode: string; routerCode: string; routerUrl: string }, signal?: AbortSignal): Promise<PageResult<RouterRecord>> {
  const { data } = await http.post<ApiResponse<PageResult<RouterRecord>>>('/auth/router/pageList', query, { signal })
  return unwrapApiResponse(data, '加载路由失败', { requireData: true })
}
export async function saveRouter(record: Partial<RouterRecord>): Promise<void> {
  const { data } = await http.post<ApiResponse<void>>(`/auth/router/${record.routerId ? 'update' : 'add'}`, record)
  unwrapApiResponse(data)
}
