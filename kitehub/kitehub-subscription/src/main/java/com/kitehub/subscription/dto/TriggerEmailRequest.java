package com.kitehub.subscription.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Request DTO for manually triggering an email send from admin panel.
 *
 * @since 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TriggerEmailRequest {

    @NotNull
    private UUID instanceId;

    @NotBlank
    private String emailType;
}
