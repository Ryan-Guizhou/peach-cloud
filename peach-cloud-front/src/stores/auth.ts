import { defineStore } from 'pinia'

import type { AuthSession, LoginInfo } from '../types/auth'
import {
  clearAuthSession,
  readAuthLoginInfo,
  readAuthSession,
  updateAuthLoginInfo,
  updateAuthSession,
  writeAuthLoginInfo,
  writeAuthSession,
} from '../utils/auth-storage'

const REMEMBER_DURATION = 7 * 24 * 60 * 60 * 1000
const TAB_DURATION = 12 * 60 * 60 * 1000

function toAuthenticatedSession(loginInfo: LoginInfo, expiresAt: number): AuthSession {
  const { token, userRefId, tenantRefId, memberRefId, appRefId, year } = loginInfo
  if (loginInfo.requiresTenantSelection || !token || !userRefId || !tenantRefId
    || !memberRefId || !appRefId || !Number.isInteger(year) || year < 1 || year > 9999) {
    throw new Error('登录尚未完成有效的租户与应用选择')
  }
  return {
    token,
    userId: loginInfo.userId,
    userRefId,
    userName: loginInfo.userName,
    tenantRefId,
    memberRefId,
    appRefId,
    tenantId: loginInfo.tenantId,
    tenantName: loginInfo.tenantName,
    orgId: loginInfo.orgId,
    orgName: loginInfo.orgName,
    year,
    expiresAt,
  }
}

interface AuthState {
  session: AuthSession | null
  loginInfo: LoginInfo | null
}

export const useAuthStore = defineStore('auth', {
  state: (): AuthState => ({
    session: readAuthSession(),
    loginInfo: readAuthLoginInfo(),
  }),
  getters: {
    isAuthenticated: state => Boolean(state.session?.token && state.session.expiresAt > Date.now()),
    permissions: state => new Set(state.loginInfo?.permissionList ?? []),
  },
  actions: {
    ensureValidSession() {
      const session = readAuthSession()
      if (!session) {
        this.session = null
        this.loginInfo = null
        clearAuthSession()
        return false
      }
      const loginInfo = readAuthLoginInfo()
      if (!loginInfo || loginInfo.token !== session.token) {
        this.clearSession()
        return false
      }
      this.session = session
      this.loginInfo = loginInfo
      return true
    },
    establishSession(loginInfo: LoginInfo, remember: boolean) {
      const session = toAuthenticatedSession(loginInfo,
        Date.now() + (remember ? REMEMBER_DURATION : TAB_DURATION))
      this.session = session
      this.loginInfo = loginInfo
      writeAuthSession(session, remember)
      writeAuthLoginInfo(loginInfo, remember)
    },
    replaceLoginInfo(loginInfo: LoginInfo) {
      const expiresAt = this.session?.expiresAt ?? Date.now() + TAB_DURATION
      const session = toAuthenticatedSession(loginInfo, expiresAt)
      this.session = session
      this.loginInfo = loginInfo
      updateAuthSession(session)
      updateAuthLoginInfo(loginInfo)
    },
    clearSession() {
      this.session = null
      this.loginInfo = null
      clearAuthSession()
    },
    updateProfileName(userName: string) {
      if (this.session) {
        this.session = {
          ...this.session,
          userName,
        }
        updateAuthSession(this.session)
      }
      if (this.loginInfo) {
        this.loginInfo = {
          ...this.loginInfo,
          userName,
        }
        updateAuthLoginInfo(this.loginInfo)
      }
    },
  },
})
