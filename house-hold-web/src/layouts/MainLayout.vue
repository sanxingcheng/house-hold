<template>
  <el-container class="main-layout">
    <el-header class="header">
      <div class="header-left">
        <el-icon :size="24" class="logo-icon"><HomeFilled /></el-icon>
        <span class="logo-text">家庭资产</span>
      </div>

      <!-- 桌面端菜单 -->
      <el-menu
        :default-active="activeRoute"
        mode="horizontal"
        :ellipsis="false"
        router
        class="nav-menu desktop-menu"
      >
        <el-menu-item index="/dashboard">
          <el-icon><DataLine /></el-icon>概览
        </el-menu-item>
        <el-sub-menu index="/family">
          <template #title>
            <el-icon><House /></el-icon>家庭
          </template>
          <el-menu-item index="/family/overview">我的家庭</el-menu-item>
          <el-menu-item index="/family">家庭管理</el-menu-item>
          <el-menu-item index="/family/assets">共有资产</el-menu-item>
          <el-menu-item index="/family/accounts">成员账户</el-menu-item>
          <el-menu-item index="/family/operation-log">操作日志</el-menu-item>
        </el-sub-menu>
        <el-sub-menu index="/wealth">
          <template #title>
            <el-icon><Wallet /></el-icon>财富
          </template>
          <el-menu-item index="/wealth/accounts">账户管理</el-menu-item>
          <el-menu-item index="/wealth/history">资产趋势</el-menu-item>
        </el-sub-menu>
      </el-menu>

      <!-- 移动端菜单按钮 -->
      <div class="mobile-menu-btn">
        <el-button
          :icon="Menu"
          circle
          @click="mobileMenuVisible = true"
        />
      </div>

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

    <SiteFooter />

    <!-- 移动端抽屉菜单 -->
    <el-drawer
      v-model="mobileMenuVisible"
      title="菜单"
      direction="ltr"
      size="200px"
      :with-header="false"
      class="mobile-drawer"
    >
      <div class="mobile-menu-header">
        <el-icon :size="24" class="logo-icon"><HomeFilled /></el-icon>
        <span class="logo-text">家庭资产</span>
      </div>
      <el-menu
        :default-active="activeRoute"
        router
        class="mobile-menu"
        @select="mobileMenuVisible = false"
      >
        <el-menu-item index="/dashboard">
          <el-icon><DataLine /></el-icon>概览
        </el-menu-item>
        <el-sub-menu index="/family">
          <template #title>
            <el-icon><House /></el-icon>家庭
          </template>
          <el-menu-item index="/family/overview">我的家庭</el-menu-item>
          <el-menu-item index="/family">家庭管理</el-menu-item>
          <el-menu-item index="/family/assets">共有资产</el-menu-item>
          <el-menu-item index="/family/accounts">成员账户</el-menu-item>
          <el-menu-item index="/family/operation-log">操作日志</el-menu-item>
        </el-sub-menu>
        <el-sub-menu index="/wealth">
          <template #title>
            <el-icon><Wallet /></el-icon>财富
          </template>
          <el-menu-item index="/wealth/accounts">账户管理</el-menu-item>
          <el-menu-item index="/wealth/history">资产趋势</el-menu-item>
        </el-sub-menu>
      </el-menu>
    </el-drawer>
  </el-container>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { storeToRefs } from 'pinia'
import { useAuthStore } from '@/stores/auth'
import SiteFooter from '@/layouts/SiteFooter.vue'
import {
  HomeFilled, DataLine, User, UserFilled,
  House, Wallet, ArrowDown, SwitchButton, Menu
} from '@element-plus/icons-vue'

const authStore = useAuthStore()
const { user } = storeToRefs(authStore)
const route = useRoute()
const router = useRouter()

const activeRoute = computed(() => route.path)
const mobileMenuVisible = ref(false)

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
  display: flex;
  flex-direction: column;
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
.desktop-menu {
  display: flex;
}
.mobile-menu-btn {
  display: none;
  flex: 1;
  justify-content: flex-end;
  margin-right: 16px;
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
  flex: 1;
  background: #f5f7fa;
  padding: 20px;
}

/* 移动端菜单样式 */
.mobile-drawer :deep(.el-drawer__body) {
  padding: 0;
}
.mobile-menu-header {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 16px 20px;
  border-bottom: 1px solid #e4e7ed;
}
.mobile-menu {
  border-right: none;
}

/* 响应式样式 */
@media screen and (max-width: 768px) {
  .header {
    padding: 0 16px;
  }
  .header-left {
    margin-right: 12px;
  }
  .logo-text {
    font-size: 1rem;
  }
  .desktop-menu {
    display: none;
  }
  .mobile-menu-btn {
    display: flex;
  }
  .username {
    display: none;
  }
  .main-content {
    padding: 12px;
  }
}

@media screen and (max-width: 480px) {
  .header {
    padding: 0 12px;
    height: 56px;
  }
  .header-left {
    margin-right: 8px;
  }
  .logo-text {
    font-size: 0.9rem;
  }
  .main-content {
    padding: 8px;
  }
}

/* 平板适配 */
@media screen and (min-width: 769px) and (max-width: 1024px) {
  .header {
    padding: 0 16px;
  }
  .main-content {
    padding: 16px;
  }
}
</style>
