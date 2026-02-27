<template>
  <GuestLayout>
    <div class="card">
      <h1 class="title">注册</h1>
      <form class="form" @submit.prevent="onSubmit">
        <div v-if="error" class="error">{{ error }}</div>
        <div class="field">
          <label>用户名（4-32 字符）</label>
          <input v-model="username" type="text" required minlength="4" maxlength="32" placeholder="请输入用户名" />
        </div>
        <div class="field">
          <label>密码（至少 6 位）</label>
          <input v-model="password" type="password" required minlength="6" placeholder="请输入密码" />
        </div>
        <div class="field">
          <label>确认密码</label>
          <input v-model="confirmPassword" type="password" required placeholder="请再次输入密码" />
        </div>
        <div class="field">
          <label>生日</label>
          <input v-model="birthday" type="date" required />
        </div>
        <div class="field">
          <label>邮箱（选填）</label>
          <input v-model="email" type="email" placeholder="选填" />
        </div>
        <div class="field">
          <label>手机（选填）</label>
          <input v-model="phone" type="text" placeholder="选填" />
        </div>
        <button type="submit" class="btn" :disabled="loading">注册</button>
      </form>
      <p class="link">
        已有账号？<router-link to="/login">去登录</router-link>
      </p>
    </div>
  </GuestLayout>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import GuestLayout from '@/layouts/GuestLayout.vue'
import { useAuthStore } from '@/stores/auth'

const authStore = useAuthStore()
const router = useRouter()

const username = ref('')
const password = ref('')
const confirmPassword = ref('')
const birthday = ref('')
const email = ref('')
const phone = ref('')
const loading = ref(false)
const error = ref('')

async function onSubmit() {
  error.value = ''
  if (password.value !== confirmPassword.value) {
    error.value = '两次输入的密码不一致'
    return
  }
  if (username.value.length < 4 || username.value.length > 32) {
    error.value = '用户名长度为 4-32 个字符'
    return
  }
  if (password.value.length < 6) {
    error.value = '密码至少 6 位'
    return
  }
  if (!birthday.value) {
    error.value = '请选择生日'
    return
  }
  loading.value = true
  try {
    await authStore.register({
      username: username.value.trim(),
      password: password.value,
      birthday: birthday.value,
      email: email.value.trim() || undefined,
      phone: phone.value.trim() || undefined,
    })
    await authStore.login(username.value.trim(), password.value)
    router.push('/dashboard')
  } catch (e: unknown) {
    const err = e as { message?: string }
    error.value = err?.message || '注册失败'
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
.field input {
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
