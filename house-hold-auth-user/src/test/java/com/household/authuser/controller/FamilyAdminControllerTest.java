package com.household.authuser.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.household.authuser.dto.request.CreateMemberRequest;
import com.household.authuser.dto.request.InviteUserRequest;
import com.household.authuser.dto.request.SetAdminRequest;
import com.household.authuser.dto.response.FamilyResponse;
import com.household.authuser.dto.response.JoinRequestResponse;
import com.household.authuser.entity.User;
import com.household.authuser.service.AuthService;
import com.household.authuser.service.FamilyService;
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

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("FamilyAdminController 单元测试")
class FamilyAdminControllerTest {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private FamilyService familyService;
    @Mock
    private AuthService authService;

    @InjectMocks
    private FamilyAdminController familyAdminController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(familyAdminController).build();
    }

    private FamilyResponse sampleFamilyResponse() {
        FamilyResponse.MemberInfo member = new FamilyResponse.MemberInfo(
                "1", "user1", "张三", "HUSBAND", true, true);
        return new FamilyResponse("10", "测试家庭", "中国", "广东", "深圳", "科技路", "1", List.of(member));
    }

    @Nested
    @DisplayName("POST /family/{familyId}/admin/create-member")
    class CreateMember {

        /** 验证管理员创建家庭成员成功时返回 200 和家庭信息 */
        @Test
        @DisplayName("管理员创建成员成功返回 200")
        void whenAdmin_thenReturns200() throws Exception {
            when(familyService.isAdmin(1L, 10L)).thenReturn(true);

            User newUser = new User();
            newUser.setId(2L);
            newUser.setUsername("newuser");
            newUser.setBirthday(LocalDate.of(2000, 1, 1));
            when(authService.registerForFamily(
                    eq("newuser"), eq("pass12"), eq("小明"),
                    eq("MALE"), eq("2000-01-01"), isNull(), isNull()))
                    .thenReturn(newUser);
            when(familyService.addCreatedMember(eq(10L), any(User.class), eq("CHILD")))
                    .thenReturn(sampleFamilyResponse());

            CreateMemberRequest req = new CreateMemberRequest();
            req.setUsername("newuser");
            req.setPassword("pass12");
            req.setName("小明");
            req.setGender("MALE");
            req.setBirthday("2000-01-01");
            req.setRole("CHILD");

            mockMvc.perform(post("/family/10/admin/create-member")
                            .header("X-User-Id", "1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.nameAlias").value("测试家庭"));
        }

        /** 验证非管理员创建成员时返回 403 */
        @Test
        @DisplayName("非管理员返回 403")
        void whenNotAdmin_thenReturns403() throws Exception {
            when(familyService.isAdmin(1L, 10L)).thenReturn(false);

            CreateMemberRequest req = new CreateMemberRequest();
            req.setUsername("newuser");
            req.setPassword("pass12");
            req.setName("小明");
            req.setGender("MALE");
            req.setBirthday("2000-01-01");

            mockMvc.perform(post("/family/10/admin/create-member")
                            .header("X-User-Id", "1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("POST /family/{familyId}/admin/invite")
    class InviteUser {

        /** 验证邀请用户端点返回 200 */
        @Test
        @DisplayName("邀请成功返回 200")
        void whenSuccess_thenReturns200() throws Exception {
            JoinRequestResponse response = new JoinRequestResponse(
                    "500", "10", "测试家庭", "2", "invitee", "INVITE", "PENDING", "WIFE", "admin", "");
            when(familyService.inviteUser(eq(1L), eq(10L), any(InviteUserRequest.class)))
                    .thenReturn(response);

            InviteUserRequest req = new InviteUserRequest();
            req.setUsername("invitee");
            req.setRole("WIFE");

            mockMvc.perform(post("/family/10/admin/invite")
                            .header("X-User-Id", "1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.requestType").value("INVITE"));
        }
    }

    @Nested
    @DisplayName("PUT /family/{familyId}/admin/members/{targetUserId}/set-admin")
    class SetAdmin {

        /** 验证设置管理员端点返回 204 */
        @Test
        @DisplayName("设置管理员返回 204")
        void whenSuccess_thenReturns204() throws Exception {
            SetAdminRequest req = new SetAdminRequest();
            req.setAdmin(true);

            mockMvc.perform(put("/family/10/admin/members/2/set-admin")
                            .header("X-User-Id", "1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isNoContent());

            verify(familyService).setAdmin(1L, 10L, 2L, true);
        }
    }

    @Nested
    @DisplayName("DELETE /family/{familyId}/admin/members/{targetUserId}")
    class RemoveMember {

        /** 验证移除成员端点返回 204 */
        @Test
        @DisplayName("移除成员返回 204")
        void whenSuccess_thenReturns204() throws Exception {
            mockMvc.perform(delete("/family/10/admin/members/3")
                            .header("X-User-Id", "1"))
                    .andExpect(status().isNoContent());

            verify(familyService).removeMember(1L, 10L, 3L);
        }
    }

    @Nested
    @DisplayName("GET /family/{familyId}/admin/requests")
    class GetPendingRequests {

        /** 验证获取待审批请求列表端点返回 200 */
        @Test
        @DisplayName("返回待审批列表")
        void returnsRequestList() throws Exception {
            when(familyService.getPendingRequests(1L, 10L)).thenReturn(List.of());

            mockMvc.perform(get("/family/10/admin/requests")
                            .header("X-User-Id", "1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray());
        }
    }

    @Nested
    @DisplayName("PUT /family/{familyId}/admin/requests/{reqId}/approve")
    class ApproveRequest {

        /** 验证审批通过端点返回 204 */
        @Test
        @DisplayName("审批通过返回 204")
        void whenSuccess_thenReturns204() throws Exception {
            mockMvc.perform(put("/family/10/admin/requests/500/approve")
                            .header("X-User-Id", "1"))
                    .andExpect(status().isNoContent());

            verify(familyService).approveRequest(1L, 10L, 500L);
        }
    }

    @Nested
    @DisplayName("PUT /family/{familyId}/admin/requests/{reqId}/reject")
    class RejectRequest {

        /** 验证审批拒绝端点返回 204 */
        @Test
        @DisplayName("审批拒绝返回 204")
        void whenSuccess_thenReturns204() throws Exception {
            mockMvc.perform(put("/family/10/admin/requests/500/reject")
                            .header("X-User-Id", "1"))
                    .andExpect(status().isNoContent());

            verify(familyService).rejectRequest(1L, 10L, 500L);
        }
    }
}
