package com.household.wealth.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 财富快照实体
 *
 * @author household
 * @date 2025/01/01
 */
@Data
@Entity
@Table(name = "wealth_snapshot")
public class WealthSnapshot {

    @Id
    private Long id;

    @Column(name = "owner_type", nullable = false, length = 16)
    private String ownerType;

    @Column(name = "owner_id", nullable = false)
    private Long ownerId;

    @Column(name = "total_assets", nullable = false)
    private Long totalAssets;

    @Column(name = "total_liabilities", nullable = false)
    private Long totalLiabilities;

    @Column(name = "net_worth", nullable = false)
    private Long netWorth;

    @Column(name = "snapshot_date", nullable = false)
    private LocalDate snapshotDate;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
