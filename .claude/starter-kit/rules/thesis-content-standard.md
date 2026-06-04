---
paths:
  - "documents/<thesis-dir>/**"
  - "documents/**/audits/persona-review/*-thesis-*"
  - ".claude/rules/thesis-content-standard.md"
---

# Thesis Content Standard — academic-quality review rubric cho khóa luận tốt nghiệp

**Priority:** 🟠 MANDATORY — academic deliverable governance
**Version:** 2.0.0
**Created:** 2026-05-23
**Last-Reviewed:** 2026-06-04
**Reviewer-Approver:** @<starter-kit-upstream-maintainer> (starter-kit upstream maintainer — distilled từ Vietnamese-university bachelor thesis sprint upstream; original v1.0.0..v2.0.0 ship 2026-05-19..05-26)
**Applies to:** Mọi file dưới `documents/<thesis-dir>/**` (vd `documents/08-thesis/`) được render thành DOCX/PDF deliverable cho academic submission. Scope = chapter MDs + final DOCX + bibliography + audit reports thesis-related. Out-of-scope: source code, internal runbooks, non-academic project docs.

**Opinionated scope:** Rule này **opinionated cho convention khóa luận đại học Việt Nam** (UTC, HUST, UET, HCMUT, BKHN — convention tương tự). Universities ngoài VN cần adapt: page-size + heading numbering + bibliography style + page-count target.

---

## 1. The Rule

> **Mọi thesis V1+ ship phải đạt ≥75/100 C+ trên 9-category rubric §2.** Format compliance + content + bibliography đã ổn nhưng dễ bỏ sót 5 critical content-quality dimensions: academic tone discipline, project-internal reference scrub, draft-marker scrub, diagram-as-image verification, compliance/legal sensitivity. Rubric 9-category này codify all dimensions để eliminate blind spots.

Force-multiplier rationale: 1 chuẩn rubric → mọi thesis V1+ V2+ subsequent auto-comply → eliminate retroactive content-quality rework cost.

**Grounding sources (in priority order — your project must source-of-truth match):**

1. **🔴 PRIMARY — khung chuẩn nguyên bản của trường** (ảnh / PDF chính thức từ khoa hoặc phòng đào tạo). Khung này list các mục bắt buộc của khóa luận. UTC convention sample mandate sections:
   - Bìa (theo mẫu trên hệ thống website của trường)
   - Bìa phụ (KHÔNG có logo)
   - Lời cảm ơn (bắt buộc)
   - Mục lục (i, ii, iii, iv, ...)
   - **Danh mục các từ viết tắt, thuật ngữ** (GỘP 2 trong 1 heading)
   - Danh mục bảng
   - Danh mục hình
   - Mở Đầu (2-3 trang): tại sao chọn đề tài / tóm tắt nội dung sẽ giải quyết (Mục tiêu / Đối tượng / Phạm vi) / các nội dung và phương pháp giải quyết
   - Chương 1: Tổng quan về bài toán và các công nghệ, công cụ (Hiện trạng / Bài toán / Công nghệ, công cụ sử dụng)
   - Chương 2: Phân tích và thiết kế hệ thống (FR/NFR / Sơ đồ tổng thể, các bên liên quan, các tính năng / Phân tích các vai trò và sử dụng — use case / Quy trình nghiệp vụ chính / Mô hình hóa hệ thống)
   - Chương 3: Phân tích, thiết kế và triển khai hệ thống (chương chính — BRD, BPM, Use case, DataBase, Class, ERD, AWS diagram)
   - Chương 4: Đánh giá kết quả và Kết luận (Kết quả triển khai và tài liệu / Kết quả triển khai / So sánh đánh giá kết quả thử nghiệm / Kết luận, kiến nghị + Phương hướng phát triển)
   - Tài liệu tham khảo
   - Phụ lục (nếu có)

   **❌ KHÔNG có trong khung UTC nguyên bản (per user direction "ngoài lời cam đoan thì cái gì không theo khung chuẩn cũng bỏ"):** LỜI CAM ĐOAN / TÓM TẮT VN abstract page riêng / ABSTRACT EN page riêng / NHẬN XÉT GVHD page / Phụ lục audit chất lượng AI. Các trường khác có thể có khung khác — adapt theo source-of-truth của trường bạn.

2. **🟠 SECONDARY — your school's thesis format spec PDF** (e.g., "Quy định trình bày đồ án tốt nghiệp" cho UTC) — de jure standard cho format details (font / margin / numbering / IEEE format). Spec PDF thường list cùng required sections như khung primary + thêm format details (A4 / TNR 13pt body / margins T=2.5 B=2.5 L=3.0 R=2.0cm / SEQ Bảng X.Y + Hình X.Y).
3. **🟡 TERTIARY — known-good DOCX sample** từ archive của trường (production-quality báo cáo thực tập hoặc đồ án từ sinh viên trước). Samples cung cấp visual reference cho format details (TOC layout, IEEE bibliography rendering, danh mục sub-section structure) NHƯNG KHÔNG override khung nguyên bản về required vs optional sections. Báo cáo thực tập sample là BCTT, KHÔNG phải DATN; scope khác — sample cho reference visual, không phải mandate sections.
4. **🟡 TERTIARY — Persona simulation audit** — outside-in audit với 3-agent roleplay (GVHD + GVPB + Defense committee) trên thesis draft
5. **🟡 TERTIARY — User-flagged inspection issues** — direct review by author / advisor pre-ship

---

## 2. The 9-category rubric / 100 points

### C1 — Format compliance (15 points)

