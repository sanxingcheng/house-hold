package com.household.wealth.service;

import com.household.wealth.cache.SummaryCacheService;
import com.household.wealth.dto.response.SnapshotPointResponse;
import com.household.wealth.entity.Account;
import com.household.wealth.entity.FamilyAsset;
import com.household.wealth.repository.AccountRepository;
import com.household.wealth.repository.FamilyAssetRepository;
import com.household.wealth.repository.WealthSnapshotRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.household.common.util.SnowflakeIdGenerator;

import java.time.LocalDate;
import java.util.List;

/**
 * 快照服务，管理财富快照的生成与历史查询
 *
 * @author household
 * @date 2025/01/01
 */
@Service
@RequiredArgsConstructor
public class SnapshotService {

    private final AccountRepository accountRepository;
    private final FamilyAssetRepository familyAssetRepository;
    private final WealthSnapshotRepository snapshotRepository;

    @Autowired(required = false)
    private SummaryCacheService summaryCacheService;

    private static final SnowflakeIdGenerator SNAPSHOT_ID_GEN = new SnowflakeIdGenerator(2, 2);
    private static final String CREDIT_CARD = "CREDIT_CARD";
    private static final String OWNER_TYPE_USER = "USER";
    private static final String OWNER_TYPE_FAMILY = "FAMILY";

    @Transactional(rollbackFor = Exception.class)
    public void triggerSnapshot(Long userId, Long familyId) {
        LocalDate today = LocalDate.now();

        List<Account> userAccounts = accountRepository.findByUserId(userId);
        long totalAssets = userAccounts.stream()
                .filter(a -> !CREDIT_CARD.equals(a.getAccountType()))
                .mapToLong(Account::getBalance).sum();
        long totalLiabilities = userAccounts.stream()
                .filter(a -> CREDIT_CARD.equals(a.getAccountType()))
                .mapToLong(Account::getBalance).sum();
        long netWorth = totalAssets - totalLiabilities;

        snapshotRepository.upsertSnapshot(
                SNAPSHOT_ID_GEN.nextId(), OWNER_TYPE_USER, userId,
                totalAssets, totalLiabilities, netWorth, today);

        if (summaryCacheService != null) {
            summaryCacheService.invalidateUser(userId);
        }

        if (familyId != null) {
            List<Account> familyAccounts = accountRepository.findByFamilyId(familyId);
            long famAccountAssets = familyAccounts.stream()
                    .filter(a -> !CREDIT_CARD.equals(a.getAccountType()))
                    .mapToLong(Account::getBalance).sum();
            long famLiabilitiesFromAccounts = familyAccounts.stream()
                    .filter(a -> CREDIT_CARD.equals(a.getAccountType()))
                    .mapToLong(Account::getBalance).sum();

            List<FamilyAsset> assets = familyAssetRepository.findByFamilyIdOrderByCreatedAtDesc(familyId);
            long famSharedAssets = assets.stream()
                    .filter(a -> !Boolean.TRUE.equals(a.getLoanOnly()))
                    .mapToLong(FamilyAsset::getAmount)
                    .sum();
            long famLoanLiabilities = assets.stream()
                    .filter(a -> "REAL_ESTATE".equals(a.getAssetType()) || "VEHICLE".equals(a.getAssetType()))
                    .mapToLong(FamilyAsset::getLoanRemaining)
                    .sum();

            long famAssets = famAccountAssets + famSharedAssets;
            long famLiabilities = famLiabilitiesFromAccounts + famLoanLiabilities;
            long famNetWorth = famAssets - famLiabilities;

            snapshotRepository.upsertSnapshot(
                    SNAPSHOT_ID_GEN.nextId(), OWNER_TYPE_FAMILY, familyId,
                    famAssets, famLiabilities, famNetWorth, today);

            if (summaryCacheService != null) {
                summaryCacheService.invalidateFamily(familyId);
            }
        }
    }

    public List<SnapshotPointResponse> getUserHistory(Long userId, LocalDate from, LocalDate to) {
        return snapshotRepository
                .findByOwnerTypeAndOwnerIdAndSnapshotDateBetweenOrderBySnapshotDateAsc(OWNER_TYPE_USER, userId, from, to)
                .stream()
                .map(s -> new SnapshotPointResponse(s.getSnapshotDate().toString(), s.getNetWorth()))
                .toList();
    }

    public List<SnapshotPointResponse> getFamilyHistory(Long familyId, LocalDate from, LocalDate to) {
        return snapshotRepository
                .findByOwnerTypeAndOwnerIdAndSnapshotDateBetweenOrderBySnapshotDateAsc(OWNER_TYPE_FAMILY, familyId, from, to)
                .stream()
                .map(s -> new SnapshotPointResponse(s.getSnapshotDate().toString(), s.getNetWorth()))
                .toList();
    }
}
