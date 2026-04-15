/**
 * Security SPI — interfaces only in this sub-PR; concrete implementations arrive in
 * Sub-PR 4.2 (SVG sanitizer, URL allowlist, CSRF token).
 *
 * <p>Three layers per ADR-011 "Defense-in-Depth":
 * <ol>
 *   <li>Input sanitization at ingress — {@link SvgSanitizer}, {@link UrlAllowlistValidator}</li>
 *   <li>Output encoding at egress — handled by templating layer (Next.js), not this package</li>
 *   <li>Request-state protection — {@link CsrfTokenProvider}</li>
 * </ol>
 *
 * <p>Kept as interfaces in the foundation so Sub-PR 4.2 agent can swap concrete impl
 * (JSoup, Apache HttpComponents, custom) without touching consumers in Sub-PRs 4.1/4.3/4.4.
 *
 * @since 3.23.0 (Wave 4 Sub-PR 4.0, ADR-011)
 */
package com.kiteclass.core.common.security;
