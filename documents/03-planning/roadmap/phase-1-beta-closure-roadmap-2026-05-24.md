---
title: Phase 1 BETA Closure Roadmap — Wave beta-readiness-1..6 chunked plan
date: 2026-05-24
phase: phase-1-beta
wave: beta-readiness-1..6 (planning)
tag_primary: beta-readiness
tags_secondary: [phase-1-closure, security, table-stakes, trust-gate, rst]
counter: 0 (roadmap doc, not single wave)
date_launch: 2026-05-24
status: planning
audience: dev
audits: [persona-review, failure-mode-matrix, vn-edu-saas-benchmark]
gaps_referenced:
  - GAP-612 # AWS suspension (gating)
  - GAP-203 # PaymentController userId=1L (P0-CRITICAL)
  - GAP-215 # XSS admin panel
  - GAP-297 # Batch monthly invoice (table-stakes TS-1)
  - GAP-291 # Session reschedule (table-stakes TS-3)
  - GAP-288 # Guided onboarding (table-stakes TS-2)
  - GAP-063 # Parent Zalo/SMS (table-stakes TS-4)
  - GAP-139 # Parent dashboard MVP (table-stakes TS-5)
  - GAP-286 # Mobile OTP/Zalo signup
  - GAP-080 # Excel/CSV import
  - GAP-726 # KC branding wizard blank (Wave 107 RST B2)
  - GAP-543 # Email content audit (Wave 107 PARTIAL 95%)
  - GAP-657 # Email layer hardening (Wave 107 PARTIAL 95%)
  - GAP-659 # Per-tone variants (Wave 107 PARTIAL 95%)
---

# Phase 1 BETA Closure Roadmap — Wave `beta-readiness-1..6` chunked plan

**Mục tiêu:** Đóng Phase 1 BETA gate (Quality audit ≥80 + 5 beta tenants live + 0 P0 incidents 2 tuần) qua 6 sub-wave tuần tự + 1 closure wave. Realistic timeline ~3-4 tuần calendar solo dev. Mỗi sub-wave 1 plan PR riêng + exec PRs.

**Sinh ra từ:** User goal 2026-05-23 "draft wave để fix hết gaps phase 1 + RST phase 1, sau đó thực hiện hết wave" + 3 outside-in audit shipped 2026-05-24:
- `documents/04-quality/audits/persona-review/2026-05-24-outside-in-phase-1-closure-persona-walkthrough.md`
- `documents/04-quality/audits/persona-review/2026-05-24-outside-in-phase-1-closure-vn-edu-saas-benchmark.md`
- `documents/04-quality/audits/persona-review/2026-05-24-outside-in-phase-1-closure-failure-mode-matrix.md`

**Per `outside-in-coverage-trigger.md` §3 Bước 5 mandate** + `inside-out-completeness-trigger.md` §3 3-source pull + `wave-tag-numbering-convention.md` (new tag-based scheme từ 2026-05-23+).

---

## §1 Brainstorm 4-bucket (per `inside-out-completeness-trigger.md` §3)

### Bucket A — ROADMAP §🚀 canonical (inside-out source 1)

Items đã track trong ROADMAP `documents/04-quality/gaps/ROADMAP.md` §🚀 Next Action 2026-05-23:
- Wave 105 post-merge audit suite (combined Wave 104.5+105 backlog per GAP-716 deadline 2026-05-25; 3-axis business-logic + api-contract + ops-readiness)
- AWS account restoration GAP-612 Day 7 (KYC docs uploaded)
- Wave thesis-2 NFR + beta + Ch.5-7 evidence (deferred chờ GAP-612)
- META retro cross-bucket pairwise annotation diff per pre-mutation-state-check.md §1.5
- Wave 105 live verify cluster (gated GAP-612)

### Bucket B — gap-status.csv canonical (inside-out source 2)

