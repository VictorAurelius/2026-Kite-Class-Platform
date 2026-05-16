# Threat Model — Auth Flow Magic-Link Login

**Created:** 2026-05-16
**Wave:** 86 Bucket E
**Status:** complete
**Scope:** Magic-link login flow (passwordless email link) — issue, transport, redeem, session bootstrap
**Linked gaps:** GAP-584 (CF Page Rules cache bypass), GAP-582 (magic-link redeem hardening), Wave 72b auth hardening
**Mitigation owners:** kitehub-platform AuthService, kitehub-email, Cloudflare config

---

## 1. Asset under threat

Magic-link là URL form `https://kitehub.me/auth/magic?token=<opaque-string>` mà KiteHub email tới user mailbox. User click → backend redeem → JWT issued → dashboard load.

**Trust boundaries crossed:**
1. Backend → SES (token sent qua email)
2. SES → user mailbox (3rd-party transport)
3. User mailbox → browser (user clicks link)
4. Browser → Cloudflare edge (cache + WAF)
5. Cloudflare edge → ALB → kitehub-platform (redeem)
6. kitehub-platform → RDS (mark token used + issue session)

---

## 2. STRIDE analysis

### S — Spoofing identity

| # | Threat | Likelihood | Impact | Existing mitigation | Gap / follow-up |
|---|--------|-----------|--------|--------------------|-----------------|
| S1 | Attacker forges magic-link token (predictable PRNG) | Low | Critical | `SecureRandom` 256-bit opaque token; never guessable | — |
| S2 | Attacker captures token from email transit (MITM) | Low | High | TLS 1.2+ enforced SES → recipient MX; opportunistic STARTTLS for inbound mail; user MX records mostly support DANE/MTA-STS | Document recommendation: corporate user MX should enforce TLS |
| S3 | Email forwarded — non-original user clicks link | Medium | High | TTL 15 min (BR-AUTH-MAGIC-001) + single-use redeem (BR-AUTH-MAGIC-002); device fingerprint compared post-redeem | Add IP-binding optional opt-in (post-launch) |
| S4 | Login form CSRF — attacker submits user email to trigger magic-link to user's inbox | Low | Low | Rate-limit per-email 1 request / 60s; CAPTCHA after 3 failures | — |

### T — Tampering

| # | Threat | Likelihood | Impact | Existing mitigation | Gap / follow-up |
|---|--------|-----------|--------|--------------------|-----------------|
| T1 | Token query-string tampered (`?token=...` modified mid-transit) | Low | Low | Opaque random — modification breaks lookup (HTTP 404) | — |
| T2 | Cloudflare cache caches `/auth/magic?token=...` → 2nd user gets cached redeem | Medium | Critical | GAP-584 — Page Rule `/auth/magic` cache=bypass; verified live Wave 86 | Monitor CF cache-hit metric — must stay 0 for `/auth/magic` |
| T3 | Replay attack — attacker captures full URL + replays | Low | Critical | Single-use atomic DB flag `magic_link_redeemed_at` set BEFORE JWT issued; 2nd redeem → 410 Gone | — |

### R — Repudiation

| # | Threat | Likelihood | Impact | Existing mitigation | Gap / follow-up |
|---|--------|-----------|--------|--------------------|-----------------|
| R1 | User claims "I didn't click that link" / "tôi không có xoá data" | Medium | Medium | `admin_audit_logs` immutable table (V60 Wave 85) — append-only, PDPL Art 11 compliant; logs user_id + ip + ua + magic_token_hash on redeem | — |
| R2 | Provider claims email was sent but no audit | Low | Low | SES delivery dashboard; `email_audit_log` row per send w/ messageId | Add SES bounce/complaint webhook (Wave 87+) |

### I — Information Disclosure

| # | Threat | Likelihood | Impact | Existing mitigation | Gap / follow-up |
|---|--------|-----------|--------|--------------------|-----------------|
| I1 | Magic-link logged in CloudWatch / nginx logs (full token visible to ops) | Medium | High | PII scrubber strips `token=` query-string per `.claude/rules/logs-format-standard.md` §3.1 | Verify scrubber covers magic + invite query patterns (test fixture) |
| I2 | Email body cached in user mailbox provider — attacker who later compromises mailbox reads token | Medium | High | TTL 15 min reduces window; single-use redeem invalidates immediately on click | Force re-authentication for sensitive actions even with valid session (step-up) |
| I3 | Referer header leaks `/auth/magic?token=...` when user clicks external link from dashboard | Low | High | `Referrer-Policy: strict-origin-when-cross-origin` set in new CSP (Wave 86 Fix 3) — referer stripped on cross-origin nav | — |

### D — Denial of Service

