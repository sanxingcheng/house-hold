package com.household.wealth.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "operation_log")
public class OperationLog {

    @Id
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "family_id")
    private Long familyId;

    @Column(name = "action", nullable = false, length = 32)
    private String action;

    @Column(name = "resource_type", nullable = false, length = 32)
    private String resourceType;

    @Column(name = "resource_id", length = 64)
    private String resourceId;

    @Column(name = "detail", length = 512)
    private String detail;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
