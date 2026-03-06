export type AccountType =
  | 'SAVING'
  | 'CREDIT_CARD'
  | 'STOCK'
  | 'FUND'
  | 'ALIPAY'
  | 'WECHAT'
  | 'OTHER'

export interface Account {
  id: string
  userId: string
  accountName: string
  accountType: AccountType
  balance: number
  currency: string
  createdAt?: string
}

export interface AccountCreateRequest {
  accountName: string
  accountType: AccountType
  balance: number
  currency?: string
}

export interface AccountUpdateRequest {
  accountName?: string
  accountType?: AccountType
  balance?: number
  currency?: string
}

export interface WealthSummary {
  ownerId: string
  ownerType: string
  totalAssets: number
  totalLiabilities: number
  netWorth: number
  snapshotTime: string
}

export interface SnapshotPoint {
  snapshotDate: string
  netWorth: number
}
