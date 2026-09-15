import { sm2 } from 'sm-crypto'

/**
 * sm-crypto 0.5：0 = C1C2C3，1 = C1C3C2（默认）。
 * Hutool SM2 默认 C1C3C2，所以这里传 1。
 */
const CIPHER_MODE_C1C3C2 = 1

/**
 * 用后端下发的 SM2 公钥加密密码。
 * 密文为 hex，C1 不含 04 前缀（后端会兼容补齐）。
 */
export function encryptPassword(plain: string, publicKeyHex: string): string {
  const key = publicKeyHex.startsWith('04') ? publicKeyHex : `04${publicKeyHex}`
  return sm2.doEncrypt(plain, key, CIPHER_MODE_C1C3C2)
}
