package com.kitehub.subscription.dto;

import com.kitehub.platform.domain.entity.Instance;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO containing database connection credentials.
 *
 * @author KiteHub Team
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DatabaseCredentials {

    /**
     * JDBC connection URL.
     */
    private String databaseUrl;

    /**
     * Database username.
     */
    private String username;

    /**
     * Database password (plain text - use with caution).
     */
    private String password;

    /**
     * Create credentials from Instance entity.
     * Note: Password will be decrypted (not implemented in MVP).
     *
     * @param instance Instance entity
     * @return Database credentials
     */
    public static DatabaseCredentials fromInstance(Instance instance) {
        return DatabaseCredentials.builder()
            .databaseUrl(instance.getDatabaseUrl())
            .username(instance.getDatabaseUsername())
            .password(instance.getDatabasePassword()) // TODO: Decrypt
            .build();
    }
}
