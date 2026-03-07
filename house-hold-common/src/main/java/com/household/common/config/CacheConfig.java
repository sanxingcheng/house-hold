package com.household.common.config;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * 公共缓存配置：Caffeine 本地缓存（Redis 由 Redisson Starter 自动配置）
 *
 * @author household
 */
@Configuration
public class CacheConfig {

    @Bean
    @ConditionalOnProperty(name = "cache.enabled", havingValue = "true")
    public Cache<String, Object> localCache(BaseCacheProperties props) {
        return Caffeine.newBuilder()
                .maximumSize(props.getLocal().getMaxSize())
                .expireAfterWrite(props.getLocal().getTtlSeconds(), TimeUnit.SECONDS)
                .build();
    }
}
