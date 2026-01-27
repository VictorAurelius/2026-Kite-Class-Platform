package com.kiteclass.core.common.constant;

import lombok.Getter;

/**
 * Gender values.
 *
 * @author KiteClass Team
 * @since 2.2.0
 */
@Getter
public enum Gender {

    MALE("Nam"),
    FEMALE("Nữ"),
    OTHER("Khác");

    private final String displayName;

    Gender(String displayName) {
        this.displayName = displayName;
    }
}
