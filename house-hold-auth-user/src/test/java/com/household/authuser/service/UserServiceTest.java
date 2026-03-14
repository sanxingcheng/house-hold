package com.household.authuser.service;

import com.household.authuser.dto.request.UserProfileUpdateRequest;
import com.household.authuser.dto.response.UserProfileResponse;
import com.household.authuser.entity.User;
import com.household.authuser.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserService 单元测试")
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    private User sampleUser() {
        User user = new User();
        user.setId(1L);
        user.setUsername("testuser");
        user.setName("张三");
        user.setBirthday(LocalDate.of(1990, 5, 20));
        user.setEmail("test@example.com");
        user.setPhone("13800138000");
        user.setFamilyId(100L);
        return user;
    }

    @Nested
    @DisplayName("getProfile")
    class GetProfile {

        /** 验证用户存在时返回正确的 UserProfileResponse */
        @Test
        @DisplayName("用户存在时返回 UserProfileResponse")
        void whenUserExists_thenReturnsProfile() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(sampleUser()));

            UserProfileResponse profile = userService.getProfile(1L);

            assertThat(profile).isNotNull();
            assertThat(profile.getId()).isEqualTo("1");
            assertThat(profile.getUsername()).isEqualTo("testuser");
            assertThat(profile.getName()).isEqualTo("张三");
            assertThat(profile.getBirthday()).isEqualTo("1990-05-20");
            assertThat(profile.getEmail()).isEqualTo("test@example.com");
            assertThat(profile.getPhone()).isEqualTo("13800138000");
            assertThat(profile.getFamilyId()).isEqualTo("100");
        }

        /** 验证用户不存在时返回 null */
        @Test
        @DisplayName("用户不存在时返回 null")
        void whenUserNotFound_thenReturnsNull() {
            when(userRepository.findById(999L)).thenReturn(Optional.empty());

            UserProfileResponse profile = userService.getProfile(999L);

            assertThat(profile).isNull();
        }

        /** 验证用户未加入家庭时 familyId 字段为 null */
        @Test
        @DisplayName("用户无家庭时 familyId 为 null")
        void whenNoFamily_thenFamilyIdIsNull() {
            User user = sampleUser();
            user.setFamilyId(null);
            when(userRepository.findById(1L)).thenReturn(Optional.of(user));

            UserProfileResponse profile = userService.getProfile(1L);

            assertThat(profile.getFamilyId()).isNull();
        }
    }

    @Nested
    @DisplayName("updateProfile")
    class UpdateProfile {

        /** 验证更新全部字段时 DB 保存并返回新值 */
        @Test
        @DisplayName("更新全部字段时返回新值")
        void whenUpdateAllFields_thenReturnsUpdatedProfile() {
            User user = sampleUser();
            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
            when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

            UserProfileUpdateRequest req = new UserProfileUpdateRequest();
            req.setName("李四");
            req.setBirthday("2000-01-01");
            req.setEmail("new@example.com");
            req.setPhone("13900139000");

            UserProfileResponse result = userService.updateProfile(1L, req);

            assertThat(result.getName()).isEqualTo("李四");
            assertThat(result.getBirthday()).isEqualTo("2000-01-01");
            assertThat(result.getEmail()).isEqualTo("new@example.com");
            assertThat(result.getPhone()).isEqualTo("13900139000");
            verify(userRepository).save(user);
        }

        /** 验证只更新部分字段时，其他字段保持不变 */
        @Test
        @DisplayName("仅更新姓名时其他字段不变")
        void whenUpdateNameOnly_thenOtherFieldsUnchanged() {
            User user = sampleUser();
            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
            when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

            UserProfileUpdateRequest req = new UserProfileUpdateRequest();
            req.setName("新名字");

            UserProfileResponse result = userService.updateProfile(1L, req);

            assertThat(result.getName()).isEqualTo("新名字");
            assertThat(result.getBirthday()).isEqualTo("1990-05-20");
            assertThat(result.getEmail()).isEqualTo("test@example.com");
        }

        /** 验证用户不存在时返回 null */
        @Test
        @DisplayName("用户不存在时返回 null")
        void whenUserNotFound_thenReturnsNull() {
            when(userRepository.findById(999L)).thenReturn(Optional.empty());

            UserProfileUpdateRequest req = new UserProfileUpdateRequest();
            req.setName("新名字");

            UserProfileResponse result = userService.updateProfile(999L, req);

            assertThat(result).isNull();
            verify(userRepository, never()).save(any());
        }

        /** 验证空生日字符串不会修改生日字段 */
        @Test
        @DisplayName("空生日字符串不修改生日")
        void whenBirthdayIsBlank_thenBirthdayUnchanged() {
            User user = sampleUser();
            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
            when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

            UserProfileUpdateRequest req = new UserProfileUpdateRequest();
            req.setBirthday("  ");

            UserProfileResponse result = userService.updateProfile(1L, req);

            assertThat(result.getBirthday()).isEqualTo("1990-05-20");
        }
    }
}
