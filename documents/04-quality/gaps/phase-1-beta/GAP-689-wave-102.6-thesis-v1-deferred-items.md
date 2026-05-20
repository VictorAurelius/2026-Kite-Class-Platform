# GAP-689: Wave 102.6 thesis V1 deferred items (Bucket F pre-thesis + P1/P2 audit remaining)

**Status:** 🔵 OPEN
**Priority:** 🟠 P1
**Domain:** Meta (thesis academic deliverable)
**Found:** 2026-05-20 (Wave 102.5 closure)
**Affects:** Thesis V1 defense readiness August 2026 (UTC University, ngành CNTT)

---

## Problem

Wave 102.5 closed 2026-05-20 với scope bundle 11 user-flagged items + 19 khung-chuẩn audit gaps (9 P0 + 10 P1). Per `wave-closure-scope-completeness.md` §3, **11 audit gaps remaining** defer Wave 102.6:

### Bucket F — Pre-thesis deliverables + post-thesis polish (4 gaps)

Per khung-chuẩn audit `documents/04-quality/audits/persona-review/2026-05-20-wave-102.5-khung-chuan-audit.md` §4 Bucket F:

| Gap | Severity | Scope |
|---|---|---|
| **G23** | P2 | Verify đề cương đã nộp + ký với GVHD chưa. Nếu chưa → ship `documents/08-thesis/de-cuong-datn-kitehub.docx` (template từ `documents/07-archived/academic/word-reports/de-cuong-datn/create_de_cuong_datn.py`) cho GVHD ký duyệt. |
| **G24** | P2 | Document quy trình in ấn + đóng quyển `documents/08-thesis/README.md` §"Quy trình in ấn + đóng quyển": format gáy `NGUYỄN VĂN KIỆT - CNTT1-K63 - 2026` + chữ ký GVHD vị trí bìa phụ + 3 phiên bản (DOCX source / PDF final / bản in giấy bìa cứng). |
| **G27** | P2 | Phụ lục expand 4-5 sub-section thực sự: A. Sample data tenant lifecycle (10 rows) + B. Sample DDL schema (3 entity) + C. Sample API contract (1 endpoint full JSON) + D. Sample audit log entry (3-5 dòng) + E. Sample bibliography excerpt (10 refs). Update `add_appendix()` line 1366+ pipeline. |
| **G29** | P2 | Add `add_advisor_review_page()` + (optional) `add_examiner_review_page()` cho "NHẬN XÉT CỦA GIÁO VIÊN HƯỚNG DẪN" block sau Phụ lục. Verify với GVHD requirement before implement. |

### Bucket A deferred from Wave 102.5 (4 gaps)

PR #1628 (Bucket A) ship 13/16 scope items; 3 deferred per agent report:

| Gap | Severity | Scope |
|---|---|---|
| **G4** | P0 | Direct quote `[N, tr.NNN]` page-num — sweep mọi `"..."` direct quote trong Ch.2 + Ch.4 body. Mỗi quote cite page-num đầy đủ HOẶC convert paraphrase. PR #1628 deferred to chapter content scope (Bucket D/E partially handled). Verify post-render coverage. |
| **G5** | P0 | UTC giáo trình refs add to bibliography — minimum 2 refs (UTC giáo trình CSDL + Công nghệ phần mềm). PR #1628 deferred per scope notes "avoid orphan creation". |
| **G6** | P0 | TOC F9 populate post-render. PR #1628 noted "LibreOffice unavailable in agent env — post-render user task". User runs `libreoffice --headless --convert-to docx thesis-v1.docx` OR opens in Word + F9 to refresh TOC fields before defense submit. |
| **G9** | P0 | `*Nguồn:* italic line` cite cho derived figures/tables Ch.1 + Ch.2 + Ch.4. PR #1628 deferred to chapter content scope. Sweep ~25 figures: identify derived vs author-original; add cite cho derived. |

### Audit P1/P2 remaining (7 gaps)

Per audit §4 cross-reference table:

| Gap | Severity | Scope |
|---|---|---|
| **G12** | P1 | Ch.2 functional + non-functional → ✅ ĐÃ CÓ (informational; NO ACTION required) |
| **G20** | P1 | Bìa chính không cần quốc hiệu CHXHCN — ✅ đã đúng (informational; NO ACTION) |
| **G21** | P1 | Final page count measurement post-Wave-102.5 render. Target ≤80 trang. Measure via LibreOffice headless OR Word page count. Nếu >80 → trim Ch.1 competitor (giảm 5→4) hoặc Ch.4 §4.3 KPI section. |
| **G22** | P1 | Screenshot source caption convention — partially covered Bucket C BeeClass + competitor PR #1630. Verify mọi screenshot có `*Nguồn: <URL>, truy cập <date>*` cite per UTC §2.4. |
| **G26** | P2 | Convert ~25% bullet → prose Ch.1-Ch.4. PR shipped bullet-heavy ratio ~50%; target <40%. Sample 3-5 sections per chapter convert. |
| **G28** | P2 | TT 8.1 verify với GVHD — informational, không block defense. |
| **G30** | P2 | Character spacing edge case (long English technical token like `SubscriptionOutboxDispatcher`) trong justified text. Polish defer. |

