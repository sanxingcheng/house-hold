import { request } from '@/api/request'
import type {
  FamilyCreateRequest, FamilyMemberRoleUpdateRequest, FamilyResponse,
  ApplyJoinRequest, CreateMemberRequest, InviteUserRequest, SetAdminRequest,
  JoinRequestResponse,
} from '@/types/family'

export function getFamily(familyId: string) {
  return request.get<FamilyResponse>(`/family/${familyId}`)
}

export function createFamily(data: FamilyCreateRequest) {
  return request.post<FamilyResponse>('/family/create', data)
}

export function updateFamily(familyId: string, data: FamilyCreateRequest) {
  return request.put<FamilyResponse>(`/family/${familyId}`, data)
}

export function updateMemberRole(familyId: string, data: FamilyMemberRoleUpdateRequest) {
  return request.put<FamilyResponse>(`/family/${familyId}/members/role`, data)
}

export function applyToJoinFamily(familyId: string, data: ApplyJoinRequest) {
  return request.post<JoinRequestResponse>(`/family/${familyId}/apply`, data)
}

export function getMyApplications() {
  return request.get<JoinRequestResponse[]>('/family/my-applications')
}

export function getMyInvitations() {
  return request.get<JoinRequestResponse[]>('/family/my-invitations')
}

export function acceptInvitation(reqId: string) {
  return request.put(`/family/invitations/${reqId}/accept`)
}

export function rejectInvitation(reqId: string) {
  return request.put(`/family/invitations/${reqId}/reject`)
}

// Admin APIs
export function adminCreateMember(familyId: string, data: CreateMemberRequest) {
  return request.post<FamilyResponse>(`/family/${familyId}/admin/create-member`, data)
}

export function adminInviteUser(familyId: string, data: InviteUserRequest) {
  return request.post<JoinRequestResponse>(`/family/${familyId}/admin/invite`, data)
}

export function adminSetAdmin(familyId: string, targetUserId: string, data: SetAdminRequest) {
  return request.put(`/family/${familyId}/admin/members/${targetUserId}/set-admin`, data)
}

export function adminRemoveMember(familyId: string, targetUserId: string) {
  return request.delete(`/family/${familyId}/admin/members/${targetUserId}`)
}

export function adminGetPendingRequests(familyId: string) {
  return request.get<JoinRequestResponse[]>(`/family/${familyId}/admin/requests`)
}

export function adminApproveRequest(familyId: string, reqId: string) {
  return request.put(`/family/${familyId}/admin/requests/${reqId}/approve`)
}

export function adminRejectRequest(familyId: string, reqId: string) {
  return request.put(`/family/${familyId}/admin/requests/${reqId}/reject`)
}
