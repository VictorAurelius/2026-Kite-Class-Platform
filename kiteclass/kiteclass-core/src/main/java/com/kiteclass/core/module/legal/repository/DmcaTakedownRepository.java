package com.kiteclass.core.module.legal.repository;

import com.kiteclass.core.module.legal.entity.DmcaStatus;
import com.kiteclass.core.module.legal.entity.DmcaTakedownRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * @since 3.24.0 (Wave 4 Sub-PR 4.3, GAP-042)
 */
@Repository
public interface DmcaTakedownRepository extends JpaRepository<DmcaTakedownRequest, Long> {

    List<DmcaTakedownRequest> findByStatusAndDeletedFalse(DmcaStatus status);
}
