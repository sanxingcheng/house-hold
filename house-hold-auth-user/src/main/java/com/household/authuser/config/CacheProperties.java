package com.household.authuser.config;

import com.household.common.config.BaseCacheProperties;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@EqualsAndHashCode(callSuper = true)
@Configuration
@ConfigurationProperties(prefix = "cache")
public class CacheProperties extends BaseCacheProperties {

    private Ttl ttl = new Ttl();

    @Override
    public int getRedisRandomMax() {
        return ttl.getRedisRandomMax();
    }

    @Override
    public int getNullPlaceholderSeconds() {
        return ttl.getNullPlaceholderSeconds();
    }

    @Data
    public static class Ttl {
        private int redisUserSeconds = 1800;
        private int redisFamilySeconds = 1800;
        private int redisRandomMax = 300;
        private int nullPlaceholderSeconds = 120;
    }
}
