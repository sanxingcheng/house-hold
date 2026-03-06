import { defineStore } from 'pinia'
import { ref } from 'vue'
import { getFamily, createFamily, joinFamily, updateFamily } from '@/api/family'
import type { FamilyResponse, FamilyCreateRequest, FamilyJoinRequest } from '@/types/family'
import { useAuthStore } from '@/stores/auth'

export const useFamilyStore = defineStore('family', () => {
  const family = ref<FamilyResponse | null>(null)
  const members = ref<FamilyResponse['members']>([])

  function setFamily(data: FamilyResponse | null) {
    family.value = data
    members.value = data?.members ?? []
  }

  async function fetchFamily(familyId: string) {
    const { data } = await getFamily(familyId)
    setFamily(data)
    return data
  }

  async function create(data: FamilyCreateRequest) {
    const { data: res } = await createFamily(data)
    setFamily(res)
    const authStore = useAuthStore()
    authStore.setFamilyId(res.id)
    return res
  }

  async function join(payload: FamilyJoinRequest) {
    const { data: res } = await joinFamily(payload)
    setFamily(res)
    const authStore = useAuthStore()
    authStore.setFamilyId(res.id)
    return res
  }

  async function updateAddress(familyId: string, payload: FamilyCreateRequest) {
    const { data: res } = await updateFamily(familyId, payload)
    setFamily(res)
    return res
  }

  function clear() {
    family.value = null
    members.value = []
  }

  return {
    family,
    members,
    setFamily,
    fetchFamily,
    create,
    join,
    updateAddress,
    clear,
  }
})
