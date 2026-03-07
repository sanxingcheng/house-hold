package com.household.wealth.scheduler;

import com.household.wealth.repository.AccountRepository;
import com.household.wealth.service.SnapshotService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 夜间批量快照定时任务。
 * 使用 Redisson 分布式锁防止多实例重复执行。
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "snapshot.scheduler.enabled", havingValue = "true", matchIfMissing = true)
public class SnapshotScheduler {

    private final AccountRepository accountRepository;
    private final SnapshotService snapshotService;
    private final RedissonClient redissonClient;

    private static final String LOCK_KEY = "household:lock:snapshot-scheduler";

    @Scheduled(cron = "${snapshot.scheduler.cron:0 0 2 * * ?}")
    public void nightlySnapshot() {
        RLock lock = redissonClient.getLock(LOCK_KEY);
        boolean acquired = false;
        try {
            acquired = lock.tryLock(0, 30, TimeUnit.MINUTES);
            if (!acquired) {
                log.info("Snapshot scheduler skipped: another instance holds the lock");
                return;
            }

            log.info("Snapshot scheduler started");
            long start = System.currentTimeMillis();

            List<Long> userIds = accountRepository.findDistinctUserIds();
            int success = 0;
            int fail = 0;

            for (Long userId : userIds) {
                try {
                    Long familyId = accountRepository.findByUserId(userId).stream()
                            .map(a -> a.getFamilyId())
                            .filter(fid -> fid != null)
                            .findFirst().orElse(null);

                    snapshotService.triggerSnapshot(userId, familyId);
                    success++;
                } catch (Exception e) {
                    fail++;
                    log.error("Snapshot failed for userId={}", userId, e);
                }
            }

            long elapsed = System.currentTimeMillis() - start;
            log.info("Snapshot scheduler finished: total={}, success={}, fail={}, elapsed={}ms",
                    userIds.size(), success, fail, elapsed);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Snapshot scheduler interrupted", e);
        } finally {
            if (acquired && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }
}
