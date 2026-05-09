package com.household.wealth.repository;

import com.household.wealth.entity.AccountBalanceSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

public interface AccountBalanceSnapshotRepository extends JpaRepository<AccountBalanceSnapshot, Long> {

    List<AccountBalanceSnapshot> findByAccountIdAndSnapshotDateBetweenOrderBySnapshotDateAsc(
            Long accountId, LocalDate from, LocalDate to);

    @Modifying
    @Transactional
    @Query(value = """
            INSERT INTO account_balance_snapshot (id, account_id, balance, snapshot_date)
            VALUES (:id, :accountId, :balance, :snapshotDate)
            ON DUPLICATE KEY UPDATE balance = :balance
            """, nativeQuery = true)
    int upsertSnapshot(@Param("id") Long id,
                       @Param("accountId") Long accountId,
                       @Param("balance") Long balance,
                       @Param("snapshotDate") LocalDate snapshotDate);
}
