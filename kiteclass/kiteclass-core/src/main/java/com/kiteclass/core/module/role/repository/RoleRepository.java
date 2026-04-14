package com.kiteclass.core.module.role.repository;

import com.kiteclass.core.module.role.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * @since 3.15.0 (GAP-058)
 */
@Repository
public interface RoleRepository extends JpaRepository<Role, Long> {

    Optional<Role> findByNameAndDeletedFalse(String name);

    List<Role> findByParentIdAndDeletedFalse(Long parentId);

    List<Role> findByLevelAndDeletedFalse(Integer level);

    boolean existsByNameAndDeletedFalse(String name);
}
