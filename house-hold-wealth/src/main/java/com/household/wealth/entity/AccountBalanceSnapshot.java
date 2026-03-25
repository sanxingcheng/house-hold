package com.household.wealth.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "account_balance_snapshot")
public class AccountBalanceSnapshot {

    @Id
    private Long id;

    @Column(name = "account_id", nullable = false)
    private Long accountId;

    @Column(name = "balance", nullable = false)
    private Long balance;

    @Column(name = "snapshot_date", nullable = false)
    private LocalDate snapshotDate;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
