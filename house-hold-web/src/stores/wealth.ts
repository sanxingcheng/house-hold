import { defineStore } from 'pinia'
import { ref } from 'vue'
import {
  getAccounts, createAccount, updateAccount, deleteAccount,
  getUserSummary, getFamilySummary, getUserHistory, getFamilyHistory,
  getFamilyAssets, createFamilyAsset, updateFamilyAsset, deleteFamilyAsset,
  getMemberAccounts, createMemberAccount, updateMemberAccount, deleteMemberAccount,
} from '@/api/wealth'
import type {
  Account, AccountCreateRequest, AccountUpdateRequest,
  WealthSummary, SnapshotPoint,
  FamilyAsset, FamilyAssetCreateRequest, FamilyAssetUpdateRequest,
} from '@/types/wealth'

export const useWealthStore = defineStore('wealth', () => {
  const accounts = ref<Account[]>([])
  const userSummary = ref<WealthSummary | null>(null)
  const familySummary = ref<WealthSummary | null>(null)
  const userHistory = ref<SnapshotPoint[]>([])
  const familyHistory = ref<SnapshotPoint[]>([])
  const familyAssets = ref<FamilyAsset[]>([])
  const memberAccounts = ref<Account[]>([])

  async function fetchAccounts() {
    const { data } = await getAccounts()
    accounts.value = data
    return data
  }

  async function addAccount(payload: AccountCreateRequest) {
    const { data } = await createAccount(payload)
    await fetchAccounts()
    return data
  }

  async function editAccount(id: string, payload: AccountUpdateRequest) {
    const { data } = await updateAccount(id, payload)
    await fetchAccounts()
    return data
  }

  async function removeAccount(id: string) {
    await deleteAccount(id)
    await fetchAccounts()
  }

  async function fetchUserSummary() {
    const { data } = await getUserSummary()
    userSummary.value = data
    return data
  }

  async function fetchFamilySummary() {
    try {
      const { data } = await getFamilySummary()
      familySummary.value = data
      return data
    } catch {
      familySummary.value = null
      return null
    }
  }

  async function fetchUserHistory(from: string, to: string) {
    const { data } = await getUserHistory(from, to)
    userHistory.value = data
    return data
  }

  async function fetchFamilyHistory(from: string, to: string) {
    try {
      const { data } = await getFamilyHistory(from, to)
      familyHistory.value = data
      return data
    } catch {
      familyHistory.value = []
      return []
    }
  }

  // Family assets
  async function fetchFamilyAssets() {
    const { data } = await getFamilyAssets()
    familyAssets.value = data
    return data
  }

  async function addFamilyAsset(payload: FamilyAssetCreateRequest) {
    const { data } = await createFamilyAsset(payload)
    await fetchFamilyAssets()
    return data
  }

  async function editFamilyAsset(id: string, payload: FamilyAssetUpdateRequest) {
    const { data } = await updateFamilyAsset(id, payload)
    await fetchFamilyAssets()
    return data
  }

  async function removeFamilyAsset(id: string) {
    await deleteFamilyAsset(id)
    await fetchFamilyAssets()
  }

  // Admin: member accounts
  async function fetchMemberAccounts(targetUserId: string) {
    const { data } = await getMemberAccounts(targetUserId)
    memberAccounts.value = data
    return data
  }

  async function addMemberAccount(targetUserId: string, payload: AccountCreateRequest) {
    const { data } = await createMemberAccount(targetUserId, payload)
    await fetchMemberAccounts(targetUserId)
    return data
  }

  async function editMemberAccount(targetUserId: string, accountId: string, payload: AccountUpdateRequest) {
    const { data } = await updateMemberAccount(targetUserId, accountId, payload)
    await fetchMemberAccounts(targetUserId)
    return data
  }

  async function removeMemberAccount(targetUserId: string, accountId: string) {
    await deleteMemberAccount(targetUserId, accountId)
    await fetchMemberAccounts(targetUserId)
  }

  function clear() {
    accounts.value = []
    userSummary.value = null
    familySummary.value = null
    userHistory.value = []
    familyHistory.value = []
    familyAssets.value = []
    memberAccounts.value = []
  }

  return {
    accounts, userSummary, familySummary, userHistory, familyHistory,
    familyAssets, memberAccounts,
    fetchAccounts, addAccount, editAccount, removeAccount,
    fetchUserSummary, fetchFamilySummary, fetchUserHistory, fetchFamilyHistory,
    fetchFamilyAssets, addFamilyAsset, editFamilyAsset, removeFamilyAsset,
    fetchMemberAccounts, addMemberAccount, editMemberAccount, removeMemberAccount,
    clear,
  }
})
