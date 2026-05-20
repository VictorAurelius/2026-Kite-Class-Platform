# Đồ án tốt nghiệp — Tài liệu nguồn

**Rules:** [`.claude/rules/docs-folder-structure.md`](../../.claude/rules/docs-folder-structure.md)

Thư mục này chứa toàn bộ source code và tài liệu nguồn cho đồ án tốt nghiệp cử nhân ngành Công nghệ thông tin — **"Xây dựng hệ thống SaaS cung cấp dịch vụ đào tạo"** (KiteHub Platform). Pipeline Python `create_thesis_v1.py` đọc các chương Markdown + bibliography + screenshots + diagrams Mermaid và xuất ra file DOCX hoàn chỉnh tuân thủ khung-chuẩn UTC.

Đồ án phục vụ phiên bảo vệ tháng 08/2026 tại Trường Đại học Giao thông Vận tải (UTC GTVT).

---

## 1. Thông tin đồ án

| Mục | Giá trị |
|---|---|
| **Đề tài** | Xây dựng hệ thống SaaS cung cấp dịch vụ đào tạo (KiteHub Platform) |
| **Loại đồ án** | Đồ án tốt nghiệp cử nhân |
| **Sinh viên** | Nguyễn Văn Kiệt — MSSV 221230890 — Lớp CNTT1-K63 |
| **Khoa** | Công nghệ thông tin |
| **Bộ môn** | Công nghệ phần mềm |
| **Trường** | Đại học Giao thông Vận tải (UTC GTVT) |
| **GVHD** | TS. Nguyễn Đức Dư |
| **Năm bảo vệ** | 2026 |

Metadata canonical đầy đủ tại [`thesis-info.md`](thesis-info.md) — mọi script render PHẢI đọc giá trị từ file này.

---

## 2. Cấu trúc đồ án

Đồ án theo khung-chuẩn UTC cho cử nhân CNTT — 4 chương chính sau Mở đầu:

| Phần | Mục tiêu |
|---|---|
| **Bìa chính + Bìa phụ** | 9 trường thông tin sinh viên + GVHD + năm bảo vệ |
| **Lời cảm ơn** | 5 đoạn: mở đầu / GVHD / Khoa + Trường / Bộ môn + thầy cô / gia đình + đóng kết |
| **Mục lục + 4 danh mục** | Mục lục chính + Danh mục Bảng biểu + Danh mục Hình ảnh + Danh mục Thuật ngữ + Danh mục Từ viết tắt |
| **Mở đầu** | 2 trang ~700-800 từ: Lý do chọn đề tài / Mục tiêu / Phạm vi / Phương pháp / Tóm tắt nội dung / Cấu trúc đồ án |
| **Chương 1 — Tổng quan đề tài** | §1.1 Giới thiệu chung + §1.2 Cơ sở chuyên ngành + §1.3 Khảo sát thị trường (5 phần mềm tham chiếu BeeClass / MISA EMIS / Mona eLMS / Easy Edu / DotB) + §1.4 Phạm vi đề tài và lộ trình triển khai |
| **Chương 2 — Kiến trúc hệ thống** | C4 model (Context + Container + Component + Code) + UML (Use Case + Class + ERD + Sequence diagrams) + Thiết kế cơ sở dữ liệu (schema 3 entity chính theo định dạng UTC §2.4) + SaaS pool model với Row-Level Security |
| **Chương 3 — Triển khai** | §3.1 Công nghệ sử dụng (Java + Spring Boot + Next.js + PostgreSQL + AWS) + §3.2 Kết quả triển khai sản phẩm (8 UI screen lấy từ ui_kits) + §3.3 Kiểm thử và đánh giá chất lượng (test pyramid Cohn + coverage Jacoco) |
| **Chương 4 — Kết quả triển khai và đánh giá** | §4.1 Cloud deployment AWS Singapore (hạ tầng hình dọc) + §4.2 User onboarding flow end-to-end + §4.3 KPI metrics + measurement plan + §4.4 Beta tenant scope + limitations |
| **Kết luận và kiến nghị** | Tổng kết kết quả đạt được + đề xuất hướng phát triển |
| **Danh mục tài liệu tham khảo** | 39+ trích dẫn IEEE-style theo thứ tự first-appearance (Vancouver order) |
| **Phụ lục** | (Reserved — chi tiết bổ sung theo yêu cầu hội đồng) |

---

## 3. Cấu trúc thư mục

