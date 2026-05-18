---
title: Release 1.5 Thesis Scope — consolidated plan
status: active
created: 2026-05-18
updated: 2026-05-18
phase: phase-1-beta
gaps: [GAP-646, GAP-647, GAP-648, GAP-649, GAP-650, GAP-651, GAP-652, GAP-653, GAP-287, GAP-297, GAP-293, GAP-562, GAP-518, GAP-286, GAP-538]
audits: [2026-05-18-thesis-persona-demo-audit, 2026-05-18-thesis-vn-saas-benchmark, 2026-05-18-thesis-defense-failure-mode-matrix]
---

# Release 1.5 Thesis Scope — consolidated plan

**Quyết định lock 2026-05-18:** Thesis scope chốt ở **Release 1.5** (Phase 1 BETA + Phase 1.5 paid features), defense window 2026-08-15 → 2026-10-15. Có option upgrade Release 2 (Phase 2 P3 medium-center) sau decision-point cuối Phase 1 (~2026-07-15) nếu Phase 1 close clean.

**Trigger:** User confirm 2026-05-18 — "lock Release 1.5, có thể upgrade sau Phase 1 close".

**Baseline references:**
- `documents/03-planning/roadmap/release-1-plan-2026.md` §4 Phase 1.5 PAID (existing scope, partially outdated post-Wave-93)
- 3 outside-in audits 2026-05-18: persona / VN benchmark / failure-mode matrix
- `documents/08-thesis/chapter-mapping.md` 7-chapter VN CS standard

---

## 1. Brainstorm — inside-out + outside-in

### 1.1 Inside-out from existing scope (release-1-plan-2026.md §4 Phase 1.5 PAID — updated post-Wave-93)

| Gap (original) | Wave 93 re-scope | Status |
|---|---|---|
| GAP-NEW-payment-processor-init | **CANCELLED** (PSP license barrier) → Casso/SePay webhook GAP-636 | Wave 93 audit decision |
| GAP-183 close (refund engine) | **Re-scope** manual SOP (KiteHub non-PSP) | Wave 93 |
| GAP-185 close (VAT) | **Re-scope** MISA MeInvoice partnership | Wave 93 |
| GAP-181 close (Acceptable Use Policy) | Defer (legal counsel needed) | Phase 2 pending |
| GAP-353c close (DSAR) | Promote Phase 1.5 (Manual email-based ok ban đầu) | Active |
| GAP-073 close (RTBF endpoint) | Promote Phase 1.5 (PDPL Art.14 mandate) | Active |
| GAP-NEW-pen-test-light | Active (OWASP top 10) | Active |
| GAP-NEW-deploy-runbook | DONE (per Wave 87/91 batches) | Closed |
| GAP-NEW-monitoring-dashboards | Partial (per GAP-115 / GAP-437) | Active |
| GAP-135 close (SLO definitions) | Partial | Active |

### 1.2 Inside-out from queue file (`documents/03-planning/inside-out-queue.md`)

- Premium plan (deferred Wave 79+) — N/A thesis
- Feedback channel (consumed Wave 78) — N/A
- Email content audit (consumed Wave 78) — N/A
- QR payment Wave 93 (consumed) — Casso/SePay GAP-636
- OCR auto-confirm (REJECTED Wave 93) — N/A

### 1.3 Inside-out from CSV (`phase-1.5-paid` rows)

| Gap | Title | Status |
|---|---|---|
| GAP-643 | sessionStorage XSS httpOnly cookie option | OPEN P2 |
| GAP-636 | Casso/SePay webhook investigation | OPEN P1 |
| GAP-625 | KYC merchant onboarding | (per Wave 93) |
| Others Phase 1.5 paid scope | (small set) | various |

### 1.4 Outside-in NEW (3 audits 2026-05-18)

Per `outside-in-coverage-trigger.md` §4 — sister rule satisfied, audits ran today, findings consolidated below.

**Persona demo audit findings:** 7 P0 BLOCKING product gaps + 4 P1 IF TIME (re-prioritized for thesis demo).

**VN benchmark audit findings:** Release 1.5 scope AMBITIOUS top 5-10% VN CS thesis 2026. DEPTH-FIRST demo style. ADD load test + PDPL + isolation demo. DROP K-12 to "future scope".

