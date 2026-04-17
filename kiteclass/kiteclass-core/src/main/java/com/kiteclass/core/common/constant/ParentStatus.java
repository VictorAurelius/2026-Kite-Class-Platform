package com.kiteclass.core.common.constant;

/**
 * Lifecycle status of a Parent account.
 *
 * <p>State transitions:
 * <ul>
 *   <li>{@link #PENDING} — created via invitation but not yet redeemed; cannot log in.</li>
 *   <li>{@link #ACTIVE} — redeemed and usable.</li>
 *   <li>{@link #INACTIVE} — admin-disabled or self-deactivated; cannot log in.</li>
 * </ul>
 *
 * @author KiteClass Team
 * @since 2.14.0 (Wave 2 — GAP-052a)
 */
public enum ParentStatus {
    PENDING,
    ACTIVE,
    INACTIVE
}
