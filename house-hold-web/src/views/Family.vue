<template>
  <MainLayout>
    <div class="family" v-loading="pageLoading">
      <h1 style="margin:0 0 24px">家庭</h1>

      <!-- 未加入家庭 -->
      <template v-if="!authStore.user?.familyId">
        <el-card shadow="never" style="margin-bottom:16px">
          <el-tabs v-model="tab">
            <el-tab-pane label="创建家庭" name="create">
              <el-form ref="createFormRef" :model="createForm" :rules="createRules" label-width="80px" @submit.prevent="onCreate">
                <el-form-item label="家庭别名" prop="nameAlias">
                  <el-input v-model="createForm.nameAlias" placeholder="如：我家" />
                </el-form-item>
                <el-form-item label="国家" prop="country">
                  <el-select v-model="createForm.country" placeholder="请选择国家" style="width:100%" filterable>
                    <el-option v-for="c in COUNTRY_OPTIONS" :key="c.value" :label="c.label" :value="c.value" />
                  </el-select>
                </el-form-item>
                <el-form-item label="省份" prop="province">
                  <el-select v-model="createForm.province" placeholder="请选择省份" style="width:100%" filterable :disabled="!createForm.country">
                    <el-option v-for="p in createProvinceOptions" :key="p.value" :label="p.label" :value="p.value" />
                  </el-select>
                </el-form-item>
                <el-form-item label="城市" prop="city">
                  <el-select v-model="createForm.city" placeholder="请选择城市" style="width:100%" filterable :disabled="!createForm.province">
                    <el-option v-for="ct in createCityOptions" :key="ct.value" :label="ct.label" :value="ct.value" />
                  </el-select>
                </el-form-item>
                <el-form-item label="街道" prop="street">
                  <el-input v-model="createForm.street" />
                </el-form-item>
                <el-form-item label="我的角色">
                  <el-select v-model="createForm.role" style="width:100%">
                    <el-option label="丈夫" value="HUSBAND" />
                    <el-option label="妻子" value="WIFE" />
                    <el-option label="子女" value="CHILD" />
                    <el-option label="其他" value="OTHER" />
                  </el-select>
                </el-form-item>
                <el-form-item>
                  <el-button type="primary" native-type="submit" :loading="creating">创建</el-button>
                </el-form-item>
              </el-form>
            </el-tab-pane>

            <el-tab-pane label="申请加入" name="apply">
              <el-form :model="applyForm" label-width="80px" @submit.prevent="onApply">
                <el-form-item label="家庭 ID">
                  <el-input v-model="applyForm.familyId" placeholder="请输入要申请加入的家庭 ID" />
                </el-form-item>
                <el-form-item label="我的角色">
                  <el-select v-model="applyForm.role" style="width:100%">
                    <el-option label="丈夫" value="HUSBAND" />
                    <el-option label="妻子" value="WIFE" />
                    <el-option label="子女" value="CHILD" />
                    <el-option label="其他" value="OTHER" />
                  </el-select>
                </el-form-item>
                <el-form-item>
                  <el-button type="primary" native-type="submit" :loading="applying">提交申请</el-button>
                </el-form-item>
              </el-form>
            </el-tab-pane>
          </el-tabs>
        </el-card>

        <!-- 收到的邀请 -->
        <el-card v-if="familyStore.myInvitations.length > 0" shadow="never" style="margin-bottom:16px">
          <template #header><span style="font-weight:600">收到的邀请</span></template>
          <el-table :data="familyStore.myInvitations" stripe>
            <el-table-column prop="familyName" label="家庭名称" />
            <el-table-column prop="initiatedByUsername" label="邀请人" />
            <el-table-column label="角色"><template #default="{ row }">{{ roleLabel(row.role) }}</template></el-table-column>
            <el-table-column label="操作" width="160">
              <template #default="{ row }">
                <el-button size="small" type="primary" :loading="acceptingId === row.id" :disabled="rejectingId === row.id" @click="onAcceptInvite(row.id)">接受</el-button>
                <el-button size="small" :loading="rejectingId === row.id" :disabled="acceptingId === row.id" @click="onRejectInvite(row.id)">拒绝</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-card>

        <!-- 我的申请进度 -->
        <el-card v-if="familyStore.myApplications.length > 0" shadow="never">
          <template #header><span style="font-weight:600">我的申请</span></template>
          <el-table :data="familyStore.myApplications" stripe>
            <el-table-column prop="familyName" label="申请家庭" />
            <el-table-column label="期望角色"><template #default="{ row }">{{ roleLabel(row.role) }}</template></el-table-column>
            <el-table-column label="申请时间">
              <template #default="{ row }">{{ formatTime(row.createdAt) }}</template>
            </el-table-column>
            <el-table-column label="处理状态" width="120">
              <template #default="{ row }">
                <el-tag :type="statusTagType(row.status)" size="small">{{ statusLabel(row.status) }}</el-tag>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </template>

      <!-- 已加入家庭 -->
      <template v-else-if="familyStore.family">
        <!-- 家庭信息卡片 -->
        <el-card shadow="never" style="margin-bottom:16px">
          <template #header>
            <div style="display:flex;justify-content:space-between;align-items:center">
              <span style="font-weight:600">家庭信息</span>
              <el-button v-if="canManage && !editing" size="small" @click="startEdit">编辑</el-button>
            </div>
          </template>
          <template v-if="!editing">
            <el-descriptions :column="1" border>
              <el-descriptions-item label="家庭 ID">{{ familyStore.family.id }}</el-descriptions-item>
              <el-descriptions-item label="别名">{{ familyStore.family.nameAlias }}</el-descriptions-item>
              <el-descriptions-item label="地址">{{ familyStore.family.country }} {{ familyStore.family.province }} {{ familyStore.family.city }} {{ familyStore.family.street }}</el-descriptions-item>
            </el-descriptions>
          </template>
          <el-form v-else :model="updateForm" label-width="80px" @submit.prevent="onUpdateAddress">
            <el-form-item label="家庭别名"><el-input v-model="updateForm.nameAlias" /></el-form-item>
            <el-form-item label="国家">
              <el-select v-model="updateForm.country" placeholder="请选择国家" style="width:100%" filterable>
                <el-option v-for="c in COUNTRY_OPTIONS" :key="c.value" :label="c.label" :value="c.value" />
              </el-select>
            </el-form-item>
            <el-form-item label="省份">
              <el-select v-model="updateForm.province" placeholder="请选择省份" style="width:100%" filterable :disabled="!updateForm.country">
                <el-option v-for="p in updateProvinceOptions" :key="p.value" :label="p.label" :value="p.value" />
              </el-select>
            </el-form-item>
            <el-form-item label="城市">
              <el-select v-model="updateForm.city" placeholder="请选择城市" style="width:100%" filterable :disabled="!updateForm.province">
                <el-option v-for="ct in updateCityOptions" :key="ct.value" :label="ct.label" :value="ct.value" />
              </el-select>
            </el-form-item>
            <el-form-item label="街道"><el-input v-model="updateForm.street" /></el-form-item>
            <el-form-item>
              <el-button type="primary" native-type="submit">保存</el-button>
              <el-button @click="editing = false">取消</el-button>
            </el-form-item>
          </el-form>
        </el-card>

        <!-- 家庭成员 -->
        <el-card shadow="never" style="margin-bottom:16px">
          <template #header>
            <div style="display:flex;justify-content:space-between;align-items:center">
              <span style="font-weight:600">家庭成员</span>
              <div v-if="canManage" style="display:flex;gap:8px">
                <el-button size="small" type="primary" @click="showInviteDialog = true">邀请用户</el-button>
                <el-button size="small" type="success" @click="showCreateMemberDialog = true">新建成员</el-button>
              </div>
            </div>
          </template>
          <el-table :data="familyStore.members" stripe>
            <el-table-column prop="username" label="用户名" />
            <el-table-column prop="name" label="姓名">
              <template #default="{ row }">{{ row.name || '—' }}</template>
            </el-table-column>
            <el-table-column label="角色" width="200">
              <template #default="{ row }">
                <template v-if="editingRoleUserId === row.userId">
                  <el-select v-model="editingRole" size="small" style="width:90px;margin-right:8px">
                    <el-option label="丈夫" value="HUSBAND" />
                    <el-option label="妻子" value="WIFE" />
                    <el-option label="子女" value="CHILD" />
                    <el-option label="其他" value="OTHER" />
                  </el-select>
                  <el-button size="small" type="primary" @click="saveRole(row.userId)">保存</el-button>
                  <el-button size="small" @click="editingRoleUserId = null">取消</el-button>
                </template>
                <template v-else>
                  <el-tag :type="roleTagType(row.role)" size="small">{{ roleLabel(row.role) }}</el-tag>
                  <el-button v-if="row.userId === authStore.user?.id" size="small" link type="primary" style="margin-left:8px" @click="startEditRole(row)">修改</el-button>
                </template>
              </template>
            </el-table-column>
            <el-table-column label="身份" width="120">
              <template #default="{ row }">
                <el-tag v-if="row.isCreator" type="danger" size="small">户主</el-tag>
                <el-tag v-else-if="row.isAdmin" type="warning" size="small">管理员</el-tag>
                <el-tag v-else type="info" size="small">成员</el-tag>
              </template>
            </el-table-column>
            <el-table-column v-if="canManage" label="操作" width="200">
              <template #default="{ row }">
                <template v-if="row.userId !== authStore.user?.id && !row.isCreator">
                  <el-button v-if="familyStore.isCreator && !row.isAdmin" size="small" link type="primary" @click="onSetAdmin(row.userId, true)">设为管理员</el-button>
                  <el-button v-if="familyStore.isCreator && row.isAdmin" size="small" link type="warning" @click="onSetAdmin(row.userId, false)">取消管理员</el-button>
                  <el-popconfirm :title="`确定移除「${row.username}」？`" @confirm="onRemoveMember(row.userId)">
                    <template #reference>
                      <el-button size="small" link type="danger">移除</el-button>
                    </template>
                  </el-popconfirm>
                </template>
              </template>
            </el-table-column>
          </el-table>
        </el-card>

        <!-- 管理员/户主: 待审批请求 -->
        <el-card v-if="canManage" shadow="never" style="margin-bottom:16px">
          <template #header>
            <div style="display:flex;justify-content:space-between;align-items:center">
              <span style="font-weight:600">待审批请求</span>
              <el-tag v-if="familyStore.pendingRequests.length > 0" type="danger" size="small">{{ familyStore.pendingRequests.length }} 条待处理</el-tag>
            </div>
          </template>
          <template v-if="familyStore.pendingRequests.length > 0">
            <el-table :data="familyStore.pendingRequests" stripe>
              <el-table-column prop="username" label="用户名" />
              <el-table-column label="类型">
                <template #default="{ row }">{{ row.requestType === 'APPLY' ? '主动申请' : '管理员邀请' }}</template>
              </el-table-column>
              <el-table-column label="期望角色">
                <template #default="{ row }">{{ roleLabel(row.role) }}</template>
              </el-table-column>
              <el-table-column label="操作" width="160">
                <template #default="{ row }">
                  <el-button size="small" type="primary" @click="onApproveRequest(row.id)">通过</el-button>
                  <el-button size="small" @click="onRejectRequestAdmin(row.id)">拒绝</el-button>
                </template>
              </el-table-column>
            </el-table>
          </template>
          <el-empty v-else description="暂无待审批请求" :image-size="64" />
        </el-card>
      </template>

      <!-- 邀请用户对话框 -->
      <el-dialog v-model="showInviteDialog" title="邀请用户加入家庭" width="440px" destroy-on-close>
        <el-form :model="inviteForm" label-width="80px">
          <el-form-item label="用户名">
            <el-input v-model="inviteForm.username" placeholder="请输入用户名" />
          </el-form-item>
          <el-form-item label="角色">
            <el-select v-model="inviteForm.role" style="width:100%">
              <el-option label="丈夫" value="HUSBAND" />
              <el-option label="妻子" value="WIFE" />
              <el-option label="子女" value="CHILD" />
              <el-option label="其他" value="OTHER" />
            </el-select>
          </el-form-item>
        </el-form>
        <template #footer>
          <el-button @click="showInviteDialog = false">取消</el-button>
          <el-button type="primary" :loading="inviting" @click="onInviteUser">发送邀请</el-button>
        </template>
      </el-dialog>

      <!-- 新建成员对话框 -->
      <el-dialog v-model="showCreateMemberDialog" title="创建新成员" width="480px" destroy-on-close>
        <el-form ref="createMemberFormRef" :model="createMemberForm" :rules="createMemberRules" label-width="80px">
          <el-form-item label="用户名" prop="username">
            <el-input v-model="createMemberForm.username" />
          </el-form-item>
          <el-form-item label="密码" prop="password">
            <el-input v-model="createMemberForm.password" type="password" show-password />
          </el-form-item>
          <el-form-item label="姓名" prop="name">
            <el-input v-model="createMemberForm.name" />
          </el-form-item>
          <el-form-item label="性别" prop="gender">
            <el-radio-group v-model="createMemberForm.gender">
              <el-radio value="MALE">男</el-radio>
              <el-radio value="FEMALE">女</el-radio>
            </el-radio-group>
          </el-form-item>
          <el-form-item label="生日" prop="birthday">
            <el-date-picker v-model="createMemberForm.birthday" type="date" value-format="YYYY-MM-DD" style="width:100%" />
          </el-form-item>
          <el-form-item label="角色">
            <el-select v-model="createMemberForm.role" style="width:100%">
              <el-option label="丈夫" value="HUSBAND" />
              <el-option label="妻子" value="WIFE" />
              <el-option label="子女" value="CHILD" />
              <el-option label="其他" value="OTHER" />
            </el-select>
          </el-form-item>
          <el-form-item label="邮箱">
            <el-input v-model="createMemberForm.email" />
          </el-form-item>
          <el-form-item label="手机">
            <el-input v-model="createMemberForm.phone" />
          </el-form-item>
        </el-form>
        <template #footer>
          <el-button @click="showCreateMemberDialog = false">取消</el-button>
          <el-button type="primary" :loading="creatingMember" @click="onCreateMember">创建</el-button>
        </template>
      </el-dialog>
    </div>
  </MainLayout>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, watch, nextTick } from 'vue'
