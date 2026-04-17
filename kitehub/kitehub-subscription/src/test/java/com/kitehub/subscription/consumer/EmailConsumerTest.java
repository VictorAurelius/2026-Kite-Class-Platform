package com.kitehub.subscription.consumer;

import com.kitehub.platform.domain.entity.EmailSentLog;
import com.kitehub.subscription.dto.EmailEvent;
import com.kitehub.subscription.repository.EmailSentLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for EmailConsumer.
 *
 * @since 1.0.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("EmailConsumer Unit Tests")
class EmailConsumerTest {

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private EmailSentLogRepository emailSentLogRepository;

    @InjectMocks
    private EmailConsumer emailConsumer;

    private EmailEvent sampleEvent;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(emailConsumer, "emailServiceUrl", "http://localhost:8083");

        sampleEvent = EmailEvent.builder()
                .instanceId(UUID.randomUUID())
                .to("test@example.com")
                .subject("Test Subject")
                .templateName("trial-expiration-warning")
                .variables(Map.of("organizationName", "Test Org", "daysRemaining", 3))
                .emailType("trial-warning")
                .build();
    }

    @Nested
    @DisplayName("handleEmailEvent - Main Queue")
    class HandleEmailEvent {

        @Test
        @DisplayName("Should send HTTP request to email service on success")
        void shouldSendHttpRequestOnSuccess() {
            when(restTemplate.postForEntity(anyString(), any(), eq(String.class)))
                    .thenReturn(new ResponseEntity<>("OK", HttpStatus.OK));

            emailConsumer.handleEmailEvent(sampleEvent);

            verify(restTemplate).postForEntity(
                    eq("http://localhost:8083/api/platform/emails/send"),
                    any(),
                    eq(String.class));
        }

        @Test
        @DisplayName("Should throw on HTTP failure to trigger retry")
        void shouldThrowOnHttpFailure() {
            when(restTemplate.postForEntity(anyString(), any(), eq(String.class)))
                    .thenThrow(new RestClientException("Connection refused"));

            assertThatThrownBy(() -> emailConsumer.handleEmailEvent(sampleEvent))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Email send failed");
        }

        @Test
        @DisplayName("Should throw on non-2xx status")
        void shouldThrowOnNon2xxStatus() {
            when(restTemplate.postForEntity(anyString(), any(), eq(String.class)))
                    .thenReturn(new ResponseEntity<>("Error", HttpStatus.INTERNAL_SERVER_ERROR));

            assertThatThrownBy(() -> emailConsumer.handleEmailEvent(sampleEvent))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Email service returned");
        }
    }

    @Nested
    @DisplayName("handleFailedEmail - DLQ")
    class HandleFailedEmail {

        @Test
        @DisplayName("Should record failed email in sent log")
        void shouldRecordFailedEmailInSentLog() {
            emailConsumer.handleFailedEmail(sampleEvent);

            ArgumentCaptor<EmailSentLog> captor = ArgumentCaptor.forClass(EmailSentLog.class);
            verify(emailSentLogRepository).save(captor.capture());

            EmailSentLog saved = captor.getValue();
            assertThat(saved.getInstanceId()).isEqualTo(sampleEvent.getInstanceId());
            assertThat(saved.getEmailType()).isEqualTo("trial-warning:FAILED");
            assertThat(saved.getRecipient()).isEqualTo("test@example.com");
            assertThat(saved.getSentAt()).isNotNull();
        }

        @Test
        @DisplayName("Should not throw when recording failure fails")
        void shouldNotThrowWhenRecordingFails() {
            when(emailSentLogRepository.save(any()))
                    .thenThrow(new RuntimeException("DB error"));

            // Should not throw
            emailConsumer.handleFailedEmail(sampleEvent);
        }
    }
}
