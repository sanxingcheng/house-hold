import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { login as apiLogin, register as apiRegister, logout as apiLogout } from '@/api/auth'
import type { RegisterRequest } from '@/types/auth'
import router from '@/router'

const TOKEN_KEY = 'household_token'
const USER_KEY = 'household_user'

export interface UserInfo {
  id: string
  username: string
  birthday?: string
  familyId?: string | null
}

export const useAuthStore = defineStore('auth', () => {
  const token = ref<string | null>(null)
  const user = ref<UserInfo | null>(null)

  const isLoggedIn = computed(() => !!token.value)

  function setAuth(t: string, u: UserInfo) {
    token.value = t
    user.value = u
    try {
      localStorage.setItem(TOKEN_KEY, t)
      localStorage.setItem(USER_KEY, JSON.stringify(u))
    } catch (_) {}
  }

  function loadFromStorage() {
    try {
      const t = localStorage.getItem(TOKEN_KEY)
      const u = localStorage.getItem(USER_KEY)
      if (t && u) {
        token.value = t
        user.value = JSON.parse(u) as UserInfo
      }
    } catch (_) {}
  }

  async function logout() {
    try { await apiLogout() } catch (_) {}
    token.value = null
    user.value = null
    try {
      localStorage.removeItem(TOKEN_KEY)
      localStorage.removeItem(USER_KEY)
    } catch (_) {}
    router.push('/login')
  }

  async function login(username: string, password: string) {
    const { data } = await apiLogin({ username, password })
    setAuth(data.token, data.user)
    return data
  }

  async function register(payload: RegisterRequest) {
    const { data } = await apiRegister(payload)
    return data
  }

  function setFamilyId(familyId: string | null) {
    if (user.value) {
      user.value = { ...user.value, familyId }
      try {
        localStorage.setItem(USER_KEY, JSON.stringify(user.value))
      } catch (_) {}
    }
  }

  return {
    token,
    user,
    isLoggedIn,
    setAuth,
    setFamilyId,
    loadFromStorage,
    logout,
    login,
    register,
  }
})
