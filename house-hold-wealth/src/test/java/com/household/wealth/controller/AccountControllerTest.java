package com.household.wealth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.household.common.exception.ForbiddenException;
import com.household.common.exception.NotFoundException;
import com.household.wealth.dto.request.AccountCreateRequest;
import com.household.wealth.dto.request.AccountUpdateRequest;
import com.household.wealth.dto.response.AccountResponse;
import com.household.wealth.exception.GlobalExceptionHandler;
import com.household.wealth.service.AccountService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AccountController 单元测试")
class AccountControllerTest {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private AccountService accountService;

    @InjectMocks
    private AccountController accountController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(accountController)
                .setMessageConverters(new MappingJackson2HttpMessageConverter())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private AccountResponse sampleResponse() {
        return new AccountResponse("1", "10", "工商银行", "SAVINGS", 10000L, "CNY", "2025-01-01T00:00", "2025-01-02T00:00");
    }

    @Nested
    @DisplayName("GET /wealth/accounts")
    class ListAccounts {

        @Test
        @DisplayName("正常返回 200 和账户列表")
        void returns200WithList() throws Exception {
            when(accountService.getAccounts(10L)).thenReturn(List.of(sampleResponse()));

            mockMvc.perform(get("/wealth/accounts")
                            .header("X-User-Id", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].id").value("1"))
                    .andExpect(jsonPath("$[0].accountName").value("工商银行"))
                    .andExpect(jsonPath("$[0].balance").value(10000));
        }

        @Test
        @DisplayName("无账户时返回空数组")
        void returns200WithEmptyList() throws Exception {
            when(accountService.getAccounts(10L)).thenReturn(List.of());

            mockMvc.perform(get("/wealth/accounts")
                            .header("X-User-Id", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$").isEmpty());
        }
    }

    @Nested
    @DisplayName("GET /wealth/accounts/family")
    class ListFamilyAccounts {

        @Test
        @DisplayName("管理员按家庭查询返回 200 和账户列表")
        void returns200WithFamilyAccounts() throws Exception {
            when(accountService.getFamilyAccounts(10L, 100L)).thenReturn(List.of(sampleResponse()));

            mockMvc.perform(get("/wealth/accounts/family")
                            .header("X-User-Id", "10")
                            .header("X-Family-Id", "100"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].id").value("1"))
                    .andExpect(jsonPath("$[0].accountName").value("工商银行"));
        }
    }

    @Nested
    @DisplayName("POST /wealth/accounts")
    class CreateAccount {

        @Test
        @DisplayName("创建成功返回 200")
        void returns200OnSuccess() throws Exception {
            when(accountService.createAccount(eq(10L), eq(100L), any(AccountCreateRequest.class)))
                    .thenReturn(sampleResponse());

            AccountCreateRequest req = new AccountCreateRequest();
            req.setAccountName("工商银行");
            req.setAccountType("SAVINGS");
            req.setBalance(10000L);

            mockMvc.perform(post("/wealth/accounts")
                            .header("X-User-Id", "10")
                            .header("X-Family-Id", "100")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.accountName").value("工商银行"));
        }

        @Test
        @DisplayName("缺少必填字段返回 400")
        void returns400WhenMissingRequired() throws Exception {
            AccountCreateRequest req = new AccountCreateRequest();

            mockMvc.perform(post("/wealth/accounts")
                            .header("X-User-Id", "10")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("PUT /wealth/accounts/{id}")
    class UpdateAccount {

        @Test
        @DisplayName("更新成功返回 200")
        void returns200OnSuccess() throws Exception {
            AccountResponse updated = new AccountResponse("1", "10", "新名称", "SAVINGS", 8000L,"CNY", "2025-01-01T00:00", "2025-01-01T00:00");
            when(accountService.updateAccount(eq(10L), eq(1L), any(AccountUpdateRequest.class)))
                    .thenReturn(updated);

            AccountUpdateRequest req = new AccountUpdateRequest();
            req.setBalance(8000L);

            mockMvc.perform(put("/wealth/accounts/1")
                            .header("X-User-Id", "10")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.balance").value(8000));
        }

        @Test
        @DisplayName("账户不存在返回 404")
        void returns404WhenNotFound() throws Exception {
            when(accountService.updateAccount(eq(10L), eq(999L), any(AccountUpdateRequest.class)))
                    .thenThrow(new NotFoundException("账户不存在"));

            AccountUpdateRequest req = new AccountUpdateRequest();
            req.setBalance(100L);

            mockMvc.perform(put("/wealth/accounts/999")
                            .header("X-User-Id", "10")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("NOT_FOUND"));
        }

        @Test
        @DisplayName("无权操作返回 403")
        void returns403WhenForbidden() throws Exception {
            when(accountService.updateAccount(eq(99L), eq(1L), any(AccountUpdateRequest.class)))
                    .thenThrow(new ForbiddenException("无权操作此账户"));

            AccountUpdateRequest req = new AccountUpdateRequest();
            req.setBalance(100L);

            mockMvc.perform(put("/wealth/accounts/1")
                            .header("X-User-Id", "99")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value("FORBIDDEN"));
        }
    }

    @Nested
    @DisplayName("DELETE /wealth/accounts/{id}")
    class DeleteAccount {

        @Test
        @DisplayName("删除成功返回 204")
        void returns204OnSuccess() throws Exception {
            doNothing().when(accountService).deleteAccount(10L, 1L);

            mockMvc.perform(delete("/wealth/accounts/1")
                            .header("X-User-Id", "10"))
                    .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("账户不存在返回 404")
        void returns404WhenNotFound() throws Exception {
            doThrow(new NotFoundException("账户不存在")).when(accountService).deleteAccount(10L, 999L);

            mockMvc.perform(delete("/wealth/accounts/999")
                            .header("X-User-Id", "10"))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("无权操作返回 403")
        void returns403WhenForbidden() throws Exception {
            doThrow(new ForbiddenException("无权操作此账户")).when(accountService).deleteAccount(99L, 1L);

            mockMvc.perform(delete("/wealth/accounts/1")
                            .header("X-User-Id", "99"))
                    .andExpect(status().isForbidden());
        }
    }
}
