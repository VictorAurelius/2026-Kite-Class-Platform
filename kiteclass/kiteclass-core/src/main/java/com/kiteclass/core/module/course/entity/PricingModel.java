package com.kiteclass.core.module.course.entity;

/**
 * Taxonomy for course pricing models in Vietnamese education center market.
 *
 * <p>PER_HOUR is the dominant model for English/STEM tutoring centers (trung tâm dạy thêm):
 * Apollo 257-344k/giờ, ILA 195-368k/giờ. MONTHLY is common for kindergarten-adjacent programs.
 * COURSE_PACKAGE suits IELTS/certification prep bundles. FREE is for trial/demo classes.
 *
 * @see <a href="../../../../../../../../../../../documents/02-architecture/adr/ADR-027-pricing-model-taxonomy.md">ADR-027</a>
 */
public enum PricingModel {

    /**
     * Per-hour billing: unitPrice × session hours.
     * Market norm for trung tâm tiếng Anh / STEM dạy thêm.
     */
    PER_HOUR,

    /**
     * Monthly fixed fee regardless of session count within the month.
     * Common for kinder-adjacent and music programs.
     */
    MONTHLY,

    /**
     * Flat-rate package for a fixed number of sessions/modules (e.g., IELTS 40-session bundle).
     */
    COURSE_PACKAGE,

    /**
     * No charge — trial class, demo, or scholarship grant.
     */
    FREE
}
