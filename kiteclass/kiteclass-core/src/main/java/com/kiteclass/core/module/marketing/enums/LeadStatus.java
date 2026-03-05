package com.kiteclass.core.module.marketing.enums;

/**
 * Lead qualification status in the sales funnel.
 *
 * @since 2.10
 */
public enum LeadStatus {
    /**
     * New lead, not yet contacted.
     */
    NEW("New"),

    /**
     * Lead has been contacted by teacher.
     */
    CONTACTED("Contacted"),

    /**
     * Lead is qualified and showing strong interest.
     */
    QUALIFIED("Qualified"),

    /**
     * Lead converted to paying student.
     */
    CONVERTED("Converted"),

    /**
     * Lead is not interested or unresponsive.
     */
    LOST("Lost"),

    /**
     * Lead email/phone invalid or spam.
     */
    INVALID("Invalid");

    private final String displayName;

    LeadStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
