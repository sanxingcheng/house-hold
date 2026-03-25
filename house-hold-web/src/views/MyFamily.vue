<template>
  <MainLayout>
    <div class="my-family" v-loading="pageLoading">
      <h1 style="margin:0 0 24px">我的家庭</h1>

      <el-empty v-if="!authStore.user?.familyId" description="请先加入或创建家庭" />

      <template v-else-if="familyStore.family">
        <!-- 家庭信息 -->
        <el-card shadow="never" class="section-card">
          <template #header><span class="card-title">家庭信息</span></template>
          <el-descriptions :column="1" border>
            <el-descriptions-item label="家庭 ID">{{ familyStore.family.id }}</el-descriptions-item>
            <el-descriptions-item label="别名">{{ familyStore.family.nameAlias }}</el-descriptions-item>
            <el-descriptions-item label="地址">
              {{ [familyStore.family.country, familyStore.family.province, familyStore.family.city, familyStore.family.street].filter(Boolean).join(' ') || '—' }}
            </el-descriptions-item>
          </el-descriptions>
        </el-card>

        <!-- 成员列表 -->
        <el-card shadow="never" class="section-card">
          <template #header><span class="card-title">成员列表</span></template>
          <el-table :data="familyStore.members" stripe>
            <el-table-column prop="username" label="用户名" min-width="100" />
            <el-table-column prop="name" label="姓名" min-width="100">
              <template #default="{ row }">{{ row.name || '—' }}</template>
            </el-table-column>
            <el-table-column label="角色" width="100">
              <template #default="{ row }">
                <el-tag v-if="row.isCreator" type="success" size="small">户主</el-tag>
                <el-tag v-else-if="row.isAdmin" type="warning" size="small">管理员</el-tag>
                <el-tag v-else type="info" size="small">成员</el-tag>
              </template>
            </el-table-column>
          </el-table>
        </el-card>

        <!-- 家庭共有资产列表 -->
        <el-card shadow="never" class="section-card">
          <template #header><span class="card-title">家庭共有资产</span></template>
          <el-empty v-if="wealthStore.familyAssets.length === 0" description="暂无共有资产" :image-size="48" />
          <el-table v-else :data="wealthStore.familyAssets" stripe>
            <el-table-column prop="assetName" label="资产名称" min-width="120" />
            <el-table-column label="类型" width="90">
              <template #default="{ row }">
                <el-tag size="small">{{ typeLabel(row.assetType) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="金额" width="130" align="right">
              <template #default="{ row }">
                <span style="font-weight:600;color:#409eff">¥ {{ formatMoney(row.amount) }}</span>
              </template>
            </el-table-column>
            <el-table-column label="贷款余额" width="120" align="right">
              <template #default="{ row }">
                <span v-if="showLoan(row.assetType) && (row.loanRemaining ?? 0) > 0" style="color:#f56c6c">
                  ¥ {{ formatMoney(row.loanRemaining!) }}
                </span>
                <span v-else>—</span>
              </template>
            </el-table-column>
            <el-table-column prop="remark" label="备注" min-width="100" show-overflow-tooltip>
              <template #default="{ row }">{{ row.remark || '—' }}</template>
            </el-table-column>
          </el-table>
        </el-card>
      </template>
    </div>
  </MainLayout>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import MainLayout from '@/layouts/MainLayout.vue'
import { useAuthStore } from '@/stores/auth'
import { useFamilyStore } from '@/stores/family'
import { useWealthStore } from '@/stores/wealth'

const authStore = useAuthStore()
const familyStore = useFamilyStore()
const wealthStore = useWealthStore()

const pageLoading = ref(false)

const TYPE_LABELS: Record<string, string> = {
  REAL_ESTATE: '房产', VEHICLE: '车辆', DEPOSIT: '存款', INVESTMENT: '投资', OTHER: '其他',
}
function typeLabel(t: string) { return TYPE_LABELS[t] || t }
function showLoan(t: string) { return t === 'REAL_ESTATE' || t === 'VEHICLE' }
function formatMoney(fen: number) {
  return (fen / 100).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

onMounted(async () => {
  if (!authStore.user?.familyId) return
  pageLoading.value = true
  try {
    await Promise.all([
      familyStore.fetchFamily(authStore.user.familyId),
      wealthStore.fetchFamilyAssets(),
    ])
  } finally {
    pageLoading.value = false
  }
})
</script>

<style scoped>
.my-family {
  margin: 0 auto;
}
.section-card {
  margin-bottom: 20px;
}
.card-title {
  font-weight: 600;
  font-size: 1rem;
}
</style>
