<template>
  <div class="guest-layout">
    <div class="guest-container">
      <div class="brand">
        <el-icon :size="40"><HomeFilled /></el-icon>
        <h1 class="brand-title">家庭资产</h1>
        <p class="brand-subtitle">安全、便捷的家庭财富管理平台</p>
      </div>
      <el-card class="guest-card" shadow="always">
        <slot />
      </el-card>
    </div>
  </div>
</template>

<script setup lang="ts">
import { HomeFilled } from '@element-plus/icons-vue'
</script>

<style scoped>
.guest-layout {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}
.guest-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 1.5rem;
  padding: 2rem;
}
.brand {
  text-align: center;
  color: #fff;
}
.brand-title {
  margin: 0.5rem 0 0.25rem;
  font-size: 2rem;
  font-weight: 700;
}
.brand-subtitle {
  margin: 0;
  font-size: 0.95rem;
  opacity: 0.85;
}
.guest-card {
  width: 400px;
  max-width: 90vw;
}
</style>
