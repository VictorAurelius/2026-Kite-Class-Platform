---
id: GAP-687
phase: phase-1-beta
status: PARTIAL
priority: P1
domain: Meta
audience: dev
---

# GAP-687: Thesis V1 draft DOCX audit follow-ups (60/100 D+ baseline)

**Status:** 🟡 PARTIAL (67% — Phase 1+2 DONE Wave thesis-1 Bucket D 2026-05-23; Phase 3 DEFER Wave thesis-2 chờ GAP-648 + GAP-649 unblock)
**Priority:** 🟠 P1 (META)
**Domain:** Meta — Thesis output review governance
**Found:** 2026-05-19 (Wave 101 closure self-audit of PR #1606 thesis-v1-draft.docx)
**Affects:** `documents/08-thesis/thesis-v1-draft.docx` + ThesisReportBuilder pipeline + `output-review-mandate.md` §3 (missing thesis review row)

## Problem

`documents/04-quality/audits/persona-review/2026-05-19-thesis-v1-draft-docx-audit.md` scored 60/100 D+ on the shipped pandoc-generated DOCX (PR #1606 commit fe9dd96c). Specific weaknesses block production V1 thesis state:

- **C1 Format vs khung-bao-cao-do-an.png VN spec (3/15):** Pandoc default ≠ A4/TNR/3-2-2-3cm/cover. Production V1 requires ThesisReportBuilder `--execute` mode (GAP-646 explicit deferral)
- **C5 Cross-ref integrity (13/20):** 0 figure cross-refs (no figure injection per pandoc draft); only 2 Bảng + 1 Table
- **C6 Examiner readiness (10/20):** 22 TODO + 5 placeholder strings; failure-mode B1 (no figures) + A2/A4 (no NFR) + B2 (no beta) + E2 (no cover page) flagged
- **Meta gap:** NO formal review rule `.claude/rules/thesis-content-standard.md` existed at ship time

## Proposed Fix — 3 phases

### Phase 1 — Immediate (Wave 102+)

1. **Strip pandoc draft from main OR rename clearly:** rename `thesis-v1-draft.docx` → `thesis-v1-draft-pandoc-quickconvert.docx` to clearly signal non-production state.
2. **Scrub 22 TODO + 5 placeholder strings** in source MDs (chapter-1-*/chapter-2-*/chapter-3-*/chapter-4-*). Either complete content OR replace with proper [PLACEHOLDER: <description>] explicit notation.
3. **File new meta rule `.claude/rules/thesis-content-standard.md`** codifying 6-category audit rubric used in 2026-05-19 audit. Per `incident-to-rule-pipeline.md` 5-stage; pair with `output-review-mandate.md` §3 matrix row "Thesis report / academic deliverable".

### Phase 2 — Format compliance (Wave 105+ defense prep window)

4. **ThesisReportBuilder `--execute` mode** (deferred per GAP-646 closure) — Spring Boot CLI runner + MD parser + figure injection + bibliography IEEE auto-format. ~6-8h focused session.
5. **Figure curation pipeline** — GAP-651 paired with --execute mode for `Hình N.M` cross-ref numbering + caption injection.
6. **Cover page generator** — add to ThesisReportBuilder; VN academic format with school/faculty/title/student/year fields.

### Phase 3 — Content evidence (Wave 102-110)

7. **Real NFR data** — GAP-648 k6 load test + CloudWatch screenshots + AWS Cost CSV → Ch.4 KPI section
8. **Beta cohort evidence** — GAP-649 ≥4 signed reviews → Ch.6 results discussion section
9. **Ch.5/6/7 content** — testing/discussion/conclusion narrative per release-1.5-thesis-scope §6 chapter mapping

## Acceptance Criteria

- [x] Phase 1 #1 — pandoc draft superseded by `documents/08-thesis/thesis-v1.docx` (Python pipeline via `create_thesis_v1.py`); 4 stale backup MD files archived to `documents/07-archived/thesis-drafts-2026-05-20-backup/` (Wave thesis-1 Bucket D)
- [x] Phase 1 #2 — TODO scrub complete; `chapter-mapping.md` 2 TODO refs updated; chapter MD body 0 TODO/FIXME/[TBD] markers (verified via grep)
- [x] Phase 1 #3 — `.claude/rules/thesis-content-standard.md` v1.1.0 filed + `output-review-mandate.md` §3 row added (Wave 102.7.0 META)
- [x] Phase 2 cluster — `create_thesis_v1.py --execute` production mode shipped (Wave thesis-1 Bucket D 2026-05-23 — 3 flags: `--execute`, `--dry-run`, `--validate-rubric`); GAP-651 figure curation skill DONE Wave thesis-1 Bucket B; GAP-646 closure DONE Wave 102 (Python pipeline pivot)
- [ ] Phase 3 cluster — DEFER Wave thesis-2: track via existing GAP-648 (NFR data) + GAP-649 (beta cohort); Ch.5-7 new gap filed for content gap (Wave thesis-2 scope per Log 2026-05-23)

## Related

- Audit report: `documents/04-quality/audits/persona-review/2026-05-19-thesis-v1-draft-docx-audit.md`
- Thesis DOCX PR: #1606 (merged commit fe9dd96c)
- Sister gaps: GAP-646 (DOCX pipeline DONE; `--execute` defer), GAP-647 (bibliography PARTIAL 80%), GAP-648 (NFR data), GAP-649 (beta cohort), GAP-650 (Ch.1 DONE), GAP-651 (image curation OPEN), GAP-653 (defense deck OPEN)
- Scope: `documents/03-planning/roadmap/release-1.5-thesis-scope.md`
- Defense window: 2026-08-15 → 2026-10-15
- Rule applied at audit: ad-hoc (no formal rule existed); meta gap surfaced this audit

## Log

- **2026-05-19 (created):** Filed post Wave 101 closure audit thesis-v1-draft.docx 60/100 D+ baseline.
- **2026-05-23 (Wave thesis-1 Bucket D — Phase 1+2 SHIPPED):** Status flipped OPEN → PARTIAL 67%. Phase 1+2 active scope DONE:
  - **Phase 1:** Backup files archived (4 chapter MD backups → `documents/07-archived/thesis-drafts-2026-05-20-backup/`); TODO/draft markers scrubbed (chapter-mapping.md 2 refs cleaned; chapter MD body verified 0 markers via grep).
  - **Phase 2:** `create_thesis_v1.py` extended với 3 flags (`--execute` production mode, `--dry-run` parse-only, `--validate-rubric` heuristic 9-category scoring per `thesis-content-standard.md` v1.1.0). Smoke test PASS — re-bake `thesis-v1.docx` 4 sections / 646 paragraphs / 27 figures / 26 tables / 38 bibliography entries / 100% cite utilization. Rubric heuristic 76/100 PARTIAL C (above ≥75 PASS minimum per `thesis-content-standard.md` §1; below ≥85 bucket target — defers Wave thesis-2).
  - **Audit artifact:** `documents/04-quality/audits/persona-review/2026-05-23-wave-thesis-1-bucket-d-docx-rubric.md`
  - **Sign-off:** `documents/08-thesis/SIGNOFF.md`
  - **Phase 3 DEFER Wave thesis-2:** Ch.5-7 content evidence (#7 NFR data + #8 beta cohort evidence + #9 chapter narrative) phụ thuộc GAP-648 + GAP-649 production data. Trigger restart Phase 3: GAP-648 + GAP-649 cả 2 DONE.
