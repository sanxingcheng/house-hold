<template>
  <MainLayout>
    <div class="dashboard">
      <h1>欢迎，{{ user?.username }}</h1>
      <p class="hint">这里是概览页，迭代二将展示个人与家庭资产卡片。</p>
    </div>
  </MainLayout>
</template>

<script setup lang="ts">
import { storeToRefs } from 'pinia'
import MainLayout from '@/layouts/MainLayout.vue'
import { useAuthStore } from '@/stores/auth'

const { user } = storeToRefs(useAuthStore())
</script>

<style scoped>
.dashboard {
  max-width: 800px;
  margin: 0 auto;
}
.hint {
  color: #666;
  margin-top: 0.5rem;
}
</style>
