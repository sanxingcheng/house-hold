package com.household.authuser.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.household.authuser.dto.request.ApplyJoinRequest;
import com.household.authuser.dto.request.FamilyCreateRequest;
import com.household.authuser.dto.request.FamilyMemberRoleUpdateRequest;
import com.household.authuser.dto.request.FamilyUpdateRequest;
import com.household.authuser.dto.response.FamilyResponse;
import com.household.authuser.dto.response.JoinRequestResponse;
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

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("FamilyController 单元测试")
class FamilyControllerTest {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private FamilyService familyService;

    @InjectMocks
    private FamilyController familyController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(familyController).build();
    }

    private FamilyResponse sampleFamilyResponse() {
        FamilyResponse.MemberInfo member = new FamilyResponse.MemberInfo(
                "1", "user1", "张三", "HUSBAND", true, true);
        return new FamilyResponse("10", "测试家庭", "中国", "广东", "深圳", "科技路", "1", List.of(member));
    }

    @Nested
    @DisplayName("GET /family/{familyId}/admin/check")
    class CheckAdmin {

        /** 验证管理员校验端点返回正确的 admin 状态 */
        @Test
        @DisplayName("返回管理员状态")
        void returnsAdminStatus() throws Exception {
            when(familyService.isAdmin(1L, 10L)).thenReturn(true);

            mockMvc.perform(get("/family/10/admin/check")
                            .header("X-User-Id", "1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.admin").value(true));
        }
    }

    @Nested
    @DisplayName("POST /family/create")
    class Create {

        /** 验证创建家庭端点返回 200 和家庭信息 */
        @Test
        @DisplayName("创建成功返回 200")
        void whenSuccess_thenReturns200() throws Exception {
            when(familyService.create(eq(1L), any(FamilyCreateRequest.class)))
                    .thenReturn(sampleFamilyResponse());

            FamilyCreateRequest req = new FamilyCreateRequest();
            req.setNameAlias("测试家庭");
            req.setCountry("中国");
            req.setProvince("广东");
            req.setCity("深圳");
            req.setStreet("科技路");
            req.setRole("HUSBAND");

            mockMvc.perform(post("/family/create")
                            .header("X-User-Id", "1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.nameAlias").value("测试家庭"));
        }
    }

    @Nested
    @DisplayName("GET /family/{familyId}")
    class GetFamily {

        /** 验证获取家庭信息端点返回 200 */
        @Test
        @DisplayName("获取成功返回 200")
        void whenSuccess_thenReturns200() throws Exception {
            when(familyService.getFamily(1L, 10L)).thenReturn(sampleFamilyResponse());

            mockMvc.perform(get("/family/10")
                            .header("X-User-Id", "1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value("10"));
        }
    }

    @Nested
    @DisplayName("PUT /family/{familyId}")
    class UpdateFamily {

        /** 验证更新家庭信息端点返回 200 */
        @Test
        @DisplayName("更新成功返回 200")
        void whenSuccess_thenReturns200() throws Exception {
            when(familyService.updateFamily(eq(1L), eq(10L), any(FamilyUpdateRequest.class)))
                    .thenReturn(sampleFamilyResponse());

            FamilyUpdateRequest req = new FamilyUpdateRequest();
            req.setNameAlias("新名字");
            req.setCountry("中国");
            req.setProvince("北京");
            req.setCity("北京");
            req.setStreet("长安街");

            mockMvc.perform(put("/family/10")
                            .header("X-User-Id", "1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.nameAlias").value("测试家庭"));
        }
    }

    @Nested
    @DisplayName("PUT /family/{familyId}/members/role")
    class UpdateMemberRole {

        /** 验证更新成员角色端点返回 200 */
        @Test
        @DisplayName("更新角色成功返回 200")
        void whenSuccess_thenReturns200() throws Exception {
            when(familyService.updateMemberRole(eq(1L), eq(10L), any(FamilyMemberRoleUpdateRequest.class)))
                    .thenReturn(sampleFamilyResponse());

            FamilyMemberRoleUpdateRequest req = new FamilyMemberRoleUpdateRequest();
            req.setRole("WIFE");

            mockMvc.perform(put("/family/10/members/role")
                            .header("X-User-Id", "1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("POST /family/{familyId}/apply")
    class ApplyToJoin {

        /** 验证申请加入家庭端点返回 200 和请求信息 */
        @Test
        @DisplayName("申请成功返回 200")
        void whenSuccess_thenReturns200() throws Exception {
            JoinRequestResponse response = new JoinRequestResponse(
                    "500", "10", "测试家庭", "1", "user1", "APPLY", "PENDING", "OTHER", "user1", "");
            when(familyService.applyToJoin(eq(1L), eq(10L), any(ApplyJoinRequest.class)))
                    .thenReturn(response);

            ApplyJoinRequest req = new ApplyJoinRequest();
            req.setRole("CHILD");

            mockMvc.perform(post("/family/10/apply")
                            .header("X-User-Id", "1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.requestType").value("APPLY"));
        }
    }

    @Nested
    @DisplayName("GET /family/my-invitations")
    class GetMyInvitations {

        /** 验证获取我的邀请列表端点返回 200 */
        @Test
        @DisplayName("返回邀请列表")
        void returnsInvitationList() throws Exception {
            when(familyService.getMyInvitations(1L)).thenReturn(List.of());

            mockMvc.perform(get("/family/my-invitations")
                            .header("X-User-Id", "1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray());
        }
    }

    @Nested
    @DisplayName("PUT /family/invitations/{reqId}/accept")
    class AcceptInvitation {

        /** 验证接受邀请端点返回 204 */
        @Test
        @DisplayName("接受邀请返回 204")
        void whenSuccess_thenReturns204() throws Exception {
            mockMvc.perform(put("/family/invitations/600/accept")
                            .header("X-User-Id", "1"))
                    .andExpect(status().isNoContent());

            verify(familyService).acceptInvitation(1L, 600L);
        }
    }

    @Nested
    @DisplayName("PUT /family/invitations/{reqId}/reject")
    class RejectInvitation {

        /** 验证拒绝邀请端点返回 204 */
        @Test
        @DisplayName("拒绝邀请返回 204")
        void whenSuccess_thenReturns204() throws Exception {
            mockMvc.perform(put("/family/invitations/600/reject")
                            .header("X-User-Id", "1"))
                    .andExpect(status().isNoContent());

            verify(familyService).rejectInvitation(1L, 600L);
        }
    }
}
