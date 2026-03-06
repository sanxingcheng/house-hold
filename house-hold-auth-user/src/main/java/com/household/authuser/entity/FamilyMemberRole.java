package com.household.authuser.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 家庭成员角色关联实体
 *
 * @author household
 * @date 2025/01/01
 */
@Data
@Entity
@Table(name = "family_member_role")
public class FamilyMemberRole {

    @Id
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "family_id", nullable = false)
    private Long familyId;

    /** HUSBAND / WIFE / CHILD / OTHER */
    @Column(nullable = false, length = 32)
    private String role;

    @Column(name = "joined_at", nullable = false)
    private LocalDateTime joinedAt;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;
}
