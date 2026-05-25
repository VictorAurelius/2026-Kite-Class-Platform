package com.kitehub.subscription.consent.immutable;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Per-resource authorization bean for {@link ImmutableConsentController} — Wave
 * beta-readiness-8 Bucket A (GAP-737).
 *
 * <p>Closes OWASP A01 (Broken Access Control) IDOR vector on consent v2 endpoints:
 * before this bean, any authenticated user could read OR write consent rows belonging
 * to a different user by supplying that user's numeric {@code userId} in the request
 * body / path. The endpoints carry GDPR/PDPL audit content — cross-user read leaks
 * privacy posture, cross-user write tampers with the immutable audit chain.</p>
 *
 * <h3>Decision rule (cf. {@link com.kitehub.subscription.config.SecurityConfig}
 * {@code XUserRolesHeaderFilter})</h3>
 *
 * <p>The gateway forwards the caller's numeric identity in the {@code X-User-Id}
 * header and roles in {@code X-User-Roles}. {@code XUserRolesHeaderFilter} maps both
 * into a Spring {@link Authentication} whose {@link Authentication#getName() name}
 * holds the {@code X-User-Id} value verbatim. {@link #canAccessUser(Long)} compares
 * that verbatim value to the requested {@code userId}, with a platform-admin escape
 * hatch.</p>
 *
 * <p>Wave 105 Bucket C teacher-per-class authz used the same {@code @authz.canX(...)}
 * SpEL idiom; this bean mirrors that pattern.</p>
 *
 * @since Wave beta-readiness-8 Bucket A — GAP-737
 */
@Component("consentAuthz")
@Slf4j
public class ConsentAuthorizationBean {

    /** Role name used by {@code SecurityConfig} for platform-level admins. */
    static final String PLATFORM_ADMIN_ROLE = "ROLE_PLATFORM_ADMIN";

    /**
     * Grant access only when the caller is the same numeric user OR a platform admin.
     *
     * <p>Behavior matrix:</p>
     * <ul>
     *   <li>{@code userId == null} → deny (request body validation should catch this
     *       earlier, but the bean must not blindly allow on missing input).</li>
     *   <li>No {@link Authentication} on the security context → deny (the
     *       {@code SecurityConfig} default-deny rule should have already returned 401,
     *       but the bean defends in depth).</li>
     *   <li>Caller has {@code ROLE_PLATFORM_ADMIN} → allow (PDPL audit / DSAR scope).</li>
     *   <li>Caller's principal name equals {@code userId.toString()} → allow.</li>
     *   <li>Anything else → deny.</li>
     * </ul>
     *
     * <p>The principal name is the literal {@code X-User-Id} header value. For numeric
     * user IDs this is the decimal form (matches {@code Long.toString()}). For UUID
     * principals the comparison still works because {@code userId.toString()} produces
     * a numeric form that never collides with a UUID string — the comparison cleanly
     * fails and the platform-admin path remains the only override.</p>
     *
     * @param userId numeric user ID referenced by the request (path variable or body field)
     * @return {@code true} when the caller may act on consent rows belonging to {@code userId}
     */
    public boolean canAccessUser(Long userId) {
        if (userId == null) {
            log.debug("consentAuthz.canAccessUser: null userId → deny");
            return false;
        }
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            log.debug("consentAuthz.canAccessUser: no authentication → deny");
            return false;
        }
        if (hasPlatformAdminRole(auth)) {
            log.debug("consentAuthz.canAccessUser: platform admin → allow (target user={})", userId);
            return true;
        }
        String principalName = auth.getName();
        if (principalName == null) {
            log.debug("consentAuthz.canAccessUser: anonymous principal → deny");
            return false;
        }
        boolean allowed = principalName.equals(userId.toString());
        if (!allowed) {
            log.warn("consentAuthz.canAccessUser: IDOR attempt blocked — principal={} requested userId={}",
                    principalName, userId);
        }
        return allowed;
    }

    private boolean hasPlatformAdminRole(Authentication auth) {
        for (GrantedAuthority authority : auth.getAuthorities()) {
            if (PLATFORM_ADMIN_ROLE.equals(authority.getAuthority())) {
                return true;
            }
        }
        return false;
    }
}
