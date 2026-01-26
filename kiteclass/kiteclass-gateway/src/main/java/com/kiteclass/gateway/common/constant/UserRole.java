package com.kiteclass.gateway.common.constant;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * User roles for access control.
 *
 * @author KiteClass Team
 * @since 1.0.0
 */
@Getter
@RequiredArgsConstructor
public enum UserRole {

    OWNER("Chủ trung tâm", "Full access to all features"),
    ADMIN("Quản trị viên", "Manage users, classes, billing"),
    TEACHER("Giáo viên", "Manage assigned classes, attendance"),
    STAFF("Nhân viên", "Limited access based on permissions"),
    PARENT("Phụ huynh", "View children's information");

    private final String displayNameVi;
    private final String description;

    /**
     * Returns the Spring Security role name with ROLE_ prefix.
     *
     * @return role name for Spring Security
     */
    public String getSecurityRole() {
        return "ROLE_" + this.name();
    }
}
