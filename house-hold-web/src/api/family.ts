import { request } from '@/api/request'
import type { FamilyCreateRequest, FamilyJoinRequest, FamilyMemberRoleUpdateRequest, FamilyResponse } from '@/types/family'

export function getFamily(familyId: string) {
  return request.get<FamilyResponse>(`/family/${familyId}`)
}

export function createFamily(data: FamilyCreateRequest) {
  return request.post<FamilyResponse>('/family/create', data)
}

export function joinFamily(data: FamilyJoinRequest) {
  return request.post<FamilyResponse>('/family/join', data)
}

export function updateFamily(familyId: string, data: FamilyCreateRequest) {
  return request.put<FamilyResponse>(`/family/${familyId}`, data)
}

export function updateMemberRole(familyId: string, data: FamilyMemberRoleUpdateRequest) {
  return request.put<FamilyResponse>(`/family/${familyId}/members/role`, data)
}
