import { mirrorSessionStorageForNewTab, readAuthSession } from './auth-storage'

function resolveManualBaseUrl(): string {
  const base = import.meta.env.VITE_MANUAL_BASE_URL
  if (typeof base === 'string' && base.length > 0) {
    return base.endsWith('/') ? base : `${base}/`
  }
  return '/manual/'
}

function buildManualTargetUrl(baseUrl: string, path: string): string {
  const normalizedPath = path.startsWith('/') ? path.slice(1) : path
  if (baseUrl.startsWith('http://') || baseUrl.startsWith('https://')) {
    return `${baseUrl}${normalizedPath}`
  }
  const basePath = baseUrl.replace(/\/$/, '')
  if (!normalizedPath) {
    return `${basePath}/`
  }
  return `${basePath}/${normalizedPath}`
}

function buildLoginRedirect(targetUrl: string): string {
  const redirectTarget = targetUrl.startsWith('http')
    ? targetUrl
    : `${window.location.origin}${targetUrl}`
  return `/login?redirect=${encodeURIComponent(redirectTarget)}`
}

export function isManualFeatureEnabled(): boolean {
  return import.meta.env.VITE_MANUAL_ENABLED !== 'false'
}

export function openPeachManual(path = '/'): void {
  const baseUrl = resolveManualBaseUrl()
  const targetUrl = buildManualTargetUrl(baseUrl, path)
  const session = readAuthSession()

  if (!session) {
    window.location.href = buildLoginRedirect(targetUrl)
    return
  }

  mirrorSessionStorageForNewTab()
  window.open(targetUrl, '_blank', 'noopener,noreferrer')
}
