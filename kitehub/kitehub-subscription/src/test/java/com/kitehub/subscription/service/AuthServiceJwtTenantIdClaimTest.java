package com.kitehub.subscription.service;

import com.kitehub.platform.domain.entity.Instance;
import com.kitehub.platform.domain.entity.User;
import com.kitehub.subscription.auth.twofactor.ChallengeTokenService;
import com.kitehub.subscription.dto.LoginRequest;
import com.kitehub.subscription.dto.LoginResponse;
import com.kitehub.subscription.repository.InstanceRepository;
import com.kitehub.subscription.repository.UserRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import javax.crypto.SecretKey;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link AuthService} JWT {@code tenantId} claim enrichment
 * (GAP-704 / Wave 104 Bucket A).
 *
 * <p>Verifies:
 * <ul>
 *   <li>OWNER login JWT contains {@code tenantId} claim matching their
 *       {@code instances.owner_id} binding.</li>
 *   <li>PLATFORM_ADMIN login JWT does NOT contain {@code tenantId} claim
 *       (tenant-agnostic).</li>
 *   <li>OWNER with no bound instance gets no claim (defensive: claim is
 *       optional rather than crashing login).</li>
 *   <li>Unknown role gets no claim (defensive fallback).</li>
 * </ul>
 *
 * <p>Closes root cause of {@code GAP-531} PARTIAL — tenant init handoff was
 * dropping the {@code tenantId} signal in the access JWT, blocking owners from
 * the onboarding wizard with X-Tenant-Id header mismatch.
 *
 * @since 1.0.0 (Wave 104 GAP-704)
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("AuthService — JWT tenantId claim (GAP-704)")
class AuthServiceJwtTenantIdClaimTest {

    @Mock InstanceRepository instanceRepository;
    @Mock InstanceService instanceService;
    @Mock UserRepository userRepository;
    @Mock CaptchaService captchaService;
    @Mock EmailSenderService emailSenderService;
    @Mock JwtKeyService jwtKeyService;
    @Mock ChallengeTokenService challengeTokenService;

    AuthService service;
    final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
    SecretKey signingKey;

    private static final String OWNER_EMAIL = "owner.hong@skyedu.vn";
    private static final String ADMIN_EMAIL = "admin@kitehub.me";
    private static final String CORRECT_PASSWORD = "CorrectHorseBatteryStaple-2026";

    @BeforeEach
    void setUp() {
        service = new AuthService(
            instanceRepository, instanceService, userRepository,
            captchaService, emailSenderService, jwtKeyService,
            null /* loginAuditService — not exercised */,
            null /* challengeTokenService — not exercised */
        );
        // Bypass @PostConstruct jwt secret guard — only login() path under test.
        ReflectionTestUtils.setField(service, "jwtSecret", "x".repeat(32));

        // Real key so the JWT decode roundtrip works.
        signingKey = Keys.hmacShaKeyFor("x".repeat(64).getBytes(StandardCharsets.UTF_8));
        when(jwtKeyService.signingKey()).thenReturn(signingKey);
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        when(instanceService.getInstancesByOwner(any())).thenReturn(Collections.emptyList());
    }

    @Test
    @DisplayName("Owner login JWT contains tenantId claim matching instances.owner_id binding")
    void ownerJwtIncludesTenantIdClaim() {
        UUID ownerId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();
        User owner = builderUser(ownerId, OWNER_EMAIL, "OWNER");
        Instance instance = instanceWithIds(tenantId, ownerId);

        when(userRepository.findByEmail(OWNER_EMAIL)).thenReturn(Optional.of(owner));
        when(instanceRepository.findByOwnerIdAndDeletedFalse(ownerId))
            .thenReturn(List.of(instance));

        LoginResponse resp = service.login(loginRequest(OWNER_EMAIL, CORRECT_PASSWORD));

        assertThat(resp).isNotNull();
        assertThat(resp.getAccessToken()).isNotBlank();

        Claims claims = parseClaims(resp.getAccessToken());
        assertThat(claims.get("tenantId", String.class))
            .as("Owner JWT MUST carry tenantId claim per GAP-704 AC")
            .isEqualTo(tenantId.toString());
        assertThat(claims.get("role", String.class)).isEqualTo("OWNER");
        assertThat(claims.getSubject()).isEqualTo(ownerId.toString());
    }

