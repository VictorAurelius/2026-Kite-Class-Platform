package com.kitehub.subscription.service;

import com.kitehub.platform.domain.entity.User;
import com.kitehub.platform.domain.enums.PricingTier;
import com.kitehub.subscription.audit.TenantAuditService;
import com.kitehub.subscription.dto.InstanceResponse;
import com.kitehub.subscription.repository.InstanceRepository;
import com.kitehub.subscription.repository.UserRepository;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit test for {@link AuthService#registerFromBetaInvite} TENANT_PROVISIONED audit wiring
 * (Wave provisioning-1 Bucket B — GAP-949).
 *
 * <p>Verifies (1) a successful beta-invite registration invokes
 * {@link TenantAuditService#recordTenantProvisioned} with tenantId/ownerId/email/subdomain,
 * and (2) registration still completes when the audit path is exercised — isolation lives
 * inside {@code TenantAuditService} (REQUIRES_NEW + swallow) per
 * {@code .claude/rules/audit-service-isolation.md} §1.</p>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AuthServiceTenantProvisionedAuditTest {

    @Mock InstanceRepository instanceRepository;
    @Mock InstanceService instanceService;
    @Mock UserRepository userRepository;
    @Mock CaptchaService captchaService;
    @Mock EmailSenderService emailSenderService;
    @Mock JwtKeyService jwtKeyService;
    @Mock TenantAuditService tenantAuditService;

    AuthService service;

    private static final UUID INSTANCE_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new AuthService(
            instanceRepository, instanceService, userRepository,
            captchaService, emailSenderService, jwtKeyService);
        ReflectionTestUtils.setField(service, "jwtSecret", "x".repeat(32));
        // field-injected dependency (mirrors @Autowired(required=false) wiring at runtime)
        ReflectionTestUtils.setField(service, "tenantAuditService", tenantAuditService);

        SecretKey key = Keys.hmacShaKeyFor("x".repeat(64).getBytes(StandardCharsets.UTF_8));
        when(jwtKeyService.signingKey()).thenReturn(key);

        when(userRepository.existsByEmail(any())).thenReturn(false);
        when(instanceRepository.existsBySubdomainAndDeletedFalse(any())).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            ReflectionTestUtils.setField(u, "id", UUID.randomUUID());
            return u;
        });

        InstanceResponse instance = InstanceResponse.builder()
            .id(INSTANCE_ID)
            .subdomain("acme")
            .slug("acme-school")
            .organizationName("Acme School")
            .tier(PricingTier.FREE)
            .build();
        when(instanceService.createTrialInstance(any())).thenReturn(instance);
    }

    @Test
    void registerFromBetaInvite_recordsTenantProvisionedAudit() {
        service.registerFromBetaInvite("Acme School", "acme", "owner@acme.test", "Sup3r$ecret-2026");

        verify(tenantAuditService).recordTenantProvisioned(
            eq(INSTANCE_ID), any(UUID.class), eq("owner@acme.test"), eq("acme"));
    }

    @Test
    void registerFromBetaInvite_completesEvenIfAuditPathExercised() {
        // The audit helper is contractually non-throwing (REQUIRES_NEW + internal swallow),
        // so even an exercised audit path leaves registration intact.
        var response = service.registerFromBetaInvite(
            "Acme School", "acme", "owner@acme.test", "Sup3r$ecret-2026");

        assertThat(response).isNotNull();
        assertThat(response.getInstance().getId()).isEqualTo(INSTANCE_ID);
        assertThat(response.getAccessToken()).isNotBlank();
    }
}
