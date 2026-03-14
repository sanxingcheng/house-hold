<template>
  <MainLayout>
    <div class="dashboard" v-loading="loading">
      <h1 class="page-title">欢迎，{{ user?.username }}</h1>

      <el-row class="summary-row" :gutter="gutter">
        <el-col :xs="24" :sm="12" :lg="12">
          <el-card shadow="hover" class="summary-card">
            <template #header>
              <div class="card-header">
                <el-icon :size="18" color="var(--el-color-primary)"><Wallet /></el-icon>
                <span>我的资产</span>
              </div>
            </template>
            <el-statistic
              :value="userNetYuan"
              :precision="2"
              prefix="¥"
              :value-style="statStyle(userNet)"
            />
            <div v-if="userSummary" class="card-detail">
              资产 ¥{{ formatMoney(userSummary.totalAssets) }} · 负债 ¥{{ formatMoney(userSummary.totalLiabilities) }}
            </div>
          </el-card>
        </el-col>

        <el-col :xs="24" :sm="12" :lg="12">
          <el-card shadow="hover" class="summary-card" :class="{ 'card-disabled': !hasFamilyId }">
            <template #header>
              <div class="card-header">
                <el-icon :size="18" color="var(--el-color-success)"><House /></el-icon>
                <span>家庭资产</span>
              </div>
            </template>
            <template v-if="hasFamilyId">
              <el-statistic
                :value="familyNetYuan"
                :precision="2"
                prefix="¥"
                :value-style="statStyle(familyNet)"
              />
              <div v-if="familySummary" class="card-detail">
                <span>成员账户 ¥{{ formatMoney(memberAccountTotal) }}</span>
                <span v-if="familySharedTotal > 0"> · 共有资产 ¥{{ formatMoney(familySharedTotal) }}</span>
                <span> · 负债 ¥{{ formatMoney(familySummary.totalLiabilities) }}</span>
              </div>
            </template>
            <template v-else>
              <el-empty description="尚未加入家庭" :image-size="60" />
            </template>
          </el-card>
        </el-col>
      </el-row>

      <el-row v-if="hasFamilyId" class="detail-row" :gutter="gutter">
        <el-col :xs="24" :sm="24" :md="14" :lg="14" :xl="14">
          <el-card shadow="never" class="table-card">
            <template #header>
              <div class="card-header">
                <span>家庭成员资产概览</span>
              </div>
            </template>
            <el-empty v-if="memberRows.length === 0" description="暂无成员资产数据" :image-size="48" />
            <div v-else class="table-wrap">
              <el-table :data="memberRows" size="small" class="responsive-table">
                <el-table-column prop="name" label="成员" min-width="80" />
                <el-table-column prop="assetYuan" label="资产" min-width="90" align="right">
                  <template #default="{ row }">¥ {{ row.assetYuan }}</template>
                </el-table-column>
                <el-table-column prop="liabilityYuan" label="负债" min-width="90" align="right">
                  <template #default="{ row }">¥ {{ row.liabilityYuan }}</template>
                </el-table-column>
                <el-table-column prop="netYuan" label="净资产" min-width="90" align="right">
                  <template #default="{ row }">
                    <span :style="{ color: row.net >= 0 ? '#409eff' : '#f56c6c' }">
                      ¥ {{ row.netYuan }}
                    </span>
                  </template>
                </el-table-column>
              </el-table>
            </div>
          </el-card>
        </el-col>

        <el-col :xs="24" :sm="24" :md="10" :lg="10" :xl="10">
          <el-card shadow="never" class="table-card">
            <template #header>
              <div class="card-header">
                <span>家庭共有资产概览</span>
              </div>
            </template>
            <el-empty v-if="familyAssets.length === 0" description="暂无共有资产" :image-size="48" />
            <div v-else class="table-wrap">
              <el-table :data="familyAssets.slice(0, 10)" size="small" class="responsive-table">
                <el-table-column prop="assetName" label="资产" min-width="80" />
                <el-table-column label="类型" min-width="70">
                  <template #default="{ row }">
                    <el-tag size="small">{{ typeLabel(row.assetType) }}</el-tag>
                  </template>
                </el-table-column>
                <el-table-column label="金额" min-width="90" align="right">
                  <template #default="{ row }">
                    <span class="amount-cell">¥ {{ formatMoney(row.amount) }}</span>
                  </template>
                </el-table-column>
                <el-table-column label="贷款余额" min-width="85" align="right">
                  <template #default="{ row }">
                    <span v-if="row.loanRemaining && row.loanRemaining > 0" class="loan-cell">
                      ¥ {{ formatMoney(row.loanRemaining) }}
                    </span>
                    <span v-else>—</span>
                  </template>
                </el-table-column>
              </el-table>
            </div>
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
import { useFamilyStore } from '@/stores/family'
import type { Account } from '@/types/wealth'
import { Wallet, House } from '@element-plus/icons-vue'

