package com.kiteclass.core.common.outbox;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OutboxEventPublisherTest {

    @Mock
    private OutboxEventRepository repository;

    @Mock
    private EventDispatcher dispatcher;

    @InjectMocks
    private OutboxEventPublisher publisher;

    private OutboxEvent pending(long id) {
        OutboxEvent e = OutboxEvent.builder()
                .eventType("instance.deployed")
                .aggregateType("FrontendInstance")
                .aggregateId(String.valueOf(id))
                .payload("{}")
                .status(OutboxStatus.PENDING)
                .retryCount(0)
                .nextAttemptAt(Instant.now())
                .build();
        e.setId(id);
        return e;
    }

    @Test
    void drain_empty_batch_is_noop() throws Exception {
        when(repository.findDispatchable(any(Instant.class), any(Pageable.class)))
                .thenReturn(List.of());

        publisher.drain();

        verify(dispatcher, never()).dispatch(any());
        verify(repository, never()).save(any());
    }

    @Test
    void drain_happy_path_marks_published() throws Exception {
        OutboxEvent e = pending(1L);
        when(repository.findDispatchable(any(Instant.class), any(Pageable.class)))
                .thenReturn(List.of(e));

        publisher.drain();

        verify(dispatcher).dispatch(e);
        ArgumentCaptor<OutboxEvent> saved = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(repository).save(saved.capture());
        assertThat(saved.getValue().getStatus()).isEqualTo(OutboxStatus.PUBLISHED);
        assertThat(saved.getValue().getPublishedAt()).isNotNull();
    }

    @Test
    void drain_on_dispatch_exception_bumps_retry_and_keeps_pending() throws Exception {
        OutboxEvent e = pending(2L);
        when(repository.findDispatchable(any(Instant.class), any(Pageable.class)))
                .thenReturn(List.of(e));
        doThrow(new DispatchException("broker down")).when(dispatcher).dispatch(e);

        publisher.drain();

        ArgumentCaptor<OutboxEvent> saved = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(repository).save(saved.capture());
        assertThat(saved.getValue().getStatus()).isEqualTo(OutboxStatus.PENDING);
        assertThat(saved.getValue().getRetryCount()).isEqualTo(1);
        assertThat(saved.getValue().getLastError()).isEqualTo("broker down");
    }

    @Test
    void drain_unexpected_runtime_still_records_failure() throws Exception {
        OutboxEvent e = pending(3L);
        when(repository.findDispatchable(any(Instant.class), any(Pageable.class)))
                .thenReturn(List.of(e));
        doThrow(new RuntimeException("boom")).when(dispatcher).dispatch(e);

        publisher.drain();

        ArgumentCaptor<OutboxEvent> saved = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(repository).save(saved.capture());
        assertThat(saved.getValue().getRetryCount()).isEqualTo(1);
        assertThat(saved.getValue().getLastError()).isEqualTo("boom");
    }

    @Test
    void drain_processes_multiple_rows_independently() throws Exception {
        OutboxEvent a = pending(1L);
        OutboxEvent b = pending(2L);
        when(repository.findDispatchable(any(Instant.class), any(Pageable.class)))
                .thenReturn(List.of(a, b));
        doNothing().when(dispatcher).dispatch(eq(a));
        doThrow(new DispatchException("skip b")).when(dispatcher).dispatch(eq(b));

        publisher.drain();

        verify(repository).save(a);
        verify(repository).save(b);
        assertThat(a.getStatus()).isEqualTo(OutboxStatus.PUBLISHED);
        assertThat(b.getStatus()).isEqualTo(OutboxStatus.PENDING);
        assertThat(b.getRetryCount()).isEqualTo(1);
    }
}