```
08-thesis/
├── README.md                              ← File này
├── thesis-info.md                         ← Metadata canonical sinh viên + đề tài + GVHD
├── thesis-v1.docx                         ← Output DOCX cuối cùng (gitignored .docx not — committed)
│
├── create_thesis_v1.py                    ← Pipeline build DOCX chính (~1700 LOC)
├── renumber_citations.py                  ← Tool renumber bibliography theo Vancouver first-appearance
│
├── chapter-1-competitor-analysis.md       ← Ch.1 §1.1-§1.3 (Giới thiệu + Khảo sát thị trường)
├── chapter-1-vn-law-methodology.md        ← Ch.1 §1.4 (Phạm vi đề tài)
├── chapter-2-system-architecture.md       ← Ch.2 (~840 LOC, kiến trúc đầy đủ)
├── chapter-3-implementation.md            ← Ch.3 (~185 LOC, triển khai sản phẩm + kiểm thử)
├── chapter-4-deployment-results.md        ← Ch.4 (~365 LOC, deployment + KPI)
├── chapter-mapping.md                     ← Mapping nội bộ kế hoạch chương
│
├── chapter-1-ai-techniques-backup-*.md    ← Backup §1.4 AI techniques (lược bỏ)
├── chapter-1-conclusion-backup-*.md       ← Backup §1.7 kết luận cũ (lược bỏ)
├── chapter-3-code-snippets-backup-*.md    ← Backup 5 code snippet analysis (lược bỏ)
├── chapter-3-test-cases-backup-*.md       ← Backup 3 sample test case code (lược bỏ)
│
├── khung-chuan/                           ← Ảnh khung-chuẩn UTC (slide khoa CNTT)
│   └── khung-bao-cao-do-an.png            ← Reference primary
│
├── references/                            ← Tài liệu tham khảo + bibliography canonical
│   ├── bibliography.md                    ← 39+ entries IEEE format (Vancouver order)
│   ├── CITATION-STYLE.md                  ← Style guide trích dẫn
│   ├── methodology.md / technology-stack.md / testing-results.md / ... (supplementary)
│   └── cross-ref-audit-*.md               ← Audit cross-reference report
│
├── screenshots/                           ← PNG capture phục vụ Chương 1 + Chương 3
│   ├── 01-marketing-landing.png           ← Ch.3 §3.2.1 (rendered từ ui_kits/kitehub-story-v2)
│   ├── 02-signup-wizard-step1.png         ← Ch.3 §3.2.2 (ai-branding-wizard-v2)
│   ├── ... (8 PNG total cho Ch.3)
│   └── competitors/                       ← Ch.1 §1.3 — 5 PNG khảo sát phần mềm
│       ├── beeclass-homepage.png
│       ├── misa-amis-truong-hoc.png
│       ├── mona-elms.png
│       ├── easy-edu.png
│       └── dotb.png
│
└── .mermaid-cache/                        ← Cache PNG render Mermaid diagram qua kroki.io (gitignored)
```

---

## 4. Build pipeline — Render DOCX local

### Yêu cầu môi trường

- Python 3.10+ với `python-docx` và `lxml`
- Mạng internet truy cập `kroki.io` để render Mermaid diagram thành PNG (có fallback mmdc local nếu cài đặt)

### Render

```bash
# Render lần đầu (download Mermaid PNG vào .mermaid-cache/)
python3 documents/08-thesis/create_thesis_v1.py

# Output: documents/08-thesis/thesis-v1.docx
```

### Clear cache + re-render khi sửa Mermaid

```bash
rm -rf documents/08-thesis/.mermaid-cache/mermaid-*.png
python3 documents/08-thesis/create_thesis_v1.py
```

### Renumber bibliography theo Vancouver order

```bash
python3 documents/08-thesis/renumber_citations.py
```

Script này quét tất cả chapter MD, phát hiện thứ tự xuất hiện đầu tiên `[N]` trong body, và renumber `references/bibliography.md` cho khớp.

---

## 5. Workflow chỉnh sửa nội dung

Cấu trúc separation of concern:

