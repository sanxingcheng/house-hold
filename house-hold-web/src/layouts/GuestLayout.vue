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
      <SiteFooter />
    </div>
  </div>
</template>

<script setup lang="ts">
import { HomeFilled } from '@element-plus/icons-vue'
import SiteFooter from '@/layouts/SiteFooter.vue'
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
  --site-footer-color: rgba(255, 255, 255, 0.82);
  --site-footer-hover-color: #fff;
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

@media screen and (max-width: 480px) {
  .guest-layout {
    align-items: flex-start;
  }
  .guest-container {
    min-height: 100vh;
    justify-content: center;
    gap: 1rem;
    padding: 1.25rem 1rem;
  }
}
</style>
