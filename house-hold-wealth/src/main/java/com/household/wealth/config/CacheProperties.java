package com.household.wealth.config;

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
        private int redisAccountSeconds = 300;
        private int redisSummarySeconds = 60;
        private int redisSnapshotSeconds = 600;
        private int redisRandomMax = 60;
        private int nullPlaceholderSeconds = 120;
    }
}
