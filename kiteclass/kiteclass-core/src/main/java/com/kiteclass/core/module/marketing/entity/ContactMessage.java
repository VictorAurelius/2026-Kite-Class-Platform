package com.kiteclass.core.module.marketing.entity;

import com.kiteclass.core.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Filter;

import java.time.Instant;

/**
 * Contact form message from website visitors.
 * Business Rule: BR-MKT-003 - Contact message triggers email to teacher.
 *
 * @since 2.10
 */
@Entity
@Table(name = "contact_messages")
@Getter
@Setter
@Filter(name = "tenantFilter", condition = "instance_id = :tenantId AND deleted = false")
public class ContactMessage extends BaseEntity {

    // Sender Information
    @Column(name = "name", nullable = false, length = 200)
    @NotBlank(message = "{contact.name.required}")
    @Size(max = 200, message = "{contact.name.size}")
    private String name;

    @Column(name = "email")
    @Email(message = "{contact.email.invalid}")
    @Size(max = 255, message = "{contact.email.size}")
    private String email;

    @Column(name = "phone", length = 20)
    @Size(max = 20, message = "{contact.phone.size}")
    private String phone;

    // Message Content
    @Column(name = "subject", length = 300)
    @Size(max = 300, message = "{contact.subject.size}")
    private String subject;

    @Column(name = "message", nullable = false, columnDefinition = "TEXT")
    @NotBlank(message = "{contact.message.required}")
    private String message;

    // Status
    @Column(name = "is_read", nullable = false)
    private Boolean isRead = false;

    @Column(name = "read_at")
    private Instant readAt;

    @Column(name = "read_by", length = 100)
    private String readBy;

    // Response tracking
    @Column(name = "replied", nullable = false)
    private Boolean replied = false;

    @Column(name = "replied_at")
    private Instant repliedAt;

    @Column(name = "reply_message", columnDefinition = "TEXT")
    private String replyMessage;
}
