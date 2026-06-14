package com.kiteclass.core.module.attendance;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kiteclass.core.common.constant.AttendanceStatus;
import com.kiteclass.core.common.context.TenantContext;
import com.kiteclass.core.config.TestContainersConfiguration;
import com.kiteclass.core.config.TestSecurityConfig;
import com.kiteclass.core.config.TestTenantContextFilter;
import com.kiteclass.core.module.attendance.dto.AttendancePeriodBatchCreateRequest;
import com.kiteclass.core.module.attendance.dto.AttendancePeriodCreateRequest;
import com.kiteclass.core.module.attendance.dto.AttendancePeriodUpdateRequest;
import com.kiteclass.core.module.attendance.entity.AttendancePeriod;
import com.kiteclass.core.module.attendance.repository.AttendancePeriodRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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

    // ----- Phase 1B (GAP-323b) write + rollup tests ----------------------------

    @Test
    void postBatch_insertsNewRowsThenIsIdempotent() throws Exception {
        AttendancePeriodCreateRequest entry = AttendancePeriodCreateRequest.builder()
                .studentId(102L)
                .classId(202L)
                .subjectSectionId(303L)
                .periodNo(2)
                .date(sampleDate)
                .status(AttendanceStatus.PRESENT)
                .notes(null)
                .build();
        AttendancePeriodBatchCreateRequest batch = AttendancePeriodBatchCreateRequest.builder()
                .entries(List.of(entry))
                .build();

        // 1st submission: insert
        mockMvc.perform(post("/api/v1/attendance/periods")
                        .header("X-Tenant-Id", TENANT.toString())
                        .header("X-User-Reference-Id", "909")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(batch)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].studentId").value(102))
                .andExpect(jsonPath("$[0].periodNo").value(2))
                .andExpect(jsonPath("$[0].status").value("PRESENT"));

        // 2nd submission of same tuple with different status → upsert (no duplicate)
        AttendancePeriodCreateRequest reentry = AttendancePeriodCreateRequest.builder()
                .studentId(102L)
                .classId(202L)
                .subjectSectionId(303L)
                .periodNo(2)
                .date(sampleDate)
                .status(AttendanceStatus.LATE)
                .notes("đi muộn 5 phút")
                .build();
        AttendancePeriodBatchCreateRequest rebatch = AttendancePeriodBatchCreateRequest.builder()
                .entries(List.of(reentry))
                .build();

        mockMvc.perform(post("/api/v1/attendance/periods")
                        .header("X-Tenant-Id", TENANT.toString())
                        .header("X-User-Reference-Id", "909")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(rebatch)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$[0].status").value("LATE"))
                .andExpect(jsonPath("$[0].notes").value("đi muộn 5 phút"));

        // Roster for the day — exactly two rows: seed (period 1) + new (period 2)
        mockMvc.perform(get("/api/v1/attendance/periods/classes/202")
                        .header("X-Tenant-Id", TENANT.toString())
                        .param("date", sampleDate.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void postBatch_periodNoOutOfRangeIsRejected() throws Exception {
        AttendancePeriodCreateRequest bad = AttendancePeriodCreateRequest.builder()
                .studentId(103L)
                .classId(202L)
                .subjectSectionId(303L)
                .periodNo(11) // V51 CHECK + DTO @Max — both reject
                .date(sampleDate)
                .status(AttendanceStatus.PRESENT)
                .build();
        AttendancePeriodBatchCreateRequest batch = AttendancePeriodBatchCreateRequest.builder()
                .entries(List.of(bad))
                .build();

        mockMvc.perform(post("/api/v1/attendance/periods")
                        .header("X-Tenant-Id", TENANT.toString())
                        .header("X-User-Reference-Id", "909")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(batch)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void patch_updatesStatusWithValidVersion() throws Exception {
        Long version = readVersion(sampleId);
        AttendancePeriodUpdateRequest req = AttendancePeriodUpdateRequest.builder()
                .status(AttendanceStatus.EXCUSED)
                .notes("ốm")
                .version(version)
                .build();

        mockMvc.perform(patch("/api/v1/attendance/periods/" + sampleId)
                        .header("X-Tenant-Id", TENANT.toString())
                        .header("X-User-Reference-Id", "808")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("EXCUSED"))
                .andExpect(jsonPath("$.notes").value("ốm"))
                .andExpect(jsonPath("$.recordedBy").value(808));
    }

    @Test
    void patch_staleVersionReturns409() throws Exception {
        Long version = readVersion(sampleId);
        AttendancePeriodUpdateRequest req = AttendancePeriodUpdateRequest.builder()
                .status(AttendanceStatus.EXCUSED)
                .version(version + 99) // wrong version
                .build();

        mockMvc.perform(patch("/api/v1/attendance/periods/" + sampleId)
                        .header("X-Tenant-Id", TENANT.toString())
                        .header("X-User-Reference-Id", "808")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("OPTIMISTIC_LOCK_CONFLICT"));
    }

    @Test
    void dailyRollup_aggregatesPerStudentPerDate() throws Exception {
        // Seed: 2 more rows for student 101 on the same day → 3 total tiết
        TenantContext.setCurrentTenant(TENANT);
        try {
            AttendancePeriod r2 = AttendancePeriod.builder()
                    .studentId(101L).classId(202L).subjectSectionId(304L).periodNo(2)
                    .date(sampleDate).status(AttendanceStatus.ABSENT)
                    .recordedBy(404L).recordedAt(LocalDateTime.of(2026, 9, 5, 8, 0))
                    .build();
            r2.setInstanceId(TENANT);
            AttendancePeriod r3 = AttendancePeriod.builder()
                    .studentId(101L).classId(202L).subjectSectionId(305L).periodNo(3)
                    .date(sampleDate).status(AttendanceStatus.LATE)
                    .recordedBy(404L).recordedAt(LocalDateTime.of(2026, 9, 5, 9, 0))
                    .build();
            r3.setInstanceId(TENANT);
            repository.save(r2);
            repository.save(r3);
        } finally {
            TenantContext.clear();
        }

        mockMvc.perform(get("/api/v1/attendance/periods/daily-rollup")
                        .header("X-Tenant-Id", TENANT.toString())
                        .param("classId", "202")
                        .param("from", sampleDate.toString())
                        .param("to", sampleDate.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].studentId").value(101))
                .andExpect(jsonPath("$[0].periodCount").value(3))
                .andExpect(jsonPath("$[0].presentCount").value(1))
                .andExpect(jsonPath("$[0].absentCount").value(1))
                .andExpect(jsonPath("$[0].lateCount").value(1))
                .andExpect(jsonPath("$[0].allDayAbsent").value(false));
    }

    private Long readVersion(Long id) {
        TenantContext.setCurrentTenant(TENANT);
        try {
            return repository.findByIdAndDeletedFalse(id)
                    .orElseThrow()
                    .getVersion();
        } finally {
            TenantContext.clear();
        }
    }
}
