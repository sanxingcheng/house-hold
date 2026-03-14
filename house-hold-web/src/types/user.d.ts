export interface UserProfile {
  id: string
  username: string
  name?: string
  gender?: string
  birthday?: string
  email?: string
  phone?: string
  familyId?: string | null
}

export interface UserProfileUpdateRequest {
  name?: string
  gender?: string
  birthday?: string
  email?: string
  phone?: string
}