Tuân thủ khung chuẩn nguyên bản (§1 PRIMARY) + your school's spec PDF (§1 SECONDARY) cho format details:

| Sub-criterion | Pts | Verify |
|---|:---:|---|
| **A4 page size (210×297mm) explicit** per spec | 2 | `section.page_width == Cm(21.0)` + `page_height == Cm(29.7)` (python-docx). Note: many python-docx pipelines default US Letter — STRICT mandate A4 per VN spec. |
| Times New Roman 13pt body / 14pt H3 / 16pt H2 / 18pt H1 per spec | 3 | python-docx introspection on Heading styles. H4 + H5 sub-sub-headings nếu used must be 13pt bold (typical VN spec define 3 levels — over-deep sectioning anti-pattern). |
| Margins top 2.5 / bottom 2.5 / left 3.0 / right 2.0 cm | 2 | `section.top_margin == Cm(2.5)` etc. Standard VN convention. |
| Binding gutter (offset for binding edge) + bìa cứng instructions | 1 | `section.gutter > 0` OR explicit printer instructions trong handover doc cho gáy in "HỌ TÊN - LỚP - NĂM" |
| **Bìa chính theo mẫu trên hệ thống website trường** (school logo + format chuẩn) | 2 | docx inline_shape inspection — actual PNG logo present, NOT `[LOGO ...]` fallback string. Format match `Mau-Decuong DATN-Cử nhân.pdf` reference template (or equivalent). **Hard FAIL if fallback string visible** |
| **Bìa phụ KHÔNG có logo per khung nguyên bản §1** + info table khác bìa chính | 2 | Per khung primary source: bìa phụ explicit "không có logo". Bìa phụ chỉ có info-table (Sinh viên / MSSV / Lớp / Khóa / GVHD / GVPB) NOT trùng lặp bìa chính content. |
| Section numbering per spec: chapter "Chương N." + section "N.M" + subsection "N.M.P" | 1 | Per khung primary: "Chương 1", "Chương 2", "Chương 3", "Chương 4" (NOT all-caps "CHƯƠNG"). `A.2.4`, `B.5` (alpha prefix) BANNED. Strict numeric only. |
| Table caption `Bảng X.Y. ...` + figure caption `Hình X.Y. ...` numbering gắn chương | 1 | SEQ field `add_table_caption` integration required (python-docx). |
| TOC + Danh mục bảng + Danh mục hình + **Danh mục các từ viết tắt, thuật ngữ (gộp 1 heading per khung)** populated (NOT placeholder text) | 1 | Placeholder "(Bấm Ctrl+A rồi F9...)" trong file in nộp = draft signal. Auto-populate via python-docx XML settings hoặc post-process F9 trước nộp. Per khung primary: "Danh mục các từ viết tắt, thuật ngữ" GỘP 1 heading (NOT tách 2 danh mục riêng). |

**Verdict thresholds:** ≥13 PASS / 10-12 PARTIAL / <10 FAIL.

### C2 — Content completeness + page count target (15 points)

Tuân thủ khung chuẩn nguyên bản (§1 PRIMARY) — **4 chương** với cấu trúc cụ thể (NOT generic 4-6):

| Sub-criterion | Pts | Verify |
|---|:---:|---|
| **Đầy đủ structure khớp khung primary §1**: Bìa + Bìa phụ + Lời cảm ơn + Mục lục + Danh mục viết tắt+thuật ngữ + Danh mục bảng + Danh mục hình + Mở đầu + Chương 1-4 + Kết luận và Kiến nghị (gộp) + Tài liệu tham khảo + Phụ lục (nếu có) | 4 | Heading 1 count + section names khớp khung primary §1 EXACTLY. |
| **Chương 1 = "Tổng quan về bài toán và các công nghệ, công cụ"** với 3 sub: 1.1 Hiện trạng / 1.2 Bài toán / 1.3 Công nghệ, công cụ sử dụng | 1 | Per khung primary §1 Ch.1 detail. |
| **Chương 2 = "Phân tích và thiết kế hệ thống"** với 5 sub (FR/NFR / Sơ đồ tổng thể, các bên liên quan, tính năng / Use case + vai trò / Quy trình nghiệp vụ chính / Mô hình hóa hệ thống) | 1 | Per khung primary §1 Ch.2 detail. |
| **Chương 3 = "Phân tích, thiết kế và triển khai hệ thống" (chương chính)** gồm: BRD / BPM / Use case / DataBase design / Class diagram / ERD / AWS diagram | 1 | Per khung primary §1 Ch.3 detail — chương chính (largest). |
| **Chương 4 = "Đánh giá kết quả và Kết luận"** với 4 sub: Kết quả triển khai và tài liệu / Kết quả triển khai / So sánh đánh giá kết quả thử nghiệm / Kết luận, kiến nghị + Phương hướng phát triển | 1 | Per khung primary §1 Ch.4 detail. |
| Page count target: cử nhân 60-80 trang / kỹ sư 80-110 / thạc sĩ 120-180. **Cap auto-FAIL:** cử nhân >90 trang | 4 | `len(doc.paragraphs)` × avg-words-per-paragraph estimate; OR LibreOffice page-count read-back. Soft deduct 1 pt per 10 trang vượt upper bound. |
| KẾT LUẬN VÀ KIẾN NGHỊ (GỘP 1 section per khung) độ dài 2-3 trang minimum + sub: Tổng kết / Hạn chế / **Phương hướng phát triển** | 2 | Per khung primary §1 — "Kết luận" và "Kiến nghị" GỘP 1 heading (KHÔNG tách 2 sections riêng). "Đóng góp" có thể là sub-bullet trong "Tổng kết" mà không phải separate sub-section. |
| Trim repo-internal retrospective content khỏi chapter body (vd "Lessons learned" + "Feature scope cut" + production incident timelines) | 1 | Chuyển retrospective insights vào KẾT LUẬN gói gọn, đỡ duplicate. Body academic prose-only. |

