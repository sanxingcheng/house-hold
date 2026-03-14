package com.household.authuser.service;

import com.household.authuser.dto.request.*;
import com.household.authuser.dto.response.FamilyResponse;
import com.household.authuser.dto.response.JoinRequestResponse;
import com.household.authuser.entity.Family;
import com.household.authuser.entity.FamilyJoinRequest;
import com.household.authuser.entity.FamilyMemberRole;
import com.household.authuser.entity.User;
import com.household.authuser.repository.FamilyJoinRequestRepository;
import com.household.authuser.repository.FamilyMemberRoleRepository;
import com.household.authuser.repository.FamilyRepository;
import com.household.authuser.repository.UserRepository;
import com.household.common.exception.BadRequestException;
import com.household.common.exception.ForbiddenException;
import com.household.common.exception.NotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.redisson.api.RLock;
import org.redisson.api.RSet;
import org.redisson.api.RedissonClient;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("FamilyService 单元测试")
class FamilyServiceTest {

    @Mock
    private FamilyRepository familyRepository;
    @Mock
    private FamilyMemberRoleRepository familyMemberRoleRepository;
    @Mock
    private FamilyJoinRequestRepository familyJoinRequestRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private RedissonClient redissonClient;

    @InjectMocks
    private FamilyService familyService;

    private User sampleUser(Long id, String username) {
        User u = new User();
        u.setId(id);
        u.setUsername(username);
        u.setName(username);
        u.setBirthday(LocalDate.of(1990, 1, 1));
        return u;
    }

    private Family sampleFamily(Long id, Long createdBy) {
        Family f = new Family();
        f.setId(id);
        f.setNameAlias("测试家庭");
        f.setCountry("中国");
        f.setProvince("广东");
        f.setCity("深圳");
        f.setStreet("科技路1号");
        f.setCreatedBy(createdBy);
        return f;
    }

    private FamilyMemberRole sampleRole(Long userId, Long familyId, boolean isAdmin) {
        FamilyMemberRole r = new FamilyMemberRole();
        r.setId(100L);
        r.setUserId(userId);
        r.setFamilyId(familyId);
        r.setRole("HUSBAND");
        r.setIsAdmin(isAdmin);
        r.setJoinedAt(LocalDateTime.now());
        return r;
    }

    @SuppressWarnings("unchecked")
    private void stubRedisSet() {
        RSet<String> set = mock(RSet.class);
        doReturn(set).when(redissonClient).getSet(anyString());
    }

