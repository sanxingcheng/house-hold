<template>
  <MainLayout>
    <div class="profile">
      <h1>个人信息</h1>
      <form v-if="!saving" @submit.prevent="onSubmit" class="form">
        <div class="field">
          <label>用户名</label>
          <input type="text" :value="profile?.username" disabled class="readonly" />
        </div>
        <div class="field">
          <label>姓名</label>
          <input v-model="form.name" type="text" placeholder="选填" />
        </div>
        <div class="field">
          <label>生日</label>
          <input v-model="form.birthday" type="date" />
        </div>
        <div class="field">
          <label>邮箱</label>
          <input v-model="form.email" type="email" placeholder="选填" />
        </div>
        <div class="field">
          <label>手机</label>
          <input v-model="form.phone" type="tel" placeholder="选填" />
        </div>
        <div class="field" v-if="profile?.familyId">
          <label>所属家庭</label>
          <input type="text" :value="familyName" disabled class="readonly" />
        </div>
        <p v-if="message" class="message" :class="{ error: isError }">{{ message }}</p>
        <button type="submit" class="btn">保存</button>
      </form>
      <p v-else class="hint">保存中…</p>
    </div>
  </MainLayout>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, computed } from 'vue'
import MainLayout from '@/layouts/MainLayout.vue'
import { getProfile, updateProfile } from '@/api/user'
import { useAuthStore } from '@/stores/auth'
import { useFamilyStore } from '@/stores/family'
import type { UserProfile, UserProfileUpdateRequest } from '@/types/user'

const authStore = useAuthStore()
const familyStore = useFamilyStore()
const profile = ref<UserProfile | null>(null)
const saving = ref(false)
const message = ref('')
const isError = ref(false)

const form = reactive<UserProfileUpdateRequest>({
  name: '',
  birthday: '',
  email: '',
  phone: '',
})

const familyName = computed(() => familyStore.family?.nameAlias ?? '—')

async function load() {
  try {
    const { data } = await getProfile()
    profile.value = data
    form.name = data.name ?? ''
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
  }
}

async function onSubmit() {
  message.value = ''
  isError.value = false
  saving.value = true
  try {
    const { data } = await updateProfile(form)
    profile.value = data
    authStore.setAuth(authStore.token!, { ...authStore.user!, ...data })
    message.value = '保存成功'
  } catch (e: unknown) {
    isError.value = true
    message.value = (e as { message?: string })?.message ?? '保存失败'
  } finally {
    saving.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.profile { max-width: 480px; margin: 0 auto; }
.form { display: flex; flex-direction: column; gap: 1rem; }
.field { display: flex; flex-direction: column; gap: 0.25rem; }
.field label { font-weight: 500; color: #333; }
.field input { padding: 0.5rem; border: 1px solid #ddd; border-radius: 4px; }
.field input.readonly { background: #f5f5f5; color: #666; }
.message { margin: 0.5rem 0; font-size: 0.9rem; }
.message.error { color: #c00; }
.btn { padding: 0.5rem 1rem; align-self: flex-start; cursor: pointer; border-radius: 4px; background: #333; color: #fff; border: none; }
.btn:hover { background: #555; }
.hint { color: #666; }
</style>
