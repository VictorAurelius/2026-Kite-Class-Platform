package com.kiteclass.gateway.module.user.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;

/**
 * Permission entity for granular access control.
 *
 * <p>Permissions define specific actions that can be performed
 * within modules (e.g., USER:CREATE, STUDENT:READ).
 *
 * @author KiteClass Team
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("permissions")
public class Permission {

    @Id
    private Long id;

    @Column("code")
    private String code;

    @Column("name")
    private String name;

    @Column("module")
    private String module;

    @Column("description")
    private String description;

    @Column("created_at")
    private Instant createdAt;
}
