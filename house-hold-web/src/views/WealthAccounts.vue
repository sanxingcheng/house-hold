<template>
  <MainLayout>
    <div class="wealth-accounts" v-loading="pageLoading">
      <div class="page-header">
        <h1 class="page-title">账户管理</h1>
        <div class="header-stats">
          <span v-if="currentSummary" class="stat-item">
            净资产：<b :style="{ color: currentSummary.netWorth >= 0 ? '#409eff' : '#f56c6c' }">¥ {{ formatMoney(currentSummary.netWorth) }}</b>
          </span>
          <span v-if="currentSummary && currentSummary.availableCash != null" class="stat-item">
            可用现金：<b style="color:#67c23a">¥ {{ formatMoney(currentSummary.availableCash) }}</b>
          </span>
          <el-button type="primary" :icon="Plus" @click="openCreateDialog">新增账户</el-button>
        </div>
      </div>

      <!-- 管理员：选择查看成员 -->
      <el-card v-if="familyStore.isAdmin && familyStore.members.length > 1" shadow="never" class="member-select-card">
        <div class="member-select-row">
          <span class="select-label">查看成员账户：</span>
          <el-select v-model="selectedMemberId" placeholder="选择成员" class="member-select" @change="onMemberChange">
            <el-option :label="myAccountOptionLabel" value="" />
            <el-option
              v-for="m in otherMembers" :key="m.userId"
              :label="`${m.username}${m.name ? ' (' + m.name + ')' : ''}`"
              :value="m.userId"
            />
          </el-select>
          <el-tag v-if="selectedMemberId" type="info" size="small">管理员代管模式</el-tag>
        </div>
      </el-card>

      <el-empty v-if="displayAccounts.length === 0 && !pageLoading" description="暂无账户，点击「新增账户」开始管理吧" />

      <el-table
        v-else
        :data="displayAccounts"
        stripe
        class="responsive-table"
        :border="true"
        @cell-dblclick="handleCellDblClick"
      >
        <el-table-column prop="accountName" label="账户名称" min-width="140" resizable />
        <el-table-column prop="accountType" label="类型" min-width="100" resizable>
          <template #default="{ row }">
            <el-tag :type="row.accountType === 'CREDIT_CARD' ? 'danger' : 'primary'" size="small">{{ typeLabel(row.accountType) }}</el-tag>
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
        <el-table-column prop="remark" label="备注" min-width="120" show-overflow-tooltip resizable />
        <el-table-column label="操作" min-width="180" align="center" resizable>
          <template #default="{ row }">
            <el-button size="small" link type="primary" @click="openEditDialog(row)">编辑</el-button>
            <el-button size="small" link type="success" @click="openTrendDialog(row)">趋势</el-button>
            <el-popconfirm :title="`确定删除「${row.accountName}」？`" confirm-button-text="删除" cancel-button-text="取消" @confirm="handleDelete(row)">
              <template #reference>
                <el-button size="small" link type="danger">删除</el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>

      <!-- 新增/编辑对话框 -->
      <el-dialog v-model="showDialog" :title="isEditing ? '编辑账户' : '新增账户'" width="500px" destroy-on-close>
        <el-form ref="dialogFormRef" :model="form" :rules="formRules" label-width="150px">
          <el-form-item label="账户名称" prop="accountName">
            <el-input v-model="form.accountName" placeholder="如：招商银行储蓄卡" maxlength="64" />
          </el-form-item>
          <el-form-item label="账户类型" prop="accountType">
            <el-select v-model="form.accountType" placeholder="请选择" style="width:100%">
              <el-option label="储蓄卡" value="SAVING" />
              <el-option label="信用卡" value="CREDIT_CARD" />
              <el-option label="股票" value="STOCK" />
              <el-option label="基金" value="FUND" />
              <el-option label="支付宝" value="ALIPAY" />
              <el-option label="微信" value="WECHAT" />
              <el-option label="其他" value="OTHER" />
            </el-select>
          </el-form-item>
          <el-form-item :label="form.accountType === 'CREDIT_CARD' ? '账单金额（元）' : '余额（元）'" prop="balanceYuan">
            <el-input-number v-model="form.balanceYuan" :precision="2" :min="0" :step="100" controls-position="right" style="width:100%" />
          </el-form-item>
          <el-form-item label="货币">
            <el-input v-model="form.currency" placeholder="CNY" />
          </el-form-item>
          <el-form-item v-if="form.accountType && form.accountType !== 'CREDIT_CARD'" label="是否立即可用现金">
            <el-switch v-model="form.availableImmediately" />
            <span style="margin-left:8px;color:#909399;font-size:12px">关闭表示投资等非随时可取</span>
          </el-form-item>
          <el-form-item label="备注">
            <el-input v-model="form.remark" type="textarea" :rows="2" placeholder="选填" maxlength="256" show-word-limit />
          </el-form-item>
        </el-form>
        <template #footer>
          <el-button @click="showDialog = false">取消</el-button>
          <el-button type="primary" :loading="submitting" @click="handleSubmit">确定</el-button>
        </template>
      </el-dialog>

      <!-- 余额趋势弹窗 -->
      <el-dialog v-model="trendDialogVisible" :title="trendAccount ? `「${trendAccount.accountName}」余额趋势` : '余额趋势'" width="560px" destroy-on-close @open="loadTrendData">
        <div class="trend-controls">
          <el-radio-group v-model="trendRange" size="small" @change="loadTrendData">
            <el-radio-button value="30">近 30 天</el-radio-button>
            <el-radio-button value="90">近 90 天</el-radio-button>
            <el-radio-button value="180">近 180 天</el-radio-button>
          </el-radio-group>
        </div>
        <div class="trend-chart-wrap" v-loading="trendLoading">
          <template v-if="!trendLoading && trendData.length === 0">
            <el-empty description="暂无余额记录，修改余额后会按日记录" :image-size="60" />
          </template>
          <v-chart v-else-if="trendData.length > 0" :option="trendChartOption" autoresize style="height:320px;width:100%" />
        </div>
      </el-dialog>
    </div>
  </MainLayout>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { storeToRefs } from 'pinia'
