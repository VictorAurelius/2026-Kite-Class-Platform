package com.kitehub.shared.logging;

import java.util.regex.Pattern;

/**
 * PII Scrubber — masks sensitive content before it reaches the log appender.
 *
 * <p>Patterns are compiled once at class-load time; hot-path cost is bounded by the
 * regex engine. Per the project standard {@code .claude/rules/logs-format-standard.md} §3.1
 * we err on the side of masking — false positives are cheap; PII leaks are not.</p>
 *
 * <p>Used in two contexts:</p>
 * <ul>
 *   <li>Logback encoder — register as a converter (see logback-spring.xml
 *       {@code <conversionRule conversionWord="pii" converterClass="..."/>}).</li>
 *   <li>Programmatic scrubbing in tests / smoke check (see {@link #scrub(String)}).</li>
 * </ul>
 *
 * <p>This class is intentionally framework-light so it can be exercised by unit tests
 * without booting Spring or Logback.</p>
 *
 * <p>Scope: GAP-116 (PII scrubbing in logs). Spec: {@code logs-format-standard.md} §3.1.</p>
 */
public final class PIIScrubber {

    /** Email — [\w._%+-]+@[\w.-]+\.\w{2,} */
    static final Pattern EMAIL = Pattern.compile(
        "([\\w._%+-]+)@([\\w.-]+\\.\\w{2,})"
    );

    /** Vietnamese phone — 10 or 11 digits starting with 0; word-boundary guarded. */
    static final Pattern VN_PHONE = Pattern.compile(
        "\\b(0\\d{9,10})\\b"
    );

    /** Credit-card-shaped sequence — 13–19 digits. */
    static final Pattern CREDIT_CARD = Pattern.compile(
        "\\b(\\d{13,19})\\b"
    );

    /** JWT bearer — three base64url segments separated by dots. */
    static final Pattern JWT_BEARER = Pattern.compile(
        "eyJ[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+"
    );

    /** {@code password=...} / {@code "password":"..."}. Matches both bare and quoted keys. */
    static final Pattern PASSWORD_KW = Pattern.compile(
        "(?i)\"?(password)\"?\\s*[=:]\\s*\"?([^\"\\s,}]+)\"?"
    );

    /** {@code apiKey=}, {@code api_key=}, {@code secret=}, {@code token=} (with case insensitivity). */
    static final Pattern API_KEY_KW = Pattern.compile(
        "(?i)\\b(api[_-]?key|secret|token)\\s*[=:]\\s*\"?([A-Za-z0-9._\\-+/=]{8,})\"?"
    );

    /** Vietnamese national ID — 9 or 12 digits when contextual to "id"/"cccd"/"cmnd". */
    static final Pattern VN_NATIONAL_ID = Pattern.compile(
        "(?i)\\b(cccd|cmnd|id)\\s*[=:]?\\s*(\\d{9}|\\d{12})\\b"
    );

    private PIIScrubber() {
        // utility — no instances
    }

    /**
     * Scrub PII from a single log message.
     *
     * <p>Order matters — we strip JWT/password/key tokens before we mask credit-card-shaped
     * digits, because both can match an opaque token. Email is masked before phone so an
     * email's local-part doesn't get partially eaten by the phone regex.</p>
     *
     * @param input the raw log message; may be {@code null}
     * @return scrubbed message; {@code null} if input was {@code null}
     */
    public static String scrub(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }
        String out = input;

        // 1) JWT — full replacement, kills any structured token first
        out = JWT_BEARER.matcher(out).replaceAll("<REDACTED_JWT>");

        // 2) password=... / "password":"..."
        out = PASSWORD_KW.matcher(out).replaceAll("$1=<REDACTED>");

        // 3) apiKey/secret/token = ...
        out = API_KEY_KW.matcher(out).replaceAll("$1=<REDACTED>");

        // 4) Vietnamese ID (CCCD / CMND, contextual)
        out = VN_NATIONAL_ID.matcher(out).replaceAll(m -> m.group(1) + "=<REDACTED_ID>");

        // 5) Email — mask local part (keep first char + domain for debug context)
        out = EMAIL.matcher(out).replaceAll(m -> {
            String local = m.group(1);
            String domain = m.group(2);
            String head = local.length() > 0 ? local.substring(0, 1) : "";
            return head + "***@" + domain;
        });

        // 6) Vietnamese phone — keep first 2 + last 2, mask middle
        out = VN_PHONE.matcher(out).replaceAll(m -> {
            String d = m.group(1);
            int n = d.length();
            return d.substring(0, 2) + "*".repeat(Math.max(n - 4, 0)) + d.substring(n - 2);
        });

        // 7) Credit-card-shaped digits — keep last 4
        out = CREDIT_CARD.matcher(out).replaceAll(m -> {
            String d = m.group(1);
            int n = d.length();
            if (n < 13) {
                return d;
            }
            return "*".repeat(n - 4) + d.substring(n - 4);
        });

        return out;
    }
}
