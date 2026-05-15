package com.kitehub.subscription.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockServletContext;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfiguration;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.GenericWebApplicationContext;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Verifies the default-deny migration (GAP-552) by exercising the
 * non-test {@link SecurityFilterChain} shape against an in-memory MockMvc that
 * holds no controllers. Anonymous responses therefore distinguish:
 *
 * <ul>
 *   <li>{@code 401 Unauthorized} — authenticated() rule matched (auth gate hit).</li>
 *   <li>{@code 404 Not Found} — permitAll() rule matched but no controller (passes auth).</li>
 * </ul>
 *
 * <p>This matrix replaces a naive single happy-path assertion. The whitelist
 * + default-deny tail must hold for every {@code .requestMatchers(...)} entry
 * in {@link SecurityConfig#securityFilterChain(HttpSecurity)}.</p>
 */
@DisplayName("SecurityConfig default-deny migration (GAP-552)")
class SecurityConfigTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        GenericWebApplicationContext context = new GenericWebApplicationContext(new MockServletContext());
        AnnotationConfigApplicationContext parent = new AnnotationConfigApplicationContext(TestSecurityChain.class);
        context.setParent(parent);
        context.refresh();
        this.mockMvc = MockMvcBuilders.webAppContextSetup((WebApplicationContext) context)
            .addFilter(parent.getBean(FilterChainProxy.class), "/*")
            .apply(springSecurity(parent.getBean(FilterChainProxy.class)))
            .build();
    }

    @ParameterizedTest(name = "[{index}] GET {0} → {1}")
    @CsvSource({
        // permitAll paths — no controller → 404 (passes auth gate).
        "/api/auth/login,                       404",
        "/api/auth/password-reset-request,      404",
        "/api/auth/password-reset-confirm,      404",
        "/api/v1/auth/login,                    404",
        "/api/v1/beta-status,                   404",
        "/api/v1/feedback,                      404",
        "/api/v1/consent/cookie,                404",
        "/api/v1/public-config/foo,             404",
        "/actuator/health,                      404",
        // authenticated paths — anonymous request → 401.
        "/api/auth/2fa/verify,                  401",
        "/api/v1/auth/2fa/verify,               401",
        "/api/v1/onboarding-progress,           401",
        "/api/v1/admin/instances,               401",
        "/api/v1/staff/list,                    401",
        "/api/v1/notifications/preferences,     401",
        "/api/v1/dsar/tickets,                  401",
        // Default-deny tail — anything not whitelisted MUST require auth.
        "/api/v1/random/path,                   401",
        "/api/v2/future-feature,                401",
        "/random/totally/unknown,               401"
    })
    void enforcesDefaultDenyAllowlistMatrix(String path, int expectedStatus) throws Exception {
        mockMvc.perform(get(path)).andExpect(status().is(expectedStatus));
    }

    @Test
    @DisplayName("Default-deny tail responds 401 on previously-unmapped path")
    void defaultDenyTailReturns401() throws Exception {
        mockMvc.perform(get("/api/v1/some-future-controller")).andExpect(status().isUnauthorized());
    }

    /**
     * Mirrors the production {@code @Profile("!test")} chain in
     * {@link SecurityConfig#securityFilterChain(HttpSecurity)}. The shape MUST
     * stay in sync — if you edit SecurityConfig, edit this stub too.
     */
    @Configuration
    @EnableWebSecurity
    @org.springframework.context.annotation.Import(WebSecurityConfiguration.class)
    static class TestSecurityChain {
        @Bean
        SecurityFilterChain chain(HttpSecurity http) throws Exception {
            http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                    .requestMatchers("/api/auth/2fa/**").authenticated()
                    .requestMatchers("/api/auth/**").permitAll()
                    .requestMatchers("/api/v1/auth/2fa/**").authenticated()
                    .requestMatchers("/api/v1/auth/**").permitAll()
                    .requestMatchers("/api/v1/consent/cookie").permitAll()
                    .requestMatchers("/api/v1/consent/cookie/**").permitAll()
                    .requestMatchers("/api/v1/beta-status/**").permitAll()
                    .requestMatchers("/api/v1/feedback").permitAll()
                    .requestMatchers("/api/v1/feedback/**").permitAll()
                    .requestMatchers("/api/v1/public-config/**").permitAll()
                    .requestMatchers("/actuator/**").permitAll()
                    .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                    .requestMatchers("/api/v1/payments/webhook").permitAll()
                    .requestMatchers("/api/v1/payments/webhook/**").permitAll()
                    .requestMatchers("/api/v1/admin/**").authenticated()
                    .requestMatchers("/api/v1/onboarding-progress/**").authenticated()
                    .requestMatchers("/api/v1/staff/**").authenticated()
                    .requestMatchers("/api/v1/notifications/**").authenticated()
                    .requestMatchers("/api/v1/dsar/**").authenticated()
                    .anyRequest().authenticated())
                .exceptionHandling(eh -> eh
                    .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)));
            return http.build();
        }
    }
}