**Verdict thresholds:** ≥13 PASS / 10-12 PARTIAL / <10 FAIL. **Page count cap:** >90 trang cử nhân auto-FAIL category regardless other sub-criteria.

**v2.0.0 MAJOR: KHÔNG còn scoring rows cho Abstract VN + Abstract EN + Lời cam đoan** — per khung primary §1, các pages này KHÔNG có trong khung chuẩn nguyên bản UTC. Pipeline tạo DOCX nên skip 4 functions (`add_oath_page` + `add_abstract_vi` + `add_abstract_en` + `add_advisor_review_page`) nếu đang implement theo project upstream pattern. Các trường khác có thể có yêu cầu khác — adapt theo source-of-truth của trường bạn.

### C3 — Bibliography IEEE format (15 points)

Per VN university spec §3 IEEE format:

| Sub-criterion | Pts | Verify |
|---|:---:|---|
| Bibliography section heading "TÀI LIỆU THAM KHẢO" (Vietnamese) | 1 | Heading 1 last section |
| ≥30 entries cử nhân OR ≥50 kỹ sư OR ≥80 thạc sĩ | 2 | Count `^[N]` pattern |
| 100% inline cite utilization (no orphan refs) | 3 | Each `[N]` in bibliography appears ≥1× in body |
| **Citation order by first appearance** trong body | 2 | `[1]`, `[2]`, `[3]`... numbered theo thứ tự lần đầu trích dẫn xuất hiện trong body, KHÔNG arbitrary bibliography order. Verify: scan body cho first `[N]` mention sequence vs bibliography numbering. |
| **Page number citation format `[15, tr.314]` cho direct quotes** | 1 | Nếu narrative chứa direct quote (in quotes "..."), citation phải có `, tr.NNN`. |
| IEEE format rendering (hanging indent + italic book title + quoted article title) | 2 | python-docx paragraph format inspection |
| Hyperlinks blue + underline cho URLs | 1 | `https?://` pattern → blue + underline run |
| Mix academic papers + standards + grey literature (NOT all vendor docs / blog posts) | 2 | Random sample 10 refs at least 30% peer-reviewed journals/conferences. Vendor docs acceptable but weight thấp. |
| **Include your department's giáo trình** (1-2 refs minimum cho cử nhân) | 1 | Committee thường có cựu GV department-internal; thiếu cite giáo trình department làm giảm "thông thuộc chương trình đào tạo". |

**Verdict thresholds:** ≥13 PASS / 10-12 PARTIAL / <10 FAIL.

### C4 — Academic tone discipline (15 points)

Academic tone = formal, objective, sinh viên perspective:

| Sub-criterion | Pts | Verify |
|---|:---:|---|
| **Pronoun discipline lock per section type:** LỜI CẢM ƠN + KẾT LUẬN dùng "em" (sinh viên perspective); body chương dùng "khóa luận" / "tác giả" passive voice — NHẤT QUÁN trong section | 3 | grep "\bbạn\b\|\bchúng ta\b\|\bchúng tôi\b" returns 0 matches in body chapters. |
| **Word choice formality — KHÔNG dùng "đối thủ"** (competitor business jargon); thay bằng "đối tượng tham khảo" / "hệ thống tương tự" / "tài liệu so sánh" / "công trình nghiên cứu liên quan" | 3 | grep "đối thủ" returns 0 matches |
| KHÔNG mixed-language code-switching pollution trong narrative — English technical token natural OK; English narrative sentences BANNED | 2 | Per sister rule `dev-readable-doc-language.md` §2 — narrative Vietnamese, identifier English natural |
| Vietnamese diacritics đầy đủ, no mojibake | 1 | grep -P '[\x{0300}-\x{036F}]' check + sample reading |
| **KHÔNG slang / informal connector / emoji** trong body — "OK", "thấy ngay", "cứ vậy", emoji 🎉/✅/⚠️/🚀/📅 | 2 | grep emoji + slang patterns |
| KHÔNG passive-aggressive / opinionated phrasing ("rõ ràng là", "ai cũng biết", "đương nhiên", "tất nhiên") | 1 | grep banned phrases |
| **Bullet vs prose balance** — bullet list ratio < 40% of content (thesis convention stricter than internship report) | 2 | Ratio bullet-paragraphs : narrative-paragraphs across body |
| **Typo + grammar polish** | 1 | Spot-check 5 paragraphs per chapter |

**Verdict thresholds:** ≥12 PASS / 9-11 PARTIAL / <9 FAIL.

### C5 — Project-internal reference scrub (10 points)

Project jargon, internal artifact names, AI assistant references BANNED in academic deliverable:

