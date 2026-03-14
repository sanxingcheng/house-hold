package com.household.wealth.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.Map;

/**
 * 调用 auth-user 服务的 REST 接口，用于同步校验家庭管理员等。
 */
@FeignClient(name = "house-hold-auth-user", path = "/family")
public interface AuthUserClient {

    @GetMapping("/{familyId}/admin/check")
    Map<String, Boolean> checkFamilyAdmin(
            @PathVariable("familyId") Long familyId,
            @RequestHeader("X-User-Id") Long userId);
}