**Failure-mode matrix findings:** 20 examiner challenges, top 10 P0 thesis-blockers, top 5 nên-drop terminology cleanup.

### 1.5 Consolidated thesis-blockers (META + Product)

**8 META gaps NEW filed:**
- GAP-646 thesis-docx-pipeline (P0)
- GAP-647 thesis-bibliography-ieee (P0)
- GAP-648 thesis-nfr-data-capture (P0)
- GAP-649 thesis-beta-cohort-execution (P0)
- GAP-650 thesis-chapter-1-literature (P0)
- GAP-651 thesis-image-curation (P1 META)
- GAP-652 thesis-multi-tenant-isolation-demo (P1)
- GAP-653 thesis-defense-prep-deck (P1)

**7 product gaps re-prioritized thesis-demo-blocker:**
- GAP-287 branding wizard skip/default (P0)
- GAP-297 batch monthly invoice UX (P0)
- GAP-293 monthly income dashboard (P0 — upgrade từ P1)
- GAP-562 RBAC role separation PARTIAL (P0)
- GAP-518 BE/FE admin role mismatch PARTIAL (P0)
- GAP-286 mobile OTP — switch email-only flow OK (P0)
- GAP-538 day-1 onboarding + demo data seed PARTIAL (P0)

**4 product gaps P1 IF TIME:**
- GAP-288 onboarding tour
- GAP-292 per-session pricing model
- GAP-294 NO_SHOW attendance status
- GAP-636 Casso/SePay webhook

---

## 2. Scope — 4 work tracks

### Track A — Thesis infrastructure META (4 P0 + 1 P1)

| # | Gap | Effort | Output |
|---|---|---|---|
| 1 | GAP-646 thesis-docx-pipeline | ~1 tuần | Template + assembler script + ThesisReportBuilder |
| 2 | GAP-647 thesis-bibliography-ieee | ~3 ngày | bibliography.md ~30 refs + citation-extract skill |
| 3 | GAP-650 thesis-chapter-1-literature | ~1 tuần | Competitor + AI theory + VN law sections |
| 4 | GAP-651 thesis-image-curation | ~3-5 ngày | Skill + criteria + INDEX.md + 5-10 sample figures |
| 5 | GAP-653 thesis-defense-prep-deck | ~1 tuần | Reveal.js deck + Q&A sheet + backup video |

### Track B — Thesis content data capture (1 P0)

| # | Gap | Effort | Output |
|---|---|---|---|
| 6 | GAP-648 thesis-nfr-data-capture | ~1 tuần | k6 load test + CloudWatch screenshots + AWS Cost CSV + Service Registry benchmark |

### Track C — Beta cohort execution (1 P0 long-running)

| # | Gap | Effort | Output |
|---|---|---|---|
| 7 | GAP-649 thesis-beta-cohort-execution | ~9 tuần (overlapping) | 5-7 invites + ≥4 signed reviews + aggregate findings doc |

### Track D — Product demo-blocker gaps (7 P0 + 4 P1)

| # | Gap | Priority | Effort | Demo impact |
|---|---|---|---|---|
| 8 | GAP-287 branding wizard skip | P0 | ~3 ngày | Both personas onboarding |
| 9 | GAP-297 batch monthly invoice UX | P0 | ~1 tuần | Revenue model demo |
| 10 | GAP-293 monthly income dashboard | P0 | ~1 tuần | SaaS value proof |
| 11 | GAP-562 RBAC role separation | P0 | ~3 ngày | Live demo bug elimination |
| 12 | GAP-518 admin role mismatch | P0 | ~2 ngày | Admin demo unblock |
| 13 | GAP-286 mobile OTP → email-only | P0 | ~2 ngày | Signup gate |
| 14 | GAP-538 onboarding + seed data | P0 | ~3 ngày | Empty state risk |
| 15 | GAP-652 multi-tenant isolation demo | P1 | ~3 ngày | Unique differentiator |
| 16 | GAP-288 onboarding tour | P1 | ~3 ngày | Polish |
| 17 | GAP-292 per-session pricing | P1 | ~3 ngày | Story diversity |
| 18 | GAP-294 NO_SHOW attendance | P1 | ~2 ngày | Real-world detail |
| 19 | GAP-636 Casso/SePay webhook | P1 | ~1 tuần | VN-market fit narrative |

---

## 3. Wave execution plan

