package com.kitehub.subscription.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Public response DTO for {@code GET /api/v1/public/tenants/by-subdomain/{slug}}.
 *
 * <p>Schema per {@code documents/01-business/kitehub/marketing/api-contract.md}
 * §9.1.3 — fields mirror Wave tenant-domain-1 GAP-813 contract.</p>
 *
 * <p>Anonymous endpoint — NO sensitive fields (database URL, contact email,
 * subscription ID, etc.) leak through here.</p>
 *
 * @author KiteHub Team
 * @since Wave tenant-domain-1 Bucket B (GAP-813)
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TenantResolveDto {

    /**
     * Tenant UUID (stable across renames).
     */
    private UUID id;

    /**
     * Subdomain slug (echo input lowercase).
     */
    private String subdomain;

    /**
     * Organization display name (Vietnamese-friendly).
     * Source: {@code Instance.organizationName}.
     */
    private String name;

    /**
     * Instance status enum name (per {@code InstanceStatus}).
     * For 200 OK responses always {@code "ACTIVE"}; for 410 GONE responses
     * one of {@code SUSPENDED} / {@code DELETED}.
     */
    private String status;
}
