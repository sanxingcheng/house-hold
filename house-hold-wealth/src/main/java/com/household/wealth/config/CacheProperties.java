package com.household.wealth.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "cache")
public class CacheProperties {
    private boolean enabled = true;
    private Local local = new Local();
    private Ttl ttl = new Ttl();
    private String keyPrefix = "household:wealth:";

    @Data
    public static class Local {
        private int maxSize = 1000;
        private int ttlSeconds = 60;
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
