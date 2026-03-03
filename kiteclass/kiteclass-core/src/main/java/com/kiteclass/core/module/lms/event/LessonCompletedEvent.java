package com.kiteclass.core.module.lms.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * Event published when a student completes a lesson.
 * Can be used by other modules for:
 * - Sending completion notifications
 * - Updating gamification/achievement systems
 * - Triggering certificate generation
 * - Analytics tracking
 *
 * @author KiteClass Team
 * @since 2.9.0
 */
@Getter
public class LessonCompletedEvent extends ApplicationEvent {

    private final Long userId;
    private final Long lessonId;

    /**
     * Creates a new lesson completed event.
     *
     * @param source the object on which the event initially occurred (never null)
     * @param userId the ID of the user who completed the lesson
     * @param lessonId the ID of the completed lesson
     */
    public LessonCompletedEvent(Object source, Long userId, Long lessonId) {
        super(source);
        this.userId = userId;
        this.lessonId = lessonId;
    }
}
