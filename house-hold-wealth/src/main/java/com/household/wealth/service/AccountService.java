package com.household.wealth.service;

import com.household.wealth.cache.AccountCacheService;
import com.household.wealth.dto.request.AccountCreateRequest;
import com.household.wealth.dto.request.AccountUpdateRequest;
import com.household.wealth.dto.response.AccountResponse;
import com.household.wealth.entity.Account;
import com.household.wealth.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

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

    @Autowired(required = false)
    private AccountCacheService accountCacheService;

    private static final AtomicLong ACCOUNT_ID_GEN = new AtomicLong(3_000_000_000_000L);
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
        account.setId(ACCOUNT_ID_GEN.incrementAndGet());
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
                .orElseThrow(() -> new AccountNotFoundException("账户不存在"));
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
                .orElseThrow(() -> new AccountNotFoundException("账户不存在"));
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

    public static class AccountNotFoundException extends RuntimeException {
        public AccountNotFoundException(String message) { super(message); }
    }

    public static class ForbiddenException extends RuntimeException {
        public ForbiddenException(String message) { super(message); }
    }

    public static class NoFamilyException extends RuntimeException {
        public NoFamilyException(String message) { super(message); }
    }
}
