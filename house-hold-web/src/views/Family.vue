<template>
  <MainLayout>
    <div class="family">
      <h1>家庭</h1>

      <!-- 未加入家庭 -->
      <template v-if="!authStore.user?.familyId">
        <div class="tabs">
          <button :class="{ active: tab === 'create' }" @click="tab = 'create'">创建家庭</button>
          <button :class="{ active: tab === 'join' }" @click="tab = 'join'">加入家庭</button>
        </div>

        <form v-if="tab === 'create'" @submit.prevent="onCreate" class="form">
          <div class="field">
            <label>家庭别名</label>
            <input v-model="createForm.nameAlias" required placeholder="如：我家" />
          </div>
          <div class="field">
            <label>国家</label>
            <input v-model="createForm.country" required />
          </div>
          <div class="field">
            <label>省份</label>
            <input v-model="createForm.province" required />
          </div>
          <div class="field">
            <label>城市</label>
            <input v-model="createForm.city" required />
          </div>
          <div class="field">
            <label>街道</label>
            <input v-model="createForm.street" required />
          </div>
          <div class="field">
            <label>我的角色</label>
            <select v-model="createForm.role">
              <option value="HUSBAND">丈夫</option>
              <option value="WIFE">妻子</option>
              <option value="CHILD">子女</option>
              <option value="OTHER">其他</option>
            </select>
          </div>
          <p v-if="createMessage" class="message error">{{ createMessage }}</p>
          <button type="submit" class="btn">创建</button>
        </form>

        <form v-if="tab === 'join'" @submit.prevent="onJoin" class="form">
          <div class="field">
            <label>家庭 ID</label>
            <input v-model="joinForm.familyId" required placeholder="请输入要加入的家庭 ID" />
          </div>
          <div class="field">
            <label>我的角色</label>
            <select v-model="joinForm.role">
              <option value="HUSBAND">丈夫</option>
              <option value="WIFE">妻子</option>
              <option value="CHILD">子女</option>
              <option value="OTHER">其他</option>
            </select>
          </div>
          <p v-if="joinMessage" class="message error">{{ joinMessage }}</p>
          <button type="submit" class="btn">加入</button>
        </form>
      </template>

      <!-- 已加入家庭 -->
      <template v-else>
        <template v-if="familyStore.family">
          <div class="card">
            <h2>家庭信息</h2>
            <p><strong>别名：</strong>{{ familyStore.family.nameAlias }}</p>
            <p><strong>地址：</strong>{{ familyStore.family.country }} {{ familyStore.family.province }} {{ familyStore.family.city }} {{ familyStore.family.street }}</p>
            <button v-if="!editing" type="button" class="btn secondary" @click="startEdit">编辑地址</button>
            <form v-else @submit.prevent="onUpdateAddress" class="form inline">
              <div class="field">
                <label>家庭别名</label>
                <input v-model="updateForm.nameAlias" required />
              </div>
              <div class="field">
                <label>国家</label>
                <input v-model="updateForm.country" required />
              </div>
              <div class="field">
                <label>省份</label>
                <input v-model="updateForm.province" required />
              </div>
              <div class="field">
                <label>城市</label>
                <input v-model="updateForm.city" required />
              </div>
              <div class="field">
                <label>街道</label>
                <input v-model="updateForm.street" required />
              </div>
              <button type="submit" class="btn">保存</button>
              <button type="button" class="btn secondary" @click="editing = false">取消</button>
            </form>
          </div>
          <div class="card">
            <h2>家庭成员</h2>
            <table class="members">
              <thead>
                <tr>
                  <th>用户名</th>
                  <th>姓名</th>
                  <th>角色</th>
                  <th>操作</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="m in familyStore.members" :key="m.userId">
                  <td>{{ m.username }}</td>
                  <td>{{ m.name || '—' }}</td>
                  <td>
                    <template v-if="editingRoleUserId === m.userId">
                      <select v-model="editingRole" class="role-select">
                        <option value="HUSBAND">丈夫</option>
                        <option value="WIFE">妻子</option>
                        <option value="CHILD">子女</option>
                        <option value="OTHER">其他</option>
                      </select>
                      <button type="button" class="btn-sm" @click="saveRole(m.userId)">保存</button>
                      <button type="button" class="btn-sm secondary" @click="editingRoleUserId = null">取消</button>
                    </template>
                    <template v-else>
                      {{ roleLabel(m.role) }}
                      <button v-if="m.userId === authStore.user?.id" type="button" class="btn-sm secondary" @click="startEditRole(m)">改</button>
                    </template>
                  </td>
                  <td></td>
                </tr>
              </tbody>
            </table>
          </div>
        </template>
        <p v-else class="hint">加载中…</p>
      </template>
    </div>
  </MainLayout>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, watch } from 'vue'