import { storeToRefs } from 'pinia'
import type { FormInstance, FormRules } from 'element-plus'
import { ElMessage } from 'element-plus'
import MainLayout from '@/layouts/MainLayout.vue'
import { useAuthStore } from '@/stores/auth'
import { useFamilyStore } from '@/stores/family'
import { updateMemberRole } from '@/api/family'
import type { FamilyCreateRequest } from '@/types/family'
import { COUNTRY_OPTIONS, getProvinces, getCities } from '@/data/region-cn'

const authStore = useAuthStore()
const familyStore = useFamilyStore()
const { isAdmin, isCreator } = storeToRefs(familyStore)
const tab = ref('create')
const editing = ref(false)
const pageLoading = ref(false)
const createFormRef = ref<FormInstance>()
const createMemberFormRef = ref<FormInstance>()
const creating = ref(false)
const applying = ref(false)
const acceptingId = ref<string | null>(null)
const rejectingId = ref<string | null>(null)
const inviting = ref(false)
const creatingMember = ref(false)

const createForm = reactive<FamilyCreateRequest>({
  nameAlias: '', country: '', province: '', city: '', street: '', role: 'OTHER',
})
const applyForm = reactive({ familyId: '', role: 'OTHER' })
const updateForm = reactive<FamilyCreateRequest>({
  nameAlias: '', country: '', province: '', city: '', street: '',
})