import type { FormInstance, FormRules } from 'element-plus'
import { ElMessage } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { LineChart } from 'echarts/charts'
import { GridComponent, TooltipComponent } from 'echarts/components'
import VChart from 'vue-echarts'
import MainLayout from '@/layouts/MainLayout.vue'
import { useAuthStore } from '@/stores/auth'
import { useFamilyStore } from '@/stores/family'
import { useWealthStore } from '@/stores/wealth'
import { getAccountHistory } from '@/api/wealth'
import type { Account, AccountType } from '@/types/wealth'

use([CanvasRenderer, LineChart, GridComponent, TooltipComponent])

const authStore = useAuthStore()
const familyStore = useFamilyStore()
const wealthStore = useWealthStore()
const { accounts, userSummary, memberAccounts } = storeToRefs(wealthStore)

const pageLoading = ref(false)
const showDialog = ref(false)
const isEditing = ref(false)
const editingId = ref('')
const submitting = ref(false)
const dialogFormRef = ref<FormInstance>()
const selectedMemberId = ref('')

const trendDialogVisible = ref(false)
const trendAccount = ref<Account | null>(null)
const trendData = ref<Array<{ snapshotDate: string; balance: number }>>([])
const trendRange = ref('30')
const trendLoading = ref(false)

const form = reactive({
  accountName: '',
  accountType: '' as AccountType | '',
  balanceYuan: 0,
  currency: 'CNY',
  availableImmediately: true,
  remark: '',
})

const formRules: FormRules = {
  accountName: [{ required: true, message: '请输入账户名称', trigger: 'blur' }],
  accountType: [{ required: true, message: '请选择账户类型', trigger: 'change' }],
  balanceYuan: [{ required: true, message: '请输入金额', trigger: 'blur' }],
}

const otherMembers = computed(() =>
  familyStore.members.filter(m => m.userId !== authStore.user?.id)
)

