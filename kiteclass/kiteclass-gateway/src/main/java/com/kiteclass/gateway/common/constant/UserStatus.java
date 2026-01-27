package com.kiteclass.gateway.common.constant;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * User account status values.
 *
 * @author KiteClass Team
 * @since 1.0.0
 */
@Getter
@RequiredArgsConstructor
public enum UserStatus {

    ACTIVE("Đang hoạt động"),
    INACTIVE("Không hoạt động"),
    PENDING("Chờ kích hoạt"),
    LOCKED("Đã khóa");

    private final String displayNameVi;

    /**
     * Checks if the status allows login.
     *
     * @return true if user can login
     */
    public boolean canLogin() {
        return this == ACTIVE;
    }
}
