---
title: Wave 100 — Release 1.5 Thesis Push
status: active
created: 2026-05-19
updated: 2026-05-19
phase: phase-1-beta
wave: 100
gaps: [GAP-650, GAP-286, GAP-297, GAP-293, GAP-680, GAP-681]
audits: [2026-05-18-thesis-persona-demo-audit, 2026-05-18-thesis-vn-saas-benchmark, 2026-05-18-thesis-defense-failure-mode-matrix]
audience: dev
---

# Wave 100 — Release 1.5 Thesis Push

**Reference canonical:** [`documents/03-planning/roadmap/release-1.5-thesis-scope.md`](../roadmap/release-1.5-thesis-scope.md) — defense window 2026-08-15 → 2026-10-15.

**Bucket E (GAP-518/538 PARTIAL → DONE):** DEFERRED Wave 101+ do AWS account 906286017800 suspended ~48h (defer 24-48h thêm); per `aws-observability-first.md` + `pre-mutation-state-check.md` không apply infra mutation trong suspended window.

---

## §1 Brainstorm

### Q1 Inside-out (dev liệt kê)

6 buckets thứ tự ship D → C → A → B + F parallel + META cross-cut:

- **D (thứ 1):** GAP-650 Part 1 — thesis chapter 1 competitor analysis + AI techniques scaffold + bibliography seed IEEE
- **C (thứ 2):** GAP-286 — email-only auth migration phased 2 tuần + deliverability smoke + landing FAQ
- **A (thứ 3):** GAP-297 — `BatchInvoiceGenerator` cron monthly + preview-modal + audit log + per-tenant sequence
- **B (thứ 4, sequential sau A merge):** GAP-293 — income aggregator endpoint + 3 KPI cards + 12-month chart + Solo simplified variant
- **F (parallel với D):** GAP-681 — rewrite `database-architecture-map.md` v1 → v2 (Vietnamese narrative + per-service table mapping + service data flow sequence diagrams + design principles + maturity assessment) — thesis Chapter 2 architecture prep
- **META (cross-bucket):** GAP-680 — file `.claude/rules/vn-localization-audit-checklist.md` v1.0.0

A+B sequential cùng touch `kitehub-subscription`; D + C + F disjoint scope (`documents/08-thesis/` vs `kitehub-platform/auth/` vs `documents/02-architecture/`).

### Q2 Outside-in (3-audit consensus)

3 audit reports 2026-05-18 (persona simulation + failure-mode matrix + VN edu SaaS benchmark) consolidated findings — full chi tiết trong audit files cited frontmatter. Consensus scope adjustments áp dụng:

- **Persona simulation (5 adjustments):** preview-modal cho A (Solo persona requirement); Solo variant 1 KPI + sparkline cho B; migration docs `email-only-migration.md` cho C; expand depth Part 1 cho D (defer Part 2 = threat-to-validity + IEEE 15+ + cross-jurisdiction Wave 101); META VN-localization cross-bucket → GAP-680
- **Failure-mode matrix (Top 5 P0 risks + mitigation):** C5 OTP user lockout → phased deprecate 2 tuần; A4 SES Free Tier 200/day quota → batch cap 50/run + idempotency key + RabbitMQ spill-over; A1 invoice race → DB sequence per-tenant `invoice_seq_${tenantId}` SELECT FOR UPDATE; B2 RLS cross-tenant leak → force-fail NULL pattern (per Wave 85 Cat 1); D6 citation accuracy Part 1 → competitor source verify, defer Part 2 VN law accuracy
- **VN edu SaaS benchmark (5 adjustments):** A → eInvoice VAT integration hook prep (MISA partnership defer Wave 101+ per Wave 93 GAP-185 re-scope); B → per-class + per-branch breakdown; C → document email rationale + landing FAQ; D Part 2 → PDPL 2025 mention defer; data localization Phase 3 watch-item

---

## §2 State-Check Evidence

Per `audit-to-gap-pipeline.md` §2.6 wave-plan state-check:

| Symbol / Artifact | Status | Reference |
|---|---|---|
| `BatchInvoiceGenerator` service | 🆕 to-be-created | Bucket A scope |
| `invoice_batch_audit` table + migration `V61__invoice_batch_audit.sql` | 🆕 to-be-created | Bucket A scope |
| `invoice_seq_${tenantId}` per-tenant DB sequence | 🆕 to-be-created | Bucket A scope |
| `IncomeService` aggregator + `/api/v1/income/monthly` endpoint | 🆕 to-be-created | Bucket B scope |
| `kitehub-platform/auth/OtpService.java` `/otp/send` endpoint | ✅ exists, deprecate target | Bucket C scope |
| `documents/05-guides/operations/email-only-migration.md` | 🆕 to-be-created | Bucket C scope |
| `documents/08-thesis/chapter-1-competitor-analysis.md` + `chapter-1-ai-techniques.md` | 🆕 to-be-created | Bucket D scope |
| `.claude/rules/vn-localization-audit-checklist.md` | 🆕 to-be-created | META GAP-680 scope |
| `V60__immutable_admin_audit_logs.sql` (Wave 85) pattern reuse | ✅ exists | Bucket B audit log dependency |
| `documents/02-architecture/database-architecture-map.md` v1 (446 LOC, 9 sections) | ✅ exists (Wave 99B B3, GAP-672 closed) | Bucket F rewrite target — v1 → v2 |
| §10 Per-service Table Mapping + §11 Service Data Flow + §12 Design Principles + §13 Maturity Assessment | 🆕 to-be-created | Bucket F new sections |

