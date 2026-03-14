package com.household.authuser.entity;

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
@Table(name = "family_join_request")
public class FamilyJoinRequest {

    @Id
    private Long id;

    @Column(name = "family_id", nullable = false)
    private Long familyId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    /** APPLY / INVITE */
    @Column(name = "request_type", nullable = false, length = 16)
    private String requestType;

    /** PENDING / APPROVED / REJECTED */
    @Column(nullable = false, length = 16)
    private String status = "PENDING";

    @Column(name = "initiated_by", nullable = false)
    private Long initiatedBy;

    @Column(name = "handled_by")
    private Long handledBy;

    @UpdateTimestamp
    @Column(name = "handled_at")
    private LocalDateTime handledAt;

    @Column(nullable = false, length = 32)
    private String role = "OTHER";
    @CreationTimestamp
    @Column(name = "created_at",  updatable = false)
    private LocalDateTime createdAt;
}
