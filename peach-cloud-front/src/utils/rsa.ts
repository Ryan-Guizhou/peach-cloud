function base64ToBytes(value: string): Uint8Array {
  const binary = window.atob(value)
  const bytes = new Uint8Array(binary.length)
  for (let index = 0; index < binary.length; index += 1) {
    bytes[index] = binary.charCodeAt(index)
  }
  return bytes
}

function bytesToHex(bytes: Uint8Array): string {
  return Array.from(bytes, byte => byte.toString(16).padStart(2, '0')).join('')
}

async function importPublicKey(publicKeyBase64: string): Promise<CryptoKey> {
  const keyBytes = base64ToBytes(publicKeyBase64)
  return crypto.subtle.importKey(
    'spki',
    keyBytes,
    { name: 'RSA-OAEP', hash: 'SHA-256' },
    false,
    ['encrypt'],
  )
}

export async function encryptLoginPassword(plainText: string, publicKey: string): Promise<string> {
  if (!publicKey) {
    throw new Error('登录公钥格式无效')
  }

  const cryptoKey = await importPublicKey(publicKey)
  const encrypted = await crypto.subtle.encrypt(
    { name: 'RSA-OAEP' },
    cryptoKey,
    new TextEncoder().encode(plainText),
  )
  return bytesToHex(new Uint8Array(encrypted))
}
