package com.kitehub.subscription.dto;

import com.kitehub.platform.domain.entity.Instance;
import com.kitehub.subscription.service.EncryptionService;
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
     * Password is decrypted using the provided encryption service.
     *
     * @param instance Instance entity
     * @param encryptionService Service to decrypt password
     * @return Database credentials with decrypted password
     */
    public static DatabaseCredentials fromInstance(Instance instance, EncryptionService encryptionService) {
        String decryptedPassword = encryptionService.decrypt(instance.getDatabasePassword());
        return DatabaseCredentials.builder()
            .databaseUrl(instance.getDatabaseUrl())
            .username(instance.getDatabaseUsername())
            .password(decryptedPassword)
            .build();
    }
}
