<template>
  <GuestLayout>
    <h2 style="text-align:center;margin:0 0 24px;font-size:1.4rem;color:#303133">登录</h2>
    <el-alert v-if="error" :title="error" type="error" :closable="false" show-icon style="margin-bottom:16px" />
    <el-form ref="formRef" :model="form" :rules="rules" label-position="top" @submit.prevent="onSubmit">
      <el-form-item label="用户名" prop="username">
        <el-input v-model="form.username" placeholder="请输入用户名" :prefix-icon="User" />
      </el-form-item>
      <el-form-item label="密码" prop="password">
        <el-input v-model="form.password" type="password" show-password placeholder="请输入密码" :prefix-icon="Lock" />
      </el-form-item>
      <el-form-item>
        <el-checkbox v-model="form.remember">记住我</el-checkbox>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" native-type="submit" :loading="loading" style="width:100%">登录</el-button>
      </el-form-item>
    </el-form>
    <div style="text-align:center;font-size:0.9rem;color:#909399">
      还没有账号？<router-link to="/register" style="color:var(--el-color-primary)">去注册</router-link>
    </div>
  </GuestLayout>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import type { FormInstance, FormRules } from 'element-plus'
import { User, Lock } from '@element-plus/icons-vue'
import GuestLayout from '@/layouts/GuestLayout.vue'
import { useAuthStore } from '@/stores/auth'

const authStore = useAuthStore()
const router = useRouter()
const formRef = ref<FormInstance>()
const loading = ref(false)
const error = ref('')

const REMEMBER_KEY = 'household_remember_username'

const form = reactive({
  username: '',
  password: '',
  remember: false,
})

const rules: FormRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
}

onMounted(() => {
  try {
    const saved = localStorage.getItem(REMEMBER_KEY)
    if (saved) form.username = saved
  } catch (_) {}
})

async function onSubmit() {
  if (!formRef.value) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  error.value = ''
  loading.value = true
  try {
    await authStore.login(form.username.trim(), form.password)
    if (form.remember) {
      try { localStorage.setItem(REMEMBER_KEY, form.username.trim()) } catch (_) {}
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
