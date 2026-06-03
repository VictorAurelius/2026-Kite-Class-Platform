package com.kiteclass.core.module.marketing.entity;

import com.kiteclass.core.common.entity.BaseEntity;
import com.kiteclass.core.module.course.entity.Course;
import com.kiteclass.core.module.marketing.enums.LeadSource;
import com.kiteclass.core.module.marketing.enums.LeadStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Filter;

import java.time.Instant;

/**
 * Lead entity representing potential students.
 * Business Rule: BR-MKT-002 - Lead email must be unique per tenant.
 *
 * @since 2.10
 */
@Entity
@Table(name = "leads")
@Getter
@Setter
@Filter(name = "tenantFilter", condition = "instance_id = :tenantId AND deleted = false")
public class Lead extends BaseEntity {

    // Lead Information
    @Column(name = "email", nullable = false)
    @NotBlank(message = "{lead.email.required}")
    @Email(message = "{lead.email.invalid}")
    @Size(max = 255, message = "{lead.email.size}")
    private String email;

    @Column(name = "name", nullable = false, length = 200)
    @NotBlank(message = "{lead.name.required}")
    @Size(max = 200, message = "{lead.name.size}")
    private String name;

    @Column(name = "phone", length = 20)
    @Size(max = 20, message = "{lead.phone.size}")
    private String phone;

    // Source & Interest
    @Enumerated(EnumType.STRING)
    @Column(name = "source", nullable = false, length = 50)
    private LeadSource source = LeadSource.LANDING_PAGE;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private LeadStatus status = LeadStatus.NEW;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_interest_id")
    private Course courseInterest;

    @Column(name = "message", columnDefinition = "TEXT")
    private String message;

    // Tracking
    @Column(name = "registration_date", nullable = false)
    private Instant registrationDate = Instant.now();

    @Column(name = "last_contacted_at")
    private Instant lastContactedAt;

    @Column(name = "converted_at")
    private Instant convertedAt;
}
