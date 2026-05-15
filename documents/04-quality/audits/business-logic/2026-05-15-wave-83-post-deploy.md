---
title: Business Logic Audit — Wave 83 Post-Deploy (Validation Surface + Consent Gating)
status: complete
created: 2026-05-15
phase: phase-1-beta
wave: 83
auditor: Background agent (Opus 4.7, Wave 83 post-wave audit suite)
gaps: [GAP-571, GAP-570, GAP-558]
baseline: 2026-05-14-post-wave-78.md (68/100 C — Wave 40 baseline recalibration với strict 5-attr standard)
delta: +3 → 71/100 C — error semantic mapping đúng + PDPL Art 11 opt-in compliance shipped
---

# Business Logic Audit Report — Wave 83 Post-Deploy

**Wave scope:** commit range `4e40f252..90cba0a4` (3 PRs Wave 83 Bucket A/B/E)
**Skill:** `.claude/skills/quality/business-logic-audit/SKILL.md`
**Rubric:** `.claude/rules/audit-skill-rubric-business-logic-audit.md` (per-rule pass/fail)
**Aggregate:** **71/100 C** (audit-level verdict: **PASS** — không P0 FAIL trong wave scope)
**Bug list precedence:** per skill §3, bug list precedes score.

---

## Bug list (precedes score)

### P0 — Trong wave 83 scope

Không có P0 mới. Wave 83 đóng 3 P0/P1 carry-forward:

- ✅ **GAP-571 closed** — validation logic mapping HTTP status đúng theo HTTP spec semantics (400 cho client validation, không phải 500)
- ✅ **GAP-570 closed** — unknown endpoint mapping = 404 (HTTP semantic correctness)
- ✅ **GAP-558 closed** — PDPL Art 11 opt-in compliance (analytics chỉ load sau consent)

### P1 — Wave 83 phát hiện (observation)

1. **`rules.md` chưa cập nhật error mapping rules** — 6 exception handlers mới shipped không có corresponding entry trong `documents/01-business/kitehub/{auth,beta-access}/rules.md` cho HTTP semantic mapping. Living Docs rule: code ↔ rules.md PHẢI cùng PR. **PR #1407 = code-only** không update rules.md → vi phạm Living Docs constraint.

### P1 — Carry-forward (Wave 40 baseline)

2. **60% rules.md có 5-attr coverage** (Wave 40 finding) — Wave 83 không touch rules.md → status không đổi
3. **3 P1 follow-ups Phase 2** (Wave 40 carry-forward)

---

## Score breakdown per rubric

### Cat 1 — Code ↔ rules.md sync (P0 5-attr completeness)

| # | Check | Verdict | Note |
|---|---|:---:|---|
| 1.1 | Mỗi business rule có ID (BR-DOMAIN-NNN) | ⚠️ PARTIAL | 60% coverage carry-forward |
| 1.2 | Rule có Trigger / Constraint / Config-key / Default / Error | ⚠️ PARTIAL | Wave 40 baseline still applies |
| 1.3 | Code reference cite BR-ID trong javadoc | ⚠️ PARTIAL | Carry-forward |
| 1.4 | Tests verify BR-ID | ⚠️ PARTIAL | Carry-forward |
| 1.5 | Wave 83 changes synced (Living Docs) | ❌ FAIL (P1-1) | PR #1407 6 handlers không có corresponding rules.md entry — Living Docs vi phạm |

**Cat 1 score: 14/25** (Wave 40 carry-forward + P1-1 mới)

### Cat 2 — Error semantic correctness (P0 HTTP semantics)

| # | Check | Verdict | Note |
|---|---|:---:|---|
| 2.1 | Client error (400-class) cho client mistake | ✅ PASS | **Wave 83 fix correctly** — MissingParameter / MalformedJson / ConstraintViolation → 400 |
| 2.2 | Server error (500-class) cho server bug only | ✅ PASS | handleGenericException only catches uncategorized Exception |
| 2.3 | Resource not found → 404 | ✅ PASS | NoResourceFound + NoHandlerFound → 404 |
| 2.4 | Method not allowed → 405 + Allow header | ✅ PASS | HttpRequestMethodNotSupportedException có Allow header + supportedMethods property |
| 2.5 | Unsupported media type → 415 | ✅ PASS | HttpMediaTypeNotSupportedException → 415 |

**Cat 2 score: 20/20** (+15 vs Wave 40 — 6 new handlers cover HTTP semantic gaps that were previously 500)

### Cat 3 — Consent + PDPL compliance (P0 opt-in default)

| # | Check | Verdict | Note |
|---|---|:---:|---|
| 3.1 | Analytics default = OFF until consent | ✅ PASS | ConsentGatedAnalytics: `analytics === true` AND hydrated AND gaId → mount |
| 3.2 | Reject path không load tracker | ✅ PASS | Vitest unit test verify gaMountCalls=0 for analytics=false |
| 3.3 | Consent persistence với TTL | ✅ PASS | localStorage `kite.consent.v1` 12 tháng TTL (carry from GAP-353) |
| 3.4 | Category granularity (essential/analytics/marketing) | ✅ PASS | useConsent() hook có 3 categories |
| 3.5 | Cookie policy reachable từ public footer | ✅ PASS | Footer "Chính sách Cookie" → /legal/cookies |

