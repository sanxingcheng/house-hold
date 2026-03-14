package com.household.wealth.service;

import com.household.common.exception.ForbiddenException;
import com.household.common.exception.NotFoundException;
import com.household.wealth.cache.AccountCacheService;
import com.household.wealth.client.AuthUserClient;
import com.household.wealth.dto.request.AccountCreateRequest;
import com.household.wealth.dto.request.AccountUpdateRequest;
import com.household.wealth.dto.response.AccountResponse;
import com.household.wealth.entity.Account;
import com.household.wealth.repository.AccountRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AccountService 单元测试")
class AccountServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private SnapshotService snapshotService;

    @Mock
    private AuthUserClient authUserClient;

    @InjectMocks
    private AccountService accountService;

    private Account sampleAccount(Long id, Long userId, Long familyId, String type, Long balance) {
        Account a = new Account();
        a.setId(id);
        a.setUserId(userId);
        a.setFamilyId(familyId);
        a.setAccountName("测试账户");
        a.setAccountType(type);
        a.setBalance(balance);
        a.setCurrency("CNY");
        a.setCreatedAt(LocalDateTime.of(2025, 1, 1, 0, 0));
        return a;
    }

    @Nested
    @DisplayName("getAccounts - 查询账户列表")
    class GetAccounts {

        @Test
        @DisplayName("正常查询返回账户列表")
        void whenAccountsExist_thenReturnsList() {
            Account a1 = sampleAccount(1L, 10L, 100L, "SAVINGS", 5000L);
            Account a2 = sampleAccount(2L, 10L, 100L, "CREDIT_CARD", 2000L);
            when(accountRepository.findByUserId(10L)).thenReturn(List.of(a1, a2));

            List<AccountResponse> result = accountService.getAccounts(10L);

            assertThat(result).hasSize(2);
            assertThat(result.get(0).getAccountName()).isEqualTo("测试账户");
            assertThat(result.get(1).getBalance()).isEqualTo(2000L);
        }

        @Test
        @DisplayName("无账户时返回空列表")
        void whenNoAccounts_thenReturnsEmpty() {
            when(accountRepository.findByUserId(10L)).thenReturn(List.of());

            List<AccountResponse> result = accountService.getAccounts(10L);

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("createAccount - 创建账户")
    class CreateAccount {

        @Test
        @DisplayName("创建成功时返回 AccountResponse 并触发快照")
        void whenValid_thenCreatesAndTriggersSnapshot() {
            AccountCreateRequest req = new AccountCreateRequest();
            req.setAccountName("工商银行");
            req.setAccountType("SAVINGS");
            req.setBalance(10000L);

            when(accountRepository.save(any(Account.class))).thenAnswer(inv -> inv.getArgument(0));

            AccountResponse result = accountService.createAccount(10L, 100L, req);

            assertThat(result.getAccountName()).isEqualTo("工商银行");
            assertThat(result.getBalance()).isEqualTo(10000L);
            assertThat(result.getCurrency()).isEqualTo("CNY");
            verify(accountRepository).save(any(Account.class));
            verify(snapshotService).triggerSnapshot(10L, 100L);
        }

        @Test
        @DisplayName("指定货币时使用指定值")
        void whenCurrencyProvided_thenUsesIt() {
            AccountCreateRequest req = new AccountCreateRequest();
            req.setAccountName("美元账户");
            req.setAccountType("SAVINGS");
            req.setBalance(500L);
            req.setCurrency("USD");

            when(accountRepository.save(any(Account.class))).thenAnswer(inv -> inv.getArgument(0));

            AccountResponse result = accountService.createAccount(10L, 100L, req);

            assertThat(result.getCurrency()).isEqualTo("USD");
        }
    }

    @Nested
    @DisplayName("updateAccount - 更新账户")
    class UpdateAccount {

        @Test
        @DisplayName("正常更新返回新值并触发快照")
        void whenOwnerUpdates_thenSuccess() {
            Account existing = sampleAccount(1L, 10L, 100L, "SAVINGS", 5000L);
            when(accountRepository.findById(1L)).thenReturn(Optional.of(existing));
            when(accountRepository.save(any(Account.class))).thenAnswer(inv -> inv.getArgument(0));

            AccountUpdateRequest req = new AccountUpdateRequest();
            req.setBalance(8000L);

            AccountResponse result = accountService.updateAccount(10L, 1L, req);

            assertThat(result.getBalance()).isEqualTo(8000L);
            verify(snapshotService).triggerSnapshot(10L, 100L);
        }

        @Test
        @DisplayName("账户不存在时抛出 NotFoundException")
        void whenAccountNotFound_thenThrows404() {
            when(accountRepository.findById(999L)).thenReturn(Optional.empty());

            AccountUpdateRequest req = new AccountUpdateRequest();
            req.setBalance(100L);

            assertThatThrownBy(() -> accountService.updateAccount(10L, 999L, req))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessageContaining("账户不存在");
        }

        @Test
        @DisplayName("非账户所有者更新时抛出 ForbiddenException")
        void whenNotOwner_thenThrows403() {
            Account existing = sampleAccount(1L, 10L, 100L, "SAVINGS", 5000L);
            when(accountRepository.findById(1L)).thenReturn(Optional.of(existing));

            AccountUpdateRequest req = new AccountUpdateRequest();
            req.setBalance(100L);

            assertThatThrownBy(() -> accountService.updateAccount(99L, 1L, req))
                    .isInstanceOf(ForbiddenException.class)
                    .hasMessageContaining("无权操作");
        }

        @Test
        @DisplayName("部分字段更新时其他字段保持不变")
        void whenPartialUpdate_thenOtherFieldsUnchanged() {
            Account existing = sampleAccount(1L, 10L, 100L, "SAVINGS", 5000L);
            when(accountRepository.findById(1L)).thenReturn(Optional.of(existing));
            when(accountRepository.save(any(Account.class))).thenAnswer(inv -> inv.getArgument(0));

            AccountUpdateRequest req = new AccountUpdateRequest();
            req.setAccountName("新名称");

            AccountResponse result = accountService.updateAccount(10L, 1L, req);

            assertThat(result.getAccountName()).isEqualTo("新名称");
            assertThat(result.getBalance()).isEqualTo(5000L);
            assertThat(result.getAccountType()).isEqualTo("SAVINGS");
        }
    }

    @Nested
    @DisplayName("deleteAccount - 删除账户")
    class DeleteAccount {

        @Test
        @DisplayName("正常删除并触发快照")
        void whenOwnerDeletes_thenSuccess() {
            Account existing = sampleAccount(1L, 10L, 100L, "SAVINGS", 5000L);
            when(accountRepository.findById(1L)).thenReturn(Optional.of(existing));

            accountService.deleteAccount(10L, 1L);

            verify(accountRepository).delete(existing);
            verify(snapshotService).triggerSnapshot(10L, 100L);
        }

        @Test
        @DisplayName("账户不存在时抛出 NotFoundException")
        void whenAccountNotFound_thenThrows404() {
            when(accountRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> accountService.deleteAccount(10L, 999L))
                    .isInstanceOf(NotFoundException.class);
        }

        @Test
        @DisplayName("非账户所有者删除时抛出 ForbiddenException")
        void whenNotOwner_thenThrows403() {
            Account existing = sampleAccount(1L, 10L, 100L, "SAVINGS", 5000L);
            when(accountRepository.findById(1L)).thenReturn(Optional.of(existing));

            assertThatThrownBy(() -> accountService.deleteAccount(99L, 1L))
                    .isInstanceOf(ForbiddenException.class);
        }
    }

    @Nested
    @DisplayName("管理员代管操作")
    class AdminProxy {

        @Test
        @DisplayName("管理员可查询成员账户")
        void adminCanListMemberAccounts() {
            when(authUserClient.checkFamilyAdmin(100L, 1L)).thenReturn(Map.of("admin", true));
            Account a = sampleAccount(1L, 20L, 100L, "SAVINGS", 5000L);
            when(accountRepository.findByUserId(20L)).thenReturn(List.of(a));

            List<AccountResponse> result = accountService.getAccountsForMember(1L, 100L, 20L);

            assertThat(result).hasSize(1);
        }

        @Test
        @DisplayName("非管理员代管操作抛出 ForbiddenException")
        void nonAdminGetsForbidden() {
            when(authUserClient.checkFamilyAdmin(100L, 2L)).thenReturn(Map.of("admin", false));

            assertThatThrownBy(() -> accountService.getAccountsForMember(2L, 100L, 20L))
                    .isInstanceOf(ForbiddenException.class)
                    .hasMessageContaining("管理员权限");
        }

        @Test
        @DisplayName("管理员可为成员创建账户并触发快照")
        void adminCanCreateForMember() {
            when(authUserClient.checkFamilyAdmin(100L, 1L)).thenReturn(Map.of("admin", true));
            when(accountRepository.save(any(Account.class))).thenAnswer(inv -> inv.getArgument(0));

            AccountCreateRequest req = new AccountCreateRequest();
            req.setAccountName("代管账户");
            req.setAccountType("SAVINGS");
            req.setBalance(3000L);

            AccountResponse result = accountService.createAccountForMember(1L, 100L, 20L, req);

            assertThat(result.getAccountName()).isEqualTo("代管账户");
            assertThat(result.getUserId()).isEqualTo("20");
            verify(snapshotService).triggerSnapshot(20L, 100L);
        }

        @Test
        @DisplayName("验证管理员权限调用失败时抛出 ForbiddenException")
        void whenAuthClientFails_thenThrowsForbidden() {
            when(authUserClient.checkFamilyAdmin(100L, 1L)).thenThrow(new RuntimeException("连接超时"));

            assertThatThrownBy(() -> accountService.getAccountsForMember(1L, 100L, 20L))
                    .isInstanceOf(ForbiddenException.class)
                    .hasMessageContaining("无法验证");
        }

        @Test
        @DisplayName("管理员可按家庭查看所有成员账户")
        void adminCanListFamilyAccounts() {
            when(authUserClient.checkFamilyAdmin(100L, 1L)).thenReturn(Map.of("admin", true));
            Account a1 = sampleAccount(1L, 10L, 100L, "SAVINGS", 5000L);
            Account a2 = sampleAccount(2L, 20L, 100L, "CREDIT_CARD", 2000L);
            when(accountRepository.findByFamilyId(100L)).thenReturn(List.of(a1, a2));

            List<AccountResponse> result = accountService.getFamilyAccounts(1L, 100L);

            assertThat(result).hasSize(2);
        }
    }
}
