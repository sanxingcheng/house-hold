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
    if (err.response?.status === 401) {
      const authStore = useAuthStore()
      authStore.logout()
      router.push('/login')
    }
    const data = err.response?.data as ApiErrorBody | undefined
    const message = data?.message || err.message || '请求失败'
    err.message = message
    return Promise.reject(err)
  }
)
