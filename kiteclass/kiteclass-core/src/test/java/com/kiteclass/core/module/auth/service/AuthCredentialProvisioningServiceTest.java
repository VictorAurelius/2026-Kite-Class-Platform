package com.kiteclass.core.module.auth.service;

import com.kiteclass.core.common.exception.BusinessException;
import com.kiteclass.core.module.auth.entity.AuthCredential;
import com.kiteclass.core.module.auth.repository.AuthCredentialRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link AuthCredentialProvisioningService} (Wave auth-2, GAP-1010/1011/1013).
 *
 * <p>Covers: idempotent provision (same-tenant) + cross-tenant reject (GAP-1011 Option A) +
 * setPassword upsert + entity-mismatch reject (GAP-1013a) + disableCredential (GAP-1013b).
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AuthCredentialProvisioningService")
class AuthCredentialProvisioningServiceTest {

    @Mock
    private AuthCredentialRepository repository;

    private AuthCredentialProvisioningService service;

    private final UUID TENANT_A = UUID.fromString("aaaaaaaa-0000-0000-0000-000000000001");
    private final UUID TENANT_B = UUID.fromString("bbbbbbbb-0000-0000-0000-000000000002");

    private static final String EMAIL = "parent@example.com";
    private static final String PASSWORD = "Password1!";

    @BeforeEach
    void setUp() {
        service = new AuthCredentialProvisioningService(repository);
    }

    private AuthCredential existing(String entityType, Long entityId, UUID tenant) {
        return AuthCredential.builder()
                .id(1L)
                .userUuid(UUID.randomUUID())
                .entityType(entityType)
                .entityId(entityId)
                .email(EMAIL)
                .passwordHash("$2a$10$existinghashexistinghashexistinghashexistinghashexa")
                .instanceId(tenant)
                .enabled(true)
                .build();
    }

    @Nested
    @DisplayName("provision()")
    class Provision {

        @Test
        @DisplayName("new email → creates + saves credential")
        void provision_newEmail_creates() {
            when(repository.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.empty());
            when(repository.save(any(AuthCredential.class))).thenAnswer(inv -> inv.getArgument(0));

            AuthCredential result = service.provision("PARENT", 7L, EMAIL, TENANT_A, PASSWORD);

            assertThat(result.getEntityType()).isEqualTo("PARENT");
            assertThat(result.getEntityId()).isEqualTo(7L);
            assertThat(result.getInstanceId()).isEqualTo(TENANT_A);
            assertThat(result.isEnabled()).isTrue();
            assertThat(result.getPasswordHash()).isNotEqualTo(PASSWORD); // BCrypt-encoded
            verify(repository).save(any(AuthCredential.class));
        }

