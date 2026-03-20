package com.kitehub.subscription.service;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

/**
 * Service for hCaptcha verification.
 * Verifies captcha tokens to prevent spam registrations.
 *
 * @author KiteHub Team
 * @since PR-SEC-3
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CaptchaService {

    private final RestTemplate restTemplate;

    @Value("${captcha.enabled:false}")
    private boolean captchaEnabled;

    @Value("${captcha.secret-key:0x0000000000000000000000000000000000000000}")
    private String secretKey;

    @Value("${captcha.verify-url:https://hcaptcha.com/siteverify}")
    private String verifyUrl;

    /**
     * Verify hCaptcha token.
     *
     * @param token Captcha token from frontend
     * @return true if verification passes, false otherwise
     */
    public boolean verifyCaptcha(String token) {
        // Bypass if captcha is disabled (local dev)
        if (!captchaEnabled) {
            log.debug("Captcha verification bypassed (captcha.enabled=false)");
            return true;
        }

        // Validate token presence
        if (token == null || token.isBlank()) {
            log.warn("Captcha verification failed: token is null or empty");
            return false;
        }

        try {
            log.info("Verifying captcha token: {}...", token.substring(0, Math.min(8, token.length())));

            // Build request body (form-encoded)
            MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
            body.add("secret", secretKey);
            body.add("response", token);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);

            // Call hCaptcha verify API
            ResponseEntity<CaptchaVerifyResponse> response = restTemplate.exchange(
                verifyUrl,
                HttpMethod.POST,
                request,
                CaptchaVerifyResponse.class
            );

            if (response.getStatusCode() != HttpStatus.OK || response.getBody() == null) {
                log.error("hCaptcha API returned non-200 status: {}", response.getStatusCode());
                return false;
            }

            CaptchaVerifyResponse verifyResponse = response.getBody();
            boolean success = verifyResponse.isSuccess();

            if (success) {
                log.info("Captcha verification SUCCESS");
            } else {
                log.warn("Captcha verification FAILED: error-codes={}", (Object) verifyResponse.getErrorCodes());
            }

            return success;

        } catch (RestClientException e) {
            log.error("hCaptcha API call failed: {}", e.getMessage(), e);
            // Fail-open: allow registration if captcha service is down (avoid blocking legitimate users)
            // In production, you might want to fail-closed (return false) depending on security requirements
            log.warn("Captcha verification defaulting to PASS due to API error (fail-open)");
            return true;
        } catch (Exception e) {
            log.error("Unexpected error during captcha verification: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * hCaptcha verify API response.
     * Ref: https://docs.hcaptcha.com/#verify-the-user-response-server-side
     */
    @Data
    private static class CaptchaVerifyResponse {
        private boolean success;

        @JsonProperty("challenge_ts")
        private String challengeTs;

        private String hostname;

        @JsonProperty("error-codes")
        private String[] errorCodes;
    }
}
