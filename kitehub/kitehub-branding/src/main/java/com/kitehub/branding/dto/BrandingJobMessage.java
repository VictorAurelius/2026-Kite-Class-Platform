package com.kitehub.branding.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.UUID;

/**
 * Message payload for branding job queue.
 *
 * @since 1.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BrandingJobMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Job ID.
     */
    private UUID jobId;

    /**
     * Instance ID.
     */
    private UUID instanceId;

    /**
     * Organization name.
     */
    private String organizationName;

    /**
     * Language code (vi, en).
     */
    private String language;

    /**
     * Logo file URL (S3).
     */
    private String logoUrl;

    /**
     * Wizard user-type axis (GAP-1133): SOLO_TEACHER / SMALL_CENTER / LARGE_CENTER.
     * Optional — null for pre-GAP-1133 messages (Jackson JSON converter tolerates
     * the new field on old payloads).
     */
    private String orgType;

    /**
     * Subscription tier driving generation mode (GAP-1135): TEMPLATE for
     * FREE/BASIC/PREMIUM, FULL_AI for ENTERPRISE. Optional — null/blank → FREE.
     */
    private String tier;
}
