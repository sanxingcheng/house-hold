package com.household.wealth.controller;

import com.household.common.exception.BadRequestException;
import com.household.wealth.dto.response.SnapshotPointResponse;
import com.household.wealth.service.SnapshotService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * 财富历史接口，提供财富变化趋势查询
 *
 * @author household
 * @date 2025/01/01
 */
@RestController
@RequestMapping("/wealth/history")
@RequiredArgsConstructor
public class HistoryController {

    private final SnapshotService snapshotService;

    @GetMapping("/user")
    public ResponseEntity<List<SnapshotPointResponse>> userHistory(
            @RequestHeader("X-User-Id") Long userId,
            @RequestParam String from,
            @RequestParam String to) {
        return ResponseEntity.ok(
                snapshotService.getUserHistory(userId, LocalDate.parse(from), LocalDate.parse(to)));
    }

    @GetMapping("/family")
    public ResponseEntity<List<SnapshotPointResponse>> familyHistory(
            @RequestHeader(value = "X-Family-Id", required = false) Long familyId,
            @RequestParam String from,
            @RequestParam String to) {
        if (familyId == null) {
            throw new BadRequestException("尚未加入家庭");
        }
        return ResponseEntity.ok(
                snapshotService.getFamilyHistory(familyId, LocalDate.parse(from), LocalDate.parse(to)));
    }
}
