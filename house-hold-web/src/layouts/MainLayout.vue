<template>
  <div class="main-layout">
    <header class="header">
      <span class="logo">家庭资产</span>
      <nav class="nav">
        <router-link to="/dashboard">概览</router-link>
        <router-link to="/profile">个人信息</router-link>
        <router-link to="/family">家庭</router-link>
        <router-link to="/wealth/accounts">账户管理</router-link>
        <router-link to="/wealth/history">资产趋势</router-link>
      </nav>
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
.nav {
  display: flex;
  gap: 1rem;
  margin-left: 1.5rem;
}
.nav a {
  color: #555;
  text-decoration: none;
  font-size: 0.9rem;
}
.nav a:hover,
.nav a.router-link-active {
  color: #333;
  font-weight: 500;
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
