---
title: Business Logic Audit — full audit post wave-p0-closeout-1
status: complete
created: 2026-06-14
phase: phase-1-beta
wave: p0-closeout-1
audit_skill: business-logic-audit
audit_version: v2 (per-check rubric audit-skill-rubric-business-logic-audit.md v1.0.1)
baseline_audit: documents/04-quality/audits/business-logic/2026-06-12-business-logic-audit.md
baseline_score: 73/100 C+ PARTIAL FAIL Cat 1
base_sha: cd44e035f
---

# Business Logic /100 — Full Audit post wave-p0-closeout-1 (2026-06-14)

**Base SHA:** `cd44e035f` (worktree `chore/audit-biz-2026-06-14`, off main).
**Skill:** `.claude/skills/quality/business-logic-audit/SKILL.md`
**Rubric:** `.claude/rules/audit-skill-rubric-business-logic-audit.md` v1.0.1 (5 cat × ≥5 sub-check; 1 P0/P1 FAIL → cat cap ≤16 + audit-level verdict FAIL).
**Gate:** Phase 1 BETA ≥80.
**Focus surface** (per wave p0-closeout-1): subscription lifecycle (trial/migration/dunning/downgrade), LMS enrollment paywall, KC tenant-admin authz, attendance status domain. Systemic carry-forward (GAP-664/666/156) re-confirmed nhưng KHÔNG file mới (per `audit-to-gap-pipeline.md` §2 dup-avoidance).

## Aggregate verdict

**70/100 C — audit-level verdict FAIL.** Delta vs baseline (2026-06-12, 73/100): **−3**.

Lý do giảm 3 điểm KHÔNG phải regression code — mà do audit này **đào sâu surface attendance** mà cadence audit trước (focus branding) chưa chạm, surface 1 cluster drift mới (BR-ATT-002/003/004 + UC-ATT-03 QR check-in documented-but-unimplemented + 5 config key cited-but-absent). Đây là bug-finding-primacy hoạt động đúng (per rubric §4: surface drift > giữ điểm). 3 focus surface còn lại (subscription / LMS paywall / KC authz) **chất lượng cao** — Wave kitehub-biz-100 + RBAC-LMS + auth-1 đã implement đầy đủ với code refs + tests.

## Bug list (primacy: bug-finding > scoring per rubric §4)

### P0 — none

Không có production runtime business-rule violation trên 4 focus surface. Subscription lifecycle gate (SUB-20 manual-VietQR-payment-gate, SUB-21 instance.tier sync, T2P §3.1 atomicity), LMS paywall (`LessonAccessGuard`), và KC authz (GAP-1299/1300/1301 X-Teacher-Id spoof close) đều đúng contract.

### P1 — attendance QR check-in + time-based auto-status UNIMPLEMENTED (NEW → GAP-1320)

**Finding 1 — BR-ATT-002/003/004 + UC-ATT-03 documented-but-unimplemented + 5 config key cited-but-absent.**

- `attendance/rules.md` §1 BR-ATT-002 ("Status by check-in time: PRESENT within grace / LATE within threshold / ABSENT beyond"), BR-ATT-003 (grace 5 phút), BR-ATT-004 (late threshold 15 phút) + §4 Config liệt kê 5 key: `attendance.grace-period-minutes`(5) / `attendance.late-threshold-minutes`(15) / `attendance.qr-code.expiry` / `attendance.low-warning-threshold`(70%) / `attendance.grade-weight`(10%).
- `attendance/use-cases.md` UC-ATT-03 "QR Code Check-in" mô tả flow đầy đủ (scan → record timestamp → auto-status theo thời gian → 400 "QR code expired") — KHÔNG đánh dấu "(Planned)" (khác section §3 Emails đã ghi rõ "Planned").
- **State-check:**
  - `grep -rnE "grace-period-minutes|late-threshold-minutes|qr-code.expiry|low-warning-threshold|grade-weight" --include="*.yml" kiteclass/` → **0 hits** (config key KHÔNG tồn tại trong bất kỳ application*.yml).
  - `grep -rniE "qr.?code|checkin|grace.?period|determineStatus" --include="*.java" .../module/attendance` → **0 hits** (chỉ payment-module QR + retention-module grace, không phải attendance).
  - `grep -rnE "BR-ATT-00[234]" --include="*.java" kiteclass/` → **0 hits** (so với BR-ATT-005/008 đều có ref).
