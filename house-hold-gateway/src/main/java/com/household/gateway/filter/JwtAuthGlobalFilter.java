package com.household.gateway.filter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * JWT 认证全局过滤器，验证请求令牌并通过 Redis Session 校验登录状态
 *
 * @author household
 * @date 2025/01/01
 */
@Component
public class JwtAuthGlobalFilter implements GlobalFilter, Ordered {

    @Value("${app.jwt.secret:house-hold-jwt-secret-key-change-in-production-min-256-bits}")
    private String secret;

    @Autowired
    private RedissonClient redissonClient;

    private static final String SESSION_KEY_PREFIX = "household:session:";
    private static final List<String> SKIP_PATHS = List.of("/auth/login", "/auth/register", "/auth/health");

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest().mutate()
                .headers(h -> { h.remove("X-User-Id"); h.remove("X-Family-Id"); })
                .build();
        exchange = exchange.mutate().request(request).build();

        if ("OPTIONS".equalsIgnoreCase(request.getMethod().name())) {
            return chain.filter(exchange);
        }
        String path = request.getPath().value();
        if (SKIP_PATHS.contains(path)) {
            return chain.filter(exchange);
        }
        if (!path.startsWith("/auth/") && !path.startsWith("/user/")
                && !path.startsWith("/family/") && !path.startsWith("/wealth/")) {
            return chain.filter(exchange);
        }
        String auth = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (auth == null || !auth.startsWith("Bearer ")) {
            return unauthorized(exchange.getResponse());
        }
        String token = auth.substring(7).trim();
        Claims claims;
        try {
            claims = parseToken(token);
        } catch (Exception e) {
            return unauthorized(exchange.getResponse());
        }

        String userId = claims.getSubject();
        String familyId = claims.get("familyId", String.class);

        ServerWebExchange finalExchange = exchange;
        return Mono.fromCallable(() -> {
                    RBucket<String> session = redissonClient.getBucket(SESSION_KEY_PREFIX + userId);
                    return token.equals(session.get());
                })
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(valid -> {
                    if (!valid) {
                        return unauthorized(finalExchange.getResponse());
                    }
                    ServerHttpRequest mutated = finalExchange.getRequest().mutate()
                            .header("X-User-Id", userId)
                            .header("X-Family-Id", familyId != null ? familyId : "")
                            .build();
                    return chain.filter(finalExchange.mutate().request(mutated).build());
                });
    }

    private Claims parseToken(String token) {
        SecretKey key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private Mono<Void> unauthorized(ServerHttpResponse response) {
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        String body = "{\"code\":\"UNAUTHORIZED\",\"message\":\"未登录或登录已过期\"}";
        DataBuffer buffer = response.bufferFactory().wrap(body.getBytes(StandardCharsets.UTF_8));
        return response.writeWith(Mono.just(buffer));
    }

    @Override
    public int getOrder() {
        return -100;
    }
}
