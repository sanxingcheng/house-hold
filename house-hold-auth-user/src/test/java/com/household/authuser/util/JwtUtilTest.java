package com.household.authuser.util;

import com.household.authuser.config.JwtConfig;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("JwtUtil 单元测试")
class JwtUtilTest {

    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        JwtConfig config = new JwtConfig();
        config.setSecret("test-secret-key-at-least-256-bits-for-hs256-algorithm");
        config.setExpirationMs(3600000L);
        jwtUtil = new JwtUtil(config);
    }

    @Test
    @DisplayName("generateToken 生成非空 token")
    void generateToken_returnsNonEmptyString() {
        String token = jwtUtil.generateToken(1L, "user1");
        assertThat(token).isNotBlank();
    }

    @Test
    @DisplayName("parseToken 可解析出 subject 和 username")
    void parseToken_returnsClaimsWithSubjectAndUsername() {
        String token = jwtUtil.generateToken(100L, "testuser");
        Claims claims = jwtUtil.parseToken(token);
        assertThat(claims.getSubject()).isEqualTo("100");
        assertThat(claims.get("username", String.class)).isEqualTo("testuser");
    }
}
