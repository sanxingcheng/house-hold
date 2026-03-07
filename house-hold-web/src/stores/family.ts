import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import {
  getFamily, createFamily, updateFamily,
  applyToJoinFamily, getMyInvitations, acceptInvitation, rejectInvitation,
  adminCreateMember, adminInviteUser, adminSetAdmin, adminRemoveMember,
  adminGetPendingRequests, adminApproveRequest, adminRejectRequest,
} from '@/api/family'
import type {
  FamilyResponse, FamilyCreateRequest, ApplyJoinRequest,
  CreateMemberRequest, InviteUserRequest, JoinRequestResponse,
} from '@/types/family'
import { useAuthStore } from '@/stores/auth'

export const useFamilyStore = defineStore('family', () => {
  const family = ref<FamilyResponse | null>(null)
  const members = ref<FamilyResponse['members']>([])
  const pendingRequests = ref<JoinRequestResponse[]>([])
  const myInvitations = ref<JoinRequestResponse[]>([])

  const currentUserMember = computed(() => {
    const authStore = useAuthStore()
    return members.value.find(m => m.userId === authStore.user?.id) ?? null
  })

  const isAdmin = computed(() => currentUserMember.value?.isAdmin ?? false)
  const isCreator = computed(() => currentUserMember.value?.isCreator ?? false)

  function setFamily(data: FamilyResponse | null) {
    family.value = data
    members.value = data?.members ?? []
  }

  async function fetchFamily(familyId: string) {
    const { data } = await getFamily(familyId)
    setFamily(data)
    return data
  }

  async function create(data: FamilyCreateRequest) {
    const { data: res } = await createFamily(data)
    setFamily(res)
    const authStore = useAuthStore()
    authStore.setFamilyId(res.id)
    return res
  }

  async function updateAddress(familyId: string, payload: FamilyCreateRequest) {
    const { data: res } = await updateFamily(familyId, payload)
    setFamily(res)
    return res
  }

  // Apply to join
  async function applyJoin(familyId: string, payload: ApplyJoinRequest) {
    const { data } = await applyToJoinFamily(familyId, payload)
    return data
  }

  // Invitations for current user
  async function fetchMyInvitations() {
    const { data } = await getMyInvitations()
    myInvitations.value = data
    return data
  }

  async function handleAcceptInvitation(reqId: string) {
    await acceptInvitation(reqId)
    myInvitations.value = myInvitations.value.filter(i => i.id !== reqId)
  }

  async function handleRejectInvitation(reqId: string) {
    await rejectInvitation(reqId)
    myInvitations.value = myInvitations.value.filter(i => i.id !== reqId)
  }

  // Admin operations
  async function createMember(familyId: string, payload: CreateMemberRequest) {
    const { data } = await adminCreateMember(familyId, payload)
    setFamily(data)
    return data
  }

  async function inviteUser(familyId: string, payload: InviteUserRequest) {
    const { data } = await adminInviteUser(familyId, payload)
    return data
  }

  async function setAdminStatus(familyId: string, targetUserId: string, admin: boolean) {
    await adminSetAdmin(familyId, targetUserId, { admin })
  }

  async function removeMember(familyId: string, targetUserId: string) {
    await adminRemoveMember(familyId, targetUserId)
  }

  async function fetchPendingRequests(familyId: string) {
    const { data } = await adminGetPendingRequests(familyId)
    pendingRequests.value = data
    return data
  }

  async function approveRequest(familyId: string, reqId: string) {
    await adminApproveRequest(familyId, reqId)
    pendingRequests.value = pendingRequests.value.filter(r => r.id !== reqId)
  }

  async function rejectRequest(familyId: string, reqId: string) {
    await adminRejectRequest(familyId, reqId)
    pendingRequests.value = pendingRequests.value.filter(r => r.id !== reqId)
  }

  function clear() {
    family.value = null
    members.value = []
    pendingRequests.value = []
    myInvitations.value = []
  }

  return {
    family, members, pendingRequests, myInvitations,
    currentUserMember, isAdmin, isCreator,
    setFamily, fetchFamily, create, updateAddress,
    applyJoin, fetchMyInvitations, handleAcceptInvitation, handleRejectInvitation,
    createMember, inviteUser, setAdminStatus, removeMember,
    fetchPendingRequests, approveRequest, rejectRequest,
    clear,
  }
})
