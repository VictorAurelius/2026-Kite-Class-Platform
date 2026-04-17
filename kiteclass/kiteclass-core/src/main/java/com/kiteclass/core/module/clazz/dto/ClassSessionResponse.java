package com.kiteclass.core.module.clazz.dto;

import com.kiteclass.core.common.constant.SessionStatus;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Response DTO for ClassSession entity.
 *
 * @param id               Session ID
 * @param classId          Parent class ID
 * @param sessionNumber    Sequential session number
 * @param sessionDate      Date of session
 * @param startTime        Start time
 * @param endTime          End time
 * @param location         Location (null = use class default)
 * @param topic            Session topic
 * @param status           SCHEDULED, COMPLETED, CANCELLED, MAKEUP
 * @param attendanceTaken  Whether attendance was recorded
 *
 * @author KiteClass Team
 * @since 2.5.0
 */
public record ClassSessionResponse(
        Long id,
        Long classId,
        Integer sessionNumber,
        LocalDate sessionDate,
        LocalTime startTime,
        LocalTime endTime,
        String location,
        String topic,
        SessionStatus status,
        Boolean attendanceTaken
) {
}
