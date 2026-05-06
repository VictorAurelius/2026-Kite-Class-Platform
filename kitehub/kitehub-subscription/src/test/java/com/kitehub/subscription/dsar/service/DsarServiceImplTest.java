package com.kitehub.subscription.dsar.service;

import com.kitehub.subscription.dsar.dto.DsarRequest;
import com.kitehub.subscription.dsar.entity.DsarRightType;
import com.kitehub.subscription.dsar.entity.DsarStatus;
import com.kitehub.subscription.dsar.entity.DsarTicket;
import com.kitehub.subscription.dsar.repository.DsarTicketRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link DsarServiceImpl}.
 *
 * @since Wave 26 Bucket A — GAP-353c
 */
@DisplayName("DsarServiceImpl")
class DsarServiceImplTest {

    private final DsarTicketRepository repository = Mockito.mock(DsarTicketRepository.class);
    private DsarServiceImpl service;

    @BeforeEach
    void setUp() {
        Mockito.reset(repository);
        service = new DsarServiceImpl(repository);
    }

    private DsarRequest sampleRequest() {
        return DsarRequest.builder()
                .rightType(DsarRightType.ACCESS)
                .requesterEmail("subject@example.com")
                .requesterName("Nguyen Test")
                .nationalIdLast4("1234")
                .scope("All marketing data")
                .reason("Personal review")
                .contactPreference("email")
                .build();
    }

    @Test
    @DisplayName("submitRequest persists ticket with PENDING status + 20-day SLA")
    void submitRequestPersistsPendingTicket() {
        when(repository.save(any(DsarTicket.class))).thenAnswer(inv -> {
            DsarTicket t = inv.getArgument(0);
            // Simulate JPA @PrePersist effects (cannot call package-private onCreate from this package).
            OffsetDateTime now = OffsetDateTime.now();
            if (t.getCreatedAt() == null) {
                t.setCreatedAt(now);
            }
            t.setUpdatedAt(now);
            if (t.getTicketUuid() == null) {
                t.setTicketUuid(UUID.randomUUID());
            }
            if (t.getStatus() == null) {
                t.setStatus(DsarStatus.PENDING);
            }
            if (t.getSlaDeadline() == null) {
                t.setSlaDeadline(t.getCreatedAt().plusDays(20));
            }
            t.setId(1L);
            return t;
        });

        DsarTicket saved = service.submitRequest(sampleRequest());

        ArgumentCaptor<DsarTicket> captor = ArgumentCaptor.forClass(DsarTicket.class);
        verify(repository).save(captor.capture());
        DsarTicket persisted = captor.getValue();
        assertThat(persisted.getRightType()).isEqualTo(DsarRightType.ACCESS);
        assertThat(persisted.getStatus()).isEqualTo(DsarStatus.PENDING);
        assertThat(saved.getTicketUuid()).isNotNull();
        assertThat(saved.getSlaDeadline()).isNotNull();
        // SLA must be ~20 days from now (allow ±1 day for clock drift in tests).
        OffsetDateTime now = OffsetDateTime.now();
        assertThat(saved.getSlaDeadline()).isBetween(now.plusDays(19), now.plusDays(21));
    }

    @Test
    @DisplayName("submitRequest rejects honeypot-populated request")
    void submitRequestRejectsHoneypot() {
        DsarRequest bot = sampleRequest();
        bot.setCompanyWebsite("http://spam.example");

        assertThatThrownBy(() -> service.submitRequest(bot))
                .isInstanceOf(IllegalArgumentException.class);
        verify(repository, never()).save(any(DsarTicket.class));
    }

    @Test
    @DisplayName("getTicket returns repository result")
    void getTicketReturnsRepositoryResult() {
        UUID uuid = UUID.randomUUID();
        DsarTicket ticket = DsarTicket.builder()
                .id(1L)
                .ticketUuid(uuid)
                .rightType(DsarRightType.ERASURE)
                .requesterEmail("a@b.com")
                .requesterName("X")
                .nationalIdLast4("0000")
                .status(DsarStatus.PENDING)
                .slaDeadline(OffsetDateTime.now().plusDays(20))
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();
        when(repository.findByTicketUuid(uuid)).thenReturn(Optional.of(ticket));

        Optional<DsarTicket> result = service.getTicket(uuid);

        assertThat(result).isPresent();
        assertThat(result.get().getTicketUuid()).isEqualTo(uuid);
    }

    @Test
    @DisplayName("getTicket returns empty when not found")
    void getTicketReturnsEmptyWhenNotFound() {
        UUID uuid = UUID.randomUUID();
        when(repository.findByTicketUuid(uuid)).thenReturn(Optional.empty());

        Optional<DsarTicket> result = service.getTicket(uuid);

        assertThat(result).isEmpty();
    }
}
