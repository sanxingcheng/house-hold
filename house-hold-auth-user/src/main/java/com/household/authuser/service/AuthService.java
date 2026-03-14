package com.household.authuser.service;

import com.household.authuser.config.JwtConfig;
import com.household.authuser.dto.request.LoginRequest;
import com.household.authuser.dto.request.RegisterRequest;
import com.household.authuser.dto.response.LoginResponse;
import com.household.authuser.dto.response.RegisterResponse;
import com.household.authuser.entity.User;
import com.household.authuser.repository.UserRepository;
import com.household.authuser.util.JwtUtil;
import com.household.common.util.SnowflakeIdGenerator;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.concurrent.TimeUnit;

/**
 * 认证服务，处理用户注册、登录与登出
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
    private final RedissonClient redissonClient;
    private final JwtConfig jwtConfig;

    private static final SnowflakeIdGenerator ID_GEN = new SnowflakeIdGenerator(1, 1);
    private static final String SESSION_KEY_PREFIX = "household:session:";

    @Transactional(rollbackFor = Exception.class)
    public RegisterResponse register(RegisterRequest req) {
        if (userRepository.findByUsername(req.getUsername()).isPresent()) {
            throw new UsernameExistsException("用户名已被使用");
        }
        LocalDate birthday = LocalDate.parse(req.getBirthday());
        User user = new User();
        user.setId(ID_GEN.nextId());
        user.setUsername(req.getUsername());
        user.setPasswordHash(passwordEncoder.encode(req.getPassword()));
        user.setName(req.getName());
        user.setGender(req.getGender());
        user.setBirthday(birthday);
        user.setEmail(req.getEmail());
        user.setPhone(req.getPhone());
        userRepository.save(user);
        return new RegisterResponse(
                String.valueOf(user.getId()),
                user.getUsername(),
                user.getName(),
                user.getGender(),
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

        RBucket<String> session = redissonClient.getBucket(SESSION_KEY_PREFIX + user.getId());
        session.set(token, jwtConfig.getExpirationMs(), TimeUnit.MILLISECONDS);

        LoginResponse.UserInfo info = new LoginResponse.UserInfo(
                String.valueOf(user.getId()),
                user.getUsername(),
                user.getBirthday() != null ? user.getBirthday().toString() : null,
                user.getFamilyId() != null ? String.valueOf(user.getFamilyId()) : null
        );
        return new LoginResponse(token, info);
    }

    @Transactional(rollbackFor = Exception.class)
    public User registerForFamily(String username, String password, String name, String gender,
                                  String birthday, String email, String phone) {
        if (userRepository.findByUsername(username).isPresent()) {
            throw new UsernameExistsException("用户名已被使用");
        }
        LocalDate bd = LocalDate.parse(birthday);
        User user = new User();
        user.setId(ID_GEN.nextId());
        user.setUsername(username);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setName(name);
        user.setGender(gender);
        user.setBirthday(bd);
        user.setEmail(email);
        user.setPhone(phone);
        userRepository.save(user);
        return user;
    }

    public void logout(Long userId) {
        redissonClient.getBucket(SESSION_KEY_PREFIX + userId).delete();
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
