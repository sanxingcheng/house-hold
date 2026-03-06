<template>
  <MainLayout>
    <div class="wealth-accounts">
      <div class="page-header">
        <h1>账户管理</h1>
        <div class="header-actions">
          <span v-if="userSummary" class="net-worth">
            净资产：<b :class="userSummary.netWorth >= 0 ? 'positive' : 'negative'">
              ¥ {{ formatMoney(userSummary.netWorth) }}
            </b>
          </span>
          <button class="btn btn-primary" @click="openCreateDialog">新增账户</button>
        </div>
      </div>

      <div v-if="accounts.length === 0" class="empty">
        <p>暂无账户，点击「新增账户」开始管理你的资产吧</p>
      </div>

      <div v-else class="account-grid">
        <div v-for="acc in accounts" :key="acc.id" class="account-card" :class="{ liability: acc.accountType === 'CREDIT_CARD' }">
          <div class="card-header">
            <span class="card-name">{{ acc.accountName }}</span>
            <span class="card-type">{{ typeLabel(acc.accountType) }}</span>
          </div>
          <div class="card-balance" :class="acc.accountType === 'CREDIT_CARD' ? 'negative' : 'positive'">
            ¥ {{ formatMoney(acc.balance) }}
          </div>
          <div class="card-footer">
            <span class="currency">{{ acc.currency }}</span>
            <div class="card-actions">
              <button class="btn-sm" @click="openEditDialog(acc)">编辑</button>
              <button class="btn-sm btn-danger" @click="confirmDelete(acc)">删除</button>
            </div>
          </div>
        </div>
      </div>

      <!-- 新增/编辑弹框 -->
      <div v-if="showDialog" class="dialog-overlay" @click.self="closeDialog">
        <div class="dialog">
          <h2>{{ editing ? '编辑账户' : '新增账户' }}</h2>
          <form @submit.prevent="handleSubmit">
            <div class="form-group">
              <label>账户名称 *</label>
              <input v-model="form.accountName" required maxlength="64" placeholder="如：招商银行储蓄卡" />
            </div>
            <div class="form-group">
              <label>账户类型 *</label>
              <select v-model="form.accountType" required>
                <option value="" disabled>请选择</option>
                <option value="SAVING">储蓄卡</option>
                <option value="CREDIT_CARD">信用卡</option>
                <option value="STOCK">股票</option>
                <option value="FUND">基金</option>
                <option value="ALIPAY">支付宝</option>
                <option value="WECHAT">微信</option>
                <option value="OTHER">其他</option>
              </select>
            </div>
            <div class="form-group">
              <label>{{ form.accountType === 'CREDIT_CARD' ? '当前账单金额' : '余额' }} *（元）</label>
              <input v-model.number="formBalanceYuan" type="number" step="0.01" min="0" required placeholder="0.00" />
            </div>
            <div class="form-group">
              <label>货币</label>
              <input v-model="form.currency" placeholder="CNY" />
            </div>
            <div class="dialog-actions">
              <button type="button" class="btn" @click="closeDialog">取消</button>
              <button type="submit" class="btn btn-primary" :disabled="submitting">
                {{ submitting ? '提交中...' : '确定' }}
              </button>
            </div>
            <p v-if="errorMsg" class="error">{{ errorMsg }}</p>
          </form>
        </div>
      </div>

      <!-- 删除确认 -->
      <div v-if="showDeleteConfirm" class="dialog-overlay" @click.self="showDeleteConfirm = false">
        <div class="dialog dialog-sm">
          <h2>确认删除</h2>
          <p>确定要删除账户「{{ deletingAccount?.accountName }}」吗？此操作不可撤销。</p>
          <div class="dialog-actions">
            <button class="btn" @click="showDeleteConfirm = false">取消</button>
            <button class="btn btn-danger" :disabled="submitting" @click="handleDelete">确定删除</button>
          </div>
        </div>
      </div>
    </div>
  </MainLayout>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { storeToRefs } from 'pinia'
import MainLayout from '@/layouts/MainLayout.vue'
import { useWealthStore } from '@/stores/wealth'
import type { Account, AccountType } from '@/types/wealth'

const wealthStore = useWealthStore()
const { accounts, userSummary } = storeToRefs(wealthStore)

const showDialog = ref(false)
const editing = ref(false)
const editingId = ref('')
const submitting = ref(false)
const errorMsg = ref('')
const showDeleteConfirm = ref(false)
const deletingAccount = ref<Account | null>(null)

const form = ref({
  accountName: '',
  accountType: '' as AccountType | '',
  currency: 'CNY',
})
const formBalanceYuan = ref(0)

const TYPE_LABELS: Record<string, string> = {
  SAVING: '储蓄卡',
  CREDIT_CARD: '信用卡',
  STOCK: '股票',
  FUND: '基金',
  ALIPAY: '支付宝',
  WECHAT: '微信',
  OTHER: '其他',
}

function typeLabel(type: string) {
  return TYPE_LABELS[type] || type
}

