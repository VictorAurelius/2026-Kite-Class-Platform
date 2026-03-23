package com.kitehub.subscription.repository;

import com.kitehub.platform.domain.entity.EmailSentLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Repository for EmailSentLog entity.
 * Used to check and record email sends for idempotency.
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
}