| Loại nội dung | File | Cách chỉnh |
|---|---|---|
| **Cover layout** (bìa chính + bìa phụ) | `create_thesis_v1.py` — `add_cover_page()` + `add_secondary_cover_page()` | Edit Python — tránh duplicate giữa 2 hàm |
| **Lời cảm ơn** | `create_thesis_v1.py` — `add_acknowledgment_page()` | 5 đoạn ~510 từ |
| **Mở đầu** | `create_thesis_v1.py` — `add_introduction()` | 2 trang ~700-800 từ |
| **Kết luận và kiến nghị** | `create_thesis_v1.py` — `add_conclusion()` | Bao gồm sub-section "Kiến nghị" |
| **Chapter content** | `chapter-{1,2,3,4}-*.md` Markdown | Edit MD trực tiếp; pipeline tự parse |
| **Bibliography** | `references/bibliography.md` | Edit MD + chạy `renumber_citations.py` |
| **UI screenshots Ch.3** | Capture Playwright headless từ `documents/02-architecture/design-system/ui_kits/**` | Lưu vào `screenshots/` rồi reference qua markdown `![alt](screenshots/<file>.png)` |
| **Competitor screenshots Ch.1** | Capture Playwright headless từ homepage external | Lưu vào `screenshots/competitors/` |
| **Mermaid diagrams** | Inline trong chapter MD trong code fence ` ```mermaid ` | Pipeline tự render PNG qua kroki.io |
| **Danh mục thuật ngữ + viết tắt** | `create_thesis_v1.py` — `add_abbreviations()` | Hardcoded list trong pipeline |

### Markdown syntax được pipeline parse

| Markdown | Render trong DOCX |
|---|---|
| `## N.X Title` | Section heading (TNR 16pt bold left) |
| `### N.X.Y Title` | Subsection heading (TNR 14pt bold left) |
| `#### Title` | Sub-subsection (bold paragraph) |
| Paragraph thường | TNR 13pt Justify dòng đầu thụt 1cm line spacing 1.2 |
| `- bullet` / `* bullet` | Bullet list item |
| `1. numbered` | Numbered list |
| `` `code` `` | Inline code (monospace) |
| ``` ```lang ... ``` ``` | Code block (skip render — Ch.3 đã loại bỏ code presentation) |
| ``` ```mermaid ... ``` ``` | Render PNG qua kroki.io + embed inline |
| `\| col \| col \|` | Markdown table → DOCX table với `Table Grid` style |
| `![alt](path.png)` | Embed image inline 14cm width (path resolve relative `08-thesis/`) |
| `> blockquote` | Italic paragraph |
| `**Hình N.M.** Caption` đặt sau Mermaid block | Figure caption (UTC §2.4: caption DƯỚI hình) |
| `**Bảng N.M.** Caption` đặt trước table block | Table caption (UTC §2.4: caption TRÊN bảng) |

### Anti-pattern

- Đừng đặt `[`/`]` trong path image — markdown parser sẽ break
- Đừng dùng `<br/>` trong Mermaid `sequenceDiagram` hoặc `stateDiagram-v2` (parser bug); dùng ` — ` em-dash hoặc xuống dòng thực
- Đừng dùng `;` trong Mermaid `Note over X,Y: ...` text (parser treat như statement terminator)
- Đừng thêm chú thích reference đến file repo (vd `V*.sql` migration / `documents/01-business/...`) trong narrative thesis — đề tài academic không tham chiếu repo internal

---

## 6. Section + page numbering scheme

Pipeline tạo 4 section breaks theo khung-chuẩn UTC §2.1:

| Section | Nội dung | Border | Page number |
|:-:|---|:-:|---|
| 0 | Bìa chính + Bìa phụ | ✅ Có | KHÔNG đánh số |
| 1 | Lời cảm ơn | ❌ Không | KHÔNG đánh số |
| 2 | Mục lục + 4 danh mục | ❌ Không | Roman thường `i, ii, iii, iv, ...` |
| 3 | Mở đầu + Ch.1-4 + Kết luận + TLTK + Phụ lục | ❌ Không | Arabic `1, 2, 3, ...` từ Mở đầu |

Page number đặt center-top header.

---

## 7. Hậu xử lý bắt buộc khi nộp — Word F9

Pipeline Python sinh DOCX với XML field codes (TOC + Danh mục Bảng/Hình + SEQ numbering) ở dạng **placeholder text** chưa được render thành text values.

**Trước khi nộp đề bảo vệ, sinh viên PHẢI:**

1. Mở `thesis-v1.docx` bằng **Microsoft Word** (KHÔNG dùng LibreOffice — F9 trên LibreOffice không update SEQ field như Word).
2. Bấm `Ctrl+A` chọn toàn bộ document.
3. Bấm `F9` cập nhật mọi field (TOC + Danh mục + Bảng X.Y + Hình X.Y SEQ numbering).
4. Bấm `Ctrl+A` + `F9` lần 2 để đảm bảo nested fields (TOC reference SEQ fields) cập nhật.
5. Save (`Ctrl+S`) — file sẵn sàng in.

