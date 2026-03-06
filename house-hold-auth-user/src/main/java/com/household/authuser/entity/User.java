package com.household.authuser.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 用户基础信息实体
 *
 * @author household
 * @date 2025/01/01
 */
@Data
@Entity
@Table(name = "user_base")
public class User {

    @Id
    private Long id;

    @Column(nullable = false, unique = true, length = 64)
    private String username;

    @Column(name = "password_hash", nullable = false, length = 128)
    private String passwordHash;

    @Column(length = 64)
    private String name;

    @Column(nullable = false)
    private LocalDate birthday;

    @Column(length = 128)
    private String email;

    @Column(length = 32)
    private String phone;

    @Column(name = "family_id")
    private Long familyId;

    /** Managed by DB DEFAULT CURRENT_TIMESTAMP — JPA reads only. */
    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    /** Managed by DB ON UPDATE CURRENT_TIMESTAMP — JPA reads only. */
    @Column(name = "updated_at", insertable = false, updatable = false)
    private LocalDateTime updatedAt;
}
