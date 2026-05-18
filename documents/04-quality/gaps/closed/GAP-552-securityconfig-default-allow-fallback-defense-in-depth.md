# GAP-552: SecurityConfig `.anyRequest().permitAll()` default-allow fallback — defense-in-depth gap

**Status:** 🔵 OPEN
**Priority:** 🟠 P1
**Domain:** Backend (Security)
**Found:** 2026-05-14 (Wave 78 post-wave Security /100 audit — P1-1)
**Affects:** `kitehub/kitehub-subscription/src/main/java/com/kitehub/subscription/config/SecurityConfig.java:86`
**Phase:** Phase 1 BETA pre-launch
**Standards:** OWASP A05 (Security Misconfiguration) + A01 (Broken Access Control) + `pre-launch-owasp-rest-hardening-checklist.md` §2.1 + §2.5

## Problem

`SecurityConfig.securityFilterChain()` (non-test profile) sử dụng pattern:

```java
.authorizeHttpRequests(auth -> auth
    .requestMatchers("/api/v1/auth/**").permitAll()
    .requestMatchers("/api/v1/beta-status/**").permitAll()
    .requestMatchers("/actuator/**").permitAll()
    .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
    .requestMatchers("/api/v1/admin/**").authenticated()
    .requestMatchers("/api/v1/onboarding-progress/**").authenticated()
    .anyRequest().permitAll()  // ❌ DEFAULT-ALLOW
)
```

Đây là **anti-pattern default-allow**. Mỗi endpoint mới mặc định public trừ khi explicitly match một trong các requestMatchers. Wave 78 ship 4 controllers mới:
- `BetaStatusController` (`/api/v1/beta-status/**`) ✅ explicit permitAll
- `OnboardingProgressController` (`/api/v1/onboarding-progress/**`) ✅ explicit authenticated
- `FeedbackController` (`/api/v1/feedback`) ⚠️ FALLBACK qua anyRequest permitAll (đúng intent nhưng không explicit)
- `TwoFactorController` (`/api/auth/2fa/**`) ⚠️ FALLBACK qua anyRequest permitAll — controller tự enforce challenge token nhưng KHÔNG có Spring Security guard

## Root Cause

- Pattern `.anyRequest().permitAll()` được giữ vì existing endpoints chưa migrate qua Spring Security gate (rely on gateway-level auth + controller-level `@PreAuthorize`).
- Default-deny migration cần audit toàn bộ public surface — work chưa scheduled.

## Impact

**P1 (defense-in-depth gap, không phải immediate exploit):**
- Mỗi endpoint mới ship sau Wave 78 mặc định public trừ khi dev nhớ add requestMatcher.
- 2FA endpoints (`/api/auth/2fa/**`) hiện không có Spring Security layer — nếu controller-level challenge token check ever bị refactor sai → no fallback gate.
- Audit-wise: Spring Security best practice là default-deny; default-allow là deviation cần document.

## Proposed Fix

### Option A (preferred — explicit allowlist + default-deny)

```java
.authorizeHttpRequests(auth -> auth
    // Public surface — explicit allowlist
    .requestMatchers("/api/v1/auth/**").permitAll()
    .requestMatchers("/api/v1/beta-status/**").permitAll()
    .requestMatchers("/api/v1/feedback").permitAll()
    .requestMatchers("/actuator/**").permitAll()
    .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
    // Authenticated surface
    .requestMatchers("/api/auth/2fa/**").authenticated()
    .requestMatchers("/api/v1/admin/**").authenticated()
    .requestMatchers("/api/v1/onboarding-progress/**").authenticated()
    // Default-deny
    .anyRequest().denyAll()
)
```

Trade-off: cần audit ALL existing endpoints để đảm bảo không break. Recommend test profile coverage first.

### Option B (minimum viable — just close the 2FA gap)

Giữ default-allow nhưng add 2FA endpoints vào authenticated list:

```java
.requestMatchers("/api/auth/2fa/**").authenticated()
.anyRequest().permitAll()  // documented exception
```

Plus add comment ghi rõ tại sao default-allow + reference gap này cho future migration.

## Acceptance Criteria

- [ ] `SecurityConfig` migrate sang default-deny pattern (Option A) HOẶC explicit document default-allow với reference link
- [ ] 2FA endpoints `/api/auth/2fa/**` có Spring Security `.authenticated()` matcher
- [ ] Feedback endpoint `/api/v1/feedback` có explicit permitAll (chứ không fallback)
- [ ] Integration test: GET random non-existent path `/api/v1/random-xyz` → 401 (Option A) hoặc 404 (Option B)
- [ ] Test suite passes (no regression on existing endpoints)

## Related

- Parent audit: `documents/04-quality/audits/security/2026-05-14-post-wave-78.md` §P1-1
- Rule: `pre-launch-owasp-rest-hardening-checklist.md` §2.1 (A01 per-resource authz) + §2.5 (A05 misconfig)
- Sister: GAP-547 (FeedbackController gateway routing) — same bucket of "endpoint ship checklist" gaps

## Log

- **2026-05-14:** DONE — Wave 79 Bucket C closure. SecurityConfig anyRequest().authenticated() default-deny + explicit allowlist (actuator/health, signup, 2FA) shipped; defense-in-depth defense for missed endpoints (PR #1367).

- **2026-05-14**: Filed from Wave 78 post-wave Security audit (89/100 B+).
