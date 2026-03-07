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

export type FamilyAssetType =
  | 'REAL_ESTATE'
  | 'VEHICLE'
  | 'DEPOSIT'
  | 'INVESTMENT'
  | 'OTHER'

export interface FamilyAsset {
  id: string
  familyId: string
  assetName: string
  assetType: FamilyAssetType
  amount: number
  currency: string
  remark?: string
  createdBy: string
  createdAt?: string
}

export interface FamilyAssetCreateRequest {
  assetName: string
  assetType: FamilyAssetType
  amount: number
  currency?: string
  remark?: string
}

export interface FamilyAssetUpdateRequest {
  assetName?: string
  assetType?: FamilyAssetType
  amount?: number
  currency?: string
  remark?: string
}
