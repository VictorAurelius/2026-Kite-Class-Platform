package com.kiteclass.gateway.module.user.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for Role information.
 *
 * @author KiteClass Team
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoleResponse {

    private Long id;
    private String code;
    private String name;
    private String description;
    private Boolean isSystem;
}
