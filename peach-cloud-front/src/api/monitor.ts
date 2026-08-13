import http from './http'
import { unwrapApiResponse } from './http'
import type { ApiResponse } from '../types/auth'
import type { DataRecord } from './admin'

export async function fetchRuntimeSnapshot(): Promise<DataRecord> {
  const { data } = await http.get<ApiResponse<DataRecord>>('/monitor/snapshot')
  return unwrapApiResponse(data, '加载监控快照失败', { requireData: true })
}
