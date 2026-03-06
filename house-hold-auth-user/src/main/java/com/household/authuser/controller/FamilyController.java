package com.household.authuser.controller;

import com.household.authuser.dto.request.FamilyCreateRequest;
import com.household.authuser.dto.request.FamilyJoinRequest;
import com.household.authuser.dto.request.FamilyMemberRoleUpdateRequest;
import com.household.authuser.dto.request.FamilyUpdateRequest;
import com.household.authuser.dto.response.FamilyResponse;
import com.household.authuser.service.FamilyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 家庭管理接口，提供家庭创建、加入、信息管理功能
 *
 * @author household
 * @date 2025/01/01
 */
@RestController
@RequestMapping("/family")
@RequiredArgsConstructor
public class FamilyController {

    private final FamilyService familyService;

    @PostMapping("/create")
    public ResponseEntity<FamilyResponse> create(
            @RequestHeader("X-User-Id") Long userId,
            @Valid @RequestBody FamilyCreateRequest request) {
        FamilyResponse response = familyService.create(userId, request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/join")
    public ResponseEntity<FamilyResponse> join(
            @RequestHeader("X-User-Id") Long userId,
            @RequestBody FamilyJoinRequest request) {
        FamilyResponse response = familyService.join(userId, request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{familyId}")
    public ResponseEntity<FamilyResponse> getFamily(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long familyId) {
        FamilyResponse response = familyService.getFamily(userId, familyId);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{familyId}")
    public ResponseEntity<FamilyResponse> updateFamily(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long familyId,
            @Valid @RequestBody FamilyUpdateRequest request) {
        FamilyResponse response = familyService.updateFamily(userId, familyId, request);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{familyId}/members/role")
    public ResponseEntity<FamilyResponse> updateMemberRole(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long familyId,
            @Valid @RequestBody FamilyMemberRoleUpdateRequest request) {
        FamilyResponse response = familyService.updateMemberRole(userId, familyId, request);
        return ResponseEntity.ok(response);
    }
}
