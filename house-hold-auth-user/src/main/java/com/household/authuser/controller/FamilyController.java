package com.household.authuser.controller;

import com.household.authuser.dto.request.*;
import com.household.authuser.dto.response.FamilyResponse;
import com.household.authuser.dto.response.JoinRequestResponse;
import com.household.authuser.service.FamilyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/family")
@RequiredArgsConstructor
public class FamilyController {

    private final FamilyService familyService;

    /** 供 wealth 等微服务通过 REST 同步校验当前用户是否为家庭管理员 */
    @GetMapping("/{familyId}/admin/check")
    public ResponseEntity<Map<String, Boolean>> checkAdmin(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long familyId) {
        return ResponseEntity.ok(Map.of("admin", familyService.isAdmin(userId, familyId)));
    }

    @PostMapping("/create")
    public ResponseEntity<FamilyResponse> create(
            @RequestHeader("X-User-Id") Long userId,
            @Valid @RequestBody FamilyCreateRequest request) {
        return ResponseEntity.ok(familyService.create(userId, request));
    }

    @GetMapping("/{familyId}")
    public ResponseEntity<FamilyResponse> getFamily(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long familyId) {
        return ResponseEntity.ok(familyService.getFamily(userId, familyId));
    }

    @PutMapping("/{familyId}")
    public ResponseEntity<FamilyResponse> updateFamily(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long familyId,
            @Valid @RequestBody FamilyUpdateRequest request) {
        return ResponseEntity.ok(familyService.updateFamily(userId, familyId, request));
    }

    @PutMapping("/{familyId}/members/role")
    public ResponseEntity<FamilyResponse> updateMemberRole(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long familyId,
            @Valid @RequestBody FamilyMemberRoleUpdateRequest request) {
        return ResponseEntity.ok(familyService.updateMemberRole(userId, familyId, request));
    }

    @PostMapping("/{familyId}/apply")
    public ResponseEntity<JoinRequestResponse> applyToJoin(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long familyId,
            @RequestBody(required = false) ApplyJoinRequest request) {
        if (request == null) request = new ApplyJoinRequest();
        return ResponseEntity.ok(familyService.applyToJoin(userId, familyId, request));
    }

    @GetMapping("/my-applications")
    public ResponseEntity<List<JoinRequestResponse>> getMyApplications(
            @RequestHeader("X-User-Id") Long userId) {
        return ResponseEntity.ok(familyService.getMyApplications(userId));
    }

    @GetMapping("/my-invitations")
    public ResponseEntity<List<JoinRequestResponse>> getMyInvitations(
            @RequestHeader("X-User-Id") Long userId) {
        return ResponseEntity.ok(familyService.getMyInvitations(userId));
    }

    @PutMapping("/invitations/{reqId}/accept")
    public ResponseEntity<Void> acceptInvitation(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long reqId) {
        familyService.acceptInvitation(userId, reqId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/invitations/{reqId}/reject")
    public ResponseEntity<Void> rejectInvitation(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long reqId) {
        familyService.rejectInvitation(userId, reqId);
        return ResponseEntity.noContent().build();
    }
}
