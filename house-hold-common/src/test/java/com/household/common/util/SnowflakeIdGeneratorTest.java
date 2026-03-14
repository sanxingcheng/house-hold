package com.household.common.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("SnowflakeIdGenerator 单元测试")
class SnowflakeIdGeneratorTest {

    /** 验证 datacenterId 超出合法范围时构造函数抛出 IllegalArgumentException */
    @Test
    @DisplayName("datacenterId 超限时抛出 IllegalArgumentException")
    void constructor_invalidDatacenterId_throws() {
        assertThatThrownBy(() -> new SnowflakeIdGenerator(-1, 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("datacenterId");

        assertThatThrownBy(() -> new SnowflakeIdGenerator(32, 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("datacenterId");
    }

    /** 验证 workerId 超出合法范围时构造函数抛出 IllegalArgumentException */
    @Test
    @DisplayName("workerId 超限时抛出 IllegalArgumentException")
    void constructor_invalidWorkerId_throws() {
        assertThatThrownBy(() -> new SnowflakeIdGenerator(0, -1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("workerId");

        assertThatThrownBy(() -> new SnowflakeIdGenerator(0, 32))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("workerId");
    }

    /** 验证合法的边界值 datacenterId=0,workerId=0 可正常构造 */
    @Test
    @DisplayName("合法边界值可正常构造")
    void constructor_validBoundaryValues_succeeds() {
        new SnowflakeIdGenerator(0, 0);
        new SnowflakeIdGenerator(31, 31);
    }

    /** 验证生成的 ID 为正数 */
    @Test
    @DisplayName("nextId 生成正数 ID")
    void nextId_returnsPositiveId() {
        SnowflakeIdGenerator gen = new SnowflakeIdGenerator(1, 1);
        long id = gen.nextId();
        assertThat(id).isPositive();
    }

    /** 验证连续生成的 1000 个 ID 全部唯一，确保序列号机制正常 */
    @Test
    @DisplayName("连续生成 1000 个 ID 全部唯一")
    void nextId_generatesUniqueIds() {
        SnowflakeIdGenerator gen = new SnowflakeIdGenerator(1, 1);
        Set<Long> ids = new HashSet<>();
        int count = 1000;
        for (int i = 0; i < count; i++) {
            ids.add(gen.nextId());
        }
        assertThat(ids).hasSize(count);
    }

    /** 验证生成的 ID 是单调递增的 */
    @Test
    @DisplayName("生成的 ID 单调递增")
    void nextId_idsAreMonotonicallyIncreasing() {
        SnowflakeIdGenerator gen = new SnowflakeIdGenerator(1, 1);
        long prev = gen.nextId();
        for (int i = 0; i < 100; i++) {
            long current = gen.nextId();
            assertThat(current).isGreaterThan(prev);
            prev = current;
        }
    }

    /** 验证不同 workerId 的生成器不会产生冲突 ID */
    @Test
    @DisplayName("不同 workerId 生成的 ID 互不重复")
    void nextId_differentWorkers_noCollision() {
        SnowflakeIdGenerator gen1 = new SnowflakeIdGenerator(1, 1);
        SnowflakeIdGenerator gen2 = new SnowflakeIdGenerator(1, 2);
        Set<Long> ids = new HashSet<>();
        for (int i = 0; i < 100; i++) {
            assertThat(ids.add(gen1.nextId())).isTrue();
            assertThat(ids.add(gen2.nextId())).isTrue();
        }
    }
}
