package com.household.wealth.config;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.concurrent.TimeUnit;

@Configuration
public class CacheConfig {

    @Bean
    @ConditionalOnProperty(name = "cache.enabled", havingValue = "true")
    public Cache<String, Object> localCache(CacheProperties props) {
        return Caffeine.newBuilder()
                .maximumSize(props.getLocal().getMaxSize())
                .expireAfterWrite(props.getLocal().getTtlSeconds(), TimeUnit.SECONDS)
                .build();
    }

    @Bean
    @ConditionalOnProperty(name = "cache.enabled", havingValue = "true")
    @ConditionalOnBean(RedisConnectionFactory.class)
    public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory factory) {
        return new StringRedisTemplate(factory);
    }
}
