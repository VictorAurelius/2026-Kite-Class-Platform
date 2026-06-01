---
title: Path to Thesis Goal — lộ trình đạt mục tiêu khoá luận + Phase 1.5 full interpretation
status: active
created: 2026-06-01
updated: 2026-06-01
audience: mixed
phase: phase-1-beta + phase-1.5-paid
gaps: [GAP-648, GAP-649, GAP-622, GAP-117, GAP-127, GAP-370, GAP-533, GAP-543, GAP-535, GAP-536, GAP-538, GAP-543, GAP-599, GAP-608, GAP-610, GAP-819, GAP-820]
related-rules: [thesis-as-future-state-mandate, post-wave-audit-mandate, gap-architecture-v2, wave-tag-numbering-convention]
---

# Path to Thesis Goal — Lộ trình tới Thesis Goal

**Trigger:** Wave meta-7 P0+P1 stale-status audit (2026-06-01) — surface 38% drift class CSV ↔ code reality + identify thesis-blocking gaps cụ thể. Doc này codify lộ trình từ hiện trạng tới (A) thesis academic submission + (B) Phase 1.5 full thesis interpretation.

**Theo `thesis-as-future-state-mandate.md` v1.0.0:** thesis = goal state; KHÔNG sửa thesis wording. Phase 1 BETA ship minimum interpretation (passive CTA = "kết nối"); Phase 1.5+ ship full interpretation (active push).

---

## 1. Thesis goal — 2 mức interpretation

### A. THESIS ACADEMIC MINIMUM (ship khoá luận)

Đủ điều kiện ship thesis academic submission khi:
- Thesis Ch1+Ch2 claims có **minimum interpretation** đã thoả (vd passive Zalo CTA = "đã kết nối Zalo OA")
- Thesis Ch4 evidence narrative anchor:
  - ≥4 signed user review từ beta cohort
  - NFR data captured (load test + cost + dashboards)
- Phase 1 BETA gate ≥80 PASS

### B. THESIS FULL INTERPRETATION (post-thesis academic, Phase 1.5 paid tier)

Phase 1.5 ship khi:
- Mọi thesis "đã" claim được delivered FULL (vd active ZNS push notification thay cho passive CTA)
- Phase 1.5 paid tier revenue-bearing features production-ready
- Mọi gap trong cluster Phase 1.5 closed hoặc honest PARTIAL với defer plan

---

## 2. Critical path — Track A Thesis Academic

```
HÔM NAY (2026-06-01)
   │
   ▼
GAP-612 AWS restore ✅ DONE (2026-05-26)
   │
   ▼
Wave meta-7 audit (đang flight) ──┐
   │                              │
   ▼                              │
Wave thesis-2 (GAP-648 NFR)       │
   ~5-7 ngày effective            │
   │                              │
   ▼                              │
Wave beta-cohort-1 EXECUTE       │
   GAP-649 invite + use + sign    │
   ~9 TUẦN wall-clock async       │
   │                              │
   ▼                              │
Wave thesis-3 (Ch4 + GAP-622)    │
   ~3-5 ngày effective            │
   │                              │
   ▼                              │
THESIS DEFENSE READY ◄────────────┘
   (Track B Phase 1 BETA gate parallel)
```

**Tổng wall-clock từ 2026-06-01:** ~12 tuần (chặn 9-tuần cohort async — dev effective work ~3-4 tuần).

---

## 3. Track A — 3 waves thesis academic

### Wave thesis-2 — Thesis NFR Data Capture

| Property | Value |
|---|---|
| Trigger | GAP-648 P0 OPEN — load test + CloudWatch + AWS cost |
| Scope | k6/JMeter load test cho 5 critical paths + CloudWatch dashboard screenshots + AWS cost report Wave 80+ |
| Effort | ~5-7 ngày (1 inline coordinator + 1 background agent k6 scripting) |
| Acceptance | Thesis Ch4 §NFR có 3 figure thực tế (latency p95 / throughput / cost-per-tenant) |
| Blockers | GAP-612 ✅ (AWS up); load test cần stack stable ~24h |
| Output | `documents/04-quality/audits/performance/2026-06-NN-thesis-load-test.md` + Ch4 update |

