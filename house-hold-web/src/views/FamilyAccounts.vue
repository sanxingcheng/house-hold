<template>
  <MainLayout>
    <div class="family-accounts" v-loading="pageLoading">
      <div class="page-header">
        <h1 class="page-title">家庭成员账户</h1>
        <el-tag v-if="!canManage" type="info" size="small">仅户主/管理员可查看</el-tag>
      </div>

      <el-empty
        v-if="!authStore.user?.familyId"
        description="请先加入或创建家庭"
      />

      <template v-else>
        <el-alert
          v-if="!canManage"
          type="warning"
          show-icon
          title="您不是该家庭的户主或管理员，无法查看所有成员账户。"
          style="margin-bottom:16px"
        />

        <template v-if="canManage">
          <div class="filter-row">
            <span class="filter-label">归属成员：</span>
            <el-select v-model="selectedMemberId" placeholder="全部" class="filter-select" clearable>
              <el-option label="全部" value="" />
              <el-option
                v-for="m in familyStore.members"
                :key="m.userId"
                :label="`${m.username}${m.name ? ' (' + m.name + ')' : ''}`"
                :value="m.userId"
              />
            </el-select>
          </div>

          <!-- 成员汇总 + 总汇总 -->
          <el-card v-if="memberSummaryRows.length > 0" shadow="never" class="summary-card">
            <template #header><span class="card-title">成员汇总</span></template>
            <el-table
              :data="memberSummaryRows"
              size="small"
              stripe
              class="responsive-table"
            >
              <el-table-column prop="name" label="成员" min-width="100" resizable />
              <el-table-column label="资产" min-width="100" align="right" resizable>
                <template #default="{ row }">¥ {{ row.assetYuan }}</template>
              </el-table-column>
              <el-table-column label="负债" min-width="100" align="right" resizable>
                <template #default="{ row }">¥ {{ row.liabilityYuan }}</template>
              </el-table-column>
              <el-table-column label="可用现金" min-width="100" align="right" resizable>
                <template #default="{ row }">¥ {{ row.availableCashYuan }}</template>
              </el-table-column>
              <el-table-column label="净资产" min-width="100" align="right" resizable>
                <template #default="{ row }">
                  <span :style="{ color: row.net >= 0 ? '#409eff' : '#f56c6c' }">¥ {{ row.netYuan }}</span>
                </template>
              </el-table-column>
            </el-table>
          </el-card>

          <el-empty
            v-if="displayAccounts.length === 0 && !pageLoading"
            description="当前筛选下暂无账户"
          />

          <el-table
            v-else-if="displayAccounts.length > 0"
            :data="displayAccounts"
            stripe
            class="responsive-table"
            :border="true"
          >
            <el-table-column label="归属成员" min-width="120" resizable>
              <template #default="{ row }">
                {{ ownerLabel(row.userId) }}
              </template>
            </el-table-column>
            <el-table-column prop="accountName" label="账户名称" min-width="120" resizable />
            <el-table-column label="类型" min-width="100" resizable>
              <template #default="{ row }">
                <el-tag size="small">{{ typeLabel(row.accountType) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="余额" min-width="140" align="right" resizable>
              <template #default="{ row }">
                <span :style="{ color: row.accountType === 'CREDIT_CARD' ? '#f56c6c' : '#409eff', fontWeight: 600 }">
                  ¥ {{ formatMoney(row.balance) }}
                </span>
              </template>
            </el-table-column>
            <el-table-column prop="currency" label="货币" min-width="80" resizable />
            <el-table-column label="最近更新时间" min-width="160" resizable>
              <template #default="{ row }">
                {{ formatTime(row.updatedAt || row.createdAt) }}
              </template>
            </el-table-column>
          </el-table>
        </template>
      </template>
    </div>
  </MainLayout>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { storeToRefs } from 'pinia'
import MainLayout from '@/layouts/MainLayout.vue'
import { useAuthStore } from '@/stores/auth'
import { useFamilyStore } from '@/stores/family'
import { useWealthStore } from '@/stores/wealth'
import type { Account } from '@/types/wealth'

const authStore = useAuthStore()
const familyStore = useFamilyStore()
const wealthStore = useWealthStore()
const { isAdmin, isCreator, members } = storeToRefs(familyStore)

const pageLoading = ref(false)
const selectedMemberId = ref('')

const canManage = computed(() => isAdmin.value || isCreator.value)

const displayAccounts = computed(() => {
  const list = wealthStore.familyAccounts
  if (!selectedMemberId.value) return list
  return list.filter((acc: Account) => String(acc.userId) === String(selectedMemberId.value))
})

interface SummaryRow {
  name: string
  assetYuan: string
  liabilityYuan: string
  availableCashYuan: string
  netYuan: string
  net: number
  isTotal?: boolean
}
const memberSummaryRows = computed((): SummaryRow[] => {
  const list = wealthStore.familyAccounts as Account[]
  if (list.length === 0 || members.value.length === 0) return []
  const groups = new Map<string, { asset: number; liability: number; availableCash: number }>()
  const mems = members.value as Array<{ userId: string; name?: string; username: string }>
  const findName = (uid: string) => {
    const m = mems.find(mb => String(mb.userId) === uid)
    return m ? (m.name || m.username || `用户 ${uid}`) : `用户 ${uid}`
  }
  list.forEach((acc: Account) => {
    const uid = acc.userId
    if (!groups.has(uid)) groups.set(uid, { asset: 0, liability: 0, availableCash: 0 })
    const g = groups.get(uid)!
    if (acc.accountType === 'CREDIT_CARD') {
      g.liability += acc.balance
    } else {
      g.asset += acc.balance
      if (acc.availableImmediately !== false) g.availableCash += acc.balance
    }
  })
  const rows: SummaryRow[] = Array.from(groups.entries()).map(([uid, g]) => ({
    name: findName(uid),
    assetYuan: (g.asset / 100).toFixed(2),
    liabilityYuan: (g.liability / 100).toFixed(2),
    availableCashYuan: (g.availableCash / 100).toFixed(2),
    netYuan: ((g.asset - g.liability) / 100).toFixed(2),
    net: (g.asset - g.liability) / 100,
  }))
  const total = rows.reduce(
    (acc, r) => ({
      asset: acc.asset + parseFloat(r.assetYuan),
      liability: acc.liability + parseFloat(r.liabilityYuan),
      availableCash: acc.availableCash + parseFloat(r.availableCashYuan),
      net: acc.net + parseFloat(r.netYuan),
    }),
    { asset: 0, liability: 0, availableCash: 0, net: 0 }
  )
  rows.push({
    name: '总汇总',
    assetYuan: total.asset.toFixed(2),
    liabilityYuan: total.liability.toFixed(2),
    availableCashYuan: total.availableCash.toFixed(2),
    netYuan: total.net.toFixed(2),
    net: total.net,
    isTotal: true,
  })
  return rows
})

const TYPE_LABELS: Record<string, string> = {
  SAVING: '储蓄',
  CREDIT_CARD: '信用卡',
  STOCK: '股票',
  FUND: '基金',
  ALIPAY: '支付宝',
  WECHAT: '微信',
  OTHER: '其他',
}

function typeLabel(t: string) {
  return TYPE_LABELS[t] || t
}

function ownerLabel(userId: string) {
  const m = members.value.find(mb => String(mb.userId) === String(userId))
  if (!m) return `用户 ${userId}`
  return m.name || m.username || `用户 ${userId}`
}

function formatMoney(fen: number) {
  return (fen / 100).toLocaleString('zh-CN', {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  })
}

function formatTime(dt?: string | null) {
  if (!dt) return '—'
  return dt.replace('T', ' ').slice(0, 16)
}

onMounted(async () => {
  if (!authStore.user?.familyId) return
  pageLoading.value = true
  try {
    await familyStore.fetchFamily(authStore.user.familyId)
    if (canManage.value) {
      await wealthStore.fetchFamilyAccounts()
    }
  } finally {
    pageLoading.value = false
  }
})
</script>

<style scoped>
.family-accounts {
  margin: 0 auto;
}
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  flex-wrap: wrap;
  gap: 12px;
}
.page-title {
  margin: 0;
  font-size: 1.5rem;
}
.filter-row {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 16px;
  flex-wrap: wrap;
}
.filter-label {
  color: #606266;
  font-weight: 500;
}
.filter-select {
  width: 200px;
}
.summary-card {
  margin-bottom: 20px;
}
.card-title {
  font-weight: 600;
}
.responsive-table {
  width: 100%;
}

/* 响应式样式 */
@media screen and (max-width: 768px) {
  .page-title {
    font-size: 1.25rem;
  }
  .filter-select {
    width: 100%;
    max-width: 200px;
  }
  .summary-card {
    margin-bottom: 16px;
  }
}

@media screen and (max-width: 480px) {
  .page-title {
    font-size: 1.1rem;
  }
  .page-header {
    margin-bottom: 16px;
  }
  .filter-row {
    margin-bottom: 12px;
  }
  .filter-select {
    width: 100%;
  }
}
</style>
