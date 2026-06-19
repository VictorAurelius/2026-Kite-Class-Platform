package com.kitehub.subscription.service;

import com.kitehub.platform.domain.entity.Instance;
import com.kitehub.platform.domain.enums.InstanceStatus;
import com.kitehub.platform.domain.enums.PricingTier;
import com.kitehub.subscription.config.DomainVerificationConfig;
import com.kitehub.subscription.dto.DomainVerifyResponse;
import com.kitehub.subscription.repository.InstanceRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for DomainService.
 *
 * @author KiteHub Team
 * @since 1.0.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("DomainService Unit Tests")
class DomainServiceTest {

    @Mock
    private InstanceRepository instanceRepository;

    @Mock
    private DomainVerificationConfig domainVerificationConfig;

    @Mock
    private DnsTxtLookupService dnsTxtLookupService;

    @Mock
    private CertProvisioningService certProvisioningService;

    @InjectMocks
    private DomainService domainService;

    private UUID instanceId;
    private Instance premiumInstance;
    private Instance freeInstance;

    @BeforeEach
    void setUp() {
        // GAP-1414: appBaseUrl is @Value-injected at runtime (default https://kitehub.me).
        // @InjectMocks bypasses Spring property resolution, so set the canonical default —
        // buildResponse() strips its scheme to build the subdomain backup URL (NPE otherwise).
        ReflectionTestUtils.setField(domainService, "appBaseUrl", "https://kitehub.me");
        instanceId = UUID.randomUUID();

        premiumInstance = new Instance();
        premiumInstance.setId(instanceId);
        premiumInstance.setSubdomain("my-school");
        premiumInstance.setOrganizationName("My School");
        premiumInstance.setOwnerId(UUID.randomUUID());
        premiumInstance.setTier(PricingTier.PREMIUM);
        premiumInstance.setStatus(InstanceStatus.ACTIVE);
        premiumInstance.setDatabaseUrl("jdbc:postgresql://localhost:5432/test");
        premiumInstance.setDatabaseUsername("user");
        premiumInstance.setDatabasePassword("pass");
        premiumInstance.setCreatedAt(LocalDateTime.now());
        premiumInstance.setUpdatedAt(LocalDateTime.now());

        freeInstance = new Instance();
        freeInstance.setId(UUID.randomUUID());
        freeInstance.setSubdomain("free-school");
        freeInstance.setOrganizationName("Free School");
        freeInstance.setOwnerId(UUID.randomUUID());
        freeInstance.setTier(PricingTier.FREE);
        freeInstance.setStatus(InstanceStatus.TRIAL);
        freeInstance.setDatabaseUrl("jdbc:postgresql://localhost:5432/test2");
        freeInstance.setDatabaseUsername("user2");
        freeInstance.setDatabasePassword("pass2");
        freeInstance.setCreatedAt(LocalDateTime.now());
        freeInstance.setUpdatedAt(LocalDateTime.now());

        lenient().when(domainVerificationConfig.getTimeoutHours()).thenReturn(48);
    }

    // =========================================================
    // initiateCustomDomain tests
    // =========================================================

