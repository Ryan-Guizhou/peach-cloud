export interface ApiResponse<T> {
  code: string
  msg: string
  data?: T
}

export interface LoginInitInfo {
  systemName: string
  systemDescription: string
  appId: string
  publicKey: string
  encryptionAlgorithm: string
  captchaType?: string
  captchaRequired?: boolean
  loginConfig?: Record<string, string>
}

export interface CaptchaChallenge {
  token: string
  secretKey?: string
  slidingOriginalImageBase64?: string
  newSlidingBlockingImageBase64?: string
  captchaType?: string
}

export interface CaptchaRequestPayload {
  captchaType: string
  clientUid: string
  browserInfo?: string
  token?: string
  answer?: string
}

export interface CaptchaCheckResult {
  captchaVerification?: string
  result?: boolean
}

export interface LoginRequest {
  username: string
  password: string
  tenantRefId?: string
  appRefId?: string
  invitationRefId?: string
  captchaVerification?: string
  clientUid?: string
}

export interface LoginTargetInfo {
  tenantRefId: string
  tenantId: string
  tenantName: string
  appRefId: string
  appId: string
  appName: string
}

export interface UserOrgInfo {
  tenantId?: string
  tenantCode?: string
  tenantName?: string
  orgId?: string
  orgCode?: string
  orgName?: string
  isDefault?: number
}

export interface RoleInfo {
  roleCode?: string
  roleName?: string
  roleScope?: string
}

export interface MenuInfo {
  menuId?: string
  menuName?: string
  menuCode?: string
  menuUrl?: string
  menuIcon?: string
  parentMenuId?: string
  funcCode?: string
}

export interface RouterInfo {
  routerId?: string
  routerCode?: string
  routerName?: string
  routerUrl?: string
  filePath?: string
  funcCode?: string
}

export interface AuthResourceInfo {
  resourceId?: string
  tenantId?: string
  orgId?: string
  partyCode?: string
  funcCode?: string
  opType?: string
  resourceCode?: string
  resourceName?: string
  appId?: string
}

export interface LoginPermissionSnapshot {
  roleList: RoleInfo[]
  menuList: MenuInfo[]
  routerList: RouterInfo[]
  resourceList: AuthResourceInfo[]
  permissionList: string[]
  apiResourceCodes?: string[]
  buttonResourceCodes?: string[]
}

export interface LoginInfo {
  userId: string
  userName: string
  userRefId?: string
  tenantRefId?: string
  memberRefId?: string
  appRefId?: string
  appId?: string
  memberVersion?: number
  authVersion?: number
  requiresTenantSelection?: boolean
  tenantOptions?: LoginTargetInfo[]
  tenantId?: string
  tenantName?: string
  orgId?: string
  orgCode?: string
  orgName?: string
  /** 无租户选择阶段不会产生 Token，必须在 Store 建立会话前验证。 */
  token?: string
  isDefaultPwd?: number
  userOrgList: UserOrgInfo[]
  roleList: RoleInfo[]
  menuList: MenuInfo[]
  routerList: RouterInfo[]
  resourceList?: AuthResourceInfo[]
  permissionList?: string[]
  permissionSnapshot?: LoginPermissionSnapshot
}

export interface AuthSession {
  token: string
  userId: string
  userRefId: string
  userName: string
  tenantRefId: string
  memberRefId: string
  appRefId: string
  tenantId?: string
  tenantName?: string
  orgId?: string
  orgName?: string
  expiresAt: number
}
