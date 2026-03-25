package com.household.wealth.controller;

import com.household.wealth.dto.response.OperationLogResponse;
import com.household.wealth.service.OperationLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/wealth/operation-logs")
@RequiredArgsConstructor
public class OperationLogController {

    private final OperationLogService operationLogService;

    @GetMapping
    public ResponseEntity<Map<String, Object>> list(
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader("X-Family-Id") Long familyId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<OperationLogResponse> result = operationLogService.listByFamily(userId, familyId, page, size);
        return ResponseEntity.ok(Map.of(
                "content", result.getContent(),
                "totalElements", result.getTotalElements(),
                "totalPages", result.getTotalPages(),
                "number", result.getNumber(),
                "size", result.getSize()));
    }
}
