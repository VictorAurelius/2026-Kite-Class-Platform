package com.kiteclass.core.module.k12.event;

import java.time.Instant;

/**
 * Domain event — every {@code SubjectGrade} for a (student, academicYear) is
 * now {@code PUBLISHED}. Fires on the publish that flips the last DRAFT/REVIEWED
 * row to PUBLISHED, signalling the học bạ generator (GAP-055) that the student's
 * year-end transcript is ready to materialise.
 *
 * <p>Routing key: {@code kiteclass.k12.grades.all-published}.
 * Consumers (planned): GAP-055 MOET học bạ generator, GAP-059 conduct grade
 * trigger.
 *
 * <p>Persisted via {@code OutboxEventWriter} (Outbox Pattern per
 * {@code design-patterns.md} §3.5) — guarantees at-least-once delivery once
 * the publish transaction commits.
 *
 * <p>Reference: BR-GRADEBOOK-006 in
 * {@code documents/01-business/kiteclass/multi-subject-gradebook/rules.md}.
 *
 * @param studentId       student aggregate id
 * @param academicYearId  academic year for which all subjects are published
 * @param publishedAt     instant of the final flip (PUBLISHED commit timestamp)
 *
 * @since 5.x (Wave 24 Bucket B — GAP-360 §360.5)
 */
public record SubjectGradeAllPublishedEvent(
        Long studentId,
        Long academicYearId,
        Instant publishedAt) {

    /** RabbitMQ routing key for cross-service consumers. */
    public static final String ROUTING_KEY = "kiteclass.k12.grades.all-published";

    /** Aggregate type identifier persisted on the Outbox row. */
    public static final String AGGREGATE_TYPE = "SubjectGradeBook";
}
