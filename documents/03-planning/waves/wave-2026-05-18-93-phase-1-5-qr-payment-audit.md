---
title: Wave 93 — Phase 1.5 PAID payment scope outside-in audit + 12 new gaps + 4 re-scope existing
status: complete
created: 2026-05-18
updated: 2026-05-18
waves: [93]
gaps: [GAP-625, GAP-626, GAP-627, GAP-628, GAP-629, GAP-630, GAP-631, GAP-632, GAP-633, GAP-634, GAP-635, GAP-636, GAP-108, GAP-183, GAP-185, GAP-594, GAP-259, GAP-581, GAP-577, GAP-578, GAP-301]
audit_reports: [2026-05-18-phase-1-5-qr-payment-outside-in.md, 2026-05-18-phase-1-5-26-gaps-re-triage.md, 2026-05-18-phase-1-5-ocr-auto-confirm-outside-in.md]
---

# Wave 93 — Phase 1.5 PAID payment scope outside-in audit

**Goal:** Outside-in audit lock Phase 1.5 PAID payment scope với 3-agent convergence + handle 2 inside-out evolution proposals (QR upload + OCR auto-confirm) + re-triage 26 phase-1.5-paid gaps cho duplicate/overlap/phase-mismatch.

**Trigger:** User inside-out proposal 2026-05-18 — "QR upload thay payment processor" + follow-up "OCR auto-confirm upload ảnh chuyển khoản". Triggers `outside-in-coverage-trigger.md` lần thứ 2 + lần thứ 3 trong cùng session (closes `feedback_outside_in_recurring_miss.md` recurrence).

**Estimated wall-clock:** ~60min coordinator + 5 parallel agents (Wave 93 base) + 3 parallel agents (OCR audit) + manual consolidation. Actual: ~75min total.

---

## 1. Brainstorm (5-10 min)

### Q1 — Alignment: 4 buckets inside-out + outside-in sources

| Source | Items |
|---|---|
| **Inside-out from canonical (ROADMAP)** | Phase 1.5 PAID original scope per `release-1-plan-2026.md` §4 — 5-7 BLOCKING + 4-5 STRONGLY-recommend gaps target P1+P2 paid market |
| **Inside-out from queue file** | 2 items consumed Wave 93: (a) QR upload approach 2026-05-18; (b) OCR auto-confirm 2026-05-18 |
| **Inside-out from re-triage audit** | 26 existing phase-1.5-paid gaps re-triage (Agent 5 ship report) — duplicate/overlap/phase-mismatch findings |
| **Outside-in NEW (per `outside-in-coverage-trigger.md`)** | 3-agent audit base (persona/benchmark/failure-mode) cho QR + 3-agent audit follow-up cho OCR = 6 outside-in agents total Wave 93 |

### Q2 — Trade-offs

| Alternative | Rejected because |
|---|---|
| Full payment processor (VNPay/MoMo merchant integration) original Phase 1.5 plan | PSP license risk + KYC merchant onboarding fail cho hộ kinh doanh dạy thêm; 90%+ user fail KYC. Industry skip processor cho persona <50 HS |
| OCR auto-confirm receipt upload | 0/7 VN edu SaaS competitor dùng OCR; format drift + 2026 AI fake receipts 14% + 75% human miss-rate; Casso/SePay webhook là dominant pattern |
| Self-build VAT engine | TCT portal integration + chữ ký số + invoice number management = compliance hell; partnership MISA MeInvoice industry standard |
| Self-build refund engine | KiteHub non-PSP — refund flow happens off-platform via Owner bank app; KiteHub track audit + policy only |
| Defer all 11 new gaps Wave 94 | User chose atomic landing Wave 93 — single PR atomic; better-organized than serial filing |

### Q3 — Risks + recovery per bucket

| Risk | Recovery |
|---|---|
| 11 new gaps + 4 re-scope = large PR; reviewer fatigue | Atomic structure: 3 audit reports + 11 gaps + 4 amends + CSV + wave plan = single coherent unit per user explicit choice option C |
| Agent 5 DUPLICATE merge GAP-259/581 may need verification before close | Cross-ref Log entries added BOTH files; flagged §6 follow-up for user decision post-merge. NO destructive close in this PR |
| Phase mismatch GAP-123/124/125/415 (infrastructure) needs user decision | Flagged §6 follow-up for user decision; NO move yet |
| OCR proposal rejected via benchmark agent — user may want re-evaluate | OCR audit fully documented `2026-05-18-phase-1-5-ocr-auto-confirm-outside-in.md`; future re-evaluation can challenge findings + revise |
| Phase 1.5 timeline extended ~3-4 tuần (now 4-6 tuần baseline + webhook investigation) | Wave 31-32 Phase 1.5a base QR manual mark; Wave 33-34 Phase 1.5b webhook investigation; Wave 35 PUBLIC PAID LAUNCH preserved |

