package com.kitehub.branding.wizard.dto;

import java.util.List;

/**
 * Response for GET /api/v1/branding/slug-availability (Wave 34 sub-GAP-272i).
 *
 * <p>{@code suggestions} is empty list when {@code available=true}; otherwise
 * holds 0..5 alternates the wizard can offer.
 */
public record SlugAvailabilityResponse(boolean available, List<String> suggestions) {}
