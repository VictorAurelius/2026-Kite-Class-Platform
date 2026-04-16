package com.kitehub.subscription.repository;

import com.kitehub.platform.domain.entity.EmailSentLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Repository for EmailSentLog entity.
 * Used to check and record email sends for idempotency,
 * and to provide admin monitoring queries.
 *
 * @author KiteHub Team
 * @since 1.0.0
 */
@Repository
public interface EmailSentLogRepository extends JpaRepository<EmailSentLog, UUID> {

    /**
     * Check if an email of a given type was already sent to a recipient
     * within a time range (typically start and end of the same day).
     *
     * @param instanceId instance ID (nullable for system-level emails)
     * @param emailType type of email (e.g., "trial-warning", "retention-warning")
     * @param recipient recipient email address
     * @param start start of the time range
     * @param end end of the time range
     * @return true if such an email exists
     */
    boolean existsByInstanceIdAndEmailTypeAndRecipientAndSentAtBetween(
        UUID instanceId, String emailType, String recipient,
        LocalDateTime start, LocalDateTime end);

    /**
     * Delete all email sent logs for an instance (used during purge).
     *
     * @param instanceId instance UUID
     */
    void deleteByInstanceId(UUID instanceId);

    // ==================== ADMIN MONITORING QUERIES ====================

    /**
     * Paginated email history filtered by instance, email type pattern, and time range.
     *
     * @param instanceId instance ID
     * @param emailType email type pattern (e.g., "trial-warning" or "%FAILED%")
     * @param from start of time range
     * @param to end of time range
     * @param pageable pagination params
     * @return page of email logs
     */
    Page<EmailSentLog> findByInstanceIdAndEmailTypeContainingAndSentAtBetween(
        UUID instanceId, String emailType, LocalDateTime from, LocalDateTime to,
        Pageable pageable);

    /**
     * Paginated email history filtered by instance and time range (all types).
     *
     * @param instanceId instance ID
     * @param from start of time range
     * @param to end of time range
     * @param pageable pagination params
     * @return page of email logs
     */
    Page<EmailSentLog> findByInstanceIdAndSentAtBetween(
        UUID instanceId, LocalDateTime from, LocalDateTime to, Pageable pageable);

    /**
     * Global paginated email history filtered by time range.
     *
     * @param from start of time range
     * @param to end of time range
     * @param pageable pagination params
     * @return page of email logs
     */
    Page<EmailSentLog> findBySentAtBetween(
        LocalDateTime from, LocalDateTime to, Pageable pageable);

    /**
     * Count all emails sent within a time range.
     *
     * @param from start of time range
     * @param to end of time range
     * @return count of emails
     */
    long countBySentAtBetween(LocalDateTime from, LocalDateTime to);

    /**
     * Count emails matching a type pattern within a time range.
     * Useful for counting failures (pattern = ":FAILED").
     *
     * @param pattern type pattern to match (e.g., ":FAILED")
     * @param from start of time range
     * @param to end of time range
     * @return count of matching emails
     */
    long countByEmailTypeContainingAndSentAtBetween(
        String pattern, LocalDateTime from, LocalDateTime to);

    /**
     * Aggregate email count grouped by email type within a time range.
     *
     * @param from start of time range
     * @param to end of time range
     * @return list of [emailType, count] pairs
     */
    @Query("SELECT e.emailType, COUNT(e) FROM EmailSentLog e " +
           "WHERE e.sentAt BETWEEN :from AND :to " +
           "GROUP BY e.emailType")
    List<Object[]> countByEmailTypeGrouped(
        @Param("from") LocalDateTime from,
        @Param("to") LocalDateTime to);
}
