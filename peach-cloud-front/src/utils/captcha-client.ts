const CAPTCHA_CLIENT_UID_KEY = 'peach:captcha:client-uid'

export interface CaptchaClientContext {
  clientUid: string
  browserInfo: string
}

function createClientUid(): string {
  if (typeof crypto !== 'undefined' && typeof crypto.randomUUID === 'function') {
    return crypto.randomUUID().replace(/-/g, '')
  }
  return `${Date.now()}${Math.random().toString(16).slice(2)}`
}

export function getCaptchaClientContext(): CaptchaClientContext {
  let clientUid = window.localStorage.getItem(CAPTCHA_CLIENT_UID_KEY)
  if (!clientUid) {
    clientUid = createClientUid()
    window.localStorage.setItem(CAPTCHA_CLIENT_UID_KEY, clientUid)
  }
  const browserInfo = `${window.navigator.userAgent}##${clientUid}`
  return { clientUid, browserInfo }
}