- **Verdict:** code chỉ hỗ trợ **manual status assignment** (teacher chọn trực tiếp PRESENT/LATE/ABSENT/EXCUSED/MAKEUP per UC-ATT-01/02). Auto-determination theo check-in time + QR flow = **chưa build**. Đây là code↔rules drift: rules.md (Layer-1 source-of-truth) + use-cases.md mô tả behavior production KHÔNG có.
- **Rule violated:** rubric §2.2 Cat 2.1 (config key cited phải tồn tại trong yml) + Cat 1.1 (BR-xxx phải có grep hit) + Cat 3.1 (UC error path "QR expired 400" không có test).
- **Fix:** EITHER implement QR check-in + time-based status + 5 config key, OR đánh dấu `(Planned Phase 1.5)` trên BR-ATT-002/003/004 + UC-ATT-03 + §4 config keys (giống pattern §3 Emails). → GAP-1320.

### P2 — attendance/rules.md Layer-1 omits MAKEUP status (NEW → GAP-1321)

**Finding 2 — MAKEUP status có trong code + Layer-2/3 nhưng VẮNG ở Layer-1 (rules.md).**

- Code `AttendanceStatus.java:22-26` có **5 giá trị**: PRESENT / ABSENT / LATE / EXCUSED / **MAKEUP** ("Học bù", −0 điểm deduction `:58`). Dùng tại `AttendanceServiceImpl.java:387,437` + `AttendancePeriod.java:47`.
- `attendance/api-contract.md:104,123,516,536` + `attendance/use-cases.md:204` + toàn bộ `period-attendance/` docs → **CÓ** document MAKEUP.
- `attendance/rules.md:23` "Attendance statuses: PRESENT, LATE, ABSENT, EXCUSED" (**4 giá trị, thiếu MAKEUP**) + Permission Matrix §1 cũng không nhắc MAKEUP.
- **Verdict:** intra-domain 3-layer inconsistency — Layer-1 (rules.md, source-of-truth per CLAUDE.md) stale vs Layer-2 (use-cases) + Layer-3 (api-contract) + code. Rubric §2.4 Cat 4.1 (no cross-layer contradiction).
- **Fix:** thêm MAKEUP vào `attendance/rules.md` §1 status list + permission matrix + point-deduction note. → GAP-1321.

### P2 META — kiteclass/multi-tenancy domain missing 2 layers (NEW → GAP-1322)

**Finding 3 — `documents/01-business/kiteclass/multi-tenancy/` chỉ có rules.md (thiếu use-cases.md + api-contract.md).**

- `ls documents/01-business/kiteclass/multi-tenancy/` → CHỈ `rules.md`.
- Multi-tenancy là cross-cutting domain load-bearing (RLS, tenant isolation BR-STU-006 / BR-ATT-009 / BR-AUTH-007) — thiếu Layer-2/3 làm verification chain UC-xxx → endpoint không trace được.
- **Verdict:** mở rộng class GAP-664 (3-layer drift) sang KiteClass domain (GAP-664 scope = kitehub preferences/email). Rubric §2.1 Cat 1.5 + CLAUDE.md §"3-Layer Structure". → GAP-1322 (đường nhánh riêng, cross-ref GAP-664).

### P3 advisory — rules.md text stale vs DONE gaps (KHÔNG file mới)

- `tenant-auth/rules.md` BR-AUTH-008 ghi "Phase 1: chưa có path tự động set enabled=false khi entity deactivate/soft-delete — theo dõi follow-up P2", nhưng **GAP-1013 (DONE)** = "Auth credential hardening cluster (… disable-on-deactivate …)". Rule text stale vs fix đã ship.
- BR-AUTH-004 ghi "follow-up P1 multi-tenant email collision … defer", nhưng **GAP-1011 (DONE)** = "auth_credentials global email-unique collides with multi-tenant".
- **Verdict:** doc-staleness nhẹ (rules.md note chưa update sau khi 2 gap đóng). Fold vào GAP-666-class README/doc-sync hygiene — KHÔNG file gap riêng (tránh over-filing per `audit-to-gap-pipeline.md` §2). Recommend: next doc-sync pass update 2 BR-AUTH note.

### Systemic carry-forward (re-confirmed, NO new gap)

