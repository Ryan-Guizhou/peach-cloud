import http from './http'
import { unwrapApiResponse } from './http'
import type { ApiResponse } from '../types/auth'
import type { DataRecord, PageResult } from './admin'

export async function fetchMessagePage(category: 'message' | 'announcement' | 'todo', query: DataRecord): Promise<PageResult<DataRecord>> {
  const { data } = await http.get<ApiResponse<PageResult<DataRecord>>>(`/message/${category}`, { params: query })
  const page = unwrapApiResponse<PageResult<DataRecord>>(data)
  return {
    ...page,
    list: page.list ?? page.result ?? [],
    total: page.total ?? page.topTotal ?? 0,
  }
}

export async function fetchUnreadCount(): Promise<number> {
  const { data } = await http.get<ApiResponse<number>>('/message/unread-count')
  return unwrapApiResponse(data) ?? 0
}

export async function readMessage(messageId: string): Promise<void> {
  const { data } = await http.post<ApiResponse<void>>(`/message/${encodeURIComponent(messageId)}/read`)
  unwrapApiResponse(data)
}

export async function readAllMessages(category?: 'message' | 'announcement' | 'todo'): Promise<void> {
  const path = category ? `/message/${category}/read-all` : '/message/read-all'
  const { data } = await http.post<ApiResponse<void>>(path)
  unwrapApiResponse(data)
}
