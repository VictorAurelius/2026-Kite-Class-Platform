package com.kiteclass.core.module.moderation;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for {@link ModerationQueue}.
 *
 * <p>Queries always filter on {@code deleted = false} — admin queue surfaces
 * use {@link #findByStatusAndDeletedFalse(ModerationStatus)} to list pending human
 * reviews; {@link #findFirstByTargetTypeAndTargetIdAndDeletedFalse(String, String)}
 * supports the BR-MOD-003 "at most one non-terminal row per target" check in callers
 * that want to dedupe before inserting.
 *
 * @since 3.24.0 (Wave 4 Sub-PR 4.1, GAP-018)
 */
@Repository
public interface ModerationQueueRepository extends JpaRepository<ModerationQueue, Long> {

    List<ModerationQueue> findByStatusAndDeletedFalse(ModerationStatus status);

    Optional<ModerationQueue> findFirstByTargetTypeAndTargetIdAndDeletedFalse(
            String targetType, String targetId);
}