const createProvinceOptions = computed(() => getProvinces(createForm.country))
const createCityOptions = computed(() => getCities(createForm.country, createForm.province))
const updateProvinceOptions = computed(() => getProvinces(updateForm.country))
const updateCityOptions = computed(() => getCities(updateForm.country, updateForm.province))

watch(() => createForm.country, () => { createForm.province = ''; createForm.city = '' })
watch(() => createForm.province, () => { createForm.city = '' })
const skipUpdateWatch = ref(false)
watch(() => updateForm.country, () => { if (!skipUpdateWatch.value) { updateForm.province = ''; updateForm.city = '' } })
watch(() => updateForm.province, () => { if (!skipUpdateWatch.value) { updateForm.city = '' } })

const canManage = computed(() => isAdmin.value || isCreator.value)

const editingRoleUserId = ref<string | null>(null)
const editingRole = ref('OTHER')

const showInviteDialog = ref(false)
const inviteForm = reactive({ username: '', role: 'OTHER' })
const showCreateMemberDialog = ref(false)
const createMemberForm = reactive({
  username: '', password: '', name: '', gender: '', birthday: '',
  role: 'OTHER', email: '', phone: '',
})

const createRules: FormRules = {
  nameAlias: [{ required: true, message: '请输入家庭别名', trigger: 'blur' }],
  country: [{ required: true, message: '请选择国家', trigger: 'change' }],
  province: [{ required: true, message: '请选择省份', trigger: 'change' }],
  city: [{ required: true, message: '请选择城市', trigger: 'change' }],
  street: [{ required: true, message: '请输入街道', trigger: 'blur' }],
}

