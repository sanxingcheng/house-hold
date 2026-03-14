package com.household.wealth.service;

import com.household.wealth.entity.Account;
import com.household.wealth.entity.FamilyAsset;
import com.household.wealth.repository.AccountRepository;
import com.household.wealth.repository.FamilyAssetRepository;
import com.household.wealth.repository.WealthSnapshotRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SnapshotService 单元测试")
class SnapshotServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private FamilyAssetRepository familyAssetRepository;

    @Mock
    private WealthSnapshotRepository snapshotRepository;

    @InjectMocks
    private SnapshotService snapshotService;

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
    @DisplayName("triggerSnapshot - 快照触发")
    class TriggerSnapshot {

        @Test
        @DisplayName("同时生成 USER 和 FAMILY 两维度快照")
        void generatesUserAndFamilySnapshots() {
            Account savings = makeAccount(10L, 100L, "SAVINGS", 10000L);
            Account credit = makeAccount(10L, 100L, "CREDIT_CARD", 2000L);

            when(accountRepository.findByUserId(10L)).thenReturn(List.of(savings, credit));
            when(accountRepository.findByFamilyId(100L)).thenReturn(List.of(savings, credit));
            when(familyAssetRepository.findByFamilyIdOrderByCreatedAtDesc(100L))
                    .thenReturn(List.of(makeFamilyAsset(100L, 300000L, 200000L)));
            when(snapshotRepository.upsertSnapshot(anyLong(), anyString(), anyLong(),
                    anyLong(), anyLong(), anyLong(), any(LocalDate.class))).thenReturn(1);

            snapshotService.triggerSnapshot(10L, 100L);

            ArgumentCaptor<String> ownerTypeCaptor = ArgumentCaptor.forClass(String.class);
            ArgumentCaptor<Long> ownerIdCaptor = ArgumentCaptor.forClass(Long.class);
            ArgumentCaptor<Long> assetsCaptor = ArgumentCaptor.forClass(Long.class);
            ArgumentCaptor<Long> liabilitiesCaptor = ArgumentCaptor.forClass(Long.class);
            ArgumentCaptor<Long> netWorthCaptor = ArgumentCaptor.forClass(Long.class);

            verify(snapshotRepository, times(2)).upsertSnapshot(
                    anyLong(), ownerTypeCaptor.capture(), ownerIdCaptor.capture(),
                    assetsCaptor.capture(), liabilitiesCaptor.capture(), netWorthCaptor.capture(),
                    any(LocalDate.class));

            List<String> ownerTypes = ownerTypeCaptor.getAllValues();
            assertThat(ownerTypes).containsExactly("USER", "FAMILY");

            List<Long> ownerIds = ownerIdCaptor.getAllValues();
            assertThat(ownerIds).containsExactly(10L, 100L);

            assertThat(assetsCaptor.getAllValues().get(0)).isEqualTo(10000L);
            assertThat(liabilitiesCaptor.getAllValues().get(0)).isEqualTo(2000L);
            assertThat(netWorthCaptor.getAllValues().get(0)).isEqualTo(8000L);

            long expectedFamilyAssets = 10000L + 300000L;
            long expectedLiabilities = 2000L + 200000L;
            assertThat(assetsCaptor.getAllValues().get(1)).isEqualTo(expectedFamilyAssets);
            assertThat(liabilitiesCaptor.getAllValues().get(1)).isEqualTo(expectedLiabilities);
            assertThat(netWorthCaptor.getAllValues().get(1)).isEqualTo(expectedFamilyAssets - expectedLiabilities);
        }

        @Test
        @DisplayName("familyId 为 null 时只生成 USER 快照")
        void whenNoFamily_onlyUserSnapshot() {
            Account savings = makeAccount(10L, null, "SAVINGS", 5000L);
            when(accountRepository.findByUserId(10L)).thenReturn(List.of(savings));
            when(snapshotRepository.upsertSnapshot(anyLong(), anyString(), anyLong(),
                    anyLong(), anyLong(), anyLong(), any(LocalDate.class))).thenReturn(1);

            snapshotService.triggerSnapshot(10L, null);

            verify(snapshotRepository, times(1)).upsertSnapshot(
                    anyLong(), eq("USER"), eq(10L),
                    eq(5000L), eq(0L), eq(5000L), any(LocalDate.class));
            verify(accountRepository, never()).findByFamilyId(anyLong());
        }

        @Test
        @DisplayName("upsert 幂等 - 相同日期调用多次 upsertSnapshot")
        void upsertIsIdempotent() {
            Account savings = makeAccount(10L, null, "SAVINGS", 5000L);
            when(accountRepository.findByUserId(10L)).thenReturn(List.of(savings));
            when(snapshotRepository.upsertSnapshot(anyLong(), anyString(), anyLong(),
                    anyLong(), anyLong(), anyLong(), any(LocalDate.class))).thenReturn(1);

            snapshotService.triggerSnapshot(10L, null);
            snapshotService.triggerSnapshot(10L, null);

            verify(snapshotRepository, times(2)).upsertSnapshot(
                    anyLong(), eq("USER"), eq(10L),
                    eq(5000L), eq(0L), eq(5000L), any(LocalDate.class));
        }

        @Test
        @DisplayName("信用卡正确归入负债")
        void creditCardCountsAsLiability() {
            Account credit = makeAccount(10L, null, "CREDIT_CARD", 8000L);
            when(accountRepository.findByUserId(10L)).thenReturn(List.of(credit));
            when(snapshotRepository.upsertSnapshot(anyLong(), anyString(), anyLong(),
                    anyLong(), anyLong(), anyLong(), any(LocalDate.class))).thenReturn(1);

            snapshotService.triggerSnapshot(10L, null);

            verify(snapshotRepository).upsertSnapshot(
                    anyLong(), eq("USER"), eq(10L),
                    eq(0L), eq(8000L), eq(-8000L), any(LocalDate.class));
        }
    }
}
