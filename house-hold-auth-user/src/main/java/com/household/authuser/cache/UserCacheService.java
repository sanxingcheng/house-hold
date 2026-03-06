package com.household.authuser.cache;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.household.authuser.config.CacheProperties;
import com.household.authuser.dto.response.UserProfileResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

@Slf4j
@Service
@ConditionalOnProperty(name = "cache.enabled", havingValue = "true")
public class UserCacheService {

    private static final String NULL_PLACEHOLDER = "__NULL__";
    private final com.github.benmanes.caffeine.cache.Cache<String, Object> localCache;
    private final CacheProperties props;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ConcurrentHashMap<String, Lock> loadLocks = new ConcurrentHashMap<>();
    private final StringRedisTemplate redisTemplate; // null when Redis not configured

    public UserCacheService(com.github.benmanes.caffeine.cache.Cache<String, Object> localCache,
                            CacheProperties props,
                            java.util.Optional<StringRedisTemplate> redisTemplate) {
        this.localCache = localCache;
        this.props = props;
        this.redisTemplate = redisTemplate.orElse(null);
    }

    public UserProfileResponse get(Long userId, Supplier<UserProfileResponse> loader) {
        String key = "user:info:" + userId;
        String fullKey = props.getKeyPrefix() + key;
        // L1
        Object cached = localCache.getIfPresent(fullKey);
        if (cached != null) {
            if (NULL_PLACEHOLDER.equals(cached)) return null;
            return (UserProfileResponse) cached;
        }
        // L2
        if (redisTemplate != null) {
            String json = redisTemplate.opsForValue().get(fullKey);
            if (json != null) {
                if (NULL_PLACEHOLDER.equals(json)) {
                    localCache.put(fullKey, NULL_PLACEHOLDER);
                    return null;
                }
                try {
                    UserProfileResponse res = objectMapper.readValue(json, UserProfileResponse.class);
                    localCache.put(fullKey, res);
                    return res;
                } catch (JsonProcessingException e) {
                    log.warn("Failed to deserialize user cache for key {}: {}", fullKey, e.getMessage());
                }
            }
        }
        Lock lock = loadLocks.computeIfAbsent(fullKey, k -> new ReentrantLock());
        lock.lock();
        try {
            cached = localCache.getIfPresent(fullKey);
            if (cached != null) {
                if (NULL_PLACEHOLDER.equals(cached)) return null;
                return (UserProfileResponse) cached;
            }
            if (redisTemplate != null) {
                String json = redisTemplate.opsForValue().get(fullKey);
                if (json != null) {
                    if (NULL_PLACEHOLDER.equals(json)) return null;
                    try {
                        UserProfileResponse res = objectMapper.readValue(json, UserProfileResponse.class);
                        localCache.put(fullKey, res);
                        return res;
                    } catch (JsonProcessingException e) {
                        log.warn("Failed to deserialize user cache for key {}: {}", fullKey, e.getMessage());
                    }
                }
            }
            UserProfileResponse res = loader.get();
            if (res == null) {
                if (redisTemplate != null) {
                    redisTemplate.opsForValue().set(fullKey, NULL_PLACEHOLDER, java.time.Duration.ofSeconds(props.getTtl().getNullPlaceholderSeconds()));
                }
                localCache.put(fullKey, NULL_PLACEHOLDER);
                return null;
            }
            int ttl = props.getTtl().getRedisUserSeconds() + ThreadLocalRandom.current().nextInt(Math.max(1, props.getTtl().getRedisRandomMax()));
            if (redisTemplate != null) {
                try {
                    redisTemplate.opsForValue().set(fullKey, objectMapper.writeValueAsString(res), java.time.Duration.ofSeconds(ttl));
                } catch (JsonProcessingException e) {
                    log.warn("Failed to serialize user cache for key {}: {}", fullKey, e.getMessage());
                }
            }
            localCache.put(fullKey, res);
            return res;
        } finally {
            lock.unlock();
            loadLocks.remove(fullKey, lock); // atomic: only remove if still this exact lock
        }
    }

    public void invalidate(Long userId) {
        String fullKey = props.getKeyPrefix() + "user:info:" + userId;
        localCache.invalidate(fullKey);
        if (redisTemplate != null) {
            redisTemplate.delete(fullKey);
        }
    }
}
