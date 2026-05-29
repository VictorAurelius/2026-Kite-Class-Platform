package com.kiteclass.core.module.parent.payment;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kiteclass.core.module.parent.notification.ZaloOaNotificationService;
import com.kiteclass.core.module.parent.repository.ParentStudentLinkRepository;
import com.kiteclass.core.module.payment.dto.CreatePaymentRequest;
import com.kiteclass.core.module.payment.dto.PaymentResponse;
import com.kiteclass.core.module.payment.enums.PaymentMethod;
import com.kiteclass.core.module.payment.service.PaymentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Wave 105 Bucket D — Web-slice tests for {@link ParentPaymentController}.
 *
 * <p>Covers acceptance criteria per Wave 105 plan §3 Bucket D:
 * <ul>
 *   <li>AC3 — Multi-child authz: Linh có 2 con, chỉ xem được con A khi click vào con A;
 *       spoof {@code childId=B} → 403 PARENT_NOT_LINKED.</li>
 *   <li>AC4 — VietQR idempotency: pay 2× → 1 payment row + 1 QR code
 *       (Idempotency-Key header).</li>
 * </ul>
 *
 * <p>Per `pre-handoff-self-test-completeness.md` §2.6 Payment flow check (d)
 * idempotency + §2.7 multi-tenant data isolation.
 *
 * @since 3.0.0 (Wave 105 Bucket D)
 */
@WebMvcTest(ParentPaymentController.class)
@AutoConfigureMockMvc
@Import({ParentPaymentControllerTest.TestSecurityConfig.class,
        ParentPaymentControllerTest.MockConfig.class})
@ActiveProfiles("test")
@DisplayName("ParentPaymentController — Wave 105 Bucket D Parent persona walk")
class ParentPaymentControllerTest {