---

## Root Cause

Wave 102.5 user AskUserQuestion 2026-05-20 chốt scope 19 gaps (9 P0 + 10 P1) cho cost-benefit. 11 remaining gaps (4 Bucket F + 4 A-deferred + 7 P1/P2) deferred Wave 102.6 per scope discipline (`wave-closure-scope-completeness.md` §3 reconciliation).

---

## Proposed Fix

### Phase 1 (P0 chapter-content sweep — ship trong Wave 102.6 Bucket A')

1. **G4 direct quote sweep:** Cross-chapter grep `"[^"]+"` Vietnamese direct quotes Ch.2 + Ch.4 → verify each cites `[N, tr.NNN]`. Convert paraphrase nếu thiếu page-num source.
2. **G9 derived figure source cite:** Sweep ~25 figures/tables Ch.1 + Ch.2 + Ch.4 → add `*Nguồn: [N] hoặc URL*` italic cho derived. Author-original không cần cite.

### Phase 2 (P0 bibliography polish — ship trong Wave 102.6 Bucket B')

3. **G5 UTC giáo trình refs:** Add [42] UTC giáo trình CSDL + [43] UTC giáo trình Công nghệ phần mềm (verify ISBN từ thư viện UTC).
4. **G6 TOC F9:** Document `documents/08-thesis/README.md` post-render task: user runs `libreoffice --headless --convert-to pdf thesis-v1.docx` HOẶC opens in Word + Ctrl+A + F9 → save → ship to GVHD.

### Phase 3 (Bucket F pre-thesis deliverables — ship Wave 102.7 nếu cần)

5. **G23 đề cương:** Adapt `create_de_cuong_datn.py` template → render `de-cuong-datn-kitehub.docx` + in + ký với GVHD.
6. **G24 in ấn quy trình:** Document `documents/08-thesis/README.md` §"In ấn + đóng quyển".
7. **G27 Phụ lục expand:** Update `add_appendix()` với 5 sub-section thực sự.
8. **G29 NHẬN XÉT page:** Add `add_advisor_review_page()` post Phụ lục.

### Phase 4 (Polish — ship Wave 102.8 pre-defense)

9. **G21 page count measure:** Render + measure; trim nếu >80.
10. **G22 screenshot source verify:** Post-screenshot capture follow-up.
11. **G26 bullet→prose:** Sample 3-5 sections per chapter convert.

---

## Acceptance Criteria

- [ ] **Phase 1 ship (Wave 102.6 Bucket A'):** G4 + G9 swept; verify grep `grep -rE "Hình [0-9]+\.[0-9]+\." documents/08-thesis/chapter-*.md` shows source cite cho derived figures
- [ ] **Phase 2 ship:** G5 [42]+[43] UTC giáo trình added; G6 post-render TOC F9 instruction in README
- [ ] **Phase 3 ship (Wave 102.7):** G23 đề cương docx + GVHD ký; G24 README quy trình in ấn; G27 Phụ lục expand; G29 NHẬN XÉT page (after GVHD requirement verify)
- [ ] **Phase 4 ship (Wave 102.8 pre-defense):** G21 page count ≤80; G22 screenshot cite verify; G26 bullet ratio <40%
- [ ] Closure verify: rubric v2 9-category score ≥95/100 A maintained post all Phases

---

## Related

- Wave 102.5 plan: `documents/03-planning/waves/wave-2026-05-20-102.5-thesis-v1-fix-bundle.md`
- Khung-chuẩn audit: `documents/04-quality/audits/persona-review/2026-05-20-wave-102.5-khung-chuan-audit.md`
- Parent GAP: GAP-688 (Wave 102 thesis V1 pipeline pivot — DONE)
- Sister gaps: GAP-687 (Wave 101 V1 draft audit follow-ups), GAP-651 (Mermaid PNG figures)
- Rule: `wave-closure-scope-completeness.md` §3 reconciliation mandate
- Pre-defense window: August 2026 (UTC defense)

---

## Log

- **2026-05-20:** GAP filed via Wave 102.5 closure protocol. 11 deferred audit gaps grouped into 4 phases for Wave 102.6 → 102.8 follow-up. P0 priority items (G4/G5/G6/G9) ship Phase 1+2 (Wave 102.6); P2 Bucket F items ship Phase 3 (Wave 102.7); polish ship Phase 4 (Wave 102.8 pre-defense).
