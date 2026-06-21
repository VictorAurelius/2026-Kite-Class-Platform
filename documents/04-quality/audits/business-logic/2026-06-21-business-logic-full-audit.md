---
title: Business Logic Audit — full refresh (phase1-closeout-loop)
status: complete
created: 2026-06-21
phase: phase-1-beta
wave: phase1-closeout-loop
audit_skill: business-logic-audit
audit_version: v2 (per-check rubric audit-skill-rubric-business-logic-audit.md v1.0.1)
baseline_audit: documents/04-quality/audits/business-logic/2026-06-14-business-logic-full-audit.md
baseline_score: 70/100 C FAIL
base_sha: 3d5179551
---

# Business Logic /100 — Full Audit Refresh (2026-06-21)

**Base SHA:** `3d5179551` (main HEAD, worktree `loop/audit-refresh-2-2026-06-21`).
**Skill:** `.claude/skills/quality/business-logic-audit/SKILL.md`
**Rubric:** `.claude/rules/audit-skill-rubric-business-logic-audit.md` v1.0.1 (5 cat × ≥5 sub-check; 1 P0/P1 FAIL → cat cap ≤16 + audit-level verdict FAIL).
**Gate:** Phase 1 BETA ≥80.
**Inputs chạy:** `scripts/check-3-layer-completeness.sh` (75 domain, 71 complete, 4 violation) + `scripts/check-business-rule-attributes.sh` (no-diff → manual sample) + `scripts/query-gaps.sh` (path-to-80 cluster status) + git log diff vs 2026-06-14 baseline.
**Focus:** delta-driven refresh — verify path-to-80 cluster (GAP-664/666/1320/1321/1322 + GAP-156/154 work since 06-14) + re-confirm 4 production-grade focus surfaces (subscription / LMS paywall / KC authz / attendance) chưa regress.

## Aggregate verdict

**78/100 C+ — audit-level verdict FAIL.** Delta vs baseline (2026-06-14, 70/100): **+8**.

Điểm tăng 8 là REAL — KHÔNG inflation. 5 fix/work cluster đã ship từ 06-14 đến 06-21:

