# IEEE Citation Rules (Project Canonical)

Quy ước cite IEEE áp dụng cho `documents/08-thesis/**` markdown sources. Tóm tắt từ `documents/08-thesis/references/CITATION-STYLE.md` — bản project-canonical, chính thức cho mọi thesis chapter.

## In-text citation (trong body chapter)

| Pattern | Cú pháp | Ví dụ |
|---|---|---|
| Single reference | `[N]` | "Theo báo cáo 6Wresearch [3], thị trường EdTech VN..." |
| Multiple references | `[N, M]` hoặc `[N, M, K]` | "Tham khảo [1, 2] cho tổng quan multi-tenant..." |
| Range of references | `[N]–[M]` (em-dash U+2013) | "Các nghiên cứu gần đây [5]–[7] đã chỉ ra..." |
| Combined | `[1, 3]–[5]` | Hiếm dùng — tách thành 2 cite riêng nếu được |

**Vị trí:** đặt cite ngay sau claim cần ground, TRƯỚC dấu câu (`[3].` chứ không `.[3]`).

**Khi nào cite:**
- Mọi external claim, fact, statistic, framework, definition lấy từ source ngoài
- Mọi industry pattern / benchmark từ vendor docs
- Mọi VN law / decree reference
- KHÔNG cần cite cho fact common knowledge trong CS (vd "HTTP là protocol web") hoặc project-internal logic

## Bibliography entry format (trong `bibliography.md`)

Flat list (post Wave 102.7.6 renumber). Mỗi entry trên 1 line đầu, bắt đầu chính xác `[N] ` (không indent):

```
[N] Author, "Title," Source, Year. [Online]. Available: URL. [Accessed YYYY-MM-DD].
```

Sections `## Chapter N` chỉ để grouping reader; cite key chạy global sequential bất kể section.

5 entry types (per CITATION-STYLE.md):

1. **Web technology docs:** `[N] AWS, "Well-Architected Framework," 2024. [Online]. Available: https://...`
2. **Academic paper:** `[N] A. Author, B. Co-Author, "Title," Journal, vol. X, no. Y, pp. Z-W, Mon Year.`
3. **VN law:** `[N] Quốc hội Việt Nam, "Luật Bảo vệ Dữ liệu Cá nhân (PDPL 2023)," Số 49/2023/QH15, 2023.`
4. **Industry report:** `[N] Vendor, "Report title," Industry Report, Year. [Online]. Available: URL.`
5. **Book:** `[N] A. Author, Book Title, Edition. Publisher, Year.`

## Numbering rule

Global sequential — `[1]`, `[2]`, ..., `[38]`. KHÔNG reset per chapter. Gap (vd `[15]` removed) bị forbidden (renumber khi remove entry).

## What this skill verifies

Skill này check:

1. **Bracket parse correctness** — extract cite keys từ body theo 3 pattern in-text (single / list / range)
2. **Bibliography entry presence** — mỗi cite key body PHẢI có matching `[N]` entry đầu line trong `bibliography.md`
3. **Reverse mapping** — mỗi entry `[N]` trong bib PHẢI được cite ít nhất 1 lần trong body chapter set

Skill KHÔNG verify:

- Entry format correctness (5 type templates) — manual review hoặc separate format-linter
- Author/year accuracy (cần WebFetch + cross-check vendor docs)
- Cite placement contextual correctness (cite có ground claim đúng không) — semantic, human-only

## Related

- `documents/08-thesis/references/CITATION-STYLE.md` — full IEEE format spec
- `documents/08-thesis/references/bibliography.md` — current bibliography (38 entries, flat list)
- `documents/08-thesis/references/cross-ref-audit-2026-05-19.md` — manual audit precedent
