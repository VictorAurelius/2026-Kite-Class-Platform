package com.kiteclass.core.module.parent.audit;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit test covering the contract for {@link ParentReadAuditLogServiceImpl}.
 *
 * <p>Phase 1B foundation expectations:
 * <ul>
 *   <li>Happy path persists exactly one row with the supplied parent/child/facet
 *       and a server-side {@code readAt} timestamp.</li>
 *   <li>Repository RuntimeException is swallowed (best-effort) — must NOT
 *       propagate and break the surrounding facet read.</li>
 * </ul>
 *
 * @since 2.18.1 (Wave 18b2 — GAP-321b Phase 1B foundation)
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ParentReadAuditLogService")
class ParentReadAuditLogServiceTest {

    @Mock
    private ParentReadAuditLogRepository repository;

    @InjectMocks
    private ParentReadAuditLogServiceImpl service;

    private static final Long PARENT_ID = 10L;
    private static final Long CHILD_ID = 100L;

    @Test
    @DisplayName("logRead persists one row with the supplied identifiers + a non-null readAt")
    void logRead_persistsRow() {
        when(repository.save(any(ParentReadAuditLog.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        service.logRead(PARENT_ID, CHILD_ID, ParentFacet.ATTENDANCE);

        ArgumentCaptor<ParentReadAuditLog> captor = ArgumentCaptor.forClass(ParentReadAuditLog.class);
        verify(repository).save(captor.capture());
        ParentReadAuditLog saved = captor.getValue();
        assertThat(saved.getParentId()).isEqualTo(PARENT_ID);
        assertThat(saved.getChildId()).isEqualTo(CHILD_ID);
        assertThat(saved.getFacet()).isEqualTo(ParentFacet.ATTENDANCE);
        assertThat(saved.getReadAt()).isNotNull();
    }

    @Test
    @DisplayName("repository RuntimeException is swallowed (best-effort write)")
    void logRead_swallowsRepositoryException() {
        when(repository.save(any(ParentReadAuditLog.class)))
                .thenThrow(new RuntimeException("simulated audit-store outage"));

        assertThatCode(() -> service.logRead(PARENT_ID, CHILD_ID, ParentFacet.FEES))
                .doesNotThrowAnyException();
    }
}
