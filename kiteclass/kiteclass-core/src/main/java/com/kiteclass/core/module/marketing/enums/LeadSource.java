package com.kiteclass.core.module.marketing.enums;

/**
 * Source of lead acquisition.
 *
 * @since 2.10
 */
public enum LeadSource {
    /**
     * Lead came from landing page registration.
     */
    LANDING_PAGE("Landing Page"),

    /**
     * Lead submitted contact form.
     */
    CONTACT_FORM("Contact Form"),

    /**
     * Lead signed up for trial directly.
     */
    TRIAL_SIGNUP("Trial Signup"),

    /**
     * Lead came from referral program.
     */
    REFERRAL("Referral"),

    /**
     * Lead came from social media.
     */
    SOCIAL_MEDIA("Social Media"),

    /**
     * Other source.
     */
    OTHER("Other");

    private final String displayName;

    LeadSource(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
