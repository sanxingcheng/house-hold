import axios from 'axios'
import type { ApiErrorBody } from '@/types/auth'
import { useAuthStore } from '@/stores/auth'
import router from '@/router'

const baseURL = import.meta.env.VITE_API_BASE_URL ?? ''

export const request = axios.create({
  baseURL,
  timeout: 10000,
  headers: { 'Content-Type': 'application/json' },
})

// Track if we are already processing a 401 to avoid infinite redirect loops
let isHandling401 = false

request.interceptors.request.use((config) => {
  const authStore = useAuthStore()
  const token = authStore.token
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

request.interceptors.response.use(
  (res) => res,
  (err) => {
    if (err.response?.status === 401 && !isHandling401) {
      isHandling401 = true
      try {
        const authStore = useAuthStore()
        // Clear local state without calling /auth/logout (which would trigger another 401)
        authStore.token = null
        authStore.user = null
        try {
          localStorage.removeItem('household_token')
          localStorage.removeItem('household_user')
        } catch (_) {}
        router.push('/login')
      } finally {
        isHandling401 = false
      }
    }
    const data = err.response?.data as ApiErrorBody | undefined
    const message = data?.message || err.message || '请求失败'
    err.message = message
    return Promise.reject(err)
  }
)
