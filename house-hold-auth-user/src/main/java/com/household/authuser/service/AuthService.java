package com.household.authuser.service;

import com.household.authuser.dto.request.LoginRequest;
import com.household.authuser.dto.request.RegisterRequest;
import com.household.authuser.dto.response.LoginResponse;
import com.household.authuser.dto.response.RegisterResponse;
import com.household.authuser.entity.User;
import com.household.authuser.repository.UserRepository;
import com.household.authuser.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 认证服务，处理用户注册与登录
 *
 * @author household
 * @date 2025/01/01
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    private static final AtomicLong ID_GEN = new AtomicLong(1_000_000_000_000L);

    @Transactional(rollbackFor = Exception.class)
    public RegisterResponse register(RegisterRequest req) {
        if (userRepository.findByUsername(req.getUsername()).isPresent()) {
            throw new UsernameExistsException("用户名已被使用");
        }
        LocalDate birthday = LocalDate.parse(req.getBirthday());
        User user = new User();
        user.setId(ID_GEN.incrementAndGet());
        user.setUsername(req.getUsername());
        user.setPasswordHash(passwordEncoder.encode(req.getPassword()));
        user.setName(req.getName());
        user.setBirthday(birthday);
        user.setEmail(req.getEmail());
        user.setPhone(req.getPhone());
        userRepository.save(user);
        return new RegisterResponse(
                String.valueOf(user.getId()),
                user.getUsername(),
                user.getName(),
                user.getBirthday().toString(),
                user.getEmail(),
                user.getPhone()
        );
    }

    public LoginResponse login(LoginRequest req) {
        User user = userRepository.findByUsername(req.getUsername()).orElse(null);
        if (user == null || !passwordEncoder.matches(req.getPassword(), user.getPasswordHash())) {
            throw new InvalidCredentialsException("用户名或密码错误");
        }
        String token = jwtUtil.generateToken(user.getId(), user.getUsername(), user.getFamilyId());
        LoginResponse.UserInfo info = new LoginResponse.UserInfo(
                String.valueOf(user.getId()),
                user.getUsername(),
                user.getBirthday() != null ? user.getBirthday().toString() : null,
                user.getFamilyId() != null ? String.valueOf(user.getFamilyId()) : null
        );
        return new LoginResponse(token, info);
    }

    public static class UsernameExistsException extends RuntimeException {
        public UsernameExistsException(String message) {
            super(message);
        }
    }

    public static class InvalidCredentialsException extends RuntimeException {
        public InvalidCredentialsException(String message) {
            super(message);
        }
    }
}