const createMemberRules: FormRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
  name: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  gender: [{ required: true, message: '请选择性别', trigger: 'change' }],
  birthday: [{ required: true, message: '请选择生日', trigger: 'change' }],
}

const ROLE_MAP: Record<string, string> = { HUSBAND: '丈夫', WIFE: '妻子', CHILD: '子女', OTHER: '其他' }
function roleLabel(role: string) { return ROLE_MAP[role] ?? role }
function roleTagType(role: string) {
  const map: Record<string, '' | 'success' | 'warning' | 'info'> = { HUSBAND: '', WIFE: 'success', CHILD: 'warning', OTHER: 'info' }
  return map[role] ?? 'info'
}

const STATUS_MAP: Record<string, string> = { PENDING: '待审批', APPROVED: '已通过', REJECTED: '已拒绝' }
function statusLabel(status: string) { return STATUS_MAP[status] ?? status }
function statusTagType(status: string): '' | 'success' | 'danger' | 'warning' {
  const map: Record<string, '' | 'success' | 'danger' | 'warning'> = { PENDING: 'warning', APPROVED: 'success', REJECTED: 'danger' }
  return map[status] ?? ''
}

function formatTime(dt: string) {
  if (!dt) return '—'
  return dt.replace('T', ' ').slice(0, 16)
}