import MainLayout from '@/layouts/MainLayout.vue'
import { useAuthStore } from '@/stores/auth'
import { useFamilyStore } from '@/stores/family'
import { updateMemberRole } from '@/api/family'
import type { FamilyCreateRequest } from '@/types/family'

const authStore = useAuthStore()
const familyStore = useFamilyStore()
const tab = ref<'create' | 'join'>('create')
const createMessage = ref('')
const joinMessage = ref('')
const editing = ref(false)

const createForm = reactive<FamilyCreateRequest>({
  nameAlias: '',
  country: '',
  province: '',
  city: '',
  street: '',
  role: 'OTHER',
})
const joinForm = reactive({ familyId: '', role: 'OTHER' })
const editingRoleUserId = ref<string | null>(null)
const editingRole = ref('OTHER')
const updateForm = reactive<FamilyCreateRequest>({
  nameAlias: '',
  country: '',
  province: '',
  city: '',
  street: '',
})

function roleLabel(role: string) {
  const map: Record<string, string> = {
    HUSBAND: '丈夫',
    WIFE: '妻子',
    CHILD: '子女',
    OTHER: '其他',
  }
  return map[role] ?? role
}

async function loadFamily() {
  const fid = authStore.user?.familyId
  if (fid) await familyStore.fetchFamily(fid)
}

async function onCreate() {
  createMessage.value = ''
  try {
    await familyStore.create(createForm)
    createForm.nameAlias = createForm.country = createForm.province = createForm.city = createForm.street = ''
    createForm.role = 'OTHER'
  } catch (e: unknown) {
    createMessage.value = (e as { message?: string })?.message ?? '创建失败'
  }
}

async function onJoin() {
  joinMessage.value = ''
  try {
    await familyStore.join({ familyId: joinForm.familyId, role: joinForm.role })
    joinForm.familyId = ''
    joinForm.role = 'OTHER'
  } catch (e: unknown) {
    joinMessage.value = (e as { message?: string })?.message ?? '加入失败'
  }
}

function startEditRole(m: { userId: string; role: string }) {
  editingRoleUserId.value = m.userId
  editingRole.value = m.role
}

async function saveRole(userId: string) {
  const fid = familyStore.family?.id
  if (!fid) return
  try {
    const { data } = await updateMemberRole(fid, { role: editingRole.value })
    familyStore.setFamily(data)
    editingRoleUserId.value = null
  } catch (_) {}
}

async function onUpdateAddress() {
  if (!familyStore.family) return
  try {
    await familyStore.updateAddress(familyStore.family.id, updateForm)
    editing.value = false
  } catch (_) {}
}

onMounted(loadFamily)
watch(() => authStore.user?.familyId, loadFamily)
</script>

<style scoped>
.family { max-width: 720px; margin: 0 auto; }
.tabs { display: flex; gap: 0.5rem; margin-bottom: 1rem; }
.tabs button { padding: 0.5rem 1rem; border: 1px solid #ddd; background: #fff; cursor: pointer; border-radius: 4px; }
.tabs button.active { background: #333; color: #fff; border-color: #333; }
.form { display: flex; flex-direction: column; gap: 1rem; }
.form.inline { gap: 0.75rem; }
.field { display: flex; flex-direction: column; gap: 0.25rem; }
.field label { font-weight: 500; color: #333; }
.field input { padding: 0.5rem; border: 1px solid #ddd; border-radius: 4px; }
.message { margin: 0; font-size: 0.9rem; }
.message.error { color: #c00; }
.btn { padding: 0.5rem 1rem; align-self: flex-start; cursor: pointer; border-radius: 4px; background: #333; color: #fff; border: none; }
.btn:hover { background: #555; }
.btn.secondary { background: #eee; color: #333; }
.btn.secondary:hover { background: #ddd; }
.card { background: #fff; padding: 1.25rem; border-radius: 8px; box-shadow: 0 1px 3px rgba(0,0,0,0.08); margin-bottom: 1rem; }
.card h2 { margin: 0 0 0.75rem 0; font-size: 1.1rem; }
.members { width: 100%; border-collapse: collapse; }
.members th, .members td { padding: 0.5rem; text-align: left; border-bottom: 1px solid #eee; }
.members th { font-weight: 600; color: #555; }
.hint { color: #666; }
.role-select { padding: 0.2rem 0.4rem; border: 1px solid #ddd; border-radius: 4px; font-size: 0.85rem; margin-right: 0.25rem; }
.btn-sm { padding: 0.2rem 0.5rem; font-size: 0.8rem; cursor: pointer; border-radius: 4px; background: #333; color: #fff; border: none; margin-right: 0.25rem; }
.btn-sm.secondary { background: #eee; color: #333; }
.btn-sm:hover { opacity: 0.85; }
.field select { padding: 0.5rem; border: 1px solid #ddd; border-radius: 4px; }
</style>