Filter `status IN (OPEN, PARTIAL, IN_PROGRESS, PLANNED) AND phase=phase-1-beta`:
- **50 P0 active** (22 OPEN + 26 PARTIAL + 1 PENDING + 1 IN_PROGRESS)
- **97 P1 active** (49 OPEN + 45 PARTIAL + 1 PENDING + 1 IN_PROGRESS)
- **38 P2 active**
- **3 P3 active**
- **Total: 188 active gaps**

15 gaps mention `GAP-612 / AWS restore / AWS suspended` trong notes — minimum subset gated AWS. Thực tế nhiều hơn (PARTIAL nhóm "Wave 98 80% + live verify post-deploy" toàn bộ gated AWS).

### Bucket C — outside-in audit findings (3 reports)

**C.1 Failure-mode matrix (M-CARRY + M-NEW-LOCAL):**

| Severity | Item | Source |
|---|---|---|
| 🔴 P0-CRITICAL | PaymentController `userId=1L` hardcoded (line 49/69) affects ALL payment records | M-CARRY GAP-203/215 |
| 🔴 P0 | Stored XSS admin panel | M-CARRY |
| 🔴 P0 | Zero idempotency POST mutations (signup/payment/enrollment/beta-request) | M-CARRY |
| 🔴 P0 | Enrollment race condition trên FULL class | M-CARRY |
| 🔴 P0 | Per-resource authz A01 OWASP unverified beyond tenant isolation | M-CARRY |
| 🟠 P1 | VND format violation billing UI (`$60.00` thay vì `1.500.000đ`) | M-NEW-LOCAL |
| 🟠 P1 | Churn email VN locale missing | M-NEW-LOCAL |
| 🟠 P1 | GAP-726 (already filed Wave 107 RST B2) | M-NEW-LOCAL |
| ⏳ P0×8 | AWS-gated cluster (signup API fallback / email delivery / API contract drift × 3 / doc perf SLA / alert rules / AI branding verify) | M-NEW-AWS-GATED — gated GAP-612 |
| 🟠 P0 | PDPL cookie/consent banner (GAP-353) — requires legal counsel | M-NEW-VENDOR |
| 🟠 P0 | Zalo/SMS OTP mobile signup (GAP-286) — requires Zalo OA registration | M-NEW-VENDOR |

**C.2 VN edu SaaS benchmark (5 table-stakes missing):**

| ID | Table-stakes | Map gap | Priority |
|---|---|---|---|
| TS-1 | Batch monthly invoice generator | GAP-297 | P0 BLOCKING |
| TS-2 | Guided onboarding tour | GAP-288 | P1 |
| TS-3 | Session reschedule / makeup class | GAP-291 | P0 BLOCKING |
| TS-4 | Parent Zalo/SMS notification | GAP-063 PARTIAL 50% | P0 |
| TS-5 | Parent dashboard MVP | GAP-139 | P1 |

3 differentiators preserve (AI Branding / PDPL audit trail / multi-tenant white-label).
8 Phase 2 acceptable defers (e-Invoice + bank webhook + VietQR + social login + analytics + multi-branch + ...).

**C.3 Persona walkthrough (5 force-multipliers, 15 NEW recommends):**

| Rank | Force-multiplier | Personas | Gap mapping |
|---|---|---|---|
| FM-1 | Data export self-service | 3/3 (P1+P2+P3) | **NEW gap P0** — no current gap |
| FM-2 | Zalo notification + VN support channel | 3/3 | extends GAP-063 + GAP-286 |
| FM-3 | Blank/empty state onboarding | 2/3 (P1+P2) | extends GAP-288 |
| FM-4 | Trust signals VN (testimonial + pricing VND + refund policy) | 3/3 | NEW gap P1 — landing page |
| FM-7 | Excel/CSV import wizard | 2/3 (P2+P3) | extends GAP-080 |

Plus 15 NEW recommends — top 5:
1. NEW-02 Data export self-service endpoint (→ becomes FM-1 force-multiplier)
2. NEW-05 Simple payment record P1 Solo (receipt log không cần gateway)
3. NEW-13 Mon-Sat schedule template + Tết holiday preset
4. NEW-17 Excel import wizard column mapping UI
5. NEW-09/NEW-04 Pricing page VND + Landing page VN edu positioning

