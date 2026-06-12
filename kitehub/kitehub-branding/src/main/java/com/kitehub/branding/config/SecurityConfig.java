package com.kitehub.branding.config;

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

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Security configuration for the kitehub-branding service (GAP-562/562b Wave 101 Bucket B
 * close-out — pairs with `RbacAccessDeniedHandler` in kitehub-subscription to enforce
 * Customer-vs-Staff role separation on branding write endpoints).
 *
 * <p>Method-level authorization via {@link EnableMethodSecurity} so write endpoints
 * (regenerate banner, generate theme, create job, delete job) annotated with
 * {@code @PreAuthorize("hasAnyRole('OWNER','PLATFORM_ADMIN','ADMIN')")} resolve correctly.
 * Mirrors the kitehub-subscription pattern (see {@code com.kitehub.subscription.config.SecurityConfig}).
 *
 * <p>Authentication source: gateway-forwarded {@code X-User-Id} + {@code X-User-Roles} headers
 * (mirrors the kiteclass-core + kitehub-subscription trust pattern). The
 * {@link XUserRolesHeaderFilter} translates these into a Spring authentication
 * token carrying {@code ROLE_<role>} authorities so {@code hasAnyRole('OWNER',...)}
 * resolves correctly.
 *
 * <p>Default-deny (per GAP-552 SecurityConfig migration): every endpoint not explicitly
 * carved out as public requires authenticated principal. Method-level {@code @PreAuthorize}
 * adds per-role enforcement on top.
 *
 * @since Wave 101 Bucket B — closes GAP-562/562b @PreAuthorize deferred-Wave-80 portion
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    /**
     * Permissive filter chain in {@code test} profile so existing Spring Boot integration
     * tests don't regress. Method-level {@code @PreAuthorize} still applies wherever
     * annotated; tests use {@code @WithMockUser} from {@code spring-security-test}.
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
            com.kitehub.branding.wizard.sse.SseTokenService sseTokenService) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // ── Public surface — actuator + openapi only ──
                        .requestMatchers("/actuator/**").permitAll()
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                        // ── Authenticated surface — role enforced per-method via @PreAuthorize ──
                        // SSE deploy-stream/preview authenticate via ?access_token query param
                        // (GAP-1021) handled by SseQueryTokenAuthFilter below, then authenticated().
                        .requestMatchers("/api/platform/branding/**").authenticated()
                        .requestMatchers("/api/v1/branding/**").authenticated()
                        // ── Default-deny tail ──
                        .anyRequest().authenticated()
                )
                .exceptionHandling(eh -> eh
                        .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED))
                )
                // GAP-1021 — SSE EventSource auth via short-lived ?access_token query param
                // (browsers can't set X-User-* headers). Runs before the header filter so a
                // valid token establishes auth for the stream; absent/invalid token = default-deny.
                .addFilterBefore(
                        new com.kitehub.branding.wizard.sse.SseQueryTokenAuthFilter(sseTokenService),
                        UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(new XUserRolesHeaderFilter(),
                        UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * Translates gateway-forwarded {@code X-User-Id} + {@code X-User-Roles} headers into
     * a Spring authentication token with {@code ROLE_<role>} authorities (mirrors
     * kitehub-subscription pattern).
     */
    static class XUserRolesHeaderFilter extends org.springframework.web.filter.OncePerRequestFilter {

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

        // Reactive endpoints (Mono/Flux return) trigger a servlet ASYNC dispatch, and
        // any error triggers an ERROR dispatch. OncePerRequestFilter skips both by default,
        // so the header-derived auth was lost on re-dispatch → 401 (masking the real result).
        // Re-run on async + error dispatch so auth is re-established from the headers.
        @Override
        protected boolean shouldNotFilterAsyncDispatch() {
            return false;
        }

        @Override
        protected boolean shouldNotFilterErrorDispatch() {
            return false;
        }
    }
}
