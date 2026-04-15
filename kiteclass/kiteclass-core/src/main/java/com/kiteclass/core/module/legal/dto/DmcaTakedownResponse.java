package com.kiteclass.core.module.legal.dto;

import com.kiteclass.core.module.legal.entity.DmcaStatus;
import com.kiteclass.core.module.legal.entity.DmcaTakedownRequest;

/**
 * Response body for public DMCA intake — echoes the assigned id + status so the submitter
 * can reference the case. No PII beyond what they supplied is returned.
 *
 * @since 3.24.0 (Wave 4 Sub-PR 4.3, GAP-042)
 */
public record DmcaTakedownResponse(
        Long id,
        DmcaStatus status
) {
    public static DmcaTakedownResponse from(DmcaTakedownRequest entity) {
        return new DmcaTakedownResponse(entity.getId(), entity.getStatus());
    }
}
