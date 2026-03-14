<template>
  <el-container class="main-layout">
    <el-header class="header">
      <div class="header-left">
        <el-icon :size="24" class="logo-icon"><HomeFilled /></el-icon>
        <span class="logo-text">家庭资产</span>
      </div>

      <el-menu
        :default-active="activeRoute"
        mode="horizontal"
        :ellipsis="false"
        router
        class="nav-menu"
      >
        <el-menu-item index="/dashboard">
          <el-icon><DataLine /></el-icon>概览
        </el-menu-item>
        <el-sub-menu index="/family">
          <template #title>
            <el-icon><House /></el-icon>家庭
          </template>
          <el-menu-item index="/family">家庭管理</el-menu-item>
          <el-menu-item index="/family/assets">共有资产</el-menu-item>
          <el-menu-item index="/family/accounts">成员账户</el-menu-item>
        </el-sub-menu>
        <el-sub-menu index="/wealth">
          <template #title>
            <el-icon><Wallet /></el-icon>财富
          </template>
          <el-menu-item index="/wealth/accounts">账户管理</el-menu-item>
          <el-menu-item index="/wealth/history">资产趋势</el-menu-item>
        </el-sub-menu>
      </el-menu>

      <div class="header-right">
        <el-dropdown trigger="click" @command="handleCommand">
          <span class="user-dropdown">
            <el-avatar :size="32" :icon="UserFilled" />
            <span class="username">{{ user?.username }}</span>
            <el-icon><ArrowDown /></el-icon>
          </span>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="profile">
                <el-icon><User /></el-icon>个人信息
              </el-dropdown-item>
              <el-dropdown-item divided command="logout">
                <el-icon><SwitchButton /></el-icon>退出登录
              </el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
    </el-header>

    <el-main class="main-content">
      <slot />
    </el-main>
  </el-container>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { storeToRefs } from 'pinia'
import { useAuthStore } from '@/stores/auth'
import {
  HomeFilled, DataLine, User, UserFilled,
  House, Wallet, ArrowDown, SwitchButton
} from '@element-plus/icons-vue'

const authStore = useAuthStore()
const { user } = storeToRefs(authStore)
const route = useRoute()
const router = useRouter()

const activeRoute = computed(() => route.path)

function handleCommand(command: string) {
  if (command === 'profile') {
    router.push('/profile')
  } else if (command === 'logout') {
    authStore.logout()
  }
}
</script>

<style scoped>
.main-layout {
  min-height: 100vh;
}
.header {
  display: flex;
  align-items: center;
  background: #fff;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.06);
  padding: 0 24px;
  height: 60px;
  z-index: 10;
}
.header-left {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-right: 24px;
  flex-shrink: 0;
}
.logo-icon {
  color: var(--el-color-primary);
}
.logo-text {
  font-size: 1.15rem;
  font-weight: 600;
  color: #303133;
}
.nav-menu {
  flex: 1;
  border-bottom: none !important;
}
.header-right {
  flex-shrink: 0;
  margin-left: 16px;
}
.user-dropdown {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  outline: none;
}
.username {
  font-size: 0.9rem;
  color: #606266;
}
.main-content {
  background: #f5f7fa;
}
</style>
