---
title: Hardcode + Mock-in-Production State-Check (FE + BE)
audience: mixed
created: 2026-06-15
scope: State-check audit MAPPING hardcoding + mock-in-production across kitehub + kiteclass (NOT a fix). Anchored by umbrella GAP-1410.
method: 2 Opus read-only agents (BE Java + FE TS/TSX) + coordinator synthesis
references:
  - documents/04-quality/gaps/phase-1-beta/GAP-1410-hardcode-mock-umbrella.md
  - .claude/rules/design-source-implementation-parity.md
  - .claude/rules/markdown-variable-reference.md
  - .claude/rules/vn-localization-audit-checklist.md
---

# Hardcode + Mock-in-Production State-Check — 2026-06-15

State-check requested by dev: "hardcode đang là GAP lớn cho cả FE và BE — cần state-check và fix toàn bộ không?" → verdict: state-check YES, fix-toàn-bộ-now NO (enormous + mid flow-verification-campaign + risky). This artifact MAPS + prioritizes; fix in dedicated wave(s) post-campaign (P0 exceptions fixed sooner).

**Critical framing (per dev directive "FE phải phân biệt rõ mock và hardcode"):** two distinct buckets with different nature, fix-direction, and severity:

| | **MOCK-in-production** | **HARDCODE** |
|---|---|---|
| Nature | fake data placeholder — real source NOT wired | value baked — should come from config/i18n/env |
| Fix direction | **wire to real source** (API/auth/store) | **extract** to config key / i18n / env var |
| Severity tendency | FUNCTIONAL — user sees WRONG/fake data → P0/P1 | maintainability/i18n (value works) → P2/P3 unless env/tenant = functional |
| Acceptable case | mock in `__tests__`/storybook/dev-only | immutable constant / English technical token (per `dev-readable-doc-language` §3) |

---

## BUCKET 1 — MOCK-in-production (fix = wire real source)

**FE-dominant; the most serious cluster.** ~36 production pages render fixture datasets → real authenticated beta users see fabricated data.

