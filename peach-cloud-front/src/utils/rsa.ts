import JSEncrypt from 'jsencrypt'

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

export async function encryptLoginPassword(plainText: string, publicKey: string): Promise<string> {
  const formattedKey = publicKey.match(/.{1,64}/g)?.join('\n')
  if (!formattedKey) {
    throw new Error('登录公钥格式无效')
  }

  const encryptor = new JSEncrypt()
  encryptor.setPublicKey(`-----BEGIN PUBLIC KEY-----\n${formattedKey}\n-----END PUBLIC KEY-----`)
  const encryptedBase64 = encryptor.encrypt(plainText)
  if (!encryptedBase64) {
    throw new Error('密码加密失败，请刷新页面后重试')
  }
  return bytesToHex(base64ToBytes(encryptedBase64))
}
