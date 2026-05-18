# GAP-553: TOTP cipher key + JWT challenge-secret dev-default fallback — fail-fast guard missing

**Status:** 🔵 OPEN
**Priority:** 🟠 P1
**Domain:** Backend (Secrets/Crypto)
**Found:** 2026-05-14 (Wave 78 post-wave Security /100 audit — P1-2)
**Affects:** `kitehub/kitehub-subscription/src/main/java/com/kitehub/subscription/auth/twofactor/TotpSecretCipher.java:40` + `kitehub/kitehub-subscription/src/main/resources/application.yml:97`
**Phase:** Phase 1 BETA pre-launch
**Standards:** OWASP A02 (Cryptographic Failures) + `pre-launch-secrets-hardening-checklist.md` §2.1 + §2.4

## Problem

Wave 72b Bucket A (PR #1301 rebased + merged trong Wave 78 session) shipped TOTP 2FA scaffolding. Hai dev-default secret fallback có trong code, không có fail-fast guard production:

### Issue 1: TotpSecretCipher dev-default key

```java
public TotpSecretCipher(
    @Value("${kitehub.auth.totp.encryption-key:dev-key-32-chars-pad-pad-pad-pad-pad}") String configuredKey) {
    byte[] keyBytes = new byte[32];
    byte[] src = configuredKey.getBytes(StandardCharsets.UTF_8);
    if (src.length < 32) {
        log.warn("TOTP encryption key length {} < 32; padding with zeros. "
            + "MUST set kitehub.auth.totp.encryption-key in production.", src.length);
    }
    System.arraycopy(src, 0, keyBytes, 0, Math.min(src.length, 32));
    // ...
}
```

- Default value `dev-key-32-chars-pad-pad-pad-pad-pad` là một string predictable trong source code → effectively plaintext.
- Nếu production deploy quên set env var `KITEHUB_AUTH_TOTP_ENCRYPTION_KEY` → all TOTP secrets at rest encrypted với key tất cả mọi người đọc source biết.
- `@PostConstruct validate()` chỉ test round-trip; KHÔNG fail-fast khi key length < 32 hoặc khi key matches dev-default.

### Issue 2: JWT challenge-secret dev-default

`application.yml:97`:
```yaml
challenge-secret: ${JWT_CHALLENGE_SECRET:dev-challenge-secret-pad-pad-pad-pad-pad}
```

Same problem — challenge tokens (5-min TTL, used in 2FA flow between login → verify) có thể bị forge với dev-default secret nếu production quên set env var.

## Root Cause

- Dev-default fallback pattern phổ biến trong codebase (`jwt.secret`, datasource password, v.v.) để dev/test container không cần env var manual.
- Nhưng các secret crypto-grade (encryption key, challenge token signing) cần stricter check — production MUST fail-fast, không fallback.
- Wave 72b shipped scaffolding với plan migrate sang AWS KMS Phase 1.5+ — middle state cần guardrail.

## Impact

**P1 (no immediate exploit nếu env var đúng, NHƯNG ops risk cao):**
- Single env-var miss → entire TOTP secret store + challenge tokens compromised silently
- Boot warn dễ bị overlook trong production log volume
- Cross-ref `pre-launch-secrets-hardening-checklist.md` §2.1 "Zero hardcoded secrets in source" — dev-default strings hiện diện trong source là technical violation (acceptable v1 IF fail-fast guard).

## Proposed Fix

### Step 1: TotpSecretCipher fail-fast trong production profile

```java
private static final String DEV_DEFAULT_KEY = "dev-key-32-chars-pad-pad-pad-pad-pad";

public TotpSecretCipher(
    @Value("${kitehub.auth.totp.encryption-key:" + DEV_DEFAULT_KEY + "}") String configuredKey,
    @Value("${spring.profiles.active:default}") String activeProfile) {

    byte[] src = configuredKey.getBytes(StandardCharsets.UTF_8);
    boolean isDevDefault = DEV_DEFAULT_KEY.equals(configuredKey);
    boolean isProduction = "production".equals(activeProfile) || "prod".equals(activeProfile);

    if (isProduction && (isDevDefault || src.length < 32)) {
        throw new IllegalStateException(
            "TOTP encryption key MUST be set via kitehub.auth.totp.encryption-key " +
            "(≥32 bytes, not dev-default) in production profile. " +
            "Got length=" + src.length + ", isDevDefault=" + isDevDefault
        );
    }

    if (!isProduction && src.length < 32) {
        log.warn("TOTP encryption key length {} < 32; padding for non-prod profile only.", src.length);
    }
    // ... existing init
}
```

### Step 2: JWT challenge-secret same guard

Tạo `ChallengeTokenServiceConfig` hoặc thêm check trong `ChallengeTokenService` constructor — same pattern: fail-fast in production nếu equals dev-default hoặc length insufficient.

### Step 3: Boot smoke test

Add `@PostConstruct` self-test ghi log SUCCESS event với key hash prefix (first 4 chars of SHA-256) để ops verify key được set đúng (không log key itself).

### Step 4: Follow-up gap để migrate sang AWS KMS

File new gap GAP-XXX-totp-kms-migration tracking Phase 1.5+ work per `pre-launch-secrets-hardening-checklist.md` §2.4 — không trong scope GAP-549 này.

## Acceptance Criteria

- [ ] `TotpSecretCipher` throws `IllegalStateException` at boot khi `spring.profiles.active=production` AND (key matches dev-default OR length < 32)
- [ ] `ChallengeTokenService` cùng guard cho `jwt.challenge-secret`
- [ ] Non-production profiles giữ existing behavior (warn only) — preserve dev DX
- [ ] Integration test: SpringBootTest với production profile + missing env var → context refresh fails với clear error message
- [ ] `pre-launch-secrets-hardening-checklist.md` §2.1 check passes (no realistic-looking secret strings in source — dev-default constants có rationale comment adjacent)

## Related

- Parent audit: `documents/04-quality/audits/security/2026-05-14-post-wave-78.md` §P1-2
- Rule: `pre-launch-secrets-hardening-checklist.md` §2.1 + §2.4
- Wave 72b Bucket A (PR #1301): TOTP 2FA backend — this gap is security follow-up
- Sister: `production-env-config-registry.md` §11 (env-coverage audits) — would have caught `KITEHUB_AUTH_TOTP_ENCRYPTION_KEY` missing in production override

## Log

- **2026-05-14:** DONE — Wave 79 Bucket C closure. TotpSecretCipher + ChallengeTokenService @PostConstruct fail-fast guard for production dev-default fallback shipped (OWASP A02) (PR #1367).

- **2026-05-14**: Filed from Wave 78 post-wave Security audit (89/100 B+).
