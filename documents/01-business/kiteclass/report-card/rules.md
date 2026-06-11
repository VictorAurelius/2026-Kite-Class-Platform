# Report Card (Bảng điểm VN K-12) — Business Rules

**Domain:** report-card
**Source:** GAP-055
**Persona:** P5 K-12 School (primary), P9 International School
**Depends on:** GAP-047 (document generation infrastructure ✅), GAP-054 (multi-subject grade model ✅), GAP-059 (conduct/hạnh kiểm — deferred to Phase 2 follow-up)

## Scope

Phase 1 MVP — single-student per-semester PDF report card, MOE-style A4 layout. Phase 2 follow-up adds batch generation, real QR verification, real digital signature, and live conduct integration.

## Rules

### Aggregation (BR-RC-AGG)

| ID | Rule | Enforcement |
|----|------|-------------|
| BR-RC-AGG-001 | Report card scoped to (studentId, semesterId) tuple | Service signature `generateReportCard(studentId, semesterId)` |
| BR-RC-AGG-002 | Tenant isolation MANDATORY — student + semester must belong to current tenant | `TenantContext.getCurrentInstanceId()` filter on every repository call |
| BR-RC-AGG-003 | Source data: `SubjectGrade` rows for (studentId, semesterId) joined with `SubjectSection.courseId` → Course (subject name) | `SubjectGradeRepository.findByStudentIdAndSemesterIdAndDeletedFalse` |
| BR-RC-AGG-004 | Soft-deleted grades excluded | repo method explicit `AndDeletedFalse` |
| BR-RC-AGG-005 | Empty grade set → 404 with code `REPORT_CARD_NO_GRADES` | Service throws domain exception |
| BR-RC-AGG-006 | Per-subject row data: subject name, regularScore (TX), midtermScore (KT giữa kỳ), finalScore (KT cuối kỳ), average (TBM), letterGrade (Xếp loại) | from `SubjectGrade` entity |
| BR-RC-AGG-007 | Missing one of TX/KT/finalScore → render `—` (em-dash), do NOT zero-fill | Template-level null-safe rendering |
| BR-RC-AGG-008 | Overall semester average: simple arithmetic mean of subject averages (weighted-by-curriculum deferred to Phase 2) | Service computes |

### Layout (BR-RC-LAY) — MOE-style A4 portrait

| ID | Rule | Detail |
|----|------|--------|
| BR-RC-LAY-001 | Page size A4 portrait, 2cm margins | `@page` CSS in template |
| BR-RC-LAY-002 | Header: school logo + school name + academic year + semester label | `branding.logoUrl`, `branding.displayName` from `DocumentBrandingAssembler` |
| BR-RC-LAY-003 | Title: "BẢNG ĐIỂM HỌC KỲ {N}" (Semester 1/2) or "BẢNG ĐIỂM CẢ NĂM" (annual variant — Phase 2) | derived from `Semester.kind` |
| BR-RC-LAY-004 | Student info block: full name, class name (Lớp), date of birth, student ID | from Student + HomeroomClass |
| BR-RC-LAY-005 | Grade table columns: STT \| Môn học \| Điểm TX \| Điểm KT giữa kỳ \| Điểm KT cuối kỳ \| TBM \| Xếp loại | fixed column order |
| BR-RC-LAY-006 | Conduct (hạnh kiểm) row: render "Chưa cập nhật" if null (GAP-059 not yet shipped) | `conduct ?: 'Chưa cập nhật'` |
| BR-RC-LAY-007 | Summary block: overall average + overall letter grade + total subjects | computed by service |
| BR-RC-LAY-008 | Signature block: GVCN (homeroom teacher), Hiệu trưởng (principal), Phụ huynh (parent) — text-only placeholders for Phase 1 | 3 dotted lines + role label |
| BR-RC-LAY-009 | QR code area reserved (top-right, ~3cm × 3cm) — Phase 1 renders text "QR: <url>", Phase 2 wires zxing | placeholder div with class `report-card-qr` |
| BR-RC-LAY-010 | VN diacritic font: DejaVuSans (already bundled per Wave 5) | reuse `InvoiceRenderer.useFont` pattern |
| BR-RC-LAY-011 | Long composite student names → `word-break: break-word` on name cell | CSS rule in template |