    @SuppressWarnings("unchecked")
    private void stubLock() {
        RLock lock = mock(RLock.class);
        try {
            when(lock.tryLock(anyLong(), anyLong(), any(TimeUnit.class))).thenReturn(true);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        doReturn(lock).when(redissonClient).getLock(anyString());
    }

    // ======================== create ========================

    @Nested
    @DisplayName("create")
    class Create {

        /** 验证创建家庭成功：保存家庭和成员角色，设置用户的 familyId */
        @Test
        @DisplayName("创建家庭成功时返回 FamilyResponse")
        void whenSuccess_thenReturnsFamilyResponse() {
            stubLock();
            User user = sampleUser(1L, "creator");
            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
            when(familyRepository.save(any(Family.class))).thenAnswer(inv -> inv.getArgument(0));
            when(familyMemberRoleRepository.save(any(FamilyMemberRole.class))).thenAnswer(inv -> inv.getArgument(0));
            when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
            stubRedisSet();

            FamilyCreateRequest req = new FamilyCreateRequest();
            req.setNameAlias("张家");
            req.setCountry("中国");
            req.setProvince("广东");
            req.setCity("深圳");
            req.setStreet("科技路1号");
            req.setRole("HUSBAND");

            FamilyResponse response = familyService.create(1L, req);

            assertThat(response.getNameAlias()).isEqualTo("张家");
            assertThat(response.getMembers()).hasSize(1);
            assertThat(response.getMembers().get(0).getRole()).isEqualTo("HUSBAND");
            verify(familyRepository).save(any(Family.class));
            verify(familyMemberRoleRepository).save(any(FamilyMemberRole.class));
        }

        /** 验证用户不存在时抛出 NotFoundException */
        @Test
        @DisplayName("用户不存在时抛出 NotFoundException")
        void whenUserNotFound_thenThrows() {
            stubLock();
            when(userRepository.findById(1L)).thenReturn(Optional.empty());

            FamilyCreateRequest req = new FamilyCreateRequest();
            req.setNameAlias("test");

            assertThatThrownBy(() -> familyService.create(1L, req))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessage("用户不存在");
        }

        /** 验证用户已属于家庭时抛出 BadRequestException */
        @Test
        @DisplayName("用户已属于家庭时抛出 BadRequestException")
        void whenAlreadyInFamily_thenThrows() {
            stubLock();
            User user = sampleUser(1L, "creator");
            user.setFamilyId(99L);
            when(userRepository.findById(1L)).thenReturn(Optional.of(user));

            FamilyCreateRequest req = new FamilyCreateRequest();
            req.setNameAlias("test");

            assertThatThrownBy(() -> familyService.create(1L, req))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("已属于一个家庭");
        }
    }

    // ======================== getFamily ========================

    @Nested
    @DisplayName("getFamily")
    class GetFamily {

        /** 验证用户属于家庭且家庭存在时返回正确的 FamilyResponse */
        @Test
        @DisplayName("成功获取家庭信息")
        void whenMemberAndFamilyExist_thenReturnsFamilyResponse() {
            FamilyMemberRole role = sampleRole(1L, 10L, false);
            when(familyMemberRoleRepository.findByUserIdAndFamilyId(1L, 10L))
                    .thenReturn(Optional.of(role));
            Family family = sampleFamily(10L, 1L);
            when(familyRepository.findById(10L)).thenReturn(Optional.of(family));
            when(familyMemberRoleRepository.findByFamilyIdOrderByJoinedAt(10L)).thenReturn(List.of(role));
            when(userRepository.findById(1L)).thenReturn(Optional.of(sampleUser(1L, "u1")));

            FamilyResponse response = familyService.getFamily(1L, 10L);

            assertThat(response.getId()).isEqualTo("10");
            assertThat(response.getNameAlias()).isEqualTo("测试家庭");
        }

        /** 验证用户不属于该家庭时抛出 ForbiddenException */
        @Test
        @DisplayName("用户不属于家庭时抛出 ForbiddenException")
        void whenNotMember_thenThrows() {
            when(familyMemberRoleRepository.findByUserIdAndFamilyId(1L, 10L))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> familyService.getFamily(1L, 10L))
                    .isInstanceOf(ForbiddenException.class)
                    .hasMessage("您不属于该家庭");
        }

        /** 验证家庭不存在时抛出 NotFoundException */
        @Test
        @DisplayName("家庭不存在时抛出 NotFoundException")
        void whenFamilyNotFound_thenThrows() {
            FamilyMemberRole role = sampleRole(1L, 10L, false);
            when(familyMemberRoleRepository.findByUserIdAndFamilyId(1L, 10L))
                    .thenReturn(Optional.of(role));
            when(familyRepository.findById(10L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> familyService.getFamily(1L, 10L))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessage("家庭不存在");
        }
    }

    // ======================== updateFamily ========================

    @Nested
    @DisplayName("updateFamily")
    class UpdateFamily {

        /** 验证管理员更新家庭信息成功 */
        @Test
        @DisplayName("管理员更新家庭信息成功")
        void whenAdmin_thenUpdatesAndReturns() {
            FamilyMemberRole adminRole = sampleRole(1L, 10L, true);
            when(familyMemberRoleRepository.findByUserIdAndFamilyId(1L, 10L))
                    .thenReturn(Optional.of(adminRole));
            Family family = sampleFamily(10L, 1L);
            when(familyRepository.findById(10L)).thenReturn(Optional.of(family));
            when(familyRepository.save(any(Family.class))).thenAnswer(inv -> inv.getArgument(0));
            when(familyMemberRoleRepository.findByFamilyIdOrderByJoinedAt(10L))
                    .thenReturn(List.of(adminRole));
            when(userRepository.findById(1L)).thenReturn(Optional.of(sampleUser(1L, "u1")));

            FamilyUpdateRequest req = new FamilyUpdateRequest();
            req.setNameAlias("新名字");
            req.setCountry("中国");
            req.setProvince("北京");
            req.setCity("北京");
            req.setStreet("长安街");

            FamilyResponse response = familyService.updateFamily(1L, 10L, req);

            assertThat(response.getNameAlias()).isEqualTo("新名字");
            verify(familyRepository).save(family);
        }

        /** 验证非管理员更新家庭信息时抛出 ForbiddenException */
        @Test
        @DisplayName("非管理员抛出 ForbiddenException")
        void whenNotAdmin_thenThrows() {
            FamilyMemberRole normalRole = sampleRole(1L, 10L, false);
            when(familyMemberRoleRepository.findByUserIdAndFamilyId(1L, 10L))
                    .thenReturn(Optional.of(normalRole));

            FamilyUpdateRequest req = new FamilyUpdateRequest();
            req.setNameAlias("new");

            assertThatThrownBy(() -> familyService.updateFamily(1L, 10L, req))
                    .isInstanceOf(ForbiddenException.class)
                    .hasMessage("需要管理员权限");
        }
    }

    // ======================== updateMemberRole ========================

    @Nested
    @DisplayName("updateMemberRole")
    class UpdateMemberRole {

        /** 验证成员更新自身角色成功 */
        @Test
        @DisplayName("成员更新角色成功")
        void whenMember_thenUpdatesRole() {
            FamilyMemberRole role = sampleRole(1L, 10L, false);
            when(familyMemberRoleRepository.findByUserIdAndFamilyId(1L, 10L))
                    .thenReturn(Optional.of(role));
            Family family = sampleFamily(10L, 1L);
            when(familyRepository.findById(10L)).thenReturn(Optional.of(family));
            when(familyMemberRoleRepository.findByFamilyIdOrderByJoinedAt(10L))
                    .thenReturn(List.of(role));
            when(userRepository.findById(1L)).thenReturn(Optional.of(sampleUser(1L, "u1")));

            FamilyMemberRoleUpdateRequest req = new FamilyMemberRoleUpdateRequest();
            req.setRole("WIFE");

            FamilyResponse response = familyService.updateMemberRole(1L, 10L, req);

            verify(familyMemberRoleRepository).updateRoleByUserIdAndFamilyId(1L, 10L, "WIFE");
            assertThat(response).isNotNull();
        }

        /** 验证不属于家庭的用户更新角色时抛出 ForbiddenException */
        @Test
        @DisplayName("不属于家庭时抛出 ForbiddenException")
        void whenNotMember_thenThrows() {
            when(familyMemberRoleRepository.findByUserIdAndFamilyId(1L, 10L))
                    .thenReturn(Optional.empty());

            FamilyMemberRoleUpdateRequest req = new FamilyMemberRoleUpdateRequest();
            req.setRole("WIFE");

            assertThatThrownBy(() -> familyService.updateMemberRole(1L, 10L, req))
                    .isInstanceOf(ForbiddenException.class);
        }
    }

    // ======================== applyToJoin ========================

    @Nested
    @DisplayName("applyToJoin")
    class ApplyToJoin {

        /** 验证用户申请加入家庭成功 */
        @Test
        @DisplayName("申请成功时返回 JoinRequestResponse")
        void whenSuccess_thenReturnsResponse() {
            stubLock();
            User user = sampleUser(1L, "applicant");
            Family family = sampleFamily(10L, 2L);
            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
            when(familyRepository.findById(10L)).thenReturn(Optional.of(family));
            when(familyJoinRequestRepository.findByFamilyIdAndUserIdAndStatus(10L, 1L, "PENDING"))
                    .thenReturn(Optional.empty());
            when(familyJoinRequestRepository.save(any(FamilyJoinRequest.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            ApplyJoinRequest req = new ApplyJoinRequest();
            req.setRole("CHILD");

            JoinRequestResponse response = familyService.applyToJoin(1L, 10L, req);

            assertThat(response.getRequestType()).isEqualTo("APPLY");
            assertThat(response.getStatus()).isEqualTo("PENDING");
            assertThat(response.getRole()).isEqualTo("CHILD");
        }

        /** 验证已属于家庭的用户申请时抛出 BadRequestException */
        @Test
        @DisplayName("已属于家庭时抛出 BadRequestException")
        void whenAlreadyInFamily_thenThrows() {
            stubLock();
            User user = sampleUser(1L, "applicant");
            user.setFamilyId(99L);
            when(userRepository.findById(1L)).thenReturn(Optional.of(user));

            ApplyJoinRequest req = new ApplyJoinRequest();

            assertThatThrownBy(() -> familyService.applyToJoin(1L, 10L, req))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessage("您已属于一个家庭");
        }

        /** 验证已有待处理申请时抛出 BadRequestException */
        @Test
        @DisplayName("已有待处理申请时抛出 BadRequestException")
        void whenPendingExists_thenThrows() {
            stubLock();
            User user = sampleUser(1L, "applicant");
            Family family = sampleFamily(10L, 2L);
            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
            when(familyRepository.findById(10L)).thenReturn(Optional.of(family));
            when(familyJoinRequestRepository.findByFamilyIdAndUserIdAndStatus(10L, 1L, "PENDING"))
                    .thenReturn(Optional.of(new FamilyJoinRequest()));

            ApplyJoinRequest req = new ApplyJoinRequest();

            assertThatThrownBy(() -> familyService.applyToJoin(1L, 10L, req))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("已提交过申请");
        }
    }

    // ======================== inviteUser ========================

    @Nested
    @DisplayName("inviteUser")
    class InviteUser {

        /** 验证管理员邀请用户成功 */
        @Test
        @DisplayName("邀请成功时返回 JoinRequestResponse")
        void whenSuccess_thenReturnsResponse() {
            FamilyMemberRole adminRole = sampleRole(1L, 10L, true);
            when(familyMemberRoleRepository.findByUserIdAndFamilyId(1L, 10L))
                    .thenReturn(Optional.of(adminRole));
            Family family = sampleFamily(10L, 1L);
            when(familyRepository.findById(10L)).thenReturn(Optional.of(family));
            User invitee = sampleUser(2L, "invitee");
            when(userRepository.findByUsername("invitee")).thenReturn(Optional.of(invitee));
            when(familyJoinRequestRepository.findByFamilyIdAndUserIdAndStatus(10L, 2L, "PENDING"))
                    .thenReturn(Optional.empty());
            when(familyJoinRequestRepository.save(any(FamilyJoinRequest.class)))
                    .thenAnswer(inv -> inv.getArgument(0));
            when(userRepository.findById(1L)).thenReturn(Optional.of(sampleUser(1L, "admin")));

            InviteUserRequest req = new InviteUserRequest();
            req.setUsername("invitee");
            req.setRole("WIFE");

            JoinRequestResponse response = familyService.inviteUser(1L, 10L, req);

            assertThat(response.getRequestType()).isEqualTo("INVITE");
            assertThat(response.getStatus()).isEqualTo("PENDING");
        }

        /** 验证被邀请用户不存在时抛出 NotFoundException */
        @Test
        @DisplayName("被邀请用户不存在时抛出 NotFoundException")
        void whenInviteeNotFound_thenThrows() {
            FamilyMemberRole adminRole = sampleRole(1L, 10L, true);
            when(familyMemberRoleRepository.findByUserIdAndFamilyId(1L, 10L))
                    .thenReturn(Optional.of(adminRole));
            Family family = sampleFamily(10L, 1L);
            when(familyRepository.findById(10L)).thenReturn(Optional.of(family));
            when(userRepository.findByUsername("nobody")).thenReturn(Optional.empty());

            InviteUserRequest req = new InviteUserRequest();
            req.setUsername("nobody");

            assertThatThrownBy(() -> familyService.inviteUser(1L, 10L, req))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessageContaining("用户不存在");
        }

        /** 验证被邀请用户已有家庭时抛出 BadRequestException */
        @Test
        @DisplayName("被邀请用户已有家庭时抛出 BadRequestException")
        void whenInviteeAlreadyInFamily_thenThrows() {
            FamilyMemberRole adminRole = sampleRole(1L, 10L, true);
            when(familyMemberRoleRepository.findByUserIdAndFamilyId(1L, 10L))
                    .thenReturn(Optional.of(adminRole));
            Family family = sampleFamily(10L, 1L);
            when(familyRepository.findById(10L)).thenReturn(Optional.of(family));
            User invitee = sampleUser(2L, "invitee");
            invitee.setFamilyId(99L);
            when(userRepository.findByUsername("invitee")).thenReturn(Optional.of(invitee));

            InviteUserRequest req = new InviteUserRequest();
            req.setUsername("invitee");

            assertThatThrownBy(() -> familyService.inviteUser(1L, 10L, req))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("已属于一个家庭");
        }
    }

    // ======================== getPendingRequests ========================

    @Nested
    @DisplayName("getPendingRequests")
    class GetPendingRequests {

        /** 验证管理员获取待审批列表成功 */
        @Test
        @DisplayName("管理员获取待审批列表")
        void whenAdmin_thenReturnsPendingList() {
            FamilyMemberRole adminRole = sampleRole(1L, 10L, true);
            when(familyMemberRoleRepository.findByUserIdAndFamilyId(1L, 10L))
                    .thenReturn(Optional.of(adminRole));
            Family family = sampleFamily(10L, 1L);
            when(familyRepository.findById(10L)).thenReturn(Optional.of(family));

            FamilyJoinRequest jr = new FamilyJoinRequest();
            jr.setId(500L);
            jr.setFamilyId(10L);
            jr.setUserId(2L);
            jr.setRequestType("APPLY");
            jr.setStatus("PENDING");
            jr.setInitiatedBy(2L);
            jr.setRole("CHILD");
            when(familyJoinRequestRepository.findByFamilyIdAndStatusOrderByCreatedAtDesc(10L, "PENDING"))
                    .thenReturn(List.of(jr));
            when(userRepository.findById(2L)).thenReturn(Optional.of(sampleUser(2L, "u2")));

            List<JoinRequestResponse> result = familyService.getPendingRequests(1L, 10L);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getRequestType()).isEqualTo("APPLY");
        }
    }

    // ======================== approveRequest ========================

    @Nested
    @DisplayName("approveRequest")
    class ApproveRequest {

        /** 验证审批通过时成员被加入家庭 */
        @Test
        @DisplayName("审批通过时成员加入家庭")
        void whenApproved_thenMemberAdded() {
            stubLock();
            FamilyMemberRole adminRole = sampleRole(1L, 10L, true);
            when(familyMemberRoleRepository.findByUserIdAndFamilyId(1L, 10L))
                    .thenReturn(Optional.of(adminRole));

            FamilyJoinRequest jr = new FamilyJoinRequest();
            jr.setId(500L);
            jr.setFamilyId(10L);
            jr.setUserId(2L);
            jr.setStatus("PENDING");
            jr.setRole("CHILD");
            when(familyJoinRequestRepository.findById(500L)).thenReturn(Optional.of(jr));

            User user = sampleUser(2L, "u2");
            when(userRepository.findById(2L)).thenReturn(Optional.of(user));
            when(familyMemberRoleRepository.findByUserIdAndFamilyId(2L, 10L))
                    .thenReturn(Optional.empty());
            when(familyMemberRoleRepository.save(any(FamilyMemberRole.class)))
                    .thenAnswer(inv -> inv.getArgument(0));
            when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

            familyService.approveRequest(1L, 10L, 500L);

            assertThat(jr.getStatus()).isEqualTo("APPROVED");
            verify(familyMemberRoleRepository).save(any(FamilyMemberRole.class));
        }

        /** 验证请求不属于该家庭时抛出 BadRequestException */
        @Test
        @DisplayName("请求不属于该家庭时抛出 BadRequestException")
        void whenWrongFamily_thenThrows() {
            FamilyMemberRole adminRole = sampleRole(1L, 10L, true);
            when(familyMemberRoleRepository.findByUserIdAndFamilyId(1L, 10L))
                    .thenReturn(Optional.of(adminRole));

            FamilyJoinRequest jr = new FamilyJoinRequest();
            jr.setId(500L);
            jr.setFamilyId(99L);
            jr.setStatus("PENDING");
            when(familyJoinRequestRepository.findById(500L)).thenReturn(Optional.of(jr));

            assertThatThrownBy(() -> familyService.approveRequest(1L, 10L, 500L))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessage("请求不属于该家庭");
        }

        /** 验证请求已处理时抛出 BadRequestException */
        @Test
        @DisplayName("请求已处理时抛出 BadRequestException")
        void whenAlreadyProcessed_thenThrows() {
            FamilyMemberRole adminRole = sampleRole(1L, 10L, true);
            when(familyMemberRoleRepository.findByUserIdAndFamilyId(1L, 10L))
                    .thenReturn(Optional.of(adminRole));

            FamilyJoinRequest jr = new FamilyJoinRequest();
            jr.setId(500L);
            jr.setFamilyId(10L);
            jr.setStatus("APPROVED");
            when(familyJoinRequestRepository.findById(500L)).thenReturn(Optional.of(jr));

            assertThatThrownBy(() -> familyService.approveRequest(1L, 10L, 500L))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessage("请求已被处理");
        }
    }

    // ======================== rejectRequest ========================

    @Nested
    @DisplayName("rejectRequest")
    class RejectRequest {

        /** 验证管理员拒绝请求成功 */
        @Test
        @DisplayName("拒绝请求成功")
        void whenRejected_thenStatusUpdated() {
            FamilyMemberRole adminRole = sampleRole(1L, 10L, true);
            when(familyMemberRoleRepository.findByUserIdAndFamilyId(1L, 10L))
                    .thenReturn(Optional.of(adminRole));

            FamilyJoinRequest jr = new FamilyJoinRequest();
            jr.setId(500L);
            jr.setFamilyId(10L);
            jr.setStatus("PENDING");
            when(familyJoinRequestRepository.findById(500L)).thenReturn(Optional.of(jr));
            when(familyJoinRequestRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            familyService.rejectRequest(1L, 10L, 500L);

            assertThat(jr.getStatus()).isEqualTo("REJECTED");
            assertThat(jr.getHandledBy()).isEqualTo(1L);
        }

        /** 验证请求不存在时抛出 NotFoundException */
        @Test
        @DisplayName("请求不存在时抛出 NotFoundException")
        void whenRequestNotFound_thenThrows() {
            FamilyMemberRole adminRole = sampleRole(1L, 10L, true);
            when(familyMemberRoleRepository.findByUserIdAndFamilyId(1L, 10L))
                    .thenReturn(Optional.of(adminRole));
            when(familyJoinRequestRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> familyService.rejectRequest(1L, 10L, 999L))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessage("请求不存在");
        }
    }

    // ======================== getMyInvitations ========================

    @Nested
    @DisplayName("getMyInvitations")
    class GetMyInvitations {

        /** 验证获取用户收到的待处理邀请列表 */
        @Test
        @DisplayName("返回用户的待处理邀请列表")
        void returnsInvitationList() {
            FamilyJoinRequest jr = new FamilyJoinRequest();
            jr.setId(600L);
            jr.setFamilyId(10L);
            jr.setUserId(2L);
            jr.setRequestType("INVITE");
            jr.setStatus("PENDING");
            jr.setInitiatedBy(1L);
            jr.setRole("WIFE");
            when(familyJoinRequestRepository.findByUserIdAndRequestTypeAndStatusOrderByCreatedAtDesc(
                    2L, "INVITE", "PENDING")).thenReturn(List.of(jr));
            when(familyRepository.findById(10L)).thenReturn(Optional.of(sampleFamily(10L, 1L)));
            when(userRepository.findById(2L)).thenReturn(Optional.of(sampleUser(2L, "u2")));
            when(userRepository.findById(1L)).thenReturn(Optional.of(sampleUser(1L, "admin")));

            List<JoinRequestResponse> result = familyService.getMyInvitations(2L);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getRequestType()).isEqualTo("INVITE");
        }
    }

    // ======================== acceptInvitation ========================

    @Nested
    @DisplayName("acceptInvitation")
    class AcceptInvitation {

        /** 验证接受邀请成功时用户被加入家庭 */
        @Test
        @DisplayName("接受邀请成功")
        void whenAccepted_thenMemberAdded() {
            stubLock();
            FamilyJoinRequest jr = new FamilyJoinRequest();
            jr.setId(600L);
            jr.setFamilyId(10L);
            jr.setUserId(2L);
            jr.setStatus("PENDING");
            jr.setRole("WIFE");
            when(familyJoinRequestRepository.findById(600L)).thenReturn(Optional.of(jr));

            User user = sampleUser(2L, "u2");
            when(userRepository.findById(2L)).thenReturn(Optional.of(user));
            when(familyMemberRoleRepository.findByUserIdAndFamilyId(2L, 10L))
                    .thenReturn(Optional.empty());
            when(familyMemberRoleRepository.save(any(FamilyMemberRole.class)))
                    .thenAnswer(inv -> inv.getArgument(0));
            when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

            familyService.acceptInvitation(2L, 600L);

            assertThat(jr.getStatus()).isEqualTo("APPROVED");
            verify(familyMemberRoleRepository).save(any(FamilyMemberRole.class));
        }

        /** 验证非本人邀请时抛出 ForbiddenException */
        @Test
        @DisplayName("非本人邀请时抛出 ForbiddenException")
        void whenNotTargetUser_thenThrows() {
            stubLock();
            FamilyJoinRequest jr = new FamilyJoinRequest();
            jr.setId(600L);
            jr.setUserId(2L);
            jr.setStatus("PENDING");
            when(familyJoinRequestRepository.findById(600L)).thenReturn(Optional.of(jr));

            assertThatThrownBy(() -> familyService.acceptInvitation(99L, 600L))
                    .isInstanceOf(ForbiddenException.class)
                    .hasMessage("无权操作此邀请");
        }

        /** 验证用户已属于家庭时抛出 BadRequestException */
        @Test
        @DisplayName("已属于家庭时抛出 BadRequestException")
        void whenAlreadyInFamily_thenThrows() {
            stubLock();
            FamilyJoinRequest jr = new FamilyJoinRequest();
            jr.setId(600L);
            jr.setUserId(2L);
            jr.setStatus("PENDING");
            when(familyJoinRequestRepository.findById(600L)).thenReturn(Optional.of(jr));

            User user = sampleUser(2L, "u2");
            user.setFamilyId(99L);
            when(userRepository.findById(2L)).thenReturn(Optional.of(user));

            assertThatThrownBy(() -> familyService.acceptInvitation(2L, 600L))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessage("您已属于一个家庭");
        }
    }

    // ======================== rejectInvitation ========================

    @Nested
    @DisplayName("rejectInvitation")
    class RejectInvitation {

        /** 验证拒绝邀请成功 */
        @Test
        @DisplayName("拒绝邀请成功")
        void whenRejected_thenStatusUpdated() {
            FamilyJoinRequest jr = new FamilyJoinRequest();
            jr.setId(600L);
            jr.setUserId(2L);
            jr.setStatus("PENDING");
            when(familyJoinRequestRepository.findById(600L)).thenReturn(Optional.of(jr));
            when(familyJoinRequestRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            familyService.rejectInvitation(2L, 600L);

            assertThat(jr.getStatus()).isEqualTo("REJECTED");
        }

        /** 验证非本人邀请时抛出 ForbiddenException */
        @Test
        @DisplayName("非本人邀请时抛出 ForbiddenException")
        void whenNotTargetUser_thenThrows() {
            FamilyJoinRequest jr = new FamilyJoinRequest();
            jr.setId(600L);
            jr.setUserId(2L);
            jr.setStatus("PENDING");
            when(familyJoinRequestRepository.findById(600L)).thenReturn(Optional.of(jr));

            assertThatThrownBy(() -> familyService.rejectInvitation(99L, 600L))
                    .isInstanceOf(ForbiddenException.class)
                    .hasMessage("无权操作此邀请");
        }

        /** 验证已处理的邀请不能再拒绝 */
        @Test
        @DisplayName("已处理的邀请抛出 BadRequestException")
        void whenAlreadyProcessed_thenThrows() {
            FamilyJoinRequest jr = new FamilyJoinRequest();
            jr.setId(600L);
            jr.setUserId(2L);
            jr.setStatus("APPROVED");
            when(familyJoinRequestRepository.findById(600L)).thenReturn(Optional.of(jr));

            assertThatThrownBy(() -> familyService.rejectInvitation(2L, 600L))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessage("邀请已被处理");
        }
    }

    // ======================== setAdmin ========================

    @Nested
    @DisplayName("setAdmin")
    class SetAdmin {

        /** 验证户主设置其他成员为管理员成功 */
        @Test
        @DisplayName("户主设置管理员成功")
        @SuppressWarnings("unchecked")
        void whenCreator_thenSetsAdmin() {
            Family family = sampleFamily(10L, 1L);
            when(familyRepository.findById(10L)).thenReturn(Optional.of(family));
            FamilyMemberRole targetRole = sampleRole(2L, 10L, false);
            when(familyMemberRoleRepository.findByUserIdAndFamilyId(2L, 10L))
                    .thenReturn(Optional.of(targetRole));
            RSet<String> set = mock(RSet.class);
            doReturn(set).when(redissonClient).getSet(anyString());

            familyService.setAdmin(1L, 10L, 2L, true);

            verify(familyMemberRoleRepository).updateIsAdminByUserIdAndFamilyId(2L, 10L, true);
        }

        /** 验证非户主设置管理员时抛出 ForbiddenException */
        @Test
        @DisplayName("非户主抛出 ForbiddenException")
        void whenNotCreator_thenThrows() {
            Family family = sampleFamily(10L, 1L);
            when(familyRepository.findById(10L)).thenReturn(Optional.of(family));

            assertThatThrownBy(() -> familyService.setAdmin(2L, 10L, 3L, true))
                    .isInstanceOf(ForbiddenException.class)
                    .hasMessage("只有户主可以设置管理员");
        }

        /** 验证户主不能修改自己的管理员状态 */
        @Test
        @DisplayName("不能修改自己的管理员状态")
        void whenSelf_thenThrows() {
            Family family = sampleFamily(10L, 1L);
            when(familyRepository.findById(10L)).thenReturn(Optional.of(family));

            assertThatThrownBy(() -> familyService.setAdmin(1L, 10L, 1L, false))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessage("不能修改自己的管理员状态");
        }
    }

    // ======================== removeMember ========================

    @Nested
    @DisplayName("removeMember")
    class RemoveMember {

        /** 验证管理员移除普通成员成功 */
        @Test
        @DisplayName("管理员移除成员成功")
        @SuppressWarnings("unchecked")
        void whenAdmin_thenRemovesMember() {
            FamilyMemberRole adminRole = sampleRole(1L, 10L, true);
            when(familyMemberRoleRepository.findByUserIdAndFamilyId(1L, 10L))
                    .thenReturn(Optional.of(adminRole));
            Family family = sampleFamily(10L, 1L);
            when(familyRepository.findById(10L)).thenReturn(Optional.of(family));
            User target = sampleUser(3L, "u3");
            target.setFamilyId(10L);
            when(userRepository.findById(3L)).thenReturn(Optional.of(target));
            when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
            RSet<String> set = mock(RSet.class);
            doReturn(set).when(redissonClient).getSet(anyString());

            familyService.removeMember(1L, 10L, 3L);

            verify(familyMemberRoleRepository).deleteByUserIdAndFamilyId(3L, 10L);
            assertThat(target.getFamilyId()).isNull();
        }

        /** 验证不能移除户主 */
        @Test
        @DisplayName("不能移除户主")
        void whenTargetIsCreator_thenThrows() {
            FamilyMemberRole adminRole = sampleRole(1L, 10L, true);
            when(familyMemberRoleRepository.findByUserIdAndFamilyId(1L, 10L))
                    .thenReturn(Optional.of(adminRole));
            Family family = sampleFamily(10L, 2L);
            when(familyRepository.findById(10L)).thenReturn(Optional.of(family));

            assertThatThrownBy(() -> familyService.removeMember(1L, 10L, 2L))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessage("不能移除户主");
        }

        /** 验证不能移除自己 */
        @Test
        @DisplayName("不能移除自己")
        void whenSelf_thenThrows() {
            FamilyMemberRole adminRole = sampleRole(1L, 10L, true);
            when(familyMemberRoleRepository.findByUserIdAndFamilyId(1L, 10L))
                    .thenReturn(Optional.of(adminRole));
            Family family = sampleFamily(10L, 99L);
            when(familyRepository.findById(10L)).thenReturn(Optional.of(family));

            assertThatThrownBy(() -> familyService.removeMember(1L, 10L, 1L))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessage("不能移除自己");
        }
    }

    // ======================== addCreatedMember ========================

    @Nested
    @DisplayName("addCreatedMember")
    class AddCreatedMember {

        /** 验证直接添加已创建的用户为家庭成员 */
        @Test
        @DisplayName("添加已创建用户为家庭成员")
        void whenSuccess_thenReturnsFamilyResponse() {
            User newUser = sampleUser(3L, "newmember");
            Family family = sampleFamily(10L, 1L);
            when(familyMemberRoleRepository.save(any(FamilyMemberRole.class)))
                    .thenAnswer(inv -> inv.getArgument(0));
            when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
            when(familyRepository.findById(10L)).thenReturn(Optional.of(family));
            FamilyMemberRole existingRole = sampleRole(1L, 10L, true);
            when(familyMemberRoleRepository.findByFamilyIdOrderByJoinedAt(10L))
                    .thenReturn(List.of(existingRole));
            when(userRepository.findById(1L)).thenReturn(Optional.of(sampleUser(1L, "creator")));

            FamilyResponse response = familyService.addCreatedMember(10L, newUser, "CHILD");

            assertThat(newUser.getFamilyId()).isEqualTo(10L);
            verify(familyMemberRoleRepository).save(any(FamilyMemberRole.class));
            assertThat(response).isNotNull();
        }
    }

    // ======================== isAdmin ========================

    @Nested
    @DisplayName("isAdmin")
    class IsAdmin {

        /** 验证用户为管理员时返回 true */
        @Test
        @DisplayName("管理员返回 true")
        void whenAdmin_thenReturnsTrue() {
            FamilyMemberRole adminRole = sampleRole(1L, 10L, true);
            when(familyMemberRoleRepository.findByUserIdAndFamilyId(1L, 10L))
                    .thenReturn(Optional.of(adminRole));

            assertThat(familyService.isAdmin(1L, 10L)).isTrue();
        }

        /** 验证非管理员时返回 false */
        @Test
        @DisplayName("普通成员返回 false")
        void whenNotAdmin_thenReturnsFalse() {
            FamilyMemberRole normalRole = sampleRole(1L, 10L, false);
            when(familyMemberRoleRepository.findByUserIdAndFamilyId(1L, 10L))
                    .thenReturn(Optional.of(normalRole));

            assertThat(familyService.isAdmin(1L, 10L)).isFalse();
        }

        /** 验证不是家庭成员时返回 false */
        @Test
        @DisplayName("非家庭成员返回 false")
        void whenNotMember_thenReturnsFalse() {
            when(familyMemberRoleRepository.findByUserIdAndFamilyId(1L, 10L))
                    .thenReturn(Optional.empty());

            assertThat(familyService.isAdmin(1L, 10L)).isFalse();
        }
    }

    // ======================== normalizeRole ========================

    @Nested
    @DisplayName("normalizeRole")
    class NormalizeRole {

        /** 验证 null 和空字符串默认为 OTHER */
        @Test
        @DisplayName("null 或空字符串返回 OTHER")
        void whenNullOrBlank_thenReturnsOther() {
            assertThat(FamilyService.normalizeRole(null)).isEqualTo("OTHER");
            assertThat(FamilyService.normalizeRole("")).isEqualTo("OTHER");
            assertThat(FamilyService.normalizeRole("  ")).isEqualTo("OTHER");
        }

        /** 验证标准角色名（大小写不敏感）正确规范化 */
        @Test
        @DisplayName("标准角色名正确规范化")
        void whenValidRoles_thenReturnsNormalized() {
            assertThat(FamilyService.normalizeRole("husband")).isEqualTo("HUSBAND");
            assertThat(FamilyService.normalizeRole("WIFE")).isEqualTo("WIFE");
            assertThat(FamilyService.normalizeRole("Child")).isEqualTo("CHILD");
        }

        /** 验证未知角色名默认为 OTHER */
        @Test
        @DisplayName("未知角色名返回 OTHER")
        void whenUnknown_thenReturnsOther() {
            assertThat(FamilyService.normalizeRole("uncle")).isEqualTo("OTHER");
            assertThat(FamilyService.normalizeRole("GRANDPA")).isEqualTo("OTHER");
        }
    }
}