### Bucket D — inside-out-queue.md + AskUserQuestion explicit (source 3)

Queue file kiểm tra: nếu không tồn tại HOẶC empty → skip per `inside-out-completeness-trigger.md` Bước 1 source 2 acceptable absent.

User explicit goal: "fix hết gaps phase 1 + RST phase 1". Realistic chunked → 6 sub-wave.

---

## §2 Wave 108-N (= wave-beta-readiness-1..6) sequence

### Wave beta-readiness-1 — P0 Security Cluster (security)

**Tag:** `beta-readiness`, secondary `[security, p0-critical]`
**Estimated:** 1-2 phiên (~4-6h)
**Risk:** HIGH — touches payment + auth core; cross-bucket conflict risk cao
**Blocking:** **PHẢI ship trước beta invite** (Phase 1 gate prerequisite)

| Bucket | Scope | Files | Test |
|---|---|---|---|
| A | PaymentController userId=1L fix — derive from JWT principal | `kiteclass-core/.../PaymentController.java` line 49/69; `ParentPaymentControllerTest` | IT test verify userId=authenticated |
| B | Stored XSS admin panel sanitize | KH admin frontend + BE escape | Playwright XSS payload test |
| C | Idempotency POST mutations (signup + payment + enrollment + beta-request) | 4 controllers + Idempotency-Key header support + DB constraint | IT test duplicate request → same response |
| D | Enrollment race condition on FULL class | `EnrollmentService` + pessimistic lock OR optimistic version | Concurrent IT test |
| E | Per-resource authz A01 OWASP review + add tests | Per controller `@PreAuthorize` audit | IT test cross-tenant cross-user |

**Out of scope (defer beta-readiness-5 AWS-gated):** live verify production payment + XSS detection in prod logs.

### Wave beta-readiness-2 — P0 Table-stakes Cluster (table-stakes)

**Tag:** `beta-readiness`, secondary `[table-stakes, billing, scheduling]`
**Estimated:** 2-3 phiên (~6-10h)
**Risk:** MEDIUM — new feature implementation; UX-heavy
**Blocking:** P2 Center Owner CANNOT operate beta without these

| Bucket | Scope | Gap | Notes |
|---|---|---|---|
| A | Batch monthly invoice generator (per-tenant cron) | GAP-297 | VND format mandatory per `vn-localization-audit-checklist.md` §2; Tết pause window |
| B | Session reschedule / makeup class flow | GAP-291 | UX standard VN edu; conflict check vs class schedule |
| C | Refund/cancellation invoice variant | (NEW gap candidate) | Pairs với invoice — refund slip |
| D | Tết holiday preset + Mon-Sat schedule template | NEW-13 | VN edu calendar convention |

### Wave beta-readiness-3 — Trust Gate Cluster (trust-gate)

**Tag:** `beta-readiness`, secondary `[trust-gate, data-export, vn-locale]`
**Estimated:** 1-2 phiên (~4-6h)
**Risk:** MEDIUM — new endpoints + UI
**Blocking:** Persona walkthrough FM-1/FM-4 — beta user trust signal

| Bucket | Scope | Source |
|---|---|---|
| A | Data export self-service endpoint (3/3 personas) | FM-1 NEW gap P0 |
| B | Pricing page VND format audit (sweep `/pricing` + admin billing UI) | M-NEW-LOCAL VND violation + FM-4 |
| C | Landing page VN edu positioning + testimonials section + refund policy | FM-4 + NEW-04/NEW-09 |
| D | Cookie/consent banner PDPL compliance (GAP-353) | M-NEW-VENDOR (legal counsel required cho production text) |

### Wave beta-readiness-4 — UX Polish + GAP-726 fix (ux-cluster)

**Tag:** `beta-readiness`, secondary `[ux, onboarding, import-wizard]`
**Estimated:** 2-3 phiên (~6-10h)
**Risk:** MEDIUM — wizard implementation + parent dashboard greenfield

