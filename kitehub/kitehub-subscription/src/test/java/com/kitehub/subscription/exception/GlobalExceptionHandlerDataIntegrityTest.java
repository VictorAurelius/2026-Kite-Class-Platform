package com.kitehub.subscription.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Wave beta-prep-1 Bucket E — Path 1 + Path 5 verification.
 *
 * <p>Unit test (no Spring context needed) verifying the new
 * {@link GlobalExceptionHandler#handleDataIntegrityViolation} branch maps
 * Spring's {@link DataIntegrityViolationException} → RFC 7807 ProblemDetail
 * with HTTP 409 Conflict.</p>
 *
 * <p><b>Investigation finding (release-fix-retry-budget.md §3.5):</b></p>
 * <ul>
 *   <li>Pre-Wave-beta-prep-1 Bucket E state: GlobalExceptionHandler had NO
 *       explicit handler for DataIntegrityViolationException; the generic
 *       Exception handler caught it → HTTP 500 Internal Server Error.</li>
 *   <li>Concurrency hot paths that hit DB-level UNIQUE constraints:
 *       <ul>
 *         <li>Path 1: {@code instances.subdomain} UNIQUE (V1__create_instances_table.sql)</li>
 *         <li>Path 5: {@code user_roles(user_id, role_id) WHERE deleted=FALSE} partial UNIQUE
 *             index (V30__create_role_hierarchy_tables.sql) — kiteclass-core domain
 *             but same exception class</li>
 *       </ul>
 *   </li>
 *   <li>Correct REST semantic per RFC 7231 §6.5.8: HTTP 409 Conflict.</li>
 * </ul>
 *
 * @see GlobalExceptionHandler#handleDataIntegrityViolation
 */
@DisplayName("GlobalExceptionHandler — DataIntegrityViolation → 409 (Bucket E)")
class GlobalExceptionHandlerDataIntegrityTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    @DisplayName("DataIntegrityViolation → ProblemDetail HTTP 409 + RESOURCE_CONFLICT errorCode")
    void handleDataIntegrityViolation_returns_409_problemDetail() {
        DataIntegrityViolationException ex = new DataIntegrityViolationException(
                "duplicate key value violates unique constraint instances_subdomain_key"
        );

        ProblemDetail pd = handler.handleDataIntegrityViolation(ex);

        assertThat(pd.getStatus())
                .as("HTTP status mapped to 409 Conflict")
                .isEqualTo(HttpStatus.CONFLICT.value());
        assertThat(pd.getTitle())
                .as("title human-readable")
                .isEqualTo("Conflict");
        assertThat(pd.getDetail())
                .as("detail provides actionable message")
                .contains("đã tồn tại");
        assertThat(pd.getProperties())
                .as("errorCode property attached for client-side discrimination")
                .containsEntry("errorCode", "RESOURCE_CONFLICT");
    }

    @Test
    @DisplayName("DataIntegrityViolation with nested cause still maps to 409")
    void handleDataIntegrityViolation_withNestedCause_maps_409() {
        Throwable rootCause = new RuntimeException(
                "ERROR: duplicate key value violates unique constraint \"user_roles_unique_active\""
        );
        DataIntegrityViolationException ex = new DataIntegrityViolationException(
                "could not execute statement", rootCause
        );

        ProblemDetail pd = handler.handleDataIntegrityViolation(ex);

        assertThat(pd.getStatus()).isEqualTo(HttpStatus.CONFLICT.value());
    }
}
