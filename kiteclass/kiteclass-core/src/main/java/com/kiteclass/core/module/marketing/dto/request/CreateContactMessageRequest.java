package com.kiteclass.core.module.marketing.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for creating a contact message.
 * Business Rule: BR-MKT-003 - Contact message triggers email to teacher.
 *
 * @since 2.10
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateContactMessageRequest {

    @NotBlank(message = "{contact.name.required}")
    @Size(max = 200, message = "{contact.name.size}")
    private String name;

    // GAP-1221: email optional — phụ huynh VN quen để SĐT; validate format chỉ khi có
    @Email(message = "{contact.email.invalid}")
    @Size(max = 255, message = "{contact.email.size}")
    private String email;

    // GAP-1221: subject optional — server default "Liên hệ từ {name}" khi bỏ trống
    @Size(max = 200, message = "{contact.subject.size}")
    private String subject;

    @NotBlank(message = "{contact.message.required}")
    @Size(max = 2000, message = "{contact.message.size}")
    private String message;

    @Size(max = 20, message = "{contact.phone.size}")
    private String phone;
}
