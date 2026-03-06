package com.household.authuser.service;

import com.household.authuser.cache.UserCacheService;
import com.household.authuser.dto.request.UserProfileUpdateRequest;
import com.household.authuser.dto.response.UserProfileResponse;
import com.household.authuser.entity.User;
import com.household.authuser.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

/**
 * 用户信息服务，处理用户资料查询与更新
 *
 * @author household
 * @date 2025/01/01
 */
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    @Autowired(required = false)
    private UserCacheService userCacheService;

    public UserProfileResponse getProfile(Long userId) {
        if (userCacheService != null) {
            return userCacheService.get(userId, () -> loadProfileFromDb(userId));
        }
        return loadProfileFromDb(userId);
    }

    private UserProfileResponse loadProfileFromDb(Long userId) {
        return userRepository.findById(userId).map(UserService::toProfileResponse).orElse(null);
    }

    @Transactional(rollbackFor = Exception.class)
    public UserProfileResponse updateProfile(Long userId, UserProfileUpdateRequest req) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return null;
        }
        if (req.getName() != null) user.setName(req.getName());
        if (req.getBirthday() != null && !req.getBirthday().isBlank()) {
            user.setBirthday(LocalDate.parse(req.getBirthday()));
        }
        if (req.getEmail() != null) user.setEmail(req.getEmail());
        if (req.getPhone() != null) user.setPhone(req.getPhone());
        userRepository.save(user);
        if (userCacheService != null) {
            userCacheService.invalidate(userId);
        }
        return toProfileResponse(user);
    }

    private static UserProfileResponse toProfileResponse(User user) {
        return new UserProfileResponse(
                String.valueOf(user.getId()),
                user.getUsername(),
                user.getName(),
                user.getBirthday() != null ? user.getBirthday().toString() : null,
                user.getEmail(),
                user.getPhone(),
                user.getFamilyId() != null ? String.valueOf(user.getFamilyId()) : null
        );
    }
}
