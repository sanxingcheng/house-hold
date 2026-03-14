package com.household.authuser.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

@Component
@RequiredArgsConstructor
@Slf4j
public class FamilyEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publish(FamilyDomainEvent event) {
        String key = event.getFamilyId() != null ? String.valueOf(event.getFamilyId()) : "unknown";
        CompletableFuture<SendResult<String, Object>> future =
                kafkaTemplate.send(FamilyDomainEvent.TOPIC, key, event);
        future.whenComplete((result, ex) -> {
            if (ex != null) {
                log.warn("Failed to send family event: {}", ex.getMessage());
            }
        });
    }
}