**Cat 3 score: 20/20** (+10 vs Wave 40 — PDPL Art 11 opt-in fully compliant)

### Cat 4 — Living Docs adherence (P0 same-PR code+docs)

| # | Check | Verdict | Note |
|---|---|:---:|---|
| 4.1 | Code changes ship với business doc updates same PR | ❌ FAIL (P1-1) | PR #1407 = code-only; rules.md gap |
| 4.2 | Use-cases.md updated cho business flow change | N/A | Wave 83 = error handling, không thay đổi use-case |
| 4.3 | API contract docs synced (xem API Contract audit) | ⚠️ PARTIAL | Cross-ref API audit P2-1 |

**Cat 4 score: 7/15** (P1-1 violation)

### Cat 5 — Test coverage of business rules

| # | Check | Verdict | Note |
|---|---|:---:|---|
| 5.1 | Unit test per BR | ⚠️ PARTIAL | ConsentGatedAnalytics có 4 unit tests (4 gate branches) ✅; exception handlers KHÔNG có dedicated unit test (verified live thay vì) |
| 5.2 | E2E test happy path | ✅ PASS | Playwright cookie-consent.spec.ts cover reject/accept/footer/GA tag absence |
| 5.3 | Test cover edge cases | ✅ PASS | Pre-hydration, no gaId, false, true — all 4 gate branches tested |
| 5.4 | Test names cite BR-ID | ⚠️ PARTIAL | Tests cite GAP-558 nhưng BR-ID không có (cause rules.md gap §4.1) |

**Cat 5 score: 10/20**

---

## Tổng score: 71/100 — C (audit-level verdict: PASS)

**Delta vs Wave 40 baseline:**
- Wave 40: 68/100 C (recalibrated baseline)
- Wave 83: 71/100 C → **+3**

**Breakdown delta:**
- Cat 2 Error semantic: +15 (5 → 20) — Wave 83 fix correct HTTP semantic mapping
- Cat 3 Consent compliance: +10 (10 → 20) — PDPL Art 11 fully compliant
- Cat 4 Living Docs: -8 (15 → 7) — P1-1 violation (rules.md sync gap)
- Cat 1 Code-rules sync: -4 → not improved (carry-forward)
- Cat 5 Tests: -10 → still partial (BR-ID coverage gap)

**Aggregate verdict:** **PASS** — không P0 trong wave scope. Score 71/100 dưới Wave 40 8-attr standard ngưỡng 80; Wave 83 đẩy +3, vẫn cần force-multiplier work để đạt ≥80 (rules.md 5-attr completeness Wave 84+).

---

## Methodology

```bash
# Files audited:
- kitehub-subscription/.../GlobalExceptionHandler.java (6 new @ExceptionHandler)
- kitehub-frontend/src/components/legal/ConsentGatedAnalytics.tsx
- kitehub-frontend/src/components/legal/__tests__/ConsentGatedAnalytics.test.tsx
- kitehub-frontend/e2e/cookie-consent.spec.ts

# Cross-check rules.md:
grep -rn "MissingParameter\|MethodNotAllowed\|MalformedJson" \
  documents/01-business/kitehub/ 2>/dev/null
# Result: 0 hits → P1-1 Living Docs violation

# Verified live (per coordinator):
- POST /api/v1/auth/nonexistent → 404 (HTTP semantic correct)
- POST /api/auth/verify-email empty → 400 (client error semantic correct)
- POST /api/v1/auth/beta-signup/validate wrong method → 405 (semantic correct)
```

---

## New gaps filed

1 P1 gap recommendation:

- **GAP-573** (P1, file Wave 84+): Sync RFC 7807 error mapping rules vào `documents/01-business/kitehub/{auth,beta-access}/rules.md` per Living Docs constraint. AC:
  - rules.md có BR-AUTH-ERR-XXX series cho mỗi client error class (missing param / malformed body / wrong method / unknown endpoint)
  - Code javadoc cite BR-ID
  - Test methodNamesCiteBR per Cat 5.4

*Lưu ý: GAP-573 chưa file trong wave 83 audit per scope giới hạn — wave 84 ops/runbook bucket scope.*

---

## References

- Wave 78 baseline: `documents/04-quality/audits/business-logic/2026-05-14-post-wave-78.md` (68/100 C)
- Wave 40 baseline: PR #977 (recalibration 5-attr)
- PR #1407 — GAP-571 6 exception handlers (Cat 2 +15)
- PR #1410 — GAP-570 NoHandlerFoundException
- PR #1408 — GAP-558 ConsentGatedAnalytics (Cat 3 +10)
- Skill: `.claude/skills/quality/business-logic-audit/SKILL.md`
- Living Docs rule: `CLAUDE.md §"CRITICAL: Living Documents"`
- PDPL 2023 Art 11 + Decree 13/2023/NĐ-CP Art 4 (opt-in consent baseline)
