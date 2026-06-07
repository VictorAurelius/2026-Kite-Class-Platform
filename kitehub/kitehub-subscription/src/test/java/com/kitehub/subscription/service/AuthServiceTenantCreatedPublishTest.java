package com.kitehub.subscription.service;

import com.kitehub.platform.domain.entity.User;
import com.kitehub.platform.domain.enums.PricingTier;
import com.kitehub.subscription.dto.InstanceResponse;
import com.kitehub.subscription.repository.InstanceRepository;
import com.kitehub.subscription.repository.UserRepository;
import com.kitehub.subscription.service.migration.SubscriptionEventEmitter;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
 * Unit test for {@link AuthService#registerFromBetaInvite} tenant.created publish
 * (Wave provisioning-1 Bucket A — GAP-945).
 *
 * <p>Verifies the publisher side of the keystone saga contract: a successful beta-invite
 * registration emits a {@code TENANT_CREATED} event with topic {@code tenant.created} and a
 * JSON payload carrying {@code tenantId / slug / audience / tone} — the exact field names the
 * kiteclass-core {@code TenantCreatedEvent} consumer deserializes.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AuthServiceTenantCreatedPublishTest {

    @Mock InstanceRepository instanceRepository;
    @Mock InstanceService instanceService;
    @Mock UserRepository userRepository;
    @Mock CaptchaService captchaService;
    @Mock EmailSenderService emailSenderService;
    @Mock JwtKeyService jwtKeyService;
    @Mock SubscriptionEventEmitter tenantEventEmitter;

    AuthService service;

    private static final UUID INSTANCE_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new AuthService(
            instanceRepository, instanceService, userRepository,
            captchaService, emailSenderService, jwtKeyService);
        ReflectionTestUtils.setField(service, "jwtSecret", "x".repeat(32));
        // field-injected dependency (mirrors @Autowired(required=false) wiring at runtime)
        ReflectionTestUtils.setField(service, "tenantEventEmitter", tenantEventEmitter);

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
    void registerFromBetaInvite_publishesTenantCreatedEvent() {
        service.registerFromBetaInvite("Acme School", "acme", "owner@acme.test", "Sup3r$ecret-2026");

        ArgumentCaptor<String> payload = ArgumentCaptor.forClass(String.class);
        verify(tenantEventEmitter).emit(eq(INSTANCE_ID), eq("TENANT_CREATED"),
                eq("tenant.created"), payload.capture());

        String json = payload.getValue();
        assertThat(json)
            .contains("\"tenantId\":\"" + INSTANCE_ID + "\"")
            .contains("\"slug\":\"acme-school\"")
            .contains("\"audience\":\"education\"")
            .contains("\"tone\":\"professional\"");
    }
}
