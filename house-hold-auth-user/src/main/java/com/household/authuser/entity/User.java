package com.household.authuser.entity;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class User {
    private Long id;
    private String username;
    private String passwordHash;
    private LocalDate birthday;
    private String email;
    private String phone;
    private Long familyId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