### Wave beta-cohort-1 — Beta Cohort Execution

| Property | Value |
|---|---|
| Trigger | GAP-649 P0 OPEN — ≥4 signed user reviews |
| Scope | Execute 4 GV persona (2 trial + 2 VIP) per `release-1-beta-cohort-plan.md` (Wave thesis-1 GAP-623 DONE) — invite + onboard + 2-4 tuần use + collect signed feedback |
| Effort | ~9 tuần wall-clock async (dev coordinator ~2-3h/tuần check-in) |
| Acceptance | 4/4 signed review template (tên + chữ ký + screenshot + use case) + 2 quote suitable cho thesis |
| Blockers | Wave thesis-2 NFR optional precede; GAP-622 PDPL parallel |
| Output | `documents/08-thesis/defense/beta-cohort-signed-reviews/` 4 files |

### Wave thesis-3 — Ch4 Update + GAP-622 Closure

| Property | Value |
|---|---|
| Trigger | GAP-622 P0 OPEN — PDPL + AWS + BETA gate consolidation |
| Scope | Thesis Ch4 update với 4 signed evidence + NFR figures + PDPL Art 11/15/16 mapping + final defense deck refresh |
| Effort | ~3-5 ngày (inline) |
| Acceptance | Thesis V2 docx + defense deck refresh + GAP-622 closure |
| Blockers | Wave thesis-2 + Wave beta-cohort-1 EVIDENCE ready |
| Output | `documents/08-thesis/thesis-v2.docx` + GVHD review |

---

## 4. Track B — Phase 1 BETA gate ≥80 (parallel với Track A)

### Wave email-finalize (~5 ngày effective)

Close email cluster gần xong (PARTIAL ≥80%):

| Gap | Current | Target | Note |
|---|---|---|---|
| GAP-370 Email infra | 95% | 100% | Email transactional Resend warm-up production |
| GAP-533 Resend deliverability | 80% | 100% | DKIM/DMARC/SPF + spam-score baseline |
| GAP-543 Email content audit VN | 95% | 100% | 5 critical email types content/tone audit |
| GAP-608 EC2 IAM ses:SendEmail | 90% | 100% | IAM policy attach + production verify |
| GAP-572 Resend secret schema | 40% | 100% | JSON schema mismatch + key leak rotate |
| GAP-530 Email flow E2E live verify | 10% | 100% | Live verify per §2.3 |

### Wave onboarding-polish (~5 ngày effective)

Close multi-tenant onboarding cluster:

| Gap | Current | Target | Note |
|---|---|---|---|
| GAP-538 Day-1 onboarding | 96% | 100% | Sample/demo data seed |
| GAP-535 Slug normalize VN | 70% | 100% | Cross-flow sweep + IT |
| GAP-536 POST /tenants idempotency | 65% | 100% | Idempotency key + rollback |
| GAP-599 JWT 2-tab storage | 90% | 100% | sessionStorage migration verify |
| GAP-610 Beta-signup validate | 75% | 100% | Live verify (GAP-612 unblocked) |
| GAP-534 Invite token single-use | 90% | DONE | Wave meta-7 Bucket A flip |

### Wave ops-mature (~7 ngày effective)

PDPL + operational maturity signal:

| Gap | Current | Target | Note |
|---|---|---|---|
| GAP-117 Restore drill | 50% | 100% | Automated drill script + measured RTO |
| GAP-257 Quarterly DR | 0% | 100% | Quarterly DR exercise + runbook |
| GAP-127 FE code-splitting | 50% | 100% | Next.js dynamic import 64 pages |

### Wave meta-8 — Follow-up Wave meta-7 (~3-5 ngày effective)

Surface từ Wave meta-7 audit findings:

