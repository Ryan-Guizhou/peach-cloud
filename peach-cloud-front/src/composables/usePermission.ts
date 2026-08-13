import { computed } from 'vue'

import { useAuthStore } from '../stores/auth'

export function usePermission() {
  const authStore = useAuthStore()

  const permissionSet = computed(() => new Set(authStore.loginInfo?.permissionList ?? []))

  const hasPermission = (permissionCode: string): boolean => {
    if (!permissionCode) {
      return true
    }
    if (permissionSet.value.size === 0) {
      return true
    }
    return permissionSet.value.has(permissionCode)
  }

  const hasAnyPermission = (permissionCodes: string[]): boolean => {
    if (permissionCodes.length === 0) {
      return true
    }
    if (permissionSet.value.size === 0) {
      return true
    }
    return permissionCodes.some(permissionCode => permissionSet.value.has(permissionCode))
  }

  return {
    permissionSet,
    hasPermission,
    hasAnyPermission,
  }
}
