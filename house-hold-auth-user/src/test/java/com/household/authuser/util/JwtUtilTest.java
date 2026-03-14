package com.household.authuser.util;

import com.household.authuser.config.JwtConfig;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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

    /** 验证两参数版本 generateToken 生成非空 JWT 字符串 */
    @Test
    @DisplayName("generateToken(userId, username) 生成非空 token")
    void generateToken_twoArgs_returnsNonEmptyString() {
        String token = jwtUtil.generateToken(1L, "user1");
        assertThat(token).isNotBlank();
    }

    /** 验证三参数版本 generateToken 在传入 familyId 时 claim 中包含 familyId */
    @Test
    @DisplayName("generateToken(userId, username, familyId) 包含 familyId claim")
    void generateToken_withFamilyId_includesFamilyIdClaim() {
        String token = jwtUtil.generateToken(1L, "user1", 200L);
        Claims claims = jwtUtil.parseToken(token);
        assertThat(claims.get("familyId", String.class)).isEqualTo("200");
    }

    /** 验证 familyId 为 null 时 claim 中不包含 familyId 字段 */
    @Test
    @DisplayName("generateToken familyId 为 null 时不含 familyId claim")
    void generateToken_withNullFamilyId_doesNotIncludeFamilyIdClaim() {
        String token = jwtUtil.generateToken(1L, "user1", null);
        Claims claims = jwtUtil.parseToken(token);
        assertThat(claims.get("familyId")).isNull();
    }

    /** 验证 parseToken 能正确解析出 subject (userId) 和 username */
    @Test
    @DisplayName("parseToken 可解析出 subject 和 username")
    void parseToken_returnsClaimsWithSubjectAndUsername() {
        String token = jwtUtil.generateToken(100L, "testuser");
        Claims claims = jwtUtil.parseToken(token);
        assertThat(claims.getSubject()).isEqualTo("100");
        assertThat(claims.get("username", String.class)).isEqualTo("testuser");
    }

    /** 验证过期 token 解析时抛出 ExpiredJwtException */
    @Test
    @DisplayName("过期 token 解析时抛出 ExpiredJwtException")
    void parseToken_expired_throwsExpiredJwtException() {
        JwtConfig expiredConfig = new JwtConfig();
        expiredConfig.setSecret("test-secret-key-at-least-256-bits-for-hs256-algorithm");
        expiredConfig.setExpirationMs(0L);
        JwtUtil expiredJwtUtil = new JwtUtil(expiredConfig);

        String token = expiredJwtUtil.generateToken(1L, "user1");

        assertThatThrownBy(() -> expiredJwtUtil.parseToken(token))
                .isInstanceOf(ExpiredJwtException.class);
    }
}
