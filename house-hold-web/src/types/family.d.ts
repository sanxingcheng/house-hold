export interface FamilyInfo {
  id: string
  nameAlias: string
  country: string
  province: string
  city: string
  street: string
}

export interface FamilyMember {
  userId: string
  username: string
  name?: string
  role: string
}

export interface FamilyCreateRequest {
  nameAlias: string
  country: string
  province: string
  city: string
  street: string
  role?: string
}

export interface FamilyJoinRequest {
  familyId?: string
  role?: string
}

export interface FamilyMemberRoleUpdateRequest {
  role: string
}

export interface FamilyResponse {
  id: string
  nameAlias: string
  country: string
  province: string
  city: string
  street: string
  members: FamilyMember[]
}
