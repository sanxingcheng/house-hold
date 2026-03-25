package com.household.wealth.service;

import com.household.common.exception.BadRequestException;
import com.household.common.exception.ForbiddenException;
import com.household.common.exception.NotFoundException;
import com.household.common.util.SnowflakeIdGenerator;
import com.household.wealth.cache.AccountCacheService;
import com.household.wealth.client.AuthUserClient;
import com.household.wealth.dto.request.AccountCreateRequest;
import com.household.wealth.dto.request.AccountUpdateRequest;
import com.household.wealth.dto.response.AccountBalancePointResponse;
import com.household.wealth.dto.response.AccountResponse;
import com.household.wealth.entity.Account;
import com.household.wealth.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * 账户服务，处理账户的增删改查
 *
 * @author household
 * @date 2025/01/01
 */
@Service
@RequiredArgsConstructor
public class AccountService {

    private final AccountRepository accountRepository;
    private final SnapshotService snapshotService;
    private final AuthUserClient authUserClient;

    @Autowired(required = false)
    private AccountCacheService accountCacheService;

    @Autowired(required = false)
    private OperationLogService operationLogService;

    private static final SnowflakeIdGenerator ACCOUNT_ID_GEN = new SnowflakeIdGenerator(2, 1);
    private static final String DEFAULT_CURRENCY = "CNY";
    private static final java.util.Set<String> INVESTMENT_TYPES = java.util.Set.of("STOCK", "FUND");

    public List<AccountResponse> getAccounts(Long userId) {
        if (accountCacheService != null) {
            return accountCacheService.getAccountList("accounts:user:" + userId,
                    () -> loadFromDb(userId));
        }
        return loadFromDb(userId);
    }

    /**
     * 户主/管理员查看家庭内所有成员的账户列表。
     */
    public List<AccountResponse> getFamilyAccounts(Long adminUserId, Long familyId) {
        requireFamilyAdmin(adminUserId, familyId);
        if (accountCacheService != null) {
            return accountCacheService.getAccountList("accounts:family:" + familyId,
                    () -> loadFamilyFromDb(familyId));
        }
        return loadFamilyFromDb(familyId);
    }

    @Transactional(rollbackFor = Exception.class)
    public AccountResponse createAccount(Long userId, Long familyId, AccountCreateRequest req) {
        Account account = new Account();
        account.setId(ACCOUNT_ID_GEN.nextId());
        account.setUserId(userId);
        account.setFamilyId(familyId);
        account.setAccountName(req.getAccountName());
        account.setAccountType(req.getAccountType());
        account.setBalance(req.getBalance());
        account.setCurrency(req.getCurrency() != null ? req.getCurrency() : DEFAULT_CURRENCY);
        account.setAvailableImmediately(resolveAvailableImmediately(req.getAccountType(), req.getAvailableImmediately()));
        account.setRemark(req.getRemark());
        accountRepository.save(account);

        invalidateCaches(userId, familyId);
        snapshotService.triggerSnapshot(userId, familyId);
        logOperation(userId, familyId, "CREATE", "ACCOUNT", String.valueOf(account.getId()), req.getAccountName());

        return toResponse(account);
    }