| Bucket | Scope | Gap | Notes |
|---|---|---|---|
| A | Guided onboarding tour (first-hour) | GAP-288 + FM-3 | Empty state CTA → first class create |
| B | Parent dashboard MVP (attendance + payment + grades minimal) | GAP-139 | TS-5 — P2 cannot sell paid plan without parent visibility |
| C | Excel/CSV import wizard (column mapping UI) | GAP-080 + FM-7 + NEW-17 | Data migration blocker P2/P3 |
| D | KC `/branding/wizard` blank fix (Wave 107 RST B2) | GAP-726 | Hardcoded tenantId + SSR ECONNREFUSED 8080 |

### Wave beta-readiness-5 — AWS-gated Cluster (post-aws-restore)

**Tag:** `beta-readiness`, secondary `[aws-restore, live-verify]`
**Estimated:** 2-3 phiên (~6-10h) — gated GAP-612 AWS restore complete
**Risk:** HIGH — live production deployment + verification
**Blocking dependency:** GAP-612 Day 7+ AWS restore complete

| Bucket | Scope | Gap (cluster) |
|---|---|---|
| A | Wave 107 3 gap flip DONE (after live verify SES delivery) | GAP-543 / GAP-657 / GAP-659 (PARTIAL 95% → DONE) |
| B | Wave 105 live verify cluster (Owner/Manager/Parent persona prod) | Wave 105 follow-up |
| C | API contract drift cluster | GAP-231 / GAP-232 / GAP-233 |
| D | 8 P0 AWS-gated (signup API fallback / email delivery / doc perf SLA / alert rules / AI branding verify / + 3) | M-NEW-AWS-GATED list |
| E | Wave 105 post-merge audit suite refresh (3-axis business-logic + api-contract + ops-readiness) | GAP-716 (deadline 2026-05-25 — defer post-restore acceptable per AWS gate) |

### Wave beta-readiness-6 — RST remaining + closure prep (rst-cluster)

**Tag:** `beta-readiness`, secondary `[rst, closure-prep]`
**Estimated:** 1-2 phiên (~4-6h)
**Risk:** LOW — RST walk + final gap triage

| Bucket | Scope | Source |
|---|---|---|
| A | RST B-CRUD walk (B5-B8) — class + student + teacher + course CRUD | Wave 106 plan §3 |
| B | RST B-vận-hành walk (B9-B13) — attendance + invoice + report + settings + staff invite | Wave 106 plan §3 |
| C | RST Mảng C walk (Nhân viên) — 3 flows post B13 staff invite | Wave 106 plan §3 |
| D | RST Mảng D3+D4 walk (Quản trị) — admin functions | Wave 106 plan §3 |
| E | Final gap triage + Phase 1 BETA gate verification | quality-audit /100 ≥80 confirmation |

### Wave beta-readiness-7 — Phase 1 BETA closure (closure)

**Tag:** `beta-readiness`, secondary `[closure, phase-gate]`
**Estimated:** 1 phiên (~3-4h)
**Risk:** LOW — meta/governance

| Bucket | Scope |
|---|---|
| A | Quality audit /100 final run (target ≥80 per Phase 1 gate) |
| B | 5 beta tenant invite (gated all prior waves complete + AWS restored) |
| C | 2-tuần monitor period start (0 P0 incident criterion) |
| D | Phase 1 → Phase 2 transition decision (Quality ≥80 + 5 tenants + 0 P0 = trigger Phase 2 P3 medium-center scope) |

---

## §3 Wave dependency graph

```
GAP-612 AWS restore (Day 7+) ────────────────────────────────────┐
                                                                 │
beta-readiness-1 (Security P0)                                   │
  │                                                              │
  ├──> beta-readiness-2 (Table-stakes invoice + reschedule)      │
  │                                                              │
  ├──> beta-readiness-3 (Trust gate: export + VND + landing)     │
  │                                                              │
  └──> beta-readiness-4 (UX onboarding + parent dashboard +      │
       Excel wizard + GAP-726 fix)                               │
                                                                 │
                                                                 ▼
                                              beta-readiness-5 (AWS-gated cluster)
                                                                 │
                                                                 ▼
                                              beta-readiness-6 (RST remaining)
                                                                 │
                                                                 ▼
                                              beta-readiness-7 (Phase 1 BETA closure)
```

