package com.kitehub.subscription.notification;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kitehub.subscription.notification.controller.NotificationPreferenceController;
import com.kitehub.subscription.notification.dto.NotificationPreferenceDto;
import com.kitehub.subscription.notification.dto.NotificationPreferenceListResponse;
import com.kitehub.subscription.notification.dto.UpdateNotificationPreferenceRequest;
import com.kitehub.subscription.notification.enums.NotificationChannelType;
import com.kitehub.subscription.notification.enums.NotificationType;
import com.kitehub.subscription.notification.service.MandatoryTypeCannotBeDisabledException;
import com.kitehub.subscription.notification.service.NotificationPreferenceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.EnumSet;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Standalone MockMvc tests for {@link NotificationPreferenceController} —
 * verifies HTTP wiring + error code mapping per api-contract.md.
 *
 * <p>Uses {@code findAndRegisterModules()} on the ObjectMapper per memory
 * {@code feedback_objectmapper_test_jsr310.md} (defensive — no LocalDateTime
 * serialization in this controller's contract today, but matches project
 * convention).</p>
 *
 * @since 1.0 (Wave 18a Bucket B — GAP-063 Phase 1)
 */
@ExtendWith(MockitoExtension.class)
class NotificationPreferenceControllerTest {

    @Mock
    private NotificationPreferenceService preferenceService;

    @InjectMocks
    private NotificationPreferenceController controller;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;
    private UUID userId;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
        objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();
        userId = UUID.randomUUID();
    }

    @Test
    void list_returnsPreferences() throws Exception {
        NotificationPreferenceListResponse resp = NotificationPreferenceListResponse.builder()
                .preferences(List.of(
                        NotificationPreferenceDto.builder()
                                .notificationType(NotificationType.ABSENCE)
                                .enabledChannels(EnumSet.of(NotificationChannelType.EMAIL))
                                .mandatory(false)
                                .build()
                )).build();
        when(preferenceService.list(eq(userId))).thenReturn(resp);

        mockMvc.perform(get("/api/v1/notification-preferences")
                        .header("X-User-Id", userId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.preferences[0].notificationType").value("ABSENCE"))
                .andExpect(jsonPath("$.preferences[0].mandatory").value(false));
    }

    @Test
    void update_returnsUpdatedDto() throws Exception {
        when(preferenceService.update(eq(userId), eq(NotificationType.ABSENCE), any()))
                .thenReturn(NotificationPreferenceDto.builder()
                        .notificationType(NotificationType.ABSENCE)
                        .enabledChannels(EnumSet.of(NotificationChannelType.EMAIL))
                        .mandatory(false)
                        .build());

        UpdateNotificationPreferenceRequest req = UpdateNotificationPreferenceRequest.builder()
                .enabledChannels(EnumSet.of(NotificationChannelType.EMAIL))
                .build();

        mockMvc.perform(patch("/api/v1/notification-preferences/ABSENCE")
                        .header("X-User-Id", userId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.notificationType").value("ABSENCE"))
                .andExpect(jsonPath("$.enabledChannels[0]").value("EMAIL"));
    }

    @Test
    void update_mandatoryTypeDisabled_returns400WithErrorCode() throws Exception {
        when(preferenceService.update(eq(userId), eq(NotificationType.BILLING_INVOICE), any()))
                .thenThrow(new MandatoryTypeCannotBeDisabledException(NotificationType.BILLING_INVOICE));

        UpdateNotificationPreferenceRequest req = UpdateNotificationPreferenceRequest.builder()
                .enabledChannels(EnumSet.noneOf(NotificationChannelType.class))
                .build();

        mockMvc.perform(patch("/api/v1/notification-preferences/BILLING_INVOICE")
                        .header("X-User-Id", userId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("MANDATORY_TYPE_CANNOT_BE_DISABLED"));
    }

    @Test
    void update_unknownNotificationType_returns400WithInvalidType() throws Exception {
        UpdateNotificationPreferenceRequest req = UpdateNotificationPreferenceRequest.builder()
                .enabledChannels(EnumSet.of(NotificationChannelType.EMAIL))
                .build();

        mockMvc.perform(patch("/api/v1/notification-preferences/NOT_A_REAL_TYPE")
                        .header("X-User-Id", userId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("INVALID_NOTIFICATION_TYPE"));
    }

    @Test
    void list_missingUserIdHeader_returns400() throws Exception {
        mockMvc.perform(get("/api/v1/notification-preferences"))
                .andExpect(status().isBadRequest());
    }
}
