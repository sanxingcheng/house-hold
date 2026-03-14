package com.household.authuser.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.household.authuser.dto.request.UserProfileUpdateRequest;
import com.household.authuser.dto.response.UserProfileResponse;
import com.household.authuser.service.UserService;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserController 单元测试")
class UserControllerTest {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private UserService userService;

    @InjectMocks
    private UserController userController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(userController).build();
    }

    @Nested
    @DisplayName("GET /user/profile")
    class GetProfile {

        /** 验证用户存在时返回 200 和完整的用户资料 */
        @Test
        @DisplayName("用户存在时返回 200 和用户资料")
        void whenUserExists_thenReturns200() throws Exception {
            UserProfileResponse profile = new UserProfileResponse(
                    "1", "testuser", "张三", null,
                    "1990-01-01", "test@example.com", "13800138000", "100");
            when(userService.getProfile(1L)).thenReturn(profile);

            mockMvc.perform(get("/user/profile")
                            .header("X-User-Id", "1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value("1"))
                    .andExpect(jsonPath("$.username").value("testuser"))
                    .andExpect(jsonPath("$.name").value("张三"))
                    .andExpect(jsonPath("$.familyId").value("100"));
        }

        /** 验证用户不存在时返回 404 */
        @Test
        @DisplayName("用户不存在时返回 404")
        void whenUserNotFound_thenReturns404() throws Exception {
            when(userService.getProfile(999L)).thenReturn(null);

            mockMvc.perform(get("/user/profile")
                            .header("X-User-Id", "999"))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("PUT /user/profile")
    class UpdateProfile {

        /** 验证更新成功时返回 200 和更新后的资料 */
        @Test
        @DisplayName("更新成功时返回 200")
        void whenSuccess_thenReturns200() throws Exception {
            UserProfileResponse updated = new UserProfileResponse(
                    "1", "testuser", "李四", null,
                    "2000-01-01", "new@example.com", "13900139000", null);
            when(userService.updateProfile(eq(1L), any(UserProfileUpdateRequest.class))).thenReturn(updated);

            UserProfileUpdateRequest req = new UserProfileUpdateRequest();
            req.setName("李四");
            req.setBirthday("2000-01-01");

            mockMvc.perform(put("/user/profile")
                            .header("X-User-Id", "1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value("李四"));
        }

        /** 验证用户不存在时返回 404 */
        @Test
        @DisplayName("用户不存在时返回 404")
        void whenUserNotFound_thenReturns404() throws Exception {
            when(userService.updateProfile(eq(999L), any(UserProfileUpdateRequest.class))).thenReturn(null);

            UserProfileUpdateRequest req = new UserProfileUpdateRequest();
            req.setName("名字");

            mockMvc.perform(put("/user/profile")
                            .header("X-User-Id", "999")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isNotFound());
        }
    }
}