    @Test
    @DisplayName("initiateCustomDomain: FREE tier instance should throw IllegalArgumentException")
    void initiateCustomDomain_freeInstance_throwsForbidden() {
        // Given
        when(instanceRepository.findById(freeInstance.getId())).thenReturn(Optional.of(freeInstance));

        // When & Then
        assertThatThrownBy(() -> domainService.initiateCustomDomain(freeInstance.getId(), "school.example.com"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Custom domain");

        verify(instanceRepository, never()).save(any());
    }

    @Test
    @DisplayName("initiateCustomDomain: BASIC tier instance should throw IllegalArgumentException")
    void initiateCustomDomain_basicInstance_throwsForbidden() {
        // Given
        Instance basicInstance = createInstanceWithTier(PricingTier.BASIC);
        when(instanceRepository.findById(basicInstance.getId())).thenReturn(Optional.of(basicInstance));

        // When & Then
        assertThatThrownBy(() -> domainService.initiateCustomDomain(basicInstance.getId(), "school.example.com"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Custom domain");

        verify(instanceRepository, never()).save(any());
    }

    @Test
    @DisplayName("initiateCustomDomain: PREMIUM instance should return verify token")
    void initiateCustomDomain_premiumInstance_returnsToken() {
        // Given
        when(instanceRepository.findById(instanceId)).thenReturn(Optional.of(premiumInstance));
        when(instanceRepository.save(any(Instance.class))).thenReturn(premiumInstance);
        when(instanceRepository.findByCustomDomainAndDeletedFalse("school.example.com")).thenReturn(Optional.empty());

        // When
        DomainVerifyResponse response = domainService.initiateCustomDomain(instanceId, "school.example.com");

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getCustomDomain()).isEqualTo("school.example.com");
        assertThat(response.getVerifyToken()).isNotBlank();
        assertThat(response.getVerifyToken()).startsWith("kitehub-verify=");
        assertThat(response.getVerifyRecord()).contains("TXT");
        assertThat(response.getStatus()).isEqualTo(Instance.DomainStatus.PENDING_VERIFY);

        verify(instanceRepository).save(any(Instance.class));
    }

    @Test
    @DisplayName("KH-7 FM-5: initiateCustomDomain should reject platform-reserved domains")
    void initiateCustomDomain_reservedDomain_throws() {
        // Given — a PREMIUM instance (passes the tier gate) tries to claim platform domains
        when(instanceRepository.findById(instanceId)).thenReturn(Optional.of(premiumInstance));

        // When & Then — reserved apex + a subdomain of a reserved apex both rejected
        assertThatThrownBy(() -> domainService.initiateCustomDomain(instanceId, "kitehub.me"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("reserved");
        assertThatThrownBy(() -> domainService.initiateCustomDomain(instanceId, "app.kiteclass.com")) // stale-domain-ok: user-supplied custom-domain rejected because kiteclass.com is reserved (denylist test)
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("reserved");
        verify(instanceRepository, never()).save(any(Instance.class));
    }

    @Test
    @DisplayName("initiateCustomDomain: ENTERPRISE instance should return verify token")
    void initiateCustomDomain_enterpriseInstance_returnsToken() {
        // Given
        Instance enterpriseInstance = createInstanceWithTier(PricingTier.ENTERPRISE);
        when(instanceRepository.findById(enterpriseInstance.getId())).thenReturn(Optional.of(enterpriseInstance));
        when(instanceRepository.save(any(Instance.class))).thenReturn(enterpriseInstance);
        when(instanceRepository.findByCustomDomainAndDeletedFalse("enterprise.example.com")).thenReturn(Optional.empty());

        // When
        DomainVerifyResponse response = domainService.initiateCustomDomain(enterpriseInstance.getId(), "enterprise.example.com");

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getVerifyToken()).startsWith("kitehub-verify=");
        assertThat(response.getStatus()).isEqualTo(Instance.DomainStatus.PENDING_VERIFY);
    }

    @Test
    @DisplayName("initiateCustomDomain: domain already in use by another instance throws exception")
    void initiateCustomDomain_domainAlreadyUsed_throwsException() {
        // Given
        Instance otherInstance = createInstanceWithTier(PricingTier.PREMIUM);
        when(instanceRepository.findById(instanceId)).thenReturn(Optional.of(premiumInstance));
        when(instanceRepository.findByCustomDomainAndDeletedFalse("taken.example.com"))
            .thenReturn(Optional.of(otherInstance)); // different instance owns the domain

        // When & Then
        assertThatThrownBy(() -> domainService.initiateCustomDomain(instanceId, "taken.example.com"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("already in use");

        verify(instanceRepository, never()).save(any());
    }

    @Test
    @DisplayName("initiateCustomDomain: instance not found throws EntityNotFoundException")
    void initiateCustomDomain_instanceNotFound_throwsEntityNotFound() {
        // Given
        UUID notFoundId = UUID.randomUUID();
        when(instanceRepository.findById(notFoundId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> domainService.initiateCustomDomain(notFoundId, "school.example.com"))
            .isInstanceOf(EntityNotFoundException.class);
    }

    // =========================================================
    // verifyCustomDomain tests
    // =========================================================

    @Test
    @DisplayName("verifyCustomDomain: instance without pending domain throws exception")
    void verifyCustomDomain_noPendingDomain_throwsException() {
        // Given - instance has no domain set
        when(instanceRepository.findById(instanceId)).thenReturn(Optional.of(premiumInstance));

        // When & Then
        assertThatThrownBy(() -> domainService.verifyCustomDomain(instanceId))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("No domain verification pending");
    }

    @Test
    @DisplayName("verifyCustomDomain: DNS lookup miss returns PENDING (state unchanged)")
    void verifyCustomDomain_dnsLookupMiss_returnsPending() {
        // Given - instance with pending domain; DNS lookup returns false (no TXT match)
        premiumInstance.setCustomDomain("school.example.com");
        premiumInstance.setDomainVerifyToken("kitehub-verify=abc123");
        premiumInstance.setDomainStatus(Instance.DomainStatus.PENDING_VERIFY);
        when(instanceRepository.findById(instanceId)).thenReturn(Optional.of(premiumInstance));
        when(instanceRepository.save(any(Instance.class))).thenReturn(premiumInstance);
        when(dnsTxtLookupService.verifyTxtRecord("school.example.com", "kitehub-verify=abc123"))
            .thenReturn(false);

        // When
        DomainVerifyResponse response = domainService.verifyCustomDomain(instanceId);

        // Then - stays PENDING (state machine waits for tenant to add TXT or timeout)
        assertThat(response).isNotNull();
        assertThat(response.getCustomDomain()).isEqualTo("school.example.com");
        assertThat(response.getStatus()).isEqualTo(Instance.DomainStatus.PENDING_VERIFY);
        verify(dnsTxtLookupService).verifyTxtRecord("school.example.com", "kitehub-verify=abc123");
    }

    @Test
    @DisplayName("verifyCustomDomain: DNS TXT match → CERT_PROVISIONING → cert issued → VERIFIED")
    void verifyCustomDomain_dnsTxtMatch_returnsVerified() {
        // Given - DNS lookup returns true (TXT match found) + stub cert issues synchronously
        premiumInstance.setCustomDomain("school.example.com");
        premiumInstance.setDomainVerifyToken("kitehub-verify=xyz789");
        premiumInstance.setDomainStatus(Instance.DomainStatus.PENDING_VERIFY);
        when(instanceRepository.findById(instanceId)).thenReturn(Optional.of(premiumInstance));
        when(instanceRepository.save(any(Instance.class))).thenReturn(premiumInstance);
        when(dnsTxtLookupService.verifyTxtRecord("school.example.com", "kitehub-verify=xyz789"))
            .thenReturn(true);
        when(certProvisioningService.requestCertificate("school.example.com"))
            .thenReturn(CertProvisioningResult.issued("stub-cert"));

        // When
        DomainVerifyResponse response = domainService.verifyCustomDomain(instanceId);

        // Then - DNS verified → CERT_PROVISIONING → cert issued → VERIFIED with timestamp recorded
        assertThat(response.getStatus()).isEqualTo(Instance.DomainStatus.VERIFIED);
        ArgumentCaptor<Instance> captor = ArgumentCaptor.forClass(Instance.class);
        verify(instanceRepository).save(captor.capture());
        assertThat(captor.getValue().getDomainStatus()).isEqualTo(Instance.DomainStatus.VERIFIED);
        assertThat(captor.getValue().getDomainVerifiedAt()).isNotNull();
        verify(certProvisioningService).requestCertificate("school.example.com");
    }

    @Test
    @DisplayName("verifyCustomDomain: DNS match but cert PENDING → stays CERT_PROVISIONING (not VERIFIED yet)")
    void verifyCustomDomain_dnsTxtMatch_certPending_staysCertProvisioning() {
        // Given - DNS verified, but cert authority still issuing (real async path)
        premiumInstance.setCustomDomain("school.example.com");
        premiumInstance.setDomainVerifyToken("kitehub-verify=pend123");
        premiumInstance.setDomainStatus(Instance.DomainStatus.PENDING_VERIFY);
        when(instanceRepository.findById(instanceId)).thenReturn(Optional.of(premiumInstance));
        when(instanceRepository.save(any(Instance.class))).thenReturn(premiumInstance);
        when(dnsTxtLookupService.verifyTxtRecord("school.example.com", "kitehub-verify=pend123"))
            .thenReturn(true);
        when(certProvisioningService.requestCertificate("school.example.com"))
            .thenReturn(CertProvisioningResult.pending("issuance-in-flight"));

        // When
        DomainVerifyResponse response = domainService.verifyCustomDomain(instanceId);

        // Then - DNS proven, cert in flight → CERT_PROVISIONING, verifiedAt NOT yet set
        assertThat(response.getStatus()).isEqualTo(Instance.DomainStatus.CERT_PROVISIONING);
        ArgumentCaptor<Instance> captor = ArgumentCaptor.forClass(Instance.class);
        verify(instanceRepository).save(captor.capture());
        assertThat(captor.getValue().getDomainStatus()).isEqualTo(Instance.DomainStatus.CERT_PROVISIONING);
        assertThat(captor.getValue().getDomainVerifiedAt()).isNull();
    }

    @Test
    @DisplayName("verifyCustomDomain: re-verify a CERT_PROVISIONING domain re-polls cert → VERIFIED (idempotent)")
    void verifyCustomDomain_certProvisioning_repollIssued_returnsVerified() {
        // Given - instance already in CERT_PROVISIONING; re-poll now succeeds
        premiumInstance.setCustomDomain("school.example.com");
        premiumInstance.setDomainVerifyToken("kitehub-verify=cp456");
        premiumInstance.setDomainStatus(Instance.DomainStatus.CERT_PROVISIONING);
        when(instanceRepository.findById(instanceId)).thenReturn(Optional.of(premiumInstance));
        when(instanceRepository.save(any(Instance.class))).thenReturn(premiumInstance);
        when(certProvisioningService.requestCertificate("school.example.com"))
            .thenReturn(CertProvisioningResult.issued("stub-cert"));

        // When
        DomainVerifyResponse response = domainService.verifyCustomDomain(instanceId);

        // Then - cert poll succeeds → VERIFIED; DNS NOT re-checked (idempotent retry path)
        assertThat(response.getStatus()).isEqualTo(Instance.DomainStatus.VERIFIED);
        verify(certProvisioningService).requestCertificate("school.example.com");
        verify(dnsTxtLookupService, never()).verifyTxtRecord(any(), any());
    }

    @Test
    @DisplayName("verifyCustomDomain: already-VERIFIED domain is idempotent no-op (no 400, no DNS/cert call)")
    void verifyCustomDomain_alreadyVerified_idempotentNoOp() {
        // Given - instance already VERIFIED (GAP-1024: was previously a 400)
        premiumInstance.setCustomDomain("school.example.com");
        premiumInstance.setDomainVerifyToken("kitehub-verify=done789");
        premiumInstance.setDomainStatus(Instance.DomainStatus.VERIFIED);
        premiumInstance.setDomainVerifiedAt(LocalDateTime.now());
        when(instanceRepository.findById(instanceId)).thenReturn(Optional.of(premiumInstance));

        // When
        DomainVerifyResponse response = domainService.verifyCustomDomain(instanceId);

        // Then - returns current VERIFIED state, no throw, no DNS/cert lookup, no save
        assertThat(response.getStatus()).isEqualTo(Instance.DomainStatus.VERIFIED);
        verify(dnsTxtLookupService, never()).verifyTxtRecord(any(), any());
        verify(certProvisioningService, never()).requestCertificate(any());
        verify(instanceRepository, never()).save(any(Instance.class));
    }

    @Test
    @DisplayName("verifyCustomDomain: FAILED domain throws (must re-initiate per BR-DOMAIN-004)")
    void verifyCustomDomain_failedDomain_throws() {
        // Given - instance in FAILED state (timeout or cert failure)
        premiumInstance.setCustomDomain("school.example.com");
        premiumInstance.setDomainVerifyToken("kitehub-verify=failed");
        premiumInstance.setDomainStatus(Instance.DomainStatus.FAILED);
        when(instanceRepository.findById(instanceId)).thenReturn(Optional.of(premiumInstance));

        // When & Then - FAILED is not directly verifiable; tenant must re-initiate
        assertThatThrownBy(() -> domainService.verifyCustomDomain(instanceId))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("No domain verification pending");
        verify(instanceRepository, never()).save(any(Instance.class));
    }

    // =========================================================
    // removeCustomDomain tests
    // =========================================================

    @Test
    @DisplayName("removeCustomDomain: clears all domain fields")
    void removeCustomDomain_clearsDomainFields() {
        // Given
        premiumInstance.setCustomDomain("school.example.com");
        premiumInstance.setDomainVerifyToken("kitehub-verify=abc123");
        premiumInstance.setDomainStatus(Instance.DomainStatus.VERIFIED);
        premiumInstance.setDomainVerifiedAt(LocalDateTime.now());

        when(instanceRepository.findById(instanceId)).thenReturn(Optional.of(premiumInstance));
        when(instanceRepository.save(any(Instance.class))).thenReturn(premiumInstance);

        // When
        domainService.removeCustomDomain(instanceId);

        // Then
        ArgumentCaptor<Instance> captor = ArgumentCaptor.forClass(Instance.class);
        verify(instanceRepository).save(captor.capture());
        Instance saved = captor.getValue();
        assertThat(saved.getCustomDomain()).isNull();
        assertThat(saved.getDomainVerifyToken()).isNull();
        assertThat(saved.getDomainVerifiedAt()).isNull();
        assertThat(saved.getDomainStatus()).isEqualTo(Instance.DomainStatus.NONE);
    }

    @Test
    @DisplayName("removeCustomDomain: instance not found throws EntityNotFoundException")
    void removeCustomDomain_instanceNotFound_throwsEntityNotFound() {
        // Given
        UUID notFoundId = UUID.randomUUID();
        when(instanceRepository.findById(notFoundId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> domainService.removeCustomDomain(notFoundId))
            .isInstanceOf(EntityNotFoundException.class);
    }

    // =========================================================
    // getDomainStatus tests
    // =========================================================

    @Test
    @DisplayName("getDomainStatus: returns current domain status")
    void getDomainStatus_returnsCurrentStatus() {
        // Given
        premiumInstance.setCustomDomain("school.example.com");
        premiumInstance.setDomainVerifyToken("kitehub-verify=abc123");
        premiumInstance.setDomainStatus(Instance.DomainStatus.VERIFIED);
        premiumInstance.setDomainVerifiedAt(LocalDateTime.now());

        when(instanceRepository.findById(instanceId)).thenReturn(Optional.of(premiumInstance));

        // When
        DomainVerifyResponse response = domainService.getDomainStatus(instanceId);

        // Then
        assertThat(response.getCustomDomain()).isEqualTo("school.example.com");
        assertThat(response.getStatus()).isEqualTo(Instance.DomainStatus.VERIFIED);
    }

    // =========================================================
    // Helper methods
    // =========================================================

    private Instance createInstanceWithTier(PricingTier tier) {
        Instance instance = new Instance();
        instance.setId(UUID.randomUUID());
        instance.setSubdomain("school-" + tier.name().toLowerCase());
        instance.setOrganizationName("School " + tier.name());
        instance.setOwnerId(UUID.randomUUID());
        instance.setTier(tier);
        instance.setStatus(InstanceStatus.ACTIVE);
        instance.setDatabaseUrl("jdbc:postgresql://localhost:5432/test");
        instance.setDatabaseUsername("user");
        instance.setDatabasePassword("pass");
        instance.setCreatedAt(LocalDateTime.now());
        instance.setUpdatedAt(LocalDateTime.now());
        return instance;
    }
}
