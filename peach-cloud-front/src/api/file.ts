import http from './http'
import { unwrapApiResponse } from './http'
import type { ApiResponse } from '../types/auth'
import type { DataRecord } from './admin'

export async function fetchStorageInstances(query: DataRecord = {}): Promise<DataRecord[]> {
  const { data } = await http.post<ApiResponse<DataRecord[]>>('/file/internal/storage/instance/list', query)
  return unwrapApiResponse(data) ?? []
}

export async function fetchEnabledStorageInstances(): Promise<DataRecord[]> {
  const { data } = await http.get<ApiResponse<DataRecord[]>>('/file/internal/storage/instance/listEnabled')
  return unwrapApiResponse(data) ?? []
}

export async function saveStorageInstance(record: DataRecord, isUpdate: boolean): Promise<DataRecord> {
  const request = isUpdate
    ? http.put<ApiResponse<DataRecord>>('/file/internal/storage/instance', record)
    : http.post<ApiResponse<DataRecord>>('/file/internal/storage/instance', record)
  const { data } = await request
  return unwrapApiResponse(data, '保存存储实例失败', { requireData: true })
}

export async function deleteStorageInstance(instanceId: string): Promise<void> {
  const { data } = await http.delete<ApiResponse<void>>(`/file/internal/storage/instance/${encodeURIComponent(instanceId)}`)
  unwrapApiResponse(data, '删除存储实例失败')
}

export async function fetchStorageObjects(instanceId: string, path: string): Promise<DataRecord[]> {
  const { data } = await http.post<ApiResponse<DataRecord[]>>(
    `/file/internal/storage/browser/${encodeURIComponent(instanceId)}/list`,
    { path },
  )
  return unwrapApiResponse(data) ?? []
}

export async function createStorageDirectory(instanceId: string, path: string): Promise<void> {
  const { data } = await http.post<ApiResponse<void>>(
    `/file/internal/storage/browser/${encodeURIComponent(instanceId)}/mkdir`,
    { path },
  )
  unwrapApiResponse(data, '创建目录失败')
}

export async function deleteStorageObject(instanceId: string, objectKey: string): Promise<void> {
  const { data } = await http.post<ApiResponse<void>>(
    `/file/internal/storage/browser/${encodeURIComponent(instanceId)}/delete-object`,
    { objectKey },
  )
  unwrapApiResponse(data, '删除对象失败')
}
