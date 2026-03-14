package com.household.wealth.service;

import com.household.wealth.cache.SummaryCacheService;
import com.household.wealth.dto.response.WealthSummaryResponse;
import com.household.wealth.entity.Account;
import com.household.wealth.entity.FamilyAsset;
import com.household.wealth.repository.AccountRepository;
import com.household.wealth.repository.FamilyAssetRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 财富汇总服务，计算用户和家庭的资产概览
 *
 * @author household
 * @date 2025/01/01
 */
@Service
@RequiredArgsConstructor
public class SummaryService {

    private final AccountRepository accountRepository;
    private final FamilyAssetRepository familyAssetRepository;

    @Autowired(required = false)
    private SummaryCacheService summaryCacheService;

    private static final String CREDIT_CARD = "CREDIT_CARD";
    private static final String OWNER_TYPE_USER = "USER";
    private static final String OWNER_TYPE_FAMILY = "FAMILY";

    public WealthSummaryResponse getUserSummary(Long userId) {
        if (summaryCacheService != null) {
            return summaryCacheService.get("summary:user:" + userId,
                    () -> computeUserSummary(userId));
        }
        return computeUserSummary(userId);
    }

    public WealthSummaryResponse getFamilySummary(Long familyId) {
        if (summaryCacheService != null) {
            return summaryCacheService.get("summary:family:" + familyId,
                    () -> computeFamilySummary(familyId));
        }
        return computeFamilySummary(familyId);
    }

    private WealthSummaryResponse computeUserSummary(Long userId) {
        List<Account> accounts = accountRepository.findByUserId(userId);
        return buildSummary(accounts, String.valueOf(userId), OWNER_TYPE_USER, 0L, 0L);
    }

    private WealthSummaryResponse computeFamilySummary(Long familyId) {
        List<Account> accounts = accountRepository.findByFamilyId(familyId);

        List<FamilyAsset> assets = familyAssetRepository.findByFamilyIdOrderByCreatedAtDesc(familyId);
        long familyAssetTotal = assets.stream()
                .filter(a -> !Boolean.TRUE.equals(a.getLoanOnly()))
                .mapToLong(FamilyAsset::getAmount)
                .sum();
        long loanLiabilities = assets.stream()
                .filter(a -> "REAL_ESTATE".equals(a.getAssetType()) || "VEHICLE".equals(a.getAssetType()))
                .mapToLong(FamilyAsset::getLoanRemaining)
                .sum();

        return buildSummary(accounts, String.valueOf(familyId), OWNER_TYPE_FAMILY, familyAssetTotal, loanLiabilities);
    }

    private WealthSummaryResponse buildSummary(List<Account> accounts, String ownerId,
                                               String ownerType, long familyAssetTotal,
                                               long extraLiabilities) {
        long accountAssets = accounts.stream()
                .filter(a -> !CREDIT_CARD.equals(a.getAccountType()))
                .mapToLong(Account::getBalance).sum();
        long totalLiabilitiesFromAccounts = accounts.stream()
                .filter(a -> CREDIT_CARD.equals(a.getAccountType()))
                .mapToLong(Account::getBalance).sum();
        long totalLiabilities = totalLiabilitiesFromAccounts + extraLiabilities;
        long totalAssets = accountAssets + familyAssetTotal;
        long netWorth = totalAssets - totalLiabilities;

        WealthSummaryResponse resp = new WealthSummaryResponse(
                ownerId, ownerType,
                totalAssets, totalLiabilities, netWorth,
                LocalDateTime.now().toString());
        if (OWNER_TYPE_FAMILY.equals(ownerType)) {
            resp.setFamilyAssetTotal(familyAssetTotal);
        }
        return resp;
    }
}
