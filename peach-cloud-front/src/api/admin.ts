import http from './http'
import { unwrapApiResponse } from './http'
import type { ApiResponse, LoginInfo } from '../types/auth'

export type PrimitiveValue = string | number | boolean | null | undefined
export type DataRecord = Record<string, PrimitiveValue | PrimitiveValue[]>

export interface PageResult<T> {
  list?: T[]
  result?: T[]
  total?: number
  topTotal?: number
  pageNum?: number
  pageSize?: number
  pages?: number
}

export interface CrudEndpoint {
  pageList: string
  add?: string
  update?: string
  delById?: string
  idField?: string
  deleteMode?: 'query' | 'body-list'
}

export async function fetchPage<T extends DataRecord>(
  url: string,
  query: DataRecord,
): Promise<PageResult<T>> {
  const { data } = await http.post<ApiResponse<PageResult<T>>>(url, query)
  const page = unwrapApiResponse<PageResult<T>>(data)
  return {
    ...page,
    list: page.list ?? page.result ?? [],
    total: page.total ?? page.topTotal ?? 0,
  }
}

export async function postRecord(url: string, record: DataRecord): Promise<void> {
  const { data } = await http.post<ApiResponse<void>>(url, record)
  unwrapApiResponse(data)
}

export async function deleteById(
  url: string,
  idField: string,
  id: string,
  mode: CrudEndpoint['deleteMode'] = 'query',
): Promise<void> {
  const request = mode === 'body-list'
    ? http.delete<ApiResponse<void>>(url, { data: [id] })
    : http.delete<ApiResponse<void>>(url, { params: { [idField]: id } })
  const { data } = await request
  unwrapApiResponse(data)
}

export async function switchContext(request: {
  tenantRefId: string
  appRefId: string
}): Promise<LoginInfo> {
  const { data } = await http.post<ApiResponse<LoginInfo>>('/auth/switchContext', request)
  return unwrapApiResponse(data, '切换机构失败', { requireData: true })
}