| Gap | Type | Scope |
|---|---|---|
| GAP-NEW1 META | New | Audit-cadence systemic enforcement (`check-post-wave-audit-cadence.sh` detector) |
| GAP-NEW2 META | New | CSV ↔ AC auto-sync mechanism (`sync-gap-csv-from-ac.sh` script) |
| GAP-461 | SCOPE-REVISE | Brand-clearance-pre-domain rule (Bucket C surfaced) |
| GAP-615 | SCOPE-REVISE | Wave 86 retro 4-extensions partial |
| GAP-723 | SCOPE-REVISE | Pre-mutation §1.6 Java extension |
| 14 SCOPE-REVISE | Existing | Rewrite gap descriptions (Bucket D 10 + Bucket B 1 + Bucket C 3) |

---

## 5. Track C — Phase 1.5 Full Interpretation (post-thesis)

8 waves ship Phase 1.5 paid tier features → make thesis "đã" claims TRUE.

### Wave zalo-1.5 (~10 ngày)

| Gap | Scope | Thesis source |
|---|---|---|
| GAP-819 P1 | Zalo OA active push ZNS adapter | Ch1 §1.1.2 + Ch1 §1.4 + Ch2 |
| GAP-820 P1 | Zalo Group link per-class | Ch1 §1.2.5 |

### Wave qr-payment-1 (~10 ngày)

| Gap | Scope |
|---|---|
| GAP-625 P0 | QR payment foundation — Owner KYC + multi-tenant QR binding |
| GAP-626 P0 | QR payment PDPL Art 11 — PII handling |
| GAP-627 P0 | Payment amount mismatch detection |

### Wave qr-payment-2 (~10 ngày)

| Gap | Scope |
|---|---|
| GAP-628 P1 | QR batch reconcile API |
| GAP-629 P1 | QR refund workflow SOP |
| GAP-630 P1 | QR evidence receipt storage |
| GAP-631 P1 | QR account verification quarterly |
| GAP-632 P1 | Manual mark-paid audit trail |

### Wave legal-policy (~5 ngày)

| Gap | Scope |
|---|---|
| GAP-180 P0 | Terms of Service finalize |
| GAP-181 P0 | Acceptable Use Policy |
| GAP-183 P0 | Refund + Dispute Resolution |

### Wave admin-hardening (~5 ngày)

| Gap | Scope |
|---|---|
| GAP-577 P0 | Platform admin MFA + IP allowlist + 30min session |
| GAP-578 P0 | P2 Owner 2FA + new-device alert |

### Wave trial-paid-migration (~5 ngày)

| Gap | Scope |
|---|---|
| GAP-192 P0 | Trial → Paid zero-downtime migration |

### Wave payment-vendor-eval (~5 ngày)

| Gap | Scope |
|---|---|
| GAP-636 P1 | Casso/SePay webhook integration investigation |

### Wave phase-1.5-closure (~3 ngày)

- Final audit Phase 1.5 gate ≥85
- Phase 1.5 launch checklist
- Closure runbook

---

## 6. Tổng kết wave estimate

| Goal level | Số wave | Wall-clock | Dev effective |
|---|---|---|---|
| **Thesis Academic Minimum** | 3 waves | ~12 tuần (chặn cohort 9 tuần) | ~3-4 tuần |
| **Phase 1 BETA gate ≥80** (parallel Track A) | 4 waves | ~3-4 tuần (chồng lắp Track A) | ~3-4 tuần |
| **Phase 1.5 Full thesis interpretation** | 8 waves | ~7-12 tuần post-thesis | ~7-10 tuần |
| **TỔNG** | **15 waves** | **~24 tuần wall-clock** | **~14-18 tuần effective** |

---

## 7. Critical path summary

```
2026-06-01: HÔM NAY
   │
   ▼
~Tuần 1: Wave meta-7 closure (audit findings → CSV updates)
   │
   ▼
Tuần 1-2: Wave meta-8 (META gaps + SCOPE-REVISE)
   │
   ├─► Track A: Wave thesis-2 (NFR) — Tuần 1-2
   │   └─► Wave beta-cohort-1 EXECUTE (Tuần 2-11 wall-clock)
   │       └─► Wave thesis-3 (Ch4 update) — Tuần 11-12
   │           └─► THESIS DEFENSE READY — Tuần 12
   │
   └─► Track B parallel (Tuần 1-4):
       ├─ Wave email-finalize (Tuần 1)
       ├─ Wave onboarding-polish (Tuần 2)
       └─ Wave ops-mature (Tuần 3-4)
       └─► PHASE 1 BETA GATE ≥80 — Tuần 4
   │
   ▼
Tuần 13+: Track C Phase 1.5 (post-thesis)
   8 waves × 5-10d = ~12-20 tuần
   │
   ▼
~Tuần 24-32: PHASE 1.5 FULL DELIVERY (thesis claims TRUE)
```

