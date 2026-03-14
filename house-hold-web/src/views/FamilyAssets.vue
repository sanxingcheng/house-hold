<template>
  <MainLayout>
    <div class="family-assets" v-loading="pageLoading">
      <div class="page-header">
        <h1 style="margin:0">家庭共有资产</h1>
        <el-button v-if="familyStore.isAdmin" type="primary" :icon="Plus" @click="openCreateDialog">新增资产</el-button>
      </div>

      <el-empty v-if="wealthStore.familyAssets.length === 0 && !pageLoading" description="暂无共有资产" />

      <el-table v-else :data="wealthStore.familyAssets" stripe style="width:100%">
        <el-table-column prop="assetName" label="资产名称" min-width="140" />
        <el-table-column label="类型" width="120">
          <template #default="{ row }">
            <el-tag size="small">{{ typeLabel(row.assetType) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="金额" width="160" align="right">
          <template #default="{ row }">
            <span style="font-weight:600;color:#409eff">¥ {{ formatMoney(row.amount) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="贷款余额" width="160" align="right">
          <template #default="{ row }">
            <span v-if="showLoan(row.assetType)">
              <span v-if="row.loanRemaining && row.loanRemaining > 0" style="font-weight:600;color:#f56c6c">
                ¥ {{ formatMoney(row.loanRemaining) }}
              </span>
              <span v-else>—</span>
            </span>
            <span v-else>—</span>
          </template>
        </el-table-column>
        <el-table-column prop="currency" label="货币" width="80" />
        <el-table-column prop="remark" label="备注" min-width="160">
          <template #default="{ row }">
            <span>{{ row.remark || '—' }}</span>
            <el-tag
              v-if="showLoan(row.assetType) && row.loanOnly"
              type="info"
              size="small"
              style="margin-left:8px"
            >
              只记负债
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column v-if="familyStore.isAdmin" label="操作" width="140" align="center">
          <template #default="{ row }">
            <el-button size="small" link type="primary" @click="openEditDialog(row)">编辑</el-button>
            <el-popconfirm :title="`确定删除「${row.assetName}」？`" @confirm="handleDelete(row)">
              <template #reference>
                <el-button size="small" link type="danger">删除</el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>

      <!-- 总计 -->
      <div v-if="wealthStore.familyAssets.length > 0" style="margin-top:16px;text-align:right;color:#606266">
        共有资产总计：
        <b style="color:#409eff;font-size:16px">¥ {{ formatMoney(totalEffectiveAmount) }}</b>
        <span v-if="totalAmount !== totalEffectiveAmount" style="margin-left:8px;font-size:12px;color:#909399">
          （含仅记负债资产原值：¥ {{ formatMoney(totalAmount - totalEffectiveAmount) }}）
        </span>
      </div>

      <!-- 新增/编辑对话框 -->
      <el-dialog v-model="showDialog" :title="isEditing ? '编辑资产' : '新增共有资产'" width="480px" destroy-on-close>
        <el-form ref="dialogFormRef" :model="form" :rules="formRules" label-width="100px">
          <el-form-item label="资产名称" prop="assetName">
            <el-input v-model="form.assetName" placeholder="如：房产-XX小区" maxlength="64" />
          </el-form-item>
          <el-form-item label="资产类型" prop="assetType">
            <el-select v-model="form.assetType" placeholder="请选择" style="width:100%">
              <el-option label="房产" value="REAL_ESTATE" />
              <el-option label="车辆" value="VEHICLE" />
              <el-option label="存款" value="DEPOSIT" />
              <el-option label="投资" value="INVESTMENT" />
              <el-option label="其他" value="OTHER" />
            </el-select>
          </el-form-item>
          <el-form-item label="金额（元）" prop="amountYuan">
            <el-input-number v-model="form.amountYuan" :precision="2" :min="0" :step="1000" controls-position="right" style="width:100%" />
          </el-form-item>
          <el-form-item label="货币">
            <el-input v-model="form.currency" placeholder="CNY" />
          </el-form-item>
          <el-form-item label="备注">
            <el-input v-model="form.remark" type="textarea" :rows="2" maxlength="256" />
          </el-form-item>
          <el-form-item label="贷款总额（元）">
            <el-input-number
              v-model="form.loanTotalYuan"
              :precision="2"
              :min="0"
              :step="10000"
              controls-position="right"
              style="width:100%"
              :placeholder="showLoan(form.assetType) ? '仅房产/车辆可填写' : '非房产/车辆一般留空'"
            />
          </el-form-item>
          <el-form-item label="当前贷款余额（元）">
            <el-input-number
              v-model="form.loanRemainingYuan"
              :precision="2"
              :min="0"
              :step="10000"
              controls-position="right"
              style="width:100%"
              :placeholder="showLoan(form.assetType) ? '计入负债的贷款余额' : '非房产/车辆一般留空'"
            />
          </el-form-item>
          <el-form-item v-if="showLoan(form.assetType)" label="只统计负债">
            <el-switch
              v-model="form.loanOnly"
              active-text="不计入资产总额"
            />
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
import type { FormInstance, FormRules } from 'element-plus'
import { ElMessage } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import MainLayout from '@/layouts/MainLayout.vue'
import { useAuthStore } from '@/stores/auth'
import { useFamilyStore } from '@/stores/family'
import { useWealthStore } from '@/stores/wealth'
import type { FamilyAsset } from '@/types/wealth'

const authStore = useAuthStore()
const familyStore = useFamilyStore()
const wealthStore = useWealthStore()

const pageLoading = ref(false)
const showDialog = ref(false)
const isEditing = ref(false)
const editingId = ref('')
const submitting = ref(false)
const dialogFormRef = ref<FormInstance>()

const form = reactive({
  assetName: '',
  assetType: '' as string,
  amountYuan: 0,
  currency: 'CNY',
  remark: '',
  loanTotalYuan: 0,
  loanRemainingYuan: 0,
  loanOnly: false,
})

const formRules: FormRules = {
  assetName: [{ required: true, message: '请输入资产名称', trigger: 'blur' }],
  assetType: [{ required: true, message: '请选择资产类型', trigger: 'change' }],
  amountYuan: [{ required: true, message: '请输入金额', trigger: 'blur' }],
}

const TYPE_LABELS: Record<string, string> = {
  REAL_ESTATE: '房产', VEHICLE: '车辆', DEPOSIT: '存款', INVESTMENT: '投资', OTHER: '其他',
}
function typeLabel(t: string) { return TYPE_LABELS[t] || t }
function showLoan(t: string) { return t === 'REAL_ESTATE' || t === 'VEHICLE' }
function formatMoney(fen: number) {
  return (fen / 100).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

const totalAmount = computed(() => wealthStore.familyAssets.reduce((s, a) => s + a.amount, 0))
// 显示用的资产总计：与后端 summary 保持一致，只统计非 loanOnly 的资产金额
const totalEffectiveAmount = computed(() =>
  wealthStore.familyAssets
    .filter(a => !a.loanOnly)
    .reduce((s, a) => s + a.amount, 0),
)

function openCreateDialog() {
  isEditing.value = false
  editingId.value = ''
  Object.assign(form, {
    assetName: '', assetType: '', amountYuan: 0, currency: 'CNY', remark: '',
    loanTotalYuan: 0, loanRemainingYuan: 0, loanOnly: false,
  })
  showDialog.value = true
}

function openEditDialog(asset: FamilyAsset) {
  isEditing.value = true
  editingId.value = asset.id
  Object.assign(form, {
    assetName: asset.assetName, assetType: asset.assetType,
    amountYuan: asset.amount / 100, currency: asset.currency, remark: asset.remark || '',
    loanTotalYuan: (asset.loanTotal ?? 0) / 100,
    loanRemainingYuan: (asset.loanRemaining ?? 0) / 100,
    loanOnly: !!asset.loanOnly,
  })
  showDialog.value = true
}

async function handleSubmit() {
  if (!dialogFormRef.value) return
  const valid = await dialogFormRef.value.validate().catch(() => false)
  if (!valid) return

  submitting.value = true
  try {
    const amountFen = Math.round(form.amountYuan * 100)
    const loanTotalFen = Math.round((form.loanTotalYuan || 0) * 100)
    const loanRemainingFen = Math.round((form.loanRemainingYuan || 0) * 100)
    if (isEditing.value) {
      await wealthStore.editFamilyAsset(editingId.value, {
        assetName: form.assetName, assetType: form.assetType as any,
        amount: amountFen, currency: form.currency || 'CNY', remark: form.remark,
        loanTotal: loanTotalFen, loanRemaining: loanRemainingFen, loanOnly: form.loanOnly,
      })
      ElMessage.success('资产已更新')
    } else {
      await wealthStore.addFamilyAsset({
        assetName: form.assetName, assetType: form.assetType as any,
        amount: amountFen, currency: form.currency || 'CNY', remark: form.remark,
        loanTotal: loanTotalFen, loanRemaining: loanRemainingFen, loanOnly: form.loanOnly,
      })
      ElMessage.success('资产已创建')
    }
    showDialog.value = false
  } catch (e: unknown) {
    ElMessage.error((e as { message?: string })?.message ?? '操作失败')
  } finally {
    submitting.value = false
  }
}

async function handleDelete(asset: FamilyAsset) {
  try {
    await wealthStore.removeFamilyAsset(asset.id)
    ElMessage.success('资产已删除')
  } catch (e: unknown) {
    ElMessage.error((e as { message?: string })?.message ?? '删除失败')
  }
}

onMounted(async () => {
  if (!authStore.user?.familyId) return
  pageLoading.value = true
  try {
    await familyStore.fetchFamily(authStore.user.familyId)
    await wealthStore.fetchFamilyAssets()
  } finally {
    pageLoading.value = false
  }
})
</script>

<style scoped>
.family-assets {
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
