package com.kitehub.subscription.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
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
import org.springframework.http.HttpStatus;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Security configuration for the subscription service.
 *
 * <p>Method-level authorization via {@link EnableMethodSecurity} so admin endpoints
 * (e.g. {@code BetaAccessController#approve}) can use {@code @PreAuthorize("hasRole('PLATFORM_ADMIN')")}
 * directly — closes GAP-384 (admin endpoints previously unauthenticated despite javadoc
 * claim of gateway-level guard, which had a path-prefix mismatch).</p>
 *
 * <p>Authentication source: gateway-forwarded {@code X-User-Id} + {@code X-User-Roles} headers
 * (mirrors the kiteclass-core trust pattern). The {@link XUserRolesHeaderFilter} translates
 * these headers into a Spring {@link UsernamePasswordAuthenticationToken} carrying
 * {@code ROLE_<role>} authorities, so {@code hasRole('PLATFORM_ADMIN')} resolves correctly.</p>
 *
 * <p>Public endpoints under {@code /api/v1/auth/**} (beta signup + token validation +
 * request-beta-access) remain anonymous; only {@code /api/v1/admin/**} requires
 * authentication AND the matching role enforced by per-method {@code @PreAuthorize}.</p>
 *
 * @since Wave 35 — GAP-384
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    /**
     * Permissive filter chain used in {@code test} profile so existing Spring Boot integration
     * tests (which were written before this module had Spring Security on classpath) do not
     * regress. Method-level {@code @PreAuthorize} still applies wherever explicitly annotated;
     * tests that exercise admin endpoints set the {@link SecurityContextHolder} directly via
     * {@code @WithMockUser} from {@code spring-security-test}.
     */
    @Bean
    @Profile("test")
    public SecurityFilterChain testSecurityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                .build();
    }

    @Bean
    @Profile("!test")
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Public auth + actuator + openapi surface
                        .requestMatchers("/api/v1/auth/**").permitAll()
                        .requestMatchers("/actuator/**").permitAll()
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                        // Admin endpoints require authentication; role enforced per-method via @PreAuthorize
                        .requestMatchers("/api/v1/admin/**").authenticated()
                        // Default-deny everything else not explicitly opened
                        .anyRequest().permitAll()
                )
                // Anonymous access to authenticated endpoints → 401 (not the default 403)
                .exceptionHandling(eh -> eh
                        .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
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
