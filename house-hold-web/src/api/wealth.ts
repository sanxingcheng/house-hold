import { request } from '@/api/request'
import type {
  Account, AccountCreateRequest, AccountUpdateRequest,
  WealthSummary, SnapshotPoint,
  FamilyAsset, FamilyAssetCreateRequest, FamilyAssetUpdateRequest,
} from '@/types/wealth'

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

/** 账户余额变化趋势（按日快照） */
export interface AccountBalancePoint {
  snapshotDate: string
  balance: number
}
export function getAccountHistory(accountId: string, from: string, to: string) {
  return request.get<AccountBalancePoint[]>(`/wealth/accounts/${accountId}/history`, { params: { from, to } })
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

// Family assets
export function getFamilyAssets() {
  return request.get<FamilyAsset[]>('/wealth/family-assets')
}

export function createFamilyAsset(data: FamilyAssetCreateRequest) {
  return request.post<FamilyAsset>('/wealth/family-assets', data)
}

export function updateFamilyAsset(id: string, data: FamilyAssetUpdateRequest) {
  return request.put<FamilyAsset>(`/wealth/family-assets/${id}`, data)
}

export function deleteFamilyAsset(id: string) {
  return request.delete(`/wealth/family-assets/${id}`)
}

// Family-level member accounts (for owner/admin)
export function getFamilyAccounts() {
  return request.get<Account[]>('/wealth/accounts/family')
}

// Admin: manage member accounts
export function getMemberAccounts(targetUserId: string) {
  return request.get<Account[]>(`/wealth/accounts/member/${targetUserId}`)
}

export function createMemberAccount(targetUserId: string, data: AccountCreateRequest) {
  return request.post<Account>(`/wealth/accounts/member/${targetUserId}`, data)
}

export function updateMemberAccount(targetUserId: string, accountId: string, data: AccountUpdateRequest) {
  return request.put<Account>(`/wealth/accounts/member/${targetUserId}/${accountId}`, data)
}

export function deleteMemberAccount(targetUserId: string, accountId: string) {
  return request.delete(`/wealth/accounts/member/${targetUserId}/${accountId}`)
}

// Operation logs (family scope, requires X-Family-Id from gateway)
export interface OperationLogsResponse {
  content: OperationLogItem[]
  totalElements: number
  totalPages: number
  number: number
  size: number
}
export interface OperationLogItem {
  id: string
  userId: string
  familyId: string | null
  action: string
  resourceType: string
  resourceId: string
  detail: string | null
  createdAt: string
}
export function getOperationLogs(page = 0, size = 20, familyId?: string) {
  const headers: Record<string, string> = {}
  if (familyId) headers['X-Family-Id'] = familyId
  return request.get<OperationLogsResponse>('/wealth/operation-logs', { params: { page, size }, headers })
}