Wave 1-4 có thể chạy parallel (cross-bucket scope rời rạc). Wave 5 gated AWS restore. Wave 6-7 sequential.

---

## §4 State-Check Evidence (per `audit-to-gap-pipeline.md` §2.6)

| Symbol referenced | Verification command | Verdict |
|---|---|---|
| `PaymentController.userId=1L` | `grep -n "userId = 1L\|userId=1L" kiteclass/kiteclass-core/src/main/java/.../PaymentController.java` | ⚠️ verify-at-spawn (M-CARRY claim) |
| GAP-297 batch invoice | `bash scripts/query-gaps.sh GAP-297` | ✅ canonical CSV |
| GAP-291 session reschedule | `bash scripts/query-gaps.sh GAP-291` | ✅ canonical CSV |
| GAP-063 parent Zalo/SMS | `bash scripts/query-gaps.sh GAP-063` | ✅ canonical CSV (PARTIAL 50%) |
| GAP-288 guided onboarding | `bash scripts/query-gaps.sh GAP-288` | ✅ canonical CSV |
| GAP-139 parent dashboard | `bash scripts/query-gaps.sh GAP-139` | ✅ canonical CSV |
| GAP-080 Excel import | `bash scripts/query-gaps.sh GAP-080` | ✅ canonical CSV |
| GAP-726 wizard blank | `bash scripts/query-gaps.sh GAP-726` | ✅ shipped Wave 107 |
| GAP-612 AWS suspension | `bash scripts/query-gaps.sh GAP-612` | ✅ Day 7 KYC uploaded |
| Data export endpoint (FM-1) | `grep -rl "export\|download" kiteclass-core/src/main/java/com/.../controller/` | ⚠️ 🆕 to-be-created (Wave beta-readiness-3 Bucket A scope) |
| Mon-Sat schedule (NEW-13) | `grep -rl "schedule\|Schedule" kiteclass-core/src/main/java/` | ⚠️ verify-at-spawn |

---

## §5 Inside-out vs Outside-in coverage table

Per `outside-in-coverage-trigger.md` Bước 5 mandate (highlight new items chỉ outside-in surface):

| Source | Count Phase 1 closure scope |
|---|---|
| **Inside-out from canonical ROADMAP** | 5 items (Wave 105 audit + GAP-612 + thesis-2 + META retro) |
| **Inside-out from gap-status.csv** | 188 active gaps (50 P0 + 97 P1 + 38 P2 + 3 P3) |
| **Outside-in failure-mode matrix** | 17 new items (5 P0-CRITICAL + 3 P1 NEW-LOCAL + 8 AWS-gated + 2 VENDOR) |
| **Outside-in VN edu SaaS benchmark** | 5 table-stakes MISSING + 3 differentiators preserve + 8 Phase 2 defers |
| **Outside-in persona walkthrough** | 38 findings (14 captured + 15 NEW gap candidates + 9 deferred) |

**Force-multipliers cross-source convergence:**
- GAP-063 Parent Zalo/SMS — appears trong failure-mode (M-NEW-VENDOR), benchmark (TS-4), persona (FM-2). 3/3 source converge → P0 critical
- GAP-297 Batch invoice — benchmark (TS-1) + implicit failure-mode (no billing = no beta). Single-source but P0 blocking
- Data export (FM-1) — persona only but 3/3 personas affected → file NEW gap P0
- VND format violation — failure-mode (M-NEW-LOCAL) + persona (FM-4 trust gate)

---

## §6 NEW gap filings (must precede sub-wave plans)

Per outside-in findings, file 7 NEW gap candidates BEFORE Wave beta-readiness-1 starts:

