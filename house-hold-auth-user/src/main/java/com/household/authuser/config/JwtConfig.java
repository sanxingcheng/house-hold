package com.household.authuser.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "app.jwt")
public class JwtConfig {
    private String secret = "house-hold-default-secret-change-in-production-at-least-256-bits";
    private long expirationMs = 86400000; // 24 hours
}
