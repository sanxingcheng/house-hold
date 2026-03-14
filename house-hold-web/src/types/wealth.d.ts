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
  updatedAt?: string
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
  /** 家庭共有资产总额（仅 ownerType=FAMILY 时有值） */
  familyAssetTotal?: number
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
  /** 贷款总额（分），主要用于房贷/车贷 */
  loanTotal?: number
  /** 当前贷款余额（分），计入负债 */
  loanRemaining?: number
  /** 是否只统计负债，不计入资产总额 */
  loanOnly?: boolean
}

export interface FamilyAssetCreateRequest {
  assetName: string
  assetType: FamilyAssetType
  amount: number
  currency?: string
  remark?: string
  loanTotal?: number
  loanRemaining?: number
  loanOnly?: boolean
}

export interface FamilyAssetUpdateRequest {
  assetName?: string
  assetType?: FamilyAssetType
  amount?: number
  currency?: string
  remark?: string
  loanTotal?: number
  loanRemaining?: number
  loanOnly?: boolean
}
