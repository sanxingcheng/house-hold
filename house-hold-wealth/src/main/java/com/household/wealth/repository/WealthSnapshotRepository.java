package com.household.wealth.repository;

import com.household.wealth.entity.WealthSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface WealthSnapshotRepository extends JpaRepository<WealthSnapshot, Long> {

    List<WealthSnapshot> findByOwnerTypeAndOwnerIdAndSnapshotDateBetweenOrderBySnapshotDateAsc(
            String ownerType, Long ownerId, LocalDate from, LocalDate to);

    @Modifying
    @Query(value = """
            INSERT INTO wealth_snapshot (id, owner_type, owner_id, total_assets, total_liabilities, net_worth, snapshot_date)
            VALUES (:id, :ownerType, :ownerId, :totalAssets, :totalLiabilities, :netWorth, :snapshotDate)
            ON DUPLICATE KEY UPDATE
              total_assets = :totalAssets,
              total_liabilities = :totalLiabilities,
              net_worth = :netWorth
            """, nativeQuery = true)
    int upsertSnapshot(@Param("id") Long id,
                       @Param("ownerType") String ownerType,
                       @Param("ownerId") Long ownerId,
                       @Param("totalAssets") Long totalAssets,
                       @Param("totalLiabilities") Long totalLiabilities,
                       @Param("netWorth") Long netWorth,
                       @Param("snapshotDate") LocalDate snapshotDate);
}
