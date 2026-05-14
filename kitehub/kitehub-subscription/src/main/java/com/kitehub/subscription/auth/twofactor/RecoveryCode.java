package com.kitehub.subscription.auth.twofactor;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Recovery code row (GAP-516 / OWASP A07 §2.4) — bcrypt-hashed single-use code.
 *
 * <p>Ten rows per user are inserted at enrollment. Consuming a code sets
 * {@link #usedAt}. Regeneration marks all existing rows used and inserts ten new
 * rows in the same transaction.</p>
 *
 * @since 1.0.0 (Wave 72b GAP-516)
 */
@Entity
@Table(name = "recovery_codes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecoveryCode {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "code_hash", nullable = false, length = 72)
    private String codeHash;

    /** Non-null = code consumed; soft-deleted but kept for audit trail. */
    @Column(name = "used_at")
    private LocalDateTime usedAt;

    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
