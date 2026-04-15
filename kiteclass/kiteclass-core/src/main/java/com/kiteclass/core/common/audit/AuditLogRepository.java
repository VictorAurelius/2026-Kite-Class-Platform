package com.kiteclass.core.common.audit;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * @since 3.23.0 (Wave 4 Sub-PR 4.0)
 */
@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    List<AuditLog> findByAggregateTypeAndAggregateIdOrderByCreatedAtDesc(
            String aggregateType, String aggregateId);

    long countByActionType(String actionType);
}
