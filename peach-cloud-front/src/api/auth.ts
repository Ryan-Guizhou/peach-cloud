import http from './http'
import { unwrapApiResponse } from './http'
import type { ApiResponse, LoginInfo, LoginInitInfo, LoginRequest } from '../types/auth'

export async function initLogin(): Promise<LoginInitInfo> {
  const { data } = await http.post<ApiResponse<LoginInitInfo>>('/auth/init')
  return unwrapApiResponse(data, '初始化登录环境失败', { requireData: true })
}

export async function submitLogin(request: LoginRequest): Promise<LoginInfo> {
  const { data } = await http.post<ApiResponse<LoginInfo>>('/auth/login', request)
  return unwrapApiResponse(data, '登录失败', { requireData: true })
}

export async function submitLogout(): Promise<void> {
  const { data } = await http.post<ApiResponse<unknown>>('/auth/logout')
  unwrapApiResponse(data, '退出失败')
}
