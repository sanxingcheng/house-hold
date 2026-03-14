package com.household.wealth.service;

import com.household.wealth.cache.SummaryCacheService;
import com.household.wealth.dto.response.WealthSummaryResponse;
import com.household.wealth.entity.Account;
import com.household.wealth.entity.FamilyAsset;
import com.household.wealth.repository.AccountRepository;
import com.household.wealth.repository.FamilyAssetRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("SummaryService 单元测试")
class SummaryServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private FamilyAssetRepository familyAssetRepository;

    @InjectMocks
    private SummaryService summaryService;

    private Account makeAccount(Long userId, Long familyId, String type, Long balance) {
        Account a = new Account();
        a.setId(System.nanoTime());
        a.setUserId(userId);
        a.setFamilyId(familyId);
        a.setAccountName("acc");
        a.setAccountType(type);
        a.setBalance(balance);
        a.setCurrency("CNY");
        return a;
    }

    private FamilyAsset makeFamilyAsset(Long familyId, Long amount, Long loanRemaining) {
        FamilyAsset fa = new FamilyAsset();
        fa.setId(System.nanoTime());
        fa.setFamilyId(familyId);
        fa.setAssetName("房产");
        fa.setAssetType("REAL_ESTATE");
        fa.setAmount(amount);
        fa.setCurrency("CNY");
        fa.setCreatedBy(1L);
        fa.setLoanTotal(loanRemaining);
        fa.setLoanRemaining(loanRemaining);
        return fa;
    }

    @Nested
    @DisplayName("getUserSummary - 用户财富汇总")
    class UserSummary {

        @Test
        @DisplayName("储蓄+信用卡，正确计算 totalAssets/totalLiabilities/netWorth")
        void computesCorrectly() {
            Account savings = makeAccount(10L, 100L, "SAVINGS", 10000L);
            Account checking = makeAccount(10L, 100L, "CHECKING", 5000L);
            Account credit = makeAccount(10L, 100L, "CREDIT_CARD", 3000L);
            when(accountRepository.findByUserId(10L)).thenReturn(List.of(savings, checking, credit));

            WealthSummaryResponse resp = summaryService.getUserSummary(10L);

            assertThat(resp.getTotalAssets()).isEqualTo(15000L);
            assertThat(resp.getTotalLiabilities()).isEqualTo(3000L);
            assertThat(resp.getNetWorth()).isEqualTo(12000L);
            assertThat(resp.getOwnerType()).isEqualTo("USER");
            assertThat(resp.getOwnerId()).isEqualTo("10");
        }

        @Test
        @DisplayName("只有信用卡负债时 totalAssets=0，netWorth 为负")
        void onlyCreditCard_negativeNetWorth() {
            Account credit = makeAccount(10L, 100L, "CREDIT_CARD", 5000L);
            when(accountRepository.findByUserId(10L)).thenReturn(List.of(credit));

            WealthSummaryResponse resp = summaryService.getUserSummary(10L);

            assertThat(resp.getTotalAssets()).isZero();
            assertThat(resp.getTotalLiabilities()).isEqualTo(5000L);
            assertThat(resp.getNetWorth()).isEqualTo(-5000L);
        }

        @Test
        @DisplayName("无账户时全部为 0")
        void noAccounts_allZero() {
            when(accountRepository.findByUserId(10L)).thenReturn(List.of());

            WealthSummaryResponse resp = summaryService.getUserSummary(10L);

            assertThat(resp.getTotalAssets()).isZero();
            assertThat(resp.getTotalLiabilities()).isZero();
            assertThat(resp.getNetWorth()).isZero();
        }
    }

    @Nested
    @DisplayName("getFamilySummary - 家庭财富汇总")
    class FamilySummary {

        @Test
        @DisplayName("包含家庭共有资产与信用卡负债的汇总计算")
        void computesWithFamilyAssets() {
            Account savings = makeAccount(10L, 100L, "SAVINGS", 20000L);
            Account credit = makeAccount(10L, 100L, "CREDIT_CARD", 4000L);
            when(accountRepository.findByFamilyId(100L)).thenReturn(List.of(savings, credit));

            FamilyAsset house = makeFamilyAsset(100L, 500000L, 300000L);
            FamilyAsset car = makeFamilyAsset(100L, 150000L, 50000L);
            when(familyAssetRepository.findByFamilyIdOrderByCreatedAtDesc(100L))
                    .thenReturn(List.of(house, car));

            WealthSummaryResponse resp = summaryService.getFamilySummary(100L);

            long totalAssets = 20000L + 500000L + 150000L;
            long loanLiabilities = 300000L + 50000L;
            assertThat(resp.getTotalAssets()).isEqualTo(totalAssets);
            assertThat(resp.getTotalLiabilities()).isEqualTo(4000L + loanLiabilities);
            assertThat(resp.getNetWorth()).isEqualTo(totalAssets - 4000L - loanLiabilities);
            assertThat(resp.getOwnerType()).isEqualTo("FAMILY");
            assertThat(resp.getFamilyAssetTotal()).isEqualTo(650000L);
        }

        @Test
        @DisplayName("无家庭共有资产时只计算个人账户")
        void noFamilyAssets_onlyAccounts() {
            Account savings = makeAccount(10L, 100L, "SAVINGS", 8000L);
            when(accountRepository.findByFamilyId(100L)).thenReturn(List.of(savings));
            when(familyAssetRepository.findByFamilyIdOrderByCreatedAtDesc(100L))
                    .thenReturn(List.of());

            WealthSummaryResponse resp = summaryService.getFamilySummary(100L);

            assertThat(resp.getTotalAssets()).isEqualTo(8000L);
            assertThat(resp.getFamilyAssetTotal()).isEqualTo(0L);
        }
    }
}
