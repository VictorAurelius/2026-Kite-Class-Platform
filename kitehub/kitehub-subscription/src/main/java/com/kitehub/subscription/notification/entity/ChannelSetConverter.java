package com.kitehub.subscription.notification.entity;

import com.kitehub.subscription.notification.enums.NotificationChannelType;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.EnumSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * JPA converter — serializes a {@code Set<NotificationChannelType>} to a
 * comma-separated VARCHAR column for compact storage.
 *
 * <p>Empty sets persist as empty string ({@code ""}); null sets read back as
 * empty {@link EnumSet} for null-safety in service code.</p>
 *
 * <p>Unknown enum values encountered during read (e.g., a future channel
 * enum value rolled back to a Phase 1 deploy) are silently dropped to avoid
 * crashing the read path. The caller will simply not see them in the set.</p>
 *
 * @since 1.0 (Wave 18a Bucket B — GAP-063 Phase 1)
 */
@Converter(autoApply = false)
public class ChannelSetConverter implements AttributeConverter<Set<NotificationChannelType>, String> {

    @Override
    public String convertToDatabaseColumn(Set<NotificationChannelType> attribute) {
        if (attribute == null || attribute.isEmpty()) {
            return "";
        }
        return attribute.stream()
                .map(Enum::name)
                .sorted()
                .collect(Collectors.joining(","));
    }

    @Override
    public Set<NotificationChannelType> convertToEntityAttribute(String dbData) {
        EnumSet<NotificationChannelType> set = EnumSet.noneOf(NotificationChannelType.class);
        if (dbData == null || dbData.isBlank()) {
            return set;
        }
        for (String token : dbData.split(",")) {
            String trimmed = token.trim();
            if (trimmed.isEmpty()) continue;
            try {
                set.add(NotificationChannelType.valueOf(trimmed));
            } catch (IllegalArgumentException ignored) {
                // Forward-compat: unknown enum value persisted by a newer schema —
                // silently drop on read. Producers won't fan out to it anyway.
            }
        }
        return set;
    }
}
