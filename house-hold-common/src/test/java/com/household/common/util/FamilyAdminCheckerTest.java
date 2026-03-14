package com.household.common.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.redisson.api.RSet;
import org.redisson.api.RedissonClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("FamilyAdminChecker 单元测试")
class FamilyAdminCheckerTest {

    @Mock
    private RedissonClient redissonClient;

    @SuppressWarnings("unchecked")
    private RSet<String> mockAdminSet(Long familyId) {
        RSet<String> set = mock(RSet.class);
        doReturn(set).when(redissonClient).getSet("family:admins:" + familyId);
        return set;
    }

    /** 验证用户在管理员集合中时 isAdmin 返回 true */
    @Test
    @DisplayName("用户是管理员时 isAdmin 返回 true")
    void isAdmin_whenUserInSet_returnsTrue() {
        RSet<String> set = mockAdminSet(10L);
        when(set.contains("1")).thenReturn(true);

        assertThat(FamilyAdminChecker.isAdmin(redissonClient, 10L, 1L)).isTrue();
    }

    /** 验证用户不在管理员集合中时 isAdmin 返回 false */
    @Test
    @DisplayName("用户不是管理员时 isAdmin 返回 false")
    void isAdmin_whenUserNotInSet_returnsFalse() {
        RSet<String> set = mockAdminSet(10L);
        when(set.contains("2")).thenReturn(false);

        assertThat(FamilyAdminChecker.isAdmin(redissonClient, 10L, 2L)).isFalse();
    }

    /** 验证 familyId 或 userId 为 null 时直接返回 false，不访问 Redis */
    @Test
    @DisplayName("familyId 或 userId 为 null 时返回 false")
    void isAdmin_nullParams_returnsFalse() {
        assertThat(FamilyAdminChecker.isAdmin(redissonClient, null, 1L)).isFalse();
        assertThat(FamilyAdminChecker.isAdmin(redissonClient, 10L, null)).isFalse();
        verifyNoInteractions(redissonClient);
    }

    /** 验证 addAdmin 将用户 ID 添加到 Redis Set */
    @Test
    @DisplayName("addAdmin 向 Redis Set 添加用户 ID")
    void addAdmin_addsUserIdToSet() {
        RSet<String> set = mockAdminSet(10L);

        FamilyAdminChecker.addAdmin(redissonClient, 10L, 5L);

        verify(set).add("5");
    }

    /** 验证 removeAdmin 从 Redis Set 移除用户 ID */
    @Test
    @DisplayName("removeAdmin 从 Redis Set 移除用户 ID")
    void removeAdmin_removesUserIdFromSet() {
        RSet<String> set = mockAdminSet(10L);

        FamilyAdminChecker.removeAdmin(redissonClient, 10L, 5L);

        verify(set).remove("5");
    }

    /** 验证 adminSetKey 生成正确的 Redis key 格式 */
    @Test
    @DisplayName("adminSetKey 返回正确的 key 格式")
    void adminSetKey_returnsCorrectKey() {
        assertThat(FamilyAdminChecker.adminSetKey(42L)).isEqualTo("family:admins:42");
    }
}
