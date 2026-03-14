<template>
  <MainLayout>
    <div class="family-accounts" v-loading="pageLoading">
      <div class="page-header">
        <h1 style="margin:0">家庭成员账户</h1>
        <el-tag v-if="!canManage" type="info" size="small">仅户主/管理员可查看</el-tag>
      </div>

      <el-empty
        v-if="!authStore.user?.familyId"
        description="请先加入或创建家庭"
      />

      <template v-else>
        <el-empty
          v-if="canManage && wealthStore.familyAccounts.length === 0 && !pageLoading"
          description="当前家庭暂无成员账户"
        />

        <el-alert
          v-if="!canManage"
          type="warning"
          show-icon
          title="您不是该家庭的户主或管理员，无法查看所有成员账户。"
          style="margin-bottom:16px"
        />

        <el-table
          v-if="canManage && wealthStore.familyAccounts.length > 0"
          :data="wealthStore.familyAccounts"
          stripe
          style="width:100%"
        >
          <el-table-column label="归属成员" min-width="140">
            <template #default="{ row }">
              {{ ownerLabel(row.userId) }}
            </template>
          </el-table-column>
          <el-table-column prop="accountName" label="账户名称" min-width="140" />
          <el-table-column label="类型" width="120">
            <template #default="{ row }">
              <el-tag size="small">{{ typeLabel(row.accountType) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="余额" width="160" align="right">
            <template #default="{ row }">
              <span style="font-weight:600;color:#409eff">
                ¥ {{ formatMoney(row.balance) }}
              </span>
            </template>
          </el-table-column>
          <el-table-column prop="currency" label="货币" width="80" />
          <el-table-column label="最近更新时间" min-width="170">
            <template #default="{ row }">
              {{ formatTime(row.updatedAt || row.createdAt) }}
            </template>
          </el-table-column>
        </el-table>
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

const authStore = useAuthStore()
const familyStore = useFamilyStore()
const wealthStore = useWealthStore()
const { isAdmin, isCreator, members } = storeToRefs(familyStore)

const pageLoading = ref(false)

const canManage = computed(() => isAdmin.value || isCreator.value)

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
  max-width: 960px;
  margin: 0 auto;
}
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}
</style>

