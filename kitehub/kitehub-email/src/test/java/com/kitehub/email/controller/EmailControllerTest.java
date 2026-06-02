package com.kitehub.email.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kitehub.email.dto.EmailRequest;
import com.kitehub.email.dto.EmailResponse;
import com.kitehub.email.service.EmailIdempotencyGuard;
import com.kitehub.email.service.EmailSender;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests for EmailController.
 *
 * <p>GAP-840 Wave local-doable-6 Bucket H — adds HTTP idempotency coverage via a
 * scripted {@link EmailIdempotencyGuard} {@link MockitoBean} that mimics
 * Caffeine-only first-seen-vs-duplicate semantics for the keys the controller
 * derives, so the controller's dedup logic is exercised without standing up
 * Redis or the full guard bean graph.
 *
 * @since 1.0
 */
@WebMvcTest(EmailController.class)
class EmailControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private EmailSender emailSender;

    @MockitoBean
    private EmailIdempotencyGuard idempotencyGuard;

    /**
     * Default-stubs idempotency guard so each test starts with "first seen = true"
     * for any key. Tests that exercise dedup override this with their own scripted
     * sequence via {@link org.mockito.Mockito#when}.
     */
    @BeforeEach
    void stubGuardAsAlwaysFirstSeen() {
        when(idempotencyGuard.computeKey(any(), any(), any(), any(), any()))
                .thenAnswer(inv -> "computed-key-" + System.nanoTime());
        when(idempotencyGuard.markIfFirstSeen(anyString())).thenReturn(true);
    }

    @Test
    void testSendTemplatedEmail() throws Exception {
        // Given
        Map<String, Object> variables = new HashMap<>();
        variables.put("organizationName", "Test Org");

        EmailRequest request = EmailRequest.builder()
                .to("test@example.com")
                .subject("Welcome")
                .templateName("welcome")
                .variables(variables)
                .build();

        EmailResponse mockResponse = EmailResponse.builder()
                .messageId("mock-123")
                .status("MOCK")
                .sentAt(LocalDateTime.now())
                .build();

        when(emailSender.sendTemplatedEmail(any(EmailRequest.class)))
                .thenReturn(mockResponse);

        // When & Then
        mockMvc.perform(post("/api/platform/emails/send")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.messageId").value("mock-123"))
                .andExpect(jsonPath("$.status").value("MOCK"));
    }

    @Test
    void testSendHtmlEmail() throws Exception {
        // Given
        EmailRequest request = EmailRequest.builder()
                .to("test@example.com")
                .subject("Test")
                .htmlBody("<h1>Test</h1>")
                .build();

        EmailResponse mockResponse = EmailResponse.builder()
                .messageId("mock-456")
                .status("MOCK")
                .sentAt(LocalDateTime.now())
                .build();

        when(emailSender.sendEmail(anyString(), anyString(), anyString()))
                .thenReturn(mockResponse);

        // When & Then
        mockMvc.perform(post("/api/platform/emails/send")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.messageId").value("mock-456"))
                .andExpect(jsonPath("$.status").value("MOCK"));
    }

    @Test
    void testSendEmail_BadRequest_NoBody() throws Exception {
        // Given - request with neither templateName nor htmlBody
        EmailRequest request = EmailRequest.builder()
                .to("test@example.com")
                .subject("Test")
                .build();

        // When & Then
        mockMvc.perform(post("/api/platform/emails/send")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testIdempotencyKeyHeader_dedupesSecondCall() throws Exception {
        // GAP-840 — same Idempotency-Key on retry → guard returns false (duplicate)
        // → controller serves cached response, emailSender NOT invoked again.
        String idempotencyKey = "stripe-style-key-42";
        EmailRequest request = EmailRequest.builder()
                .to("idem@example.com")
                .subject("Welcome via idempotency")
                .templateName("welcome")
                .variables(new HashMap<>(Map.of("organizationName", "Test Org")))
                .build();

        EmailResponse mockResponse = EmailResponse.builder()
                .messageId("idem-msg-1")
                .status("MOCK")
                .sentAt(LocalDateTime.now())
                .build();

        // Script guard: computeKey deterministic; first markIfFirstSeen=true, then false.
        AtomicReference<String> stableKey = new AtomicReference<>("stable-key-stripe-style");
        when(idempotencyGuard.computeKey(any(), any(), any(), any(), any()))
                .thenAnswer(inv -> stableKey.get());
        when(idempotencyGuard.markIfFirstSeen(anyString()))
                .thenReturn(true)
                .thenReturn(false);
        when(emailSender.sendTemplatedEmail(any(EmailRequest.class))).thenReturn(mockResponse);

        // First call — proceeds; emailSender invoked exactly once
        mockMvc.perform(post("/api/platform/emails/send")
                        .header("Idempotency-Key", idempotencyKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.messageId").value("idem-msg-1"))
                .andExpect(jsonPath("$.status").value("MOCK"));

        // Second call same key — guard says duplicate → cached response replayed
        mockMvc.perform(post("/api/platform/emails/send")
                        .header("Idempotency-Key", idempotencyKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.messageId").value("idem-msg-1"))
                .andExpect(jsonPath("$.status").value("MOCK"));

        // emailSender invoked exactly ONCE (second call deduped)
        verify(emailSender, times(1)).sendTemplatedEmail(any(EmailRequest.class));
    }

    @Test
    void testContentDerivedKey_dedupesIdenticalRetry_whenHeaderAbsent() throws Exception {
        // GAP-840 — without explicit Idempotency-Key header, identical body re-POSTs
        // produce identical derived key → second call collides → dedup engages.
        EmailRequest request = EmailRequest.builder()
                .to("derived@example.com")
                .subject("Content-derived dedup")
                .templateName("welcome")
                .variables(new HashMap<>(Map.of("organizationName", "Test Org")))
                .build();

        EmailResponse mockResponse = EmailResponse.builder()
                .messageId("derived-msg-1")
                .status("MOCK")
                .sentAt(LocalDateTime.now())
                .build();

        when(idempotencyGuard.computeKey(any(), any(), any(), any(), any()))
                .thenReturn("stable-content-hash-7777");
        when(idempotencyGuard.markIfFirstSeen(anyString()))
                .thenReturn(true)
                .thenReturn(false);
        when(emailSender.sendTemplatedEmail(any(EmailRequest.class))).thenReturn(mockResponse);

        // First call — no header, derived key consulted, guard says first-seen
        mockMvc.perform(post("/api/platform/emails/send")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.messageId").value("derived-msg-1"));

        // Second call — same body, no header. Derived key collides; guard says duplicate.
        mockMvc.perform(post("/api/platform/emails/send")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.messageId").value("derived-msg-1"));

        verify(emailSender, times(1)).sendTemplatedEmail(any(EmailRequest.class));
    }
}
