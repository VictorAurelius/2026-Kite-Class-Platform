package com.kiteclass.core.module.marketing.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for updating landing page content.
 * All fields optional for partial updates (PATCH semantics).
 *
 * @since 2.10
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateLandingPageRequest {

    @Size(max = 200, message = "{landing.hero.title.size}")
    private String heroTitle;

    @Size(max = 500, message = "{landing.hero.subtitle.size}")
    private String heroSubtitle;

    @Size(max = 500, message = "{landing.hero.image.size}")
    private String heroImageUrl;

    private String teacherBio;

    @Size(max = 500, message = "{landing.logo.size}")
    private String logoUrl;

    @Size(max = 200, message = "{landing.tagline.size}")
    private String tagline;

    @Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "{landing.color.invalid}")
    private String primaryColor;

    @Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "{landing.color.invalid}")
    private String secondaryColor;

    @Email(message = "{landing.email.invalid}")
    @Size(max = 255, message = "{landing.email.size}")
    private String contactEmail;

    @Size(max = 20, message = "{landing.phone.size}")
    private String contactPhone;

    private String address;

    @Size(max = 255, message = "{landing.social.size}")
    private String facebookUrl;

    @Size(max = 255, message = "{landing.social.size}")
    private String youtubeUrl;

    @Size(max = 255, message = "{landing.social.size}")
    private String instagramUrl;
}
