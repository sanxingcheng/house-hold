package com.household.wealth.controller;

import com.household.wealth.dto.response.WealthSummaryResponse;
import com.household.wealth.service.AccountService;
import com.household.wealth.service.SummaryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 财富汇总接口，提供用户和家庭的资产概览查询
 *
 * @author household
 * @date 2025/01/01
 */
@RestController
@RequestMapping("/wealth/summary")
@RequiredArgsConstructor
public class SummaryController {

    private final SummaryService summaryService;

    @GetMapping("/user")
    public ResponseEntity<WealthSummaryResponse> userSummary(@RequestHeader("X-User-Id") Long userId) {
        return ResponseEntity.ok(summaryService.getUserSummary(userId));
    }

    @GetMapping("/family")
    public ResponseEntity<WealthSummaryResponse> familySummary(
            @RequestHeader(value = "X-Family-Id", required = false) Long familyId) {
        if (familyId == null) {
            throw new AccountService.NoFamilyException("尚未加入家庭");
        }
        return ResponseEntity.ok(summaryService.getFamilySummary(familyId));
    }
}