---

## §3 Scope (per-bucket AC)

| Bucket | Gap | AC ngắn (full AC trong gap files) |
|---|---|---|
| **D** | GAP-650 Part 1 | (1) 2-3 files thesis chapter 1 competitor analysis + AI techniques shipped; (2) bibliography seed IEEE format ≥5 sources verified; (3) Part 2 explicit defer Wave 101 với gap link |
| **C** | GAP-286 | (1) BE deprecate `/otp/send` phased 2 tuần header `Deprecation:` + sunset date; (2) FE/mobile bump version + force re-verify email; (3) Gmail/Outlook deliverability smoke pre-launch PASS; (4) landing FAQ "Vì sao chỉ email?" published; (5) future mobile OTP Phase 2 documented note |
| **A** | GAP-297 | (1) `BatchInvoiceGenerator` + cron monthly + FE batch-invoice page; (2) preview-modal + dry-run mode; (3) audit log `invoice_batch_audit` + per-tenant sequence SELECT FOR UPDATE; (4) batch cap 50/run + idempotency + RabbitMQ spill-over; (5) eInvoice VAT integration hook prep stub |
| **B** | GAP-293 | (1) `/api/v1/income/monthly` aggregator + 3 KPI cards + 12-month bar chart + Solo simplified variant; (2) per-class + per-branch breakdown; (3) force-fail RLS NULL + admin cross-tenant audit log V60 pattern; (4) VND format + Vietnamese label + MoM/YoY delta |
| **F** (parallel D) | GAP-681 | (1) Rewrite v1 → v2 — narrative Vietnamese per `dev-readable-doc-language.md` §2 row Architecture docs (target ratio ≥40% Vietnamese từ baseline ~5-8%); (2) §10 Per-service Table Mapping (≥7 services × tables matrix + hot operations); (3) §11 Service Data Flow ≥5 Mermaid sequenceDiagram (login + trial→paid + tenant provision + class enrollment + email outbox) per `diagram-format-selection.md` §2.2; (4) §12 Database Design Principles (RLS rationale + type choice + FK convention + migration discipline + naming) với historical Wave/GAP cite; (5) §13 Maturity Assessment table + Wave 101+ roadmap (GAP-677 auto-gen + GAP-185 MISA schema + Phase 2 EKS) |
| **META** | GAP-680 | (1) rule `.claude/rules/vn-localization-audit-checklist.md` v1.0.0 với 4 sections (VND/date + Vietnamese label + VN sample data + VN cultural awareness Zalo/niên khóa/GVCN); (2) reviewer-checklist embed Wave 100 PR §1 Brainstorm Q2 + §3 per-bucket AC; (3) `rules-index.csv` row + `output-review-mandate.md` §3 matrix row paired same PR per `rule-change-process.md` §6.5 |

---

## §4 Execution Strategy

**Thứ tự ship D → C → A → B + F parallel → META (cross-cut):**
- D thứ 1 — disjoint scope, agent độc lập có thể start ngay không blocker
- C thứ 2 — disjoint với A/B, phased 2 tuần deprecate cần kick-off sớm cho deliverability smoke window
- A thứ 3 — `kitehub-subscription` service mutation lớn, ship trước B để B re-use migration framework
- B thứ 4 sequential sau A merge — cùng service, tránh conflict per `concurrent-production-mutation-ops.md` (same service deploy concurrency banned)
- F parallel với D — disjoint scope `documents/02-architecture/`, thesis Chapter 2 prep dependency cho Bucket D Part 2 Wave 101 narrative source

**Parallel-agent worktree mapping:** 4 worktree song song (D + C + A + F) tại `/tmp/wt-d`, `/tmp/wt-c`, `/tmp/wt-a`, `/tmp/wt-f` per Wave 99B 5x speedup pattern. B kicked-off sau A merge confirm (sequential constraint). META GAP-680 cross-cut: file rule body trong Bucket D's PR (lightweight, 0.5 ngày) để land sớm + reviewer-checklist available cho A/B/F PRs.

**Wall-clock estimate:** ~3.5 tuần total (D ~3-4 ngày || C ~3-4 ngày || A ~5-7 ngày || F ~3-4 ngày; B ~4-5 ngày sequential; META ~0.5 ngày embedded D).

---

## §5 Audit Refresh Schedule

Per `post-wave-audit-mandate.md` ≤3 ngày after Wave 100 close:

- **Quality audit /110** — refresh expected (last 90/110 B+ Wave 98 2026-05-19); delta from Bucket A/B business logic depth + META rule landing
- **Business Logic audit /100** — refresh required (last 73/100 C+ PARTIAL FAIL Cat 1 Wave 98); path to 80 PASS via Bucket A `invoice_batch_audit` + Bucket B aggregator code ↔ rules.md verify

**Deferred (per usual cadence):** Performance (last 86/100 Wave 85, refresh Wave 102+), Security (last 93/100 v2 Wave 94c, refresh Wave 102+), UI (last 110.6/128 Wave 98, refresh Wave 102+), Ops Readiness (last 77/100 Wave 94c, blocked by AWS restore GAP-612).