function startEdit() {
  const f = familyStore.family!
  skipUpdateWatch.value = true
  Object.assign(updateForm, { nameAlias: f.nameAlias, country: f.country, province: f.province, city: f.city, street: f.street })
  nextTick(() => { skipUpdateWatch.value = false })
  editing.value = true
}

function startEditRole(m: { userId: string; role: string }) {
  editingRoleUserId.value = m.userId
  editingRole.value = m.role
}

async function loadFamily() {
  const fid = authStore.user?.familyId
  if (fid) {
    pageLoading.value = true
    try {
      await familyStore.fetchFamily(fid)
      if (isAdmin.value || isCreator.value) {
        await familyStore.fetchPendingRequests(fid)
      }
    } finally {
      pageLoading.value = false
    }
  } else {
    await Promise.all([
      familyStore.fetchMyInvitations(),
      familyStore.fetchMyApplications(),
    ])
  }
}

async function onCreate() {
  if (createFormRef.value) {
    const valid = await createFormRef.value.validate().catch(() => false)
    if (!valid) return
  }
  if (creating.value) return
  creating.value = true
  try {
    await familyStore.create(createForm)
    Object.assign(createForm, { nameAlias: '', country: '', province: '', city: '', street: '', role: 'OTHER' })
    ElMessage.success('家庭创建成功')
  } catch (e: unknown) {
    ElMessage.error((e as { message?: string })?.message ?? '创建失败')
  } finally {
    creating.value = false
  }
}

async function onApply() {
  if (!applyForm.familyId) {
    ElMessage.warning('请输入家庭 ID')
    return
  }
  if (applying.value) return
  applying.value = true
  try {
    await familyStore.applyJoin(applyForm.familyId, { role: applyForm.role })
    applyForm.familyId = ''
    applyForm.role = 'OTHER'
    ElMessage.success('申请已提交，请等待管理员审批')
  } catch (e: unknown) {
    ElMessage.error((e as { message?: string })?.message ?? '申请失败')
  } finally {
    applying.value = false
  }
}

async function onAcceptInvite(reqId: string) {
  if (acceptingId.value) return
  acceptingId.value = reqId
  try {
    await familyStore.handleAcceptInvitation(reqId)
    ElMessage.success('已接受邀请')
    window.location.reload()
  } catch (e: unknown) {
    ElMessage.error((e as { message?: string })?.message ?? '操作失败')
  } finally {
    acceptingId.value = null
  }
}

