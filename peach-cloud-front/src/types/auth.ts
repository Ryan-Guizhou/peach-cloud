export interface ApiResponse<T> {
  code: string
  msg: string
  data?: T
}

export interface LoginInitInfo {
  systemName: string
  systemDescription: string
  appId: string
  fiscal: number
  publicKey: string
  encryptionAlgorithm: string
}

export interface LoginRequest {
  username: string
  password: string
  fiscal: number
  tenantId?: string
  orgId?: string
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
  fiscal?: number
}

export interface LoginInfo {
  userId: string
  userName: string
  fiscal: string
  tenantId: string
  tenantCode?: string
  tenantName?: string
  orgId: string
  orgCode?: string
  orgName?: string
  token: string
  isDefaultPwd?: number
  userOrgList: UserOrgInfo[]
  roleList: RoleInfo[]
  menuList: MenuInfo[]
  routerList: RouterInfo[]
  resourceList?: AuthResourceInfo[]
  permissionList?: string[]
}

export interface AuthSession {
  token: string
  userId: string
  userName: string
  tenantId: string
  tenantName?: string
  orgId: string
  orgName?: string
  fiscal: string
  expiresAt: number
}