const myAccountOptionLabel = computed(() => {
  const me = familyStore.members.find(m => String(m.userId) === String(authStore.user?.id))
  if (me) return `${me.username}${me.name ? ' (' + me.name + ')' : ''}`
  return authStore.user?.username ?? '— 我的账户 —'
})

const displayAccounts = computed(() =>
  selectedMemberId.value ? memberAccounts.value : accounts.value
)

const currentSummary = computed(() => {
  if (selectedMemberId.value) {
    const list = memberAccounts.value
    if (list.length === 0) return null
    
    let totalAssets = 0
    let totalLiabilities = 0
    let availableCash = 0
    
    list.forEach(acc => {
      if (acc.accountType === 'CREDIT_CARD') {
        totalLiabilities += acc.balance
      } else {
        totalAssets += acc.balance
        if (acc.availableImmediately !== false) {
          availableCash += acc.balance
        }
      }
    })
    
    return {
      netWorth: totalAssets - totalLiabilities,
      availableCash,
    }
  }
  return userSummary.value
})

const TYPE_LABELS: Record<string, string> = {
  SAVING: '储蓄卡', CREDIT_CARD: '信用卡', STOCK: '股票', FUND: '基金',
  ALIPAY: '支付宝', WECHAT: '微信', OTHER: '其他',
}
function typeLabel(t: string) { return TYPE_LABELS[t] || t }
function formatMoney(fen: number) {
  return (fen / 100).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

function openTrendDialog(acc: Account) {
  trendAccount.value = acc
  trendDialogVisible.value = true
  trendRange.value = '30'
}

function getTrendDateRange() {
  const to = new Date()
  const from = new Date()
  from.setDate(from.getDate() - parseInt(trendRange.value))
  return { from: from.toISOString().slice(0, 10), to: to.toISOString().slice(0, 10) }
}

async function loadTrendData() {
  if (!trendAccount.value) return
  trendLoading.value = true
  try {
    const { from, to } = getTrendDateRange()
    const res = await getAccountHistory(trendAccount.value.id, from, to)
    trendData.value = res.data ?? []
  } catch {
    trendData.value = []
  } finally {
    trendLoading.value = false
  }
}

const trendChartOption = computed(() => {
  const dates = trendData.value.map(p => p.snapshotDate)
  const values = trendData.value.map(p => p.balance / 100)
  const isCredit = trendAccount.value?.accountType === 'CREDIT_CARD'
  const color = isCredit ? '#f56c6c' : '#409eff'
  return {
    tooltip: {
      trigger: 'axis' as const,
      formatter: (params: Array<{ axisValue: string; value: number }>) => {
        const p = params[0]
        return `${p.axisValue}<br/>${isCredit ? '账单' : '余额'}：<b>¥ ${(p.value).toLocaleString('zh-CN', { minimumFractionDigits: 2 })}</b>`
      },
    },
    grid: { left: 60, right: 24, top: 24, bottom: 32 },
    xAxis: {
      type: 'category' as const,
      data: dates,
      axisLabel: { formatter: (v: string) => v.slice(5) },
    },
    yAxis: {
      type: 'value' as const,
      axisLabel: {
        formatter: (v: number) => {
          if (Math.abs(v) >= 10000) return (v / 10000).toFixed(1) + '万'
          return v.toFixed(0)
        },
      },
    },
    series: [{
      type: 'line' as const,
      data: values,
      smooth: true,
      symbol: 'circle',
      symbolSize: 6,
      lineStyle: { color, width: 2 },
      itemStyle: { color },
      areaStyle: { color, opacity: 0.08 },
    }],
  }
})

function openCreateDialog() {
  isEditing.value = false
  editingId.value = ''
  Object.assign(form, { accountName: '', accountType: '', balanceYuan: 0, currency: 'CNY', availableImmediately: true, remark: '' })
  showDialog.value = true
}

function openEditDialog(acc: Account) {
  isEditing.value = true
  editingId.value = acc.id
  Object.assign(form, {
    accountName: acc.accountName,
    accountType: acc.accountType,
    balanceYuan: acc.balance / 100,
    currency: acc.currency,
    availableImmediately: acc.availableImmediately !== false,
    remark: acc.remark ?? '',
  })
  showDialog.value = true
}

function handleCellDblClick(row: Account) {
  openEditDialog(row)
}

async function onMemberChange(userId: string) {
  if (userId) {
    pageLoading.value = true
    try { await wealthStore.fetchMemberAccounts(userId) } finally { pageLoading.value = false }
  }
}

async function handleSubmit() {
  if (!dialogFormRef.value) return
  const valid = await dialogFormRef.value.validate().catch(() => false)
  if (!valid) return

  submitting.value = true
  try {
    const balanceFen = Math.round(form.balanceYuan * 100)
    const payload: any = {
      accountName: form.accountName,
      accountType: form.accountType as AccountType,
      balance: balanceFen,
      currency: form.currency || 'CNY',
      remark: form.remark || undefined,
    }
    if (form.accountType !== 'CREDIT_CARD') {
      payload.availableImmediately = form.availableImmediately
    }

    if (selectedMemberId.value) {
      if (isEditing.value) {
        await wealthStore.editMemberAccount(selectedMemberId.value, editingId.value, payload)
        ElMessage.success('成员账户已更新')
      } else {
        await wealthStore.addMemberAccount(selectedMemberId.value, payload)
        ElMessage.success('成员账户已创建')
      }
    } else {
      if (isEditing.value) {
        await wealthStore.editAccount(editingId.value, payload)
        ElMessage.success('账户已更新')
      } else {
        await wealthStore.addAccount(payload)
        ElMessage.success('账户已创建')
      }
      await wealthStore.fetchUserSummary()
    }
    showDialog.value = false
  } catch (e: unknown) {
    ElMessage.error((e as { message?: string })?.message ?? '操作失败')
  } finally {
    submitting.value = false
  }
}

async function handleDelete(acc: Account) {
  try {
    if (selectedMemberId.value) {
      await wealthStore.removeMemberAccount(selectedMemberId.value, acc.id)
    } else {
      await wealthStore.removeAccount(acc.id)
      await wealthStore.fetchUserSummary()
    }
    ElMessage.success('账户已删除')
  } catch (e: unknown) {
    ElMessage.error((e as { message?: string })?.message ?? '删除失败')
  }
}

onMounted(async () => {
  pageLoading.value = true
  try {
    const promises: Promise<unknown>[] = [wealthStore.fetchAccounts(), wealthStore.fetchUserSummary()]
    if (authStore.user?.familyId) {
      promises.push(familyStore.fetchFamily(authStore.user.familyId))
    }
    await Promise.all(promises)
  } finally {
    pageLoading.value = false
  }
})
</script>

<style scoped>
.wealth-accounts {
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
.header-stats {
  display: flex;
  align-items: center;
  gap: 16px;
  flex-wrap: wrap;
}
.stat-item {
  color: #909399;
}
.member-select-card {
  margin-bottom: 16px;
}
.member-select-row {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}
.select-label {
  color: #606266;
  font-weight: 500;
}
.member-select {
  width: 200px;
}
.responsive-table {
  width: 100%;
}
.trend-controls {
  margin-bottom: 12px;
}
.trend-chart-wrap {
  min-height: 320px;
  display: flex;
  align-items: center;
  justify-content: center;
}

/* 响应式样式 */
@media screen and (max-width: 768px) {
  .page-title {
    font-size: 1.25rem;
  }
  .header-stats {
    gap: 12px;
  }
  .member-select {
    width: 100%;
    max-width: 200px;
  }
}

@media screen and (max-width: 480px) {
  .page-title {
    font-size: 1.1rem;
  }
  .page-header {
    margin-bottom: 16px;
  }
  .header-stats {
    width: 100%;
    justify-content: flex-start;
  }
  .member-select {
    width: 100%;
  }
}
</style>
