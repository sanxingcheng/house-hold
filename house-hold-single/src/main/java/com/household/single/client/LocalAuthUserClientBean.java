package com.household.single.client;

import com.household.authuser.service.FamilyService;
import com.household.wealth.client.AuthUserClient;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Primary implementation of AuthUserClient (Feign interface) that delegates
 * to FamilyService directly instead of calling via HTTP.
 */
@Primary
@Component
public class LocalAuthUserClientBean implements AuthUserClient {

    private final FamilyService familyService;

    public LocalAuthUserClientBean(FamilyService familyService) {
        this.familyService = familyService;
    }

    @Override
    public Map<String, Boolean> checkFamilyAdmin(Long familyId, Long userId) {
        boolean admin = familyService.isAdmin(userId, familyId);
        return Map.of("admin", admin);
    }
}
