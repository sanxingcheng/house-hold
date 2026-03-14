<template>
  <GuestLayout>
    <h2 style="text-align:center;margin:0 0 24px;font-size:1.4rem;color:#303133">注册</h2>
    <el-alert v-if="error" :title="error" type="error" :closable="false" show-icon style="margin-bottom:16px" />
    <el-form ref="formRef" :model="form" :rules="rules" label-position="top" @submit.prevent="onSubmit">
      <el-form-item label="用户名" prop="username">
        <el-input v-model="form.username" placeholder="4-32 个字符" :prefix-icon="User" />
      </el-form-item>
      <el-form-item label="姓名" prop="name">
        <el-input v-model="form.name" placeholder="请输入姓名" :prefix-icon="User" />
      </el-form-item>
      <el-form-item label="性别" prop="gender">
        <el-radio-group v-model="form.gender">
          <el-radio value="MALE">男</el-radio>
          <el-radio value="FEMALE">女</el-radio>
        </el-radio-group>
      </el-form-item>
      <el-form-item label="密码" prop="password">
        <el-input v-model="form.password" type="password" show-password placeholder="至少 6 位" :prefix-icon="Lock" />
      </el-form-item>
      <el-form-item label="确认密码" prop="confirmPassword">
        <el-input v-model="form.confirmPassword" type="password" show-password placeholder="请再次输入密码" :prefix-icon="Lock" />
      </el-form-item>
      <el-form-item label="生日" prop="birthday">
        <el-date-picker v-model="form.birthday" type="date" placeholder="选择日期" value-format="YYYY-MM-DD" style="width:100%" />
      </el-form-item>
      <el-form-item label="邮箱（选填）">
        <el-input v-model="form.email" placeholder="选填" />
      </el-form-item>
      <el-form-item label="手机（选填）">
        <el-input v-model="form.phone" placeholder="选填" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" native-type="submit" :loading="loading" style="width:100%">注册</el-button>
      </el-form-item>
    </el-form>
    <div style="text-align:center;font-size:0.9rem;color:#909399">
      已有账号？<router-link to="/login" style="color:var(--el-color-primary)">去登录</router-link>
    </div>
  </GuestLayout>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import type { FormInstance, FormRules } from 'element-plus'
import { ElMessage } from 'element-plus'
import { User, Lock } from '@element-plus/icons-vue'
import GuestLayout from '@/layouts/GuestLayout.vue'
import { useAuthStore } from '@/stores/auth'

const authStore = useAuthStore()
const router = useRouter()
const formRef = ref<FormInstance>()
const loading = ref(false)
const error = ref('')

const form = reactive({
  username: '',
  name: '',
  gender: '',
  password: '',
  confirmPassword: '',
  birthday: '',
  email: '',
  phone: '',
})

const validateConfirm = (_rule: unknown, value: string, callback: (err?: Error) => void) => {
  if (value !== form.password) {
    callback(new Error('两次输入的密码不一致'))
  } else {
    callback()
  }
}

const rules: FormRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 4, max: 32, message: '用户名长度为 4-32 个字符', trigger: 'blur' },
  ],
  name: [
    { required: true, message: '请输入姓名', trigger: 'blur' },
  ],
  gender: [
    { required: true, message: '请选择性别', trigger: 'change' },
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码至少 6 位', trigger: 'blur' },
  ],
  confirmPassword: [
    { required: true, message: '请确认密码', trigger: 'blur' },
    { validator: validateConfirm, trigger: 'blur' },
  ],
  birthday: [
    { required: true, message: '请选择生日', trigger: 'change' },
  ],
}

async function onSubmit() {
  if (!formRef.value) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  error.value = ''
  loading.value = true
  try {
    await authStore.register({
      username: form.username.trim(),
      name: form.name.trim(),
      gender: form.gender,
      password: form.password,
      birthday: form.birthday,
      email: form.email.trim() || undefined,
      phone: form.phone.trim() || undefined,
    })
    ElMessage.success('注册成功，正在登录...')
    await authStore.login(form.username.trim(), form.password)
    router.push('/dashboard')
  } catch (e: unknown) {
    const err = e as { message?: string }
    error.value = err?.message || '注册失败'
  } finally {
    loading.value = false
  }
}
</script>
