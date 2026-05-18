package com.kitehub.email.api;

/**
 * Persona-aware tone register for email content (GAP-659 — Wave 98 Bucket B1).
 *
 * <p>Vietnamese business email convention surfaces 4 distinct registers per
 * recipient seniority/role. External benchmark sources: Misa eInvoice templates,
 * Talkpal VN formal email guide, Travel With Languages VN email convention.</p>
 *
 * <p><b>Wave 98 simplification:</b> all templates default to
 * {@link #FORMAL_SAFE_DEFAULT} salutation ("Kính gửi anh/chị ...", closing
 * "Trân trọng, Đội ngũ KiteHub"). The resolution logic per role is wired
 * (see {@link EmailTemplateRenderer}) but the per-tone variant templates
 * defer to Wave 99 per GAP-659 §Step 2.</p>
 *
 * <p>Per-role mapping (Wave 99+ when variants ship):</p>
 * <ul>
 *   <li>{@code PLATFORM_ADMIN}, {@code CENTER_OWNER} → {@link #FORMAL_AUTHORITY}</li>
 *   <li>{@code CENTER_MANAGER} → {@link #SEMI_FORMAL_PEER}</li>
 *   <li>{@code TEACHER} (solo) → {@link #INFORMAL_FRIEND}</li>
 *   <li>Anonymous / unknown → {@link #FORMAL_SAFE_DEFAULT}</li>
 * </ul>
 *
 * @since Wave 98 Bucket B1 (GAP-659)
 */
public enum Tone {

    /**
     * Highest register — authority figures (P2 Center Owner, Platform Admin).
     * Salutation: {@code Kính gửi chị/anh {Name},}.
     * Vocabulary: senior register, full honorifics, longer sentences.
     */
    FORMAL_AUTHORITY,

    /**
     * Mid register — peer professionals (P3 Center Manager).
     * Salutation: {@code Chào anh/chị {Name},}.
     * Vocabulary: professional but not stiff.
     */
    SEMI_FORMAL_PEER,

    /**
     * Friendly register — solo teachers, casual relationships (P1).
     * Salutation: {@code Chào bạn,}.
     * Vocabulary: action-oriented, shorter sentences.
     */
    INFORMAL_FRIEND,

    /**
     * Safe default — used when recipient role unknown OR anonymous prospect.
     * Salutation: {@code Kính gửi anh/chị {Name},} or {@code Kính gửi Quý khách,}.
     * Safer to over-formalize than under-formalize for authority figures in
     * VN business culture (trust-burning if too casual to senior recipient).
     *
     * <p>Wave 98 — all templates default here per GAP-659 §Step 4.</p>
     */
    FORMAL_SAFE_DEFAULT;

    /**
     * Resolve {@link Tone} from a recipient role string (typically a user role
     * enum value like {@code "PLATFORM_ADMIN"}). Unknown/null inputs map to the
     * safe default per Wave 98 simplification.
     *
     * @param role recipient role identifier (case-insensitive); may be null
     * @return matching {@link Tone}; {@link #FORMAL_SAFE_DEFAULT} if unknown
     */
    public static Tone fromRole(String role) {
        if (role == null || role.isBlank()) {
            return FORMAL_SAFE_DEFAULT;
        }
        switch (role.trim().toUpperCase()) {
            case "PLATFORM_ADMIN":
            case "CENTER_OWNER":
            case "P2_CENTER_OWNER":
                return FORMAL_AUTHORITY;
            case "CENTER_MANAGER":
            case "P3_CENTER_MANAGER":
                return SEMI_FORMAL_PEER;
            case "TEACHER":
            case "P1_SOLO_TEACHER":
            case "SOLO_TEACHER":
                return INFORMAL_FRIEND;
            default:
                return FORMAL_SAFE_DEFAULT;
        }
    }
}
