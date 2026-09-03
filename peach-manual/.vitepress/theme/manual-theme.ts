export type ManualThemeMode = 'light' | 'dark' | 'auto'

export const MANUAL_THEME_STORAGE_KEY = 'peach-manual-theme'

export function resolveManualThemeIsDark(mode: ManualThemeMode): boolean {
  if (mode === 'dark') {
    return true
  }
  if (mode === 'auto') {
    return window.matchMedia('(prefers-color-scheme: dark)').matches
  }
  return false
}

export function applyManualTheme(mode: ManualThemeMode): void {
  if (typeof document === 'undefined') {
    return
  }
  const root = document.documentElement
  root.classList.toggle('dark', resolveManualThemeIsDark(mode))
  root.dataset.manualTheme = mode
}

export function readStoredManualTheme(): ManualThemeMode {
  if (typeof localStorage === 'undefined') {
    return 'light'
  }
  const value = localStorage.getItem(MANUAL_THEME_STORAGE_KEY)
  if (value === 'dark' || value === 'auto' || value === 'light') {
    return value
  }
  return 'light'
}

export function storeManualTheme(mode: ManualThemeMode): void {
  localStorage.setItem(MANUAL_THEME_STORAGE_KEY, mode)
}

export const manualThemeOptions: Array<{ value: ManualThemeMode; label: string }> = [
  { value: 'light', label: '浅色' },
  { value: 'dark', label: '深色' },
  { value: 'auto', label: '自动' },
]
