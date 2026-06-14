package com.kitehub.subscription.service;

import com.kitehub.subscription.dto.vietqr.VietQRRequest;
import com.kitehub.subscription.dto.vietqr.VietQRResponse;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.*;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for VietQRService.
 *
 * @author KiteHub Team
 * @since 1.0.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("VietQRService Unit Tests")
class VietQRServiceTest {

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private VietQRService vietQRService;

    private static final String BANK_CODE = "VCB";
    private static final String ACCOUNT_NUMBER = "1234567890";
    private static final String ACCOUNT_NAME = "CONG TY KITECLASS";
    private static final String API_URL = "https://api.vietqr.io/v2/generate";
    private static final String DEFAULT_TEMPLATE = "compact";

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(vietQRService, "bankCode", BANK_CODE);
        ReflectionTestUtils.setField(vietQRService, "accountNumber", ACCOUNT_NUMBER);
        ReflectionTestUtils.setField(vietQRService, "accountName", ACCOUNT_NAME);
        ReflectionTestUtils.setField(vietQRService, "apiUrl", API_URL);
        ReflectionTestUtils.setField(vietQRService, "apiKey", null);
        ReflectionTestUtils.setField(vietQRService, "defaultTemplate", DEFAULT_TEMPLATE);
    }

    @Test
    @DisplayName("Should generate QR code successfully via API")
    void shouldGenerateQRCodeSuccessfully() {
        // Given
        UUID paymentId = UUID.randomUUID();
        UUID subscriptionId = UUID.randomUUID();
        Long amount = 100000L;
        String expectedQrUrl = "https://img.vietqr.io/image/qr-code.jpg";

        VietQRResponse.VietQRData responseData = new VietQRResponse.VietQRData();
        responseData.setQrDataUrl(expectedQrUrl);

        VietQRResponse mockResponse = new VietQRResponse();
        mockResponse.setCode("00");
        mockResponse.setDescription("Success");
        mockResponse.setData(responseData);

        ResponseEntity<VietQRResponse> responseEntity = ResponseEntity.ok(mockResponse);

        when(restTemplate.exchange(
            eq(API_URL),
            eq(HttpMethod.POST),
            any(HttpEntity.class),
            eq(VietQRResponse.class)
        )).thenReturn(responseEntity);

        // When
        String result = vietQRService.generateQRCode(paymentId, amount, subscriptionId);

        // Then
        assertThat(result).isEqualTo(expectedQrUrl);
        verify(restTemplate).exchange(
            eq(API_URL),
            eq(HttpMethod.POST),
            any(HttpEntity.class),
            eq(VietQRResponse.class)
        );
    }

    @Test
    @DisplayName("Should include API key in headers when provided")
    void shouldIncludeApiKeyWhenProvided() {
        // Given
        String apiKey = "test-api-key";
        ReflectionTestUtils.setField(vietQRService, "apiKey", apiKey);

        UUID paymentId = UUID.randomUUID();
        UUID subscriptionId = UUID.randomUUID();
        Long amount = 100000L;

        VietQRResponse.VietQRData responseData = new VietQRResponse.VietQRData();
        responseData.setQrDataUrl("https://img.vietqr.io/image/qr-code.jpg");

        VietQRResponse mockResponse = new VietQRResponse();
        mockResponse.setCode("00");
        mockResponse.setData(responseData);

        ResponseEntity<VietQRResponse> responseEntity = ResponseEntity.ok(mockResponse);

        when(restTemplate.exchange(
            anyString(),
            eq(HttpMethod.POST),
            any(HttpEntity.class),
            eq(VietQRResponse.class)
        )).thenReturn(responseEntity);

        // When
        vietQRService.generateQRCode(paymentId, amount, subscriptionId);

        // Then
        @SuppressWarnings("unchecked")
        ArgumentCaptor<HttpEntity<VietQRRequest>> captor = ArgumentCaptor.forClass(HttpEntity.class);
        verify(restTemplate).exchange(
            anyString(),
            eq(HttpMethod.POST),
            captor.capture(),
            eq(VietQRResponse.class)
        );

        HttpEntity<VietQRRequest> capturedEntity = captor.getValue();
        assertThat(capturedEntity.getHeaders().get("x-client-id"))
            .containsExactly(apiKey);
    }

    @Test
    @DisplayName("Should build correct VietQR request")
    void shouldBuildCorrectVietQRRequest() {
        // Given
        UUID paymentId = UUID.randomUUID();
        UUID subscriptionId = UUID.randomUUID();
        Long amount = 100000L;

        VietQRResponse.VietQRData responseData = new VietQRResponse.VietQRData();
        responseData.setQrDataUrl("https://img.vietqr.io/image/qr-code.jpg");

        VietQRResponse mockResponse = new VietQRResponse();
        mockResponse.setCode("00");
        mockResponse.setData(responseData);

        ResponseEntity<VietQRResponse> responseEntity = ResponseEntity.ok(mockResponse);

        when(restTemplate.exchange(
            anyString(),
            eq(HttpMethod.POST),
            any(HttpEntity.class),
            eq(VietQRResponse.class)
        )).thenReturn(responseEntity);

        // When
        vietQRService.generateQRCode(paymentId, amount, subscriptionId);

        // Then
        @SuppressWarnings("unchecked")
        ArgumentCaptor<HttpEntity<VietQRRequest>> captor = ArgumentCaptor.forClass(HttpEntity.class);
        verify(restTemplate).exchange(
            anyString(),
            eq(HttpMethod.POST),
            captor.capture(),
            eq(VietQRResponse.class)
        );

        HttpEntity<VietQRRequest> capturedEntity = captor.getValue();
        VietQRRequest request = capturedEntity.getBody();

        assertThat(request).isNotNull();
        assertThat(request.getAcqId()).isEqualTo(BANK_CODE);
        assertThat(request.getAccountNo()).isEqualTo(ACCOUNT_NUMBER);
        assertThat(request.getAccountName()).isEqualTo(ACCOUNT_NAME);
        assertThat(request.getAmount()).isEqualTo(amount);
        assertThat(request.getTemplate()).isEqualTo(DEFAULT_TEMPLATE);
        assertThat(request.getAddInfo()).startsWith("KITECLASS ");
    }

    @Test
    @DisplayName("Should use fallback URL when API call fails")
    void shouldUseFallbackUrlWhenApiCallFails() {
        // Given
        UUID paymentId = UUID.randomUUID();
        UUID subscriptionId = UUID.randomUUID();
        Long amount = 100000L;

        when(restTemplate.exchange(
            anyString(),
            eq(HttpMethod.POST),
            any(HttpEntity.class),
            eq(VietQRResponse.class)
        )).thenThrow(new RestClientException("Connection timeout"));

        // When
        String result = vietQRService.generateQRCode(paymentId, amount, subscriptionId);

        // Then
        assertThat(result).contains("https://img.vietqr.io/image/");
        assertThat(result).contains(BANK_CODE);
        assertThat(result).contains(ACCOUNT_NUMBER);
        assertThat(result).contains("amount=" + amount);
    }

    @Test
    @DisplayName("Should throw exception when API returns error code")
    void shouldThrowExceptionWhenApiReturnsErrorCode() {
        // Given
        UUID paymentId = UUID.randomUUID();
        UUID subscriptionId = UUID.randomUUID();
        Long amount = 100000L;

        VietQRResponse mockResponse = new VietQRResponse();
        mockResponse.setCode("99");
        mockResponse.setDescription("Invalid account");

        ResponseEntity<VietQRResponse> responseEntity = ResponseEntity.ok(mockResponse);

        when(restTemplate.exchange(
            anyString(),
            eq(HttpMethod.POST),
            any(HttpEntity.class),
            eq(VietQRResponse.class)
        )).thenReturn(responseEntity);

        // When/Then
        assertThatThrownBy(() -> vietQRService.generateQRCode(paymentId, amount, subscriptionId))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("Failed to generate QR code");
    }

    @Test
    @DisplayName("Should generate payment content correctly")
    void shouldGeneratePaymentContentCorrectly() {
        // Given
        UUID subscriptionId = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");

        // When
        String content = vietQRService.generatePaymentContent(subscriptionId);

        // Then
        assertThat(content).isEqualTo("KITECLASS 550E8400");
    }

    @Test
    @DisplayName("Should return false for payment verification (not implemented)")
    void shouldReturnFalseForPaymentVerification() {
        // Given
        String transactionId = "TXN123456";
        Long expectedAmount = 100000L;
        String expectedContent = "KITECLASS 550E8400";

        // When
        boolean result = vietQRService.verifyPayment(transactionId, expectedAmount, expectedContent);

        // Then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("Should handle null transaction ID in verification")
    void shouldHandleNullTransactionIdInVerification() {
        // When
        boolean result = vietQRService.verifyPayment(null, 100000L, "KITECLASS 123");

        // Then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("Should get bank info correctly")
    void shouldGetBankInfoCorrectly() {
        // When
        String bankInfo = vietQRService.getBankInfo();

        // Then
        assertThat(bankInfo).contains(BANK_CODE);
        assertThat(bankInfo).contains(ACCOUNT_NUMBER);
        assertThat(bankInfo).contains(ACCOUNT_NAME);
    }

    // ---- GAP-1361: circuit breaker wiring -------------------------------------

    @Test
    @DisplayName("GAP-1361: generateQRCode is @CircuitBreaker-protected with a fallback")
    void generateQRCode_hasCircuitBreaker() throws Exception {
        CircuitBreaker cb = VietQRService.class
                .getDeclaredMethod("generateQRCode", UUID.class, Long.class, String.class)
                .getAnnotation(CircuitBreaker.class);

        assertThat(cb).as("generateQRCode(UUID,Long,String) must be @CircuitBreaker-annotated")
                .isNotNull();
        assertThat(cb.name()).isEqualTo(VietQRService.CB_NAME);
        assertThat(cb.fallbackMethod()).isEqualTo("generateQRCodeFallback");
    }

    @Test
    @DisplayName("GAP-1361: fallback returns the public VietQR image URL")
    void generateQRCodeFallback_returnsPublicImageUrl() throws Exception {
        UUID paymentId = UUID.randomUUID();
        Long amount = 100000L;

        var method = VietQRService.class.getDeclaredMethod(
                "generateQRCodeFallback", UUID.class, Long.class, String.class, Throwable.class);
        method.setAccessible(true);

        String url = (String) method.invoke(
                vietQRService, paymentId, amount, "KITECLASS TEST",
                new RuntimeException("circuit open"));

        assertThat(url).contains("https://img.vietqr.io/image/");
        assertThat(url).contains(BANK_CODE);
        assertThat(url).contains(ACCOUNT_NUMBER);
        assertThat(url).contains("amount=" + amount);
    }
}
