package com.household.single.config;

import com.household.authuser.config.CacheProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Creates CacheProperties beans for both auth-user and wealth modules
 * with bean names that do not conflict.
 */
@Configuration
public class CachePropertiesConfig {

    @Primary
    @Bean(name = "authCacheProperties")
    @ConfigurationProperties(prefix = "cache")
    public CacheProperties authCacheProperties() {
        return new CacheProperties();
    }

    @Bean(name = "wealthCacheProperties")
    @ConfigurationProperties(prefix = "cache")
    public com.household.wealth.config.CacheProperties wealthCacheProperties() {
        return new com.household.wealth.config.CacheProperties();
    }
}
