export interface LoginRequest {
  username: string
  password: string
}

export interface EncryptedLoginRequest {
  username: string
  encryptedPassword: string
}

export interface LoginResponse {
  token: string
  user: {
    id: string
    username: string
    birthday?: string
  }
}

export interface RegisterRequest {
  username: string
  name: string
  gender: string
  password: string
  birthday: string
  email?: string
  phone?: string
}

export interface EncryptedRegisterRequest {
  username: string
  name: string
  gender: string
  encryptedPassword: string
  birthday: string
  email?: string
  phone?: string
}

export interface RegisterResponse {
  id: string
  username: string
  name?: string
  gender?: string
  birthday?: string
  email?: string
  phone?: string
}

export interface ApiErrorBody {
  code: string
  message: string
}

export interface PasswordPublicKeyResponse {
  keyId: string
  algorithm: 'RSA-OAEP-256'
  publicKey: string
}
