package com.kiteclass.core.module.settings.repository;

import com.kiteclass.core.module.settings.entity.UserPreferences;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository for UserPreferences entity.
 *
 * @since 2.9
 */
@Repository
public interface UserPreferencesRepository extends JpaRepository<UserPreferences, Long> {

    /**
     * Find user preferences by user ID (non-deleted).
     *
     * @param instanceId tenant instance ID
     * @param userId     user ID (Gateway User reference)
     * @return Optional of UserPreferences
     */
    @Query("SELECT up FROM UserPreferences up WHERE up.instanceId = :instanceId AND up.userId = :userId AND up.deleted = false")
    Optional<UserPreferences> findByInstanceIdAndUserIdAndDeletedFalse(
            @Param("instanceId") UUID instanceId,
            @Param("userId") Long userId
    );

    /**
     * Check if user preferences exist for user.
     *
     * @param instanceId tenant instance ID
     * @param userId     user ID
     * @return true if exists
     */
    @Query("SELECT COUNT(up) > 0 FROM UserPreferences up WHERE up.instanceId = :instanceId AND up.userId = :userId AND up.deleted = false")
    boolean existsByInstanceIdAndUserIdAndDeletedFalse(
            @Param("instanceId") UUID instanceId,
            @Param("userId") Long userId
    );
}
