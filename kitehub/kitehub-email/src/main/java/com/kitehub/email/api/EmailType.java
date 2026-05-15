package com.kitehub.email.api;

/**
 * Catalog of transactional email types supported by kitehub-email.
 *
 * <p>Each enum value maps to a Thymeleaf template under
 * {@code src/main/resources/templates/emails/}. The {@link #getTemplateName()}
 * value matches the template filename (without {@code .html} suffix) and is
 * what callers pass to {@link com.kitehub.email.dto.EmailRequest#setTemplateName(String)}.</p>
 *
 * <p>Adding a new template:</p>
 * <ol>
 *   <li>Create the {@code .html} file under {@code templates/emails/}</li>
 *   <li>Add a new enum value here with the matching template name</li>
 *   <li>Update {@code email-ses-setup-runbook.md} if the template requires SES
 *       production approval (e.g., new sender domain)</li>
 * </ol>
 *
 * @since Wave 33 Bucket B (GAP-370 — beta email templates)
 */
public enum EmailType {

    // ---- Onboarding + verification ----
    WELCOME("welcome"),
    EMAIL_VERIFICATION("email-verification"),
    ONBOARDING_TIPS("onboarding-tips"),

    // ---- Beta program (GAP-370 / GAP-372 — Wave 33) ----
    BETA_INVITE("beta-invite"),
    BETA_REQUEST_CONFIRM("beta-request-confirmation"),

    // ---- Staff invitation (GAP-561b — Wave 80 Bucket B) ----
    INVITE_STAFF("invite-staff"),

    // ---- Subscription lifecycle ----
    SUBSCRIPTION_CREATED("subscription-created"),
    SUBSCRIPTION_RENEWAL_REMINDER("subscription-renewal-reminder"),
    SUBSCRIPTION_EXPIRED("subscription-expired"),
    SUBSCRIPTION_SUSPENDED("subscription-suspended"),

    // ---- Trial lifecycle ----
    TRIAL_MIDPOINT("trial-midpoint"),
    TRIAL_EXPIRATION_WARNING("trial-expiration-warning"),
    TRIAL_EXPIRED("trial-expired"),

    // ---- Data retention / PDPL ----
    DATA_RETENTION_WARNING("data-retention-warning"),
    DATA_RETENTION_FINAL_WARNING("data-retention-final-warning"),
    DATA_DELETED("data-deleted");

    private final String templateName;

    EmailType(String templateName) {
        this.templateName = templateName;
    }

    /**
     * @return Thymeleaf template filename stem (without {@code .html} suffix).
     */
    public String getTemplateName() {
        return templateName;
    }

    /**
     * Resolve an {@link EmailType} from its template name.
     *
     * @param templateName template filename stem (e.g. {@code beta-invite})
     * @return matching enum value
     * @throws IllegalArgumentException if no enum value matches
     */
    public static EmailType fromTemplateName(String templateName) {
        for (EmailType type : values()) {
            if (type.templateName.equals(templateName)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown email template: " + templateName);
    }
}
