package com.kiteclass.core.module.role.repository;

import com.kiteclass.core.module.role.entity.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * @since 3.15.0 (GAP-058)
 */
@Repository
public interface UserRoleRepository extends JpaRepository<UserRole, Long> {

    List<UserRole> findByUserIdAndDeletedFalse(Long userId);

    List<UserRole> findByRoleIdAndDeletedFalse(Long roleId);

    Optional<UserRole> findByUserIdAndRoleIdAndDeletedFalse(Long userId, Long roleId);

    boolean existsByUserIdAndRoleIdAndDeletedFalse(Long userId, Long roleId);
}
