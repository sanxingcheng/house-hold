package com.household.wealth.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "family_asset")
public class FamilyAsset {

    @Id
    private Long id;

    @Column(name = "family_id", nullable = false)
    private Long familyId;

    @Column(name = "asset_name", nullable = false, length = 64)
    private String assetName;

    @Column(name = "asset_type", nullable = false, length = 32)
    private String assetType;

    @Column(nullable = false)
    private Long amount;

    @Column(nullable = false, length = 8)
    private String currency;

    @Column(length = 256)
    private String remark;

    /**
     * 贷款总额（单位：分），主要用于房贷/车贷等场景，可选。
     */
    @Column(name = "loan_total", nullable = false)
    private Long loanTotal = 0L;

    /**
     * 当前贷款余额（单位：分），计入负债。
     */
    @Column(name = "loan_remaining", nullable = false)
    private Long loanRemaining = 0L;

    /**
     * 是否只统计负债且不把 amount 计入资产总额（主要用于房产/车辆只想看贷款）。
     */
    @Column(name = "loan_only", nullable = false)
    private Boolean loanOnly = false;

    @Column(name = "created_by", nullable = false)
    private Long createdBy;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
