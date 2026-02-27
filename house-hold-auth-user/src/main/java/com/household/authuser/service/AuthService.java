package com.household.authuser.service;

import com.household.authuser.dto.request.LoginRequest;
import com.household.authuser.dto.request.RegisterRequest;
import com.household.authuser.dto.response.LoginResponse;
import com.household.authuser.dto.response.RegisterResponse;
import com.household.authuser.entity.User;
import com.household.authuser.mapper.UserMapper;
import com.household.authuser.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.concurrent.atomic.AtomicLong;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    // 简单自增 ID（本迭代单机可用，后续可换雪花算法）
    private static final AtomicLong ID_GEN = new AtomicLong(1_000_000_000_000L);

    @Transactional(rollbackFor = Exception.class)
    public RegisterResponse register(RegisterRequest req) {
        if (userMapper.selectByUsername(req.getUsername()) != null) {
            throw new UsernameExistsException("用户名已被使用");
        }
        LocalDate birthday = LocalDate.parse(req.getBirthday());
        User user = new User();
        user.setId(ID_GEN.incrementAndGet()); // 单机唯一，后续可改为雪花算法
        user.setUsername(req.getUsername());
        user.setPasswordHash(passwordEncoder.encode(req.getPassword()));
        user.setBirthday(birthday);
        user.setEmail(req.getEmail());
        user.setPhone(req.getPhone());
        userMapper.insert(user);
        return new RegisterResponse(
                String.valueOf(user.getId()),
                user.getUsername(),
                user.getBirthday().toString(),
                user.getEmail(),
                user.getPhone()
        );
    }

    public LoginResponse login(LoginRequest req) {
        User user = userMapper.selectByUsername(req.getUsername());
        if (user == null || !passwordEncoder.matches(req.getPassword(), user.getPasswordHash())) {
            throw new InvalidCredentialsException("用户名或密码错误");
        }
        String token = jwtUtil.generateToken(user.getId(), user.getUsername());
        LoginResponse.UserInfo info = new LoginResponse.UserInfo(
                String.valueOf(user.getId()),
                user.getUsername(),
                user.getBirthday() != null ? user.getBirthday().toString() : null
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