| Sub-criterion | Pts | Verify |
|---|:---:|---|
| KHÔNG mention "Claude" / "Cursor" / "Copilot" / AI assistant tools by name in body narrative | 2 | grep "Claude" returns 0 matches in body (exception: bibliography ref [N] vendor citation OK; technical mention "LLM API providers" acceptable) |
| KHÔNG repo wave/release names ("Wave N", "Phase X BETA", "Phase X.5") | 2 | Replace với generic phrasing: "giai đoạn phát triển", "phiên bản thử nghiệm", "giai đoạn beta" |
| KHÔNG gap IDs (project tracking IDs) | 1 | Strip hoàn toàn — không phải reference cho academic |
| KHÔNG rule/skill internal file paths (`.claude/rules/*.md`, `.claude/skills/*/SKILL.md`) | 1 | grep `\.claude/` returns 0 |
| KHÔNG repo internal status markers ("DONE", "PARTIAL", "OPEN", "DEFER", "audit X/100") | 1 | grep banned status patterns 0 in body |
| **KHÔNG rebrand existing methodology** as "original methodology" without literature citation | 2 | "audit-driven methodology" appears original nhưng tương đương TDD/CI/Lean Six Sigma. Either (a) cite Deming PDCA / Beck TDD / Poppendieck Lean / IEEE 730 SQA literature support OR (b) re-frame as "Quality-Driven Development approach" acknowledging precedent. |
| **Persona / role-play skill content moved khỏi Ch.1** — internal feature implementation không phải research methodology | 1 | Move to Ch.3 Implementation hoặc Ch.4 Lessons. |

**Verdict thresholds:** ≥8 PASS / 5-7 PARTIAL / <5 FAIL. **Hard rule:** any "Claude" mention in body narrative = auto category FAIL regardless other sub-criteria.

**Standalone-document principle:** Thesis docx PHẢI standalone — committee không thấy được repo. KHÔNG reference internal `documents/**` paths trong body narrative. Bibliography refs CHỈ từ sources công khai uy tín (peer-reviewed papers / standards ISO/IEEE/RFC / official vendor docs / regulator gov.vn). KHÔNG cite internal audit reports / chapter MD cross-refs / planning docs.

### C6 — Draft-marker scrub (5 points)

Draft-only conventions không phù hợp final academic deliverable:

| Sub-criterion | Pts | Verify |
|---|:---:|---|
| KHÔNG `## TL;DR` sections (Twitter/blog convention, not academic) | 2 | grep "## TL;DR\|## TLDR" returns 0 matches |
| KHÔNG visible `TODO` / `FIXME` / `XXX` / `[placeholder]` / `[stub]` markers in body | 2 | grep banned markers 0 in body; acceptable form: explicit "[Đang thu thập số liệu — sẽ cập nhật trước defense]" honest acknowledgment |
| KHÔNG date-prefix in heading ("Cập nhật lần cuối: YYYY-MM-DD", "v0.X-beta") | 1 | grep version markers in heading 0 matches |

**Verdict thresholds:** ≥4 PASS / 3 PARTIAL / <3 FAIL.

### C7 — Diagram + figure rendering (10 points)

Mermaid/PlantUML diagrams trong source MD → MUST render as PNG/JPG image trong DOCX:

