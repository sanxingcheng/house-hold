import { request } from '@/api/request'
import type { UserProfile, UserProfileUpdateRequest } from '@/types/user'

export function getProfile() {
  return request.get<UserProfile>('/user/profile')
}

export function updateProfile(data: UserProfileUpdateRequest) {
  return request.put<UserProfile>('/user/profile', data)
}
