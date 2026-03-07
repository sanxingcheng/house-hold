package com.household.wealth.controller;

import com.household.wealth.dto.request.FamilyAssetCreateRequest;
import com.household.wealth.dto.request.FamilyAssetUpdateRequest;
import com.household.wealth.dto.response.FamilyAssetResponse;
import com.household.wealth.service.FamilyAssetService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/wealth/family-assets")
@RequiredArgsConstructor
public class FamilyAssetController {

    private final FamilyAssetService familyAssetService;

    @GetMapping
    public ResponseEntity<List<FamilyAssetResponse>> list(
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader(value = "X-Family-Id", required = false) Long familyId) {
        if (familyId == null) {
            return ResponseEntity.ok(List.of());
        }
        return ResponseEntity.ok(familyAssetService.listByFamily(familyId));
    }

    @PostMapping
    public ResponseEntity<FamilyAssetResponse> create(
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader(value = "X-Family-Id", required = false) Long familyId,
            @Valid @RequestBody FamilyAssetCreateRequest request) {
        return ResponseEntity.ok(familyAssetService.create(userId, familyId, request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<FamilyAssetResponse> update(
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader(value = "X-Family-Id", required = false) Long familyId,
            @PathVariable Long id,
            @RequestBody FamilyAssetUpdateRequest request) {
        return ResponseEntity.ok(familyAssetService.update(userId, familyId, id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader(value = "X-Family-Id", required = false) Long familyId,
            @PathVariable Long id) {
        familyAssetService.delete(userId, familyId, id);
        return ResponseEntity.noContent().build();
    }
}