| Portal | Mock source (render-path imported, NOT test) | Pages | Severity |
|---|---|---|---|
| Teacher (kiteclass-core, Phase 1/2) | `kiteclass-frontend/src/components/teacher/teacher-mock-data.ts` (230 L) | `(teacher)/layout.tsx` + 11 pages | 🔴 P1 |
| Parent (Phase 1/2) | `kiteclass-frontend/src/components/parent/parent-mock-data.ts` (141 L) | 7 `(dashboard)/parent/*` pages — grades/attendance/**billing fabricated** | 🔴 P1 (financial trust) |
| Student (Phase 1/2) | inline `const TODAY_CLASSES/PENDING_TASKS/CLASSES = [...]` | 9 `(dashboard)/student/*` pages | 🔴 P1 |
| School-admin (K-12, Phase 3) | `kitehub-frontend/src/components/school-admin/school-admin-mock-data.ts` (335 L) | shell + 11 pages | 🟡 P3 (roadmap-deferral aligned) |
| Customer instances | `kitehub-frontend/src/app/(customer)/instances/_lifecycle-mock.ts` (140 L) | instances list/detail + branding wizard | 🟡 P2 |

**Top concrete instances:**
- `teacher-shell.tsx:70` — `teacherName = 'Cô Trần Thu Hà'` default rendered at `:146` (masks unwired state instead of erroring)
- `(teacher)/layout.tsx:36` — `teacherName={TEACHER_PROFILE.fullName}` (mock for ALL teachers)
- `(teacher)/teacher/dashboard/page.tsx:52` — `{greeting}, {TEACHER_PROFILE.shortName}` (no API call)
- parent pages `(dashboard)/parent/{page,grades,attendance,billing}` — all import `parent-mock-data`
- `(dashboard)/student/today/page.tsx:35,41` — inline fixture arrays

**BE mock (fewer):** `MockProvisioningService` (branding progress strings), `VietQRService:89` mock QR placeholder — mostly dev-path, lower concern.

**Existing gap coverage:** GAP-268/268a (teacher, PARTIAL **P2 Phase 2** — UNDER-RATED for Phase 1 BETA live persona), GAP-269a (student social-login adjacent), GAP-1091 (closed, MOCK_QUOTA), GAP-1213. **No gap tracks parent-portal-mock or student-portal-mock as a class** → NEW GAP-1411 / GAP-1412.

---

## BUCKET 2 — HARDCODE (fix = extract to config/i18n/env)

**Discipline is actually GOOD overall** — 298 MessageSource usages (BE i18n broadly adopted), ~24 well-formed `@Value` config keys. Concentration of FUNCTIONAL hardcoding:

### C3 Env / URL / ID (largest functional cluster — BE)
- `kitehub-subscription/.../client/EmailServiceClient.java` — ~25 hardcoded URLs with **domain inconsistency**: 9× `kitehub.com`, 9× `kitehub.me`, 3× `kitehub.vn` in ONE file → customer emails point to ≥2 wrong prod domains. 🔴 P1 → NEW GAP-1414
- `OwnerNotificationDispatcher.java:91-139` — `kitehub.me/billing` literals
- `DomainService.java:285` — instance backup URL domain hardcoded
- FE `DEFAULT_TENANT_ID = '11111111-...'` ×10 (`lib/api/tenant-landing.ts:17`, `(public)/layout.tsx`, `sso/callback/page.tsx:24`, `lib/auth/jwt-storage.ts:72`) — cross-tenant landing risk on unresolved tenant 🔴 P1
- Correction to earlier session note: baked tenant-ID is in `docker-compose.kitehub.yml` (env-level demo tenant `n-69fc-…`), NOT FE src.

### C2 Business constants / magic numbers (BE)
- `GradeServiceImpl.java:361` credit `3.0` (GAP-1001) + `:96/:503` + `Grade.java:143` passThreshold `50.0` 🔴 P1 → NEW GAP-1415
- `InvoiceServiceImpl.java:65` LATE_FEE_RATE `0.001` + `InvoiceBatchServiceImpl.java:58` DUE_DAYS `7` 🟠 P1 → extend GAP-108

### C1 User-facing messages (BE)
- Mostly i18n'd. Residual functional: `AuthService.java:178,440,511` (3 VN auth strings not i18n-keyed). Rest = COSMETIC bulk-import parse errors.

### C5 Enum display labels (BE)
- 31 VN labels across 11 enums (`StudentStatus`, `SessionStatus`, `AttendanceStatus` (also mixes UI color into domain enum), `RefundStatus`, etc.) — COSMETIC now but blocks EN locale → GAP-965 extend

### C4 English UI strings (FE)
- ~4 residuals: admin `<h1>Dashboard</h1>` ×3, form-select placeholder (GAP-140). Low.

### C5 FE role/status labels — clean (shared maps, no action).

---

## INTERSECTION — TOP P0 (multi-tenant correctness)

"Tenant resolution stubbed to a constant" = mock-nature (unwired) + hardcode-form (nil-UUID literal). HIGHEST priority — affects RLS isolation across flows:

1. `kiteclass-core/.../parent/notification/impl/ZaloOaNotificationServiceImpl.java:140` — `resolveTenantId()` returns nil-UUID `00000000-...` ("Wave 105 stub / Wave 106 wiring" TODO) → outbox tenant scope broken
2. `kiteclass-core/.../parent/payment/ParentPaymentController.java:208` — `currentTenantId()` returns nil-UUID

→ **NEW GAP-1413 (P0)** — multi-tenant isolation risk, not tracked by any existing gap.

---

## Priority map

| Priority | Items | Action |
|---|---|---|
| **P0** | nil-UUID tenant resolvers (GAP-1413) | Fix SOON (multi-tenant — affects any flow) — exception to post-campaign defer |
| **P1** | parent/student/teacher mock-in-prod (1411/1412/268-rerate), EmailServiceClient domain (1414), grade/invoice constants (1415/108), DEFAULT_TENANT_ID | Dedicated fix wave(s) post-campaign |
| **P2/P3** | enum VN labels (965), English UI (140), school-admin mock (Phase 3), instances lifecycle-mock, cosmetic constants | Opportunistic |

## Gap structure (filed this audit)

- **GAP-1410** umbrella anchor (this state-check)
- **GAP-1411** parent-portal mock-in-production (P1, NEW)
- **GAP-1412** student-portal mock-in-production (P1, NEW)
- **GAP-1413** nil-UUID tenant resolvers (P0, NEW)
- **GAP-1414** EmailServiceClient URL domain inconsistency + externalize (P1, NEW)
- **GAP-1415** grade pass-threshold + grade/invoice business constants → config (P1, NEW)
- Consolidate/extend (not edited here, noted): GAP-268 (teacher mock — re-rate P2→P1 for Phase 1 BETA), GAP-108 (invoice constants), GAP-1001 (grade credit), GAP-965 (enum labels + AttendanceStatus color), GAP-140 (placeholder), GAP-692 (env hardcode — Java-URL overlap)

## Recommendation
1. Fix systematic in dedicated wave(s) **post flow-verification-campaign** (campaign MODE: don't pick-gap-to-fix mid-campaign).
2. **Exception:** GAP-1413 P0 nil-UUID resolvers — fix sooner (multi-tenant correctness).
3. **Meta candidate:** rule "no-mock-in-production-render" (fail-loud when shell/page renders mock instead of wiring real source) + extend `design-source-implementation-parity.md` mock-vs-real distinction. Defer per premature-rule guard until decided.

## Method note
Both agents used Bash+ripgrep (Grep/Glob dedicated tools unavailable in their env) per `mcp-first-with-fallback.md` tier-3 fallback. Counts are estimates (magnitude + representative top instances + severity), not exhaustive enumeration.
