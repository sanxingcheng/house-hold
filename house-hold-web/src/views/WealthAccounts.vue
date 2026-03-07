<template>
  <MainLayout>
    <div class="wealth-accounts" v-loading="pageLoading">
      <div class="page-header">
        <h1 style="margin:0">账户管理</h1>
        <div style="display:flex;align-items:center;gap:16px">
          <span v-if="userSummary" style="color:#909399">
            净资产：<b :style="{ color: userSummary.netWorth >= 0 ? '#409eff' : '#f56c6c' }">¥ {{ formatMoney(userSummary.netWorth) }}</b>
          </span>
          <el-button type="primary" :icon="Plus" @click="openCreateDialog">新增账户</el-button>
        </div>
      </div>

      <!-- 管理员：选择查看成员 -->
      <el-card v-if="familyStore.isAdmin && familyStore.members.length > 1" shadow="never" style="margin-bottom:16px">
        <div style="display:flex;align-items:center;gap:12px">
          <span style="color:#606266;font-weight:500">查看成员账户：</span>
          <el-select v-model="selectedMemberId" placeholder="选择成员" style="width:200px" @change="onMemberChange">
            <el-option label="— 我的账户 —" value="" />
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

      <el-table v-else :data="displayAccounts" stripe style="width:100%">
        <el-table-column prop="accountName" label="账户名称" min-width="150" />
        <el-table-column prop="accountType" label="类型" width="120">
          <template #default="{ row }">
            <el-tag :type="row.accountType === 'CREDIT_CARD' ? 'danger' : 'primary'" size="small">{{ typeLabel(row.accountType) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="余额" width="160" align="right">
          <template #default="{ row }">
            <span :style="{ color: row.accountType === 'CREDIT_CARD' ? '#f56c6c' : '#409eff', fontWeight: 600 }">
              ¥ {{ formatMoney(row.balance) }}
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="currency" label="货币" width="80" />
        <el-table-column label="操作" width="160" align="center">
          <template #default="{ row }">
            <el-button size="small" link type="primary" @click="openEditDialog(row)">编辑</el-button>
            <el-popconfirm :title="`确定删除「${row.accountName}」？`" confirm-button-text="删除" cancel-button-text="取消" @confirm="handleDelete(row)">
              <template #reference>
                <el-button size="small" link type="danger">删除</el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>

      <!-- 新增/编辑对话框 -->
      <el-dialog v-model="showDialog" :title="isEditing ? '编辑账户' : '新增账户'" width="460px" destroy-on-close>
        <el-form ref="dialogFormRef" :model="form" :rules="formRules" label-width="100px">
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
        </el-form>
        <template #footer>
          <el-button @click="showDialog = false">取消</el-button>
          <el-button type="primary" :loading="submitting" @click="handleSubmit">确定</el-button>
        </template>
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
import MainLayout from '@/layouts/MainLayout.vue'
import { useAuthStore } from '@/stores/auth'
import { useFamilyStore } from '@/stores/family'
import { useWealthStore } from '@/stores/wealth'
import type { Account, AccountType } from '@/types/wealth'

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

const form = reactive({
  accountName: '',
  accountType: '' as AccountType | '',
  balanceYuan: 0,
  currency: 'CNY',
})

const formRules: FormRules = {
  accountName: [{ required: true, message: '请输入账户名称', trigger: 'blur' }],
  accountType: [{ required: true, message: '请选择账户类型', trigger: 'change' }],
  balanceYuan: [{ required: true, message: '请输入金额', trigger: 'blur' }],
}

const otherMembers = computed(() =>
  familyStore.members.filter(m => m.userId !== authStore.user?.id)
)

const displayAccounts = computed(() =>
  selectedMemberId.value ? memberAccounts.value : accounts.value
)

const TYPE_LABELS: Record<string, string> = {
  SAVING: '储蓄卡', CREDIT_CARD: '信用卡', STOCK: '股票', FUND: '基金',
  ALIPAY: '支付宝', WECHAT: '微信', OTHER: '其他',
}
function typeLabel(t: string) { return TYPE_LABELS[t] || t }
function formatMoney(fen: number) {
  return (fen / 100).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

function openCreateDialog() {
  isEditing.value = false
  editingId.value = ''
  Object.assign(form, { accountName: '', accountType: '', balanceYuan: 0, currency: 'CNY' })
  showDialog.value = true
}

function openEditDialog(acc: Account) {
  isEditing.value = true
  editingId.value = acc.id
  Object.assign(form, { accountName: acc.accountName, accountType: acc.accountType, balanceYuan: acc.balance / 100, currency: acc.currency })
  showDialog.value = true
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
    const payload = {
      accountName: form.accountName,
      accountType: form.accountType as AccountType,
      balance: balanceFen,
      currency: form.currency || 'CNY',
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