**Verification trước khi ship:** Mục lục + 4 Danh mục hiển thị đầy đủ section names + page numbers; Bảng/Hình captions có số tuần tự (Bảng 1.1, Bảng 1.2, ..., Hình 1.1, Hình 1.2, ...).

---

## 8. Reference khung-chuẩn UTC

| Tài liệu | Vai trò |
|---|---|
| [`khung-chuan/khung-bao-cao-do-an.png`](khung-chuan/khung-bao-cao-do-an.png) | Slide khoa CNTT đặc tả 18 mục bố cục bắt buộc (primary reference) |
| [`../07-archived/academic/word-reports/templates/Quy dinh trinh bay do an tot nghiep.pdf`](../07-archived/academic/word-reports/templates/) | Quy định trình bày chính thức UTC (§1 bố cục + §2 trình bày + §3 IEEE citation) |
| [`../07-archived/academic/word-reports/templates/Mau-Decuong DATN-Cử nhân.pdf`](../07-archived/academic/word-reports/templates/) | Mẫu đề cương đồ án cử nhân |
| [`../07-archived/academic/word-reports/de-cuong-datn/DE_CUONG_DATN.docx`](../07-archived/academic/word-reports/de-cuong-datn/) | Sample đề cương đã được phê duyệt |
| [`../07-archived/academic/word-reports/bao-cao-thuc-tap/BAO_CAO_THUC_TAP.pdf`](../07-archived/academic/word-reports/bao-cao-thuc-tap/) | Sample báo cáo thực tập (reference bìa + lời cảm ơn + format) |

---

## 9. Tài liệu liên quan trong repo

| Folder | Vai trò |
|---|---|
| [`../00-brd/`](../00-brd/) | Business Requirements Document — nguồn personas + objectives |
| [`../01-business/`](../01-business/) | Business rules + use cases + API contracts cho 3 sản phẩm |
| [`../02-architecture/`](../02-architecture/) | Kiến trúc hệ thống + design system + ADR — nguồn cho Ch.2 |
| [`../02-architecture/design-system/ui_kits/`](../02-architecture/design-system/ui_kits/) | UI HTML/JSX prototypes — capture cho Ch.3 §3.2 |
| [`../03-planning/`](../03-planning/) | Wave plans + session handoffs (lịch sử Wave 102.x) |
| [`../04-quality/audits/`](../04-quality/audits/) | Audit reports — nguồn metrics cho Ch.3 §3.3 testing section |
| [`../04-quality/gaps/`](../04-quality/gaps/) | Gap files — track follow-up GAP-689 Wave 102.6+ |
| [`../05-guides/`](../05-guides/) | Deploy + operations runbooks |

---

## 10. Quy trình cập nhật

1. **Chỉnh sửa nội dung:** Edit chapter MD hoặc Python function tương ứng (theo bảng §5)
2. **Re-render local:** `python3 documents/08-thesis/create_thesis_v1.py`
3. **Verify output:** Mở `thesis-v1.docx` trong Word/LibreOffice — kiểm tra layout + diagram + screenshot
4. **Commit:** Tạo branch `wave/102.x-<topic>` → commit → mở PR → CI check
5. **Pre-defense:** Trước khi nộp, chạy bước Word F9 (§7) để populate TOC + SEQ fields

Follow-up công việc cho phiên bản hoàn thiện pre-defense — xem [`../04-quality/gaps/phase-1-beta/GAP-689-wave-102.6-thesis-v1-deferred-items.md`](../04-quality/gaps/phase-1-beta/GAP-689-wave-102.6-thesis-v1-deferred-items.md).

---

## 11. Lịch sử phiên bản

Đồ án được phát triển qua nhiều wave iteration. Chi tiết từng wave xem [`../03-planning/waves/`](../03-planning/waves/) (tìm các file `wave-2026-05-2*-102.*-thesis*.md`). Mọi quyết định lớn (chọn pipeline Python thay LaTeX, tách Bucket F deferred items, override khung-chuẩn UTC với UML/ERD co-exist C4) đều được audit + sign-off trước khi merge.

Output DOCX hiện tại (`thesis-v1.docx`) là kết quả tích hợp toàn bộ wave 102.x — sẵn sàng cho phiên review của GVHD trước hội đồng tháng 08/2026 sau khi sinh viên thực hiện bước Word F9 (§7).
