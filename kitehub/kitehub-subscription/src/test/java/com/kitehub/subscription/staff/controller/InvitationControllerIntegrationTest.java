package com.kitehub.subscription.staff.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kitehub.platform.domain.entity.User;
import com.kitehub.subscription.client.EmailServiceClient;
import com.kitehub.subscription.repository.UserRepository;
import com.kitehub.subscription.staff.dto.AcceptStaffInvitationRequest;
import com.kitehub.subscription.staff.dto.CreateStaffInvitationRequest;
import com.kitehub.subscription.staff.entity.StaffInvitation;
import com.kitehub.subscription.staff.entity.StaffInvitationStatus;
import com.kitehub.subscription.staff.repository.StaffInvitationAuditRepository;
import com.kitehub.subscription.staff.repository.StaffInvitationRepository;
import com.kitehub.subscription.staff.service.StaffInvitationService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for {@link StaffInvitationController} (Wave 80 Bucket B,
 * GAP-561b).
 *
 * <p>Testcontainers Postgres backs JPA so the SHA-256 token-hash, idempotency
 * guard, audit-log emit, and TTL clock all exercise the production stack.</p>
 *
 * <p>Covered cases (9 — per AC):</p>
 * <ol>
 *   <li>POST happy path → 201 + audit CREATED+SENT rows</li>
 *   <li>GET list returns rows for tenant</li>
 *   <li>DELETE revoke happy path → 204 + audit REVOKED row</li>
 *   <li>POST /accept happy path → 200 + user row + audit ACCEPTED</li>
 *   <li>POST /resend rotates token + audit RESENT</li>
 *   <li>Expired token at accept → 404 INVALID_OR_EXPIRED_TOKEN</li>
 *   <li>Already-revoked token at accept → 404</li>
 *   <li>Duplicate-email re-invite → revokes old + creates new (idempotency)</li>
 *   <li>Weak password at accept → 400 WEAK_PASSWORD</li>
 * </ol>
 *
 * @since Wave 80 — GAP-561b
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
@WithMockUser(roles = "OWNER")
@DisplayName("StaffInvitationController Integration Tests (GAP-561b)")
class InvitationControllerIntegrationTest {

    @Container
    @SuppressWarnings("resource")
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("kitehub_test")
            .withUsername("test")
            .withPassword("test")
            .withReuse(true);

    @DynamicPropertySource
    static void registerDatasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("spring.jpa.properties.hibernate.dialect",
                () -> "org.hibernate.dialect.PostgreSQLDialect");
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper mapper;

    @Autowired
    private StaffInvitationRepository invitationRepository;

    @Autowired
    private StaffInvitationAuditRepository auditRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private StaffInvitationService service;

    // Email dispatch is best-effort fire-and-forget. Stub to prevent boot of
    // RabbitTemplate machinery in test profile and to assert call shape.
    @MockitoBean
    private EmailServiceClient emailServiceClient;

    @MockitoBean
    private RabbitTemplate rabbitTemplate;

    private static final UUID TENANT_ID = UUID.fromString("11111111-2222-3333-4444-555555555555");
    private static final UUID OWNER_ID = UUID.fromString("22222222-3333-4444-5555-666666666666");

    @BeforeEach
    void cleanState() {
        auditRepository.deleteAll();
        invitationRepository.deleteAll();
        userRepository.deleteAll();
        Mockito.reset(emailServiceClient);
    }

