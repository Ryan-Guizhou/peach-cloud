import http from './http'
import { unwrapApiResponse } from './http'
import { getCaptchaClientContext } from '../utils/captcha-client'
import type {
  ApiResponse,
  CaptchaChallenge,
  CaptchaCheckResult,
  CaptchaRequestPayload,
  LoginInfo,
  LoginInitInfo,
  LoginRequest,
} from '../types/auth'

function withCaptchaClient(payload: Partial<CaptchaRequestPayload>): CaptchaRequestPayload {
  const client = getCaptchaClientContext()
  return {
    captchaType: payload.captchaType ?? 'BLOCKPUZZLE',
    clientUid: payload.clientUid ?? client.clientUid,
    browserInfo: payload.browserInfo ?? client.browserInfo,
    token: payload.token,
    answer: payload.answer,
  }
}

export async function initLogin(): Promise<LoginInitInfo> {
  const { data } = await http.post<ApiResponse<LoginInitInfo>>('/auth/init')
  return unwrapApiResponse(data, '初始化登录环境失败', { requireData: true })
}

export async function fetchLoginCaptcha(captchaType: string): Promise<CaptchaChallenge> {
  const { data } = await http.post<ApiResponse<CaptchaChallenge>>('/auth/getCaptcha', withCaptchaClient({ captchaType }))
  return unwrapApiResponse(data, '获取验证码失败', { requireData: true })
}

export async function verifyLoginCaptcha(request: {
  captchaType: string
  token: string
  answer: string
}): Promise<CaptchaCheckResult> {
  const { data } = await http.post<ApiResponse<CaptchaCheckResult>>('/auth/checkCaptcha', withCaptchaClient(request))
  return unwrapApiResponse(data, '验证码校验失败', { requireData: true })
}

export async function submitLogin(request: LoginRequest): Promise<LoginInfo> {
  const client = getCaptchaClientContext()
  const { data } = await http.post<ApiResponse<LoginInfo>>('/auth/login', {
    ...request,
    clientUid: request.clientUid ?? client.clientUid,
  })
  return unwrapApiResponse(data, '登录失败', { requireData: true })
}

export async function submitLogout(): Promise<void> {
  const { data } = await http.post<ApiResponse<unknown>>('/auth/logout')
  unwrapApiResponse(data, '退出失败')
}
