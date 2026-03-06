package com.household.authuser.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 家庭信息实体
 *
 * @author household
 * @date 2025/01/01
 */
@Data
@Entity
@Table(name = "family")
public class Family {

    @Id
    private Long id;

    @Column(name = "name_alias", nullable = false, length = 64)
    private String nameAlias;

    @Column(length = 64)
    private String country;

    @Column(length = 64)
    private String province;

    @Column(length = 64)
    private String city;

    @Column(length = 256)
    private String street;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private LocalDateTime updatedAt;
}
