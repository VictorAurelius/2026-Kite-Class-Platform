package com.kitehub.branding.lifecycle.dto;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Compact deploy-status summary for the post-deploy {@code /branding} page
 * (GAP-1108). Surfaces the instance lifecycle state + the placeholder
 * {@code frontendUrl} recorded on the latest {@code deploy-completed} marker so
 * the FE can render a "site is ready" card + landing link without parsing the
 * full lifecycle-events feed.
 *
 * <p>Wire shape per {@code documents/01-business/kitehub/ai-branding/api-contract.md}
 * §"GET /api/v1/branding/instances/{instanceId}/deploy-status".</p>
 *
 * @param instanceId      tenant instance id
 * @param state           current {@code LifecycleState} name (null when no state row yet)
 * @param deployed        {@code true} when {@code state == DEPLOYED}
 * @param frontendUrl     placeholder landing URL from the latest deploy marker (null when never deployed)
 * @param templateId      template selected on the latest deploy (null when absent)
 * @param slug            tenant slug from the latest deploy (null when absent)
 * @param brandingVersion branding version counter from the state row (null when no state row)
 * @param deployedAt      timestamp of the latest {@code deploy-completed} marker (null when never deployed)
 */
public record DeployStatusResponse(
    UUID instanceId,
    String state,
    boolean deployed,
    String frontendUrl,
    String templateId,
    String slug,
    Integer brandingVersion,
    LocalDateTime deployedAt) {
}
