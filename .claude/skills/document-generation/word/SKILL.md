---
name: document-generation-word
description: "Use when user asks to generate a Word / docx — contract, letter, policy, certificate, 'hợp đồng', 'công văn', 'khoá luận', thesis report. Wave 5 shipped teacher-contract; Wave 100.7 Phase 3b shipped thesis-report (Create pipeline). Additional templates land per GAP-047 / GAP-208 roadmap."
---

# Document Generation — Word (XWPF)

Word strategy for `com.kiteclass.core.module.document.DocumentGenerationService` (ADR-019 Facade + Strategy). Auto-wired via Spring's `List<Generator>` injection.

## When to use

- User needs an editable document with Vietnamese typography defaults (Times New Roman, A4)
- Output needs manual follow-up (signatures, stamps, legal review, supervisor markup before submit)
- Supported templateIds:
  - `teacher-contract` (Wave 5) — 2.54 cm symmetric margins; placeholder legal wording
  - `thesis-report` (Wave 100.7 Phase 3b) — asymmetric 3-2-2-3 cm margins for binding gutter; 7-chapter skeleton + bibliography + appendix; 13pt body / 14pt heading per HUST/UIT/UET norm

## 3-pipeline routing (MiniMax minimax-docx taxonomy)

| Pipeline | Use case | Status |
|----------|----------|:-------------:|
| Create | Build a fresh DOCX from structured data | ✅ shipped — `teacher-contract`, `thesis-report` |
| Edit-Fill | Open an existing .docx template and substitute placeholders | ⬜ later wave (scoped via `documents/08-thesis/docx-pipeline-scoping.md`; not adopted V1 since LibreOffice not on-stack) |
| Reformat | Read + restructure an existing .docx | ⬜ later wave |

Stay in **Create** pipeline. File a follow-up gap before touching Edit-Fill / Reformat.

## Thesis pipeline (Wave 100.7 Phase 3b)

`thesis-report` templateId → `ThesisReportBuilder`. Programmatic skeleton build via POI XWPF — same Create pattern as `TeacherContractBuilder`, scaled to VN academic thesis norms.

**Required data keys:**
- `title` — thesis title (VN)
- `studentName` — full student name
- `studentId` — MSSV
- `supervisor` — GVHD with title (e.g., "TS. Trần Thị Hồng")
- `year` — academic year
- `school` — school/department (rendered uppercase on cover)

**Optional data keys (for content injection by assembly layer):**
- `chapter.N.title` — override default chapter heading (N = 1..7)
- `chapter.N.body` — chapter body text block (V1 plain text; V2 will accept MD-parsed XWPF runs)
- `bibliography.entries` — bibliography text block (V1 plain text; V2 will format from `bibliography.md` IEEE refs structured data)

**Skeleton structure (V1):**
1. Cover page (school + "KHOÁ LUẬN TỐT NGHIỆP" + title + student info + supervisor + year)
2. TOC stub (`[Mục lục]` placeholder — Word auto-TOC field code deferred V2)
3. 7 chapter shells (default titles match Wave 100.7 `chapter-mapping.md`; body placeholder when no `chapter.N.body` provided)
4. Bibliography section (`TÀI LIỆU THAM KHẢO` header + body)
5. Appendix section (`PHỤ LỤC` header + placeholder)

**VN academic norms applied:**
- Page: A4 portrait (11906 × 16838 twips)
- Margins: 3 cm left (1701 twips — binding gutter) + 2 cm top/right/bottom (1134 twips)
- Font: Times New Roman 13pt body / 14pt heading / 18pt cover title (all bold cover; non-bold body)

**Validation script:** `scripts/assemble-thesis-docx.sh` (dry-run mode) verifies pipeline pre-conditions. Production `--execute` mode (full chapter MD parsing + figure injection + bibliography auto-format from `bibliography.md`) deferred to follow-up gap requiring Spring Boot CLI runner scope.

**Tests:** `kiteclass-core/src/test/java/.../docx/ThesisReportBuilderTest.java` (17 tests covering cover render, chapter shells, bibliography injection, A4 + binding-gutter margins, VN diacritics, soft-cap perf).

## How it works

1. Caller hands `DocumentRequest(format=DOCX, templateId, tenantId, data)` to facade.
2. `DocxGenerator` routes by templateId; `teacher-contract` → `TeacherContractBuilder`.
3. Builder constructs a fresh `XWPFDocument`:
   - Applies A4 page size + 2.54 cm margins via `CTSectPr` / `CTPageSz` / `CTPageMar`.
   - Headings: "CỘNG HÒA XÃ HỘI CHỦ NGHĨA VIỆT NAM" → "Độc lập - Tự do - Hạnh phúc" → bold 14 pt "HỢP ĐỒNG GIẢNG DẠY".
   - Body paragraphs (Times New Roman 12 pt justified): party declarations, 4 contract terms, signature block with two tabbed columns (Bên A / Bên B).
   - Salary formatted with `vi-VN` thousand separator (`15.000.000`).
4. Returns `DocumentResponse(bytes, docx MIME, "teacher-contract-<slug>-<startDate>.docx")`.

## Gotchas

- **Font:** Times New Roman is the de-facto default for VN legal documents. `Arial` / `Calibri` look foreign in official contracts.
- **Page setup:** A4 portrait (11906 × 16838 twips), 2.54 cm margins (1440 twips). These are the norms on Vietnamese official letterhead; deviating raises eyebrows.
- **Diacritics:** XWPF handles Unicode natively — no font substitution needed. Just pass through.
- **Long composite names:** lines like `Nguyễn Phạm Hồng Ánh Tuấn` can overflow a single-line signature slot; wrap with justified paragraph alignment (not left).
- **Placeholder legal wording:** Wave 5 contract text is NOT production-ready — legal review is deferred. Clearly label placeholder sections in rendered output (e.g., "[sẽ được pháp lý duyệt ở wave sau]").
- **Tables vs tabs:** for quick 2-column layouts (signature block) tabs are good enough; for complex multi-row layouts use `XWPFTable` (not in scope this wave).

## Reference

- `reference/docx-3-pipelines.md` — detailed pipeline descriptions + XSD validation approach

## Out of scope (Wave 5)

- Branding-package integration (Sub-PR 5.5 tints title + injects logo image from ADR-009 API).
- HTTP endpoints (Sub-PR 5.5 — download only, no preview).
- Legal-reviewed contract wording (separate legal-review wave).
- Templates beyond `teacher-contract` (GAP-208, Wave 7).
- Edit-Fill and Reformat pipelines.
- Async queue (GAP-210).
- XWPF macros, embedded OLE objects, revision tracking.
