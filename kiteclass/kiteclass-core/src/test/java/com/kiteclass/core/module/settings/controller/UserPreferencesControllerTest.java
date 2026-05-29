package com.kiteclass.core.module.settings.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kiteclass.core.config.TestSecurityConfig;
import com.kiteclass.core.module.settings.dto.request.UpdateUserPreferencesRequest;
import com.kiteclass.core.module.settings.dto.response.UserPreferencesResponse;
import com.kiteclass.core.module.settings.service.UserPreferencesService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Unit tests for UserPreferencesController.
 *
 * <p>Uses @TestConfiguration to provide mock beans instead of deprecated @MockitoBean.
 *
 * @since 2.9
 */
@WebMvcTest(UserPreferencesController.class)
@Import(TestSecurityConfig.class)
@ActiveProfiles("test")
@DisplayName("UserPreferencesController Tests")
class UserPreferencesControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserPreferencesService userPreferencesService;

    @TestConfiguration
    static class TestConfig {
        /**
         * Provides a mock UserPreferencesService for testing.
         *
         * @return mock UserPreferencesService instance
         */
        @Bean
        public UserPreferencesService userPreferencesService() {
            return mock(UserPreferencesService.class);
        }
    }

    @Test
    @DisplayName("Should get user preferences")
    void shouldGetUserPreferences() throws Exception {
        // Given
        Long userId = 1L;
        Map<String, Boolean> notificationPrefs = new HashMap<>();
        notificationPrefs.put("email", true);
        notificationPrefs.put("push", true);

        UserPreferencesResponse response = UserPreferencesResponse.builder()
                .id(1L)
                .userId(userId)
                .language("vi")
                .timezone("Asia/Ho_Chi_Minh")
                .theme("light")
                .notificationPreferences(notificationPrefs)
                .build();

        when(userPreferencesService.getUserPreferences(userId)).thenReturn(response);

        // When & Then
        mockMvc.perform(get("/api/v1/users/{userId}/preferences", userId)
                        .header("X-User-Id", userId)
                        .header("X-User-Reference-Id", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.userId").value(userId))
                .andExpect(jsonPath("$.data.language").value("vi"))
                .andExpect(jsonPath("$.data.theme").value("light"))
                .andExpect(jsonPath("$.data.timezone").value("Asia/Ho_Chi_Minh"));
    }

    @Test
    @DisplayName("Should update user preferences")
    void shouldUpdateUserPreferences() throws Exception {
        // Given
        Long userId = 1L;
        UpdateUserPreferencesRequest request = UpdateUserPreferencesRequest.builder()
                .language("en")
                .theme("dark")
                .timezone("America/New_York")
                .build();

        UserPreferencesResponse response = UserPreferencesResponse.builder()
                .id(1L)
                .userId(userId)
                .language("en")
                .theme("dark")
                .timezone("America/New_York")
                .build();

        when(userPreferencesService.updateUserPreferences(eq(userId), any(UpdateUserPreferencesRequest.class)))
                .thenReturn(response);

        // When & Then
        mockMvc.perform(patch("/api/v1/users/{userId}/preferences", userId)
                        .header("X-User-Id", userId)
                        .header("X-User-Reference-Id", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.language").value("en"))
                .andExpect(jsonPath("$.data.theme").value("dark"))
                .andExpect(jsonPath("$.data.timezone").value("America/New_York"));
    }

    @Test
    @DisplayName("Should update notification preferences")
    void shouldUpdateNotificationPreferences() throws Exception {
        // Given
        Long userId = 1L;
        Map<String, Boolean> notificationPrefs = new HashMap<>();
        notificationPrefs.put("email", false);
        notificationPrefs.put("push", true);
        notificationPrefs.put("sms", true);

        UpdateUserPreferencesRequest request = UpdateUserPreferencesRequest.builder()
                .notificationPreferences(notificationPrefs)
                .build();

        UserPreferencesResponse response = UserPreferencesResponse.builder()
                .id(1L)
                .userId(userId)
                .notificationPreferences(notificationPrefs)
                .build();

        when(userPreferencesService.updateUserPreferences(eq(userId), any(UpdateUserPreferencesRequest.class)))
                .thenReturn(response);

        // When & Then
        mockMvc.perform(patch("/api/v1/users/{userId}/preferences", userId)
                        .header("X-User-Id", userId)
                        .header("X-User-Reference-Id", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.notificationPreferences.email").value(false))
                .andExpect(jsonPath("$.data.notificationPreferences.push").value(true))
                .andExpect(jsonPath("$.data.notificationPreferences.sms").value(true));
    }

    @Test
    @DisplayName("Should reject invalid language code")
    void shouldRejectInvalidLanguageCode() throws Exception {
        // Given
        Long userId = 1L;
        UpdateUserPreferencesRequest request = UpdateUserPreferencesRequest.builder()
                .language("invalid")
                .build();

        // When & Then
        mockMvc.perform(patch("/api/v1/users/{userId}/preferences", userId)
                        .header("X-User-Id", userId)
                        .header("X-User-Reference-Id", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should reject invalid theme code")
    void shouldRejectInvalidThemeCode() throws Exception {
        // Given
        Long userId = 1L;
        UpdateUserPreferencesRequest request = UpdateUserPreferencesRequest.builder()
                .theme("invalid")
                .build();

        // When & Then
        mockMvc.perform(patch("/api/v1/users/{userId}/preferences", userId)
                        .header("X-User-Id", userId)
                        .header("X-User-Reference-Id", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should initialize default preferences")
    void shouldInitializeDefaultPreferences() throws Exception {
        // Given
        Long userId = 1L;
        Map<String, Boolean> defaultNotificationPrefs = new HashMap<>();
        defaultNotificationPrefs.put("email", true);
        defaultNotificationPrefs.put("push", true);
        defaultNotificationPrefs.put("sms", false);

        UserPreferencesResponse response = UserPreferencesResponse.builder()
                .id(1L)
                .userId(userId)
                .language("vi")
                .timezone("Asia/Ho_Chi_Minh")
                .theme("light")
                .notificationPreferences(defaultNotificationPrefs)
                .build();

        when(userPreferencesService.initializeDefaultPreferences(userId)).thenReturn(response);

        // When & Then
        mockMvc.perform(post("/api/v1/users/{userId}/preferences/initialize", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.userId").value(userId))
                .andExpect(jsonPath("$.data.language").value("vi"))
                .andExpect(jsonPath("$.data.theme").value("light"));
    }

    @Test
    @DisplayName("Should allow partial update with only language")
    void shouldAllowPartialUpdateWithOnlyLanguage() throws Exception {
        // Given
        Long userId = 1L;
        UpdateUserPreferencesRequest request = UpdateUserPreferencesRequest.builder()
                .language("en")
                .build();

        UserPreferencesResponse response = UserPreferencesResponse.builder()
                .id(1L)
                .userId(userId)
                .language("en")
                .theme("light")
                .timezone("Asia/Ho_Chi_Minh")
                .build();

        when(userPreferencesService.updateUserPreferences(eq(userId), any(UpdateUserPreferencesRequest.class)))
                .thenReturn(response);

        // When & Then
        mockMvc.perform(patch("/api/v1/users/{userId}/preferences", userId)
                        .header("X-User-Id", userId)
                        .header("X-User-Reference-Id", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.language").value("en"));
    }

    @Test
    @DisplayName("Should allow partial update with only theme")
    void shouldAllowPartialUpdateWithOnlyTheme() throws Exception {
        // Given
        Long userId = 1L;
        UpdateUserPreferencesRequest request = UpdateUserPreferencesRequest.builder()
                .theme("dark")
                .build();

        UserPreferencesResponse response = UserPreferencesResponse.builder()
                .id(1L)
                .userId(userId)
                .language("vi")
                .theme("dark")
                .timezone("Asia/Ho_Chi_Minh")
                .build();

        when(userPreferencesService.updateUserPreferences(eq(userId), any(UpdateUserPreferencesRequest.class)))
                .thenReturn(response);

        // When & Then
        mockMvc.perform(patch("/api/v1/users/{userId}/preferences", userId)
                        .header("X-User-Id", userId)
                        .header("X-User-Reference-Id", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.theme").value("dark"));
    }
}
