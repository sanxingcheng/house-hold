package com.household.authuser.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.household.authuser.dto.request.LoginRequest;
import com.household.authuser.dto.request.RegisterRequest;
import com.household.authuser.dto.response.LoginResponse;
import com.household.authuser.dto.response.RegisterResponse;
import com.household.authuser.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthController 单元测试")
class AuthControllerTest {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private AuthService authService;

    @InjectMocks
    private AuthController authController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(authController).build();
    }

    @Nested
    @DisplayName("GET /auth/health")
    class Health {

        /** 验证健康检查端点返回 200 OK */
        @Test
        @DisplayName("返回 200 和 OK 字符串")
        void health_returns200() throws Exception {
            mockMvc.perform(get("/auth/health"))
                    .andExpect(status().isOk())
                    .andExpect(content().string("OK"));
        }
    }

    @Nested
    @DisplayName("POST /auth/register")
    class Register {

        /** 验证合法注册请求返回 200 和用户信息 */
        @Test
        @DisplayName("请求合法时返回 200 和用户信息")
        void whenValidRequest_thenReturns200AndBody() throws Exception {
            RegisterRequest req = new RegisterRequest();
            req.setUsername("user1");
            req.setName("测试");
            req.setGender("MALE");
            req.setPassword("pass123");
            req.setBirthday("1990-01-01");
            RegisterResponse res = new RegisterResponse(
                    "1", "user1", "测试",
                    "MALE", "1990-01-01",
                    null, null);
            when(authService.register(any(RegisterRequest.class))).thenReturn(res);

            mockMvc.perform(post("/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value("1"))
                    .andExpect(jsonPath("$.username").value("user1"))
                    .andExpect(jsonPath("$.birthday").value("1990-01-01"));
        }
    }

    @Nested
    @DisplayName("POST /auth/login")
    class Login {

        /** 验证合法登录请求返回 200、JWT token 和用户信息 */
        @Test
        @DisplayName("请求合法时返回 200 和 token、user")
        void whenValidRequest_thenReturns200AndToken() throws Exception {
            LoginRequest req = new LoginRequest();
            req.setUsername("u1");
            req.setPassword("pass");
            LoginResponse.UserInfo userInfo = new LoginResponse.UserInfo("1", "u1", "1990-01-01", null);
            LoginResponse res = new LoginResponse("jwt-token", userInfo);
            when(authService.login(any(LoginRequest.class))).thenReturn(res);

            mockMvc.perform(post("/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.token").value("jwt-token"))
                    .andExpect(jsonPath("$.user.username").value("u1"))
                    .andExpect(jsonPath("$.user.id").value("1"));
        }
    }

    @Nested
    @DisplayName("POST /auth/logout")
    class Logout {

        /** 验证登出端点调用 service 并返回 204 No Content */
        @Test
        @DisplayName("携带 X-User-Id 时返回 204")
        void whenLogout_thenReturns204() throws Exception {
            mockMvc.perform(post("/auth/logout")
                            .header("X-User-Id", "100"))
                    .andExpect(status().isNoContent());

            verify(authService).logout(100L);
        }
    }
}
