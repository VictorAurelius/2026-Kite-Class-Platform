package com.kitehub.subscription.consent.immutable;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for {@link ConsentRecordImmutable}.
 *
 * <p>NO update/delete methods exposed — entity itself is append-only và DB-level RLS
 * blocks UPDATE/DELETE (V56). Even if developer adds `repository.deleteAll()` accidentally,
 * Postgres rejects.
 *
 * @since Wave beta-readiness-4 Bucket B — GAP-353b
 */
@Repository
public interface ConsentRecordImmutableRepository extends JpaRepository<ConsentRecordImmutable, Long> {

    /** Lịch sử consent đầy đủ cho user, oldest → newest, dùng cho hash chain validation. */
    List<ConsentRecordImmutable> findByUserIdOrderBySignedAtAsc(Long userId);

    /** Row mới nhất cho user — dùng để compute prevHash khi insert row mới. */
    Optional<ConsentRecordImmutable> findFirstByUserIdOrderBySignedAtDesc(Long userId);
}
