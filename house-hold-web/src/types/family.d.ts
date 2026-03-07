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
  isAdmin: boolean
  isCreator: boolean
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
  createdBy: string
  members: FamilyMember[]
}

export interface ApplyJoinRequest {
  role?: string
}

export interface CreateMemberRequest {
  username: string
  password: string
  name: string
  birthday: string
  email?: string
  phone?: string
  role?: string
}

export interface InviteUserRequest {
  username: string
  role?: string
}

export interface SetAdminRequest {
  admin: boolean
}

export interface JoinRequestResponse {
  id: string
  familyId: string
  familyName: string
  userId: string
  username: string
  requestType: 'APPLY' | 'INVITE'
  status: 'PENDING' | 'APPROVED' | 'REJECTED'
  role: string
  initiatedByUsername: string
  createdAt: string
}
