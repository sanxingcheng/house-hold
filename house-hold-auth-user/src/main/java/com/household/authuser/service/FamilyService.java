package com.household.authuser.service;

import com.household.authuser.cache.FamilyCacheService;
import com.household.authuser.cache.UserCacheService;
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
import com.household.common.util.FamilyAdminChecker;
import com.household.common.util.SnowflakeIdGenerator;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FamilyService {

    private static final SnowflakeIdGenerator FAMILY_ID_GEN = new SnowflakeIdGenerator(1, 2);
    private static final SnowflakeIdGenerator MEMBER_ROLE_ID_GEN = new SnowflakeIdGenerator(1, 3);
    private static final SnowflakeIdGenerator JOIN_REQ_ID_GEN = new SnowflakeIdGenerator(1, 4);

    private static final String ROLE_HUSBAND = "HUSBAND";
    private static final String ROLE_WIFE = "WIFE";
    private static final String ROLE_CHILD = "CHILD";
    private static final String ROLE_OTHER = "OTHER";

    private static final String REQ_TYPE_APPLY = "APPLY";
    private static final String REQ_TYPE_INVITE = "INVITE";
    private static final String STATUS_PENDING = "PENDING";
    private static final String STATUS_APPROVED = "APPROVED";
    private static final String STATUS_REJECTED = "REJECTED";

    private final FamilyRepository familyRepository;
    private final FamilyMemberRoleRepository familyMemberRoleRepository;
    private final FamilyJoinRequestRepository familyJoinRequestRepository;
    private final UserRepository userRepository;
    private final RedissonClient redissonClient;

    @Autowired(required = false)
    private FamilyCacheService familyCacheService;
    @Autowired(required = false)
    private UserCacheService userCacheService;

    // ======================== 基础操作 ========================

    @Transactional(rollbackFor = Exception.class)
    public FamilyResponse create(Long userId, FamilyCreateRequest req) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("用户不存在"));
        if (user.getFamilyId() != null) {
            throw new BadRequestException("您已属于一个家庭，无法重复创建");
        }
        Long familyId = FAMILY_ID_GEN.nextId();
        Family family = new Family();
        family.setId(familyId);
        family.setNameAlias(req.getNameAlias());
        family.setCountry(req.getCountry());
        family.setProvince(req.getProvince());
        family.setCity(req.getCity());
        family.setStreet(req.getStreet());
        family.setCreatedBy(userId);
        familyRepository.save(family);

        FamilyMemberRole role = new FamilyMemberRole();
        role.setId(MEMBER_ROLE_ID_GEN.nextId());
        role.setUserId(userId);
        role.setFamilyId(familyId);
        role.setRole(normalizeRole(req.getRole()));
        role.setIsAdmin(true);
        role.setJoinedAt(LocalDateTime.now());
        familyMemberRoleRepository.save(role);

        user.setFamilyId(familyId);
        userRepository.save(user);

        FamilyAdminChecker.addAdmin(redissonClient, familyId, userId);
        invalidateCaches(familyId, userId);

        return toResponse(family, List.of(role));
    }

    public FamilyResponse getFamily(Long userId, Long familyId) {
        if (familyMemberRoleRepository.findByUserIdAndFamilyId(userId, familyId).isEmpty()) {
            throw new ForbiddenException("您不属于该家庭");
        }
        if (familyCacheService != null) {
            FamilyResponse cached = familyCacheService.getFamily(familyId, () -> loadFamilyFromDb(familyId));
            if (cached == null) {
                throw new NotFoundException("家庭不存在");
            }
            return cached;
        }
        FamilyResponse resp = loadFamilyFromDb(familyId);
        if (resp == null) {
            throw new NotFoundException("家庭不存在");
        }
        return resp;
    }

    private FamilyResponse loadFamilyFromDb(Long familyId) {
        return familyRepository.findById(familyId)
                .map(family -> {
                    List<FamilyMemberRole> roles = familyMemberRoleRepository.findByFamilyIdOrderByJoinedAt(familyId);
                    return toResponse(family, roles);
                })
                .orElse(null);
    }

    @Transactional(rollbackFor = Exception.class)
    public FamilyResponse updateFamily(Long userId, Long familyId, FamilyUpdateRequest req) {
        requireAdmin(userId, familyId);
        Family family = familyRepository.findById(familyId)
                .orElseThrow(() -> new NotFoundException("家庭不存在"));
        family.setNameAlias(req.getNameAlias());
        family.setCountry(req.getCountry());
        family.setProvince(req.getProvince());
        family.setCity(req.getCity());
        family.setStreet(req.getStreet());
        familyRepository.save(family);
        invalidateCaches(familyId, null);
        List<FamilyMemberRole> roles = familyMemberRoleRepository.findByFamilyIdOrderByJoinedAt(familyId);
        return toResponse(family, roles);
    }

    @Transactional(rollbackFor = Exception.class)
    public FamilyResponse updateMemberRole(Long userId, Long familyId, FamilyMemberRoleUpdateRequest req) {
        if (familyMemberRoleRepository.findByUserIdAndFamilyId(userId, familyId).isEmpty()) {
            throw new ForbiddenException("您不属于该家庭");
        }
        String role = normalizeRole(req.getRole());
        familyMemberRoleRepository.updateRoleByUserIdAndFamilyId(userId, familyId, role);
        invalidateCaches(familyId, null);
        return getFamily(userId, familyId);
    }

    // ======================== 申请加入 ========================

    @Transactional(rollbackFor = Exception.class)
    public JoinRequestResponse applyToJoin(Long userId, Long familyId, ApplyJoinRequest req) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("用户不存在"));
        if (user.getFamilyId() != null) {
            throw new BadRequestException("您已属于一个家庭");
        }
        Family family = familyRepository.findById(familyId)
                .orElseThrow(() -> new NotFoundException("家庭不存在"));
        if (familyJoinRequestRepository.findByFamilyIdAndUserIdAndStatus(familyId, userId, STATUS_PENDING).isPresent()) {
            throw new BadRequestException("您已提交过申请，请等待审批");
        }

        FamilyJoinRequest jr = new FamilyJoinRequest();
        jr.setId(JOIN_REQ_ID_GEN.nextId());
        jr.setFamilyId(familyId);
        jr.setUserId(userId);
        jr.setRequestType(REQ_TYPE_APPLY);
        jr.setStatus(STATUS_PENDING);
        jr.setInitiatedBy(userId);
        jr.setRole(normalizeRole(req.getRole()));
        familyJoinRequestRepository.save(jr);

        return toJoinRequestResponse(jr, family, user, user);
    }

    // ======================== 管理员邀请 ========================

    @Transactional(rollbackFor = Exception.class)
    public JoinRequestResponse inviteUser(Long adminUserId, Long familyId, InviteUserRequest req) {
        requireAdmin(adminUserId, familyId);
        Family family = familyRepository.findById(familyId)
                .orElseThrow(() -> new NotFoundException("家庭不存在"));
        User invitee = userRepository.findByUsername(req.getUsername())
                .orElseThrow(() -> new NotFoundException("用户不存在: " + req.getUsername()));
        if (invitee.getFamilyId() != null) {
            throw new BadRequestException("该用户已属于一个家庭");
        }
        if (familyJoinRequestRepository.findByFamilyIdAndUserIdAndStatus(familyId, invitee.getId(), STATUS_PENDING).isPresent()) {
            throw new BadRequestException("已有待处理的请求");
        }

        FamilyJoinRequest jr = new FamilyJoinRequest();
        jr.setId(JOIN_REQ_ID_GEN.nextId());
        jr.setFamilyId(familyId);
        jr.setUserId(invitee.getId());
        jr.setRequestType(REQ_TYPE_INVITE);
        jr.setStatus(STATUS_PENDING);
        jr.setInitiatedBy(adminUserId);
        jr.setRole(normalizeRole(req.getRole()));
        familyJoinRequestRepository.save(jr);

        User admin = userRepository.findById(adminUserId).orElse(null);
        return toJoinRequestResponse(jr, family, invitee, admin);
    }

    // ======================== 审批 ========================

    public List<JoinRequestResponse> getPendingRequests(Long adminUserId, Long familyId) {
        requireAdmin(adminUserId, familyId);
        Family family = familyRepository.findById(familyId)
                .orElseThrow(() -> new NotFoundException("家庭不存在"));
        List<FamilyJoinRequest> requests = familyJoinRequestRepository
                .findByFamilyIdAndStatusOrderByCreatedAtDesc(familyId, STATUS_PENDING);
        return requests.stream().map(jr -> {
            User u = userRepository.findById(jr.getUserId()).orElse(null);
            User initiator = userRepository.findById(jr.getInitiatedBy()).orElse(null);
            return toJoinRequestResponse(jr, family, u, initiator);
        }).toList();
    }

    @Transactional(rollbackFor = Exception.class)
    public void approveRequest(Long adminUserId, Long familyId, Long requestId) {
        requireAdmin(adminUserId, familyId);
        FamilyJoinRequest jr = familyJoinRequestRepository.findById(requestId)
                .orElseThrow(() -> new NotFoundException("请求不存在"));
        if (!jr.getFamilyId().equals(familyId)) {
            throw new BadRequestException("请求不属于该家庭");
        }
        if (!STATUS_PENDING.equals(jr.getStatus())) {
            throw new BadRequestException("请求已被处理");
        }

        User user = userRepository.findById(jr.getUserId())
                .orElseThrow(() -> new NotFoundException("用户不存在"));
        if (user.getFamilyId() != null) {
            jr.setStatus(STATUS_REJECTED);
            jr.setHandledBy(adminUserId);
            jr.setHandledAt(LocalDateTime.now());
            familyJoinRequestRepository.save(jr);
            throw new BadRequestException("该用户已属于其他家庭");
        }

        addMemberToFamily(jr.getUserId(), familyId, jr.getRole());

        jr.setStatus(STATUS_APPROVED);
        jr.setHandledBy(adminUserId);
        jr.setHandledAt(LocalDateTime.now());
        familyJoinRequestRepository.save(jr);
    }

    @Transactional(rollbackFor = Exception.class)
    public void rejectRequest(Long adminUserId, Long familyId, Long requestId) {
        requireAdmin(adminUserId, familyId);
        FamilyJoinRequest jr = familyJoinRequestRepository.findById(requestId)
                .orElseThrow(() -> new NotFoundException("请求不存在"));
        if (!jr.getFamilyId().equals(familyId)) {
            throw new BadRequestException("请求不属于该家庭");
        }
        if (!STATUS_PENDING.equals(jr.getStatus())) {
            throw new BadRequestException("请求已被处理");
        }
        jr.setStatus(STATUS_REJECTED);
        jr.setHandledBy(adminUserId);
        jr.setHandledAt(LocalDateTime.now());
        familyJoinRequestRepository.save(jr);
    }

    // ======================== 用户查看/处理邀请 ========================

    public List<JoinRequestResponse> getMyInvitations(Long userId) {
        List<FamilyJoinRequest> invitations = familyJoinRequestRepository
                .findByUserIdAndRequestTypeAndStatusOrderByCreatedAtDesc(userId, REQ_TYPE_INVITE, STATUS_PENDING);
        return invitations.stream().map(jr -> {
            Family family = familyRepository.findById(jr.getFamilyId()).orElse(null);
            User user = userRepository.findById(jr.getUserId()).orElse(null);
            User initiator = userRepository.findById(jr.getInitiatedBy()).orElse(null);
            return toJoinRequestResponse(jr, family, user, initiator);
        }).toList();
    }

    @Transactional(rollbackFor = Exception.class)
    public void acceptInvitation(Long userId, Long requestId) {
        FamilyJoinRequest jr = familyJoinRequestRepository.findById(requestId)
                .orElseThrow(() -> new NotFoundException("邀请不存在"));
        if (!jr.getUserId().equals(userId)) {
            throw new ForbiddenException("无权操作此邀请");
        }
        if (!STATUS_PENDING.equals(jr.getStatus())) {
            throw new BadRequestException("邀请已被处理");
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("用户不存在"));
        if (user.getFamilyId() != null) {
            throw new BadRequestException("您已属于一个家庭");
        }

        addMemberToFamily(userId, jr.getFamilyId(), jr.getRole());

        jr.setStatus(STATUS_APPROVED);
        jr.setHandledBy(userId);
        jr.setHandledAt(LocalDateTime.now());
        familyJoinRequestRepository.save(jr);
    }

    @Transactional(rollbackFor = Exception.class)
    public void rejectInvitation(Long userId, Long requestId) {
        FamilyJoinRequest jr = familyJoinRequestRepository.findById(requestId)
                .orElseThrow(() -> new NotFoundException("邀请不存在"));
        if (!jr.getUserId().equals(userId)) {
            throw new ForbiddenException("无权操作此邀请");
        }
        if (!STATUS_PENDING.equals(jr.getStatus())) {
            throw new BadRequestException("邀请已被处理");
        }
        jr.setStatus(STATUS_REJECTED);
        jr.setHandledBy(userId);
        jr.setHandledAt(LocalDateTime.now());
        familyJoinRequestRepository.save(jr);
    }

    // ======================== 管理员成员管理 ========================

    @Transactional(rollbackFor = Exception.class)
    public void setAdmin(Long operatorId, Long familyId, Long targetUserId, boolean admin) {
        Family family = familyRepository.findById(familyId)
                .orElseThrow(() -> new NotFoundException("家庭不存在"));
        if (!family.getCreatedBy().equals(operatorId)) {
            throw new ForbiddenException("只有户主可以设置管理员");
        }
        if (operatorId.equals(targetUserId)) {
            throw new BadRequestException("不能修改自己的管理员状态");
        }
        FamilyMemberRole memberRole = familyMemberRoleRepository.findByUserIdAndFamilyId(targetUserId, familyId)
                .orElseThrow(() -> new NotFoundException("该用户不是家庭成员"));
        familyMemberRoleRepository.updateIsAdminByUserIdAndFamilyId(targetUserId, familyId, admin);
        if (admin) {
            FamilyAdminChecker.addAdmin(redissonClient, familyId, targetUserId);
        } else {
            FamilyAdminChecker.removeAdmin(redissonClient, familyId, targetUserId);
        }
        invalidateCaches(familyId, null);
    }

    @Transactional(rollbackFor = Exception.class)
    public void removeMember(Long adminUserId, Long familyId, Long targetUserId) {
        requireAdmin(adminUserId, familyId);
        Family family = familyRepository.findById(familyId)
                .orElseThrow(() -> new NotFoundException("家庭不存在"));
        if (family.getCreatedBy().equals(targetUserId)) {
            throw new BadRequestException("不能移除户主");
        }
        if (adminUserId.equals(targetUserId)) {
            throw new BadRequestException("不能移除自己");
        }
        familyMemberRoleRepository.deleteByUserIdAndFamilyId(targetUserId, familyId);
        FamilyAdminChecker.removeAdmin(redissonClient, familyId, targetUserId);

        User user = userRepository.findById(targetUserId).orElse(null);
        if (user != null && familyId.equals(user.getFamilyId())) {
            user.setFamilyId(null);
            userRepository.save(user);
            if (userCacheService != null) {
                userCacheService.invalidate(targetUserId);
            }
        }
        invalidateCaches(familyId, null);
    }

    @Transactional(rollbackFor = Exception.class)
    public FamilyResponse addCreatedMember(Long familyId, User newUser, String role) {
        FamilyMemberRole memberRole = new FamilyMemberRole();
        memberRole.setId(MEMBER_ROLE_ID_GEN.nextId());
        memberRole.setUserId(newUser.getId());
        memberRole.setFamilyId(familyId);
        memberRole.setRole(normalizeRole(role));
        memberRole.setIsAdmin(false);
        memberRole.setJoinedAt(LocalDateTime.now());
        familyMemberRoleRepository.save(memberRole);

        newUser.setFamilyId(familyId);
        userRepository.save(newUser);
        if (userCacheService != null) {
            userCacheService.invalidate(newUser.getId());
        }
        invalidateCaches(familyId, null);
        return loadFamilyFromDb(familyId);
    }

    // ======================== 内部方法 ========================

    private void addMemberToFamily(Long userId, Long familyId, String role) {
        if (familyMemberRoleRepository.findByUserIdAndFamilyId(userId, familyId).isPresent()) {
            throw new BadRequestException("用户已是家庭成员");
        }
        FamilyMemberRole memberRole = new FamilyMemberRole();
        memberRole.setId(MEMBER_ROLE_ID_GEN.nextId());
        memberRole.setUserId(userId);
        memberRole.setFamilyId(familyId);
        memberRole.setRole(normalizeRole(role));
        memberRole.setIsAdmin(false);
        memberRole.setJoinedAt(LocalDateTime.now());
        familyMemberRoleRepository.save(memberRole);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("用户不存在"));
        user.setFamilyId(familyId);
        userRepository.save(user);
        if (userCacheService != null) {
            userCacheService.invalidate(userId);
        }
        invalidateCaches(familyId, null);
    }

    private void requireAdmin(Long userId, Long familyId) {
        FamilyMemberRole memberRole = familyMemberRoleRepository.findByUserIdAndFamilyId(userId, familyId)
                .orElseThrow(() -> new ForbiddenException("您不属于该家庭"));
        if (!Boolean.TRUE.equals(memberRole.getIsAdmin())) {
            throw new ForbiddenException("需要管理员权限");
        }
    }

    public boolean isAdmin(Long userId, Long familyId) {
        return familyMemberRoleRepository.findByUserIdAndFamilyId(userId, familyId)
                .map(r -> Boolean.TRUE.equals(r.getIsAdmin()))
                .orElse(false);
    }

    private void invalidateCaches(Long familyId, Long userId) {
        if (familyCacheService != null) {
            familyCacheService.invalidateFamily(familyId);
        }
        if (userId != null && userCacheService != null) {
            userCacheService.invalidate(userId);
        }
    }

    private FamilyResponse toResponse(Family family, List<FamilyMemberRole> roles) {
        List<FamilyResponse.MemberInfo> members = new ArrayList<>(roles.size());
        for (FamilyMemberRole r : roles) {
            User u = userRepository.findById(r.getUserId()).orElse(null);
            members.add(new FamilyResponse.MemberInfo(
                    String.valueOf(r.getUserId()),
                    u != null ? u.getUsername() : "",
                    u != null ? u.getName() : null,
                    r.getRole(),
                    Boolean.TRUE.equals(r.getIsAdmin()),
                    family.getCreatedBy().equals(r.getUserId())
            ));
        }
        return new FamilyResponse(
                String.valueOf(family.getId()),
                family.getNameAlias(),
                family.getCountry(),
                family.getProvince(),
                family.getCity(),
                family.getStreet(),
                String.valueOf(family.getCreatedBy()),
                members
        );
    }

    private JoinRequestResponse toJoinRequestResponse(FamilyJoinRequest jr, Family family, User user, User initiator) {
        return new JoinRequestResponse(
                String.valueOf(jr.getId()),
                String.valueOf(jr.getFamilyId()),
                family != null ? family.getNameAlias() : "",
                String.valueOf(jr.getUserId()),
                user != null ? user.getUsername() : "",
                jr.getRequestType(),
                jr.getStatus(),
                jr.getRole(),
                initiator != null ? initiator.getUsername() : "",
                jr.getCreatedAt() != null ? jr.getCreatedAt().toString() : ""
        );
    }

    static String normalizeRole(String role) {
        if (role == null || role.isBlank()) {
            return ROLE_OTHER;
        }
        return switch (role.toUpperCase()) {
            case ROLE_HUSBAND -> ROLE_HUSBAND;
            case ROLE_WIFE -> ROLE_WIFE;
            case ROLE_CHILD -> ROLE_CHILD;
            default -> ROLE_OTHER;
        };
    }
}
