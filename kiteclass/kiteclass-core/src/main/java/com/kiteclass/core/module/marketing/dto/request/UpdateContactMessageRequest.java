package com.kiteclass.core.module.marketing.dto.request;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for updating a contact message.
 * All fields optional for partial updates (PATCH semantics).
 *
 * @since 2.10
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateContactMessageRequest {

    @Size(max = 200, message = "{contact.subject.size}")
    private String subject;

    @Size(max = 2000, message = "{contact.message.size}")
    private String message;
}
