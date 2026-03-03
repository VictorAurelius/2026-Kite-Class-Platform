package com.kiteclass.core.common.constant;

/**
 * Types of learning resources that can be attached to lessons.
 * Used for categorizing supplemental materials in the LMS module.
 *
 * @since 2.9.0
 */
public enum ResourceType {
    /**
     * Video resource (YouTube, Vimeo, S3 video, etc.)
     */
    VIDEO,

    /**
     * PDF document resource
     */
    PDF,

    /**
     * Slide presentation resource (PowerPoint, Google Slides, etc.)
     */
    SLIDE,

    /**
     * Audio file resource
     */
    AUDIO,

    /**
     * External URL/link resource
     */
    LINK,

    /**
     * Code sample resource
     */
    CODE,

    /**
     * Other resource type
     */
    OTHER
}
