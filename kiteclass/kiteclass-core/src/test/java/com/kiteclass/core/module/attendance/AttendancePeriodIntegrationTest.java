package com.kiteclass.core.module.attendance;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kiteclass.core.common.constant.AttendanceStatus;
import com.kiteclass.core.common.context.TenantContext;
import com.kiteclass.core.config.TestContainersConfiguration;
import com.kiteclass.core.config.TestSecurityConfig;
import com.kiteclass.core.config.TestTenantContextFilter;
import com.kiteclass.core.module.attendance.entity.AttendancePeriod;
import com.kiteclass.core.module.attendance.repository.AttendancePeriodRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for the Phase 1A read-only AttendancePeriod surface.
 *
 * <p>Exercises the full stack (Controller → Service → Repository → Postgres
 * via TestContainers) for {@link AttendancePeriodController}.
 *
 * @since GAP-323 Phase 1A (Wave 18b1)
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import({TestContainersConfiguration.class, TestSecurityConfig.class, TestTenantContextFilter.class})
@ContextConfiguration(initializers = TestContainersConfiguration.Initializer.class)
@Transactional
class AttendancePeriodIntegrationTest {

    private static final UUID TENANT = UUID.fromString("00000000-0000-0000-0000-000000000001");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AttendancePeriodRepository repository;

    private Long sampleId;
    private final LocalDate sampleDate = LocalDate.of(2026, 9, 5);

    @BeforeEach
    void seed() {
        TenantContext.setCurrentTenant(TENANT);
        try {
            AttendancePeriod row = AttendancePeriod.builder()
                    .studentId(101L)
                    .classId(202L)
                    .subjectSectionId(303L)
                    .periodNo(1)
                    .date(sampleDate)
                    .status(AttendanceStatus.PRESENT)
                    .recordedBy(404L)
                    .recordedAt(LocalDateTime.of(2026, 9, 5, 7, 5))
                    .notes("seed")
                    .build();
            row.setInstanceId(TENANT);
            sampleId = repository.save(row).getId();
        } finally {
            TenantContext.clear();
        }
    }

    @Test
    void findById_returnsRecord() throws Exception {
        mockMvc.perform(get("/api/v1/attendance/periods/" + sampleId)
                        .header("X-Tenant-Id", TENANT.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(sampleId))
                .andExpect(jsonPath("$.studentId").value(101))
                .andExpect(jsonPath("$.classId").value(202))
                .andExpect(jsonPath("$.subjectSectionId").value(303))
                .andExpect(jsonPath("$.periodNo").value(1))
                .andExpect(jsonPath("$.status").value("PRESENT"))
                .andExpect(jsonPath("$.recordedBy").value(404));
    }

    @Test
    void findById_unknownReturns404() throws Exception {
        mockMvc.perform(get("/api/v1/attendance/periods/9999999")
                        .header("X-Tenant-Id", TENANT.toString()))
                .andExpect(status().isNotFound());
    }

    @Test
    void findByStudent_returnsPagedResults() throws Exception {
        mockMvc.perform(get("/api/v1/attendance/periods/students/101")
                        .header("X-Tenant-Id", TENANT.toString())
                        .param("from", sampleDate.toString())
                        .param("to", sampleDate.plusDays(7).toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].studentId").value(101));
    }

    @Test
    void findByClassAndDate_returnsRoster() throws Exception {
        mockMvc.perform(get("/api/v1/attendance/periods/classes/202")
                        .header("X-Tenant-Id", TENANT.toString())
                        .param("date", sampleDate.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].periodNo").value(1));
    }

    @Test
    void findBySubjectSection_returnsPagedResults() throws Exception {
        mockMvc.perform(get("/api/v1/attendance/periods/subject-sections/303")
                        .header("X-Tenant-Id", TENANT.toString())
                        .param("from", sampleDate.toString())
                        .param("to", sampleDate.plusDays(30).toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].subjectSectionId").value(303));
    }
}
