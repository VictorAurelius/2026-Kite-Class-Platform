package com.kiteclass.core.module.marketing.dto.request;

import com.kiteclass.core.module.marketing.enums.LeadSource;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for creating a new lead (trial registration).
 * Business Rule: BR-MKT-002 - Lead email must be unique per tenant.
 * Business Rule: BR-MKT-004 - Lead creation sends confirmation email.
 *
 * @since 2.10
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateLeadRequest {

    @NotBlank(message = "{lead.email.required}")
    @Email(message = "{lead.email.invalid}")
    @Size(max = 255, message = "{lead.email.size}")
    private String email;

    @NotBlank(message = "{lead.name.required}")
    @Size(max = 200, message = "{lead.name.size}")
    private String name;

    @Size(max = 20, message = "{lead.phone.size}")
    private String phone;

    private LeadSource source;

    private Long courseInterestId;

    private String message;
}
