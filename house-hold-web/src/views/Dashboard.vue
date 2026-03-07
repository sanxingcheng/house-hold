<template>
  <MainLayout>
    <div class="dashboard" v-loading="loading">
      <h1 style="margin:0 0 24px">欢迎，{{ user?.username }}</h1>

      <el-row :gutter="20">
        <el-col :xs="24" :sm="12">
          <el-card shadow="hover">
            <template #header>
              <div class="card-header">
                <el-icon :size="18" color="var(--el-color-primary)"><Wallet /></el-icon>
                <span>我的资产</span>
              </div>
            </template>
            <el-statistic :value="userNetYuan" :precision="2" prefix="¥"
              :value-style="{ color: userNet >= 0 ? '#409eff' : '#f56c6c', fontSize: '1.8rem' }" />
            <div v-if="userSummary" class="card-detail">
              资产 ¥{{ formatMoney(userSummary.totalAssets) }} · 负债 ¥{{ formatMoney(userSummary.totalLiabilities) }}
            </div>
          </el-card>
        </el-col>

        <el-col :xs="24" :sm="12">
          <el-card shadow="hover" :class="{ 'card-disabled': !hasFamilyId }">
            <template #header>
              <div class="card-header">
                <el-icon :size="18" color="var(--el-color-success)"><House /></el-icon>
                <span>家庭资产</span>
              </div>
            </template>
            <template v-if="hasFamilyId">
              <el-statistic :value="familyNetYuan" :precision="2" prefix="¥"
                :value-style="{ color: familyNet >= 0 ? '#409eff' : '#f56c6c', fontSize: '1.8rem' }" />
              <div v-if="familySummary" class="card-detail">
                资产 ¥{{ formatMoney(familySummary.totalAssets) }} · 负债 ¥{{ formatMoney(familySummary.totalLiabilities) }}
              </div>
            </template>
            <template v-else>
              <el-empty description="尚未加入家庭" :image-size="60" />
            </template>
          </el-card>
        </el-col>
      </el-row>
    </div>
  </MainLayout>
</template>

<script setup lang="ts">
import { computed, ref, onMounted } from 'vue'
import { storeToRefs } from 'pinia'
import MainLayout from '@/layouts/MainLayout.vue'
import { useAuthStore } from '@/stores/auth'
import { useWealthStore } from '@/stores/wealth'
import { Wallet, House } from '@element-plus/icons-vue'

const authStore = useAuthStore()
const wealthStore = useWealthStore()
const { user } = storeToRefs(authStore)
const { userSummary, familySummary } = storeToRefs(wealthStore)
const loading = ref(false)

const hasFamilyId = computed(() => !!user.value?.familyId)
const userNet = computed(() => userSummary.value?.netWorth ?? 0)
const familyNet = computed(() => familySummary.value?.netWorth ?? 0)
const userNetYuan = computed(() => userNet.value / 100)
const familyNetYuan = computed(() => familyNet.value / 100)

function formatMoney(fen: number) {
  return (fen / 100).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

onMounted(async () => {
  loading.value = true
  try {
    await wealthStore.fetchUserSummary()
    if (hasFamilyId.value) {
      await wealthStore.fetchFamilySummary()
    }
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.dashboard {
  max-width: 900px;
  margin: 0 auto;
}
.card-header {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 600;
}
.card-detail {
  margin-top: 12px;
  font-size: 0.85rem;
  color: #909399;
}
.card-disabled {
  opacity: 0.55;
}
</style>
