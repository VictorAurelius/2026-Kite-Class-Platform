package com.kitehub.subscription.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpMethod;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import com.kitehub.subscription.dto.vietqr.VietQRResponse;

import java.net.SocketTimeoutException;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Timeout-path tests for {@link VietQRService} — closes the payment half of
 * GAP-146 (remaining 3 sites from GAP-131: payment / email / captcha).
 *
 * <p>The injected {@link RestTemplate} already carries the 5 s connect + 30 s
 * read timeouts declared in {@code RestTemplateConfig} (GAP-131). This test
 * verifies the BEHAVIOURAL contract — on a simulated read timeout, the service
 * logs the error and returns the public fallback image URL instead of
 * propagating a {@code RestClientException} up to the Tomcat worker. Without
 * this guarantee, a slow/hung VietQR upstream would surface as a 500 to the
 * user even though the fallback exists.
 *
 * @since Wave 9-F (GAP-146)
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("VietQRService — timeout / fallback behaviour (GAP-146)")
class VietQRServiceTimeoutTest {

    private static final String API_URL = "https://api.vietqr.io/v2/generate";

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private VietQRService vietQRService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(vietQRService, "bankCode", "VCB");
        ReflectionTestUtils.setField(vietQRService, "accountNumber", "1234567890");
        ReflectionTestUtils.setField(vietQRService, "accountName", "CONG TY KITECLASS");
        ReflectionTestUtils.setField(vietQRService, "apiUrl", API_URL);
        ReflectionTestUtils.setField(vietQRService, "apiKey", null);
        ReflectionTestUtils.setField(vietQRService, "defaultTemplate", "compact");
        ReflectionTestUtils.setField(vietQRService, "mockMode", false);
    }

    @Test
    @DisplayName("On read timeout, returns public fallback QR URL (no cascade failure)")
    void returnsFallbackOnReadTimeout() {
        UUID paymentId = UUID.randomUUID();
        UUID subscriptionId = UUID.randomUUID();
        Long amount = 250_000L;

        // RestTemplate wraps SocketTimeoutException as ResourceAccessException
        // (this is what actually happens when RestTemplateConfig's read
        // timeout fires).
        when(restTemplate.exchange(
                anyString(),
                eq(HttpMethod.POST),
                any(),
                eq(VietQRResponse.class)))
            .thenThrow(new ResourceAccessException(
                    "Read timed out",
                    new SocketTimeoutException("Read timed out")));

        String result = vietQRService.generateQRCode(paymentId, amount, subscriptionId);

        assertThat(result)
                .as("fallback URL must be returned on timeout, never null/exception")
                .isNotBlank()
                .startsWith("https://img.vietqr.io/image/")
                .contains("VCB")
                .contains("1234567890")
                .contains(String.valueOf(amount));

        verify(restTemplate).exchange(anyString(), eq(HttpMethod.POST), any(), eq(VietQRResponse.class));
    }

    @Test
    @DisplayName("On connect timeout, returns public fallback QR URL")
    void returnsFallbackOnConnectTimeout() {
        UUID paymentId = UUID.randomUUID();
        UUID subscriptionId = UUID.randomUUID();

        when(restTemplate.exchange(
                anyString(),
                eq(HttpMethod.POST),
                any(),
                eq(VietQRResponse.class)))
            .thenThrow(new ResourceAccessException(
                    "Connection refused / connect timeout"));

        String result = vietQRService.generateQRCode(paymentId, 100_000L, subscriptionId);

        assertThat(result)
                .as("connect-timeout must fall back the same way as read-timeout")
                .isNotBlank()
                .startsWith("https://img.vietqr.io/image/");
    }
}
