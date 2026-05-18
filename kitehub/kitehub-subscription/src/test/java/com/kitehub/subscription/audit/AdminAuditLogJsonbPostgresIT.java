package com.kitehub.subscription.audit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * GAP-642 — V54 JSONB columns Testcontainers IT.
 *
 * <p>Verify PostgreSQL-specific JSONB binding cho 2 columns thêm bởi V54
 * migration ({@code before_state}, {@code after_state}) trong {@code admin_audit_log}.
 *
 * <p>Tại sao cần test này (per {@code .claude/rules/postgres-specific-type-testcontainers.md} §3):
 * <ul>
 *   <li>H2 in-memory không support native JSONB type — H2 silently stores JSONB
 *       as CLOB, Postgres validates JSON syntax + rejects malformed JSON.</li>
 *   <li>H2 unit tests (pre-existing) sẽ pass dù binding code sai — bug chỉ lộ
 *       khi chạy trên real Postgres trong production.</li>
 *   <li>Testcontainers IT dùng {@code postgres:15-alpine} (prod-aligned version)
 *       catch binding drift TRƯỚC khi merge.</li>
 * </ul>
 *
 * <p>Test scope:
 * <ol>
 *   <li>JSONB round-trip {@code before_state} (write + flush + read)</li>
 *   <li>JSONB round-trip {@code after_state} (write + flush + read)</li>
 *   <li>Null JSONB columns (nullable contract)</li>
 *   <li>Empty JSON object {@code {}} binding</li>
 *   <li>Large JSONB payload &gt;1KB round-trip</li>
 *   <li>Unicode chars trong JSONB string values</li>
 * </ol>
 *
 * @since Wave 97 Bucket D — GAP-642
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
@DisplayName("AdminAuditLog V54 JSONB columns — PostgreSQL Testcontainers IT (GAP-642)")
class AdminAuditLogJsonbPostgresIT {

    @Container
    @SuppressWarnings("resource")
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("kitehub_test")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void datasourceProps(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("spring.jpa.properties.hibernate.dialect",
                () -> "org.hibernate.dialect.PostgreSQLDialect");
    }

    @Autowired
    private AdminAuditLogRepository repository;

    @BeforeEach
    void cleanState() {
        repository.deleteAll();
        repository.flush();
    }

    // ─── Test 1: before_state JSONB round-trip ────────────────────────────────

    @Test
    @DisplayName("before_state JSONB: write + flush + read — binding round-trip không lỗi")
    void beforeState_jsonbRoundTrip() {
        // Given — JSON snapshot trạng thái trước action (PENDING → APPROVED)
        String beforeStateJson = "{\"status\":\"PENDING\",\"submittedAt\":\"2026-05-18T10:00:00Z\","
                + "\"email\":\"test@example.com\",\"invitedBy\":null}";

        AdminAuditLog row = AdminAuditLog.builder()
                .adminUserId(UUID.randomUUID())
                .action("BETA_REQUEST_APPROVE")
                .targetEntityType("beta_access_request")
                .targetEntityId("101")
                .requestIp("203.0.113.1")
                .userAgent("TestRunner/1.0")
                .payloadJson("{}")
                .success(true)
                .createdAt(LocalDateTime.now())
                .beforeState(beforeStateJson)
                // afterState để null — kiểm tra mixed null/non-null OK
                .build();

        // When — flush() bắt buộc: forces SQL INSERT thực thi ngay lập tức,
        // catch JSONB binding errors BEFORE transaction commit
        AdminAuditLog saved = repository.save(row);
        repository.flush();

        // Then
        AdminAuditLog reloaded = repository.findById(saved.getId()).orElseThrow();
        assertThat(reloaded.getBeforeState())
                .as("before_state JSONB phải persist + reload đầy đủ")
                .isNotNull()
                .contains("\"status\":\"PENDING\"")
                .contains("\"email\":\"test@example.com\"");
        assertThat(reloaded.getAfterState())
                .as("after_state null khi không set")
                .isNull();
    }

    // ─── Test 2: after_state JSONB round-trip ────────────────────────────────