| Sub-criterion | Pts | Verify |
|---|:---:|---|
| Mermaid code blocks rendered as images (NOT raw text) | 4 | grep ```mermaid trong rendered docx returns 0 (means code stripped + replaced với image) |
| Figure captions "Hình N.M. ..." với SEQ field auto-numbering | 2 | Caption SEQ field inspection |
| Cross-references "xem Hình N.M" linking to actual figures | 2 | "xem Hình" / "Hình \d+\.\d+" cross-ref count > 0 + each ref points to actual figure |
| Figure resolution ≥150 DPI for print | 1 | Image metadata inspection |
| Source attribution per VN spec: "Nguồn: ..." cho figures lấy từ ngoài | 1 | Caption convention |

**Verdict thresholds:** ≥8 PASS / 5-7 PARTIAL / <5 FAIL.

### C8 — Examiner readiness (10 points)

| Sub-criterion | Pts | Verify |
|---|:---:|---|
| Cover page formal (school + faculty + title + student + advisor + year) | 2 | Cover content inspection. Bìa cứng + gáy in instructions. |
| Bibliography 100% cite utilization no orphans | 1 | Same as C3 |
| VN law citations current (PDPL 2023 + Cybersecurity 2018 + Decree 13/53/2022 + Thông tư 78/2021 + Decree 147/2024 — if scope relevant) | 1 | Bibliography content |
| Methodology section explicit + literature-backed (Deming / Beck / Poppendieck / IEEE 730) | 2 | Ch.1 hoặc separate methodology chapter |
| **Architecture diagrams present as PNG/JPG embedded (NOT Mermaid code text)** | 2 | Figure count > 5 PNG images + Hình X.Y caption + chú thích paragraph SAU mỗi hình. |
| Real data/KPI/benchmarks NOT placeholder | 1 | Strip "(placeholder ...)" public + add ≥1-2 KPI cụ thể measured trước defense. |
| Beta user feedback embedded (if applicable; recommended for thạc sĩ, optional cử nhân) | 1 | If scope has real users |

**Verdict thresholds:** ≥8 PASS / 5-7 PARTIAL / <5 FAIL.

### C9 — Compliance + legal sensitivity (5 points)

Committee/GVPB chất vấn pháp luật + ethics:

| Sub-criterion | Pts | Verify |
|---|:---:|---|
| **KHÔNG admit explicit vi phạm pháp luật VN** (Decree 53/2022 data localization / PDPL 2023 / Cybersecurity 2018) trong body | 2 | Rewrite mềm: "Phase BETA invite-only chưa kích hoạt ngưỡng [threshold]; roadmap migrate sang [compliance path] trong giai đoạn GA" defensive phrasing |
| **Sample data anonymization** — KHÔNG dùng tên thật trong narrative khi chưa có consent | 1 | Replace với "(tên giả định)" suffix hoặc fictional name |
| **DPO / DPIA roadmap explicit** trong Ch.1 hoặc Ch.4 (NOT "TODO Phase 2+") | 1 | Admit honest: "DPO + DPIA scheduled [date] trước GA launch" defense-proof |
| **Penetration test evidence** cho security claims | 1 | Cite security audit score + test results. Without evidence, security claim weak in defense. |

**Verdict thresholds:** ≥4 PASS / 3 PARTIAL / <3 FAIL. **Hard rule:** explicit "vi phạm" / "violation" phrasing trong body = auto category FAIL.

---

## 3. Banned patterns reference table

Concrete grep-able patterns:

| Category | Banned pattern | Required replacement |
|---|---|---|
| C4 Academic tone | "đối thủ" | "đối tượng tham khảo" / "hệ thống tương tự" / "công trình nghiên cứu liên quan" |
| C4 Academic tone | "OK", "không sao", "cứ vậy", emoji 🎉/✅/⚠️ trong body | Drop emoji; formal: "đạt yêu cầu", "phù hợp" |
| C4 Academic tone | "bạn" / "chúng ta" trong narrative formal sections | "em" / "tôi" (sinh viên perspective) |
| C5 Project-internal | "Claude" / "Cursor" / "Copilot" (trừ bibliography vendor ref [N]) | Strip hoặc generic "trợ lý AI" / "công cụ AI assistant" |
| C5 Project-internal | "Wave N", "Phase X BETA" | "giai đoạn phát triển N" / "phiên bản 1" / strip nếu không cần |
| C5 Project-internal | Project tracking IDs (gap IDs, issue numbers) | strip hoàn toàn |
| C5 Project-internal | `.claude/rules/*.md`, `.claude/skills/**` paths | strip hoàn toàn |
| C5 Standalone | Internal `documents/**` path references | Restate content inline OR cite published source |
| C5 Standalone | Bibliography refs từ internal repo (audit reports / chapter MD cross-refs / plans) | Replace với public source OR strip claim |
| C9 Compliance | "vi phạm Nghị định 53/2022" / "compliance debt được chấp nhận" explicit | Defensive phrasing "chưa kích hoạt ngưỡng X; roadmap migrate Y" |
| C9 Compliance | Sample data tên thật không có consent | Suffix "(tên giả định)" |
| **C1 No-icon principle** — banned chars trong body narrative | ✅ ✗ ❌ ⚠️ 🎉 🚀 📅 🆘 🔴 🟢 🟡 🟠 ▪️ ◆ ■ ▲ ⇒ ⇐ → ← (emoji + arrow + colored circle) | "đạt yêu cầu" / "không đạt" / " - " / " thì " narrative phrasing |
| **C1 Character set allowed** | non-typeable special chars | Vietnamese alphabet (a-z, đ, dấu thanh) + Latin alphabet + chữ số + standard punctuation |
| **C4 No-font-swap principle** — KHÔNG đổi font inline | Courier New cho `inline code` markdown / Cambria cho equations | Spec mandates TNR 13pt mọi đoạn văn body. Emphasis dùng *italic* / **bold** / UPPERCASE / "ngoặc kép" |
| **C4 Inline code rendering** | `instance_id` rendered with Courier New | `instance_id` rendered TNR italic OR UPPERCASE `INSTANCE_ID` per VN academic convention |
| C6 Draft-marker | `## TL;DR` | Strip — academic content flows directly từ Mở đầu vào chương |
| C6 Draft-marker | `TODO`, `FIXME`, `XXX`, `[placeholder]`, `[stub]` | "[Đang thu thập số liệu — sẽ cập nhật trước defense]" honest form OR strip |
| C6 Draft-marker | `**Cập nhật lần cuối:** YYYY-MM-DD` | Strip from heading (move to frontmatter) |
| C6 Draft-marker | `v0.X-beta`, `v1.0.0-rc` version markers | Strip |
| **C2 Non-khung section** (added v2.0.0) — anything NOT in khung primary §1 = banned per source-of-truth | **LỜI CAM ĐOAN page** (nếu khung UTC nguyên bản không list) | REMOVE page. Khung chuẩn nguyên bản UTC KHÔNG list LỜI CAM ĐOAN. Adapt theo source-of-truth của trường bạn. |
| **C2 Non-khung section** | **TÓM TẮT (VN abstract page riêng)** (nếu khung không list) | REMOVE page. Khung UTC KHÔNG list. Adapt theo trường. |
| **C2 Non-khung section** | **ABSTRACT (EN page riêng)** (nếu khung không list) | REMOVE page. Khung UTC KHÔNG list. Adapt theo trường. |
| **C2 Non-khung section** | **NHẬN XÉT CỦA GVHD page** (nếu khung không list) | REMOVE page. Khung UTC KHÔNG list — NHẬN XÉT GVHD là giấy tờ riêng kèm DATN, không phải page trong docx body. Adapt theo trường. |
| **C2 Khung order strict** (added v2.0.0) — frontmatter sau Bìa+Bìa phụ PHẢI là Lời cảm ơn → Mục lục → Danh mục viết tắt+thuật ngữ → Danh mục bảng → Danh mục hình → Mở đầu (per khung UTC) | Mọi sequence khác (vd insert NHẬN XÉT/LỜI CAM ĐOAN/TÓM TẮT/ABSTRACT giữa Bìa phụ và Lời cảm ơn) | REMOVE inserted sections; preserve khung primary §1 exact order. |

---

## 4. Page count target (cap-based)

| Thesis level | Target trang | Soft deduct | Cap (auto-FAIL) |
|---|:---:|:---:|:---:|
| Cử nhân (bachelor) | 60-80 | 81-90 → -1 pt per 10 trang | >90 |
| Kỹ sư (engineer) | 80-110 | 111-120 → -1 pt per 10 trang | >120 |
| Thạc sĩ (master) | 120-180 | 181-200 → -1 pt per 10 trang | >200 |
| Tiến sĩ (PhD) | 150-300 | n/a — case-by-case advisor approval | n/a |

Rationale: pages within target = chấp nhận (concise scholarly writing valued); soft-deduct window allows minor over without category FAIL; >cap = forced reduction (committee bias against long thesis on assumption of unfocused scope).

---

## 5. Extension rules (8 supplemental constraints)

8 additional rules surfaced via 3-agent outside-in audit (persona simulation + benchmark + failure-mode matrix). Apply per chapter MD pre-ship:

### S1 — Single-child heading ban

Mọi heading cấp con (H3/H4/H5) phải có ≥2 sibling cùng cha. Nếu chỉ có 1 sub-heading → merge content lên parent OR add sibling thực sự.

**Verify:** grep `^### \d+\.\d+\.1\.` → if `\.1` exists but `\.2` doesn't → FAIL.

### S2 — Cấm chapter intro + summary sections

Mỗi chapter MD KHÔNG được có `## N.0 Giới thiệu chương` / `## Tóm tắt chương N` / `## Kết luận chương N`. Nội dung intro/summary nếu cần phải nằm trong section nội dung thực hoặc trong KẾT LUẬN chapter cuối.

**Verify:** grep `^## \d+\.0 |^## (Giới thiệu|Tóm tắt|Mục đích) chương` → 0 match required.

### S3 — Vietnamese narrative strict

Heading **PHẢI 100% tiếng Việt** — KHÔNG English heading (vd: `## 2.1.1 Domain capabilities` BANNED → `## 2.1.1 Năng lực miền`).

Body uyển chuyển — KHÔNG listing pattern "bao gồm X, Y, Z, A, B, C, D" (≥5 items in single sentence = listing-style anti-pattern). Chuyển sang prose-style narrative.

Khi dùng English term lần đầu → mở ngoặc bổ sung Vietnamese (vd: `JWT (JSON Web Token, mã xác thực web)`).

### S4 — Citation evidence mandate

Mọi numeric/factual claim trong narrative body PHẢI có citation `[N, tr.NNN]` hoặc `[N]`. Page-num bắt buộc cho:
1. Direct quote `"..."` từ source
2. Specific number/percentage cited as fact (vd "80% trung tâm chuyển khoản", "92/100 Lighthouse")
3. Specific vendor stat

Cite format `[N, tr.NNN]` cho document page-num; `[N]` cho web với access date `, truy cập DD/MM/YYYY` inline.

### S5 — Measurement methodology mandate

Mọi performance/benchmark/measurement claim PHẢI có methodology block trong cùng section:
1. **Tool**: tool/script đo (vd "Lighthouse 11.0", "JMeter 5.5")
2. **N (sample size)**: số measurement (vd "N=500 requests")
3. **Date**: thời gian đo (vd "đo DD/MM/YYYY lúc HH:MM UTC+7")
4. **Env**: môi trường (vd "production endpoint api.example.com", "4G mobile Saigon")

### S6 — Cross-reference integrity

Mọi cross-reference `§X.Y.Z` / `Chương N §X.Y` / `Hình X.Y` / `Bảng X.Y` trong narrative PHẢI verify anchor tồn tại. Pre-ship sweep:

```bash
for ref in $(grep -oE "§[0-9]+\.[0-9]+(\.[0-9]+)?" chapter-N.md); do
  grep -q "^## ${ref#§}\|^### ${ref#§}" chapter-N.md || echo "BROKEN: $ref"
done
```

### S7 — Acronym defined at first use

Mọi acronym 2+ ký tự (vd `GVCN`, `OIDC`, `DPO`, `DPIA`, `LMS`, `K-12`, `SaaS`) PHẢI được define tại first occurrence với format `<Acronym> (<full name VN>, <full name EN nếu cần>)`. Sau đó dùng acronym tự do.

Danh mục từ viết tắt riêng tốt nhưng KHÔNG thay được first-use definition.

### S8 — Figure source attribution mandatory

Mọi figure (markdown image, Mermaid block, embedded PNG) PHẢI có dòng italic source attribution NGAY SAU caption — TRỪ author-original figures (plain attribution optional):

| Figure type | Required attribution |
|---|---|
| **Derived from external source** | `*Nguồn: [N, tr.NNN]*` HOẶC `*Nguồn: <URL>, truy cập DD/MM/YYYY*` — **MANDATORY** |
| **Author-original** (drawn by author) | **OPTIONAL** — plain `*Nguồn: tác giả tự xây dựng*` treated as redundant fluff (sinh viên = author of thesis = default attribution implicit). **Composite reference** `*Nguồn: tác giả tự xây dựng dựa trên [N, tr.NNN]*` vẫn **MANDATORY** khi figure derived from external mô hình/framework. |
| **Screenshot of own product** | `*Nguồn: ảnh chụp giao diện <product>, truy cập DD/MM/YYYY*` — **MANDATORY** (timestamp evidence cho reproducibility) |

---

## 6. Worked self-test (template — apply to YOUR thesis V1)

Apply rubric to your thesis V1 retroactively. Compare estimated score against ≥75 C+ target.

| Category | Pts max | Your score | Notes |
|---|:---:|:---:|---|
| C1 Format | 15 | | logo present? margins? sub-section numbering strict? |
| C2 Content + page count | 15 | | within target cap? khớp khung primary 4 chương? KẾT LUẬN 2-3 trang gộp? |
| C3 Bibliography | 15 | | ≥30 refs? IEEE format? department giáo trình cited? |
| C4 Academic tone | 15 | | pronoun discipline? no "đối thủ"? no emoji? |
| C5 Project-internal scrub | 10 | | no "Claude"? no project gap IDs? no rule paths? |
| C6 Draft-marker scrub | 5 | | no TL;DR? no TODO? |
| C7 Diagram + figure | 10 | | Mermaid rendered as PNG? Hình X.Y captions? |
| C8 Examiner readiness | 10 | | architecture PNG embedded? real KPI? methodology? |
| C9 Compliance + legal | 5 | | no "vi phạm" admission? sample data anonymized? |
| **Total** | **100** | **/100** | Target ≥75 C+ |

