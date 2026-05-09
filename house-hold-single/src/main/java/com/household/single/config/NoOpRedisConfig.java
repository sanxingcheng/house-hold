package com.household.single.config;

import org.redisson.api.RBucket;
import org.redisson.api.RLock;
import org.redisson.api.RSet;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * Provides a no-op RedissonClient via dynamic proxy when Redis is not available.
 * The monolithic JAR does not require Redis - session management is JWT-only.
 */
@Configuration
public class NoOpRedisConfig {

    @Primary
    @Bean
    public RedissonClient noOpRedissonClient() {
        return (RedissonClient) Proxy.newProxyInstance(
                RedissonClient.class.getClassLoader(),
                new Class<?>[]{RedissonClient.class},
                new NoOpHandler()
        );
    }

    /**
     * Dynamic proxy handler that returns sensible defaults for all methods.
     * getBucket() returns a proxy RBucket that returns null for get()
     * getLock() returns a proxy RLock that returns true for tryLock()
     */
    private static class NoOpHandler implements InvocationHandler {
        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            String name = method.getName();
            // Return null for most methods
            if ("getBucket".equals(name)) {
                return Proxy.newProxyInstance(
                        RBucket.class.getClassLoader(),
                        new Class<?>[]{RBucket.class},
                        (bProxy, bMethod, bArgs) -> {
                            String bName = bMethod.getName();
                            if ("get".equals(bName) || "getAndDelete".equals(bName)) return null;
                            if ("delete".equals(bName) || "isExists".equals(bName)) return false;
                            if ("getName".equals(bName)) return "";
                            if ("size".equals(bName)) return 0L;
                            if ("touch".equals(bName)) return false;
                            if ("isExistsAsync".equals(bName) || "deleteAsync".equals(bName)
                                    || "touchAsync".equals(bName) || "compareAndSetAsync".equals(bName)
                                    || "trySetAsync".equals(bName)) return null;
                            if ("dump".equals(bName)) return new byte[0];
                            if ("dumpAsync".equals(bName)) return null;
                            if ("remainTimeToLive".equals(bName)) return 0L;
                            if ("remainTimeToLiveAsync".equals(bName)) return null;
                            if ("clearExpire".equals(bName) || "clearExpireAsync".equals(bName)) return false;
                            return null;
                        }
                );
            }
            if ("getSet".equals(name)) {
                return Proxy.newProxyInstance(
                        RSet.class.getClassLoader(),
                        new Class<?>[]{RSet.class},
                        (sProxy, sMethod, sArgs) -> {
                            String sName = sMethod.getName();
                            if ("add".equals(sName) || "remove".equals(sName)) return true;
                            if ("contains".equals(sName) || "containsAll".equals(sName)
                                    || "retainAll".equals(sName)) return false;
                            if ("size".equals(sName)) return 0;
                            if ("isEmpty".equals(sName)) return true;
                            if ("delete".equals(sName) || "isExists".equals(sName)
                                    || "clear".equals(sName)) return false;
                            if ("getName".equals(sName)) return "";
                            return null;
                        }
                );
            }
            if ("getLock".equals(name)) {
                return Proxy.newProxyInstance(
                        RLock.class.getClassLoader(),
                        new Class<?>[]{RLock.class},
                        (lProxy, lMethod, lArgs) -> {
                            String lName = lMethod.getName();
                            if ("tryLock".equals(lName)) return true;
                            if ("isLocked".equals(lName) || "isHeldByCurrentThread".equals(lName)) return false;
                            if ("forceUnlock".equals(lName) || "delete".equals(lName)
                                    || "isExists".equals(lName)) return false;
                            if ("getName".equals(lName)) return "";
                            if ("getHoldCount".equals(lName) || "remainTimeToLive".equals(lName)) return 0;
                            return null;
                        }
                );
            }
            if ("shutdown".equals(name)) return null;
            if ("isShutdown".equals(name) || "isShuttingDown".equals(name)) return false;
            if ("getConfig".equals(name)) return new Config();
            if ("isNativeTransportAvailable".equals(name)) return false;

            // Fields like $jacocoInit or toString/hashCode/equals
            if ("toString".equals(name)) return "NoOpRedissonClient";
            if ("hashCode".equals(name)) return System.identityHashCode(proxy);
            if ("equals".equals(name)) return proxy == args[0];
            return null;
        }
    }
}
