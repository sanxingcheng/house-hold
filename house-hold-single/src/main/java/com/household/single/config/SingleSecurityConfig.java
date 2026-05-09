package com.household.single.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Unified security configuration for the monolithic JAR.
 * Merges rules from auth-user and wealth.
 * JWT authentication is handled by {@link com.household.single.filter.JwtAuthFilter}
 * which runs as a servlet filter (registered via @Component).
 */
@Configuration
@EnableWebSecurity
public class SingleSecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(a -> a
                        .requestMatchers(
                                "/auth/**",
                                "/user/**",
                                "/family/**",
                                "/wealth/**",
                                "/actuator/health",
                                "/actuator/info",
                                "/error",
                                "/favicon.ico",
                                "/js/**",
                                "/css/**",
                                "/fonts/**",
                                "/img/**",
                                "/assets/**"
                        ).permitAll()
                        .anyRequest().permitAll()
                );
        return http.build();
    }
}