---

## 8. Risks + mitigation

| Risk | Mitigation |
|---|---|
| GAP-649 beta cohort 9 tuần wall-clock — chậm gây dời thesis defense | Start invite ngay sau wave-meta-7 closure; Track B parallel để dev không idle |
| Phase 1 BETA gate ≥80 còn xa (hiện ~77/100) | Track B 4 waves nhắm tăng +3-8 điểm; cluster email-onboarding-ops chính yếu |
| Phase 1.5 scope creep (Zalo + QR + Legal + MFA = 25 gaps) | Phase 1.5 split sub-phase (1.5a Zalo + Legal / 1.5b QR / 1.5c hardening) defer 1.5c nếu chậm |
| Wave meta-7 audit findings discover thêm gaps phải fix | Wave meta-8 catch-all queue + Track B incorporate sister sweep findings |
| Examiner request additional NFR/evidence trong Ch4 | Wave thesis-2 scope flexible — extend nếu cần thêm benchmark |

---

## 9. Decision points

User chốt:

1. **Start Wave thesis-2 NFR ngay sau Wave meta-7 closure?** (recommend YES — không chặn)
2. **Beta cohort invite execution start ngay tuần này hay đợi Wave thesis-2 NFR figures?** (recommend start ngay — async)
3. **Phase 1.5 scope split 1.5a/b/c hay 1 sweep duy nhất?** (recommend split để defer-able)
4. **Track B Phase 1 BETA gate priority order:** email → onboarding → ops, hay parallel?

---

## 10. Cross-references

- **Rule:** `.claude/rules/thesis-as-future-state-mandate.md` v1.0.0 — thesis = goal state mandate
- **Rule:** `.claude/rules/wave-tag-numbering-convention.md` v1.0.0 — wave naming (thesis-N / beta-N / meta-N)
- **Wave plan:** `documents/03-planning/waves/wave-2026-06-01-meta-7-p0-p1-stale-audit.md` (đang flight)
- **Existing roadmap:** `documents/03-planning/roadmap/release-1-plan-2026.md`
- **Existing roadmap:** `documents/03-planning/roadmap/phase-1-beta-readiness-runbook-2026-05-15.md`
- **Beta cohort plan:** `documents/03-planning/release-1-beta-cohort-plan.md` (GAP-623 DONE)
- **Thesis source:** `documents/08-thesis/chapter-1-*.md` + `chapter-2-system-architecture.md`
- **Gap canonical:** `documents/04-quality/gaps/gap-status.csv`

---

## 11. Log

- **2026-06-01:** Doc created. Triggered by user direction 2026-06-01 "lưu báo cáo này thành path to thesis goal được không? làm luôn chứ". Doc consolidates Wave meta-7 audit findings (172 P0+P1 gaps, ~38% drift class) + thesis blocker identification (3 P0: GAP-648/649/622) + Phase 1 BETA gate ≥80 supporting cluster + Phase 1.5 full interpretation roadmap (8 waves). Per `thesis-as-future-state-mandate.md` v1.0.0 §3.1 minimum-vs-full interpretation. Estimate 15 waves total ~24 tuần wall-clock / ~14-18 tuần effective dev work (chặn 9 tuần GAP-649 cohort async). Critical path: Wave meta-7 closure → Wave thesis-2 NFR → Wave beta-cohort-1 (9 tuần async) → Wave thesis-3 Ch4 update → THESIS DEFENSE READY. Track B Phase 1 BETA gate parallel ~4 tuần. Track C Phase 1.5 8 waves post-thesis.
