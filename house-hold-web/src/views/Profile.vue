<template>
  <MainLayout>
    <div class="profile" v-loading="pageLoading">
      <el-card shadow="never">
        <template #header>
          <div class="card-header">
            <el-icon :size="20"><User /></el-icon>
            <span>个人信息</span>
          </div>
        </template>

        <el-form ref="formRef" :model="form" label-width="80px" label-position="left" style="max-width:480px">
          <el-form-item label="用户名">
            <el-input :model-value="profile?.username" disabled />
          </el-form-item>
          <el-form-item label="姓名">
            <el-input v-model="form.name" placeholder="选填" />
          </el-form-item>
          <el-form-item label="性别">
            <el-radio-group v-model="form.gender">
              <el-radio value="MALE">男</el-radio>
              <el-radio value="FEMALE">女</el-radio>
            </el-radio-group>
          </el-form-item>
          <el-form-item label="生日">
            <el-date-picker v-model="form.birthday" type="date" placeholder="选择日期" value-format="YYYY-MM-DD" style="width:100%" />
          </el-form-item>
          <el-form-item label="邮箱">
            <el-input v-model="form.email" placeholder="选填" />
          </el-form-item>
          <el-form-item label="手机">
            <el-input v-model="form.phone" placeholder="选填" />
          </el-form-item>
          <el-form-item v-if="profile?.familyId" label="所属家庭">
            <el-input :model-value="familyName" disabled />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" :loading="saving" @click="onSubmit">保存</el-button>
          </el-form-item>
        </el-form>
      </el-card>
    </div>
  </MainLayout>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { User } from '@element-plus/icons-vue'
import MainLayout from '@/layouts/MainLayout.vue'
import { getProfile, updateProfile } from '@/api/user'
import { useAuthStore } from '@/stores/auth'
import { useFamilyStore } from '@/stores/family'
import type { UserProfile, UserProfileUpdateRequest } from '@/types/user'

const authStore = useAuthStore()
const familyStore = useFamilyStore()
const profile = ref<UserProfile | null>(null)
const saving = ref(false)
const pageLoading = ref(false)

const form = reactive<UserProfileUpdateRequest>({
  name: '',
  gender: '',
  birthday: '',
  email: '',
  phone: '',
})

const familyName = computed(() => familyStore.family?.nameAlias ?? '—')

async function load() {
  pageLoading.value = true
  try {
    const { data } = await getProfile()
    profile.value = data
    form.name = data.name ?? ''
    form.gender = data.gender ?? ''
    form.birthday = data.birthday ?? ''
    form.email = data.email ?? ''
    form.phone = data.phone ?? ''
    if (data.familyId && !familyStore.family) {
      await familyStore.fetchFamily(data.familyId)
    }
  } catch (_) {
    profile.value = authStore.user ? {
      id: authStore.user.id,
      username: authStore.user.username,
      birthday: authStore.user.birthday ?? undefined,
      familyId: authStore.user.familyId ?? undefined,
    } : null
    if (profile.value) {
      form.birthday = profile.value.birthday ?? ''
    }
  } finally {
    pageLoading.value = false
  }
}

async function onSubmit() {
  saving.value = true
  try {
    const { data } = await updateProfile(form)
    profile.value = data
    authStore.setAuth(authStore.token!, { ...authStore.user!, ...data })
    ElMessage.success('保存成功')
  } catch (e: unknown) {
    ElMessage.error((e as { message?: string })?.message ?? '保存失败')
  } finally {
    saving.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.profile {
  max-width: 640px;
  margin: 0 auto;
}
.card-header {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 600;
}
</style>
