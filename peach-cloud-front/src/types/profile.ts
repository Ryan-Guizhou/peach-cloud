export interface AvatarHistoryItem {
  avatarHistoryId: string
  fileId: string
  avatarUrl?: string
  sortNo: number
  isCurrent: number
  createdTime?: string
}

export interface UserProfile {
  userId: string
  userCode: string
  userName: string
  mobilePhone?: string
  email?: string
  status?: string
  lastestLogin?: string
  defaultOrgId?: string
  currentAvatar?: AvatarHistoryItem
  avatarHistory: AvatarHistoryItem[]
}

export interface UserProfileUpdateRequest {
  userName: string
  mobilePhone?: string
  email?: string
}
