package com.household.authuser.service;

import com.household.authuser.cache.FamilyCacheService;
import com.household.authuser.cache.UserCacheService;
import com.household.authuser.dto.request.FamilyCreateRequest;
import com.household.authuser.dto.request.FamilyJoinRequest;
import com.household.authuser.dto.request.FamilyMemberRoleUpdateRequest;
import com.household.authuser.dto.request.FamilyUpdateRequest;
import com.household.authuser.dto.response.FamilyResponse;
import com.household.authuser.entity.Family;
import com.household.authuser.entity.FamilyMemberRole;
import com.household.authuser.entity.User;
import com.household.authuser.repository.FamilyMemberRoleRepository;
import com.household.authuser.repository.FamilyRepository;
import com.household.authuser.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 家庭服务，处理家庭创建、加入、信息管理
 *
 * @author household
 * @date 2025/01/01
 */
@Service
@RequiredArgsConstructor
public class FamilyService {

    private static final AtomicLong FAMILY_ID_GEN = new AtomicLong(2_000_000_000_000L);
    private static final AtomicLong MEMBER_ROLE_ID_GEN = new AtomicLong(3_000_000_000_000L);

    private static final String ROLE_HUSBAND = "HUSBAND";
    private static final String ROLE_WIFE = "WIFE";
    private static final String ROLE_CHILD = "CHILD";
    private static final String ROLE_OTHER = "OTHER";

    private final FamilyRepository familyRepository;
    private final FamilyMemberRoleRepository familyMemberRoleRepository;
    private final UserRepository userRepository;
    @Autowired(required = false)
    private FamilyCacheService familyCacheService;
    @Autowired(required = false)
    private UserCacheService userCacheService;

    @Transactional(rollbackFor = Exception.class)
    public FamilyResponse create(Long userId, FamilyCreateRequest req) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            throw new NotFoundException("用户不存在");
        }
        if (user.getFamilyId() != null) {
            throw new AlreadyInFamilyException("您已属于一个家庭，无法重复创建");
        }
        Long familyId = FAMILY_ID_GEN.incrementAndGet();
        Family family = new Family();
        family.setId(familyId);
        family.setNameAlias(req.getNameAlias());
        family.setCountry(req.getCountry());
        family.setProvince(req.getProvince());
        family.setCity(req.getCity());
        family.setStreet(req.getStreet());
        familyRepository.save(family);
        if (familyCacheService != null) {
            familyCacheService.invalidateFamily(familyId);
        }

        FamilyMemberRole role = new FamilyMemberRole();
        role.setId(MEMBER_ROLE_ID_GEN.incrementAndGet());
        role.setUserId(userId);
        role.setFamilyId(familyId);
        role.setRole(normalizeRole(req.getRole()));
        role.setJoinedAt(LocalDateTime.now());
        familyMemberRoleRepository.save(role);

        user.setFamilyId(familyId);
        userRepository.save(user);
        if (userCacheService != null) {
            userCacheService.invalidate(userId);
        }
        return toResponse(family, List.of(role));
    }

    @Transactional(rollbackFor = Exception.class)
    public FamilyResponse join(Long userId, FamilyJoinRequest req) {
        if (req.getFamilyId() == null || req.getFamilyId().isBlank()) {
            throw new BadRequestException("家庭ID不能为空");
        }
        Long familyId;
        try {
            familyId = Long.parseLong(req.getFamilyId().trim());
        } catch (NumberFormatException e) {
            throw new BadRequestException("家庭ID格式不正确");
        }
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            throw new NotFoundException("用户不存在");
        }
        if (user.getFamilyId() != null) {
            throw new AlreadyInFamilyException("您已属于一个家庭，无法重复加入");
        }
        Family family = familyRepository.findById(familyId).orElse(null);
        if (family == null) {
            throw new NotFoundException("家庭不存在");
        }
        if (familyMemberRoleRepository.findByUserIdAndFamilyId(userId, familyId).isPresent()) {
            throw new BadRequestException("您已是该家庭成员");
        }
        FamilyMemberRole role = new FamilyMemberRole();
        role.setId(MEMBER_ROLE_ID_GEN.incrementAndGet());
        role.setUserId(userId);
        role.setFamilyId(familyId);
        role.setRole(normalizeRole(req.getRole()));
        role.setJoinedAt(LocalDateTime.now());
        familyMemberRoleRepository.save(role);

        user.setFamilyId(familyId);
        userRepository.save(user);
        if (userCacheService != null) {
            userCacheService.invalidate(userId);
        }
        if (familyCacheService != null) {
            familyCacheService.invalidateFamily(familyId);
        }
        return getFamily(userId, familyId);
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
        return loadFamilyFromDb(familyId);
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
        if (familyMemberRoleRepository.findByUserIdAndFamilyId(userId, familyId).isEmpty()) {
            throw new ForbiddenException("您不属于该家庭，无法修改");
        }
        Family family = familyRepository.findById(familyId)
                .orElseThrow(() -> new NotFoundException("家庭不存在"));
        family.setNameAlias(req.getNameAlias());
        family.setCountry(req.getCountry());
        family.setProvince(req.getProvince());
        family.setCity(req.getCity());
        family.setStreet(req.getStreet());
        familyRepository.save(family);
        if (familyCacheService != null) {
            familyCacheService.invalidateFamily(familyId);
        }
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
        if (familyCacheService != null) {
            familyCacheService.invalidateFamily(familyId);
        }
        return getFamily(userId, familyId);
    }

    private FamilyResponse toResponse(Family family, List<FamilyMemberRole> roles) {
        List<FamilyResponse.MemberInfo> members = new ArrayList<>(roles.size());
        for (FamilyMemberRole r : roles) {
            User u = userRepository.findById(r.getUserId()).orElse(null);
            members.add(new FamilyResponse.MemberInfo(
                    String.valueOf(r.getUserId()),
                    u != null ? u.getUsername() : "",
                    u != null ? u.getName() : null,
                    r.getRole()
            ));
        }
        return new FamilyResponse(
                String.valueOf(family.getId()),
                family.getNameAlias(),
                family.getCountry(),
                family.getProvince(),
                family.getCity(),
                family.getStreet(),
                members
        );
    }

    private static String normalizeRole(String role) {
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

    public static class NotFoundException extends RuntimeException {
        public NotFoundException(String message) { super(message); }
    }

    public static class AlreadyInFamilyException extends RuntimeException {
        public AlreadyInFamilyException(String message) { super(message); }
    }

    public static class BadRequestException extends RuntimeException {
        public BadRequestException(String message) { super(message); }
    }

    public static class ForbiddenException extends RuntimeException {
        public ForbiddenException(String message) { super(message); }
    }
}
