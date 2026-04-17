package com.kiteclass.core.module.marketing.dto.request;

import com.kiteclass.core.module.marketing.enums.LeadSource;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for updating an existing lead.
 * All fields optional for partial updates (PATCH semantics).
 *
 * @since 2.10
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateLeadRequest {

    @Email(message = "{lead.email.invalid}")
    @Size(max = 255, message = "{lead.email.size}")
    private String email;

    @Size(max = 200, message = "{lead.name.size}")
    private String name;

    @Size(max = 20, message = "{lead.phone.size}")
    private String phone;

    private LeadSource source;

    private Long courseInterestId;

    private String message;
}
