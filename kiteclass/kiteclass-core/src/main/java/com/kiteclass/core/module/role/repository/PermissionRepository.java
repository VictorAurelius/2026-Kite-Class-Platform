package com.kiteclass.core.module.role.repository;

import com.kiteclass.core.module.role.entity.Permission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * @since 3.15.0 (GAP-058)
 */
@Repository
public interface PermissionRepository extends JpaRepository<Permission, Long> {

    Optional<Permission> findByNameAndDeletedFalse(String name);

    List<Permission> findByCategoryAndDeletedFalse(String category);

    boolean existsByNameAndDeletedFalse(String name);
}
