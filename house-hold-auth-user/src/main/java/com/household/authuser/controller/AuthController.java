package com.household.authuser.controller;

import com.household.authuser.dto.request.LoginRequest;
import com.household.authuser.dto.request.RegisterRequest;
import com.household.authuser.dto.response.LoginResponse;
import com.household.authuser.dto.response.PasswordPublicKeyResponse;
import com.household.authuser.dto.response.RegisterResponse;
import com.household.authuser.service.AuthService;
import com.household.authuser.service.PasswordCryptoService;
import com.household.common.exception.BadRequestException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

/**
 * 认证接口，提供注册、登录和登出功能
 *
 * @author household
 * @date 2025/01/01
 */
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final PasswordCryptoService passwordCryptoService;

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("OK");
    }

    @GetMapping("/password-public-key")
    public ResponseEntity<PasswordPublicKeyResponse> passwordPublicKey() {
        return ResponseEntity.ok(passwordCryptoService.getPublicKey());
    }

    @PostMapping("/register")
    public ResponseEntity<RegisterResponse> register(@Valid @RequestBody RegisterRequest request) {
        request.setPassword(resolvePassword(request.getPassword(), request.getEncryptedPassword()));
        validateRegisterPassword(request.getPassword());
        RegisterResponse response = authService.register(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        request.setPassword(resolvePassword(request.getPassword(), request.getEncryptedPassword()));
        LoginResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestHeader(value = "X-User-Id", required = false) Long userId) {
        if (userId != null) {
            authService.logout(userId);
        }
        return ResponseEntity.noContent().build();
    }

    /**
     * 解析客户端提交的密码字段；优先使用密文，避免前端明文密码进入业务层。
     *
     * @param plainPassword 兼容旧客户端或内部测试的明文密码
     * @param encryptedPassword 前端使用 RSA-OAEP/SHA-256 加密后的密码
     * @return 可交给认证服务处理的密码明文
     */
    private String resolvePassword(String plainPassword, String encryptedPassword) {
        if (StringUtils.hasText(encryptedPassword)) {
            return passwordCryptoService.decryptPassword(encryptedPassword);
        }
        if (!StringUtils.hasText(plainPassword)) {
            throw new BadRequestException("密码不能为空");
        }
        return plainPassword;
    }

    /**
     * 注册密码长度校验需要在服务端解密后再次执行，防止密文请求绕过 DTO 的明文字段校验。
     *
     * @param password 解密后的注册密码
     */
    private void validateRegisterPassword(String password) {
        if (password.length() < 6) {
            throw new BadRequestException("密码至少6位");
        }
    }
}
