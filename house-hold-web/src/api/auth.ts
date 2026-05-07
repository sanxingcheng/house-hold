import { request } from './request'
import type {
  EncryptedLoginRequest,
  EncryptedRegisterRequest,
  LoginRequest,
  LoginResponse,
  PasswordPublicKeyResponse,
  RegisterRequest,
  RegisterResponse,
} from '@/types/auth'

let passwordPublicKeyPromise: Promise<CryptoKey> | null = null

export async function login(data: LoginRequest) {
  return request.post<LoginResponse>('/auth/login', await encryptLoginRequest(data))
}

export async function register(data: RegisterRequest) {
  return request.post<RegisterResponse>('/auth/register', await encryptRegisterRequest(data))
}

export function logout() {
  return request.post('/auth/logout')
}

async function encryptLoginRequest(data: LoginRequest): Promise<EncryptedLoginRequest> {
  return {
    username: data.username,
    encryptedPassword: await encryptPassword(data.password),
  }
}

async function encryptRegisterRequest(data: RegisterRequest): Promise<EncryptedRegisterRequest> {
  return {
    username: data.username,
    name: data.name,
    gender: data.gender,
    encryptedPassword: await encryptPassword(data.password),
    birthday: data.birthday,
    email: data.email,
    phone: data.phone,
  }
}

/**
 * 使用服务端公钥加密密码，避免登录/注册请求体中出现明文密码。
 */
async function encryptPassword(password: string): Promise<string> {
  if (!globalThis.crypto?.subtle) {
    throw new Error('当前浏览器不支持安全密码加密，请更换现代浏览器后重试')
  }
  const publicKey = await getPasswordPublicKey()
  const ciphertext = await crypto.subtle.encrypt(
    { name: 'RSA-OAEP' },
    publicKey,
    new TextEncoder().encode(password),
  )
  return arrayBufferToBase64(ciphertext)
}

async function getPasswordPublicKey(): Promise<CryptoKey> {
  if (!passwordPublicKeyPromise) {
    passwordPublicKeyPromise = request
      .get<PasswordPublicKeyResponse>('/auth/password-public-key')
      .then(({ data }) => {
        if (data.algorithm !== 'RSA-OAEP-256') {
          throw new Error('服务端密码加密算法不受支持')
        }
        return crypto.subtle.importKey(
          'spki',
          base64ToArrayBuffer(data.publicKey),
          { name: 'RSA-OAEP', hash: 'SHA-256' },
          false,
          ['encrypt'],
        )
      })
      .catch((error) => {
        passwordPublicKeyPromise = null
        throw error
      })
  }
  return passwordPublicKeyPromise
}

function base64ToArrayBuffer(value: string): ArrayBuffer {
  const binary = atob(value)
  const bytes = new Uint8Array(binary.length)
  for (let index = 0; index < binary.length; index += 1) {
    bytes[index] = binary.charCodeAt(index)
  }
  return bytes.buffer
}

function arrayBufferToBase64(value: ArrayBuffer): string {
  const bytes = new Uint8Array(value)
  let binary = ''
  for (const byte of bytes) {
    binary += String.fromCharCode(byte)
  }
  return btoa(binary)
}