    @Transactional(rollbackFor = Exception.class)
    public AccountResponse updateAccount(Long userId, Long accountId, AccountUpdateRequest req) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new NotFoundException("账户不存在"));
        if (!account.getUserId().equals(userId)) {
            throw new ForbiddenException("无权操作此账户");
        }

        if (req.getAccountName() != null) account.setAccountName(req.getAccountName());
        if (req.getAccountType() != null) account.setAccountType(req.getAccountType());
        if (req.getBalance() != null) account.setBalance(req.getBalance());
        if (req.getCurrency() != null) account.setCurrency(req.getCurrency());
        if (req.getAvailableImmediately() != null) account.setAvailableImmediately(req.getAvailableImmediately());
        if (req.getRemark() != null) account.setRemark(req.getRemark());
        accountRepository.save(account);

        invalidateCaches(userId, account.getFamilyId());
        snapshotService.triggerSnapshot(userId, account.getFamilyId());
        logOperation(userId, account.getFamilyId(), "UPDATE", "ACCOUNT", String.valueOf(accountId), account.getAccountName());

        return toResponse(account);
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteAccount(Long userId, Long accountId) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new NotFoundException("账户不存在"));
        if (!account.getUserId().equals(userId)) {
            throw new ForbiddenException("无权操作此账户");
        }

        Long familyId = account.getFamilyId();
        String name = account.getAccountName();
        accountRepository.delete(account);

        invalidateCaches(userId, familyId);
        snapshotService.triggerSnapshot(userId, familyId);
        logOperation(userId, familyId, "DELETE", "ACCOUNT", String.valueOf(accountId), name);
    }

    private List<AccountResponse> loadFromDb(Long userId) {
        return accountRepository.findByUserId(userId).stream()
                .map(this::toResponse)
                .toList();
    }

    private List<AccountResponse> loadFamilyFromDb(Long familyId) {
        return accountRepository.findByFamilyId(familyId).stream()
                .map(this::toResponse)
                .toList();
    }

    private void invalidateCaches(Long userId, Long familyId) {
        if (accountCacheService != null) {
            accountCacheService.invalidateUser(userId);
            accountCacheService.invalidateFamily(familyId);
        }
    }

    // ======================== 管理员代管操作 ========================

    public List<AccountResponse> getAccountsForMember(Long adminUserId, Long familyId, Long targetUserId) {
        requireFamilyAdmin(adminUserId, familyId);
        return loadFromDb(targetUserId);
    }

    /**
     * 查询账户余额变化趋势。本人可查自己的账户；管理员可查家庭内成员账户。
     */
    public List<AccountBalancePointResponse> getAccountBalanceHistory(Long userId, Long familyId, Long accountId,
                                                                     LocalDate from, LocalDate to) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new NotFoundException("账户不存在"));
        if (account.getUserId().equals(userId)) {
            return snapshotService.getAccountBalanceHistory(accountId, from, to);
        }
        if (familyId != null && familyId.equals(account.getFamilyId())) {
            requireFamilyAdmin(userId, familyId);
            return snapshotService.getAccountBalanceHistory(accountId, from, to);
        }
        throw new ForbiddenException("无权查看该账户");
    }

    @Transactional(rollbackFor = Exception.class)
    public AccountResponse createAccountForMember(Long adminUserId, Long familyId, Long targetUserId,
                                                   AccountCreateRequest req) {
        requireFamilyAdmin(adminUserId, familyId);
        Account account = new Account();
        account.setId(ACCOUNT_ID_GEN.nextId());
        account.setUserId(targetUserId);
        account.setFamilyId(familyId);
        account.setAccountName(req.getAccountName());
        account.setAccountType(req.getAccountType());
        account.setBalance(req.getBalance());
        account.setCurrency(req.getCurrency() != null ? req.getCurrency() : DEFAULT_CURRENCY);
        account.setAvailableImmediately(resolveAvailableImmediately(req.getAccountType(), req.getAvailableImmediately()));
        account.setRemark(req.getRemark());
        accountRepository.save(account);
        invalidateCaches(targetUserId, familyId);
        snapshotService.triggerSnapshot(targetUserId, familyId);
        logOperation(adminUserId, familyId, "CREATE", "ACCOUNT", String.valueOf(account.getId()), req.getAccountName());
        return toResponse(account);
    }

    @Transactional(rollbackFor = Exception.class)
    public AccountResponse updateAccountForMember(Long adminUserId, Long familyId, Long targetUserId,
                                                   Long accountId, AccountUpdateRequest req) {
        requireFamilyAdmin(adminUserId, familyId);
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new NotFoundException("账户不存在"));
        if (!account.getUserId().equals(targetUserId)) {
            throw new ForbiddenException("账户不属于该成员");
        }
        if (req.getAccountName() != null) account.setAccountName(req.getAccountName());
        if (req.getAccountType() != null) account.setAccountType(req.getAccountType());
        if (req.getBalance() != null) account.setBalance(req.getBalance());
        if (req.getCurrency() != null) account.setCurrency(req.getCurrency());
        if (req.getAvailableImmediately() != null) account.setAvailableImmediately(req.getAvailableImmediately());
        if (req.getRemark() != null) account.setRemark(req.getRemark());
        accountRepository.save(account);
        invalidateCaches(targetUserId, familyId);
        snapshotService.triggerSnapshot(targetUserId, familyId);
        logOperation(adminUserId, familyId, "UPDATE", "ACCOUNT", String.valueOf(accountId), account.getAccountName());
        return toResponse(account);
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteAccountForMember(Long adminUserId, Long familyId, Long targetUserId, Long accountId) {
        requireFamilyAdmin(adminUserId, familyId);
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new NotFoundException("账户不存在"));
        if (!account.getUserId().equals(targetUserId)) {
            throw new ForbiddenException("账户不属于该成员");
        }
        String name = account.getAccountName();
        accountRepository.delete(account);
        invalidateCaches(targetUserId, familyId);
        snapshotService.triggerSnapshot(targetUserId, familyId);
        logOperation(adminUserId, familyId, "DELETE", "ACCOUNT", String.valueOf(accountId), name);
    }

    private void logOperation(Long userId, Long familyId, String action, String resourceType, String resourceId, String detail) {
        if (operationLogService != null && familyId != null) {
            operationLogService.createLog(userId, familyId, action, resourceType, resourceId, detail);
        }
    }

    private void requireFamilyAdmin(Long userId, Long familyId) {
        try {
            Boolean admin = authUserClient.checkFamilyAdmin(familyId, userId).get("admin");
            if (!Boolean.TRUE.equals(admin)) {
                throw new ForbiddenException("需要家庭管理员权限");
            }
        } catch (Exception e) {
            if (e instanceof ForbiddenException) throw e;
            throw new ForbiddenException("无法验证管理员权限");
        }
    }

    /** 信用卡无“是否立即可用”含义；投资类默认 false；其他默认 true */
    private static Boolean resolveAvailableImmediately(String accountType, Boolean fromRequest) {
        if ("CREDIT_CARD".equals(accountType)) return true;
        if (fromRequest != null) return fromRequest;
        return !INVESTMENT_TYPES.contains(accountType);
    }

    private AccountResponse toResponse(Account a) {
        return new AccountResponse(
                String.valueOf(a.getId()),
                String.valueOf(a.getUserId()),
                a.getAccountName(),
                a.getAccountType(),
                a.getBalance(),
                a.getCurrency(),
                a.getAvailableImmediately(),
                a.getRemark(),
                a.getCreatedAt() != null ? a.getCreatedAt().toString() : null,
                a.getUpdatedAt() != null ? a.getUpdatedAt().toString() : null);
    }

}
