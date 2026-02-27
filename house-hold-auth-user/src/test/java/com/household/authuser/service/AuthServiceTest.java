package com.household.authuser.service;

import com.household.authuser.dto.request.LoginRequest;
import com.household.authuser.dto.request.RegisterRequest;
import com.household.authuser.dto.response.LoginResponse;
import com.household.authuser.dto.response.RegisterResponse;
import com.household.authuser.entity.User;
import com.household.authuser.mapper.UserMapper;
import com.household.authuser.util.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService 单元测试")
class AuthServiceTest {

    @Mock
    private UserMapper userMapper;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtUtil jwtUtil;

    @InjectMocks
    private AuthService authService;

    @Nested
    @DisplayName("register")
    class Register {

        private RegisterRequest validRequest() {
            RegisterRequest req = new RegisterRequest();
            req.setUsername("testuser");
            req.setPassword("password123");
            req.setBirthday("1990-01-15");
            req.setEmail("test@example.com");
            req.setPhone("13800138000");
            return req;
        }

        @Test
        @DisplayName("用户名已存在时抛出 UsernameExistsException")
        void whenUsernameExists_thenThrowsUsernameExists() {
            when(userMapper.selectByUsername("testuser")).thenReturn(new User());
            RegisterRequest req = validRequest();

            assertThatThrownBy(() -> authService.register(req))
                    .isInstanceOf(AuthService.UsernameExistsException.class)
                    .hasMessage("用户名已被使用");
            verify(userMapper).selectByUsername("testuser");
        }

        @Test
        @DisplayName("注册成功时加密密码并插入用户、返回响应")
        void whenRegisterSuccess_thenEncodesPasswordAndReturnsResponse() {
            when(userMapper.selectByUsername(anyString())).thenReturn(null);
            when(passwordEncoder.encode("password123")).thenReturn("encoded");

            RegisterRequest req = validRequest();
            RegisterResponse res = authService.register(req);

            assertThat(res.getUsername()).isEqualTo("testuser");
            assertThat(res.getBirthday()).isEqualTo("1990-01-15");
            assertThat(res.getEmail()).isEqualTo("test@example.com");
            assertThat(res.getPhone()).isEqualTo("13800138000");
            assertThat(res.getId()).isNotNull();

            ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
            verify(userMapper).insert(userCaptor.capture());
            User saved = userCaptor.getValue();
            assertThat(saved.getUsername()).isEqualTo("testuser");
            assertThat(saved.getPasswordHash()).isEqualTo("encoded");
            assertThat(saved.getBirthday()).isEqualTo(LocalDate.of(1990, 1, 15));
        }
    }

    @Nested
    @DisplayName("login")
    class Login {

        @Test
        @DisplayName("用户不存在时抛出 InvalidCredentialsException")
        void whenUserNotFound_thenThrowsInvalidCredentials() {
            when(userMapper.selectByUsername("nobody")).thenReturn(null);
            LoginRequest req = new LoginRequest();
            req.setUsername("nobody");
            req.setPassword("any");

            assertThatThrownBy(() -> authService.login(req))
                    .isInstanceOf(AuthService.InvalidCredentialsException.class)
                    .hasMessage("用户名或密码错误");
        }

        @Test
        @DisplayName("密码错误时抛出 InvalidCredentialsException")
        void whenPasswordWrong_thenThrowsInvalidCredentials() {
            User user = new User();
            user.setId(1L);
            user.setUsername("u1");
            user.setPasswordHash("encoded");
            user.setBirthday(LocalDate.of(1990, 1, 1));
            when(userMapper.selectByUsername("u1")).thenReturn(user);
            when(passwordEncoder.matches("wrong", "encoded")).thenReturn(false);

            LoginRequest req = new LoginRequest();
            req.setUsername("u1");
            req.setPassword("wrong");

            assertThatThrownBy(() -> authService.login(req))
                    .isInstanceOf(AuthService.InvalidCredentialsException.class)
                    .hasMessage("用户名或密码错误");
        }

        @Test
        @DisplayName("登录成功时返回 token 和用户信息")
        void whenLoginSuccess_thenReturnsTokenAndUser() {
            User user = new User();
            user.setId(100L);
            user.setUsername("u1");
            user.setPasswordHash("encoded");
            user.setBirthday(LocalDate.of(1990, 1, 1));
            when(userMapper.selectByUsername("u1")).thenReturn(user);
            when(passwordEncoder.matches("pass", "encoded")).thenReturn(true);
            when(jwtUtil.generateToken(100L, "u1")).thenReturn("jwt-here");

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
}