async function onRejectInvite(reqId: string) {
  if (rejectingId.value) return
  rejectingId.value = reqId
  try {
    await familyStore.handleRejectInvitation(reqId)
    ElMessage.success('已拒绝邀请')
  } catch (e: unknown) {
    ElMessage.error((e as { message?: string })?.message ?? '操作失败')
  } finally {
    rejectingId.value = null
  }
}

async function saveRole(_userId: string) {
  const fid = familyStore.family?.id
  if (!fid) return
  try {
    const { data } = await updateMemberRole(fid, { role: editingRole.value })
    familyStore.setFamily(data)
    editingRoleUserId.value = null
    ElMessage.success('角色已更新')
  } catch {
    ElMessage.error('更新失败')
  }
}

async function onUpdateAddress() {
  if (!familyStore.family) return
  try {
    await familyStore.updateAddress(familyStore.family.id, updateForm)
    editing.value = false
    ElMessage.success('地址已更新')
  } catch {
    ElMessage.error('更新失败')
  }
}

async function onInviteUser() {
  if (!inviteForm.username) {
    ElMessage.warning('请输入用户名')
    return
  }
  if (inviting.value) return
  inviting.value = true
  try {
    await familyStore.inviteUser(familyStore.family!.id, { username: inviteForm.username, role: inviteForm.role })
    showInviteDialog.value = false
    inviteForm.username = ''
    inviteForm.role = 'OTHER'
    ElMessage.success('邀请已发送')
  } catch (e: unknown) {
    ElMessage.error((e as { message?: string })?.message ?? '邀请失败')
  } finally {
    inviting.value = false
  }
}

async function onCreateMember() {
  if (createMemberFormRef.value) {
    const valid = await createMemberFormRef.value.validate().catch(() => false)
    if (!valid) return
  }
  if (creatingMember.value) return
  creatingMember.value = true
  try {
    await familyStore.createMember(familyStore.family!.id, {
      username: createMemberForm.username,
      password: createMemberForm.password,
      name: createMemberForm.name,
      gender: createMemberForm.gender,
      birthday: createMemberForm.birthday,
      email: createMemberForm.email || undefined,
      phone: createMemberForm.phone || undefined,
      role: createMemberForm.role,
    })
    showCreateMemberDialog.value = false
    Object.assign(createMemberForm, { username: '', password: '', name: '', gender: '', birthday: '', role: 'OTHER', email: '', phone: '' })
    ElMessage.success('成员已创建')
  } catch (e: unknown) {
    ElMessage.error((e as { message?: string })?.message ?? '创建失败')
  } finally {
    creatingMember.value = false
  }
}

async function onSetAdmin(userId: string, admin: boolean) {
  try {
    await familyStore.setAdminStatus(familyStore.family!.id, userId, admin)
    await familyStore.fetchFamily(familyStore.family!.id)
    ElMessage.success(admin ? '已设为管理员' : '已取消管理员')
  } catch (e: unknown) {
    ElMessage.error((e as { message?: string })?.message ?? '操作失败')
  }
}

async function onRemoveMember(userId: string) {
  try {
    await familyStore.removeMember(familyStore.family!.id, userId)
    await familyStore.fetchFamily(familyStore.family!.id)
    ElMessage.success('已移除成员')
  } catch (e: unknown) {
    ElMessage.error((e as { message?: string })?.message ?? '移除失败')
  }
}

async function onApproveRequest(reqId: string) {
  try {
    await familyStore.approveRequest(familyStore.family!.id, reqId)
    await familyStore.fetchFamily(familyStore.family!.id)
    ElMessage.success('已通过')
  } catch (e: unknown) {
    ElMessage.error((e as { message?: string })?.message ?? '操作失败')
  }
}

async function onRejectRequestAdmin(reqId: string) {
  try {
    await familyStore.rejectRequest(familyStore.family!.id, reqId)
    ElMessage.success('已拒绝')
  } catch (e: unknown) {
    ElMessage.error((e as { message?: string })?.message ?? '操作失败')
  }
}

onMounted(loadFamily)
watch(() => authStore.user?.familyId, loadFamily)
</script>

<style scoped>
.family {
  max-width: 900px;
  margin: 0 auto;
}
</style>