| New gap | Source | Priority | Map sub-wave |
|---|---|---|---|
| GAP-727 Data export self-service endpoint | FM-1 persona 3/3 | P0 | beta-readiness-3 Bucket A |
| GAP-728 VND format sweep audit billing UI | M-NEW-LOCAL + FM-4 | P1 | beta-readiness-3 Bucket B |
| GAP-729 Landing page VN edu positioning + testimonials | FM-4 + NEW-04/NEW-09 | P1 | beta-readiness-3 Bucket C |
| GAP-730 Churn email VN locale template | M-NEW-LOCAL | P1 | beta-readiness-4 (or beta-readiness-2 nếu fit invoice context) |
| GAP-731 Tết holiday preset + Mon-Sat schedule template | NEW-13 + persona walkthrough | P1 | beta-readiness-2 Bucket D |
| GAP-732 Refund invoice variant (pairs với GAP-297) | benchmark TS-1 extension | P1 | beta-readiness-2 Bucket C |
| GAP-733 Excel import wizard column mapping UI extension | FM-7 + GAP-080 extends | P1 | beta-readiness-4 Bucket C |

Each NEW gap file: standalone `documents/04-quality/gaps/phase-1-beta/GAP-NNN-*.md` + CSV row. File trong same PR HOẶC follow-up PR before sub-wave plan.

---

## §7 Closure protocol per sub-wave

Mỗi sub-wave Wave `beta-readiness-N` follows standard protocol:

1. Plan PR draft (separate file `documents/03-planning/waves/wave-2026-MM-DD-beta-readiness-N-{descriptor}.md`) — per `wave-tag-numbering-convention.md` §2.2 filename + §2.4 frontmatter
2. State-check evidence §4 + bucket scope §3 + verification gates §5
3. Spawn pattern §6 — 3-5 agent parallel where bucket scope rời rạc, sequential where shared module conflict risk
4. Closure PR per sub-wave với scope-completeness reconciliation table per `wave-closure-scope-completeness.md` §3
5. Audit suite per `post-wave-audit-mandate.md` §2.1 file-pattern matrix — UI / Security / Business / API / Ops as applicable
6. Wave-history.jsonl append với new tag-based format per `wave-tag-numbering-convention.md` §2.5
7. ROADMAP §🚀 update + 4-target sync per `post-merge-sync-completeness.md` §2

---

## §8 Wave 108 = first sub-wave execution decision

**Recommend Wave 108 = Wave `beta-readiness-1` (P0 Security Cluster)** vì:
- ✅ HIGHEST priority — Phase 1 BETA gate prerequisite
- ✅ Local-executable — không gated AWS restore
- ✅ Concrete scope với CSV-tracked gaps (GAP-203 PaymentController + GAP-215 XSS + idempotency family)
- ✅ Failure-mode matrix marked P0-CRITICAL (blocks ANY beta invite)
- ✅ Cross-persona impact (security affects all 3 personas + Phase 2 carryover)

**Alternative options nếu user prefer different scope:**
- Wave 108 = beta-readiness-3 (trust gate) — quick win UX-side; no payment risk
- Wave 108 = beta-readiness-4 Bucket D only (GAP-726 fix isolated) — minimal scope quick win
- Wave 108 = file 7 NEW gaps first then beta-readiness-1 — gap-prep wave

---

## §9 Risk assessment + outside-in audit decision

Per `outside-in-coverage-trigger.md` §4 exemption row 4 — "User đã trải qua outside-in (audit gần đây ≤30 ngày)" — this roadmap shipped với 3 outside-in audit same session (2026-05-24). Audit findings feed Bucket C §1 brainstorm directly. **Outside-in COVERED for Wave beta-readiness-1..7 lookahead.**

Sub-wave plan PRs (Wave beta-readiness-N each) can SKIP outside-in audit IF scope stays within audit findings + ≤30 ngày freshness. Re-trigger outside-in audit if:
- Scope drift to NEW persona (vd if adding P4 / P5)
- New external dependency surface (vd vendor swap)
- 30 ngày elapsed without re-audit

