---
title: Wave 104 Bucket E — Live Verify (Partial PASS + 3 New Bugs Surfaced)
status: complete
audience: dev
created: 2026-05-22
phase: phase-1-beta
wave: 104
bucket: E
gaps: [GAP-710, GAP-711, GAP-712, GAP-713]
scope: Bucket E live verify — Bucket A JWT enrichment + Bucket D LoginAudit cooldown VERIFIED PASS; Bucket A end-to-end + Bucket B email send VERIFIED FAIL (3 new bugs filed Wave 105 follow-up)
---

# Wave 104 Bucket E — Live Verify Report

## TL;DR

Per Wave 104 plan §3 Bucket E AC: 6 verify steps + audit doc. Executed 4/6 steps. **2 Wave 104 fixes verified PASS** (Bucket A JWT enrichment + Bucket D LoginAudit cooldown). **3 new bugs surfaced** (gateway tenant resolver + controller tenant fallback + email service config key drift) — filed GAP-711/712/713 Wave 105 candidates per Bucket E AC ("if surfaced → file Wave 105 follow-up gap").

| Step | Status | Outcome |
|---|---|---|
| 1. Pre-check infra healthy | ✅ PASS | 13/13 services healthy post infra-restart + 3-service rebuild |
| 2. Owner persona walk → /onboarding-progress 200 (no X-Tenant-Id) | 🔴 FAIL → 2 bugs | Bucket A JWT enrich OK; controller+gateway not wired |
| 3. Mailhog +1 approval email | 🔴 FAIL → 1 bug | Email send to wrong host (`localhost:8083`) |
| 4. Mailhog × 5 email types | ⏳ SKIP | Blocked by Step 3 finding |
| 5. 2FA enroll via gateway:9000 | ⏳ SKIP | Blocked by Step 2 gateway finding |
| 6. GAP-707 log scan (5× login, 0 WARN) | ✅ PASS | Bucket D fix verified |
| 7. This audit doc | ✅ done | — |

---

## 1. Session context

