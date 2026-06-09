package com.kitehub.subscription.saleslead.repository;

import com.kitehub.subscription.saleslead.entity.SalesLead;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository for {@link SalesLead} (GAP-1101 — KiteHub PLATFORM sales lead).
 *
 * @since GAP-1101
 */
@Repository
public interface SalesLeadRepository extends JpaRepository<SalesLead, Long> {
}
