package com.kiteclass.core.module.clazz.dto;

import java.time.Instant;

/**
 * Response DTO after generating a class enrollment code.
 *
 * @param classCode   Generated or custom class code
 * @param expiresAt   Expiry time (null = no expiry)
 *
 * @author KiteClass Team
 * @since 2.5.0
 */
public record ClassCodeResponse(
        String classCode,
        Instant expiresAt
) {
}