---

## 2. Task Breakdown

| Bucket | Owner | Effort | Disjoint? | Status |
|--------|-------|--------|-----------|--------|
| 0 — Outside-in audit base QR (3 agents parallel) | bg-agents | ~25 min | ✅ research independent | ✅ DONE |
| A — Draft 3 P0 new gaps GAP-625/626/627 | bg-agent A | ~6 min | ✅ greenfield files | ✅ DONE |
| B — Draft 5 P1 new gaps GAP-628..632 | bg-agent B | ~8 min | ✅ greenfield files | ✅ DONE |
| C — Draft 3 P2 new gaps GAP-633/634/635 | bg-agent C | ~4 min | ✅ greenfield files | ✅ DONE |
| D — Re-scope 4 existing GAP-108/183/185/594 | bg-agent D | ~5 min | ✅ disjoint files (additive Log entries) | ✅ DONE |
| 5 — Re-triage 26 existing phase-1.5-paid gaps | bg-agent 5 | ~12 min | ✅ research-only report | ✅ DONE |
| 6 — Outside-in audit OCR (3 agents parallel) | bg-agents | ~10 min | ✅ research independent | ✅ DONE |
| Coordinator — synthesize + write audits + GAP-636 + CSV + queue + plan + PR | me | ~30 min | sequential consolidation | 🟡 in-flight |

**Disjoint check:** All bg-agents independent file targets — no cross-file conflict. Coordinator owns CSV + wave plan + queue + PR consolidation.

---

## 3. Scope (compact schema)

**Stake tier:** MEDIUM → model Opus medium. Mostly governance scope (audit reports + gap files + wave plan); zero code change.
**Cross-layer:** NO → no Bucket 0 Foundation per `contract-first-for-cross-layer.md`.

### 3.1 Files created Wave 93

| Artifact | Path | Owner |
|---|---|---|
| Audit report base | `documents/04-quality/audits/persona-review/2026-05-18-phase-1-5-qr-payment-outside-in.md` | Coordinator |
| Audit report re-triage | `documents/04-quality/audits/persona-review/2026-05-18-phase-1-5-26-gaps-re-triage.md` | Agent 5 |
| Audit report OCR | `documents/04-quality/audits/persona-review/2026-05-18-phase-1-5-ocr-auto-confirm-outside-in.md` | Coordinator |
| GAP-625 P0 foundation | `documents/04-quality/gaps/GAP-625-qr-payment-foundation-kyc-multi-tenant-binding.md` | Agent A |
| GAP-626 P0 PDPL PII | `documents/04-quality/gaps/GAP-626-qr-payment-pdpl-pii-handling.md` | Agent A |
| GAP-627 P0 amount mismatch | `documents/04-quality/gaps/GAP-627-payment-amount-mismatch-detection.md` | Agent A |
| GAP-628 P1 batch reconcile | `documents/04-quality/gaps/GAP-628-qr-batch-reconcile-api.md` | Agent B |
| GAP-629 P1 refund SOP | `documents/04-quality/gaps/GAP-629-qr-refund-workflow-sop.md` | Agent B |
| GAP-630 P1 evidence storage | `documents/04-quality/gaps/GAP-630-qr-evidence-receipt-storage.md` | Agent B |
| GAP-631 P1 account verify | `documents/04-quality/gaps/GAP-631-qr-account-verification-refresh.md` | Agent B |
| GAP-632 P1 mark-paid audit | `documents/04-quality/gaps/GAP-632-qr-manual-mark-paid-audit-trail.md` | Agent B |
| GAP-633 P2 VietQR EduPay Phase 2 | `documents/04-quality/gaps/GAP-633-vietqr-edupay-napas-partnership-phase-2.md` | Agent C |
| GAP-634 P2 MISA MeInvoice | `documents/04-quality/gaps/GAP-634-misa-meinvoice-partnership-vat-einvoice.md` | Agent C |
| GAP-635 P2 installment Phase 2 | `documents/04-quality/gaps/GAP-635-qr-installment-payment-support-phase-2.md` | Agent C |
| GAP-636 P1 Casso/SePay webhook | `documents/04-quality/gaps/GAP-636-casso-sepay-webhook-integration-investigation.md` | Coordinator |
| Wave plan (this file) | `documents/03-planning/waves/wave-2026-05-18-93-phase-1-5-qr-payment-audit.md` | Coordinator |

### 3.2 Files amended Wave 93

