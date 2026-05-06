package com.kitehub.subscription.consent.service;

import com.kitehub.subscription.consent.dto.ConsentRequest;
import com.kitehub.subscription.consent.entity.ConsentRecord;

import java.util.Optional;
import java.util.UUID;

/**
 * Service contract for the server-side PDPL consent record store.
 *
 * <p>Idempotent upsert by {@code visitorId} — re-posting the same id updates
 * the existing active record rather than inserting a duplicate. Revoke is a
 * separate flow that sets {@code revokedAt} on the latest record.</p>
 *
 * @since Wave 25 Bucket A — GAP-353b
 */
public interface ConsentService {

    /**
     * Idempotent upsert: insert a new record OR update the latest active record
     * for {@code request.visitorId}. Always returns the persisted entity.
     */
    ConsentRecord recordConsent(ConsentRequest request);

    /** Read latest record for visitor, or empty if none. */
    Optional<ConsentRecord> findLatest(UUID visitorId);

    /**
     * Revoke the latest active record for {@code visitorId}. Returns the
     * updated record. Throws {@link java.util.NoSuchElementException} if no
     * record exists.
     */
    ConsentRecord revoke(UUID visitorId);
}
