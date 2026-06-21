package com.kiteclass.core.module.instance.controller;

import com.kiteclass.core.module.instance.entity.FrontendInstance;
import com.kiteclass.core.module.instance.repository.FrontendInstanceRepository;
import com.kiteclass.core.module.instance.service.InstanceLifecycleService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Web-slice authorization tests for {@link InstanceController} (GAP-1491, OWASP A01).
 *
 * <p>{@code FrontendInstance} is a platform-level provisioning table; every lifecycle
 * endpoint was unguarded. The fix restricts the whole controller to
 * {@code hasAnyRole('PLATFORM_ADMIN','ADMIN','OWNER')} (class-level), with the cross-tenant
 * {@code list()} tightened to {@code hasAnyRole('PLATFORM_ADMIN','ADMIN')} (no OWNER).
 */
@WebMvcTest(InstanceController.class)
@AutoConfigureMockMvc
@Import({InstanceControllerAuthzTest.TestSecurityConfig.class, InstanceControllerAuthzTest.MockConfig.class})
@ActiveProfiles("test")
@DisplayName("InstanceController @PreAuthorize role gate (GAP-1491, OWASP A01)")
class InstanceControllerAuthzTest {

    @TestConfiguration
    @EnableMethodSecurity(prePostEnabled = true)
    static class TestSecurityConfig {
        @Bean
        SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
            http.csrf(AbstractHttpConfigurer::disable)
                    .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
            return http.build();
        }
    }

    @TestConfiguration
    static class MockConfig {
        @Bean
        @Primary
        InstanceLifecycleService instanceLifecycleService() {
            return Mockito.mock(InstanceLifecycleService.class);
        }

        @Bean
        @Primary
        FrontendInstanceRepository frontendInstanceRepository() {
            return Mockito.mock(FrontendInstanceRepository.class);
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private InstanceLifecycleService lifecycle;

    @Autowired
    private FrontendInstanceRepository repository;

    private static final Long INSTANCE_ID = 1L;

    @BeforeEach
    void resetMocks() {
        Mockito.reset(lifecycle, repository);
    }

    @Test
    @DisplayName("PLATFORM_ADMIN → 200 on GET /instances (list, platform-admin tier)")
    @WithMockUser(roles = "PLATFORM_ADMIN")
    void list_platformAdmin_allowed() throws Exception {
        when(repository.findAll(any(Pageable.class))).thenReturn(Page.<FrontendInstance>empty());
        mockMvc.perform(get("/api/v1/instances"))
                .andExpect(status().isOk());
        verify(repository).findAll(any(Pageable.class));
    }

    @Test
    @DisplayName("OWASP A01: OWNER → denied GET /instances (list is cross-tenant, no OWNER)")
    @WithMockUser(roles = "OWNER")
    void list_owner_denied() throws Exception {
        mockMvc.perform(get("/api/v1/instances"))
                .andExpect(result -> assertDenied(result.getResponse().getStatus(), "OWNER list"));
        verifyNoInteractions(repository);
    }

    @Test
    @DisplayName("OWASP A01: STUDENT → denied POST /instances/{id}/retry (lifecycle, service NOT invoked)")
    @WithMockUser(roles = "STUDENT")
    void retry_student_denied() throws Exception {
        mockMvc.perform(post("/api/v1/instances/{id}/retry", INSTANCE_ID))
                .andExpect(result -> assertDenied(result.getResponse().getStatus(), "STUDENT retry"));
        verifyNoInteractions(lifecycle);
    }

    private static void assertDenied(int statusCode, String label) {
        if (statusCode >= 200 && statusCode < 300) {
            throw new AssertionError(label + " must be denied by @PreAuthorize, got " + statusCode);
        }
    }
}
