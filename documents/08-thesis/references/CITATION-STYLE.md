# Citation Style: IEEE (chuẩn VN CS thesis 2026)

**Quyết định:** IEEE per GAP-647, phù hợp VN CS convention (UIT/HUST/UET) + computer science international norm.

## In-text citation patterns

| Pattern | Use case | Example |
|---|---|---|
| `[N]` | Single reference | "Spring Boot là framework phổ biến nhất cho Java backend trong VN [1]." |
| `[N, M]` | Multiple references | "Multi-tenant SaaS pattern được mô tả chi tiết trong [3, 7]." |
| `[N]–[M]` | Range (consecutive) | "PDPL 2023 (Luật BVDLCN) [12]–[14] quy định data subject rights cụ thể." |
| `Author trong [N]` | Author-attributed citation | "Brown trong [5] giới thiệu few-shot learning cho LLMs." |

## Bibliography entry formats

### Web technology docs / vendor docs

```
[N] <Organization>, "<Title>," <Year>. [Online]. Available: <URL>. [Accessed <Date>].
```

Example:
```
[1] AWS, "Well-Architected Framework, 6 Pillars," 2024. [Online]. Available: https://aws.amazon.com/architecture/well-architected. [Accessed 2026-05-18].
```

### Academic papers (IEEE journal/conference)

```
[N] <Author A.>, <Author B.>, and <Author C.>, "<Title of paper>," <Journal Name>, vol. <X>, no. <Y>, pp. <Z>-<W>, <Month> <Year>.
```

Example:
```
[5] T. Brown, B. Mann, N. Ryder, et al., "Language Models are Few-Shot Learners," Advances in Neural Information Processing Systems (NeurIPS), vol. 33, pp. 1877-1901, Dec. 2020.
```

### Vietnamese law / standards / decrees

```
[N] Quốc hội Việt Nam, "<Tên luật/nghị định>," <Số hiệu>, <Year>. [Online]. Available: <URL>.
```

Example:
```
[12] Quốc hội Việt Nam, "Luật Bảo vệ Dữ liệu Cá nhân (PDPL 2023)," Số 49/2023/QH15, 2023. [Online]. Available: https://thuvienphapluat.vn/van-ban/Cong-nghe-thong-tin/Luat-Bao-ve-du-lieu-ca-nhan-2023.
```

### Books

```
[N] <Author>, <Title>, <Edition (if not 1st)>. <City>: <Publisher>, <Year>.
```

Example:
```
[20] S. Newman, Building Microservices: Designing Fine-Grained Systems, 2nd ed. Sebastopol: O'Reilly Media, 2021.
```

### Industry reports / market research

```
[N] <Organization>, "<Report title>," <Type>, <Year>. [Online]. Available: <URL>.
```

Example:
```
[25] 6Wresearch, "Vietnam Learning Management System Market Report 2024-2030," Industry Report, 2024. [Online]. Available: https://www.6wresearch.com/industry-report/vietnam-learning-management-system-market.
```

## Mandatory metadata fields

Mỗi entry PHẢI có ≥3 trong 5 fields:
- Author / Organization
- Title (in quotes)
- Source (Journal / Publisher / URL)
- Year
- Page numbers OR URL accessed date

Missing metadata → mark `[N/A: <reason>]` để reviewer biết.

## Bibliography file structure

`documents/08-thesis/references/bibliography.md` organized by Chapter target:

```markdown
## Chapter 1: Introduction
[1] ...
[2] ...

## Chapter 2: Theoretical Background
[3] ...
```

Số `[N]` chạy global (không reset per chapter), IEEE convention.

## Anti-patterns

| ❌ Don't | ✅ Do |
|---|---|
| Mix citation styles (APA + IEEE) | Stick IEEE only |
| Cite mà không có entry trong bibliography.md | Mỗi `[N]` in-text MUST có entry |
| Dùng "ibid." hoặc "op. cit." | IEEE không dùng, re-cite `[N]` |
| Bỏ qua URL accessed date cho web sources | Mandatory `[Accessed YYYY-MM-DD]` cho online refs |
| Cite blog post chưa peer-reviewed như academic source | Mark là "Industry blog" / "Vendor docs" rõ ràng |
| Cite Wikipedia trực tiếp | Cite primary source Wikipedia references đến |

## Tooling

### Manual update workflow

Khi thêm refs mới:
1. Append entry vào matching `## Chapter N` section của `bibliography.md`
2. Increment `[N]` per global sequence (last `[N]` + 1)
3. In-text cite `[N]` trong source markdown
4. Future skill `quality/citation-extract/SKILL.md` (per GAP-647 Step 3) auto-extract từ WebFetch

### Verification

Pre-commit / pre-merge check (deferred Phase 2 per `incident-to-rule-pipeline.md` premature-rule guard):
- Scan thesis source markdown for unsourced claims (heuristic: scientific claim + no `[N]` near it → WARN)
- Verify each `[N]` in-text has matching entry trong `bibliography.md`

## Related

- GAP-647 thesis-bibliography-ieee, parent gap defining this style
- GAP-646 thesis-docx-pipeline, bibliography section trong DOCX template injection
- GAP-650 thesis-chapter-1-literature, Ch1 literature review must cite per this style
- `documents/08-thesis/chapter-mapping.md`, chapter-to-source map
- `.claude/rules/dev-readable-doc-language.md`, Vietnamese narrative + English identifiers (citation labels English [N], titles trong quote per source)
