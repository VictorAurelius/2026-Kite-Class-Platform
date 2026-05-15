# GAP-563: META — User manual content review standard (15-item checklist + persona matrix + discoverability gates)

**Status:** 🔵 OPEN
**Priority:** 🟠 P1 (META P0 force-multiplier per `.claude/rules/meta-gap-priority.md` §3 — meta gap precedes feature gap GAP-537; without standard, F1 sample ship lệch và rework Wave 80+)
**Domain:** Meta (Rules/Skills/Docs governance)
**Found:** 2026-05-14 (Wave 79 Bucket F1 outside-in audit — `documents/04-quality/audits/persona-review/2026-05-14-pre-wave-79-bucket-f1-user-manual-outside-in.md`)
**Affects:** All user manual pages (GAP-537 + future Wave 80+ persona expansions), tenant-facing help content discoverability, support burden reduction Phase 1 BETA
**Phase:** phase-1-beta

## Current State (verified 2026-05-14)

| Piece | File / Path | Status |
|---|---|---|
| User manual content standard | `.claude/rules/user-manual-content-standard.md` OR `documents/05-guides/user-manual/README.md` | ❌ missing — `find .claude/rules/ -iname "*user-manual*"` → 0; `find documents/05-guides/user-manual` → 0 |
| `output-review-mandate.md` §3 row "User manual pages" | `.claude/rules/output-review-mandate.md` | ❌ missing — `grep -i "user manual pages" .claude/rules/output-review-mandate.md` → 0 |
| GAP-537 scope refinement (cite GAP-563 standard) | `documents/04-quality/gaps/GAP-537-*.md` | ⚠️ vague — current scope nói "5-10 screenshots per persona" thiếu format/discoverability/checklist |
| Outside-in audit report | `documents/04-quality/audits/persona-review/2026-05-14-pre-wave-79-bucket-f1-user-manual-outside-in.md` | ✅ shipped (this PR sibling) |

## Problem

Wave 79 Bucket F1 sample user manual sắp ship (per Wave 79 plan §3 row 6 + outside-in audit recommendation). GAP-537 scope hiện tại nói **"5-10 screenshots per persona × 4 personas"** — thiếu:

1. **Format diversity** — chỉ screenshot không đủ; cần web page + PDF + annotated screenshot
2. **Persona-specific landing** — chưa rõ `/help/p2-owner` vs `/help/p3-manager` separate hay merge
3. **Discoverability gates** — Hằng/Tâm/Vy stuck → 90% bottleneck là "không tìm được manual"; cần header "?" + footer link + onboarding CTA
4. **Cognitive load TL;DR pattern** — manual đọc khi stuck cần TL;DR top + 5-bullet, không wall-of-text
5. **VN edu context** — sample data VN + currency VND + date format VN + Zalo share + A4 print PDF
6. **Trust gates** — last-updated badge + support footer + version sync
7. **Accessibility** — WCAG AA, mobile responsive, screen reader compatible
8. **Search functional** — Fuse.js v1 hoặc Algolia v1.5+
9. **Vietnamese narrative mandate** per `.claude/rules/dev-readable-doc-language.md` cho end-user docs scope

Nếu F1 sample ship **TRƯỚC** khi standard tồn tại → ship lệch → Wave 80+ expand (P2 Owner + P3 Manager) phải rework retroactively. Cost compound nhanh: 1 lệch F1 → 4 personas × 5-10 pages rework.

Per `.claude/rules/meta-gap-priority.md` §3, meta gap về **review standard** ưu tiên HƠN feature gap GAP-537 — fix standard 1 lần ship Wave 79, force-multiplier mọi page user manual subsequent.

## Root Cause

Wave 78 inside-out brainstorm + Wave 79 plan §3 Bucket F không có outside-in audit cho user manual SPECIFICALLY về format/media/discoverability. Outside-in audit pre-Wave-79 (`2026-05-14-pre-wave-79-outside-in.md`) cover content gaps (cookie consent, RBAC, invite-staff) nhưng KHÔNG cover user manual format. Audit này (Bucket F1 outside-in) là attempt close that gap PRE F1 spawn per `outside-in-coverage-trigger.md` Bước 4 mandate.

## Proposed Fix

### Option A (recommended): Ship as `.claude/rules/user-manual-content-standard.md` rule file

Lý do: per `rule-change-process.md` §6.5 Enforcement Parity Mandate, rule format có built-in enforcement (CI rule frontmatter + reviewer-checklist + memory auto-load + path-scope). Standard cần force-multiplier governance, không chỉ doc.

Ship same PR với Wave 79 Bucket F1:

1. **Rule file:** `.claude/rules/user-manual-content-standard.md` v1.0.0 với:
   - 15-item checklist mandatory per page (xem §"Acceptance Criteria" dưới)
   - Persona discoverability matrix (4 persona × 3 entry points)
   - Format diversity requirement (web + PDF + annotated screenshot)
   - VN edu context mandate (sample VN + VND + date VN + Zalo + A4)
   - Cross-reference với `dev-readable-doc-language.md` §2 row "End-user docs"
   - Cross-reference với `output-review-mandate.md` §3 (add new row)
   - SaaS benchmark cited (Intercom + Stripe + Linear + Notion + Loom + Misa + KiteOS)

2. **Output review mandate matrix row:** add `output-review-mandate.md` §3:
   | Output Type | Review Standard | Process | Reviewer |
   |---|---|---|---|
   | **User manual pages** | GAP-563 15-item checklist + WCAG AA + Vietnamese narrative | Per-page pre-merge | Author + UI reviewer + 1 native VN reader |

3. **Rules-index.csv row:** add `user-manual-content-standard,MANDATORY,1.0.0,2026-05-14,2026-05-14,user-manual-content-standard.md`

