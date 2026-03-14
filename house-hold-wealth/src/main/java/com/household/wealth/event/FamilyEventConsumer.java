package com.household.wealth.event;

import com.household.wealth.cache.SummaryCacheService;
import com.household.wealth.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class FamilyEventConsumer {

    private final AccountRepository accountRepository;

    @Autowired(required = false)
    private SummaryCacheService summaryCacheService;

    @KafkaListener(topics = "household.family.events", groupId = "house-hold-wealth")
    @Transactional(rollbackFor = Exception.class)
    public void onFamilyEvent(Map<String, Object> event) {
        String eventType = (String) event.get("eventType");
        Long familyId = toLong(event.get("familyId"));
        Long targetUserId = toLong(event.get("targetUserId"));

        if (eventType == null) return;

        switch (eventType) {
            case "MEMBER_JOINED" -> {
                if (targetUserId != null && familyId != null) {
                    int updated = accountRepository.updateFamilyIdByUserId(targetUserId, familyId);
                    log.info("MEMBER_JOINED: synced {} accounts for user {} to family {}",
                            updated, targetUserId, familyId);
                    invalidateCache(familyId);
                }
            }
            case "MEMBER_REMOVED" -> {
                if (targetUserId != null) {
                    int cleared = accountRepository.clearFamilyIdByUserId(targetUserId);
                    log.info("MEMBER_REMOVED: cleared familyId on {} accounts for user {}",
                            cleared, targetUserId);
                    invalidateCache(familyId);
                }
            }
            default -> log.debug("Ignoring family event: {}", eventType);
        }
    }

    private void invalidateCache(Long familyId) {
        if (summaryCacheService != null && familyId != null) {
            summaryCacheService.invalidateFamily(familyId);
        }
    }

    private static Long toLong(Object value) {
        if (value == null) return null;
        if (value instanceof Number n) return n.longValue();
        try {
            return Long.parseLong(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
