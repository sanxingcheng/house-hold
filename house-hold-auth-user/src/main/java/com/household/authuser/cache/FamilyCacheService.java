package com.household.authuser.cache;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.household.authuser.config.CacheProperties;
import com.household.authuser.dto.response.FamilyResponse;
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
public class FamilyCacheService {

    private static final String NULL_PLACEHOLDER = "__NULL__";
    private final com.github.benmanes.caffeine.cache.Cache<String, Object> localCache;
    private final CacheProperties props;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final RedissonClient redissonClient;

    public FamilyCacheService(com.github.benmanes.caffeine.cache.Cache<String, Object> localCache,
                              CacheProperties props,
                              Optional<RedissonClient> redissonClient) {
        this.localCache = localCache;
        this.props = props;
        this.redissonClient = redissonClient.orElse(null);
    }

    public FamilyResponse getFamily(Long familyId, Supplier<FamilyResponse> loader) {
        String key = "family:info:" + familyId;
        String fullKey = props.getKeyPrefix() + key;
        Object cached = localCache.getIfPresent(fullKey);
        if (cached != null) {
            if (NULL_PLACEHOLDER.equals(cached)) return null;
            return (FamilyResponse) cached;
        }
        if (redissonClient != null) {
            String json = (String) redissonClient.getBucket(fullKey).get();
            if (json != null) {
                if (NULL_PLACEHOLDER.equals(json)) {
                    localCache.put(fullKey, NULL_PLACEHOLDER);
                    return null;
                }
                try {
                    FamilyResponse res = objectMapper.readValue(json, FamilyResponse.class);
                    localCache.put(fullKey, res);
                    return res;
                } catch (JsonProcessingException e) {
                    log.warn("Failed to deserialize family cache for key {}: {}", fullKey, e.getMessage());
                }
            }
        }
        RLock lock = redissonClient != null ? redissonClient.getLock("lock:" + fullKey) : null;
        if (lock != null) lock.lock();
        try {
            cached = localCache.getIfPresent(fullKey);
            if (cached != null) {
                if (NULL_PLACEHOLDER.equals(cached)) return null;
                return (FamilyResponse) cached;
            }
            if (redissonClient != null) {
                String json = (String) redissonClient.getBucket(fullKey).get();
                if (json != null) {
                    if (NULL_PLACEHOLDER.equals(json)) return null;
                    try {
                        FamilyResponse res = objectMapper.readValue(json, FamilyResponse.class);
                        localCache.put(fullKey, res);
                        return res;
                    } catch (JsonProcessingException e) {
                        log.warn("Failed to deserialize family cache for key {}: {}", fullKey, e.getMessage());
                    }
                }
            }
            FamilyResponse res = loader.get();
            if (res == null) {
                if (redissonClient != null) {
                    redissonClient.getBucket(fullKey).set(NULL_PLACEHOLDER,
                            Duration.ofSeconds(props.getTtl().getNullPlaceholderSeconds()));
                }
                localCache.put(fullKey, NULL_PLACEHOLDER);
                return null;
            }
            int ttl = props.getTtl().getRedisFamilySeconds()
                    + ThreadLocalRandom.current().nextInt(Math.max(1, props.getTtl().getRedisRandomMax()));
            if (redissonClient != null) {
                try {
                    redissonClient.getBucket(fullKey).set(objectMapper.writeValueAsString(res),
                            Duration.ofSeconds(ttl));
                } catch (JsonProcessingException e) {
                    log.warn("Failed to serialize family cache for key {}: {}", fullKey, e.getMessage());
                }
            }
            localCache.put(fullKey, res);
            return res;
        } finally {
            if (lock != null && lock.isHeldByCurrentThread()) lock.unlock();
        }
    }

    public void invalidateFamily(Long familyId) {
        String fullKey = props.getKeyPrefix() + "family:info:" + familyId;
        localCache.invalidate(fullKey);
        if (redissonClient != null) {
            redissonClient.getBucket(fullKey).delete();
        }
    }
}
