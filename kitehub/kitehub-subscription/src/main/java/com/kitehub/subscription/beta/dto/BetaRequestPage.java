package com.kitehub.subscription.beta.dto;

import java.util.List;

/**
 * Plain pagination wrapper for {@link BetaRequestResponse} listings.
 *
 * <p>Avoids returning Spring Data {@code Page} directly — the default Jackson
 * serialization of {@code PageImpl} is unstable across Spring releases (see
 * Spring Data Web warning) and adds noisy fields the FE doesn't use. This DTO
 * exposes only the fields the coordinator dashboard needs.</p>
 *
 * @since Wave 33 — GAP-372
 */
public record BetaRequestPage(
        List<BetaRequestResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {}
