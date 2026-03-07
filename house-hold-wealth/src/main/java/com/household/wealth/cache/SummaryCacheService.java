package com.household.wealth.cache;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.household.wealth.config.CacheProperties;
import com.household.wealth.dto.response.WealthSummaryResponse;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;

@Slf4j
@Service
@ConditionalOnProperty(name = "cache.enabled", havingValue = "true")
public class SummaryCacheService {

    private static final String NULL_PLACEHOLDER = "__NULL__";

    private final com.github.benmanes.caffeine.cache.Cache<String, Object> localCache;
    private final CacheProperties props;
    private final ObjectMapper objectMapper;
    private final RedissonClient redissonClient;

    public SummaryCacheService(com.github.benmanes.caffeine.cache.Cache<String, Object> localCache,
                               CacheProperties props,
                               Optional<RedissonClient> redissonClient) {
        this.localCache = localCache;
        this.props = props;
        this.redissonClient = redissonClient.orElse(null);
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

        if (redissonClient != null) {
            String json = (String) redissonClient.getBucket(fullKey).get();
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

        RLock lock = redissonClient != null ? redissonClient.getLock("lock:" + fullKey) : null;
        if (lock != null) lock.lock();
        try {
            cached = localCache.getIfPresent(fullKey);
            if (cached != null) {
                if (NULL_PLACEHOLDER.equals(cached)) return null;
                return (WealthSummaryResponse) cached;
            }

            WealthSummaryResponse res = loader.get();
            if (res == null) {
                if (redissonClient != null) {
                    redissonClient.getBucket(fullKey).set(NULL_PLACEHOLDER,
                            Duration.ofSeconds(props.getTtl().getNullPlaceholderSeconds()));
                }
                localCache.put(fullKey, NULL_PLACEHOLDER);
                return null;
            }

            int ttl = props.getTtl().getRedisSummarySeconds()
                    + ThreadLocalRandom.current().nextInt(Math.max(1, props.getTtl().getRedisRandomMax()));
            if (redissonClient != null) {
                try {
                    redissonClient.getBucket(fullKey).set(objectMapper.writeValueAsString(res),
                            Duration.ofSeconds(ttl));
                } catch (JsonProcessingException e) {
                    log.warn("Failed to serialize summary cache for key {}: {}", fullKey, e.getMessage());
                }
            }
            localCache.put(fullKey, res);
            return res;
        } finally {
            if (lock != null && lock.isHeldByCurrentThread()) lock.unlock();
        }
    }

    public void invalidateUser(Long userId) {
        String fullKey = props.getKeyPrefix() + "summary:user:" + userId;
        localCache.invalidate(fullKey);
        if (redissonClient != null) {
            redissonClient.getBucket(fullKey).delete();
        }
    }

    public void invalidateFamily(Long familyId) {
        if (familyId == null) return;
        String fullKey = props.getKeyPrefix() + "summary:family:" + familyId;
        localCache.invalidate(fullKey);
        if (redissonClient != null) {
            redissonClient.getBucket(fullKey).delete();
        }
    }
}
