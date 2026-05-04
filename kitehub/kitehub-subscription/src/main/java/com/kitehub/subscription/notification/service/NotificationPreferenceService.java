package com.kitehub.subscription.notification.service;

import com.kitehub.platform.domain.entity.User;
import com.kitehub.subscription.notification.dto.NotificationPreferenceDto;
import com.kitehub.subscription.notification.dto.NotificationPreferenceListResponse;
import com.kitehub.subscription.notification.dto.UpdateNotificationPreferenceRequest;
import com.kitehub.subscription.notification.entity.NotificationPreference;
import com.kitehub.subscription.notification.enums.NotificationChannelType;
import com.kitehub.subscription.notification.enums.NotificationType;
import com.kitehub.subscription.notification.repository.NotificationPreferenceRepository;
import com.kitehub.subscription.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Manages a user's {@link NotificationPreference} rows.
 *
 * <p>Phase 1 scope (Wave 18a Bucket B — GAP-063 Phase 1): list + upsert via PATCH.
 * Default fallbacks (BR-NOTIF-005/006) synthesized in {@link #list} for missing
 * rows so the UI always sees a complete table without seeding the DB at user
 * creation time.</p>
 *
 * @since 1.0 (Wave 18a Bucket B — GAP-063 Phase 1)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationPreferenceService {

    private final NotificationPreferenceRepository preferenceRepository;
    private final UserRepository userRepository;

    /**
     * List all preferences for a user — synthesizes defaults for types with no row.
     *
     * @param userId user id resolved from JWT
     * @return one DTO per {@link NotificationType} (7 in Phase 1)
     */
    @Transactional(readOnly = true)
    public NotificationPreferenceListResponse list(UUID userId) {
        if (userId == null) {
            throw new IllegalArgumentException("userId must not be null");
        }
        List<NotificationPreference> stored = preferenceRepository.findByUserId(userId);
        Map<NotificationType, NotificationPreference> byType = new EnumMap<>(NotificationType.class);
        for (NotificationPreference p : stored) {
            byType.put(p.getNotificationType(), p);
        }

        List<NotificationPreferenceDto> dtos = new ArrayList<>(NotificationType.values().length);
        for (NotificationType type : NotificationType.values()) {
            NotificationPreference p = byType.get(type);
            if (p != null) {
                dtos.add(toDto(type, p.getEnabledChannels()));
            } else {
                // BR-NOTIF-005/006: default-on EMAIL for both engagement + mandatory types.
                dtos.add(toDto(type, EnumSet.of(NotificationChannelType.EMAIL)));
            }
        }
        return NotificationPreferenceListResponse.builder().preferences(dtos).build();
    }

    /**
     * Upsert a preference row.
     *
     * @throws IllegalArgumentException                  if user not found
     * @throws MandatoryTypeCannotBeDisabledException    if EMAIL removed from a mandatory type
     */
    @Transactional
    public NotificationPreferenceDto update(UUID userId,
                                            NotificationType type,
                                            UpdateNotificationPreferenceRequest request) {
        if (userId == null || type == null || request == null || request.getEnabledChannels() == null) {
            throw new IllegalArgumentException("userId, type, and request.enabledChannels must not be null");
        }

        Set<NotificationChannelType> requested = new HashSet<>(request.getEnabledChannels());

        // BR-NOTIF-008: mandatory types must keep EMAIL enabled.
        if (type.isMandatory() && !requested.contains(NotificationChannelType.EMAIL)) {
            throw new MandatoryTypeCannotBeDisabledException(type);
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        NotificationPreference pref = preferenceRepository
                .findByUserIdAndNotificationType(userId, type)
                .orElseGet(() -> NotificationPreference.builder()
                        .user(user)
                        .notificationType(type)
                        .enabledChannels(EnumSet.noneOf(NotificationChannelType.class))
                        .build());

        Set<NotificationChannelType> previous = pref.getEnabledChannels() != null
                ? new HashSet<>(pref.getEnabledChannels())
                : new HashSet<>();
        pref.setEnabledChannels(requested);
        NotificationPreference saved = preferenceRepository.save(pref);

        // BR-NOTIF-011: structured audit log (no message body — PII-safe).
        log.info("notification.preference.changed userId={} notificationType={} before={} after={}",
                userId, type, previous, requested);

        return toDto(type, saved.getEnabledChannels());
    }

    private NotificationPreferenceDto toDto(NotificationType type, Set<NotificationChannelType> channels) {
        Set<NotificationChannelType> safe = channels != null
                ? new HashSet<>(channels)
                : new HashSet<>();
        return NotificationPreferenceDto.builder()
                .notificationType(type)
                .enabledChannels(safe)
                .mandatory(type.isMandatory())
                .build();
    }
}
