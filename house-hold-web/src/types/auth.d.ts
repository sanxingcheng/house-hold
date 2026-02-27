export interface LoginRequest {
  username: string
  password: string
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
  password: string
  birthday: string
  email?: string
  phone?: string
}

export interface RegisterResponse {
  id: string
  username: string
  birthday?: string
  email?: string
  phone?: string
}

export interface ApiErrorBody {
  code: string
  message: string
}
