package com.household.authuser.service;

import com.household.authuser.config.JwtConfig;
import com.household.authuser.dto.request.LoginRequest;
import com.household.authuser.dto.request.RegisterRequest;
import com.household.authuser.dto.response.LoginResponse;
import com.household.authuser.dto.response.RegisterResponse;
import com.household.authuser.entity.User;
import com.household.authuser.repository.UserRepository;
import com.household.authuser.util.JwtUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService 单元测试")
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtUtil jwtUtil;
    @Mock
    private RedissonClient redissonClient;
    @Mock
    private JwtConfig jwtConfig;

    @InjectMocks
    private AuthService authService;

    @Nested
    @DisplayName("register")
    class Register {

        private RegisterRequest validRequest() {
            RegisterRequest req = new RegisterRequest();
            req.setUsername("testuser");
            req.setName("张三");
            req.setPassword("password123");
            req.setBirthday("1990-01-15");
            req.setEmail("test@example.com");
            req.setPhone("13800138000");
            return req;
        }

        /** 验证用户名已存在时抛出 UsernameExistsException */
        @Test
        @DisplayName("用户名已存在时抛出 UsernameExistsException")
        void whenUsernameExists_thenThrowsUsernameExists() {
            when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(new User()));
            RegisterRequest req = validRequest();

            assertThatThrownBy(() -> authService.register(req))
                    .isInstanceOf(AuthService.UsernameExistsException.class)
                    .hasMessage("用户名已被使用");
            verify(userRepository).findByUsername("testuser");
        }

        /** 验证正常注册流程：密码加密、用户持久化、响应字段正确 */
        @Test
        @DisplayName("注册成功时加密密码并保存用户、返回响应")
        void whenRegisterSuccess_thenEncodesPasswordAndReturnsResponse() {
            when(userRepository.findByUsername(anyString())).thenReturn(Optional.empty());
            when(passwordEncoder.encode("password123")).thenReturn("encoded");
            when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

            RegisterRequest req = validRequest();
            RegisterResponse res = authService.register(req);

            assertThat(res.getUsername()).isEqualTo("testuser");
            assertThat(res.getName()).isEqualTo("张三");
            assertThat(res.getBirthday()).isEqualTo("1990-01-15");
            assertThat(res.getEmail()).isEqualTo("test@example.com");
            assertThat(res.getPhone()).isEqualTo("13800138000");
            assertThat(res.getId()).isNotNull();

            ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(userCaptor.capture());
            User saved = userCaptor.getValue();
            assertThat(saved.getUsername()).isEqualTo("testuser");
            assertThat(saved.getName()).isEqualTo("张三");
            assertThat(saved.getPasswordHash()).isEqualTo("encoded");
            assertThat(saved.getBirthday()).isEqualTo(LocalDate.of(1990, 1, 15));
        }
    }

    @Nested
    @DisplayName("login")
    class Login {

        @SuppressWarnings("unchecked")
        private void stubRedisSession() {
            RBucket<String> bucket = mock(RBucket.class);
            doReturn(bucket).when(redissonClient).getBucket(anyString());
            when(jwtConfig.getExpirationMs()).thenReturn(3600000L);
        }

        /** 验证用户不存在时登录失败 */
        @Test
        @DisplayName("用户不存在时抛出 InvalidCredentialsException")
        void whenUserNotFound_thenThrowsInvalidCredentials() {
            when(userRepository.findByUsername("nobody")).thenReturn(Optional.empty());
            LoginRequest req = new LoginRequest();
            req.setUsername("nobody");
            req.setPassword("any");

            assertThatThrownBy(() -> authService.login(req))
                    .isInstanceOf(AuthService.InvalidCredentialsException.class)
                    .hasMessage("用户名或密码错误");
        }

        /** 验证密码不匹配时登录失败 */
        @Test
        @DisplayName("密码错误时抛出 InvalidCredentialsException")
        void whenPasswordWrong_thenThrowsInvalidCredentials() {
            User user = new User();
            user.setId(1L);
            user.setUsername("u1");
            user.setPasswordHash("encoded");
            user.setBirthday(LocalDate.of(1990, 1, 1));
            when(userRepository.findByUsername("u1")).thenReturn(Optional.of(user));
            when(passwordEncoder.matches("wrong", "encoded")).thenReturn(false);

            LoginRequest req = new LoginRequest();
            req.setUsername("u1");
            req.setPassword("wrong");

            assertThatThrownBy(() -> authService.login(req))
                    .isInstanceOf(AuthService.InvalidCredentialsException.class)
                    .hasMessage("用户名或密码错误");
        }

        /** 验证登录成功时生成 JWT、写入 Redis 会话、返回 token 和用户信息 */
        @Test
        @DisplayName("登录成功时返回 token 和用户信息")
        void whenLoginSuccess_thenReturnsTokenAndUser() {
            User user = new User();
            user.setId(100L);
            user.setUsername("u1");
            user.setPasswordHash("encoded");
            user.setBirthday(LocalDate.of(1990, 1, 1));
            when(userRepository.findByUsername("u1")).thenReturn(Optional.of(user));
            when(passwordEncoder.matches("pass", "encoded")).thenReturn(true);
            when(jwtUtil.generateToken(100L, "u1", null)).thenReturn("jwt-here");
            stubRedisSession();

            LoginRequest req = new LoginRequest();
            req.setUsername("u1");
            req.setPassword("pass");

            LoginResponse res = authService.login(req);

            assertThat(res.getToken()).isEqualTo("jwt-here");
            assertThat(res.getUser()).isNotNull();
            assertThat(res.getUser().getId()).isEqualTo("100");
            assertThat(res.getUser().getUsername()).isEqualTo("u1");
            assertThat(res.getUser().getBirthday()).isEqualTo("1990-01-01");
        }
    }

    @Nested
    @DisplayName("registerForFamily")
    class RegisterForFamily {

        /** 验证为家庭注册用户时用户名重复仍然抛出 UsernameExistsException */
        @Test
        @DisplayName("用户名已存在时抛出 UsernameExistsException")
        void whenUsernameExists_thenThrows() {
            when(userRepository.findByUsername("dup")).thenReturn(Optional.of(new User()));

            assertThatThrownBy(() ->
                    authService.registerForFamily("dup", "pass", "name", "MALE", "2000-01-01", null, null))
                    .isInstanceOf(AuthService.UsernameExistsException.class);
        }

        /** 验证为家庭注册成功时返回完整的 User 实体 */
        @Test
        @DisplayName("注册成功时返回 User 实体")
        void whenSuccess_thenReturnsUserEntity() {
            when(userRepository.findByUsername("newuser")).thenReturn(Optional.empty());
            when(passwordEncoder.encode("pass123")).thenReturn("hashed");
            when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

            User result = authService.registerForFamily("newuser", "pass123", "李四",
                    "MALE", "1995-06-15", "li@test.com", "13900139000");

            assertThat(result.getUsername()).isEqualTo("newuser");
            assertThat(result.getName()).isEqualTo("李四");
            assertThat(result.getPasswordHash()).isEqualTo("hashed");
            assertThat(result.getBirthday()).isEqualTo(LocalDate.of(1995, 6, 15));
            assertThat(result.getEmail()).isEqualTo("li@test.com");
        }
    }

    @Nested
    @DisplayName("logout")
    class Logout {

        /** 验证登出时删除 Redis 中对应的会话 key */
        @Test
        @DisplayName("登出时删除 Redis 中的会话")
        @SuppressWarnings("unchecked")
        void whenLogout_thenDeletesRedisSession() {
            RBucket<String> bucket = mock(RBucket.class);
            doReturn(bucket).when(redissonClient).getBucket("household:session:42");

            authService.logout(42L);

            verify(bucket).delete();
        }
    }
}