---

## §10 Realistic timeline

Solo dev assumption + ~3 sessions/week + ~3-4h/session:

| Sub-wave | Sessions | Calendar |
|---|---|---|
| beta-readiness-1 (security P0) | 1-2 | Week 1 |
| beta-readiness-2 (table-stakes invoice+reschedule) | 2-3 | Week 1-2 |
| beta-readiness-3 (trust gate) | 1-2 | Week 2 |
| beta-readiness-4 (UX polish + GAP-726) | 2-3 | Week 2-3 |
| beta-readiness-5 (AWS-gated cluster — gated GAP-612) | 2-3 | Week 3-4 (gated AWS restore) |
| beta-readiness-6 (RST remaining 16 flows) | 1-2 | Week 4 |
| beta-readiness-7 (Phase 1 BETA closure) | 1 | Week 4 |

**Total realistic:** 3-4 tuần calendar (assuming AWS restore unblocks Week 3). Solo dev mode acceptable per `release-1-plan-2026.md` Phase 1 BETA 9-12 tuần budget — current pace within window.

---

## §11 Open items

- [ ] File 7 NEW gap candidates per §6 (GAP-727..733)
- [ ] Draft Wave beta-readiness-1 plan PR (first sub-wave execution)
- [ ] Verify PaymentController userId=1L state-check (per §4 ⚠️ verify-at-spawn)
- [ ] Confirm tag-based naming với user: `beta-readiness` OR alternative (`phase-1-closure`, `prod-readiness`, etc.)
- [ ] Defer / cancel decision: Wave 106 full RST plan (23 flows) — superseded by beta-readiness-6 chunked? Decision needed.
- [ ] Coordinate với Wave 105 post-merge audit suite deadline 2026-05-25 (GAP-716) — overlap với beta-readiness-5 Bucket E

---

## §12 Cross-link

- 3 outside-in audit shipped 2026-05-24:
  - `documents/04-quality/audits/persona-review/2026-05-24-outside-in-phase-1-closure-persona-walkthrough.md`
  - `documents/04-quality/audits/persona-review/2026-05-24-outside-in-phase-1-closure-vn-edu-saas-benchmark.md`
  - `documents/04-quality/audits/persona-review/2026-05-24-outside-in-phase-1-closure-failure-mode-matrix.md`
- Rules driving structure:
  - `.claude/rules/outside-in-coverage-trigger.md` §3 mandate (audits feed scope)
  - `.claude/rules/inside-out-completeness-trigger.md` §3 3-source pull
  - `.claude/rules/wave-tag-numbering-convention.md` §2 tag-based naming
  - `.claude/rules/wave-closure-scope-completeness.md` §3 reconciliation per sub-wave
  - `.claude/rules/meta-gap-priority.md` §3 Phase 1 BETA gate
  - `.claude/rules/release-fix-retry-budget.md` (retry discipline per sub-wave)
- Phase 1 BETA reference:
  - `documents/03-planning/roadmap/release-1-plan-2026.md` §3 Phase 1 (9-12 tuần)
  - `documents/04-quality/gaps/ROADMAP.md` §🚀 Next Action canonical queue
- Wave 107 just-shipped context: 4 PR merged (#1744 + #1745 + #1746 + #1747) + closure PR #1757

---

## §13 Log

- **2026-05-24 (planning):** Roadmap shipped. Sinh ra từ user goal 2026-05-23 "fix hết gaps phase 1 + RST phase 1" + 3 outside-in audit 2026-05-24. Per `outside-in-coverage-trigger.md` §3 + `inside-out-completeness-trigger.md` §3 (4-source brainstorm). Per `wave-tag-numbering-convention.md` §2 (new tag-based scheme `beta-readiness-1..7`). Realistic timeline 3-4 tuần calendar solo dev. Wave 108 recommend = beta-readiness-1 (P0 Security Cluster) — first execution. User confirm needed §11 Open items trước khi file NEW gaps + draft sub-wave plans.