| # | Threat | Likelihood | Impact | Existing mitigation | Gap / follow-up |
|---|--------|-----------|--------|--------------------|-----------------|
| D1 | Attacker floods `/auth/magic/request` with random emails → DB fills with pending tokens | Medium | Medium | Rate-limit per-IP 10/min + per-email 1/60s; tokens expire 15min → auto-cleanup | Cleanup cron purge expired magic tokens daily (verify) |
| D2 | SES email-sending quota exhausted (Free Tier 200/day) | Medium | Medium | SES production tier increase requested Wave 84 GAP-423 | — |
| D3 | Attacker pre-warms cache by hitting `/auth/magic?token=junk` repeatedly | Low | Low | GAP-584 cache bypass eliminates cache amplification on auth paths | — |

### E — Elevation of Privilege

| # | Threat | Likelihood | Impact | Existing mitigation | Gap / follow-up |
|---|--------|-----------|--------|--------------------|-----------------|
| E1 | Magic-link issued to platform admin email — attacker compromises admin mailbox | Low | Critical | Admin accounts MUST have 2FA enrolled (BR-AUTH-006 totp_required=true via V37); magic-link login disabled for admin role | — |
| E2 | Magic-link redeem returns JWT with wrong role | Low | Critical | JWT issued from DB user.role; role-guard server-side per `pre-handoff-self-test-completeness.md` §2.4 | — |
| E3 | Token re-used across browser/device → session hijack | Low | High | Single-use redeem (T3 mitigation) | — |

---

## 3. Mitigation status summary

| Severity | Total threats | Mitigated | Open follow-up |
|---|---|---|---|
| Critical | 5 (S1/S2 high not crit, T2/T3/E1/E2/E3) | 5 | 0 |
| High | 4 | 4 | 1 (scrubber test fixture I1) |
| Medium | 4 | 3 | 2 (corporate MX TLS recommendation, cleanup cron verify) |
| Low | 4 | 4 | 1 (SES bounce webhook future scope) |

**Verdict:** Acceptable risk posture for Phase 1 BETA. No P0 blockers. 4 follow-ups tracked.

---

## 4. Trust boundary diagram

```
[User Browser]
     |  HTTPS (CSP enforces)
     v
[Cloudflare Edge]  --- Page Rule /auth/magic cache=bypass (GAP-584)
     |  TLS
     v
[ALB] -- WAF rate-limit
     |
     v
[kitehub-platform AuthService]
     |  @Transactional
     v
[RDS users.magic_link_token + magic_link_redeemed_at]
     |
     v
[admin_audit_logs] -- immutable trigger (V60 Wave 85)
```

---

## 5. Test cases (acceptance criteria)

- [ ] T2 verify: `curl -sI https://kitehub.me/auth/magic?token=test` returns `cf-cache-status: BYPASS` (GAP-584 verified Wave 86 ✓)
- [ ] T3 verify: redeem same token twice → 1st returns 200 + JWT, 2nd returns 410
- [ ] R1 verify: redeem operation writes row to `admin_audit_logs` with `event=magic_link_redeem`
- [ ] I1 verify: integration test emits log line containing magic-link URL → assert scrubber masks `token=<REDACTED>`
- [ ] E1 verify: try magic-link login as user with role=PLATFORM_ADMIN → returns 403 "Admins require 2FA"

---

## 6. Open follow-ups

1. **I1 follow-up:** Add unit test fixture for PII scrubber covering magic-link + invite query patterns. Track Wave 87 grooming.
2. **S2 follow-up:** Document recommended user-mailbox MX TLS hardening in user-manual P2/P3. Track Wave 87+.
3. **D1 follow-up:** Verify expired magic-token cleanup cron exists; if not, add `@Scheduled` daily purge. Track Wave 87+.
4. **R2 follow-up:** Wire SES bounce/complaint SNS webhook to email_audit_log. Track post-launch Phase 1.5.

---

## 7. References

- [`pre-launch-auth-hardening-checklist.md`](../../../.claude/rules/pre-launch-auth-hardening-checklist.md) — Cat 4 auth hardening
- [`logs-format-standard.md`](../../../.claude/rules/logs-format-standard.md) §3.1 — PII scrubbing patterns
- [`pre-handoff-self-test-completeness.md`](../../../.claude/rules/pre-handoff-self-test-completeness.md) §2.4 — Admin-flow verify
- GAP-584 (cache bypass `/auth/magic`)
- GAP-582 (magic-link redeem hardening)
- Wave 85 Bucket B (V60 admin_audit_logs immutable)

---

## 8. Log

- **2026-05-16:** Threat model created (Wave 86 Bucket E Fix 4). Baseline 17 threats analyzed; 4 follow-ups filed.
