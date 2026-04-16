package com.kitehub.subscription.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Event published when an instance is permanently purged.
 * Other services (branding, kiteclass) listen for this to clean up their resources.
 *
 * @author KiteHub Team
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PurgeEvent implements Serializable {
    private static final long serialVersionUID = 1L;

    private UUID instanceId;
    private String subdomain;
    private LocalDateTime purgedAt;
}
