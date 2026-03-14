package com.household.wealth.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * 账户信息实体
 *
 * @author household
 * @date 2025/01/01
 */
@Data
@Entity
@Table(name = "account")
public class Account {

    @Id
    private Long id;

    @Column(name = "user_id", nullable = false, updatable = false)
    private Long userId;

    @Column(name = "family_id")
    private Long familyId;

    @Column(name = "account_name", nullable = false, length = 64)
    private String accountName;

    @Column(name = "account_type", nullable = false, length = 32)
    private String accountType;

    @Column(nullable = false)
    private Long balance;

    @Column(nullable = false, length = 8)
    private String currency;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
