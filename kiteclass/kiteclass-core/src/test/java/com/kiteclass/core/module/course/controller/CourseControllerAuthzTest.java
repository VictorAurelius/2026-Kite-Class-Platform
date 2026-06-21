package com.kiteclass.core.module.course.controller;

import com.kiteclass.core.module.course.service.CourseService;
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
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Web-slice authorization tests for {@link CourseController} (GAP-1491, OWASP A01).
 *
 * <p>Course mutations (create/update/delete/publish/archive/prerequisites) were unguarded
 * — a low-privilege role could publish or delete catalog courses. The fix guards mutations
 * at {@code hasAnyRole('TEACHER','ADMIN','OWNER','PLATFORM_ADMIN','STAFF')} while the catalog
 * READ endpoints stay {@code permitAll()} (intentionally public — anonymous catalog browse
 * + sitemap, see {@code kiteclass-frontend/src/lib/api/public.ts}).
 */
@WebMvcTest(CourseController.class)
@AutoConfigureMockMvc
@Import({CourseControllerAuthzTest.TestSecurityConfig.class, CourseControllerAuthzTest.MockConfig.class})
@ActiveProfiles("test")
@DisplayName("CourseController @PreAuthorize role gate (GAP-1491, OWASP A01)")
class CourseControllerAuthzTest {

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
        CourseService courseService() {
            return Mockito.mock(CourseService.class);
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CourseService courseService;

    private static final String TENANT_HEADER = UUID.randomUUID().toString();
    private static final Long COURSE_ID = 3L;

    @BeforeEach
    void resetMocks() {
        Mockito.reset(courseService);
    }

    // ----- Public catalog read stays open (permitAll) -----

    @Test
    @DisplayName("Anonymous → 200 on GET /courses/{id} (public catalog read, permitAll)")
    void getById_anonymous_allowed() throws Exception {
        mockMvc.perform(get("/api/v1/courses/{id}", COURSE_ID).header("X-Tenant-Id", TENANT_HEADER))
                .andExpect(status().isOk());
        verify(courseService).getCourseById(COURSE_ID);
    }

    // ----- Mutation allow for staff/teacher -----

    @Test
    @DisplayName("OWNER → 204 on DELETE /courses/{id} (mutation tier, service invoked)")
    @WithMockUser(roles = "OWNER")
    void delete_owner_allowed() throws Exception {
        mockMvc.perform(delete("/api/v1/courses/{id}", COURSE_ID).header("X-Tenant-Id", TENANT_HEADER))
                .andExpect(status().isNoContent());
        verify(courseService).deleteCourse(COURSE_ID);
    }

    // ----- Low-privilege deny on mutations (OWASP A01) -----

    @Test
    @DisplayName("OWASP A01: STUDENT → denied DELETE /courses/{id} (service NOT invoked)")
    @WithMockUser(roles = "STUDENT")
    void delete_student_denied() throws Exception {
        mockMvc.perform(delete("/api/v1/courses/{id}", COURSE_ID).header("X-Tenant-Id", TENANT_HEADER))
                .andExpect(result -> assertDenied(result.getResponse().getStatus(), "STUDENT delete"));
        verifyNoInteractions(courseService);
    }

    @Test
    @DisplayName("OWASP A01: PARENT → denied POST /courses/{id}/publish (service NOT invoked)")
    @WithMockUser(roles = "PARENT")
    void publish_parent_denied() throws Exception {
        mockMvc.perform(post("/api/v1/courses/{id}/publish", COURSE_ID).header("X-Tenant-Id", TENANT_HEADER))
                .andExpect(result -> assertDenied(result.getResponse().getStatus(), "PARENT publish"));
        verifyNoInteractions(courseService);
    }

    private static void assertDenied(int statusCode, String label) {
        if (statusCode >= 200 && statusCode < 300) {
            throw new AssertionError(label + " must be denied by @PreAuthorize, got " + statusCode);
        }
    }
}