| Artifact | Path | Owner | Change |
|---|---|---|---|
| GAP-108 re-scope | `documents/04-quality/gaps/GAP-108-payment-invoice-config-hardcoded.md` | Agent D | `## Scope Refinement (2026-05-18 audit)` + Log entry |
| GAP-183 re-scope | `documents/04-quality/gaps/GAP-183-refund-dispute-resolution-policy.md` | Agent D | Same pattern |
| GAP-185 re-scope | `documents/04-quality/gaps/pending/GAP-185-billing-terms-vat-tct-compliance.md` | Agent D | Same pattern |
| GAP-594 re-scope | `documents/04-quality/gaps/GAP-594-refund-policy-30-day-money-back.md` | Agent D | Same pattern |
| GAP-259 cross-ref | `documents/04-quality/gaps/GAP-259-gateway-rate-limit-tenant-key.md` | Coordinator | Log entry DUPLICATE flag + Wave 93 payment scope cross-ref |
| GAP-581 cross-ref | `documents/04-quality/gaps/GAP-581-per-tenant-rate-limit.md` | Coordinator | Same pattern |
| GAP-577 cross-ref | `documents/04-quality/gaps/GAP-577-platform-admin-hardening-wave-86.md` | Coordinator | KYC dependency cross-ref |
| GAP-578 cross-ref | `documents/04-quality/gaps/GAP-578-p2-owner-2fa-mandatory.md` | Coordinator | KYC dependency cross-ref |
| GAP-301 cross-ref | `documents/04-quality/gaps/pending/GAP-301-tenant-data-export-bundle-completeness.md` | Coordinator | DSAR distinction vs GAP-626 + new Log section |
| Inside-out queue | `documents/03-planning/inside-out-queue.md` | Coordinator | 2 consumed entries (QR + OCR) |
| gap-status.csv | `documents/04-quality/gaps/gap-status.csv` | Coordinator | 12 new rows + 4 last_verified updates |
| audits-index.csv | `documents/04-quality/audits/audits-index.csv` | Coordinator | 3 new audit rows |

### 3.3 State-Check Evidence (per `audit-to-gap-pipeline.md` §2.6)

| Symbol | Verification | Verdict |
|---|---|---|
| 12 new GAP-IDs unique | `bash scripts/query-gaps.sh GAP-62 ""` returns 0 hits before Wave 93 | ✅ canonical free |
| Casso webhook code references | `grep -rl "casso\|sepay" kitehub/ kiteclass/` returns 0 hits | ✅ greenfield Phase 1.5b scope |
| 4 re-scope target files exist | `ls documents/04-quality/gaps/GAP-{108,183,594}*.md pending/GAP-185*.md` | ✅ all exist |
| 5 cross-ref target files exist | `ls documents/04-quality/gaps/GAP-{259,581,577,578}*.md pending/GAP-301*.md` | ✅ all exist |
| Inside-out queue file exists + accepts append | `ls documents/03-planning/inside-out-queue.md` | ✅ exists, format follows §Format |

---

## 4. Audit reports verdict

### 4.1 QR base audit (3-agent convergence)

> ✅ **PROCEED with QR approach cho cả P1 + P2 Phase 1.5** — mandatory do compliance VN (PSP license + KYC barrier). Phase 2 pivot VietQR EduPay partnership khi PH > 100.

Score: N/A (persona-review category — non-scoring).

### 4.2 Re-triage 26 gaps audit

> ✅ 12 KEEP / 8 RE-SCOPE / 4 MOVE-PHASE / 2 OVERLAP / 0 CANCEL. 1 DUPLICATE merge candidate GAP-259≈GAP-581. KYC audit-log dependency GAP-625↔GAP-577/578. Phase ambiguity GAP-123/124/125/415 (user decision §6).

### 4.3 OCR audit (3-agent convergence override)

> 🔴 **OCR auto-confirm REJECTED Phase 1.5+** — benchmark agent OVERRIDES persona + failure-mode conditional verdicts. VN edu SaaS 0/7 dùng OCR; Casso/SePay webhook là dominant pattern 2026; ~0% fraud risk; better path adopted.

Pivot: GAP-636 P1 Casso/SePay webhook investigation Phase 1.5b. OCR optional fallback Phase 2.

---

## 5. Quality bar + acceptance

Wave 93 ship criteria:

- [x] All 11 new gap files written với measurable AC + Vietnamese narrative + cross-references
- [x] 4 re-scope gap files amended với scope refinement section + Log entry
- [x] 5 cross-reference gap files amended (additive Log entries only; no Status change)
- [x] 3 audit reports written với 3-agent transcript citations
- [x] gap-status.csv 12 new rows + 4 last_verified updates
- [x] audits-index.csv 3 new audit rows
- [x] Inside-out queue 2 consumed entries
- [x] Wave plan (this file)
- [ ] CI `gap-status-csv` validator PASS post-merge
- [ ] CI `audits-index-csv` validator PASS post-merge
- [ ] PR description cites 3 audit reports + 11 new gaps + 4 re-scope + 5 cross-ref + Agent 5 follow-up §6

