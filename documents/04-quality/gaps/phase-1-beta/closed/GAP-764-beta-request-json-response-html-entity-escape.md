---
audience: dev
---

# GAP-764 — Beta request UTF-8 corruption (HtmlUtils.htmlEscape over-escape)

**Status:** 🟢 DONE 2026-05-27 — P0 escalation + fix shipped Wave 106 same session
**Priority:** 🔴 P0 (escalated from P1 after DB write corruption confirmed)
**Domain:** Backend (sanitization layer)
**Found:** 2026-05-27 (Wave 106 RST Mảng A2 + DB inspection)
**Affects:** All beta_access_request rows shipped post Wave 105 Bucket E0 deploy — name + org_name + referral_source fields with Vietnamese diacritic
**Phase:** phase-1-beta

## Problem (escalated)

POST `/api/v1/auth/request-beta-access` với Vietnamese org name → DB row stored với HTML entity escape thay vì UTF-8 raw:

| Row id | org_name DB value | Status |
|---|---|---|
| 12 (Wave 106 RST POST) | `Trung t&acirc;m Anh ngữ Sky Education` | ❌ CORRUPTED |
| 11 (test smoke earlier) | `Trung t&acirc;m Smoke Test` | ❌ CORRUPTED |
| 10 (pre-Wave-105 ABORTED) | `Trung tâm Sky Education (Q.1)` | ✅ raw |

Vietnamese diacritic `â` (U+00E2) bị escape thành `&acirc;` ngay tại BE write path → persisted vào DB. Email rendering + admin panel + future read paths đều thấy entity literal thay vì raw char.

## Root Cause

`BetaAccessService.sanitizeFreeText()` line 124 (introduced Wave 105 Bucket E0 Bug 2 defense-in-depth XSS sanitization):

```java
return org.springframework.web.util.HtmlUtils.htmlEscape(stripped);  // ❌ single-arg
```

`HtmlUtils.htmlEscape(String)` single-arg variant escapes ALL non-ASCII chars as numeric character references. Vietnamese diacritics (byte range overlap với XSS chars trong basic HTML entity table) → entities like `&acirc;`.

## Fix Shipped (Wave 106 GAP-764 PR — same session)

1. **Code fix:** `HtmlUtils.htmlEscape(stripped)` → `HtmlUtils.htmlEscape(stripped, "UTF-8")` two-arg variant — escapes ONLY 5 XSS chars (`<>&"'`), preserves VN diacritic raw
2. **Flyway V57 migration backfill** — UPDATE existing corrupted rows replacing 14 most-frequent VN diacritic HTML entities back to UTF-8 raw
3. **META rule extension** `vn-localization-audit-checklist.md` v1.0.0 → v1.1.0 §5 "Data roundtrip preservation through sanitization layers" — force-multiplier prevent recurrence cho mọi future input sanitization
4. **Follow-up** GAP-769 — Testcontainers IT VN diacritic roundtrip test (defer next session per scope budget)

## Acceptance Criteria

- [x] DB state verified (CORRUPTED at write — root cause `BetaAccessService.sanitizeFreeText` line 124)
- [x] Sanitization root cause identified (`HtmlUtils.htmlEscape(input)` single-arg over-escape non-ASCII)
- [x] Code fix shipped: response trả raw UTF-8 cho mọi VN diacritic
- [x] Flyway migration backfill corrupted rows (V57)
- [x] META rule force-multiplier prevent recurrence (vn-localization-audit-checklist.md §5)
- [ ] Testcontainers IT VN diacritic roundtrip (defer GAP-769 — follow-up gap filed)

## Verification

```bash
# Post-fix smoke
curl -X POST http://localhost:8081/api/v1/auth/request-beta-access \
  -H "Content-Type: application/json" \
  -d '{"name":"Trần Thị Hồng","email":"hong@verify.vn","phone":"0901234567","orgName":"Trung tâm Sky Education","persona":"P2_CENTER_OWNER","consentGiven":true,"honeypot":""}'
# Expected: response orgName = "Trung tâm Sky Education" (raw, not &acirc;)
# Expected: DB query returns org_name = "Trung tâm Sky Education" (raw UTF-8)
```

## Related

- Wave 106 plan §3 row A2 — `documents/03-planning/waves/wave-2026-05-23-106-rst-phase-1-beta-full-walk.md`
- Wave 105 Bucket E0 Bug 2 origin (introduced single-arg sanitize)
- META rule: `.claude/rules/vn-localization-audit-checklist.md` v1.1.0 §5
- Follow-up: GAP-769 (Testcontainers IT)
- Sister incident class: `.claude/rules/postgres-specific-type-testcontainers.md` (similar test layer gap)

## Log

- **2026-05-27 (DONE):** Wave 106 RST Mảng A2 walk caught corruption. Same session fix shipped: code (HtmlUtils UTF-8 mode) + Flyway V57 backfill + META rule §5 force-multiplier. Production rows id 11+12 will be cleaned on V57 apply. Testcontainers IT deferred GAP-769.
- **2026-05-27 (OPEN, then escalated):** Wave 106 RST Mảng A2 walk caught corruption. Initially filed P1 cho response-only suspect; DB inspection confirmed write-path corruption → escalated P0. Source: PR #1875 Wave beta-prep-1 Bucket E concurrency hardening (HtmlUtils.htmlEscape added to defense-in-depth XSS).
