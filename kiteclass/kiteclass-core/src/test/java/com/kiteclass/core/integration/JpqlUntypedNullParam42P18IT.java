package com.kiteclass.core.integration;

import com.kiteclass.core.module.childprotection.converter.AesGcmAttributeConverter;
import com.kiteclass.core.module.childprotection.entity.Incident;
import com.kiteclass.core.module.childprotection.entity.Vetting;
import com.kiteclass.core.module.childprotection.enums.IncidentCategory;
import com.kiteclass.core.module.childprotection.enums.IncidentSeverity;
import com.kiteclass.core.module.childprotection.enums.IncidentStatus;
import com.kiteclass.core.module.childprotection.enums.VettingStatus;
import com.kiteclass.core.module.childprotection.repository.IncidentRepository;
import com.kiteclass.core.module.childprotection.repository.VettingRepository;
import com.kiteclass.core.module.course.entity.Course;
import com.kiteclass.core.module.course.repository.CourseRepository;
import com.kiteclass.core.module.payroll.entity.PayrollPeriod;
import com.kiteclass.core.module.payroll.repository.PayrollPeriodRepository;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Base64;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * GAP-1109 — regression IT proving the multi-optional-filter repository methods
 * no longer emit an UNTYPED null in an {@code IS NULL} position (PostgreSQL
 * {@code 42P18 could not determine data type of parameter} at PREPARE time).
 *
 * <p>The four methods under test ({@link CourseRepository#findByLevelAndCategory},
 * {@link IncidentRepository#findByFilters}, {@link VettingRepository#findByFilters},
 * {@link PayrollPeriodRepository#findByFilters}) previously used the JPQL idiom
 * {@code (:param IS NULL OR col = :param)}. Hibernate bound an untyped null for
 * the standalone {@code :param IS NULL} reference; Postgres rejects this at PREPARE
 * time. H2 silently accepts it, so the bug was invisible to the default
 * Hibernate-generated test slice. This IT runs the real Flyway schema on a real
 * PostgreSQL container (per {@code postgres-specific-type-testcontainers.md}) and
 * exercises each method with (a) all params null and (b) some params set, asserting
 * neither path throws and that filtering returns the expected rows.
 *
 * <p><b>Why not the default test slice:</b> the kc-core {@code test} profile
 * disables Flyway and uses {@code ddl-auto: create-drop}, so a normal repository
 * slice would assert against a Hibernate-generated schema on H2/whatever DB — that
 * masks the 42P18 binding bug entirely.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "spring.flyway.enabled=false",            // run Flyway manually in @BeforeAll (not Boot)
        "spring.jpa.hibernate.ddl-auto=none",     // schema comes from Flyway, NOT Hibernate
        "spring.jpa.properties.hibernate.default_schema=public"
})
@DisplayName("GAP-1109 — JPQL untyped-null filter sweep (real Postgres, Testcontainers)")
class JpqlUntypedNullParam42P18IT {

    static {
        // Register the AES-GCM converter singleton BEFORE Hibernate builds the
        // SessionFactory: the Incident/Vetting entities map encrypted BYTEA
        // columns via @Convert(AesGcmAttributeConverter), which Hibernate
        // instantiates through its no-arg constructor — that delegate requires a
        // Spring-registered singleton. @DataJpaTest does NOT load @Component beans,
        // so we register one explicitly here (ephemeral 256-bit key; "test" profile).
        new AesGcmAttributeConverter(
                Base64.getEncoder().encodeToString(new byte[32]), "test");
    }

    @SuppressWarnings("resource") // lifecycle managed by @Testcontainers
    @Container
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>(DockerImageName.parse("postgres:16-alpine"))
                    .withDatabaseName("jpql_untyped_null_it")
                    .withUsername("test")
                    .withPassword("test");

    @DynamicPropertySource
    static void datasourceProps(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @BeforeAll
    static void runFlyway() {
        Flyway.configure()
                .dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
                .locations("classpath:db/migration")
                .load()
                .migrate();
    }

    private static final Pageable PAGE = PageRequest.of(0, 50);
    private static final UUID TENANT = UUID.randomUUID();

    @Autowired
    private CourseRepository courseRepository;
    @Autowired
    private IncidentRepository incidentRepository;
    @Autowired
    private VettingRepository vettingRepository;
    @Autowired
    private PayrollPeriodRepository payrollPeriodRepository;

    // ---------------------------------------------------------------------
    // CourseRepository.findByLevelAndCategory — :level / :category (String)
    // ---------------------------------------------------------------------

    @Test
    @DisplayName("CourseRepository.findByLevelAndCategory: null + non-null params, no 42P18")
    void courseFilter_noUntypedNullError() {
        courseRepository.save(newCourse("ENG-B1", "Beginner", "Language"));
        courseRepository.save(newCourse("MAT-A1", "Advanced", "Math"));
        courseRepository.save(newCourse("FREE-1", null, null));
        courseRepository.flush(); // force INSERT SQL execution

        // (a) all params null — previously bound 2 untyped nulls -> 42P18
        assertThatCode(() -> courseRepository.findByLevelAndCategory(null, null, PAGE))
                .doesNotThrowAnyException();
        assertThat(courseRepository.findByLevelAndCategory(null, null, PAGE).getTotalElements())
                .isEqualTo(3);

        // (b) some params set — correct filtering
        assertThat(courseRepository.findByLevelAndCategory("Beginner", null, PAGE).getContent())
                .extracting(Course::getCode).containsExactly("ENG-B1");
        assertThat(courseRepository.findByLevelAndCategory("Advanced", "Math", PAGE).getContent())
                .extracting(Course::getCode).containsExactly("MAT-A1");
        assertThat(courseRepository.findByLevelAndCategory("Beginner", "Math", PAGE).getTotalElements())
                .isZero();
    }

    // ---------------------------------------------------------------------
    // IncidentRepository.findByFilters — :severity / :category / :status (enums)
    // ---------------------------------------------------------------------

    @Test
    @DisplayName("IncidentRepository.findByFilters: null + non-null enum params, no 42P18")
    void incidentFilter_noUntypedNullError() {
        incidentRepository.save(newIncident(IncidentSeverity.HIGH, IncidentCategory.ABUSE, IncidentStatus.REPORTED));
        incidentRepository.save(newIncident(IncidentSeverity.LOW, IncidentCategory.BULLYING, IncidentStatus.RESOLVED));
        incidentRepository.flush();

        // (a) all params null — previously bound 3 untyped nulls -> 42P18
        assertThatCode(() -> incidentRepository.findByFilters(null, null, null, PAGE))
                .doesNotThrowAnyException();
        assertThat(incidentRepository.findByFilters(null, null, null, PAGE).getTotalElements())
                .isEqualTo(2);

        // (b) some params set — correct filtering
        assertThat(incidentRepository.findByFilters(IncidentSeverity.HIGH, null, null, PAGE).getTotalElements())
                .isEqualTo(1);
        assertThat(incidentRepository.findByFilters(
                IncidentSeverity.HIGH, IncidentCategory.ABUSE, IncidentStatus.REPORTED, PAGE).getTotalElements())
                .isEqualTo(1);
        assertThat(incidentRepository.findByFilters(
                IncidentSeverity.LOW, IncidentCategory.ABUSE, null, PAGE).getTotalElements())
                .isZero();
    }

    // ---------------------------------------------------------------------
    // VettingRepository.findByFilters — :status (enum)
    // ---------------------------------------------------------------------

    @Test
    @DisplayName("VettingRepository.findByFilters: null + non-null enum param, no 42P18")
    void vettingFilter_noUntypedNullError() {
        vettingRepository.save(newVetting(7L, VettingStatus.PENDING));
        vettingRepository.save(newVetting(8L, VettingStatus.APPROVED));
        vettingRepository.flush();

        // (a) param null — previously bound 1 untyped null -> 42P18
        assertThatCode(() -> vettingRepository.findByFilters(null, PAGE))
                .doesNotThrowAnyException();
        assertThat(vettingRepository.findByFilters(null, PAGE).getTotalElements())
                .isEqualTo(2);

        // (b) param set — correct filtering
        Page<Vetting> pending = vettingRepository.findByFilters(VettingStatus.PENDING, PAGE);
        assertThat(pending.getTotalElements()).isEqualTo(1);
        assertThat(pending.getContent()).extracting(Vetting::getTeacherId).containsExactly(7L);
    }

    // ---------------------------------------------------------------------
    // PayrollPeriodRepository.findByFilters — :teacherId (Long) / :startDate / :endDate (LocalDate)
    // ---------------------------------------------------------------------

    @Test
    @DisplayName("PayrollPeriodRepository.findByFilters: null + non-null Long/date params, no 42P18")
    void payrollFilter_noUntypedNullError() {
        payrollPeriodRepository.save(newPayroll(1L, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31)));
        payrollPeriodRepository.save(newPayroll(2L, LocalDate.of(2026, 2, 1), LocalDate.of(2026, 2, 28)));
        payrollPeriodRepository.flush();

        // (a) all params null — previously bound 3 untyped nulls -> 42P18
        assertThatCode(() -> payrollPeriodRepository.findByFilters(null, null, null, PAGE))
                .doesNotThrowAnyException();
        assertThat(payrollPeriodRepository.findByFilters(null, null, null, PAGE).getTotalElements())
                .isEqualTo(2);

        // (b) some params set — correct filtering (Long teacher + date range bounds)
        assertThat(payrollPeriodRepository.findByFilters(1L, null, null, PAGE).getContent())
                .extracting(PayrollPeriod::getTeacherId).containsExactly(1L);
        assertThat(payrollPeriodRepository.findByFilters(null, LocalDate.of(2026, 2, 1), null, PAGE).getContent())
                .extracting(PayrollPeriod::getTeacherId).containsExactly(2L);
        assertThat(payrollPeriodRepository.findByFilters(null, null, LocalDate.of(2026, 1, 31), PAGE).getContent())
                .extracting(PayrollPeriod::getTeacherId).containsExactly(1L);
    }

    // ---------------------------------------------------------------------
    // Fixtures — BaseEntity audit/tenant fields set via setters because
    // @DataJpaTest does NOT enable JPA auditing (created_at is NOT NULL).
    // ---------------------------------------------------------------------

    private Course newCourse(String code, String level, String category) {
        Course c = Course.builder()
                .name("Course " + code)
                .code(code)
                .level(level)
                .category(category)
                .build();
        stampAudit(c);
        c.setUpdatedAt(Instant.now()); // courses.updated_at is NOT NULL
        return c;
    }

    private Incident newIncident(IncidentSeverity severity, IncidentCategory category, IncidentStatus status) {
        Incident i = Incident.builder()
                .title("incident")
                .severity(severity)
                .category(category)
                .status(status)
                .reporterUserId(99L)
                .build();
        stampAudit(i);
        return i;
    }

    private Vetting newVetting(Long teacherId, VettingStatus status) {
        Vetting v = Vetting.builder()
                .teacherId(teacherId)
                .status(status)
                .build();
        stampAudit(v);
        return v;
    }

    private PayrollPeriod newPayroll(Long teacherId, LocalDate start, LocalDate end) {
        PayrollPeriod p = PayrollPeriod.builder()
                .teacherId(teacherId)
                .startDate(start)
                .endDate(end)
                .hoursWorked(BigDecimal.TEN)
                .grossAmount(new BigDecimal("1000000"))
                .deductions(BigDecimal.ZERO)
                .netAmount(new BigDecimal("1000000"))
                .build();
        stampAudit(p);
        return p;
    }

    private void stampAudit(com.kiteclass.core.common.entity.BaseEntity entity) {
        entity.setInstanceId(TENANT);
        entity.setCreatedAt(Instant.now());
        entity.setDeleted(false);
    }
}
