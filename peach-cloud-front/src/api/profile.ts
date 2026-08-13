import http from './http'
import { unwrapApiResponse } from './http'
import type { ApiResponse } from '../types/auth'
import type { AvatarHistoryItem, UserProfile, UserProfileUpdateRequest } from '../types/profile'

export async function fetchCurrentProfile(): Promise<UserProfile> {
  const { data } = await http.get<ApiResponse<UserProfile>>('/auth/profile')
  return unwrapApiResponse(data, '个人资料加载失败', { requireData: true })
}

export async function updateCurrentProfile(request: UserProfileUpdateRequest): Promise<UserProfile> {
  const { data } = await http.post<ApiResponse<UserProfile>>('/auth/profile/basic', request)
  return unwrapApiResponse(data, '个人信息保存失败', { requireData: true })
}

export async function uploadProfileAvatar(file: File): Promise<AvatarHistoryItem> {
  const formData = new FormData()
  formData.append('file', file)
  const { data } = await http.post<ApiResponse<AvatarHistoryItem>>('/auth/profile/avatar', formData)
  return unwrapApiResponse(data, '头像上传失败', { requireData: true })
}

export async function selectProfileAvatar(avatarHistoryId: string): Promise<AvatarHistoryItem> {
  const { data } = await http.post<ApiResponse<AvatarHistoryItem>>(
    `/auth/profile/avatar/${encodeURIComponent(avatarHistoryId)}/select`,
  )
  return unwrapApiResponse(data, '头像切换失败', { requireData: true })
}