    @TestConfiguration
    static class TestSecurityConfig {
        @Bean
        SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
            http.csrf(AbstractHttpConfigurer::disable)
                    .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
            return http.build();
        }
    }

    @TestConfiguration
    static class MockConfig {
        @Bean @Primary PaymentService paymentService() { return Mockito.mock(PaymentService.class); }
        @Bean @Primary ParentStudentLinkRepository linkRepository() {
            return Mockito.mock(ParentStudentLinkRepository.class);
        }
        @Bean @Primary PaymentIdempotencyService idempotencyService() {
            return Mockito.mock(PaymentIdempotencyService.class);
        }
        @Bean @Primary ZaloOaNotificationService zaloOaNotificationService() {
            return Mockito.mock(ZaloOaNotificationService.class);
        }
    }

    @Autowired private MockMvc mockMvc;
    @Autowired private PaymentService paymentService;
    @Autowired private ParentStudentLinkRepository linkRepository;
    @Autowired private PaymentIdempotencyService idempotencyService;
    @Autowired private ZaloOaNotificationService zaloOaNotificationService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    // Linh = parent with 2 children. Per VN-localization §3 sample data.
    private static final Long PARENT_LINH_ID = 10L;
    private static final Long CHILD_A_ID = 100L;  // linked
    private static final Long CHILD_B_ID = 200L;  // NOT linked — cross-child spoof target
    private static final String VALID_KEY = "550e8400-e29b-41d4-a716-446655440000";

    private CreatePaymentRequest sampleRequest() {
        return CreatePaymentRequest.builder()
                .invoiceId(1L)
                .amount(new BigDecimal("1500000"))  // 1.500.000đ per VN-localization §1
                .paymentMethod(PaymentMethod.VNPAY)
                .build();
    }

    @BeforeEach
    void resetMocks() {
        Mockito.reset(paymentService, linkRepository, idempotencyService, zaloOaNotificationService);
        // Default: validKey returns the header value as-is for valid format.
        when(idempotencyService.requireValidKey(VALID_KEY)).thenReturn(VALID_KEY);
    }

    @Test
    @DisplayName("AC4 — first request: 201 + X-Payment-Idempotent-Replay: false + Zalo OA stub called")
    void firstRequest_creates_payment_and_records_zalo_stub() throws Exception {
        // Linh linked to Child A
        when(linkRepository.existsByParentIdAndStudentIdAndDeletedFalse(PARENT_LINH_ID, CHILD_A_ID))
                .thenReturn(true);
        // No prior idempotency mapping (first write)
        when(idempotencyService.lookup(anyString(), eq(VALID_KEY)))
                .thenReturn(Optional.empty());
        // Underlying PaymentService creates payment with REAL parentId (not 1L)
        PaymentResponse created = PaymentResponse.builder()
                .id(999L)
                .invoiceId(1L)
                .amount(new BigDecimal("1500000"))
                .paymentMethod(PaymentMethod.VNPAY)
                .build();
        // GAP-795: payment actor = X-User-Id UUID via UserContext (null in @WebMvcTest
        // slice — no TenantFilterInterceptor). Assert call shape, not the actor value.
        when(paymentService.createPayment(any(CreatePaymentRequest.class), any()))
                .thenReturn(created);
        // First-write insert succeeds (no race)
        when(idempotencyService.recordFirstWrite(anyString(), eq(VALID_KEY),
                eq(PARENT_LINH_ID), eq(1L), eq(999L), anyString()))
                .thenReturn(true);

        mockMvc.perform(post("/api/v1/parent/children/{childId}/payments", CHILD_A_ID)
                        .header("X-User-Reference-Id", PARENT_LINH_ID)
                        .header("Idempotency-Key", VALID_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleRequest())))
                .andExpect(status().isCreated())
                .andExpect(header().string("X-Payment-Idempotent-Replay", "false"))
                .andExpect(jsonPath("$.data.id").value(999));

        // Zalo OA stub called per AC5 (3 events scope — payment confirm)
        verify(zaloOaNotificationService, times(1))
                .recordPaymentConfirm(eq(PARENT_LINH_ID), eq(1L), anyLong(), anyString());
        // createPayment invoked once (actor = X-User-Id UUID via UserContext per GAP-795;
        // not asserted here — UserContext unpopulated in web slice).
        verify(paymentService, times(1))
                .createPayment(any(CreatePaymentRequest.class), any());
    }

    @Test
    @DisplayName("AC4 — second request same key: 200 + X-Payment-Idempotent-Replay: true + no new payment")
    void replay_returns_cached_payment_no_new_charge() throws Exception {
        when(linkRepository.existsByParentIdAndStudentIdAndDeletedFalse(PARENT_LINH_ID, CHILD_A_ID))
                .thenReturn(true);
        // Idempotency mapping exists from first request
        when(idempotencyService.lookup(anyString(), eq(VALID_KEY)))
                .thenReturn(Optional.of(new PaymentIdempotencyService.IdempotentResult(
                        999L, "VIETQR-STUB|paymentId=999|...")));
        when(paymentService.getPaymentById(999L)).thenReturn(
                PaymentResponse.builder().id(999L).invoiceId(1L)
                        .amount(new BigDecimal("1500000"))
                        .paymentMethod(PaymentMethod.VNPAY).build());

        mockMvc.perform(post("/api/v1/parent/children/{childId}/payments", CHILD_A_ID)
                        .header("X-User-Reference-Id", PARENT_LINH_ID)
                        .header("Idempotency-Key", VALID_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleRequest())))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Payment-Idempotent-Replay", "true"))
                .andExpect(jsonPath("$.data.id").value(999));

        // CRITICAL: createPayment never called on replay (1 payment row only)
        verify(paymentService, Mockito.never()).createPayment(any(), any());
        // Zalo OA also not called on replay (avoid duplicate notifications)
        verify(zaloOaNotificationService, Mockito.never())
                .recordPaymentConfirm(anyLong(), anyLong(), anyLong(), anyString());
    }

    @Test
    @DisplayName("AC3 — cross-child spoof: Linh linked to A pays for B → 403 PARENT_NOT_LINKED")
    void cross_child_spoof_returns_403() throws Exception {
        // Linh NOT linked to Child B
        when(linkRepository.existsByParentIdAndStudentIdAndDeletedFalse(PARENT_LINH_ID, CHILD_B_ID))
                .thenReturn(false);

        mockMvc.perform(post("/api/v1/parent/children/{childId}/payments", CHILD_B_ID)
                        .header("X-User-Reference-Id", PARENT_LINH_ID)
                        .header("Idempotency-Key", VALID_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleRequest())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("PARENT_NOT_LINKED"));

        // PaymentService MUST NOT be invoked for unlinked child
        verify(paymentService, Mockito.never()).createPayment(any(), any());
        verify(idempotencyService, Mockito.never())
                .lookup(anyString(), anyString());
    }

    @Test
    @DisplayName("Idempotency-Key header missing → 400 IDEMPOTENCY_KEY_REQUIRED")
    void missing_idempotency_key_returns_400() throws Exception {
        // requireValidKey throws on null — simulate that contract
        when(idempotencyService.requireValidKey(null)).thenThrow(
                new com.kiteclass.core.common.exception.BusinessException(
                        "IDEMPOTENCY_KEY_REQUIRED", org.springframework.http.HttpStatus.BAD_REQUEST));

        mockMvc.perform(post("/api/v1/parent/children/{childId}/payments", CHILD_A_ID)
                        .header("X-User-Reference-Id", PARENT_LINH_ID)
                        // No Idempotency-Key header
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleRequest())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("IDEMPOTENCY_KEY_REQUIRED"));
    }

    @Test
    @DisplayName("Auth missing (gateway didn't forward parentId) → 401 AUTH_REQUIRED")
    void missing_parent_header_returns_401() throws Exception {
        mockMvc.perform(post("/api/v1/parent/children/{childId}/payments", CHILD_A_ID)
                        // No X-User-Reference-Id header
                        .header("Idempotency-Key", VALID_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleRequest())))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_REQUIRED"));
    }
}
