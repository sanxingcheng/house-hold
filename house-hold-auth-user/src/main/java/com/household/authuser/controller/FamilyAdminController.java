package com.household.authuser.controller;

import com.household.authuser.dto.request.CreateMemberRequest;
import com.household.authuser.dto.request.InviteUserRequest;
import com.household.authuser.dto.request.SetAdminRequest;
import com.household.authuser.dto.response.FamilyResponse;
import com.household.authuser.dto.response.JoinRequestResponse;
import com.household.authuser.entity.User;
import com.household.authuser.service.AuthService;
import com.household.authuser.service.FamilyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/family/{familyId}/admin")
@RequiredArgsConstructor
public class FamilyAdminController {

    private final FamilyService familyService;
    private final AuthService authService;

    @PostMapping("/create-member")
    public ResponseEntity<FamilyResponse> createMember(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long familyId,
            @Valid @RequestBody CreateMemberRequest request) {
        if (!familyService.isAdmin(userId, familyId)) {
            return ResponseEntity.status(403).build();
        }
        User newUser = authService.registerForFamily(
                request.getUsername(), request.getPassword(), request.getName(),
                request.getBirthday(), request.getEmail(), request.getPhone());
        FamilyResponse response = familyService.addCreatedMember(familyId, newUser, request.getRole());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/invite")
    public ResponseEntity<JoinRequestResponse> inviteUser(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long familyId,
            @Valid @RequestBody InviteUserRequest request) {
        return ResponseEntity.ok(familyService.inviteUser(userId, familyId, request));
    }

    @PutMapping("/members/{targetUserId}/set-admin")
    public ResponseEntity<Void> setAdmin(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long familyId,
            @PathVariable Long targetUserId,
            @RequestBody SetAdminRequest request) {
        familyService.setAdmin(userId, familyId, targetUserId, request.isAdmin());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/members/{targetUserId}")
    public ResponseEntity<Void> removeMember(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long familyId,
            @PathVariable Long targetUserId) {
        familyService.removeMember(userId, familyId, targetUserId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/requests")
    public ResponseEntity<List<JoinRequestResponse>> getPendingRequests(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long familyId) {
        return ResponseEntity.ok(familyService.getPendingRequests(userId, familyId));
    }

    @PutMapping("/requests/{reqId}/approve")
    public ResponseEntity<Void> approveRequest(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long familyId,
            @PathVariable Long reqId) {
        familyService.approveRequest(userId, familyId, reqId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/requests/{reqId}/reject")
    public ResponseEntity<Void> rejectRequest(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long familyId,
            @PathVariable Long reqId) {
        familyService.rejectRequest(userId, familyId, reqId);
        return ResponseEntity.noContent().build();
    }
}