    @Test
    @DisplayName("after_state JSONB: write + flush + read — binding round-trip không lỗi")
    void afterState_jsonbRoundTrip() {
        // Given — JSON snapshot trạng thái sau action
        String afterStateJson = "{\"status\":\"APPROVED\",\"approvedAt\":\"2026-05-18T11:30:00Z\","
                + "\"approvedBy\":\"admin@kitehub.me\",\"inviteToken\":\"tok-abc-123\"}";

        AdminAuditLog row = AdminAuditLog.builder()
                .adminUserId(UUID.randomUUID())
                .action("BETA_REQUEST_APPROVE")
                .targetEntityType("beta_access_request")
                .targetEntityId("102")
                .requestIp("10.0.0.1")
                .userAgent("TestRunner/1.0")
                .payloadJson("{\"approvedBy\":\"admin@kitehub.me\"}")
                .success(true)
                .createdAt(LocalDateTime.now())
                // beforeState để null — kiểm tra mixed null/non-null OK
                .afterState(afterStateJson)
                .build();

        // When
        AdminAuditLog saved = repository.save(row);
        repository.flush();

        // Then
        AdminAuditLog reloaded = repository.findById(saved.getId()).orElseThrow();
        assertThat(reloaded.getAfterState())
                .as("after_state JSONB phải persist + reload đầy đủ")
                .isNotNull()
                .contains("\"status\":\"APPROVED\"")
                .contains("\"approvedBy\":\"admin@kitehub.me\"");
        assertThat(reloaded.getBeforeState())
                .as("before_state null khi không set")
                .isNull();
    }

    // ─── Test 3: Null JSONB columns (nullable contract) ──────────────────────

    @Test
    @DisplayName("null JSONB: cả before_state + after_state null — nullable contract đúng")
    void nullJsonbColumns_handleNullCorrectly() {
        // Given — row không có before/after state (vd: action không phải update)
        AdminAuditLog row = AdminAuditLog.builder()
                .adminUserId(UUID.randomUUID())
                .action("BETA_REQUEST_LIST")
                .targetEntityType("beta_access_request")
                .targetEntityId(null)
                .requestIp("192.168.1.1")
                .payloadJson("{}")
                .success(true)
                .createdAt(LocalDateTime.now())
                // KHÔNG set beforeState / afterState
                .build();

        // When
        AdminAuditLog saved = repository.save(row);
        repository.flush();

        // Then — nullable JSONB columns phải persist + reload đúng là null
        AdminAuditLog reloaded = repository.findById(saved.getId()).orElseThrow();
        assertThat(reloaded.getBeforeState())
                .as("before_state phải null khi không set — nullable JSONB contract")
                .isNull();
        assertThat(reloaded.getAfterState())
                .as("after_state phải null khi không set — nullable JSONB contract")
                .isNull();
    }

    // ─── Test 4: Empty JSON object {} ────────────────────────────────────────

    @Test
    @DisplayName("empty JSON {}: before_state = '{}' — Postgres chấp nhận, round-trip OK")
    void emptyJsonObject_validJsonb() {
        // Given — empty JSON object là valid JSONB trong Postgres
        // (H2 cũng accept nhưng Postgres validate JSON syntax nghiêm hơn)
        AdminAuditLog row = AdminAuditLog.builder()
                .adminUserId(UUID.randomUUID())
                .action("BETA_REQUEST_REJECT")
                .targetEntityType("beta_access_request")
                .targetEntityId("103")
                .requestIp("10.0.0.2")
                .payloadJson("{}")
                .success(true)
                .createdAt(LocalDateTime.now())
                .beforeState("{}")        // empty JSON object
                .afterState("{}")         // empty JSON object
                .build();

        // When
        assertThatCode(() -> {
            AdminAuditLog saved = repository.save(row);
            repository.flush();  // catches JSONB validation errors
            AdminAuditLog reloaded = repository.findById(saved.getId()).orElseThrow();
            assertThat(reloaded.getBeforeState())
                    .as("empty {} phải persist + reload như '{}'")
                    .isEqualTo("{}");
            assertThat(reloaded.getAfterState())
                    .as("empty {} phải persist + reload như '{}'")
                    .isEqualTo("{}");
        }).doesNotThrowAnyException();
    }

    // ─── Test 5: Large JSONB payload >1KB ────────────────────────────────────