4. **Worked self-test:** Bucket F1 anonymous-prospect 5-page prototype demonstrably pass 15-item checklist (per `rule-change-process.md` §6.5 Stage 4 self-test)

### Option B: Ship as `documents/05-guides/user-manual/README.md` standard doc

Lý do counter: nhẹ hơn, ship cùng F1 sample folder. Counter-counter: không có CI enforcement, dễ drift; xem `output-review-mandate.md` §4 historical VIOLATIONS với standards lacking enforcement.

**Recommendation: Option A** (rule file với enforcement parity).

### GAP-537 scope refinement (same PR)

Update GAP-537 §Proposed Fix:
- Bucket F1 (Wave 79): anonymous-prospect persona 5-page prototype + apply GAP-563 standard
- Bucket F2 (Wave 80): P2 Owner persona 10 pages
- Bucket F3 (Wave 80): P3 Manager persona 5-7 pages
- Bucket F4 (Wave 81+): videos + marketing brochure + comparison page

## Acceptance Criteria

15-item checklist trong rule file (cite outside-in audit report):

- [ ] Rule file `.claude/rules/user-manual-content-standard.md` v1.0.0 ship same PR với Wave 79 Bucket F1
- [ ] Rule §2 mandatory frontmatter per page: `persona`, `topic`, `last-updated`, `version` (app version), `effort_minutes`
- [ ] Rule §3 TL;DR box pattern (1 sentence + 3-5 bullet steps) đầu mỗi page
- [ ] Rule §4 persona-specific landing structure (`/help/{persona-slug}` separate, không merge)
- [ ] Rule §5 screenshot annotation requirement (mũi tên đỏ + viền vàng + số bước, không screenshot trần)
- [ ] Rule §6 VN edu sample data mandate ("Trung tâm Anh ngữ Sky Education", "Trần Thị Hồng", không Lorem Ipsum/English placeholder)
- [ ] Rule §7 currency VND + date format VN ("Thứ Hai, 14/05/2026") mandate
- [ ] Rule §8 Vietnamese narrative cross-reference `dev-readable-doc-language.md` §2 row "End-user docs"
- [ ] Rule §9 support footer mandate mỗi page (`support@kitehub.me` + Zalo + "Báo lỗi" mailto)
- [ ] Rule §10 last-updated badge + auto-update from git log
- [ ] Rule §11 print-friendly CSS `@media print` (A4 portrait, Times New Roman, no nav/sidebar)
- [ ] Rule §12 mobile responsive ≥360px viewport
- [ ] Rule §13 WCAG AA (heading hierarchy + alt text + contrast ≥4.5:1)
- [ ] Rule §14 discoverability matrix per persona (header "?" + footer link + onboarding CTA — ≥3 entry points)
- [ ] Rule §15 search functional (Fuse.js v1 / Algolia v1.5+)
- [ ] `output-review-mandate.md` §3 row "User manual pages" added
- [ ] `rules-index.csv` row added
- [ ] Worked self-test: Bucket F1 anonymous-prospect 5 pages demonstrably pass 15-item checklist
- [ ] GAP-537 scope refined to cite GAP-563 standard (no longer vague "5-10 screenshots")
- [ ] Cross-link added trong related rules: `dev-readable-doc-language.md`, `output-review-mandate.md`, `meta-gap-priority.md`

## Related

- Outside-in audit: `documents/04-quality/audits/persona-review/2026-05-14-pre-wave-79-bucket-f1-user-manual-outside-in.md`
- Parent gap: GAP-537 (user manual Vietnamese screenshots-based, scope refinement target)
- Sister outside-in audit: `documents/04-quality/audits/persona-review/2026-05-14-pre-wave-79-outside-in.md` (content gaps)
- Rule: `.claude/rules/outside-in-coverage-trigger.md` (mandates outside-in audit pre-F1 spawn)
- Rule: `.claude/rules/meta-gap-priority.md` §3 (META gap force-multiplier > feature gap)
- Rule: `.claude/rules/dev-readable-doc-language.md` §2 (Vietnamese end-user doc narrative)
- Rule: `.claude/rules/output-review-mandate.md` §3 (add row "User manual pages")
- Rule: `.claude/rules/rule-change-process.md` §6.5 Enforcement Parity Mandate
- Wave 79 plan: `documents/03-planning/waves/wave-2026-05-14-79-beta-invite-close-out.md` §3 row 6 Bucket F
- SaaS benchmarks (public refs): Intercom Articles, Stripe Docs, Linear Method, Notion Help, Loom
- VN edu benchmarks: Misa, KiteOS, Smile, KidsPay

## Log

- **2026-05-14:** DONE — Wave 79 Bucket F1 closure. user-manual-content-standard.md rule v1.0.0 shipped (META P1 force-multiplier). 15-item checklist + persona discoverability matrix + reviewer-checklist + worked self-test on F1 5-page anonymous sample. `output-review-mandate.md` §3 matrix row added + rules-index.csv row added. Per `incident-to-rule-pipeline.md` 5-stage applied (PR #1371).

- **2026-05-14:** Gap filed via Wave 79 Bucket F1 outside-in audit (Persona 1/2/3/4 walkthrough × 5 questions = format/media/discoverability/cognitive/VN/trust). Outside-in caught format+discoverability blind spot mà GAP-537 inside-out scope chỉ liệt kê "5-10 screenshots per persona" thiếu enforcement. META P0 force-multiplier per `meta-gap-priority.md` §3 — ship standard same PR với Bucket F1 sample để F1 demonstrably apply standard (avoid F2/F3 rework Wave 80+). Reviewer: @nguyenvankiet (solo-dev). Closing PR will demonstrate Wave 79 Bucket F1 5-page anonymous-prospect prototype passes 15-item checklist as self-test.
