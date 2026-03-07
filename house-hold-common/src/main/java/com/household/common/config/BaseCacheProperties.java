package com.household.common.config;

import lombok.Data;

/**
 * 缓存配置公共基类，各模块继承后补充模块专属 TTL 字段
 *
 * @author household
 */
@Data
public abstract class BaseCacheProperties {

    private boolean enabled = true;
    private String keyPrefix = "household:";
    private Local local = new Local();

    @Data
    public static class Local {
        private int maxSize = 1000;
        private int ttlSeconds = 300;
    }

    public abstract int getRedisRandomMax();

    public abstract int getNullPlaceholderSeconds();
}
