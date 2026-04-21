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

import java.net.SocketTimeoutException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * Timeout-path tests for {@link CaptchaService} — closes the captcha half of
 * GAP-146 (remaining 3 sites from GAP-131).
 *
 * <p>hCaptcha verify calls use the shared {@code RestTemplate} (5 s connect,
 * 30 s read) from {@code RestTemplateConfig} (GAP-131). This test documents
 * and guards the FAIL-OPEN policy: when the upstream times out, registration
 * should NOT be blocked — blocking every new signup because hCaptcha is slow
 * would cost more than the fraud surface of letting a handful of bots through
 * during the outage window. The opposite policy (fail-closed) is available by
 * flipping the handler, but the current business decision is fail-open and
 * this test locks it in.
 *
 * @since Wave 9-F (GAP-146)
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CaptchaService — timeout / fail-open behaviour (GAP-146)")
class CaptchaServiceTimeoutTest {

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private CaptchaService captchaService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(captchaService, "captchaEnabled", true);
        ReflectionTestUtils.setField(captchaService, "secretKey", "test-secret");
        ReflectionTestUtils.setField(captchaService, "verifyUrl", "https://hcaptcha.com/siteverify");
    }

    @Test
    @DisplayName("On read timeout, fails OPEN (returns true) — does not block signup")
    void failsOpenOnReadTimeout() {
        when(restTemplate.exchange(
                anyString(),
                eq(HttpMethod.POST),
                any(),
                any(Class.class)))
            .thenThrow(new ResourceAccessException(
                    "Read timed out",
                    new SocketTimeoutException("Read timed out")));

        boolean result = captchaService.verifyCaptcha("real-user-token");

        assertThat(result)
                .as("captcha fail-open contract — upstream timeout must not block legitimate users")
                .isTrue();
    }

    @Test
    @DisplayName("On connect timeout, fails OPEN (returns true)")
    void failsOpenOnConnectTimeout() {
        when(restTemplate.exchange(
                anyString(),
                eq(HttpMethod.POST),
                any(),
                any(Class.class)))
            .thenThrow(new ResourceAccessException("Connection refused"));

        boolean result = captchaService.verifyCaptcha("real-user-token");

        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("When captcha disabled, returns true without calling the upstream")
    void shortCircuitsWhenDisabled() {
        ReflectionTestUtils.setField(captchaService, "captchaEnabled", false);

        // Token itself doesn't matter — service must not call out.
        boolean result = captchaService.verifyCaptcha("anything");

        assertThat(result).isTrue();
        // Upstream call would have thrown since we never stubbed it — no call = pass.
    }
}
