package com.household.wealth.controller;

import com.household.wealth.dto.request.AccountCreateRequest;
import com.household.wealth.dto.request.AccountUpdateRequest;
import com.household.wealth.dto.response.AccountResponse;
import com.household.wealth.service.AccountService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 账户管理接口，提供账户的增删改查功能
 *
 * @author household
 * @date 2025/01/01
 */
@RestController
@RequestMapping("/wealth/accounts")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;

    @GetMapping
    public ResponseEntity<List<AccountResponse>> list(@RequestHeader("X-User-Id") Long userId) {
        return ResponseEntity.ok(accountService.getAccounts(userId));
    }

    @PostMapping
    public ResponseEntity<AccountResponse> create(
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader(value = "X-Family-Id", required = false) Long familyId,
            @Valid @RequestBody AccountCreateRequest req) {
        return ResponseEntity.ok(accountService.createAccount(userId, familyId, req));
    }

    @PutMapping("/{id}")
    public ResponseEntity<AccountResponse> update(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long id,
            @Valid @RequestBody AccountUpdateRequest req) {
        return ResponseEntity.ok(accountService.updateAccount(userId, id, req));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long id) {
        accountService.deleteAccount(userId, id);
        return ResponseEntity.noContent().build();
    }
}