    @Test
    @DisplayName("PLATFORM_ADMIN login JWT does NOT contain tenantId claim (tenant-agnostic)")
    void platformAdminJwtOmitsTenantIdClaim() {
        UUID adminId = UUID.randomUUID();
        User admin = builderUser(adminId, ADMIN_EMAIL, "PLATFORM_ADMIN");

        when(userRepository.findByEmail(ADMIN_EMAIL)).thenReturn(Optional.of(admin));

        LoginResponse resp = service.login(loginRequest(ADMIN_EMAIL, CORRECT_PASSWORD));

        assertThat(resp).isNotNull();
        assertThat(resp.getAccessToken()).isNotBlank();

        Claims claims = parseClaims(resp.getAccessToken());
        assertThat(claims.get("tenantId"))
            .as("PLATFORM_ADMIN JWT MUST NOT carry tenantId claim — tenant-agnostic role")
            .isNull();
        assertThat(claims.get("role", String.class)).isEqualTo("PLATFORM_ADMIN");
    }

    @Test
    @DisplayName("Owner with no bound instance: tenantId claim omitted (defensive — login still succeeds)")
    void ownerWithoutInstanceGetsNoTenantClaim() {
        UUID ownerId = UUID.randomUUID();
        User owner = builderUser(ownerId, OWNER_EMAIL, "OWNER");

        when(userRepository.findByEmail(OWNER_EMAIL)).thenReturn(Optional.of(owner));
        when(instanceRepository.findByOwnerIdAndDeletedFalse(ownerId))
            .thenReturn(Collections.emptyList());

        LoginResponse resp = service.login(loginRequest(OWNER_EMAIL, CORRECT_PASSWORD));

        Claims claims = parseClaims(resp.getAccessToken());
        assertThat(claims.get("tenantId"))
            .as("Owner with no instance binding → omit claim rather than crash")
            .isNull();
    }

    @Test
    @DisplayName("Unknown / future role gets no tenantId claim (defensive fallback)")
    void unknownRoleGetsNoTenantClaim() {
        UUID userId = UUID.randomUUID();
        // STAFF is a tenant-scoped role whose auth path is not yet wired through
        // this service — defensive fallback omits the claim per GAP-704 §4.
        User staff = builderUser(userId, "staff@skyedu.vn", "STAFF");

        when(userRepository.findByEmail("staff@skyedu.vn")).thenReturn(Optional.of(staff));

        LoginResponse resp = service.login(loginRequest("staff@skyedu.vn", CORRECT_PASSWORD));

        Claims claims = parseClaims(resp.getAccessToken());
        assertThat(claims.get("tenantId"))
            .as("STAFF role not yet wired here — omit claim until per-role tenant lookup lands")
            .isNull();
    }

    /* helpers */

    private User builderUser(UUID id, String email, String role) {
        return User.builder()
            .id(id)
            .email(email)
            .name("Tester")
            .passwordHash(encoder.encode(CORRECT_PASSWORD))
            .role(role)
            .emailVerified(true)
            .build();
    }

    /**
     * Build an Instance with a forced id via reflection. {@code BaseEntity.id} has no
     * public setter; production code always lets JPA generate it. For tests we need a
     * known UUID to compare against the JWT claim.
     */
    private Instance instanceWithIds(UUID instanceId, UUID ownerId) {
        Instance instance = new Instance();
        instance.setOwnerId(ownerId);
        try {
            Field idField = instance.getClass().getSuperclass().getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(instance, instanceId);
        } catch (NoSuchFieldException | IllegalAccessException ex) {
            throw new AssertionError("Failed to set Instance.id via reflection: " + ex.getMessage(), ex);
        }
        return instance;
    }

    private LoginRequest loginRequest(String email, String password) {
        LoginRequest r = new LoginRequest();
        r.setEmail(email);
        r.setPassword(password);
        return r;
    }

    private Claims parseClaims(String jwt) {
        return Jwts.parser()
            .verifyWith(signingKey)
            .build()
            .parseSignedClaims(jwt)
            .getPayload();
    }
}
