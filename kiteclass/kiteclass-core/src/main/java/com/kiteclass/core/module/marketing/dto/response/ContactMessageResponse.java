package com.kiteclass.core.module.marketing.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Response DTO for ContactMessage entity.
 *
 * @since 2.10
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContactMessageResponse {

    private Long id;

    private String name;
    private String email;
    private String phone;

    private String subject;
    private String message;

    private Boolean isRead;
    private LocalDateTime readAt;
    private String readBy;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
