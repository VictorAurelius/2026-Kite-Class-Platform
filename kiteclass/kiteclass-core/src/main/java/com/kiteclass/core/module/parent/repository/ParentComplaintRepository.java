package com.kiteclass.core.module.parent.repository;

import com.kiteclass.core.module.parent.entity.ParentComplaint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Data-access for {@link ParentComplaint}.
 *
 * <p>v1 surface is INSERT-only — workflow queries (status filter, sort
 * by SLA, escalation join) ship in GAP-339 along with the back-office
 * controller. Inheriting {@link JpaRepository} gives us {@code save} +
 * {@code findById} for the v1 controller IT.
 *
 * @since 2.19.0 (Wave 19 — GAP-321c Phase 1C v1)
 */
@Repository
public interface ParentComplaintRepository extends JpaRepository<ParentComplaint, Long> {
}
