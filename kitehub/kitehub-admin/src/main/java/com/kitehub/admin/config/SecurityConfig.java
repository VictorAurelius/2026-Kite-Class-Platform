package com.kitehub.admin.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Security configuration for the admin service.
 *
 * <p>Method-level authorization via {@link EnableMethodSecurity} so admin endpoints
 * (e.g. {@link com.kitehub.admin.controller.AdminInstancesController},
 * {@link com.kitehub.admin.controller.AdminPaymentsController},
 * {@link com.kitehub.admin.controller.AdminRevenueController})
 * can use {@code @PreAuthorize("hasRole('PLATFORM_ADMIN')")} directly —
 * closes GAP-637 (admin v1 controllers missing class-level authorization,
 * OWASP A01 Broken Access Control finding from Wave 92 audit).</p>
 *
 * <p>Bean name is {@code adminSecurityConfig} to avoid conflict with
 * {@code kitehub-subscription}'s {@code SecurityConfig} (same dependency classpath).
 * Both coexist in the full application context when kitehub-admin depends on
 * kitehub-subscription.</p>
 *
 * <p>Authentication source: gateway-forwarded {@code X-User-Id} + {@code X-User-Roles} headers
 * (mirrors the kitehub-subscription trust pattern). The {@link XUserRolesHeaderFilter}
 * translates these headers into a Spring {@link UsernamePasswordAuthenticationToken} carrying
 * {@code ROLE_<role>} authorities, so {@code hasRole('PLATFORM_ADMIN')} resolves correctly.</p>
 *
 * @since Wave 97 Bucket A — GAP-637
 */
@Configuration("adminSecurityConfig")
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Bean("adminSecurityFilterChain")
    @Profile("!test")
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // Scope this filter chain to admin paths only — prevents catch-all conflict
                // with kitehub-subscription's SecurityFilterChain when both are on classpath.
                .securityMatcher("/api/v1/admin/**", "/api/platform/admin/**",
                        "/actuator/**", "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html")
                .csrf(csrf -> csrf.disable())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Actuator + OpenAPI — open for internal monitoring
                        .requestMatchers("/actuator/**").permitAll()
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                        // All admin v1 paths — require authentication; role enforced per-method via @PreAuthorize
                        .requestMatchers("/api/v1/admin/**").authenticated()
                        // Legacy admin paths — require authentication
                        .requestMatchers("/api/platform/admin/**").authenticated()
                )
                .exceptionHandling(eh ->
                        eh.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
                .addFilterBefore(new XUserRolesHeaderFilter(),
                        UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * Translates gateway-forwarded {@code X-User-Id} + {@code X-User-Roles} headers into
     * a Spring {@link UsernamePasswordAuthenticationToken} with {@code ROLE_<role>}
     * authorities. Roles are split on comma; each role is prefixed with {@code ROLE_}
     * unless already prefixed.
     */
    static class XUserRolesHeaderFilter extends OncePerRequestFilter {

        @Override
        protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                        FilterChain chain) throws ServletException, IOException {
            String userId = request.getHeader("X-User-Id");
            String rolesHeader = request.getHeader("X-User-Roles");

            if (userId != null && !userId.isBlank() && rolesHeader != null && !rolesHeader.isBlank()) {
                List<GrantedAuthority> authorities = Arrays.stream(rolesHeader.split(","))
                        .map(String::trim)
                        .filter(r -> !r.isEmpty())
                        .map(r -> r.startsWith("ROLE_") ? r : "ROLE_" + r)
                        .map(SimpleGrantedAuthority::new)
                        .collect(Collectors.toList());

                UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                        userId, null, authorities);
                SecurityContextHolder.getContext().setAuthentication(auth);
            }

            chain.doFilter(request, response);
        }
    }
}
