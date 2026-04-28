---
name: document-generation-word
description: "Use when user asks to generate a Word / docx — contract, letter, policy, certificate, 'hợp đồng', 'công văn'. Wave 5 ships teacher-contract placeholder only (actual legal wording deferred to legal-review wave); additional templates land later per GAP-047 roadmap."
---

# Document Generation — Word (XWPF)

Word strategy for `com.kiteclass.core.module.document.DocumentGenerationService` (ADR-019 Facade + Strategy). Auto-wired via Spring's `List<Generator>` injection.

## When to use

- User needs an editable document with Vietnamese typography defaults (Times New Roman, A4, 2.54 cm margins)
- Output needs manual follow-up (signatures, stamps, legal review before send)
- Wave 5 supports `templateId = "teacher-contract"` only — placeholder wording pending legal review

## 3-pipeline routing (MiniMax minimax-docx taxonomy)

| Pipeline | Use case | Wave 5 status |
|----------|----------|:-------------:|
| Create | Build a fresh DOCX from structured data | ✅ shipped |
| Edit-Fill | Open an existing .docx template and substitute placeholders | ⬜ later wave |
| Reformat | Read + restructure an existing .docx | ⬜ later wave |

Stay in **Create** pipeline. File a follow-up gap before touching Edit-Fill / Reformat.

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