    @Test
    @DisplayName("large JSONB >1KB: before_state với nested object lớn — round-trip OK")
    void largeJsonbPayload_roundTrip() {
        // Given — JSONB payload lớn >1KB giả lập snapshot trạng thái đầy đủ
        // (full tenant config object trong before_state khi admin reset config)
        StringBuilder sb = new StringBuilder("{\"tenantId\":\"t-large-test\",\"config\":{");
        for (int i = 0; i < 50; i++) {
            if (i > 0) sb.append(",");
            sb.append("\"setting_").append(i).append("\":\"value_")
              .append("x".repeat(10)).append("_").append(i).append("\"");
        }
        sb.append("},\"metadata\":{\"version\":\"1.0\",\"updatedAt\":\"2026-05-18T10:00:00Z\","
                + "\"updatedBy\":\"admin@kitehub.me\",\"reason\":\"")
          .append("Admin config reset for Phase 1 BETA launch preparation. ".repeat(3))
          .append("\"}}");
        String largeJson = sb.toString();

        // Verify payload thực sự > 1KB
        assertThat(largeJson.length())
                .as("test payload phải > 1KB")
                .isGreaterThan(1024);

        AdminAuditLog row = AdminAuditLog.builder()
                .adminUserId(UUID.randomUUID())
                .action("TENANT_CONFIG_RESET")
                .targetEntityType("tenant_config")
                .targetEntityId("t-large-test")
                .requestIp("10.0.0.3")
                .payloadJson("{\"action\":\"reset\"}")
                .success(true)
                .createdAt(LocalDateTime.now())
                .beforeState(largeJson)
                .afterState("{\"tenantId\":\"t-large-test\",\"config\":{}}")
                .build();

        // When
        AdminAuditLog saved = repository.save(row);
        repository.flush();

        // Then
        AdminAuditLog reloaded = repository.findById(saved.getId()).orElseThrow();
        assertThat(reloaded.getBeforeState())
                .as("large JSONB >1KB phải persist + reload đầy đủ không truncate")
                .isNotNull()
                .hasSizeGreaterThan(1024)
                .contains("\"tenantId\":\"t-large-test\"")
                .contains("\"setting_49\"");
    }

    // ─── Test 6: Unicode chars trong JSONB ───────────────────────────────────

    @Test
    @DisplayName("unicode trong JSONB: tên tiếng Việt, emoji — Postgres lưu đúng UTF-8")
    void unicodeCharsInJsonb_utf8Preserved() {
        // Given — JSONB với chars ngoài ASCII
        // (common cho VN edu platform: tên trường, tên người dùng)
        String jsonWithUnicode = "{\"tenantName\":\"Trung tâm Anh ngữ Ánh Sáng\","
                + "\"ownerName\":\"Nguyễn Thị Hoa\","
                + "\"city\":\"Thành phố Hồ Chí Minh\","
                + "\"note\":\"Cảm ơn bạn đã đăng ký! 🎉\","
                + "\"createdAt\":\"2026-05-18T10:00:00+07:00\"}";

        AdminAuditLog row = AdminAuditLog.builder()
                .adminUserId(UUID.randomUUID())
                .action("TENANT_APPROVE")
                .targetEntityType("tenant")
                .targetEntityId("t-vn-edu-test")
                .requestIp("10.0.0.4")
                .payloadJson("{\"approved\":true}")
                .success(true)
                .createdAt(LocalDateTime.now())
                .beforeState("{\"status\":\"PENDING\"}")
                .afterState(jsonWithUnicode)
                .build();

        // When
        AdminAuditLog saved = repository.save(row);
        repository.flush();

        // Then — UTF-8 chars phải được preserve đúng (no mojibake)
        AdminAuditLog reloaded = repository.findById(saved.getId()).orElseThrow();
        assertThat(reloaded.getAfterState())
                .as("unicode chars (Vietnamese + emoji) phải preserve exact trong JSONB")
                .isNotNull()
                .contains("Trung tâm Anh ngữ Ánh Sáng")
                .contains("Nguyễn Thị Hoa")
                .contains("Thành phố Hồ Chí Minh")
                .contains("🎉");
    }
}