const authStore = useAuthStore()
const wealthStore = useWealthStore()
const familyStore = useFamilyStore()
const { user } = storeToRefs(authStore)
const { userSummary, familySummary, familyAccounts, familyAssets } = storeToRefs(wealthStore)
const loading = ref(false)

const hasFamilyId = computed(() => !!user.value?.familyId)
const userNet = computed(() => userSummary.value?.netWorth ?? 0)
const familyNet = computed(() => familySummary.value?.netWorth ?? 0)
const userNetYuan = computed(() => userNet.value / 100)
const familyNetYuan = computed(() => familyNet.value / 100)
const familySharedTotal = computed(() => familySummary.value?.familyAssetTotal ?? 0)
const memberAccountTotal = computed(() =>
  (familySummary.value?.totalAssets ?? 0) - familySharedTotal.value
)

// 成员维度资产：从 familyAccounts 和 family.members 计算
const memberRows = computed(() => {
  if (!hasFamilyId.value || familyAccounts.value.length === 0 || familyStore.members.length === 0) return []

  const groups = new Map<string, { userId: string; name: string; asset: number; liability: number }>()
  const members = familyStore.members as Array<{ userId: string; name?: string; username: string }>

  const findName = (uid: string) => {
    const m = members.find(mb => String(mb.userId) === uid)
    if (!m) return `用户 ${uid}`
    return m.name || m.username || `用户 ${uid}`
  }

  familyAccounts.value.forEach((acc: Account) => {
    const uid = acc.userId
    if (!groups.has(uid)) {
      groups.set(uid, { userId: uid, name: findName(uid), asset: 0, liability: 0 })
    }
    const g = groups.get(uid)!
    if (acc.accountType === 'CREDIT_CARD') {
      g.liability += acc.balance
    } else {
      g.asset += acc.balance
    }
  })

  return Array.from(groups.values()).map(g => ({
    ...g,
    net: g.asset - g.liability,
    assetYuan: (g.asset / 100).toFixed(2),
    liabilityYuan: (g.liability / 100).toFixed(2),
    netYuan: ((g.asset - g.liability) / 100).toFixed(2),
  }))
})

function formatMoney(fen: number) {
  return (fen / 100).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

function statStyle(net: number) {
  return {
    color: net >= 0 ? '#409eff' : '#f56c6c',
    fontSize: 'clamp(1.25rem, 4vw, 1.8rem)',
  }
}

const gutter = 16

const TYPE_LABELS: Record<string, string> = {
  REAL_ESTATE: '房产', VEHICLE: '车辆', DEPOSIT: '存款', INVESTMENT: '投资', OTHER: '其他',
}
function typeLabel(t: string) { return TYPE_LABELS[t] || t }

onMounted(async () => {
  loading.value = true
  try {
    await wealthStore.fetchUserSummary()
    if (hasFamilyId.value && user.value?.familyId) {
      await Promise.all([
        wealthStore.fetchFamilySummary(),
        familyStore.fetchFamily(user.value.familyId),
        wealthStore.fetchFamilyAccounts(),
        wealthStore.fetchFamilyAssets(),
      ])
    }
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.dashboard {
  width: 100%;
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 16px;
  box-sizing: border-box;
}

.page-title {
  margin: 0 0 24px;
  font-size: clamp(1.25rem, 3vw, 1.5rem);
  font-weight: 600;
}

.summary-row {
  margin-bottom: 0;
}

.summary-row .el-col {
  margin-bottom: 16px;
}

@media (min-width: 768px) {
  .summary-row .el-col {
    margin-bottom: 0;
  }
}

.summary-card {
  height: 100%;
  min-height: 120px;
}

.detail-row {
  margin-top: 20px;
}

.detail-row .el-col {
  margin-bottom: 16px;
}

@media (min-width: 992px) {
  .detail-row .el-col {
    margin-bottom: 0;
  }
}

.table-card {
  height: 100%;
}

.table-wrap {
  width: 100%;
  overflow-x: auto;
  -webkit-overflow-scrolling: touch;
}

.responsive-table {
  min-width: 280px;
}

.amount-cell {
  font-weight: 500;
  color: #409eff;
}

.loan-cell {
  color: #f56c6c;
}

.card-header {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 600;
  font-size: clamp(0.9rem, 2vw, 1rem);
}

.card-detail {
  margin-top: 12px;
  font-size: clamp(0.75rem, 1.5vw, 0.85rem);
  color: #909399;
  word-break: break-word;
}

.card-disabled {
  opacity: 0.55;
}

@media (max-width: 767px) {
  .dashboard {
    padding: 0 12px;
  }

  .page-title {
    margin-bottom: 16px;
  }

  .detail-row {
    margin-top: 16px;
  }
}
</style>