- **Date:** 2026-05-22 (UTC)
- **Wave 104 merge:** 2026-05-22T09:17:13Z (`b5ec57d6` PR #1712)
- **Stack state at verify time:** 13/13 healthy, 3 services rebuilt với Wave 104 code
- **Branch:** `chore/wave-104-bucket-e-partial-verify-and-rebuild-fix`

## 2. Pre-check infrastructure

Initial state surfaced cascade failure (5 infra containers exit 255, kitehub-email FailingStreak=254 unhealthy). Recovery via `bash kitehub/scripts/up.sh` → 13/13 healthy. Image rebuild for 3 services (subscription/email/gateway) post-Wave-104. See §3.1-§3.2 of earlier audit version.

| Image | Created | Post-Wave-104? |
|---|---|---|
| `kitehub-subscription:latest` | 2026-05-22T10:15:08Z | ✅ |
| `kitehub-email:latest` | 2026-05-22T10:17:41Z | ✅ |
| `kite-gateway:latest` | 2026-05-22T10:22:11Z | ✅ |

## 3. Side-fix: `rebuild.sh` `gateway` mapping

`bash kitehub/scripts/rebuild.sh gateway` failed `no such service: kitehub-gateway`. Script auto-prepends `kitehub-` for non-`kitehub-`/`kiteclass-` names, but compose service = `kite-gateway`. Patch added special case + `kite-*` prefix exclusion. Verified.

## 4. Step 2 — Owner persona walk

### 4.1 Flow execution

| # | Step | Method | Result |
|---|---|---|---|
| 1 | Anonymous beta-request | `POST /api/v1/auth/request-beta-access` | HTTP 201, id=4, status=PENDING |
| 2 | Admin login | `POST /api/auth/login` admin@kitehub.com | HTTP 200, JWT issued (first attempt cold-JVM 503) |
| 3 | Admin approve | `POST /api/v1/admin/beta-requests/4/approve` | HTTP 200, status PENDING→APPROVED, invite_token persisted in DB |
| 4 | Beta signup | `POST /api/v1/auth/beta-signup` token + subdomain + owner password | HTTP 200, status APPROVED→SIGNED_UP, Owner user + Instance row created |
| 5 | Owner login | `POST /api/auth/login` hong.test+w104e-*@skyedu.vn | HTTP 200, JWT issued |

### 4.2 Bucket A AC verify — JWT enrichment

**Owner JWT payload (decoded):**
```json
{
  "sub": "d4f26f27-d7bd-4aa1-8940-2da1ae54cc29",
  "email": "hong.test+w104e-1779446457@skyedu.vn",
  "role": "OWNER",
  "type": "access",
  "tenantId": "96cc496c-d6bc-4f59-b600-73856000d440",
  "iat": 1779446651,
  "exp": 1779533051
}
```

✅ **`tenantId` claim present** and matches instance `id` from DB. Bucket A JWT issuance side: **PASS**.

### 4.3 Bucket A AC verify — End-to-end onboarding-progress (FAIL)

Wave 104 plan §3 Bucket A line 122: `"curl GET /api/v1/onboarding-progress Bearer Owner JWT → 200 OK (no X-Tenant-Id header needed)"`.

Test (NO X-Tenant-Id header):
```bash
curl -X GET http://localhost:9000/api/v1/onboarding-progress \
  -H "Authorization: Bearer $OWNER_JWT"
```
**Result:** HTTP 400 (content-length 0).

Adding X-Tenant-Id header → still HTTP 400 via gateway. Direct subscription :8081 + spoofed `X-User-Roles: OWNER` + X-Tenant-Id → **HTTP 200 with full payload**.

**Root cause analysis:**

**Bug 1 — Gateway `TenantResolverGatewayFilterFactory` doesn't fallback to JWT claim:**
```
2026-05-22 10:44:25.468 DEBUG c.k.g.f.TenantResolverGatewayFilterFactory - TenantResolverFilter: Host = localhost
2026-05-22 10:44:25.983 WARN  c.k.g.f.TenantResolverGatewayFilterFactory - Could not resolve tenant from request
```
Gateway resolves tenant only via Host header (subdomain). `localhost` has no tenant subdomain → resolution fails → gateway rejects/strips downstream context. Should fallback to JWT `tenantId` claim when Host-based resolution returns null.

**Bug 2 — `OnboardingProgressController.resolveTenant()` still requires X-Tenant-Id header:**
```java
private UUID resolveTenant(String tenantHeader, String authorizationHeader) {
    if (tenantHeader == null || tenantHeader.isBlank()) {
        throw new TenantContextMissingException("X-Tenant-Id header missing");
    }
    ...
}
```
Method throws if header missing. Should fallback to JWT `tenantId` claim (`extractJwtTenantClaim` helper exists but only used for CROSS-CHECK, not for primary resolution).

### 4.4 Bucket A verdict

- JWT issuance (Bucket A primary scope): ✅ PASS
- End-to-end AC (no X-Tenant-Id header): ❌ FAIL
- Owner walk: passes if X-Tenant-Id + X-User-Roles forwarded by gateway, but **gateway doesn't forward them in local stack**
- Wave 103 GAP-531 PARTIAL 70% remains **PARTIAL** — JWT enrichment shipped, gateway+controller consumption NOT wired

→ File **GAP-711** (gateway tenant resolver JWT fallback) + **GAP-712** (controller tenant fallback) Wave 105.

## 5. Step 3 — Mailhog approval email (FAIL)

Mailhog inbox count before approve: 0. After approve: 0 (polled 5× 2s intervals).

**Root cause (Bug 3 — config key drift):**

`kitehub/kitehub-subscription/src/main/resources/application.yml` has **two duplicate keys for email service URL**:
```yaml
email-service:
  url: ${EMAIL_SERVICE_URL:http://kitehub-email:8080}   # ← correct default for Docker
  ...
email:
  service:
    url: ${EMAIL_SERVICE_URL:http://localhost:8083}     # ← wrong default for Docker network
```

`EmailConsumer` uses the second key. Runtime log:
```
ERROR c.k.s.consumer.EmailConsumer - Failed to send email via HTTP: type=beta-invite, to=h***@skyedu.vn,
       error=I/O error on POST request for "http://localhost:8083/api/platform/emails/send": null
```
`localhost` inside subscription container = subscription itself, not email service. Send always fails. Retries exhausted → DLQ.

**Impact:** Bucket B1 RabbitMQ event flow ✅ WIRED correctly (`Processing email event: type=beta-invite` log line confirms wire-up), but HTTP send to email service fails because URL points to wrong host. Approval email Bucket B1 fix is **partially correct** — Wave 104 wired event publish but didn't fix the downstream URL config.

Same root cause also affects:
- `admin-new-login-alert` emails (same I/O error in logs)
- All 5 email types Step 4 would test → all fail same way

→ File **GAP-713** (consolidate `email.service.url` vs `email-service.url` config keys) Wave 105.

## 6. Step 6 — GAP-707 log scan (PASS)

```
Attempt 1: HTTP 200
Attempt 2: HTTP 200
Attempt 3: HTTP 200
Attempt 4: HTTP 200
Attempt 5: HTTP 200

Count of "Query did not return a unique result" WARN: 0
```

✅ **Bucket D fix VERIFIED.** LoginAuditService cooldown logic eliminates duplicate-row WARN from Wave 103 finding.

## 7. Wave 104 fix verification matrix

| Wave 104 fix | Bucket | Status | Evidence |
|---|---|---|---|
| GAP-704: JWT tenantId claim enrichment | A | ✅ JWT issuance PASS | Owner JWT payload decoded shows `tenantId` |
| GAP-704: End-to-end JWT-only tenant resolution | A | 🔴 FAIL | `onboarding-progress` returns 400 without X-Tenant-Id; gateway + controller not wired |
| GAP-702: Approval email wire | B1 | 🟡 PARTIAL | RabbitMQ event flow ✅; HTTP delivery fails due to URL config drift (independent bug) |
| GAP-703: Multipart + List-Unsubscribe | B2 | ⏳ NOT TESTED | Blocked by GAP-713 email config |
| GAP-705: Gateway HS256 challenge token | C1 | ⏳ NOT TESTED | Standard JWT routing OK; challenge path untested |
| GAP-706: Subscription ChallengeTokenAuthenticationFilter | C2 | ⏳ NOT TESTED | Blocked by gateway test |
| GAP-707: LoginAudit duplicate-row fix | D | ✅ PASS | 5× login, 0 WARN |

## 8. Wave 103 PARTIAL gap status — REVISED

| Gap | Pre-Wave-104 | Post-Wave-104-merge | Post-Bucket-E-verify (this) |
|---|---|---|---|
| GAP-531 | 70% | 70% (claimed by plan) | **70% unchanged** — JWT shipped but gateway+controller not wired |
| GAP-516 | 90% | 90% (claimed by plan) | **90% unchanged** — Bucket C not testable yet |
| GAP-543 | 65% | 65% (claimed by plan) | **65% unchanged** — Bucket B2 not testable |
| GAP-657 | 40% | 40% (claimed by plan) | **40% unchanged** — Bucket B2 not testable |
| GAP-659 | 50% | 50% (claimed by plan) | **50% unchanged** — persona tone not tested |
| GAP-702 | NEW (DONE 100% on code merge) | DONE | **75% revised** — event wired but delivery broken |
| GAP-703 | NEW (DONE 100% on code merge) | DONE | **50% revised** — not testable yet |
| GAP-704 | NEW (DONE 100% on code merge) | DONE | **60% revised** — JWT issuance OK but end-to-end broken |
| GAP-705 | NEW (DONE 100% on code merge) | DONE | **50% revised** — not tested |
| GAP-706 | NEW (DONE 100% on code merge) | DONE | **50% revised** — not tested |
| GAP-707 | NEW (DONE 100% on code merge) | DONE | **100% confirmed** — log scan PASS |

## 9. Follow-up gaps filed (Wave 105 candidates)

| Gap | Title | Priority | Phase | Trigger |
|---|---|---|---|---|
| **GAP-711** | Gateway TenantResolverFilter JWT claim fallback | P1 | phase-1-beta | Wave 104 Bucket A end-to-end AC requires gateway to derive tenant from JWT when Host-based resolution fails |
| **GAP-712** | OnboardingProgressController.resolveTenant() JWT fallback | P1 | phase-1-beta | Wave 104 Bucket A end-to-end AC requires controller to derive tenant from JWT when X-Tenant-Id header missing |
| **GAP-713** | Email service URL config key drift (`email.service.url` vs `email-service.url`) | P0 | phase-1-beta | Wave 104 Bucket B email delivery to ALL recipients blocked; Bucket B1/B2 cannot be verified end-to-end |

## 10. Cross-links

- Wave 104 plan: `documents/03-planning/waves/wave-2026-05-22-104-fix-followup-bugs.md` §3 Bucket E
- Wave 104 follow-up cluster: `documents/04-quality/gaps/phase-1-beta/GAP-710-wave-104-follow-up-cluster.md`
- Wave 103 owner walk pattern: `documents/04-quality/audits/local-stack/2026-05-22-wave-103-owner-persona-walk.md`
- Side-fix PR scope: `kitehub/scripts/rebuild.sh` patch
- New gaps filed: GAP-711, GAP-712, GAP-713 (this PR)
- Bucket E AC met (Wave 104 plan §3 Bucket E lines 231-235): comparison table ✅, no new bugs OR file Wave 105 ✅ (filed 3)

## 11. Out-of-scope this session (next session pickup)

- Bucket B2 verify (multipart + List-Unsubscribe) → blocked by GAP-713
- Bucket C verify (2FA via gateway HS256 path) → blocked by GAP-711
- Bucket C2 verify (ChallengeTokenAuthenticationFilter) → blocked by GAP-711
- GAP-659 persona-tone matrix → out-of-scope per plan
