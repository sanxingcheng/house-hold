<template>
  <MainLayout>
    <div class="dashboard">
      <h1>欢迎，{{ user?.username }}</h1>

      <div class="wealth-cards">
        <div class="wealth-card">
          <div class="card-label">我的资产</div>
          <div class="card-amount" :class="userNet >= 0 ? 'positive' : 'negative'">
            ¥ {{ formatMoney(userNet) }}
          </div>
          <div v-if="userSummary" class="card-detail">
            资产 ¥{{ formatMoney(userSummary.totalAssets) }} · 负债 ¥{{ formatMoney(userSummary.totalLiabilities) }}
          </div>
        </div>
        <div class="wealth-card" :class="{ disabled: !hasFamilyId }">
          <div class="card-label">家庭资产</div>
          <template v-if="hasFamilyId">
            <div class="card-amount" :class="familyNet >= 0 ? 'positive' : 'negative'">
              ¥ {{ formatMoney(familyNet) }}
            </div>
            <div v-if="familySummary" class="card-detail">
              资产 ¥{{ formatMoney(familySummary.totalAssets) }} · 负债 ¥{{ formatMoney(familySummary.totalLiabilities) }}
            </div>
          </template>
          <template v-else>
            <div class="card-hint">尚未加入家庭</div>
          </template>
        </div>
      </div>
    </div>
  </MainLayout>
</template>

<script setup lang="ts">
import { computed, onMounted } from 'vue'
import { storeToRefs } from 'pinia'
import MainLayout from '@/layouts/MainLayout.vue'
import { useAuthStore } from '@/stores/auth'
import { useWealthStore } from '@/stores/wealth'

const authStore = useAuthStore()
const wealthStore = useWealthStore()
const { user } = storeToRefs(authStore)
const { userSummary, familySummary } = storeToRefs(wealthStore)

const hasFamilyId = computed(() => !!user.value?.familyId)
const userNet = computed(() => userSummary.value?.netWorth ?? 0)
const familyNet = computed(() => familySummary.value?.netWorth ?? 0)

function formatMoney(fen: number) {
  return (fen / 100).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

onMounted(async () => {
  await wealthStore.fetchUserSummary()
  if (hasFamilyId.value) {
    await wealthStore.fetchFamilySummary()
  }
})
</script>

<style scoped>
.dashboard {
  max-width: 800px;
  margin: 0 auto;
}
.wealth-cards {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 1.25rem;
  margin-top: 1.5rem;
}
.wealth-card {
  background: #fff;
  border-radius: 10px;
  padding: 1.5rem;
  box-shadow: 0 1px 3px rgba(0,0,0,0.08);
}
.wealth-card.disabled {
  opacity: 0.5;
}
.card-label {
  font-size: 0.9rem;
  color: #888;
  margin-bottom: 0.5rem;
}
.card-amount {
  font-size: 1.75rem;
  font-weight: 700;
}
.positive { color: #2563eb; }
.negative { color: #dc2626; }
.card-detail {
  margin-top: 0.5rem;
  font-size: 0.8rem;
  color: #999;
}
.card-hint {
  color: #bbb;
  font-size: 0.9rem;
  margin-top: 0.5rem;
}
</style>
