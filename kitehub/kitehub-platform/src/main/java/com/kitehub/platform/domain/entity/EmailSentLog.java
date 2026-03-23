package com.kitehub.platform.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entity for tracking sent emails to prevent duplicate sends.
 * Each record represents one email sent to a recipient.
 * The unique constraint on (instance_id, email_type, recipient, date)
 * ensures at most one email of each type per day per recipient.
 *
 * @author KiteHub Team
 * @since 1.0.0
 */
@Entity
@Table(name = "email_sent_log")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmailSentLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "instance_id")
    private UUID instanceId;

    @Column(name = "email_type", nullable = false, length = 100)
    private String emailType;

    @Column(name = "recipient", nullable = false)
    private String recipient;

    @Column(name = "sent_at", nullable = false)
    private LocalDateTime sentAt;
}
