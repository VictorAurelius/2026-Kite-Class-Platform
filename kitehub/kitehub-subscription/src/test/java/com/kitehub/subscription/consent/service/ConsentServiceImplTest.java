package com.kitehub.subscription.consent.service;

import com.kitehub.subscription.consent.dto.ConsentRequest;
import com.kitehub.subscription.consent.entity.ConsentRecord;
import com.kitehub.subscription.consent.repository.ConsentRecordRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link ConsentServiceImpl}.
 *
 * @since Wave 25 Bucket A — GAP-353b
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ConsentServiceImpl")
class ConsentServiceImplTest {

    @Mock
    private ConsentRecordRepository repository;

    private ConsentServiceImpl service;

    private UUID visitorId;

    @BeforeEach
    void setUp() {
        service = new ConsentServiceImpl(repository);
        visitorId = UUID.randomUUID();
    }

    @Test
    @DisplayName("recordConsent: insert when no existing record")
    void recordConsentInsertsNewRecord() {
        when(repository.findFirstByVisitorIdOrderByCreatedAtDesc(visitorId))
                .thenReturn(Optional.empty());
        when(repository.save(any(ConsentRecord.class))).thenAnswer(inv -> {
            ConsentRecord r = inv.getArgument(0);
            r.setId(1L);
            return r;
        });

        ConsentRequest req = ConsentRequest.builder()
                .visitorId(visitorId)
                .analyticsConsented(true)
                .marketingConsented(false)
                .build();

        ConsentRecord saved = service.recordConsent(req);

        assertThat(saved.getVisitorId()).isEqualTo(visitorId);
        assertThat(saved.getAnalyticsConsented()).isTrue();
        assertThat(saved.getMarketingConsented()).isFalse();
        assertThat(saved.getEssentialConsented()).isTrue();
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getExpiresAt()).isAfter(saved.getCreatedAt());
    }

    @Test
    @DisplayName("recordConsent: idempotent — updates existing active record")
    void recordConsentUpdatesExistingActiveRecord() {
        OffsetDateTime now = OffsetDateTime.now();
        ConsentRecord existing = ConsentRecord.builder()
                .id(42L)
                .visitorId(visitorId)
                .analyticsConsented(false)
                .marketingConsented(false)
                .essentialConsented(true)
                .createdAt(now.minusDays(1))
                .updatedAt(now.minusDays(1))
                .expiresAt(now.plusMonths(11))
                .build();

        when(repository.findFirstByVisitorIdOrderByCreatedAtDesc(visitorId))
                .thenReturn(Optional.of(existing));
        when(repository.save(any(ConsentRecord.class))).thenAnswer(inv -> inv.getArgument(0));

        ConsentRequest req = ConsentRequest.builder()
                .visitorId(visitorId)
                .analyticsConsented(true)
                .marketingConsented(true)
                .build();

        ConsentRecord saved = service.recordConsent(req);

        assertThat(saved.getId()).isEqualTo(42L);
        assertThat(saved.getAnalyticsConsented()).isTrue();
        assertThat(saved.getMarketingConsented()).isTrue();
        // createdAt preserved on update
        assertThat(saved.getCreatedAt()).isEqualTo(existing.getCreatedAt());
    }

    @Test
    @DisplayName("recordConsent: revoked existing → insert NEW record")
    void recordConsentInsertsNewWhenExistingRevoked() {
        OffsetDateTime now = OffsetDateTime.now();
        ConsentRecord revoked = ConsentRecord.builder()
                .id(7L)
                .visitorId(visitorId)
                .analyticsConsented(false)
                .marketingConsented(false)
                .essentialConsented(true)
                .createdAt(now.minusDays(2))
                .updatedAt(now.minusDays(1))
                .expiresAt(now.plusMonths(10))
                .revokedAt(now.minusHours(1))
                .build();

        when(repository.findFirstByVisitorIdOrderByCreatedAtDesc(visitorId))
                .thenReturn(Optional.of(revoked));
        when(repository.save(any(ConsentRecord.class))).thenAnswer(inv -> {
            ConsentRecord r = inv.getArgument(0);
            r.setId(8L);
            return r;
        });

        ConsentRequest req = ConsentRequest.builder()
                .visitorId(visitorId)
                .analyticsConsented(true)
                .marketingConsented(true)
                .build();

        ConsentRecord saved = service.recordConsent(req);

        // New record (different id), revoked status fresh
        assertThat(saved.getId()).isEqualTo(8L);
        assertThat(saved.getRevokedAt()).isNull();
    }

    @Test
    @DisplayName("recordConsent: essential coerced to true even if client lies")
    void recordConsentForcesEssentialTrue() {
        when(repository.findFirstByVisitorIdOrderByCreatedAtDesc(visitorId))
                .thenReturn(Optional.empty());
        when(repository.save(any(ConsentRecord.class))).thenAnswer(inv -> inv.getArgument(0));

        ConsentRequest req = ConsentRequest.builder()
                .visitorId(visitorId)
                .essentialConsented(false)
                .analyticsConsented(false)
                .marketingConsented(false)
                .build();

        ConsentRecord saved = service.recordConsent(req);
        assertThat(saved.getEssentialConsented()).isTrue();
    }

    @Test
    @DisplayName("recordConsent: null visitorId → IllegalArgumentException")
    void recordConsentRejectsNullVisitorId() {
        ConsentRequest req = ConsentRequest.builder()
                .analyticsConsented(true)
                .marketingConsented(false)
                .build();
        assertThatThrownBy(() -> service.recordConsent(req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("visitorId");
    }

    @Test
    @DisplayName("findLatest: delegates to repo")
    void findLatestDelegates() {
        ConsentRecord record = ConsentRecord.builder().id(1L).visitorId(visitorId).build();
        when(repository.findFirstByVisitorIdOrderByCreatedAtDesc(visitorId))
                .thenReturn(Optional.of(record));

        Optional<ConsentRecord> result = service.findLatest(visitorId);
        assertThat(result).isPresent().get().isEqualTo(record);
    }

    @Test
    @DisplayName("revoke: sets revokedAt + flips analytics/marketing false")
    void revokeUpdatesRecord() {
        OffsetDateTime now = OffsetDateTime.now();
        ConsentRecord active = ConsentRecord.builder()
                .id(99L)
                .visitorId(visitorId)
                .analyticsConsented(true)
                .marketingConsented(true)
                .essentialConsented(true)
                .createdAt(now.minusDays(1))
                .updatedAt(now.minusDays(1))
                .expiresAt(now.plusMonths(11))
                .build();

        when(repository.findFirstByVisitorIdOrderByCreatedAtDesc(visitorId))
                .thenReturn(Optional.of(active));
        when(repository.save(any(ConsentRecord.class))).thenAnswer(inv -> inv.getArgument(0));

        ConsentRecord result = service.revoke(visitorId);

        ArgumentCaptor<ConsentRecord> captor = ArgumentCaptor.forClass(ConsentRecord.class);
        verify(repository).save(captor.capture());
        ConsentRecord saved = captor.getValue();
        assertThat(saved.getRevokedAt()).isNotNull();
        assertThat(saved.getAnalyticsConsented()).isFalse();
        assertThat(saved.getMarketingConsented()).isFalse();
        assertThat(result.isRevoked()).isTrue();
    }

    @Test
    @DisplayName("revoke: idempotent — already-revoked record unchanged")
    void revokeIdempotent() {
        OffsetDateTime past = OffsetDateTime.now().minusDays(1);
        ConsentRecord revoked = ConsentRecord.builder()
                .id(1L)
                .visitorId(visitorId)
                .analyticsConsented(false)
                .marketingConsented(false)
                .essentialConsented(true)
                .createdAt(past.minusDays(1))
                .updatedAt(past)
                .expiresAt(past.plusMonths(11))
                .revokedAt(past)
                .build();
        when(repository.findFirstByVisitorIdOrderByCreatedAtDesc(visitorId))
                .thenReturn(Optional.of(revoked));

        ConsentRecord result = service.revoke(visitorId);

        assertThat(result.isRevoked()).isTrue();
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("revoke: missing record → NoSuchElementException")
    void revokeMissingThrows() {
        when(repository.findFirstByVisitorIdOrderByCreatedAtDesc(visitorId))
                .thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.revoke(visitorId))
                .isInstanceOf(NoSuchElementException.class);
    }

    @Test
    @DisplayName("findLatest: null id → IllegalArgumentException")
    void findLatestRejectsNull() {
        assertThatThrownBy(() -> service.findLatest(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("revoke: null id → IllegalArgumentException")
    void revokeRejectsNull() {
        assertThatThrownBy(() -> service.revoke(null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
