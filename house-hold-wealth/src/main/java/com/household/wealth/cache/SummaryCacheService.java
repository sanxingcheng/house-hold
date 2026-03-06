package com.household.wealth.cache;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.household.wealth.config.CacheProperties;
import com.household.wealth.dto.response.WealthSummaryResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

@Slf4j
@Service
@ConditionalOnProperty(name = "cache.enabled", havingValue = "true")
public class SummaryCacheService {

    private static final String NULL_PLACEHOLDER = "__NULL__";

    private final com.github.benmanes.caffeine.cache.Cache<String, Object> localCache;
    private final CacheProperties props;
    private final ObjectMapper objectMapper;
    private final ConcurrentHashMap<String, Lock> loadLocks = new ConcurrentHashMap<>();
    private final StringRedisTemplate redisTemplate;

    public SummaryCacheService(com.github.benmanes.caffeine.cache.Cache<String, Object> localCache,
                               CacheProperties props,
                               Optional<StringRedisTemplate> redisTemplate) {
        this.localCache = localCache;
        this.props = props;
        this.redisTemplate = redisTemplate.orElse(null);
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    public WealthSummaryResponse get(String dimensionKey, Supplier<WealthSummaryResponse> loader) {
        String fullKey = props.getKeyPrefix() + dimensionKey;

        Object cached = localCache.getIfPresent(fullKey);
        if (cached != null) {
            if (NULL_PLACEHOLDER.equals(cached)) return null;
            return (WealthSummaryResponse) cached;
        }

        if (redisTemplate != null) {
            String json = redisTemplate.opsForValue().get(fullKey);
            if (json != null) {
                if (NULL_PLACEHOLDER.equals(json)) {
                    localCache.put(fullKey, NULL_PLACEHOLDER);
                    return null;
                }
                try {
                    WealthSummaryResponse res = objectMapper.readValue(json, WealthSummaryResponse.class);
                    localCache.put(fullKey, res);
                    return res;
                } catch (JsonProcessingException e) {
                    log.warn("Failed to deserialize summary cache for key {}: {}", fullKey, e.getMessage());
                }
            }
        }

        Lock lock = loadLocks.computeIfAbsent(fullKey, k -> new ReentrantLock());
        lock.lock();
        try {
            cached = localCache.getIfPresent(fullKey);
            if (cached != null) {
                if (NULL_PLACEHOLDER.equals(cached)) return null;
                return (WealthSummaryResponse) cached;
            }

            WealthSummaryResponse res = loader.get();
            if (res == null) {
                if (redisTemplate != null) {
                    redisTemplate.opsForValue().set(fullKey, NULL_PLACEHOLDER,
                            Duration.ofSeconds(props.getTtl().getNullPlaceholderSeconds()));
                }
                localCache.put(fullKey, NULL_PLACEHOLDER);
                return null;
            }

            int ttl = props.getTtl().getRedisSummarySeconds()
                    + ThreadLocalRandom.current().nextInt(Math.max(1, props.getTtl().getRedisRandomMax()));
            if (redisTemplate != null) {
                try {
                    redisTemplate.opsForValue().set(fullKey, objectMapper.writeValueAsString(res),
                            Duration.ofSeconds(ttl));
                } catch (JsonProcessingException e) {
                    log.warn("Failed to serialize summary cache for key {}: {}", fullKey, e.getMessage());
                }
            }
            localCache.put(fullKey, res);
            return res;
        } finally {
            lock.unlock();
            loadLocks.remove(fullKey, lock);
        }
    }

    public void invalidateUser(Long userId) {
        String fullKey = props.getKeyPrefix() + "summary:user:" + userId;
        localCache.invalidate(fullKey);
        if (redisTemplate != null) {
            redisTemplate.delete(fullKey);
        }
    }

    public void invalidateFamily(Long familyId) {
        if (familyId == null) return;
        String fullKey = props.getKeyPrefix() + "summary:family:" + familyId;
        localCache.invalidate(fullKey);
        if (redisTemplate != null) {
            redisTemplate.delete(fullKey);
        }
    }
}
