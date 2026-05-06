package com.kitehub.subscription.notification;

import com.kitehub.platform.domain.entity.User;
import com.kitehub.subscription.notification.dto.NotificationPreferenceDto;
import com.kitehub.subscription.notification.dto.NotificationPreferenceListResponse;
import com.kitehub.subscription.notification.dto.UpdateNotificationPreferenceRequest;
import com.kitehub.subscription.notification.entity.NotificationPreference;
import com.kitehub.subscription.notification.enums.NotificationChannelType;
import com.kitehub.subscription.notification.enums.NotificationType;
import com.kitehub.subscription.notification.repository.NotificationPreferenceRepository;
import com.kitehub.subscription.notification.service.MandatoryTypeCannotBeDisabledException;
import com.kitehub.subscription.notification.service.NotificationPreferenceService;
import com.kitehub.subscription.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link NotificationPreferenceService} covering BR-NOTIF-005..009.
 *
 * @since 1.0 (Wave 18a Bucket B — GAP-063 Phase 1)
 */
@ExtendWith(MockitoExtension.class)
class NotificationPreferenceServiceTest {

    @Mock
    private NotificationPreferenceRepository preferenceRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private NotificationPreferenceService service;

    private UUID userId;
    private User user;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        user = User.builder().id(userId).email("u@kite.test").name("Test").role("OWNER")
                .passwordHash("hash").build();
    }

    @Test
    void list_synthesizesDefaultsForUserWithNoStoredRows() {
        // BR-NOTIF-005/006: when no row exists, EMAIL is enabled by default for all 7 types.
        when(preferenceRepository.findByUserId(userId)).thenReturn(Collections.emptyList());

        NotificationPreferenceListResponse resp = service.list(userId);

        assertThat(resp.getPreferences()).hasSize(NotificationType.values().length);
        for (NotificationPreferenceDto dto : resp.getPreferences()) {
            assertThat(dto.getEnabledChannels()).containsExactly(NotificationChannelType.EMAIL);
            assertThat(dto.isMandatory()).isEqualTo(dto.getNotificationType().isMandatory());
        }
    }

    @Test
    void list_returnsStoredRowMergedWithDefaultsForMissingTypes() {
        NotificationPreference stored = NotificationPreference.builder()
                .user(user)
                .notificationType(NotificationType.ABSENCE)
                .enabledChannels(EnumSet.noneOf(NotificationChannelType.class)) // user disabled
                .build();
        when(preferenceRepository.findByUserId(userId)).thenReturn(List.of(stored));

        NotificationPreferenceListResponse resp = service.list(userId);

        // ABSENCE row reflects the stored empty set (user disabled it)
        NotificationPreferenceDto absence = resp.getPreferences().stream()
                .filter(d -> d.getNotificationType() == NotificationType.ABSENCE)
                .findFirst().orElseThrow();
        assertThat(absence.getEnabledChannels()).isEmpty();
        assertThat(absence.isMandatory()).isFalse();

        // BILLING_INVOICE not stored → synthesized default with EMAIL + mandatory=true
        NotificationPreferenceDto billing = resp.getPreferences().stream()
                .filter(d -> d.getNotificationType() == NotificationType.BILLING_INVOICE)
                .findFirst().orElseThrow();
        assertThat(billing.getEnabledChannels()).containsExactly(NotificationChannelType.EMAIL);
        assertThat(billing.isMandatory()).isTrue();
    }

    @Test
    void update_nonMandatoryType_upsertsRow() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(preferenceRepository.findByUserIdAndNotificationType(userId, NotificationType.ABSENCE))
                .thenReturn(Optional.empty());
        when(preferenceRepository.save(any(NotificationPreference.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        UpdateNotificationPreferenceRequest req = UpdateNotificationPreferenceRequest.builder()
                .enabledChannels(EnumSet.of(NotificationChannelType.EMAIL))
                .build();

        NotificationPreferenceDto dto = service.update(userId, NotificationType.ABSENCE, req);

        assertThat(dto.getNotificationType()).isEqualTo(NotificationType.ABSENCE);
        assertThat(dto.getEnabledChannels()).containsExactly(NotificationChannelType.EMAIL);
        assertThat(dto.isMandatory()).isFalse();

        ArgumentCaptor<NotificationPreference> captor = ArgumentCaptor.forClass(NotificationPreference.class);
        verify(preferenceRepository, times(1)).save(captor.capture());
        assertThat(captor.getValue().getUser()).isEqualTo(user);
        assertThat(captor.getValue().getNotificationType()).isEqualTo(NotificationType.ABSENCE);
    }

    @Test
    void update_disablingMandatoryEmail_throws() {
        // BR-NOTIF-008: MANDATORY_TYPE_CANNOT_BE_DISABLED
        UpdateNotificationPreferenceRequest req = UpdateNotificationPreferenceRequest.builder()
                .enabledChannels(EnumSet.noneOf(NotificationChannelType.class))
                .build();

        assertThatThrownBy(() -> service.update(userId, NotificationType.BILLING_INVOICE, req))
                .isInstanceOf(MandatoryTypeCannotBeDisabledException.class);
    }

    @Test
    void update_addingNonEmailChannelToMandatory_isAllowedIfEmailRetained() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(preferenceRepository.findByUserIdAndNotificationType(userId, NotificationType.SECURITY_ALERT))
                .thenReturn(Optional.empty());
        when(preferenceRepository.save(any(NotificationPreference.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        UpdateNotificationPreferenceRequest req = UpdateNotificationPreferenceRequest.builder()
                .enabledChannels(EnumSet.of(NotificationChannelType.EMAIL, NotificationChannelType.SMS))
                .build();

        NotificationPreferenceDto dto = service.update(userId, NotificationType.SECURITY_ALERT, req);
        // Mandatory rule satisfied: EMAIL still in set; SMS forward-compat (skipped at send-time per BR-NOTIF-010).
        assertThat(dto.getEnabledChannels()).contains(NotificationChannelType.EMAIL, NotificationChannelType.SMS);
    }

    @Test
    void update_unknownUser_throws() {
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        UpdateNotificationPreferenceRequest req = UpdateNotificationPreferenceRequest.builder()
                .enabledChannels(EnumSet.of(NotificationChannelType.EMAIL))
                .build();

        assertThatThrownBy(() -> service.update(userId, NotificationType.ABSENCE, req))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
