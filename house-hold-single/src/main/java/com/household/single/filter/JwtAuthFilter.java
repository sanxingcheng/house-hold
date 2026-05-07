package com.household.single.filter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Servlet-based JWT authentication filter.
 * Validates Bearer token signature and expiration (no Redis required).
 * Injects X-User-Id / X-Family-Id headers from token claims.
 */
@Component
@Order(-100)
public class JwtAuthFilter implements Filter {

    @Value("${app.jwt.secret:house-hold-jwt-secret-key-change-in-production-min-256-bits}")
    private String secret;

    private static final List<String> SKIP_PATHS = List.of(
            "/auth/login",
            "/auth/logout",
            "/auth/register",
            "/auth/password-public-key",
            "/auth/health"
    );

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse,
                         FilterChain chain) throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;

        // CORS preflight
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            chain.doFilter(request, response);
            return;
        }

        String path = request.getRequestURI();
        // Skip paths that don't need authentication
        if (SKIP_PATHS.contains(path)) {
            chain.doFilter(request, response);
            return;
        }
        // Only protect API paths
        if (!path.startsWith("/auth/") && !path.startsWith("/user/")
                && !path.startsWith("/family/") && !path.startsWith("/wealth/")) {
            chain.doFilter(request, response);
            return;
        }

        String auth = request.getHeader("Authorization");
        if (auth == null || !auth.startsWith("Bearer ")) {
            writeUnauthorized(response);
            return;
        }

        String token = auth.substring(7).trim();
        Claims claims;
        try {
            claims = parseToken(token);
        } catch (Exception e) {
            writeUnauthorized(response);
            return;
        }

        String userId = claims.getSubject();
        String familyId = claims.get("familyId", String.class);

        if (userId == null || userId.isEmpty()) {
            writeUnauthorized(response);
            return;
        }

        // Wrap request with custom headers
        HeaderMapRequestWrapper wrapper = new HeaderMapRequestWrapper(request);
        wrapper.addHeader("X-User-Id", userId);
        if (familyId != null) {
            wrapper.addHeader("X-Family-Id", familyId);
        }

        chain.doFilter(wrapper, response);
    }

    private Claims parseToken(String token) {
        SecretKey key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private void writeUnauthorized(HttpServletResponse response) throws IOException {
        response.setStatus(401);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"code\":\"UNAUTHORIZED\",\"message\":\"未登录或登录已过期\"}");
    }

    private static class HeaderMapRequestWrapper extends jakarta.servlet.http.HttpServletRequestWrapper {
        private final java.util.Map<String, String> customHeaders = new java.util.HashMap<>();

        public HeaderMapRequestWrapper(HttpServletRequest request) {
            super(request);
        }

        public void addHeader(String name, String value) {
            customHeaders.put(name, value);
        }

        @Override
        public String getHeader(String name) {
            String val = customHeaders.get(name);
            return val != null ? val : super.getHeader(name);
        }

        @Override
        public java.util.Enumeration<String> getHeaderNames() {
            java.util.Set<String> set = new java.util.HashSet<>();
            java.util.Enumeration<String> e = super.getHeaderNames();
            while (e.hasMoreElements()) set.add(e.nextElement());
            set.addAll(customHeaders.keySet());
            return java.util.Collections.enumeration(set);
        }

        @Override
        public java.util.Enumeration<String> getHeaders(String name) {
            if (customHeaders.containsKey(name)) {
                return java.util.Collections.enumeration(java.util.List.of(customHeaders.get(name)));
            }
            return super.getHeaders(name);
        }
    }
}
