import { request } from './request'
import type {
  LoginRequest,
  LoginResponse,
  RegisterRequest,
  RegisterResponse,
} from '@/types/auth'

export function login(data: LoginRequest) {
  return request.post<LoginResponse>('/auth/login', data)
}

export function register(data: RegisterRequest) {
  return request.post<RegisterResponse>('/auth/register', data)
}

export function logout() {
  return request.post('/auth/logout')
}
