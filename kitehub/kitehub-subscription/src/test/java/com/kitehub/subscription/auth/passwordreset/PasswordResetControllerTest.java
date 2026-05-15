package com.kitehub.subscription.auth.passwordreset;

import com.kitehub.subscription.auth.passwordreset.PasswordResetService.PasswordResetTokenInvalidException;
import com.kitehub.subscription.auth.passwordreset.PasswordResetService.WeakPasswordException;
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
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * MockMvc tests for {@link PasswordResetController} (Wave 79 GAP-548).
 */
@WebMvcTest(controllers = PasswordResetController.class)
@Import(SecurityConfig.class)
@DisplayName("PasswordResetController")
class PasswordResetControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockitoBean private PasswordResetService service;
    @MockitoBean private JpaMetamodelMappingContext jpaMetamodelMappingContext;

    @BeforeEach
    void setUp() {
        Mockito.reset(service);
    }

    @Test
    @DisplayName("POST /password-reset-request — 202 with constant body for any email shape")
    void requestReturns202Accepted() throws Exception {
        doNothing().when(service).request(eq("user@example.com"));

        mockMvc.perform(post("/api/auth/password-reset-request")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"user@example.com\"}"))
            .andExpect(status().isAccepted())
            .andExpect(jsonPath("$.message").exists());

        verify(service).request("user@example.com");
    }

    @Test
    @DisplayName("POST /password-reset-request — 400 on missing email")
    void requestRejectsMissingEmail() throws Exception {
        mockMvc.perform(post("/api/auth/password-reset-request")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("PASSWORD_RESET_INVALID_PAYLOAD"));
        verify(service, never()).request(any());
    }

    @Test
    @DisplayName("POST /password-reset-confirm — 200 on valid token + new password")
    void confirmReturns200OnValid() throws Exception {
        doNothing().when(service).confirm(eq("tok-xyz"), eq("Str0ngP@ssword1"));

        mockMvc.perform(post("/api/auth/password-reset-confirm")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"token\":\"tok-xyz\",\"newPassword\":\"Str0ngP@ssword1\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.message").exists());

        verify(service).confirm("tok-xyz", "Str0ngP@ssword1");
    }

    @Test
    @DisplayName("POST /password-reset-confirm — 400 PASSWORD_RESET_TOKEN_INVALID on invalid token")
    void confirmRejectsInvalidToken() throws Exception {
        doThrow(new PasswordResetTokenInvalidException("token expired"))
            .when(service).confirm(eq("stale"), any());

        mockMvc.perform(post("/api/auth/password-reset-confirm")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"token\":\"stale\",\"newPassword\":\"Str0ngP@ssword1\"}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("PASSWORD_RESET_TOKEN_INVALID"));
    }

    @Test
    @DisplayName("POST /password-reset-confirm — 400 PASSWORD_RESET_WEAK_PASSWORD when service rejects")
    void confirmRejectsWeakPassword() throws Exception {
        doThrow(new WeakPasswordException("Password must be at least 12 characters"))
            .when(service).confirm(eq("tok"), eq("short"));

        mockMvc.perform(post("/api/auth/password-reset-confirm")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"token\":\"tok\",\"newPassword\":\"short\"}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("PASSWORD_RESET_WEAK_PASSWORD"));
    }
}