    @AfterEach
    void tearDown() {
        auditRepository.deleteAll();
        invitationRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("POST happy path — 201 created + CREATED + SENT audit rows")
    void postCreatesInvitationAndAuditRows() throws Exception {
        CreateStaffInvitationRequest payload = CreateStaffInvitationRequest.builder()
                .email("staff.new@example.edu.vn")
                .fullName("Nguyễn Văn Mẫu")
                .build();

        MvcResult result = mockMvc.perform(post("/api/v1/staff-invitations")
                        .header("X-Tenant-Id", TENANT_ID.toString())
                        .header("X-User-Id", OWNER_ID.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(payload)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("staff.new@example.edu.vn"))
                .andExpect(jsonPath("$.role").value("STAFF"))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andReturn();

        // Two audit rows expected: CREATED + SENT.
        assertThat(auditRepository.findAllByTenantIdOrderByOccurredAtDesc(TENANT_ID))
                .hasSize(2);

        // Email dispatch attempted with vi-VN expiry string + accept URL.
        ArgumentCaptor<String> urlCaptor = ArgumentCaptor.forClass(String.class);
        Mockito.verify(emailServiceClient).sendInviteStaffEmail(
                Mockito.eq("staff.new@example.edu.vn"),
                Mockito.anyString(),
                Mockito.anyString(),
                Mockito.anyString(),
                Mockito.eq("STAFF"),
                urlCaptor.capture(),
                Mockito.anyString());
        assertThat(urlCaptor.getValue()).contains("/staff/accept-invite?token=");
    }

    @Test
    @DisplayName("GET list — returns rows for tenant")
    void getListReturnsRowsForTenant() throws Exception {
        seedInvitation("a@example.com", "User A", StaffInvitationStatus.PENDING);
        seedInvitation("b@example.com", "User B", StaffInvitationStatus.PENDING);

        mockMvc.perform(get("/api/v1/staff-invitations")
                        .header("X-Tenant-Id", TENANT_ID.toString())
                        .header("X-User-Id", OWNER_ID.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    @DisplayName("DELETE revoke — 204 + REVOKED audit row")
    void deleteRevokesInvitation() throws Exception {
        StaffInvitation inv = seedInvitation("revoke@example.com", "Revoke User",
                StaffInvitationStatus.PENDING);

        mockMvc.perform(delete("/api/v1/staff-invitations/{id}", inv.getId())
                        .header("X-Tenant-Id", TENANT_ID.toString())
                        .header("X-User-Id", OWNER_ID.toString()))
                .andExpect(status().isNoContent());

        StaffInvitation reloaded = invitationRepository.findById(inv.getId()).orElseThrow();
        assertThat(reloaded.getStatus()).isEqualTo(StaffInvitationStatus.REVOKED);
        assertThat(auditRepository.findAllByInvitationIdOrderByOccurredAtAsc(inv.getId()))
                .anyMatch(a -> a.getEventType().name().equals("REVOKED"));
    }

    @Test
    @DisplayName("POST /accept happy path — 200 + user row + ACCEPTED audit")
    void postAcceptHappyPath() throws Exception {
        InvitationFixture fix = seedWithToken("accept@example.com", "Accept User");

        AcceptStaffInvitationRequest payload = AcceptStaffInvitationRequest.builder()
                .password("StrongPass123Aa")
                .fullName("Accept User Updated")
                .build();

        mockMvc.perform(post("/api/v1/staff-invitations/{token}/accept", fix.rawToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(payload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("accept@example.com"))
                .andExpect(jsonPath("$.role").value("STAFF"))
                .andExpect(jsonPath("$.userId").exists());

        StaffInvitation reloaded = invitationRepository.findById(fix.invitation.getId()).orElseThrow();
        assertThat(reloaded.getStatus()).isEqualTo(StaffInvitationStatus.ACCEPTED);
        assertThat(userRepository.findByEmail("accept@example.com")).isPresent();
        assertThat(auditRepository.findAllByInvitationIdOrderByOccurredAtAsc(fix.invitation.getId()))
                .anyMatch(a -> a.getEventType().name().equals("ACCEPTED"));
    }

    @Test
    @DisplayName("POST /resend — rotates token + RESENT audit")
    void postResendRotatesToken() throws Exception {
        InvitationFixture fix = seedWithToken("resend@example.com", "Resend User");
        String originalHash = fix.invitation.getTokenHash();

        mockMvc.perform(post("/api/v1/staff-invitations/{id}/resend", fix.invitation.getId())
                        .header("X-Tenant-Id", TENANT_ID.toString())
                        .header("X-User-Id", OWNER_ID.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING"));

        StaffInvitation reloaded = invitationRepository.findById(fix.invitation.getId()).orElseThrow();
        assertThat(reloaded.getTokenHash()).isNotEqualTo(originalHash);
        assertThat(auditRepository.findAllByInvitationIdOrderByOccurredAtAsc(fix.invitation.getId()))
                .anyMatch(a -> a.getEventType().name().equals("RESENT"));
    }

    @Test
    @DisplayName("Expired token at accept — 404 INVALID_OR_EXPIRED_TOKEN")
    void acceptExpiredTokenReturns404() throws Exception {
        InvitationFixture fix = seedWithToken("expired@example.com", "Expired User");
        StaffInvitation inv = fix.invitation;
        inv.setExpiresAt(OffsetDateTime.now().minusDays(1));
        invitationRepository.save(inv);

        AcceptStaffInvitationRequest payload = AcceptStaffInvitationRequest.builder()
                .password("StrongPass123Aa")
                .fullName("Expired User")
                .build();

        mockMvc.perform(post("/api/v1/staff-invitations/{token}/accept", fix.rawToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(payload)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("INVALID_OR_EXPIRED_TOKEN"));
    }

    @Test
    @DisplayName("Revoked invitation at accept — 404")
    void acceptRevokedReturns404() throws Exception {
        InvitationFixture fix = seedWithToken("revokedaccept@example.com", "Revoked User");
        StaffInvitation inv = fix.invitation;
        inv.setStatus(StaffInvitationStatus.REVOKED);
        invitationRepository.save(inv);

        AcceptStaffInvitationRequest payload = AcceptStaffInvitationRequest.builder()
                .password("StrongPass123Aa")
                .fullName("Revoked User")
                .build();

        mockMvc.perform(post("/api/v1/staff-invitations/{token}/accept", fix.rawToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(payload)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Duplicate email re-invite — revokes old + creates new (idempotency)")
    void duplicateEmailReinviteRevokesOldCreatesNew() throws Exception {
        CreateStaffInvitationRequest payload = CreateStaffInvitationRequest.builder()
                .email("dup@example.com")
                .fullName("Dup User")
                .build();

        // First invite
        mockMvc.perform(post("/api/v1/staff-invitations")
                        .header("X-Tenant-Id", TENANT_ID.toString())
                        .header("X-User-Id", OWNER_ID.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(payload)))
                .andExpect(status().isCreated());

        // Second invite for same email → old revoked + new created
        mockMvc.perform(post("/api/v1/staff-invitations")
                        .header("X-Tenant-Id", TENANT_ID.toString())
                        .header("X-User-Id", OWNER_ID.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(payload)))
                .andExpect(status().isCreated());

        long pending = invitationRepository.findAllByTenantIdOrderByCreatedAtDesc(TENANT_ID).stream()
                .filter(i -> i.getStatus() == StaffInvitationStatus.PENDING)
                .count();
        long revoked = invitationRepository.findAllByTenantIdOrderByCreatedAtDesc(TENANT_ID).stream()
                .filter(i -> i.getStatus() == StaffInvitationStatus.REVOKED)
                .count();
        assertThat(pending).isEqualTo(1);
        assertThat(revoked).isEqualTo(1);
    }

    @Test
    @DisplayName("Weak password at accept — 400 WEAK_PASSWORD")
    void acceptWeakPasswordReturns400() throws Exception {
        InvitationFixture fix = seedWithToken("weak@example.com", "Weak User");

        AcceptStaffInvitationRequest payload = AcceptStaffInvitationRequest.builder()
                .password("short1A")  // < 12 chars → WEAK_PASSWORD
                .fullName("Weak User")
                .build();

        mockMvc.perform(post("/api/v1/staff-invitations/{token}/accept", fix.rawToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(payload)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("WEAK_PASSWORD"));
    }

    // ── fixture helpers ─────────────────────────────────────────────────

    /** Build a PENDING invitation directly via service (raw token retained). */
    private InvitationFixture seedWithToken(String email, String fullName) {
        StaffInvitationService.InvitationIssued issued =
                service.create(TENANT_ID, OWNER_ID, email, fullName);
        return new InvitationFixture(issued.invitation(), issued.rawToken());
    }

    private StaffInvitation seedInvitation(String email, String fullName,
                                           StaffInvitationStatus status) {
        StaffInvitation inv = StaffInvitation.builder()
                .id(UUID.randomUUID())
                .tenantId(TENANT_ID)
                .email(email)
                .fullName(fullName)
                .invitedBy(OWNER_ID)
                .tokenHash("dummy-hash-" + UUID.randomUUID())
                .status(status)
                .createdAt(OffsetDateTime.now())
                .expiresAt(OffsetDateTime.now().plusDays(7))
                .build();
        return invitationRepository.save(inv);
    }

    private record InvitationFixture(StaffInvitation invitation, String rawToken) {}
}