function formatMoney(fen: number) {
  return (fen / 100).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

function openCreateDialog() {
  editing.value = false
  editingId.value = ''
  form.value = { accountName: '', accountType: '', currency: 'CNY' }
  formBalanceYuan.value = 0
  errorMsg.value = ''
  showDialog.value = true
}

function openEditDialog(acc: Account) {
  editing.value = true
  editingId.value = acc.id
  form.value = {
    accountName: acc.accountName,
    accountType: acc.accountType,
    currency: acc.currency,
  }
  formBalanceYuan.value = acc.balance / 100
  errorMsg.value = ''
  showDialog.value = true
}

function closeDialog() {
  showDialog.value = false
}

function confirmDelete(acc: Account) {
  deletingAccount.value = acc
  showDeleteConfirm.value = true
}

async function handleSubmit() {
  if (!form.value.accountName || !form.value.accountType) return
  submitting.value = true
  errorMsg.value = ''
  try {
    const balanceFen = Math.round(formBalanceYuan.value * 100)
    if (editing.value) {
      await wealthStore.editAccount(editingId.value, {
        accountName: form.value.accountName,
        accountType: form.value.accountType as AccountType,
        balance: balanceFen,
        currency: form.value.currency || 'CNY',
      })
    } else {
      await wealthStore.addAccount({
        accountName: form.value.accountName,
        accountType: form.value.accountType as AccountType,
        balance: balanceFen,
        currency: form.value.currency || 'CNY',
      })
    }
    closeDialog()
    await wealthStore.fetchUserSummary()
  } catch (e: any) {
    errorMsg.value = e.message || '操作失败'
  } finally {
    submitting.value = false
  }
}

async function handleDelete() {
  if (!deletingAccount.value) return
  submitting.value = true
  try {
    await wealthStore.removeAccount(deletingAccount.value.id)
    showDeleteConfirm.value = false
    deletingAccount.value = null
    await wealthStore.fetchUserSummary()
  } catch (e: any) {
    errorMsg.value = e.message || '删除失败'
  } finally {
    submitting.value = false
  }
}

onMounted(async () => {
  await Promise.all([wealthStore.fetchAccounts(), wealthStore.fetchUserSummary()])
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
  margin-bottom: 1.5rem;
}
.page-header h1 {
  font-size: 1.5rem;
  color: #333;
}
.header-actions {
  display: flex;
  align-items: center;
  gap: 1rem;
}
.net-worth {
  font-size: 1rem;
  color: #666;
}
.positive { color: #2563eb; }
.negative { color: #dc2626; }
.empty {
  text-align: center;
  padding: 3rem;
  color: #999;
  background: #fff;
  border-radius: 8px;
}
.account-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 1rem;
}
.account-card {
  background: #fff;
  border-radius: 8px;
  padding: 1.25rem;
  box-shadow: 0 1px 3px rgba(0,0,0,0.08);
  border-left: 4px solid #2563eb;
}
.account-card.liability {
  border-left-color: #dc2626;
}
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 0.75rem;
}
.card-name {
  font-weight: 600;
  color: #333;
}
.card-type {
  font-size: 0.75rem;
  padding: 2px 8px;
  border-radius: 10px;
  background: #f0f0f0;
  color: #666;
}
.card-balance {
  font-size: 1.5rem;
  font-weight: 700;
  margin-bottom: 0.75rem;
}
.card-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.currency {
  font-size: 0.8rem;
  color: #999;
}
.card-actions {
  display: flex;
  gap: 0.5rem;
}
.btn {
  padding: 0.5rem 1rem;
  border: 1px solid #ddd;
  border-radius: 6px;
  background: #fff;
  cursor: pointer;
  font-size: 0.875rem;
}
.btn:hover { background: #f5f5f5; }
.btn-primary {
  background: #2563eb;
  color: #fff;
  border-color: #2563eb;
}
.btn-primary:hover { background: #1d4ed8; }
.btn-primary:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}
.btn-danger {
  background: #dc2626;
  color: #fff;
  border-color: #dc2626;
}
.btn-danger:hover { background: #b91c1c; }
.btn-sm {
  padding: 0.25rem 0.6rem;
  border: 1px solid #ddd;
  border-radius: 4px;
  background: #fff;
  cursor: pointer;
  font-size: 0.8rem;
}
.btn-sm:hover { background: #f5f5f5; }
.btn-sm.btn-danger {
  color: #dc2626;
  border-color: #fca5a5;
  background: #fff;
}
.btn-sm.btn-danger:hover {
  background: #fef2f2;
}

.dialog-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0,0,0,0.4);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 100;
}
.dialog {
  background: #fff;
  border-radius: 12px;
  padding: 2rem;
  width: 420px;
  max-width: 90vw;
  box-shadow: 0 8px 30px rgba(0,0,0,0.12);
}
.dialog-sm { width: 360px; }
.dialog h2 {
  margin-bottom: 1.25rem;
  font-size: 1.25rem;
  color: #333;
}
.form-group {
  margin-bottom: 1rem;
}
.form-group label {
  display: block;
  margin-bottom: 0.3rem;
  font-size: 0.875rem;
  color: #555;
}
.form-group input,
.form-group select {
  width: 100%;
  padding: 0.5rem 0.75rem;
  border: 1px solid #ddd;
  border-radius: 6px;
  font-size: 0.9rem;
  box-sizing: border-box;
}
.form-group input:focus,
.form-group select:focus {
  outline: none;
  border-color: #2563eb;
  box-shadow: 0 0 0 2px rgba(37,99,235,0.15);
}
.dialog-actions {
  display: flex;
  justify-content: flex-end;
  gap: 0.75rem;
  margin-top: 1.5rem;
}
.error {
  color: #dc2626;
  margin-top: 0.75rem;
  font-size: 0.875rem;
}
</style>
