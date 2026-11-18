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

function parsePermissionSnapshot(value: unknown): LoginInfo['permissionSnapshot'] {
  if (!isRecord(value)) {
    return undefined
  }
  return {
    roleList: Array.isArray(value.roleList) ? value.roleList as LoginInfo['roleList'] : [],
    menuList: Array.isArray(value.menuList) ? value.menuList as LoginInfo['menuList'] : [],
    routerList: Array.isArray(value.routerList) ? value.routerList as LoginInfo['routerList'] : [],
    resourceList: Array.isArray(value.resourceList) ? value.resourceList as NonNullable<LoginInfo['resourceList']> : [],
    permissionList: Array.isArray(value.permissionList)
      ? value.permissionList.filter((item): item is string => typeof item === 'string')
      : [],
    apiResourceCodes: Array.isArray(value.apiResourceCodes)
      ? value.apiResourceCodes.filter((item): item is string => typeof item === 'string')
      : undefined,
    buttonResourceCodes: Array.isArray(value.buttonResourceCodes)
      ? value.buttonResourceCodes.filter((item): item is string => typeof item === 'string')
      : undefined,
  }
}

function parseSession(raw: string | null): AuthSession | null {
  if (!raw) {
    return null
  }
  try {
    const value: unknown = JSON.parse(raw)
    if (!isRecord(value) || typeof value.expiresAt !== 'number'
      || value.expiresAt <= Date.now() || !Number.isInteger(value.year)
      || value.year < 1 || value.year > 9999) {
      return null
    }
    const token = normalizeRequiredString(value.token)
    const userId = normalizeRequiredString(value.userId)
    const userRefId = normalizeRequiredString(value.userRefId)
    const userName = normalizeRequiredString(value.userName)
    const tenantRefId = normalizeRequiredString(value.tenantRefId)
    const memberRefId = normalizeRequiredString(value.memberRefId)
    const appRefId = normalizeRequiredString(value.appRefId)
    if (!token || !userId || !userRefId || !userName || !tenantRefId
      || !memberRefId || !appRefId) {
      return null
    }
    return {
      token,
      userId,
      userRefId,
      userName,
      tenantRefId,
      memberRefId,
      appRefId,
      tenantId: normalizeOptionalString(value.tenantId),
      tenantName: normalizeOptionalString(value.tenantName),
      orgId: normalizeOptionalString(value.orgId),
      orgName: normalizeOptionalString(value.orgName),
      year: value.year as number,
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
  const session = readAuthSession()
  if (!session) {
    return null
  }
  const raw = window.localStorage.getItem(LOGIN_INFO_KEY)
    ?? window.sessionStorage.getItem(LOGIN_INFO_KEY)
  if (!raw) {
    return null
  }
  try {
    const value: unknown = JSON.parse(raw)
    if (!isRecord(value) || value.requiresTenantSelection === true
      || value.token !== session.token
      || value.userRefId !== session.userRefId
      || value.tenantRefId !== session.tenantRefId
      || value.memberRefId !== session.memberRefId
      || value.appRefId !== session.appRefId
      || value.year !== session.year
      || !isRequiredString(value.userId) || !isRequiredString(value.userName)) {
      return null
    }
    return {
      userId: value.userId,
      userName: value.userName,
      userRefId: session.userRefId,
      tenantRefId: session.tenantRefId,
      memberRefId: session.memberRefId,
      appRefId: session.appRefId,
      year: session.year,
      tenantId: normalizeOptionalString(value.tenantId),
      tenantName: normalizeOptionalString(value.tenantName),
      orgId: normalizeOptionalString(value.orgId),
      orgCode: normalizeOptionalString(value.orgCode),
      orgName: normalizeOptionalString(value.orgName),
      appId: normalizeOptionalString(value.appId),
      memberVersion: typeof value.memberVersion === 'number' ? value.memberVersion : undefined,
      authVersion: typeof value.authVersion === 'number' ? value.authVersion : undefined,
      token: session.token,
      requiresTenantSelection: false,
      tenantOptions: Array.isArray(value.tenantOptions)
        ? value.tenantOptions as LoginInfo['tenantOptions'] : [],
      isDefaultPwd: typeof value.isDefaultPwd === 'number' ? value.isDefaultPwd : undefined,
      userOrgList: Array.isArray(value.userOrgList) ? value.userOrgList : [],
      roleList: Array.isArray(value.roleList) ? value.roleList : [],
      menuList: Array.isArray(value.menuList) ? value.menuList : [],
      routerList: Array.isArray(value.routerList) ? value.routerList : [],
      resourceList: Array.isArray(value.resourceList) ? value.resourceList : [],
      permissionList: Array.isArray(value.permissionList)
        ? value.permissionList.filter((item): item is string => typeof item === 'string' && item.length > 0)
        : [],
      permissionSnapshot: parsePermissionSnapshot(value.permissionSnapshot),
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

/** 将仅存在于 sessionStorage 的会话镜像到 localStorage，供新标签页手册站点读取。 */
export function mirrorSessionStorageForNewTab(): void {
  if (window.localStorage.getItem(SESSION_KEY)) {
    return
  }
  const sessionRaw = window.sessionStorage.getItem(SESSION_KEY)
  if (!sessionRaw) {
    return
  }
  window.localStorage.setItem(SESSION_KEY, sessionRaw)
  const loginInfoRaw = window.sessionStorage.getItem(LOGIN_INFO_KEY)
  if (loginInfoRaw) {
    window.localStorage.setItem(LOGIN_INFO_KEY, loginInfoRaw)
  }
}
