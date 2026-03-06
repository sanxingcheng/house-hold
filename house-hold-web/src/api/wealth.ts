import { request } from '@/api/request'
import type { Account, AccountCreateRequest, AccountUpdateRequest, WealthSummary, SnapshotPoint } from '@/types/wealth'

export function getAccounts() {
  return request.get<Account[]>('/wealth/accounts')
}

export function createAccount(data: AccountCreateRequest) {
  return request.post<Account>('/wealth/accounts', data)
}

export function updateAccount(id: string, data: AccountUpdateRequest) {
  return request.put<Account>(`/wealth/accounts/${id}`, data)
}

export function deleteAccount(id: string) {
  return request.delete(`/wealth/accounts/${id}`)
}

export function getUserSummary() {
  return request.get<WealthSummary>('/wealth/summary/user')
}

export function getFamilySummary() {
  return request.get<WealthSummary>('/wealth/summary/family')
}

export function getUserHistory(from: string, to: string) {
  return request.get<SnapshotPoint[]>('/wealth/history/user', { params: { from, to } })
}

export function getFamilyHistory(from: string, to: string) {
  return request.get<SnapshotPoint[]>('/wealth/history/family', { params: { from, to } })
}
