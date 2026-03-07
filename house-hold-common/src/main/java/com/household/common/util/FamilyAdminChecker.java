package com.household.common.util;

import org.redisson.api.RSet;
import org.redisson.api.RedissonClient;

/**
 * 通过 Redis Set 校验家庭管理员身份，供各微服务共用。
 * auth-user 负责写入，wealth 等服务负责读取。
 */
public final class FamilyAdminChecker {

    private static final String ADMIN_SET_PREFIX = "family:admins:";

    private FamilyAdminChecker() {}

    public static boolean isAdmin(RedissonClient redissonClient, Long familyId, Long userId) {
        if (familyId == null || userId == null) {
            return false;
        }
        RSet<String> adminSet = redissonClient.getSet(ADMIN_SET_PREFIX + familyId);
        return adminSet.contains(String.valueOf(userId));
    }

    public static void addAdmin(RedissonClient redissonClient, Long familyId, Long userId) {
        RSet<String> adminSet = redissonClient.getSet(ADMIN_SET_PREFIX + familyId);
        adminSet.add(String.valueOf(userId));
    }

    public static void removeAdmin(RedissonClient redissonClient, Long familyId, Long userId) {
        RSet<String> adminSet = redissonClient.getSet(ADMIN_SET_PREFIX + familyId);
        adminSet.remove(String.valueOf(userId));
    }

    public static String adminSetKey(Long familyId) {
        return ADMIN_SET_PREFIX + familyId;
    }
}