---

## 6. Follow-up actions (post-merge user decisions)

### 6.1 Agent 5 re-triage findings requiring user decision

| # | Action | Recommendation | Status |
|---|---|---|---|
| 1 | **MERGE GAP-581 → GAP-259 DUPLICATE** | Close GAP-581 as DUPLICATE in CSV; GAP-259 retains canonical scope (PARTIAL 50%, P1, since 2026-04-28). Cross-ref Log entries already added BOTH files Wave 93. | ⏳ Pending user decision post-merge |
| 2 | **Phase mismatch decision GAP-123/124/125/415** | Infrastructure gaps assigned phase-1.5-paid không có payment dependency. Options: (a) confirm phase-1.5-paid là deployment prerequisite, (b) move phase-1-beta, (c) move phase-2 EKS migration. Need user judgment. | ⏳ Pending user decision |
| 3 | **GAP-625↔GAP-577 sequential ordering** | KYC infrastructure (GAP-625 Phase 1.5a) ships before Wave 86 admin audit log (GAP-577) → shared immutable log pattern. Document sequential dependency. | ✅ Cross-ref Log entries added Wave 93 — no decision needed |
| 4 | **GAP-625↔GAP-578 KYC + 2FA ordering** | KYC identity-verified baseline (GAP-625 Phase 1.5a) → GAP-578 2FA layer leverages identity. User decision: defer GAP-578 Wave 86 OR pair Phase 1.5a sequential | ⏳ Pending user decision |
| 5 | **GAP-301 vs GAP-626 DSAR scope clarification** | Tenant-DSAR (GAP-301) vs PH-DSAR (GAP-626) distinct. Both retained pending legal counsel. | ✅ Cross-ref Log entries added Wave 93 — no decision needed |

### 6.2 Wave 93 deliverables NOT in scope (defer Wave 94+)

| Item | Defer to | Reason |
|---|---|---|
| Casso vs SePay vendor evaluation execute | Wave 94+ via GAP-636 | Wave 93 = file gap only; execution defers Phase 1.5b trigger |
| Webhook receiver implementation | Wave 94+ via GAP-636 | Same as above |
| GAP-625/626/627 P0 foundation implementation | Wave 31-32 Phase 1.5a trigger | Wave 93 = file gaps only |
| 11 new gaps' code implementation | Phase 1.5a/1.5b execution waves | Wave 93 = governance + planning only |

### 6.3 OCR proposal residual

OCR rejected primary. Optional Phase 2 fallback path:
- If Casso/SePay coverage incomplete cho some VN banks → may revisit OCR-only Phase 2 fallback
- File new gap nếu user wants explicit Phase 2 OCR follow-up tracking; defer for now

---

## 7. Audit closure protocol

Per `wave-closure-scope-completeness.md` §3 — every §3 scope item categorized DONE / PARTIAL / NOT-IMPLEMENTED:

| §3 item | Category | Reference |
|---|---|---|
| 12 new gap files | ✅ DONE | All files exist + verified state-check |
| 4 re-scope amendments | ✅ DONE | Agent D verdict: all 4 successful |
| 5 cross-ref Log entries | ✅ DONE | Coordinator applied |
| 3 audit reports | ✅ DONE | All in `documents/04-quality/audits/persona-review/` |
| CSV updates | ✅ DONE | gap-status.csv + audits-index.csv |
| Inside-out queue | ✅ DONE | 2 consumed entries |
| Wave plan | ✅ DONE | This file |
| CI validators PASS | ⏳ post-merge | Will verify in PR checks |
| §6 follow-up user decisions | 🟡 PARTIAL | 3/5 require user decision post-merge (documented above) |

Wave 93 status: **complete** post-PR-merge.

---

## 8. Log

- **2026-05-18:** Wave 93 shipped. Triggered by user inside-out proposal "QR upload thay payment processor" (2026-05-18). Outside-in audit fired lần thứ 2 trong session (after inside-out 1: original Phase 1.5 brainstorm Wave 91-92, inside-out 2: QR proposal). Subsequent inside-out 3: OCR auto-confirm — outside-in fired lần thứ 3, benchmark agent OVERRIDES với Casso/SePay webhook recommendation. User extended Wave 93 scope per Agent 5 re-triage findings (26 gaps). Atomic landing: 3 audit reports + 11 new gaps + 4 re-scope + 5 cross-ref + CSV updates + wave plan = 1 PR. Demonstrates `outside-in-coverage-trigger.md` 5-Bước flow applied 2 lần liên tiếp same session + value of external benchmark agent catching alternative path outside original problem framing. Closes `feedback_outside_in_recurring_miss.md` recurrence pattern (3 inside-out proposals trong session, 3 outside-in audits fired, 0 missed). Reviewer: @nguyenvankiet (solo-dev wave coordinator).