        @Test
        @DisplayName("same email + same tenant → idempotent, keeps existing, no save")
        void provision_sameTenant_idempotent() {
            AuthCredential pre = existing("PARENT", 7L, TENANT_A);
            when(repository.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.of(pre));

            AuthCredential result = service.provision("PARENT", 7L, EMAIL, TENANT_A, "NewPass2@");

            assertThat(result).isSameAs(pre);
            assertThat(result.getPasswordHash()).startsWith("$2a$10$existinghash"); // unchanged
            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("same email + different tenant → 409 AUTH_EMAIL_CROSS_TENANT (GAP-1011)")
        void provision_crossTenant_rejected() {
            when(repository.findByEmailIgnoreCase(EMAIL))
                    .thenReturn(Optional.of(existing("PARENT", 7L, TENANT_A)));

            assertThatThrownBy(() -> service.provision("PARENT", 9L, EMAIL, TENANT_B, PASSWORD))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("code",
                            AuthCredentialProvisioningService.ERR_EMAIL_CROSS_TENANT)
                    .hasFieldOrPropertyWithValue("status", HttpStatus.CONFLICT);

            verify(repository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("setPassword()")
    class SetPassword {

        @Test
        @DisplayName("new email → creates credential with new password")
        void setPassword_newEmail_creates() {
            when(repository.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.empty());
            when(repository.save(any(AuthCredential.class))).thenAnswer(inv -> inv.getArgument(0));

            AuthCredential result = service.setPassword("TEACHER", 42L, EMAIL, TENANT_A, PASSWORD);

            assertThat(result.getEntityType()).isEqualTo("TEACHER");
            assertThat(result.getEntityId()).isEqualTo(42L);
            assertThat(result.getPasswordHash()).isNotBlank();
            verify(repository).save(any(AuthCredential.class));
        }

        @Test
        @DisplayName("existing same entity + same tenant → rotates password")
        void setPassword_sameEntity_rotates() {
            AuthCredential pre = existing("TEACHER", 42L, TENANT_A);
            String oldHash = pre.getPasswordHash();
            when(repository.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.of(pre));
            when(repository.save(any(AuthCredential.class))).thenAnswer(inv -> inv.getArgument(0));

            AuthCredential result = service.setPassword("TEACHER", 42L, EMAIL, TENANT_A, "Rotated9#");

            assertThat(result.getPasswordHash()).isNotEqualTo(oldHash);
            assertThat(result.getUpdatedAt()).isNotNull();
            verify(repository).save(pre);
        }

        @Test
        @DisplayName("existing different entity → 409 AUTH_CREDENTIAL_ENTITY_MISMATCH (GAP-1013a)")
        void setPassword_entityMismatch_rejected() {
            when(repository.findByEmailIgnoreCase(EMAIL))
                    .thenReturn(Optional.of(existing("PARENT", 7L, TENANT_A)));

            assertThatThrownBy(() -> service.setPassword("TEACHER", 42L, EMAIL, TENANT_A, PASSWORD))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("code",
                            AuthCredentialProvisioningService.ERR_CREDENTIAL_ENTITY_MISMATCH)
                    .hasFieldOrPropertyWithValue("status", HttpStatus.CONFLICT);

            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("existing different tenant → 409 AUTH_EMAIL_CROSS_TENANT (GAP-1011)")
        void setPassword_crossTenant_rejected() {
            when(repository.findByEmailIgnoreCase(EMAIL))
                    .thenReturn(Optional.of(existing("TEACHER", 42L, TENANT_A)));

            assertThatThrownBy(() -> service.setPassword("TEACHER", 42L, EMAIL, TENANT_B, PASSWORD))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("code",
                            AuthCredentialProvisioningService.ERR_EMAIL_CROSS_TENANT);

            verify(repository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("disableCredential() (GAP-1013b)")
    class DisableCredential {

        @Test
        @DisplayName("enabled credential → flips enabled=false + saves")
        void disable_enabled_setsFalse() {
            AuthCredential pre = existing("TEACHER", 42L, TENANT_A);
            when(repository.findByEntityTypeAndEntityId("TEACHER", 42L)).thenReturn(Optional.of(pre));
            when(repository.save(any(AuthCredential.class))).thenAnswer(inv -> inv.getArgument(0));

            service.disableCredential("TEACHER", 42L);

            ArgumentCaptor<AuthCredential> captor = ArgumentCaptor.forClass(AuthCredential.class);
            verify(repository).save(captor.capture());
            assertThat(captor.getValue().isEnabled()).isFalse();
            assertThat(captor.getValue().getUpdatedAt()).isNotNull();
        }

        @Test
        @DisplayName("no credential for entity → no-op (admin never provisioned login)")
        void disable_absent_noOp() {
            when(repository.findByEntityTypeAndEntityId("TEACHER", 99L)).thenReturn(Optional.empty());

            service.disableCredential("TEACHER", 99L);

            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("already-disabled credential → no-op (idempotent)")
        void disable_alreadyDisabled_noOp() {
            AuthCredential pre = existing("TEACHER", 42L, TENANT_A);
            pre.setEnabled(false);
            when(repository.findByEntityTypeAndEntityId("TEACHER", 42L)).thenReturn(Optional.of(pre));

            service.disableCredential("TEACHER", 42L);

            verify(repository, never()).save(any());
        }
    }
}
