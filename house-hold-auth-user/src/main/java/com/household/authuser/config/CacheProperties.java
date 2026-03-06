package com.household.authuser.config;

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
    private String keyPrefix = "household:auth:";

    @Data
    public static class Local {
        private int maxSize = 1000;
        private int ttlSeconds = 300;
    }

    @Data
    public static class Ttl {
        private int redisUserSeconds = 1800;
        private int redisFamilySeconds = 1800;
        private int redisRandomMax = 300;
        private int nullPlaceholderSeconds = 120;
    }
}
