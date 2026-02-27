<template>
  <div class="main-layout">
    <header class="header">
      <span class="logo">家庭资产</span>
      <span class="user">{{ user?.username }}</span>
      <button type="button" class="btn-logout" @click="handleLogout">退出</button>
    </header>
    <main class="main">
      <slot />
    </main>
  </div>
</template>

<script setup lang="ts">
import { storeToRefs } from 'pinia'
import { useAuthStore } from '@/stores/auth'
import { useRouter } from 'vue-router'

const authStore = useAuthStore()
const { user } = storeToRefs(authStore)
const router = useRouter()

function handleLogout() {
  authStore.logout()
  router.push('/login')
}
</script>

<style scoped>
.main-layout {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background: #f5f5f5;
}
.header {
  display: flex;
  align-items: center;
  gap: 1rem;
  padding: 1rem 2rem;
  background: #fff;
  box-shadow: 0 1px 2px rgba(0,0,0,0.05);
}
.logo {
  font-size: 1.25rem;
  font-weight: 600;
  color: #333;
}
.user {
  margin-left: auto;
  color: #666;
}
.btn-logout {
  padding: 0.35rem 0.75rem;
  border: 1px solid #ddd;
  border-radius: 4px;
  background: #fff;
  cursor: pointer;
  font-size: 0.875rem;
}
.btn-logout:hover {
  background: #f5f5f5;
}
.main {
  flex: 1;
  padding: 2rem;
}
</style>
