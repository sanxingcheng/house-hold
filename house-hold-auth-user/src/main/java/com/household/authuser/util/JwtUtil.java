package com.household.authuser.util;

import com.household.authuser.config.JwtConfig;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * JWT 工具类，提供令牌生成与解析功能
 *
 * @author household
 * @date 2025/01/01
 */
@Component
public class JwtUtil {

    private final JwtConfig config;

    public JwtUtil(JwtConfig config) {
        this.config = config;
    }

    private SecretKey secretKey() {
        byte[] keyBytes = config.getSecret().getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public String generateToken(Long userId, String username) {
        return generateToken(userId, username, null);
    }

    public String generateToken(Long userId, String username, Long familyId) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + config.getExpirationMs());
        var builder = Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("username", username)
                .issuedAt(now)
                .expiration(expiry);
        if (familyId != null) {
            builder.claim("familyId", String.valueOf(familyId));
        }
        return builder.signWith(secretKey()).compact();
    }

    public Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(secretKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
