package com.kitehub.subscription.auth.otp;

import com.kitehub.subscription.auth.otp.OtpService.OtpRequestResult;
import com.kitehub.subscription.auth.otp.OtpService.OtpVerifyResult;
import com.kitehub.subscription.auth.otp.OtpService.VerifyFailureReason;
import com.kitehub.subscription.config.SecurityConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * MockMvc tests for {@link OtpController} (GAP-286). Mirrors
 * {@code PasswordResetControllerTest} ({@code @WebMvcTest} + {@code SecurityConfig}).
 */
@WebMvcTest(controllers = OtpController.class)
@Import(SecurityConfig.class)
@DisplayName("OtpController")
class OtpControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockitoBean private OtpService service;
    @MockitoBean private JpaMetamodelMappingContext jpaMetamodelMappingContext;

    @BeforeEach
    void setUp() {
        Mockito.reset(service);
    }

    @Test
    @DisplayName("POST /request-otp — 200 with requestId + channel + expiresInSeconds + mock")
    void requestOtpReturns200() throws Exception {
        when(service.requestOtp(eq("0901234567"), eq("ZALO")))
            .thenReturn(OtpRequestResult.issued("req-123", "ZALO", 300, true));

        mockMvc.perform(post("/api/v1/auth/signup/request-otp")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"phone\":\"0901234567\",\"channel\":\"ZALO\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.requestId").value("req-123"))
            .andExpect(jsonPath("$.channel").value("ZALO"))
            .andExpect(jsonPath("$.expiresInSeconds").value(300))
            .andExpect(jsonPath("$.mock").value(true));

        verify(service).requestOtp("0901234567", "ZALO");
    }

    @Test
    @DisplayName("POST /request-otp — 429 RATE_LIMITED with retryAfterSeconds")
    void requestOtpReturns429WhenRateLimited() throws Exception {
        when(service.requestOtp(eq("0901234567"), any()))
            .thenReturn(OtpRequestResult.rateLimited(42));

        mockMvc.perform(post("/api/v1/auth/signup/request-otp")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"phone\":\"0901234567\"}"))
            .andExpect(status().isTooManyRequests())
            .andExpect(jsonPath("$.error").value("RATE_LIMITED"))
            .andExpect(jsonPath("$.retryAfterSeconds").value(42));
    }

    @Test
    @DisplayName("POST /request-otp — 400 OTP_INVALID_PHONE on a malformed phone")
    void requestOtpRejectsInvalidPhone() throws Exception {
        mockMvc.perform(post("/api/v1/auth/signup/request-otp")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"phone\":\"12345\"}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("OTP_INVALID_PHONE"));

        verify(service, never()).requestOtp(any(), any());
    }

    @Test
    @DisplayName("POST /verify-otp — 200 verified:true + signupToken")
    void verifyOtpReturns200WithToken() throws Exception {
        when(service.verifyOtp(eq("0901234567"), eq("123456")))
            .thenReturn(OtpVerifyResult.success("signup-jwt-xyz"));

        mockMvc.perform(post("/api/v1/auth/signup/verify-otp")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"phone\":\"0901234567\",\"code\":\"123456\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.verified").value(true))
            .andExpect(jsonPath("$.signupToken").value("signup-jwt-xyz"));

        verify(service).verifyOtp("0901234567", "123456");
    }

    @Test
    @DisplayName("POST /verify-otp — 400 verified:false + reason on bad code")
    void verifyOtpReturns400WithReason() throws Exception {
        when(service.verifyOtp(eq("0901234567"), eq("000000")))
            .thenReturn(OtpVerifyResult.failure(VerifyFailureReason.INVALID_CODE));

        mockMvc.perform(post("/api/v1/auth/signup/verify-otp")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"phone\":\"0901234567\",\"code\":\"000000\"}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.verified").value(false))
            .andExpect(jsonPath("$.reason").value("INVALID_CODE"));
    }
}
