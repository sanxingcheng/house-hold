import { defineStore } from 'pinia'
import { ref } from 'vue'
import {
  getAccounts,
  createAccount,
  updateAccount,
  deleteAccount,
  getUserSummary,
  getFamilySummary,
  getUserHistory,
  getFamilyHistory,
} from '@/api/wealth'
import type { Account, AccountCreateRequest, AccountUpdateRequest, WealthSummary, SnapshotPoint } from '@/types/wealth'

export const useWealthStore = defineStore('wealth', () => {
  const accounts = ref<Account[]>([])
  const userSummary = ref<WealthSummary | null>(null)
  const familySummary = ref<WealthSummary | null>(null)
  const userHistory = ref<SnapshotPoint[]>([])
  const familyHistory = ref<SnapshotPoint[]>([])

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

  function clear() {
    accounts.value = []
    userSummary.value = null
    familySummary.value = null
    userHistory.value = []
    familyHistory.value = []
  }

  return {
    accounts,
    userSummary,
    familySummary,
    userHistory,
    familyHistory,
    fetchAccounts,
    addAccount,
    editAccount,
    removeAccount,
    fetchUserSummary,
    fetchFamilySummary,
    fetchUserHistory,
    fetchFamilyHistory,
    clear,
  }
})