### Branding integration (BR-RC-BRAND)

| ID | Rule | Detail |
|----|------|--------|
| BR-RC-BRAND-001 | Reuse `DocumentBrandingAssembler.enrich()` — no new assembler | depends on Wave 5 GAP-047 |
| BR-RC-BRAND-002 | Header band fill = `branding.primaryColor` (fallback `#1f2937` neutral if absent) | template `style` attribute |
| BR-RC-BRAND-003 | If `branding.logoUrl` null → render only school name centered (no broken image) | Thymeleaf `th:if` |

### Tier (BR-RC-TIER)

| ID | Rule | Detail |
|----|------|--------|
| BR-RC-TIER-001 | Single-card endpoint available all tiers (FREE+) | no quota gate |
| BR-RC-TIER-002 | Batch endpoint (Phase 2) gated BASIC+ | TBD when Phase 2 lands |

### Grading scale (reused from k12-model)

| Average TBM | Letter Grade (Xếp loại) |
|-------------|-------------------------|
| ≥ 8.0 | Giỏi |
| 6.5 - 7.99 | Khá |
| 5.0 - 6.49 | Trung bình |
| < 5.0 | Yếu |

(Per `BR-SG-004` in `k12-model/rules.md`. Report card consumes; does not redefine.)

## Out-of-scope (Phase 2 follow-up gap)

- Batch generation 30 cards per class (1-click)
- Real QR verification link (zxing dep + verification endpoint)
- Real digital signature (Bouncy Castle + PDFBox)
- Conduct (hạnh kiểm) live wiring (waits on GAP-059)
- Annual report card variant ("BẢNG ĐIỂM CẢ NĂM")
- Multi-year transcript variant ("Bảng điểm tổng hợp nhiều năm")
- Curriculum-weighted overall average

## Config keys

```yaml
# kiteclass-core/src/main/resources/application.yml (no new keys for Phase 1 — reuses GAP-047 doc-gen + branding)
```

## Five-attribute review per `business-logic-review.md`

Per-rule attributes (Source / Rationale / Reviewer / Compliance check / Review cadence) backfilled at file-level placeholder per Phase 1 of GAP-433. Per-rule granularity tracked via GAP-156 Phase 2 stakeholder sign-offs.

- **Source:** Existing rules in this file derive from a mix of: feature gaps cited inline (where present), ADRs, persona reviews, and informed-gut estimates from Wave 1-30 work. Rules without inline citation default to `informed gut` per `business-logic-review.md` §2.1 and inherit quarterly re-review obligation below.
- **Rationale:** Rule values reflect product judgment + (where applicable) competitor benchmarks + VN regulatory minimums. Detailed per-rule rationale to be backfilled during GAP-156 Phase 2 stakeholder review; until then, treat values as `informed gut` subject to next quarterly review.
- **Reviewer:** @nguyenvankiet (acting Product Owner, solo-dev, 2026-05-08). Formal stakeholder + legal counsel sign-off queued via GAP-156. Solo-dev exemption per `business-logic-review.md` §2.3 — the Reviewer line documents which hat is being worn AND obligation is attached for team-growth or pre-launch trigger.
- **Compliance check:** **Compliant** — Luật Giáo dục 2019 Đ.83 (report obligation); PDPL Decree 13/2023 Art 17 (parental access for under-16); MoET grade-reporting standard.
- **Review cadence:** Quarterly (default per `business-logic-review.md` §2.5). **Next review:** 2026-08-08. Event triggers: MoET report-card format update, parental-access law amendment.

## Log

- 2026-04-28 — Initial Phase 1 rules (GAP-055)
