<template>
  <GuestLayout>
    <div class="card">
      <h1 class="title">登录</h1>
      <form class="form" @submit.prevent="onSubmit">
        <div v-if="error" class="error">{{ error }}</div>
        <div class="field">
          <label>用户名</label>
          <input v-model="username" type="text" required placeholder="请输入用户名" />
        </div>
        <div class="field">
          <label>密码</label>
          <input v-model="password" type="password" required placeholder="请输入密码" />
        </div>
        <div class="field row">
          <input id="remember" v-model="remember" type="checkbox" />
          <label for="remember">记住我</label>
        </div>
        <button type="submit" class="btn" :disabled="loading">登录</button>
      </form>
      <p class="link">
        还没有账号？<router-link to="/register">去注册</router-link>
      </p>
    </div>
  </GuestLayout>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import GuestLayout from '@/layouts/GuestLayout.vue'
import { useAuthStore } from '@/stores/auth'

const authStore = useAuthStore()
const router = useRouter()

const username = ref('')
const password = ref('')
const remember = ref(false)
const loading = ref(false)
const error = ref('')

const REMEMBER_KEY = 'household_remember_username'

onMounted(() => {
  try {
    const saved = localStorage.getItem(REMEMBER_KEY)
    if (saved) username.value = saved
  } catch (_) {}
})

async function onSubmit() {
  error.value = ''
  if (!username.value.trim() || !password.value) {
    error.value = '请填写用户名和密码'
    return
  }
  loading.value = true
  try {
    await authStore.login(username.value.trim(), password.value)
    if (remember.value) {
      try { localStorage.setItem(REMEMBER_KEY, username.value.trim()) } catch (_) {}
    } else {
      try { localStorage.removeItem(REMEMBER_KEY) } catch (_) {}
    }
    const redirect = (router.currentRoute.value.query.redirect as string) || '/dashboard'
    router.push(redirect)
  } catch (e: unknown) {
    const err = e as { message?: string }
    error.value = err?.message || '登录失败'
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.card {
  width: 100%;
  max-width: 360px;
  padding: 2rem;
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.08);
}
.title {
  margin: 0 0 1.5rem;
  font-size: 1.5rem;
  text-align: center;
}
.form {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}
.error {
  padding: 0.5rem;
  background: #fee;
  color: #c00;
  border-radius: 4px;
  font-size: 0.875rem;
}
.field {
  display: flex;
  flex-direction: column;
  gap: 0.35rem;
}
.field label { font-size: 0.875rem; color: #555; }
.field.row {
  flex-direction: row;
  align-items: center;
}
.field.row label { margin-left: 0.5rem; }
.field input[type="text"],
.field input[type="password"] {
  padding: 0.5rem 0.75rem;
  border: 1px solid #ddd;
  border-radius: 4px;
  font-size: 1rem;
}
.btn {
  margin-top: 0.5rem;
  padding: 0.6rem 1rem;
  background: #198754;
  color: #fff;
  border: none;
  border-radius: 4px;
  font-size: 1rem;
  cursor: pointer;
}
.btn:hover:not(:disabled) { background: #157347; }
.btn:disabled { opacity: 0.7; cursor: not-allowed; }
.link {
  margin: 1.5rem 0 0;
  text-align: center;
  font-size: 0.875rem;
  color: #666;
}
.link a { color: #0d6efd; }
</style>