**Note:** Wave 97 (audit P0+P1 gate-closing) đang ship trước, KHÔNG nằm trong Release 1.5 thesis scope nhưng dependency cho audit scores (Business Logic 70→78+, API Contract 79→85+).

### Wave 98 (next) — Thesis infrastructure foundation (Track A items 1-2)

- Bucket A: GAP-646 thesis-docx-pipeline (BE infrastructure)
- Bucket B: GAP-647 thesis-bibliography-ieee (Docs)
- Bucket C: GAP-650 thesis-chapter-1-literature Part 1 — competitor + AI sections
- Bucket D: GAP-648 thesis-nfr-data-capture Step 1 — k6 load test scenario

Spawn: 4 bg-agents parallel after Wave 97 closure. Wall-clock ~1 tuần.

### Wave 99 — Product demo-blockers wave 1 (Track D P0 batch 1)

- Bucket A: GAP-518 admin role mismatch close-out (90% → 100%)
- Bucket B: GAP-287 branding wizard skip/default
- Bucket C: GAP-562 RBAC role separation
- Bucket D: GAP-538 onboarding + seed data close-out

Wall-clock ~1.5 tuần.

### Wave 100 — Product demo-blockers wave 2 + thesis Chapter 1 Part 2

- Bucket A: GAP-297 batch monthly invoice UX (largest scope)
- Bucket B: GAP-293 monthly income dashboard
- Bucket C: GAP-286 mobile OTP → email-only switch
- Bucket D: GAP-650 thesis-chapter-1-literature Part 2 — VN law section + methodology extend

Wall-clock ~2 tuần.

### Wave 101 — Thesis evidence capture + figure curation

- Bucket A: GAP-651 thesis-image-curation + sample figures
- Bucket B: GAP-648 thesis-nfr-data-capture continuation (CloudWatch screenshots + AWS cost)
- Bucket C: GAP-652 multi-tenant isolation demo + seed-thesis-demo-tenants.sh
- Bucket D: GAP-636 Casso/SePay webhook investigation

Wall-clock ~1 tuần.

### Wave 102 — Thesis polish + beta cohort kickoff

- Bucket A: GAP-294 + GAP-292 (P1 quick wins)
- Bucket B: GAP-288 onboarding tour
- Bucket C: GAP-649 thesis-beta-cohort-execution Week 1 — invite 7 candidates
- Bucket D: GAP-647 bibliography expand (~30 refs to populated state)

Wall-clock ~1 tuần.

### Wave 103-104 — Beta cohort onboarding + first feedback

(Beta cohort 9-week timeline parallel với product tweaks based on feedback)

### Wave 105+ — Defense prep + thesis assembly

- GAP-653 thesis-defense-prep-deck — Reveal.js deck + Q&A sheet
- GAP-646 thesis-docx-pipeline final assembly run
- 2 practice runs T-3 + T-2 weeks before defense

---

## 4. Timeline + Decision-point cuối Phase 1

```
2026-05-18 ─────── NOW (Release 1.5 locked)
                       │
                       │ Wave 97 (audit P0+P1) — in flight
                       │ Wave 98-99 (thesis infra + demo-blocker 1)
                       │
2026-06-15 ─────── ~Wave 99 close
                       │ Wave 100-101 (demo-blocker 2 + evidence)
                       │
2026-07-15 ◄────── DECISION-POINT cuối Phase 1 ◄────────
                       │
                       │ Trigger evaluation:
                       │ ✅ Phase 1 close clean (≥80 + 5 beta + 0 P0) → upgrade Release 2 option
                       │ ⚠️ Phase 1 PARTIAL → continue Release 1.5
                       │ ❌ Phase 1 stuck → cut to Release 1 PROD only
                       │
                       │ Wave 102-104 (polish + beta cohort active)
                       │
2026-08-01 ─────── Wave 105+ defense prep
                       │
2026-08-15 ─────── DEFENSE WINDOW OPEN (target ready)
2026-09-15 ─────── DEFENSE WINDOW MID
2026-10-15 ─────── DEFENSE WINDOW CLOSE
```

### Decision criteria @ 2026-07-15

