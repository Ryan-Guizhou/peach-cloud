import axios from 'axios'

import { clearAuthSession, readAuthSession } from '../utils/auth-storage'
import type { ApiResponse } from '../types/auth'

const LOGIN_PATH = '/login'

const http = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL ?? '/api',
  timeout: 12000,
  withCredentials: true,
})

http.interceptors.request.use((config) => {
  const session = readAuthSession()
  if (session?.token) {
    config.headers.set('Authorization', session.token)
  }
  return config
})

http.interceptors.response.use(
  response => response,
  (error: unknown) => {
    if (axios.isAxiosError(error)) {
      if (error.response?.status === 401) {
        clearAuthSession()
        const currentPath = `${window.location.pathname}${window.location.search}${window.location.hash}`
        const redirect = currentPath && currentPath !== LOGIN_PATH
          ? `?redirect=${encodeURIComponent(currentPath)}`
          : ''
        window.location.replace(`${LOGIN_PATH}${redirect}`)
      }
      const status = error.response?.status
      const statusText = error.response?.statusText
      const url = error.config?.url
      let message = '网络请求失败'
      if (error.code === 'ECONNABORTED' || error.message.includes('timeout')) {
        message = `请求超时，请检查服务是否启动 (${url || ''})`
      } else if (error.code === 'ERR_NETWORK' || !error.response) {
        message = `无法连接到服务器，请检查网关/后端服务 (${url || ''})`
      } else if (status && statusText) {
        message = `HTTP ${status} ${statusText} (${url || ''})`
      }
      const wrappedError = new Error(message)
      ;(wrappedError as Error & { cause?: unknown }).cause = error
      return Promise.reject(wrappedError)
    }
    return Promise.reject(error)
  },
)

export default http

export function isSuccessCode(code: unknown): boolean {
  if (typeof code === 'number') {
    return code >= 200 && code < 300
  }
  if (typeof code === 'string') {
    return /^2\d{2}$/.test(code)
  }
  return false
}

export function unwrapApiResponse<T>(
  response: ApiResponse<T> | undefined | null,
  fallbackMessage = '请求失败',
  options: { requireData?: boolean } = {},
): T {
  if (!response) {
    throw new Error('接口返回为空')
  }
  if (!isSuccessCode(response.code)) {
    throw new Error(response.msg || fallbackMessage)
  }
  if (options.requireData && (response.data === undefined || response.data === null)) {
    throw new Error(response.msg || '接口未返回有效数据')
  }
  return response.data as T
}
