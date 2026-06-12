package com.kitehub.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Instance summary DTO for admin listing.
 *
 * @since 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InstanceSummary {

    /**
     * Instance ID.
     */
    private UUID id;

    /**
     * Organization name.
     */
    private String organizationName;

    /**
     * Subdomain (e.g., "abc" for abc.kitehub.me).
     */
    private String subdomain;

    /**
     * Instance status (TRIAL, ACTIVE, SUSPENDED, EXPIRED, DELETED).
     */
    private String status;

    /**
     * Subscription tier (FREE, BASIC, PREMIUM, ENTERPRISE).
     */
    private String tier;

    /**
     * Owner email.
     */
    private String ownerEmail;

    /**
     * Owner phone.
     */
    private String ownerPhone;

    /**
     * Trial end date (if in trial).
     */
    private LocalDateTime trialEndDate;

    /**
     * Subscription end date (if paid).
     */
    private LocalDateTime subscriptionEndDate;

    /**
     * Database URL.
     */
    private String databaseUrl;

    /**
     * Total users in this instance.
     */
    private Long totalUsers;

    /**
     * Total students in this instance.
     */
    private Long totalStudents;

    /**
     * Total courses in this instance.
     */
    private Long totalCourses;

    /**
     * Created timestamp.
     */
    private LocalDateTime createdAt;

    /**
     * Last updated timestamp.
     */
    private LocalDateTime updatedAt;
}