| Phase 1 state | Action | Thesis impact |
|---|---|---|
| ≥80 quality + 5 beta live + 0 P0 2 tuần | **Upgrade Release 2** option (add P3 Manager scope to thesis) | Stronger thesis, narrower buffer |
| ≥75 quality + ≥3 beta live + ≤2 P0 | **Stay Release 1.5** | Plan as written |
| <75 quality OR <3 beta OR >2 P0 | **Cut to Release 1 PROD only** (drop Phase 1.5 paid features từ thesis demo) | Defensive, defense buffer maximized |

---

## 5. Risk mitigation

### 5.1 Product demo risks (from Persona audit Move 1+2)

- **GAP-287 wizard "cold start"** — first impression failure → Wave 99 priority
- **GAP-297 + GAP-293 billing UI missing** → loss of revenue model demo → Wave 100 priority
- Backup: pre-recorded demo per GAP-653 backup video

### 5.2 Thesis content risks (from Failure-mode audit)

- **Chapter 1 literature thin** → examiner drills sources → GAP-650 ship Wave 98-100 across 2 waves
- **IEEE citations vắng** → grading deduction → GAP-647 priority Wave 98
- **No NFR data** → "không có load test = điểm trừ top schools" → GAP-648 Wave 98+101
- **Internal terminology** (Wave/GAP/bucket) leak vào thesis → Cleanup tracked GAP-653 Step terminology sanitize

### 5.3 Beta cohort risks (from Persona/VN benchmark)

- **9-week timeline tight** — start NGAY 2026-05-18, no delay
- **No signed feedback** — feedback form template + PDF + signature mandatory GAP-649 Step 3
- **PDPL consent ethics** — anonymization protocol GAP-649 Step 6

### 5.4 Defense day risks (from Failure-mode aggregate)

- **Live demo bug** — backup video GAP-653 Step 4
- **20 examiner Q&A** — response sheet GAP-653 Step 3
- **v2.0.0 stable ≥2 tuần trước defense** — feature freeze T-3 weeks before defense

---

## 6. Chapter mapping update

Per `documents/08-thesis/chapter-mapping.md` — extend với new artifacts từ Track A/B/C:

| Chapter | Existing sources | NEW Release 1.5 sources |
|---|---|---|
| Ch1 Introduction | `01-business/README.md` + `07-archived/research/competitive/` | GAP-650 → `references/chapter-1-{competitor,ai-techniques,vn-law-compliance}.md` |
| Ch2 Theoretical | `references/technology-stack.md` + `references/methodology.md` | GAP-650 Step 4 methodology extend (audit-driven dev section) |
| Ch3 Requirements | `01-business/` + `06-diagrams/plantuml/` | (existing OK) |
| Ch4 Design | `02-architecture/` + `06-diagrams/plantuml/` | + bounded context diagram (Failure-mode A5) |
| Ch5 Implementation | `05-guides/` + `03-planning/kitehub-saas-implementation-plan.md` | (existing OK) — rename terminology per Failure-mode top-5 cleanup |
| Ch6 Testing | `04-quality/` audits | GAP-648 NFR data + `08-thesis/beta-feedback/aggregate-findings.md` + audit score evolution chart |
| Ch7 Conclusion | `04-quality/` final | + honest limitations chapter (Failure-mode preempt) + future scope (K-12 Phase 3 mention only) |

---

## 7. Closure trigger

Release 1.5 thesis-scope = COMPLETE khi:

- [ ] Tất cả 8 META gaps DONE (GAP-646..653)
- [ ] Tất cả 7 product P0 demo-blocker DONE (GAP-287/297/293/562/518/286/538)
- [ ] ≥4 signed beta reviews collected (GAP-649)
- [ ] Thesis DOCX render clean ~80-120 trang (GAP-646 final assembly run)
- [ ] Defense deck + Q&A sheet + backup video ready (GAP-653)
- [ ] v2.0.0 stable tag ≥2 tuần trước defense
- [ ] ≥2 practice runs với advisor/alumni done

**Estimated total wall-clock:** 8-10 tuần Wave 98-105+ (paired với Phase 1 close in parallel). Defense ready by 2026-08-01 — 2 tuần buffer trước defense window open 2026-08-15.

---

## 8. Log

- **2026-05-18 (active):** Plan created. Lock Release 1.5 thesis scope. 8 META gaps GAP-646..653 filed. 7 product gaps re-prioritized. 3 outside-in audits saved to persona-review/. Decision-point cuối Phase 1 (~2026-07-15) cho upgrade Release 2 option.
