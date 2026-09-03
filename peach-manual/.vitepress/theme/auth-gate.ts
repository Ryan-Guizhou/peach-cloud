const SESSION_KEY = 'peach.auth.session.v1'

export interface ManualAccessResult {
  ok: boolean
  message?: string
  redirectUrl?: string
}

function isAuthEnabled(): boolean {
  return import.meta.env.VITE_MANUAL_AUTH_ENABLED !== 'false'
}

function readToken(): string | null {
  for (const storage of [window.localStorage, window.sessionStorage]) {
    const raw = storage.getItem(SESSION_KEY)
    if (!raw) {
      continue
    }
    try {
      const parsed: unknown = JSON.parse(raw)
      if (typeof parsed !== 'object' || parsed === null) {
        continue
      }
      const record = parsed as { token?: unknown; expiresAt?: unknown }
      if (typeof record.token !== 'string' || record.token.length === 0) {
        continue
      }
      if (typeof record.expiresAt !== 'number' || record.expiresAt <= Date.now()) {
        continue
      }
      return record.token
    } catch {
      continue
    }
  }
  return null
}

function buildLoginRedirect(): string {
  const loginPath = import.meta.env.VITE_MANUAL_LOGIN_PATH ?? '/login'
  const current = `${window.location.pathname}${window.location.search}${window.location.hash}`
  return `${loginPath}?redirect=${encodeURIComponent(current)}`
}

export async function verifyManualAccess(): Promise<ManualAccessResult> {
  if (!isAuthEnabled()) {
    return { ok: true }
  }

  const token = readToken()
  if (!token) {
    return {
      ok: false,
      message: '未登录，正在跳转登录页…',
      redirectUrl: buildLoginRedirect(),
    }
  }

  const apiBase = import.meta.env.VITE_MANUAL_API_BASE ?? '/api'
  try {
    const response = await fetch(`${apiBase}/auth/profile`, {
      headers: {
        Authorization: token,
      },
      credentials: 'include',
    })
    if (response.status === 401) {
      return {
        ok: false,
        message: '登录已失效，正在跳转登录页…',
        redirectUrl: buildLoginRedirect(),
      }
    }
    if (!response.ok) {
      return {
        ok: false,
        message: `登录校验失败（HTTP ${response.status}），请稍后重试`,
      }
    }
    return { ok: true }
  } catch {
    return {
      ok: false,
      message: '无法连接认证服务，请确认网关与 peach-auth 已启动',
    }
  }
}