- **GAP-664** (PARTIAL 40%) — 3-layer backfill kitehub preferences (rules.md+use-cases.md) + email (use-cases.md). Unchanged.
- **GAP-666** (OPEN) — BR-ID javadoc code refs = 0 project-wide; README business index stale. Re-confirmed broader than original scope (BR-ATT-002/003/004 + BR-SUB/BR-LMS partial: chỉ BR-LMS + BR-ATT-005/008 + BR-ENROLL có javadoc refs; SUB-* dùng GAP-id refs thay BR-id). README chưa sync.
- **GAP-156** (PENDING) — 5-attribute coverage. SUB-22 entitlement matrix exemplary (full 5-attr); phần lớn BR khác = file-level placeholder.

## Per-surface verification table

| Focus surface | BR/rule | Code evidence | Verdict |
|---|---|---|---|
| **Subscription lifecycle** | SUB-23 dunning + pending-payment-ttl | `SubscriptionExpirationChecker.processStalePendingPayments` + `SubscriptionConfig.pendingPaymentTtlDays=7` + yml + tests `:270,291` | ✅ PASS |
| | SUB-24 involuntary churn | `SubscriptionRenewalService.suspendExpiredSubscription` + `ChurnType{VOLUNTARY,INVOLUNTARY}` (ReactivateResponse) + test GAP-1260 `:319` | ✅ PASS (churn_type column documented-deferred Phase 1.5, V74 reserved) |
| | SUB-25 suspended_at retention clock | `DataRetentionService.retentionClockStart` prefers `Instance.getSuspendedAt()` `:70,127,216` | ✅ PASS |
| | SUB-26 downgrade over-cap preview | `OwnerBillingService.getDowngradePreview` (GAP-1261) | ✅ PASS |
| | SUB-20 create-paid VietQR gate | `SubscriptionService.createSubscription` PENDING + `applyPendingUpgrade` | ✅ PASS |
| | SUB-21 instance.tier sync | `InstanceTierSyncService` single sync point (GAP-1256) | ✅ PASS |
| | T2P §3.1 migration atomicity | pessimistic lock (GAP-1253) + REQUIRES_NEW dead-letter (GAP-1254) + reversal anchor `migrationCompletedAt` (GAP-1272) + idempotency (GAP-1271) | ✅ PASS |
| **LMS enrollment paywall** | BR-LMS-001/002/019 | `LessonAccessGuard` single-source (GAP-1115 read + GAP-1116 write); ACTIVE enrollment in any course class | ✅ PASS |
| | BR-ENROLL-001/002/003 | capacity atomic INSERT (`ClassRepository`) + unique constraint + `finalAmount` @PrePersist (`Enrollment.java:121`) | ✅ PASS |
| | LMS paywall storage-path carve-out | `StorageController` download-url chỉ enforce visibility (PUBLIC/PRIVATE/TENANT), KHÔNG enrollment → paid-lesson material TENANT-scope tải được bởi non-enrolled same-tenant student | ⚠️ KNOWN — GAP-1307 OPEN P1 (đã filed wave-p0-closeout-1 #2403; KHÔNG file mới) |
| **KC tenant-admin authz** | BR-AUTH-001..008 (tenant-auth) | `AuthCredential` + `AuthService` uniform-401 + `AuthTokenService` HS512 + V89 — full code citations | ✅ PASS |
| | X-Teacher-Id spoof close | GAP-1299 (LMS authoring) + GAP-1300 (attendance) + GAP-1301 (grade): acting teacher từ principal `X-User-Reference-Id`, KHÔNG client header | ✅ PASS |
| | OWNER tenant-admin | `AuthorizationBean.isAdminOrOwner` (`ROLE_OWNER` treated as tenant-admin) `:381-404` | ✅ PASS |
| **Attendance status domain** | BR-ATT-005 EXCUSED requires note | `AttendanceServiceImpl:100` (GAP-993) | ✅ PASS |
| | BR-ATT-008 rate calc | `(present+late)/total` `:391,441` (GAP-994) | ✅ PASS |
| | BR-ATT-002/003/004 + UC-ATT-03 QR | **0 code, 0 yml** | ❌ FAIL → GAP-1320 |
| | MAKEUP status doc (Layer-1) | code 5-value, rules.md 4-value | ❌ FAIL → GAP-1321 |

## Category scores (per rubric §2)

| # | Category (20) | Sub-check verdict | Score | Notes |
|---|---|---|:---:|---|
| 1 | **Rule Coverage** | BR-ATT-002/003/004 unimpl (P1) + systemic BR-ID javadoc 0-refs (P2, GAP-666). Focus-surface BR khác có refs (BR-LMS / BR-ENROLL / BR-AUTH / SUB via GAP-id). | **13/20** | Unchanged vs baseline; attendance unimpl offset bởi authz/LMS traceability tốt. |
| 2 | **Config Accuracy** | subscription (`pending-payment-ttl-days:7` + `grace-period-days:3` + `warning-days`) + auth (`jwt.secret` + `access-token-ttl:PT12H`) wired+matched. **Attendance 5 config key cited-but-absent (2.1 FAIL).** | **16/20** | −4 NEW finding (attendance config drift); cap-16 per "1 sub-check FAIL". |
| 3 | **Edge Case Tests** | subscription tests mạnh (`SubscriptionExpirationCheckerTest` + `SubscriptionRenewalServiceTest` GAP-1260 + `SubscriptionPendingNullableColumnsIT`); LMS guard; attendance UC-ATT-03 "QR expired 400" untested (unimpl). | **15/20** | +1 vs baseline (subscription edge tests). |
| 4 | **Cross-Domain Consistency** | MAKEUP Layer-1 drift (P2) + multi-tenancy missing layers (P2) + 2 stale BR-AUTH notes (P3) + README index stale (carry). Cross-rule citations otherwise clean. | **16/20** | −1 (MAKEUP intra-domain layer drift NEW). |
| 5 | **Stakeholder Alignment** | 5-attr 0% trên đa số BR (GAP-156 carry); SUB-22 entitlement matrix exemplary (full 5-attr + Reviewer hat). | **10/20** | Unchanged. |

## Overall

```
Total = 13 + 16 + 15 + 16 + 10 = 70/100  (Grade C)
Audit-level verdict = FAIL (Cat 1 BR-ATT unimpl P1 + Cat 2 attendance config drift; dưới gate ≥80)
```

## Delta vs baseline (2026-06-12)

| Metric | Baseline (2026-06-12) | This audit (2026-06-14) | Delta |
|---|:---:|:---:|:---:|
| Overall | 73/100 C+ | **70/100 C** | **−3** |
| Cat 1 | 12 | 13 | +1 |
| Cat 2 | 20 | 16 | −4 (NEW attendance config drift) |
| Cat 3 | 14 | 15 | +1 |
| Cat 4 | 17 | 16 | −1 (NEW MAKEUP Layer-1 drift) |
| Cat 5 | 10 | 10 | 0 |

**Vì sao −3:** Audit này deep-dive attendance surface (baseline chỉ chạm branding) → surface 2 NEW findings (config drift + MAKEUP). Bug-finding-primacy đúng. 3 focus surface còn lại (subscription/LMS/authz) chất lượng production-grade.

## Path to 80 (gate)

1. **GAP-1320** (attendance QR/config): EITHER implement OR mark `(Planned Phase 1.5)` 5 config key + BR-ATT-002/003/004 + UC-ATT-03 → Cat 2 phục hồi +4 → ~74.
2. **GAP-664** (3-layer backfill kitehub) + **GAP-1322** (multi-tenancy layers) → Cat 1 +~3 → ~77.
3. **GAP-666** (BR-ID javadoc + README sync) + **GAP-1321** (MAKEUP Layer-1) → Cat 1 + Cat 4 +~3 → ~80.

Ước tính sau cluster: **~80/100 PASS**.

## Gaps filed (3 NEW — reserved block, 1 gap = 1 finding)

- **GAP-1320** (P1, Backend/Docs) — Attendance QR check-in + time-based auto-status + 5 config key documented-but-unimplemented (BR-ATT-002/003/004 + UC-ATT-03 drift).
- **GAP-1321** (P2, Docs) — attendance/rules.md Layer-1 omits MAKEUP status (code + Layer-2/3 + period-attendance đều có).
- **GAP-1322** (P2, Meta) — kiteclass/multi-tenancy domain missing use-cases.md + api-contract.md (3-layer incomplete; extends GAP-664 class).

Carry-forward (re-confirm, NO new gap): GAP-664, GAP-666, GAP-156. Dup-avoided (per `audit-to-gap-pipeline.md` §2): GAP-1013 (disable-on-deactivate DONE), GAP-1011 (multi-tenant email DONE) → cover BR-AUTH-008/004 follow-ups; chỉ note rules.md text stale (P3 fold GAP-666).
