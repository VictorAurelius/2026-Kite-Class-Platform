package com.kiteclass.core.module.lms;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kiteclass.core.common.constant.CourseStatus;
import com.kiteclass.core.common.context.TenantContext;
import com.kiteclass.core.config.TestContainersConfiguration;
import com.kiteclass.core.config.TestSecurityConfig;
import com.kiteclass.core.config.TestTenantContextFilter;
import com.kiteclass.core.module.course.entity.Course;
import com.kiteclass.core.module.course.repository.CourseRepository;
import com.kiteclass.core.module.lms.dto.request.CreateCourseModuleRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * GAP-1299 — LMS authoring privilege-escalation closure (BE authz).
 *
 * <p>The G1 runtime walk (appended to GAP-798) proved a STUDENT token plus a client-supplied
 * {@code X-Teacher-Id: <teacherId>} header could create LMS content AS that teacher
 * (privilege escalation + impersonation), because the authoring endpoints had (1) no
 * {@code @PreAuthorize} role gate and (2) trusted the spoofable {@code X-Teacher-Id} header
 * for the actor identity.
 *
 * <p>This IT enforces both fix layers end-to-end through real method security + a real
 * JPA-backed course:
 * <ol>
 *   <li><strong>Role gate</strong> — STUDENT/PARENT are blocked entirely (403).</li>
 *   <li><strong>Identity from token</strong> — the acting teacher is derived from the
 *       gateway-injected {@code X-User-Reference-Id} ({@code UserContext}), NOT the
 *       client {@code X-Teacher-Id} header. Teacher A cannot impersonate Teacher B by
 *       setting {@code X-Teacher-Id: B} — the spoof is ignored and A (the non-owner) is denied.</li>
 *   <li><strong>Happy path preserved</strong> — the course-owning teacher gets 201.</li>
 *   <li><strong>ADMIN/OWNER bypass</strong> — tenant-admins (no numeric reference id) get 201.</li>
 * </ol>
 *
 * <p>Method security is OFF in {@link TestSecurityConfig}; the inner
 * {@link MethodSecurityEnablerConfig} activates {@code @PreAuthorize} so the role gate is
 * enforced for real (same trick as {@code StaffRolePreAuthorizeIT} / {@code CrossUserAuthzTest}).
 * The real {@code TenantFilterInterceptor} (registered via {@code WebMvcConfig}, not test-profile
 * gated) reads {@code X-User-Reference-Id} into {@code UserContext} during request dispatch.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import({
        TestContainersConfiguration.class,
        TestSecurityConfig.class,
        TestTenantContextFilter.class,
        LmsAuthoringAuthzTest.MethodSecurityEnablerConfig.class
})
@ContextConfiguration(initializers = TestContainersConfiguration.Initializer.class)
@Transactional
@DisplayName("GAP-1299 — LMS authoring authz (role gate + X-Teacher-Id spoof closure)")
class LmsAuthoringAuthzTest {

    /**
     * Activates {@code @PreAuthorize} evaluation — {@link TestSecurityConfig} enables
     * {@code @EnableWebSecurity} but NOT {@code @EnableMethodSecurity}, so without this the
     * guards are silently ignored.
     */
    @TestConfiguration
    @EnableMethodSecurity(prePostEnabled = true)
    static class MethodSecurityEnablerConfig {
        // annotation alone activates the AOP advisor
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CourseRepository courseRepository;

    private final UUID tenantId = UUID.randomUUID();
    /** The teacher who owns the seeded course (teachers.id == X-User-Reference-Id). */
    private final Long ownerTeacherId = 100L;
    /** A different authenticated teacher who is NOT the course owner. */
    private final Long otherTeacherId = 999L;

    private Course course;

    @BeforeEach
    void setUp() {
        TenantContext.setCurrentTenant(tenantId);
        course = Course.builder()
                .name("Authz Course")
                .code("AUTHZ-LMS-001")
                .teacherId(ownerTeacherId)
                .status(CourseStatus.PUBLISHED)
                .price(new BigDecimal("1000.00"))
                .build();
        course.setInstanceId(tenantId);
        course = courseRepository.save(course);
    }

    private String moduleBody() throws Exception {
        return objectMapper.writeValueAsString(
                new CreateCourseModuleRequest("Module 1", "Description", 1));
    }

    // ── Layer 1: role gate — STUDENT / PARENT blocked entirely ──────────────

    @Test
    @DisplayName("STUDENT cannot create a module — 403 (was 201 PoC privilege escalation)")
    @WithMockUser(roles = "STUDENT")
    void student_cannotCreateModule_evenWithSpoofedTeacherHeader() throws Exception {
        mockMvc.perform(post("/api/v1/lms/courses/{courseId}/modules", course.getId())
                        .header("X-Tenant-Id", tenantId.toString())
                        // The exact PoC: spoofed client header claiming to be the course owner.
                        .header("X-Teacher-Id", ownerTeacherId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(moduleBody()))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("PARENT cannot create a module — 403")
    @WithMockUser(roles = "PARENT")
    void parent_cannotCreateModule() throws Exception {
        mockMvc.perform(post("/api/v1/lms/courses/{courseId}/modules", course.getId())
                        .header("X-Tenant-Id", tenantId.toString())
                        .header("X-Teacher-Id", ownerTeacherId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(moduleBody()))
                .andExpect(status().isForbidden());
    }

    // ── Layer 2: identity from token — X-Teacher-Id spoof ignored ───────────

    @Test
    @DisplayName("Teacher A + spoofed X-Teacher-Id=owner cannot impersonate owner — 403")
    @WithMockUser(roles = "TEACHER")
    void teacher_cannotImpersonateOtherTeacherViaHeader() throws Exception {
        // Authenticated principal = otherTeacherId (gateway-injected X-User-Reference-Id),
        // but the request spoofs X-Teacher-Id = ownerTeacherId. The controller ignores the
        // spoofed header and uses the principal → otherTeacherId is NOT the owner → 403.
        mockMvc.perform(post("/api/v1/lms/courses/{courseId}/modules", course.getId())
                        .header("X-Tenant-Id", tenantId.toString())
                        .header("X-User-Reference-Id", otherTeacherId.toString())
                        .header("X-Teacher-Id", ownerTeacherId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(moduleBody()))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Course-owning teacher creates a module — 201 (happy path preserved)")
    @WithMockUser(roles = "TEACHER")
    void owningTeacher_canCreateModule() throws Exception {
        mockMvc.perform(post("/api/v1/lms/courses/{courseId}/modules", course.getId())
                        .header("X-Tenant-Id", tenantId.toString())
                        .header("X-User-Reference-Id", ownerTeacherId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(moduleBody()))
                .andExpect(status().isCreated());
    }

    // ── Layer 2: ADMIN / OWNER bypass per-course ownership ──────────────────

    @Test
    @DisplayName("OWNER creates a module without numeric reference id — 201 (admin bypass)")
    @WithMockUser(roles = "OWNER")
    void owner_canCreateModule_viaAdminBypass() throws Exception {
        mockMvc.perform(post("/api/v1/lms/courses/{courseId}/modules", course.getId())
                        .header("X-Tenant-Id", tenantId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(moduleBody()))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("ADMIN creates a module without numeric reference id — 201 (admin bypass)")
    @WithMockUser(roles = "ADMIN")
    void admin_canCreateModule_viaAdminBypass() throws Exception {
        mockMvc.perform(post("/api/v1/lms/courses/{courseId}/modules", course.getId())
                        .header("X-Tenant-Id", tenantId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(moduleBody()))
                .andExpect(status().isCreated());
    }
}
