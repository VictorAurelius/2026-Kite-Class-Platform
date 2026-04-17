package com.kiteclass.core.module.marketing.dto.response;

import com.kiteclass.core.module.marketing.enums.LeadSource;
import com.kiteclass.core.module.marketing.enums.LeadStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Response DTO for Lead entity.
 *
 * @since 2.10
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LeadResponse {

    private Long id;

    private String email;
    private String name;
    private String phone;

    private LeadSource source;
    private LeadStatus status;

    private Long courseInterestId;
    private String message;

    private Instant createdAt;
    private Instant updatedAt;
}
