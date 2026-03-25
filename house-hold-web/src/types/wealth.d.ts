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
  /** 是否立即可用现金，信用卡无此含义 */
  availableImmediately?: boolean
  remark?: string
  createdAt?: string
  updatedAt?: string
}

export interface AccountCreateRequest {
  accountName: string
  accountType: AccountType
  balance: number
  currency?: string
  availableImmediately?: boolean
  remark?: string
}

export interface AccountUpdateRequest {
  accountName?: string
  accountType?: AccountType
  balance?: number
  currency?: string
  availableImmediately?: boolean
  remark?: string
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
  /** 可用现金（立即可用且非信用卡账户余额之和，单位分） */
  availableCash?: number
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
  /** 商贷总额/余额、公积金总额/余额（分），房产可区分 */
  commercialLoanTotal?: number
  commercialLoanRemaining?: number
  providentLoanTotal?: number
  providentLoanRemaining?: number
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
  commercialLoanTotal?: number
  commercialLoanRemaining?: number
  providentLoanTotal?: number
  providentLoanRemaining?: number
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
  commercialLoanTotal?: number
  commercialLoanRemaining?: number
  providentLoanTotal?: number
  providentLoanRemaining?: number
  loanOnly?: boolean
}
