import http, { unwrapApiResponse } from './http'
import type { ApiResponse } from '../types/auth'
import type { DataRecord, PageResult } from './admin'

export interface FunctionDefinition extends DataRecord {
  funcId: string
  funcCode: string
  funcName: string
  parentFuncCode?: string
  funcSeq?: string
}
export interface FunctionResource extends DataRecord {
  resourceId: string
  funcCode: string
  resourceCode: string
  resourceName: string
  resourceType: string
}
export async function fetchFunctions(signal?: AbortSignal): Promise<FunctionDefinition[]> {
  const { data } = await http.post<ApiResponse<FunctionDefinition[]>>('/auth/function/list', {}, { signal })
  return unwrapApiResponse(data) ?? []
}
export async function fetchResources(query: DataRecord, signal?: AbortSignal): Promise<PageResult<FunctionResource>> {
  const { data } = await http.post<ApiResponse<PageResult<FunctionResource>>>('/auth/resource/pageList', query, { signal })
  return unwrapApiResponse(data) ?? {}
}
