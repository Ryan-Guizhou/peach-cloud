import type { AuthSession, LoginInfo } from '../types/auth'

const SESSION_KEY = 'peach.auth.session.v1'
const LOGIN_INFO_KEY = 'peach.auth.loginInfo.v1'

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === 'object' && value !== null
}

function isRequiredString(value: unknown): value is string {
  return typeof value === 'string' && value.length > 0
}

function normalizeOptionalString(value: unknown): string | undefined {
  return typeof value === 'string' && value.length > 0 ? value : undefined
}

function normalizeRequiredString(value: unknown): string | null {
  if (typeof value === 'string' && value.length > 0) {
    return value
  }
  if (typeof value === 'number' && Number.isFinite(value)) {
    return String(value)
  }
  return null
}

function parseSession(raw: string | null): AuthSession | null {
  if (!raw) {
    return null
  }
  try {
    const value: unknown = JSON.parse(raw)
    if (!isRecord(value) || typeof value.expiresAt !== 'number') {
      return null
    }
    const token = normalizeRequiredString(value.token)
    const userId = normalizeRequiredString(value.userId)
    const userName = normalizeRequiredString(value.userName)
    const tenantId = normalizeRequiredString(value.tenantId)
    const orgId = normalizeRequiredString(value.orgId)
    const fiscal = normalizeRequiredString(value.fiscal)
    if (!token || !userId || !userName || !tenantId || !orgId || !fiscal) {
      return null
    }
    if (value.expiresAt <= Date.now()) {
      return null
    }
    return {
      token,
      userId,
      userName,
      tenantId,
      tenantName: normalizeOptionalString(value.tenantName),
      orgId,
      orgName: normalizeOptionalString(value.orgName),
      fiscal,
      expiresAt: value.expiresAt,
    }
  } catch {
    return null
  }
}

export function readAuthSession(): AuthSession | null {
  const localSession = parseSession(window.localStorage.getItem(SESSION_KEY))
  if (localSession) {
    return localSession
  }
  window.localStorage.removeItem(SESSION_KEY)
  window.localStorage.removeItem(LOGIN_INFO_KEY)
  const tabSession = parseSession(window.sessionStorage.getItem(SESSION_KEY))
  if (!tabSession) {
    window.sessionStorage.removeItem(SESSION_KEY)
    window.sessionStorage.removeItem(LOGIN_INFO_KEY)
  }
  return tabSession
}

export function readAuthLoginInfo(): LoginInfo | null {
  if (!readAuthSession()) {
    return null
  }
  const raw = window.localStorage.getItem(LOGIN_INFO_KEY) ?? window.sessionStorage.getItem(LOGIN_INFO_KEY)
  if (!raw) {
    return null
  }
  try {
    const value: unknown = JSON.parse(raw)
    if (!isRecord(value)
      || !isRequiredString(value.token)
      || !isRequiredString(value.userId)
      || !isRequiredString(value.userName)
      || !isRequiredString(value.tenantId)
      || !isRequiredString(value.orgId)) {
      return null
    }
    const fiscal = normalizeRequiredString(value.fiscal)
    if (!fiscal) {
      return null
    }
    return {
      userId: value.userId,
      userName: value.userName,
      fiscal,
      tenantId: value.tenantId,
      tenantCode: normalizeOptionalString(value.tenantCode),
      tenantName: normalizeOptionalString(value.tenantName),
      orgId: value.orgId,
      orgCode: normalizeOptionalString(value.orgCode),
      orgName: normalizeOptionalString(value.orgName),
      token: value.token,
      isDefaultPwd: typeof value.isDefaultPwd === 'number' ? value.isDefaultPwd : undefined,
      userOrgList: Array.isArray(value.userOrgList) ? value.userOrgList : [],
      roleList: Array.isArray(value.roleList) ? value.roleList : [],
      menuList: Array.isArray(value.menuList) ? value.menuList : [],
      routerList: Array.isArray(value.routerList) ? value.routerList : [],
      resourceList: Array.isArray(value.resourceList) ? value.resourceList : undefined,
      permissionList: Array.isArray(value.permissionList)
        ? value.permissionList.filter((item): item is string => typeof item === 'string' && item.length > 0)
        : undefined,
    }
  } catch {
    return null
  }
}

export function writeAuthSession(session: AuthSession, remember: boolean): void {
  clearAuthSession()
  const storage = remember ? window.localStorage : window.sessionStorage
  storage.setItem(SESSION_KEY, JSON.stringify(session))
}

export function writeAuthLoginInfo(loginInfo: LoginInfo, remember: boolean): void {
  const storage = remember ? window.localStorage : window.sessionStorage
  storage.setItem(LOGIN_INFO_KEY, JSON.stringify(loginInfo))
}

export function updateAuthLoginInfo(loginInfo: LoginInfo): void {
  const hasLocalInfo = window.localStorage.getItem(LOGIN_INFO_KEY) !== null
  const hasTabInfo = window.sessionStorage.getItem(LOGIN_INFO_KEY) !== null
  if (hasLocalInfo) {
    window.localStorage.setItem(LOGIN_INFO_KEY, JSON.stringify(loginInfo))
  }
  if (hasTabInfo || !hasLocalInfo) {
    window.sessionStorage.setItem(LOGIN_INFO_KEY, JSON.stringify(loginInfo))
  }
}

export function updateAuthSession(session: AuthSession): void {
  const hasLocalSession = window.localStorage.getItem(SESSION_KEY) !== null
  const hasTabSession = window.sessionStorage.getItem(SESSION_KEY) !== null
  if (hasLocalSession) {
    window.localStorage.setItem(SESSION_KEY, JSON.stringify(session))
  }
  if (hasTabSession || !hasLocalSession) {
    window.sessionStorage.setItem(SESSION_KEY, JSON.stringify(session))
  }
}

export function clearAuthSession(): void {
  window.localStorage.removeItem(SESSION_KEY)
  window.localStorage.removeItem(LOGIN_INFO_KEY)
  window.sessionStorage.removeItem(SESSION_KEY)
  window.sessionStorage.removeItem(LOGIN_INFO_KEY)
}
