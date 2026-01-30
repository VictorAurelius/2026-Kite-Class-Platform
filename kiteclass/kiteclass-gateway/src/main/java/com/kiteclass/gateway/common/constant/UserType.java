package com.kiteclass.gateway.common.constant;

/**
 * User type enum for cross-service linking.
 *
 * <p>Defines the type of user account and determines if a referenceId is required
 * to link to Core service entities.
 *
 * <h3>Type Categories:</h3>
 * <ul>
 *   <li><b>Internal Staff (no referenceId):</b> ADMIN, STAFF</li>
 *   <li><b>External Users (with referenceId):</b> TEACHER, PARENT, STUDENT</li>
 * </ul>
 *
 * <h3>ReferenceId Mapping:</h3>
 * <ul>
 *   <li>ADMIN → referenceId = NULL (internal staff, no Core entity)</li>
 *   <li>STAFF → referenceId = NULL (internal staff, no Core entity)</li>
 *   <li>TEACHER → referenceId = teachers.id in Core service</li>
 *   <li>PARENT → referenceId = parents.id in Core service</li>
 *   <li>STUDENT → referenceId = students.id in Core service</li>
 * </ul>
 *
 * <h3>Usage Examples:</h3>
 * <pre>
 * // Admin user (no profile in Core)
 * User admin = new User();
 * admin.setUserType(UserType.ADMIN);
 * admin.setReferenceId(null); // No Core entity
 *
 * // Student user (has profile in Core)
 * User student = new User();
 * student.setUserType(UserType.STUDENT);
 * student.setReferenceId(123L); // Links to students.id = 123 in Core
 * </pre>
 *
 * @see com.kiteclass.gateway.module.user.entity.User
 * @see com.kiteclass.gateway.service.CoreServiceClient
 * @author KiteClass Team
 * @since 1.8.0
 */
public enum UserType {
    /**
     * System administrator - internal staff with full system access.
     * <p><b>ReferenceId:</b> NULL (no Core entity)
     */
    ADMIN,

    /**
     * Staff member - internal staff with limited administrative access.
     * <p><b>ReferenceId:</b> NULL (no Core entity)
     */
    STAFF,

    /**
     * Teacher user - has teaching profile in Core service.
     * <p><b>ReferenceId:</b> teachers.id in Core service
     */
    TEACHER,

    /**
     * Parent user - has parent profile in Core service.
     * <p><b>ReferenceId:</b> parents.id in Core service
     */
    PARENT,

    /**
     * Student user - has student profile in Core service.
     * <p><b>ReferenceId:</b> students.id in Core service
     */
    STUDENT;

    /**
     * Checks if this user type requires a referenceId to Core entity.
     *
     * @return true if TEACHER, PARENT, or STUDENT; false if ADMIN or STAFF
     */
    public boolean requiresReferenceId() {
        return this == TEACHER || this == PARENT || this == STUDENT;
    }

    /**
     * Checks if this user type is internal staff.
     *
     * @return true if ADMIN or STAFF; false otherwise
     */
    public boolean isInternalStaff() {
        return this == ADMIN || this == STAFF;
    }
}
