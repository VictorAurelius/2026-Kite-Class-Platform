/**
 * Concrete implementations of the security SPI defined in
 * {@link com.kiteclass.core.common.security}.
 *
 * <p>Shipped by Wave 4 Sub-PR 4.2 per ADR-011 "Defense-in-Depth":
 * <ul>
 *   <li>{@link com.kiteclass.core.common.security.impl.JsoupSvgSanitizer} — JSoup-backed SVG XSS stripper</li>
 *   <li>{@link com.kiteclass.core.common.security.impl.DefaultUrlAllowlistValidator} — SSRF-safe URL gate</li>
 *   <li>{@link com.kiteclass.core.common.security.impl.DoubleSubmitCsrfTokenProvider} — HMAC double-submit CSRF token</li>
 * </ul>
 *
 * <p>Library types (JSoup, java.net.URI) MUST NOT leak across the package boundary — the SPI
 * still exposes only {@link String}/{@code boolean}.
 *
 * @since 3.24.0 (Wave 4 Sub-PR 4.2)
 */
package com.kiteclass.core.common.security.impl;
