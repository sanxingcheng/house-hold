package com.household.wealth.service;

import com.household.common.exception.BadRequestException;
import com.household.common.exception.ForbiddenException;
import com.household.common.exception.NotFoundException;
import com.household.common.util.FamilyAdminChecker;
import com.household.common.util.SnowflakeIdGenerator;
import com.household.wealth.cache.AccountCacheService;
import com.household.wealth.dto.request.AccountCreateRequest;
import com.household.wealth.dto.request.AccountUpdateRequest;
import com.household.wealth.dto.response.AccountResponse;
import com.household.wealth.entity.Account;
import com.household.wealth.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    private final RedissonClient redissonClient;

    @Autowired(required = false)
    private AccountCacheService accountCacheService;

    private static final SnowflakeIdGenerator ACCOUNT_ID_GEN = new SnowflakeIdGenerator(2, 1);
    private static final String DEFAULT_CURRENCY = "CNY";

    public List<AccountResponse> getAccounts(Long userId) {
        if (accountCacheService != null) {
            return accountCacheService.getAccountList("accounts:user:" + userId,
                    () -> loadFromDb(userId));
        }
        return loadFromDb(userId);
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
        accountRepository.save(account);

        invalidateCaches(userId, familyId);
        snapshotService.triggerSnapshot(userId, familyId);

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
        accountRepository.save(account);

        invalidateCaches(userId, account.getFamilyId());
        snapshotService.triggerSnapshot(userId, account.getFamilyId());

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
        accountRepository.delete(account);

        invalidateCaches(userId, familyId);
        snapshotService.triggerSnapshot(userId, familyId);
    }

    private List<AccountResponse> loadFromDb(Long userId) {
        return accountRepository.findByUserId(userId).stream()
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
        accountRepository.save(account);
        invalidateCaches(targetUserId, familyId);
        snapshotService.triggerSnapshot(targetUserId, familyId);
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
        accountRepository.save(account);
        invalidateCaches(targetUserId, familyId);
        snapshotService.triggerSnapshot(targetUserId, familyId);
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
        accountRepository.delete(account);
        invalidateCaches(targetUserId, familyId);
        snapshotService.triggerSnapshot(targetUserId, familyId);
    }

    private void requireFamilyAdmin(Long userId, Long familyId) {
        if (!FamilyAdminChecker.isAdmin(redissonClient, familyId, userId)) {
            throw new ForbiddenException("需要家庭管理员权限");
        }
    }

    private AccountResponse toResponse(Account a) {
        return new AccountResponse(
                String.valueOf(a.getId()),
                String.valueOf(a.getUserId()),
                a.getAccountName(),
                a.getAccountType(),
                a.getBalance(),
                a.getCurrency(),
                a.getCreatedAt() != null ? a.getCreatedAt().toString() : null);
    }

}