1. **GAP-1320 (attendance config drift)** RESOLVED qua doc-honesty (PR #2423): 5 config key + BR-ATT-002/003/004 + UC-ATT-03 QR flow đều marked `(Planned — Phase 1.5)` giống pattern §3 Emails. Cat 2 `cited-but-absent` silent drift biến mất → +3.
2. **GAP-1321 (MAKEUP Layer-1)** DONE: `attendance/rules.md` §1 thêm MAKEUP + status/points-deduction table → Cat 4 intra-domain drift fixed.
3. **GAP-1322 (multi-tenancy 3-layer)** DONE: thêm `use-cases.md` (UC-MT-01..05) + `api-contract.md` (X-Tenant-Id contract) grounded TenantResolverGatewayFilterFactory/V58 → Cat 4 + Cat 1.5.
4. **GAP-156 AC-C (compliance-checklist.md)** DONE: `documents/00-brd/compliance-checklist.md` (155 dòng, 7 luật VN × business-rule mapping) → Cat 5.4 compliance flagging.
5. **BR-ID javadoc traceability**: project-wide hiện **489 BR-* refs** trong Java across nhiều family (BR-ACYR / BR-APRV / BR-ASSIGN / BR-LMS / BR-ATT / BR-ENROLL...) — so với baseline claim "0 project-wide" → Cat 1 traceability mạnh lên.

Vẫn FAIL vì 2 P0 carry chưa đóng: (a) 5-attribute **independent-verification ~0%** (91% structural 5/5 block nhưng quality = informed-gut — GAP-156 AC-B + AC-D counsel-blocked), (b) 3-layer completeness 4 domain còn thiếu layer (GAP-664 preferences/email + NEW marketing/consent). Score dưới gate 80 đúng 2 điểm — path-to-80 nằm gọn trong cluster 3-layer + GAP-156.

## Bug list (primacy: bug-finding > scoring per rubric §4)

### P0 — none

Không có production runtime business-rule violation. 4 focus surface giữ production-grade contract (verify code presence + git-log không regression từ 06-14):
- **Subscription lifecycle**: `OwnerBillingService` / `InstanceTierSyncService` / `SubscriptionRenewalService` present; git log không touch contract (chỉ mobile-OTP + security + config-sweep + branding-RLS).
- **LMS paywall**: `LessonMaterialAccessGuardImpl` + `LmsServiceImpl` + `LessonProgressServiceImpl` single-source guard intact.
- **KC tenant-admin authz**: `AuthorizationBean` + `UserContext` `X-User-Reference-Id` (GAP-1299/1300/1301 spoof-close) intact.
- **Attendance**: manual marking path (UC-ATT-01/02/04/09 + BR-ATT-005/008) intact; auto-status honestly marked Planned.

### P1 — none NEW

GAP-664 (preferences + email 3-layer) carry P1; không file mới.

### P2 — kitehub/marketing + kitehub/consent thiếu use-cases.md (NEW → GAP-1516)

**Finding 1 — 2 domain ngoài scope GAP-664 vi phạm 3-layer completeness.**

- `scripts/check-3-layer-completeness.sh` → 4 violation: `kitehub/preferences` (thiếu rules.md + use-cases.md), `kitehub/email` (thiếu use-cases.md) — **2 cái này thuộc GAP-664 scope (preferences + email)**; CỘNG `kitehub/marketing` (thiếu use-cases.md, có README + api-contract + rules — created 2026-05-06 PR #816) + `kitehub/consent` (thiếu use-cases.md, có README + api-contract + rules — created 2026-05-24 PR #1782).
- **State-check (per `audit-to-gap-pipeline.md` §2.5):** GAP-664 file scope ghi rõ "preferences + email domains" — KHÔNG bao gồm marketing/consent. Grep gaps cho "marketing use-cases" / "consent use-cases" → 0 gap khớp (GAP-353 = marketing rules creation, không phải use-cases gap). → genuinely NEW, không duplicate.
- **Verdict:** mở rộng cùng class GAP-664 (3-layer drift) sang 2 domain mới. Rubric §2.1 Cat 1.5 + §2.4 Cat 4.1 + CLAUDE.md §"3-Layer Structure". → GAP-1516 (đường nhánh riêng, cross-ref GAP-664; recommend gộp về cùng class khi backfill).
- **Fix:** backfill `use-cases.md` cho marketing (UC: PDPL consent banner display + marketing-kit theme) + consent (UC: consent capture/withdraw/version-stamp + immutable hash chain) — cùng wave với GAP-664 preferences/email backfill.

### P3 advisory — không file mới (fold vào carry gaps)

- **Business sub-folder README index:** `documents/01-business/kitehub/` + `kiteclass/` KHÔNG có README.md sub-index (chỉ top-level `documents/01-business/README.md`). Fold vào GAP-666 "business README index sync" — KHÔNG file riêng (tránh over-filing).
- **tenant-auth BR-AUTH-008/004 rules.md text stale** vs DONE gaps GAP-1013/1011 (carry từ 06-14 audit). Fold GAP-666 doc-sync pass.
- **GAP-666 BR-ID javadoc portion largely resolved** (489 refs) nhưng README index portion + một số SUB-* dùng GAP-id thay BR-id còn lại → GAP-666 vẫn OPEN, không re-file.

### Systemic carry-forward (re-confirmed, NO new gap)

- **GAP-664** (PARTIAL 40%) — 3-layer backfill kitehub preferences (rules.md + use-cases.md) + email (use-cases.md). Unchanged.
- **GAP-666** (OPEN) — BR-ID javadoc ĐÃ cải thiện mạnh (489 refs); README business index sync + sub-folder README còn lại.
- **GAP-156** (PARTIAL 70%) — 5-attribute: 91% structural 5/5 block / ~0% independent-verification (AC-A baseline 2026-06-21); AC-C compliance-checklist DONE; AC-B backfill + AC-D counsel-blocked.
- **GAP-1320** (PARTIAL 50%, phase-2) — attendance QR feature build deferred Phase 1.5/phase-2; doc-honesty đã ship (không còn drift FAIL).
- **GAP-1307** (OPEN P1) — LMS paywall storage-path carve-out (non-enrolled same-tenant tải paid material). Dup-avoided, không re-file.

## Per-surface verification table

| Focus surface | BR/rule | Code evidence | Verdict |
|---|---|---|---|
| **Subscription lifecycle** | SUB-20..26 (dunning/churn/suspended_at/downgrade/tier-sync) + T2P §3.1 | `OwnerBillingService` + `InstanceTierSyncService` + `SubscriptionRenewalService` present; no contract change since 06-14 | ✅ PASS (carry) |
| **LMS enrollment paywall** | BR-LMS-001/002/019 + BR-ENROLL | `LessonMaterialAccessGuardImpl` single-source (GAP-1115/1116) intact | ✅ PASS (carry) |
| **KC tenant-admin authz** | BR-AUTH-001..008 + X-Teacher-Id spoof close | `AuthorizationBean` + `UserContext` X-User-Reference-Id (GAP-1299/1300/1301) intact | ✅ PASS (carry) |
| **Attendance status domain** | BR-ATT-005/008 (manual) | `AttendanceServiceImpl` manual marking intact | ✅ PASS |
| | BR-ATT-002/003/004 + UC-ATT-03 QR + 5 config key | marked `(Planned — Phase 1.5)` rules.md §1/§2/§4 (GAP-1320 PR #2423) | ✅ doc-honest (no silent drift) |
| | MAKEUP Layer-1 | rules.md §1 status list + table (GAP-1321 DONE) | ✅ PASS |
| **mobile OTP signup (NEW)** | signup-otp config keys | `kitehub.auth.signup-otp.{code-ttl-seconds,max-verify-attempts}` in application.yml:155-158 (GAP-286) | ✅ PASS (3-layer complete) |
| **multi-tenancy** | UC-MT-01..05 + X-Tenant-Id | use-cases.md + api-contract.md added (GAP-1322 DONE) | ✅ PASS |
| **kitehub marketing + consent** | 3-layer completeness | use-cases.md MISSING (have README + api-contract + rules) | ❌ FAIL → GAP-1516 |

## Category scores (per rubric §2)

| # | Category (20) | Sub-check verdict | Score | Δ vs 06-14 |
|---|---|---|:---:|:---:|
| 1 | **Rule Coverage** | BR-ID javadoc 489 refs (1.2/1.3 mạnh) + multi-tenancy 3-layer DONE (1.5) + 5-attr 91% structural (1.6). Carry: 1.1 chưa 100% (SUB-* dùng GAP-id), 1.6 quality ~0% independent-verify (P0). | **15/20** | +2 |
| 2 | **Config Accuracy** | attendance 5 config key + BR-ATT-002/003/004 marked Planned → no silent drift (2.1 PASS); subscription (`pending-payment-ttl-days`) + signup-otp keys (`code-ttl-seconds`/`max-verify-attempts`) wired+matched. | **19/20** | +3 |
| 3 | **Edge Case Tests** | subscription edge tests mạnh (carry); mobile-OTP edge (TTL/rate-limit/max-attempt) shipped full-stack nhưng chưa deep-verify test. | **15/20** | 0 |
| 4 | **Cross-Domain Consistency** | MAKEUP Layer-1 DONE + multi-tenancy layers DONE (+2). Offset −1: marketing/consent use-cases NEW. compliance-checklist reconcile 4.4. Carry preferences/email. | **17/20** | +1 |
| 5 | **Stakeholder Alignment** | compliance-checklist.md 7 VN luật (GAP-156 AC-C DONE) → 5.4 mạnh; 5.1 reviewer field structural 91%. Carry: 5.5 independent-verification ~0% (P0 FAIL, informed-gut). | **12/20** | +2 |

## Overall

```
Total = 15 + 19 + 15 + 17 + 12 = 78/100  (Grade C+)
Audit-level verdict = FAIL
  - P0 sub-checks vẫn FAIL: Cat 1.6 5-attr independent-verification ~0% + Cat 5.5 informed-gut prevalence
  - Score 78 < gate 80 (đúng 2 điểm)
```

## Delta vs baseline (2026-06-14)

| Metric | Baseline (2026-06-14) | This audit (2026-06-21) | Delta |
|---|:---:|:---:|:---:|
| Overall | 70/100 C | **78/100 C+** | **+8** |
| Cat 1 | 13 | 15 | +2 (BR-ID javadoc 489 + multi-tenancy 3-layer + 5-attr 91% structural) |
| Cat 2 | 16 | 19 | +3 (attendance config doc-honesty GAP-1320; signup-otp keys wired) |
| Cat 3 | 15 | 15 | 0 |
| Cat 4 | 16 | 17 | +1 (MAKEUP + multi-tenancy DONE; −1 marketing/consent NEW) |
| Cat 5 | 10 | 12 | +2 (compliance-checklist.md 7 VN laws) |

## Path to 80 (gate) — +2 cần thiết

1. **GAP-664 + GAP-1516** (3-layer completeness cluster): backfill `use-cases.md` cho preferences + email + marketing + consent + rules.md cho preferences → Cat 1 +1, Cat 4 +1 → **~80 PASS**.
2. **GAP-156 AC-B** (5-attr independent-verification highest-stakes BR) + **GAP-666** (README index sync) → Cat 5 +1, Cat 1 +1 buffer → ~81-82.

Ước tính sau khi đóng cluster 3-layer: **~80/100 PASS**. Cluster gọn, không cần code change (toàn doc backfill).

## Gaps filed (1 NEW — reserved block GAP-1516..1523, dùng 1)

- **GAP-1516** (P2, Meta) — kitehub/marketing + kitehub/consent thiếu use-cases.md (3-layer drift; extends GAP-664 class, cross-ref).

Carry-forward (re-confirm, NO new gap): GAP-664, GAP-666, GAP-156, GAP-1320, GAP-1307. Dup-avoided per `audit-to-gap-pipeline.md` §2.5: GAP-1321/1322 (DONE since 06-14), GAP-1013/1011 (auth follow-ups DONE), business sub-folder README + BR-AUTH stale notes (fold GAP-666).
