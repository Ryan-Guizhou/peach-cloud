function base64Encode(bytes: Uint8Array): string {
  let binary = ''
  bytes.forEach(byte => {
    binary += String.fromCharCode(byte)
  })
  return window.btoa(binary)
}

export async function encryptCaptchaPayload(payload: string, secretKey: string): Promise<string> {
  if (!secretKey) {
    return payload
  }
  const iv = crypto.getRandomValues(new Uint8Array(12))
  const keyMaterial = new TextEncoder().encode(secretKey)
  const cryptoKey = await crypto.subtle.importKey(
    'raw',
    keyMaterial,
    { name: 'AES-GCM' },
    false,
    ['encrypt'],
  )
  const encrypted = await crypto.subtle.encrypt(
    { name: 'AES-GCM', iv, tagLength: 128 },
    cryptoKey,
    new TextEncoder().encode(payload),
  )
  const cipherBytes = new Uint8Array(encrypted)
  const combined = new Uint8Array(iv.length + cipherBytes.length)
  combined.set(iv, 0)
  combined.set(cipherBytes, iv.length)
  return base64Encode(combined)
}

export function buildCaptchaPointPayload(secretKey: string, x: number, y: number): string {
  return JSON.stringify({ secretKey, x, y })
}
