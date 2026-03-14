package com.household.authuser.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * 家庭领域事件，用于 Kafka 异步通知下游服务（如财富服务可做缓存失效等）
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FamilyDomainEvent {

    public static final String TOPIC = "household.family.events";

    public static final String TYPE_FAMILY_CREATED = "FAMILY_CREATED";
    public static final String TYPE_ADMIN_CHANGED = "ADMIN_CHANGED";
    public static final String TYPE_MEMBER_JOINED = "MEMBER_JOINED";
    public static final String TYPE_MEMBER_REMOVED = "MEMBER_REMOVED";

    private String eventType;
    private Long familyId;
    private Long userId;
    private Long targetUserId;
    private String occurredAt;

    public static FamilyDomainEvent familyCreated(Long familyId, Long creatorUserId) {
        return new FamilyDomainEvent(TYPE_FAMILY_CREATED, familyId, creatorUserId, null, Instant.now().toString());
    }

    public static FamilyDomainEvent adminChanged(Long familyId, Long targetUserId) {
        return new FamilyDomainEvent(TYPE_ADMIN_CHANGED, familyId, null, targetUserId, Instant.now().toString());
    }

    public static FamilyDomainEvent memberJoined(Long familyId, Long userId) {
        return new FamilyDomainEvent(TYPE_MEMBER_JOINED, familyId, null, userId, Instant.now().toString());
    }

    public static FamilyDomainEvent memberRemoved(Long familyId, Long removedUserId) {
        return new FamilyDomainEvent(TYPE_MEMBER_REMOVED, familyId, null, removedUserId, Instant.now().toString());
    }
}