**Path to ≥75 C+** depends on per-category gap. Common gains:
- C5 +5-8 (strip "Claude" + Wave/Phase + project IDs + rule paths)
- C7 +5-7 (Mermaid → PNG render pipeline + captions + cross-refs)
- C6 +3-5 (strip TL;DR + TODO + date heading)
- C4 +3-6 (strip emoji + "đối thủ" + pronoun lock)
- C2 +3-5 (remove non-khung sections per v2.0.0 if pipeline ships them; trim page count to target)

---

## 7. Enforcement (per `rule-change-process.md` §6.5)

### 7.1 Reviewer-checklist (active)

Pre-merge review cho PR touching `documents/<thesis-dir>/**`:

- [ ] Apply 9-category rubric §2 → estimated score documented in PR body
- [ ] Score ≥75/100 C+ for thesis V1+ release OR explicit "draft milestone" disclaimer
- [ ] C2 Khung compliance: structure khớp khung primary §1 (4 chương UTC pattern OR your school's equivalent); NON-khung sections removed (LỜI CAM ĐOAN/TÓM TẮT/ABSTRACT/NHẬN XÉT GVHD per UTC sample)
- [ ] C5 Project-internal scrub: grep `Claude\|Wave [0-9]\|Phase [0-9]\|\.claude/` returns 0 matches in chapter MDs body
- [ ] C6 Draft-marker scrub: grep `## TL;DR\|TODO\|FIXME\|placeholder\|\[stub\]` returns 0 matches
- [ ] C7 Diagram rendering: zero ```mermaid blocks in rendered docx (means rendered as image)
- [ ] Page count within target cap per §4 (cử nhân ≤80, kỹ sư ≤110, thạc sĩ ≤170)
- [ ] §5 extension rules S1-S8 applied per chapter MD

### 7.2 Audit standard

Post-thesis-ship audit MUST apply 9-category rubric. Earlier 6-category rubrics inadequate — missing 5 critical content-quality dimensions (C4 tone, C5 scrub, C6 draft, C7 diagram, C9 compliance).

### 7.3 Cross-reference `output-review-mandate.md` §3

Add matrix row "Thesis report / academic deliverable" tracking this rule's review standard.

### 7.4 CI detector (optional)

Future `scripts/check-thesis-content-standard.sh` — heuristic grep for banned patterns §3:

```bash
# Detect C5 project-internal references
grep -rnE "Claude|Wave [0-9]|Phase [0-9]+ BETA|\.claude/" \
  documents/<thesis-dir>/chapter-*.md 2>/dev/null \
  && { echo "WARN: project-internal references — strip per thesis-content-standard.md §3"; exit 0; }

# Detect C6 draft markers
grep -rnE "## TL;DR|TODO|FIXME|\[placeholder\]|\[stub\]" \
  documents/<thesis-dir>/chapter-*.md 2>/dev/null \
  && { echo "WARN: draft markers — strip per thesis-content-standard.md §3"; exit 0; }
```

### 7.5 Override mechanism

Genuine exception (vd thesis V1 milestone = "draft baseline" intentional):

```
git commit -m "...
THESIS_CONTENT_STANDARD_OVERRIDE: <file> — <reason — e.g., 'V1 = content draft milestone; V2 fix-pass closing scope gaps'>"
```

Trailer logged. Pattern frequency >10%/quarter triggers meta-review.

---

## 8. Anti-patterns

| ❌ Don't | ✅ Do |
|---|---|
| Re-use 6-category rubric cho thesis audit (misses 4 critical dimensions) | Apply 9-category rubric §2 |
| Ship thesis với "Claude" / "Wave N" / project gap IDs trong body narrative | Strip prior ship; bibliography vendor ref OK |
| Treat `## TL;DR` section as legitimate academic convention | Strip — content flows directly từ Mở đầu vào chương |
| Mermaid code rendering as text trong DOCX | Render to PNG via headless browser pipeline; embed image |
| 110-page bachelor thesis "vì nhiều content tốt" | Trim to ≤70 trang — committee bias against verbose theses regardless content quality |
| Use "đối thủ" (business jargon) trong khóa luận | "đối tượng tham khảo" / "công trình nghiên cứu liên quan" academic phrasing |
| **Add TÓM TẮT page riêng** khi khung trường KHÔNG list (vd UTC sample) | ❌ v2.0.0 — khung chuẩn nguyên bản trường mandate sections. Mở đầu §1 đã cover lý do + tóm tắt nội dung. Adapt theo source-of-truth của trường bạn. |
| **Add LỜI CAM ĐOAN page** khi khung trường KHÔNG list | ❌ v2.0.0 — adapt theo source-of-truth của trường bạn. UTC sample không list. |
| **Add ABSTRACT EN page riêng** khi khung trường KHÔNG list | ❌ v2.0.0 — adapt theo trường. UTC sample không list. |
| **Add NHẬN XÉT GVHD page trong docx body** khi khung trường KHÔNG list | ❌ v2.0.0 — NHẬN XÉT GVHD thường là giấy tờ riêng kèm DATN, không phải page trong thesis docx. Adapt theo trường. |
| **Tách Danh mục từ viết tắt riêng + Danh mục thuật ngữ riêng** | ❌ v2.0.0 — khung UTC nguyên bản GỘP "Danh mục các từ viết tắt, thuật ngữ" 1 heading. Adapt theo trường. |
| Use `[LOGO ...]` placeholder fallback in production thesis | Embed actual PNG; verify visible via LibreOffice/Word render check |

---

## 9. Relationship to other rules

- **`output-review-mandate.md`** §3 — add row "Thesis report / academic deliverable" tracking this rule's review standard
- **`dev-readable-doc-language.md`** §2-§4 — Vietnamese narrative + English identifier; this rule extends WITH academic-tone discipline cho thesis scope (more strict than general dev docs)
- **`user-manual-content-standard.md`** — sister rule cho end-user manual scope (similar checklist pattern)
- **`incident-to-rule-pipeline.md`** — this rule applies 5-stage pipeline cho thesis-quality misses
- **`rule-change-process.md`** §5.1 atomic-unique bar — passed: ✅ atomic / ✅ unique / ✅ widely applicable
- **`rule-change-process.md`** §6.5 Enforcement Parity Mandate — rule + rubric + reviewer-checklist + worked self-test all same PR
- **`meta-gap-priority.md`** §3 — META P0 force-multiplier (1 rubric → mọi thesis ship subsequent auto-comply)

---

## 10. Companion skills

Two paired skills ship same kit version provide tooling cho thesis V1+ workflow:

- **`skills/quality/thesis-citation-extract/SKILL.md`** — Parse `[N]` cite keys từ chapter markdown + verify chéo với `documents/<thesis-dir>/references/bibliography.md` → báo 3 bucket (matched / orphan-body / orphan-bib). Closes C3 Bibliography category audit gap.
- **`skills/quality/thesis-figure-curation/SKILL.md`** — Codifies selection criteria (figure vs table vs prose), per-chapter numbering `N.M`, Vietnamese caption format, per-chapter INDEX generation. Closes C7 Diagram + figure rendering category audit gap.

Use this rule as the rubric; use the skills as the audit tooling.

---

## 11. Log

- **2026-06-04 (v2.0.0):** MAJOR — re-ground khung chuẩn nguyên bản UTC theo source-of-truth chính thức (`khung-bao-cao-do-an.png` from school) thay BCTT sample. §1 grounding sources reorder: PRIMARY khung-bao-cao-do-an (mọi required section list mandate from this image) + SECONDARY UTC PDF spec (format details A4/TNR/margins/SEQ) + TERTIARY samples (visual reference only, NOT source for required-sections list). MAJOR constraint removals (per `rule-change-process.md` §4 semver — removing constraints = MAJOR bump): (1) **LỜI CAM ĐOAN** dropped from required — khung UTC nguyên bản KHÔNG list. (2) **TÓM TẮT VN abstract page riêng** dropped — không trong khung. (3) **ABSTRACT EN page riêng** dropped — không trong khung. (4) **NHẬN XÉT GVHD page** dropped — không trong khung (giấy tờ riêng kèm DATN, không phải docx page). C2 chapter structure updated khớp khung 4 chương UTC exact (Ch.1 Tổng quan / Ch.2 Phân tích thiết kế / Ch.3 chương chính / Ch.4 Đánh giá). §3 Banned patterns extended với 6 NEW rows codifying non-khung sections + khung order strict. §7 Anti-patterns updated: REMOVED "Skip TÓM TẮT" + "Skip LỜI CAM ĐOAN" rows; ADDED 5 NEW anti-patterns banning addition of these non-khung sections (with adapt note cho universities ngoài UTC). §10 Companion skills extended với 2 new paired skills `thesis-citation-extract` + `thesis-figure-curation` (closes C3 + C7 audit gaps). Universities ngoài UTC adapt theo source-of-truth của trường — khung primary §1 PRIMARY là điểm bám điều chỉnh. Existing thesis V1 grandfathered cho tới next refresh; rule applies prospectively. Reviewer: @<starter-kit-upstream-maintainer> (starter-kit upstream maintainer — MAJOR self-approve allowed per `rule-change-process.md` §5 in upstream solo-maintainer mode; constraint loosening explicitly intended to align với khung chuẩn nguyên bản source-of-truth thay BCTT sample mismatch).

- **2026-05-23 (v1.0.0):** Rule promoted to upstream starter-kit từ downstream project source (original v1.0.0..v1.1.0 ship 2026-05-19..05-20 trên Vietnamese-university bachelor thesis sprint). Light-scrub applied: UTC-specific → generic "your school's spec PDF"; project paths (`documents/<thesis-dir>/` / `documents/**/audits/`) → placeholder; project tracking IDs / wave names → generic phrasing; project-specific personas + sample data → generic VN edu placeholders. Rubric structure (9 categories /100), 8 extension rules (S1-S8), banned patterns matrix preserved unchanged — those represent generalizable academic-writing constraints. Reviewer: @<starter-kit-upstream-maintainer> (starter-kit upstream maintainer — MINOR self-approve per `rule-change-process.md` §5; new rule with built-in enforcement per §6.5 Enforcement Parity Mandate; no constraint loosening). Companion skill workflow shipped same kit version per §10.
