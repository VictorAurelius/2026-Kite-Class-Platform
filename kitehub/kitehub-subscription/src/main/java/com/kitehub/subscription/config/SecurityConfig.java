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
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.web.filter.OncePerRequestFilter;

import com.kitehub.subscription.auth.twofactor.ChallengeTokenAuthenticationFilter;
import com.kitehub.subscription.auth.twofactor.ChallengeTokenService;

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
 * request-beta-access) and the small anonymous surface listed in the chain stay
 * anonymous; everything else (including 2FA endpoints under {@code /api/auth/2fa/**}
 * and {@code /api/v1/auth/2fa/**}, all admin / staff / onboarding paths, and any
 * future controller that doesn't explicitly opt in via the allowlist) defaults to
 * {@code authenticated()} — the {@code anyRequest().authenticated()} default-deny
 * tail closes GAP-552 (was previously {@code permitAll()} which silently let new
 * controllers ship public). Method-level {@code @PreAuthorize} still enforces
 * per-role checks for admin paths.</p>
 *
 * @since Wave 35 — GAP-384; default-deny migration Wave 79 — GAP-552
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
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            ObjectProvider<AccessDeniedHandler> accessDeniedHandlerProvider,
            ObjectProvider<ChallengeTokenService> challengeTokenServiceProvider) throws Exception {
        // GAP-562b (Wave 80 Bucket C): if a RbacAccessDeniedHandler bean is present
        // (production profile), wire it so privilege-escalation attempts (STAFF →
        // OWNER endpoints) write an admin_audit_log row before returning 403.
        AccessDeniedHandler accessDeniedHandler = accessDeniedHandlerProvider.getIfAvailable();

        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // ── Public surface — explicit allowlist (GAP-552 default-deny migration) ──
                        // Legacy /api/auth/** (register, login, refresh, verify-email,
                        // resend-verification, password-reset-*, change-password, profile).
                        // 2FA endpoints under /api/auth/2fa/** carved out below as authenticated.
                        // 2FA paths require ROLE_CHALLENGE (mid-2FA bearer) OR a fully
                        // authenticated regular session (e.g. re-enrol from settings).
                        // GAP-706: ChallengeTokenAuthenticationFilter bridges the
                        // HS256 Bearer challenge token into Spring Authentication
                        // with ROLE_CHALLENGE before this rule evaluates.
                        .requestMatchers("/api/auth/2fa/**").hasAnyRole("CHALLENGE",
                                "PLATFORM_ADMIN", "TENANT_OWNER", "TENANT_STAFF", "TENANT_USER")
                        .requestMatchers("/api/auth/**").permitAll()
                        // Versioned auth surface (per versioning-policy.md migration target).
                        .requestMatchers("/api/v1/auth/2fa/**").hasAnyRole("CHALLENGE",
                                "PLATFORM_ADMIN", "TENANT_OWNER", "TENANT_STAFF", "TENANT_USER")
                        .requestMatchers("/api/v1/auth/**").permitAll()
                        // PDPL cookie consent (Wave 79 Bucket B GAP-558 — anonymous OK).
                        .requestMatchers("/api/v1/consent/cookie").permitAll()
                        .requestMatchers("/api/v1/consent/cookie/**").permitAll()
                        // Beta status + feedback (Wave 78 GAP-540/542) — anonymous OK.
                        .requestMatchers("/api/v1/beta-status/**").permitAll()
                        .requestMatchers("/api/v1/feedback").permitAll()
                        .requestMatchers("/api/v1/feedback/**").permitAll()
                        // Public config / actuator / openapi surface.
                        .requestMatchers("/api/v1/public-config/**").permitAll()
                        .requestMatchers("/actuator/**").permitAll()
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                        // Payment webhook (Stripe-style signature validation in controller).
                        .requestMatchers("/api/v1/payments/webhook").permitAll()
                        .requestMatchers("/api/v1/payments/webhook/**").permitAll()
                        // Staff invitation public recipient endpoints (Bug #19 — Wave A
                        // Bucket B walk 2026-05-28): controller marks "Recipient accepts
                        // invitation + sets password (public)" but anyRequest()
                        // .authenticated() catch-all denied — recipient has no JWT yet,
                        // token in URL is the credential. Owner-side POST + list +
                        // resend + revoke still authenticated (default-deny).
                        .requestMatchers("/api/v1/staff-invitations/by-token/**").permitAll()
                        .requestMatchers(org.springframework.http.HttpMethod.POST,
                                "/api/v1/staff-invitations/*/accept").permitAll()
                        // ── Authenticated surface — role enforced per-method via @PreAuthorize ──
                        .requestMatchers("/api/v1/admin/**").authenticated()
                        .requestMatchers("/api/v1/onboarding-progress/**").authenticated()
                        .requestMatchers("/api/v1/staff/**").authenticated()
                        .requestMatchers("/api/v1/notifications/**").authenticated()
                        .requestMatchers("/api/v1/dsar/**").authenticated()
                        // ── Default-deny everything else (GAP-552 / OWASP A05 + A01) ──
                        .anyRequest().authenticated()
                )
                // Anonymous access to authenticated endpoints → 401 (not the default 403)
                // GAP-562b: authenticated-but-insufficient-role → 403 via RbacAccessDeniedHandler
                // which also writes an admin_audit_log row for forensics.
                .exceptionHandling(eh -> {
                    eh.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED));
                    if (accessDeniedHandler != null) {
                        eh.accessDeniedHandler(accessDeniedHandler);
                    }
                })
                .addFilterBefore(new XUserRolesHeaderFilter(),
                        UsernamePasswordAuthenticationFilter.class);

        // GAP-706: bridge HS256 challenge tokens BEFORE UsernamePasswordAuthenticationFilter
        // (ordering parallels XUserRolesHeaderFilter — Spring runs filters in registration
        // order within the same insertion slot, so the first addFilterBefore call places
        // the challenge filter ahead of XUserRolesHeaderFilter). Active on the explicit
        // /api/v1/auth/2fa/** + /api/auth/2fa/** allowlist; bypass elsewhere.
        ChallengeTokenService challengeTokenService = challengeTokenServiceProvider.getIfAvailable();
        if (challengeTokenService != null) {
            http.addFilterBefore(new ChallengeTokenAuthenticationFilter(challengeTokenService),
                    UsernamePasswordAuthenticationFilter.class);
        }

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
