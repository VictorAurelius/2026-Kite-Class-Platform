package com.kiteclass.core.module.auth;

import java.util.regex.Pattern;

/**
 * Unified KC-native login password policy (Wave auth-2 — GAP-1013c).
 *
 * <p>Single source of truth for credential password validation across ALL
 * {@code auth_credentials} provisioning paths (parent self-redeem + admin
 * teacher/student set-password). Before this class, the parent
 * ({@code RedeemInvitationRequest}) and teacher ({@code SetPasswordRequest})
 * DTOs carried two divergent regexes — see business-logic audit P2-1.
 *
 * <p>Policy = 8–100 chars, requires lowercase + uppercase + digit + special
 * char. This is the strict union of the two prior policies:
 * <ul>
 *   <li>stronger than the old teacher policy (which did NOT split upper/lower);</li>
 *   <li>broader symbol set than the old parent policy (any non-alphanumeric,
 *       not just {@code [@$!%*?&#]}) — symbols are not restricted because that
 *       only rejected valid passwords without adding security.</li>
 * </ul>
 *
 * <p>{@link #PATTERN}, {@link #MIN_LENGTH}, {@link #MAX_LENGTH} are compile-time
 * constants so the DTOs can reference them directly in {@code @Pattern} /
 * {@code @Size} annotation attributes.
 */
public final class AuthPasswordPolicy {

    private AuthPasswordPolicy() {
    }

    /** Minimum password length. */
    public static final int MIN_LENGTH = 8;

    /** Maximum password length (BCrypt truncates >72 bytes; cap well below for clarity). */
    public static final int MAX_LENGTH = 100;

    /**
     * Requires ≥1 lowercase, ≥1 uppercase, ≥1 digit, ≥1 non-alphanumeric symbol.
     * Length bounds enforced separately via {@code @Size} / {@link #isValid(String)}.
     */
    public static final String PATTERN =
            "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9]).+$";

    /** Vietnamese validation message reused by both DTOs. */
    public static final String MESSAGE =
            "Mật khẩu phải từ 8-100 ký tự, gồm chữ hoa, chữ thường, số và ký tự đặc biệt";

    private static final Pattern COMPILED = Pattern.compile(PATTERN);

    /**
     * Programmatic validation — defense-in-depth for callers that reach the
     * provisioning service without bean-validation (e.g. internal flows).
     *
     * @param rawPassword candidate password
     * @return true if it satisfies the length bounds AND the complexity pattern
     */
    public static boolean isValid(String rawPassword) {
        return rawPassword != null
                && rawPassword.length() >= MIN_LENGTH
                && rawPassword.length() <= MAX_LENGTH
                && COMPILED.matcher(rawPassword).matches();
    }
}
