package com.kiteclass.core.module.clazz.entity;

import com.kiteclass.core.common.constant.SessionStatus;
import com.kiteclass.core.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * ClassSession entity representing a single session (buổi học) within a class.
 *
 * <p>Sessions are generated from the class schedule and represent individual
 * teaching events. Each session tracks:
 * <ul>
 *   <li>Date and time</li>
 *   <li>Topic and location (can override class defaults)</li>
 *   <li>Status (SCHEDULED, COMPLETED, CANCELLED, MAKEUP)</li>
 *   <li>Whether attendance has been taken</li>
 * </ul>
 *
 * @author KiteClass Team
 * @since 2.5.0
 */
@Entity
@Table(
        name = "class_sessions",
        indexes = {
                @Index(name = "idx_class_sessions_class_id", columnList = "class_id"),
                @Index(name = "idx_class_sessions_date", columnList = "session_date"),
                @Index(name = "idx_class_sessions_status", columnList = "status"),
                @Index(name = "idx_class_sessions_instance_id", columnList = "instance_id")
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClassSession extends BaseEntity {

    /**
     * Foreign key to parent class.
     * Required, cascades delete from class.
     */
    @Column(name = "class_id", nullable = false)
    private Long classId;

    /**
     * Sequential number of this session within the class.
     * Starts at 1, must be unique per class.
     * Example: Session 1, Session 2, Session 3...
     */
    @Column(name = "session_number", nullable = false)
    private Integer sessionNumber;

    /**
     * Date when this session takes place.
     * Required.
     */
    @Column(name = "session_date", nullable = false)
    private LocalDate sessionDate;

    /**
     * Session start time.
     * Required. Example: 18:00
     */
    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    /**
     * Session end time.
     * Required, must be after start_time. Example: 20:00
     */
    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    /**
     * Optional location override for this specific session.
     * When null, falls back to the class location.
     * Max 200 characters.
     */
    @Column(name = "location", length = 200)
    private String location;

    /**
     * Topic or content of this session.
     * Optional, max 200 characters.
     * Example: "Unit 3: Business Writing"
     */
    @Column(name = "topic", length = 200)
    private String topic;

    /**
     * Status of this session.
     * SCHEDULED → COMPLETED or CANCELLED
     * MAKEUP: Replacement session for a cancelled one.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private SessionStatus status = SessionStatus.SCHEDULED;

    /**
     * Whether attendance has been taken for this session.
     * Defaults to false. Set to true after teacher marks attendance.
     */
    @Column(name = "attendance_taken", nullable = false)
    @Builder.Default
    private Boolean attendanceTaken = false;
}
