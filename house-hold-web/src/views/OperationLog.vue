<template>
  <MainLayout>
    <div class="operation-log" v-loading="loading">
      <div class="page-header">
        <h1 class="page-title">操作日志</h1>
      </div>

      <el-empty v-if="!authStore.user?.familyId" description="请先加入家庭后查看操作日志" />

      <template v-else>
        <el-alert v-if="!familyStore.isAdmin" type="info" show-icon style="margin-bottom:16px">
          仅户主/管理员可查看家庭操作日志。
        </el-alert>

        <template v-else-if="list.length === 0 && !loading">
          <el-empty description="暂无操作记录" />
        </template>

        <template v-else>
          <el-table
            :data="list"
            stripe
            class="responsive-table"
            :border="true"
          >
            <el-table-column prop="createdAt" label="时间" min-width="160" resizable>
              <template #default="{ row }">{{ formatTime(row.createdAt) }}</template>
            </el-table-column>
            <el-table-column prop="action" label="操作" min-width="80" resizable>
              <template #default="{ row }">
                <el-tag :type="actionTagType(row.action)" size="small">{{ actionLabel(row.action) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="resourceType" label="资源类型" min-width="90" resizable>
              <template #default="{ row }">{{ resourceTypeLabel(row.resourceType) }}</template>
            </el-table-column>
            <el-table-column prop="resourceId" label="资源ID" min-width="110" show-overflow-tooltip resizable />
            <el-table-column prop="detail" label="详情" min-width="140" show-overflow-tooltip resizable>
              <template #default="{ row }">{{ row.detail || '—' }}</template>
            </el-table-column>
          </el-table>

          <el-pagination
            v-model:current-page="currentPage"
            :page-size="pageSize"
            :total="total"
            layout="total, prev, pager, next"
            class="pagination"
            @current-change="fetchLogs"
          />
        </template>
      </template>
    </div>
  </MainLayout>
</template>

<script setup lang="ts">
import { ref, onMounted, watch } from 'vue'
import MainLayout from '@/layouts/MainLayout.vue'
import { useAuthStore } from '@/stores/auth'
import { useFamilyStore } from '@/stores/family'
import { getOperationLogs, type OperationLogItem } from '@/api/wealth'

const authStore = useAuthStore()
const familyStore = useFamilyStore()

const loading = ref(false)
const list = ref<OperationLogItem[]>([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = 20

function actionLabel(a: string) {
  const map: Record<string, string> = { CREATE: '创建', UPDATE: '更新', DELETE: '删除' }
  return map[a] ?? a
}
function actionTagType(a: string) {
  if (a === 'CREATE') return 'success'
  if (a === 'DELETE') return 'danger'
  return 'warning'
}
function resourceTypeLabel(t: string) {
  const map: Record<string, string> = { FAMILY: '家庭', ACCOUNT: '账户', MEMBER: '成员', ASSET: '资产' }
  return map[t] ?? t
}
function formatTime(s?: string | null) {
  if (!s) return '—'
  return s.replace('T', ' ').slice(0, 19)
}

async function fetchLogs() {
  if (!authStore.user?.familyId) return
  loading.value = true
  try {
    const familyId = authStore.user?.familyId ?? undefined
    const res = await getOperationLogs(currentPage.value - 1, pageSize, familyId)
    list.value = res.data.content ?? []
    total.value = res.data.totalElements ?? 0
  } catch {
    list.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

onMounted(async () => {
  const fid = authStore.user?.familyId
  if (!fid) return
  await familyStore.fetchFamily(fid)
  if (familyStore.isAdmin) fetchLogs()
})
watch(() => authStore.user?.familyId, async (fid) => {
  if (!fid) return
  await familyStore.fetchFamily(fid)
  if (familyStore.isAdmin) fetchLogs()
})
</script>

<style scoped>
.operation-log {
  margin: 0 auto;
}
.page-header {
  margin-bottom: 20px;
}
.page-title {
  margin: 0;
  font-size: 1.5rem;
}
.responsive-table {
  width: 100%;
}
.pagination {
  margin-top: 16px;
  justify-content: flex-end;
}

/* 响应式样式 */
@media screen and (max-width: 768px) {
  .page-title {
    font-size: 1.25rem;
  }
  .pagination {
    justify-content: center;
  }
}

@media screen and (max-width: 480px) {
  .page-title {
    font-size: 1.1rem;
  }
  .page-header {
    margin-bottom: 16px;
  }
  .pagination {
    margin-top: 12px;
  }
}
</style>
