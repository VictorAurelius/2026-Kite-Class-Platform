---
paths:
  - "documents/<thesis-dir>/**"
  - "documents/**/audits/persona-review/*-thesis-*"
  - ".claude/rules/thesis-content-standard.md"
---

# Thesis Content Standard — academic-quality review rubric cho khóa luận tốt nghiệp

**Priority:** 🟠 MANDATORY — academic deliverable governance
**Version:** 1.0.0
**Created:** 2026-05-23
**Last-Reviewed:** 2026-05-23
**Reviewer-Approver:** @nguyenvankiet (starter-kit upstream maintainer — distilled từ Vietnamese-university bachelor thesis sprint upstream; original v1.0.0..v1.1.0 of source rule ship 2026-05-19..05-20 — see CHANGELOG v2.6.0)
**Applies to:** Mọi file dưới `documents/<thesis-dir>/**` (vd `documents/08-thesis/`) được render thành DOCX/PDF deliverable cho academic submission. Scope = chapter MDs + final DOCX + bibliography + audit reports thesis-related. Out-of-scope: source code, internal runbooks, non-academic project docs.

**Opinionated scope:** Rule này **opinionated cho convention khóa luận đại học Việt Nam** (UTC, HUST, UET, HCMUT, BKHN — convention tương tự). Universities ngoài VN cần adapt: page-size + heading numbering + bibliography style + page-count target.

---

## 1. The Rule

> **Mọi thesis V1+ ship phải đạt ≥75/100 C+ trên 9-category rubric §2.** Format compliance + content + bibliography đã ổn nhưng dễ bỏ sót 5 critical content-quality dimensions: academic tone discipline, project-internal reference scrub, draft-marker scrub, diagram-as-image verification, compliance/legal sensitivity. Rubric 9-category này codify all dimensions để eliminate blind spots.

Force-multiplier rationale: 1 chuẩn rubric → mọi thesis V1+ V2+ subsequent auto-comply → eliminate retroactive content-quality rework cost.

**Grounding sources (consulted at rule design — these are the canonical sources YOUR project should ground in):**

1. **Your school's thesis format spec PDF** (e.g., "Quy định trình bày đồ án tốt nghiệp" cho UTC) — de jure standard (font / margin / numbering / IEEE format)
2. **A known-good DOCX sample** từ your school's archive (production-quality báo cáo thực tập hoặc đồ án từ sinh viên trước) — de facto convention
3. **Persona simulation audit** — outside-in audit với 3-agent roleplay (GVHD + GVPB + Defense committee) trên thesis draft
4. **User-flagged inspection issues** — direct review by author / advisor pre-ship

---

## 2. The 9-category rubric / 100 points

### C1 — Format compliance (15 points)

Tuân thủ your school's spec + match known-good DOCX sample conventions:

| Sub-criterion | Pts | Verify |
|---|:---:|---|
| **A4 page size (210×297mm) explicit** per spec | 2 | `section.page_width == Cm(21.0)` + `page_height == Cm(29.7)` (python-docx). Note: many python-docx pipelines default US Letter — STRICT mandate A4 per VN spec. |
| Times New Roman 13pt body / 14pt H3 / 16pt H2 / 18pt H1 per spec | 3 | python-docx introspection on Heading styles. H4 + H5 sub-sub-headings nếu used must be 13pt bold (typical VN spec define 3 levels — over-deep sectioning anti-pattern). |
| Margins top 2.5 / bottom 2.5 / left 3.0 / right 2.0 cm | 2 | `section.top_margin == Cm(2.5)` etc. Standard VN convention. |
| Binding gutter (offset for binding edge) + bìa cứng instructions | 1 | `section.gutter > 0` OR explicit printer instructions trong handover doc cho gáy in "HỌ TÊN - LỚP - NĂM" |
| **Cover page với school logo embedded (NOT placeholder text)** | 2 | docx inline_shape inspection — actual PNG present, NOT `[LOGO ...]` fallback string. **Hard FAIL if fallback string visible** |
| Bìa phụ — bìa phụ PHẢI có khung info 6-field chuẩn (Sinh viên / MSSV / Lớp / Khóa / GVHD / GVPB) NOT trùng lặp bìa chính | 2 | Bìa phụ phải DIFFERENT từ bìa chính. Bìa chính = title-focused; bìa phụ = info-focused. |
| Section numbering strict per spec: chapter `CHƯƠNG N.` + section `N.M` + subsection `N.M.P` | 1 | `A.2.4`, `B.5` (alpha prefix) BANNED trong thesis numbering. Strict numeric only. |
| Table caption `Bảng X.Y. ...` + figure caption `Hình X.Y. ...` numbering gắn chương | 1 | SEQ field `add_table_caption` integration required (python-docx). |
| TOC + Danh mục hình + Danh mục bảng + Danh mục thuật ngữ + Danh mục từ viết tắt populated (NOT placeholder text) | 1 | Placeholder "(Bấm Ctrl+A rồi F9...)" trong file in nộp = draft signal. Auto-populate via python-docx XML settings hoặc post-process F9 trước nộp. |

**Verdict thresholds:** ≥13 PASS / 10-12 PARTIAL / <10 FAIL.

### C2 — Content completeness + page count target (15 points)

Per VN university bachelor thesis convention + advisor expectation:

| Sub-criterion | Pts | Verify |
|---|:---:|---|
| Đầy đủ Mở đầu + 4-6 chương nội dung + Kết luận + Phụ lục | 4 | Heading 1 count + section names. VN convention cử nhân CNTT thường 5-6 chương (Tổng quan / Cơ sở lý thuyết / Phân tích yêu cầu / Thiết kế / Triển khai / Kết luận). Ghép Phân tích + Thiết kế + Kiến trúc vào 1 chương = anti-pattern. |
| Page count target per §4 (cử nhân 60-80 / kỹ sư 80-110 / thạc sĩ 120-180). **Cap auto-FAIL:** cử nhân >90 trang | 4 | LibreOffice page-count read-back. Soft deduct 1 pt per 10 trang vượt upper bound. |
| Nội dung chính balance chương (no chương quá dài/quá ngắn 2-3x lệch) | 3 | Variance check across chapter paragraph counts. Soft deduct nếu chương lớn nhất > 2× chương nhỏ nhất. |
| KẾT LUẬN VÀ KIẾN NGHỊ độ dài 2-3 trang minimum + 4 sub-sections (Tổng kết / Hạn chế / Hướng phát triển / **Đóng góp khoa học** explicit) | 2 | VN convention requires "đóng góp khoa học" explicit, NOT chỉ tổng kết + hạn chế + hướng phát triển. List 1-2 contributions methodological hoặc empirical novel. |
| Trim repo-internal retrospective content khỏi chapter body (vd "Lessons learned" + "Feature scope cut" + production incident timelines) | 2 | Chuyển retrospective insights vào KẾT LUẬN gói gọn, đỡ duplicate. Body academic prose-only. |

**Verdict thresholds:** ≥13 PASS / 10-12 PARTIAL / <10 FAIL. **Page count cap:** >90 trang cử nhân auto-FAIL category regardless other sub-criteria.

**Re Abstract VN/EN + Lời cam đoan:** OPTIONAL cho cử nhân scope (many VN samples không có); recommended cho thạc sĩ+ scope. Adapt to your school's convention.

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
| KHÔNG gap IDs ("GAP-XXX", project tracking IDs) | 1 | Strip hoàn toàn — không phải reference cho academic |
| KHÔNG rule/skill internal file paths (`.claude/rules/*.md`, `.claude/skills/*/SKILL.md`) | 1 | grep `\.claude/` returns 0 |
| KHÔNG repo internal status markers ("DONE", "PARTIAL", "OPEN", "DEFER Wave X+", "audit X/100") | 1 | grep banned status patterns 0 in body |
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
| C5 Project-internal | "GAP-XXX" hoặc project tracking IDs | strip hoàn toàn |
| C5 Project-internal | `.claude/rules/*.md`, `.claude/skills/**` paths | strip hoàn toàn |
| C5 Standalone | Internal `documents/**` path references | Restate content inline OR cite published source |
| C5 Standalone | Bibliography refs từ internal repo (audit reports / chapter MD cross-refs / plans) | Replace với public source OR strip claim |
| C9 Compliance | "vi phạm Nghị định 53/2022" / "compliance debt được chấp nhận" explicit | Defensive phrasing "chưa kích hoạt ngưỡng X; roadmap migrate Y" |
| C9 Compliance | Sample data tên thật không có consent | Suffix "(tên giả định)" |
| **C1 No-icon principle** — banned chars trong body narrative | ✅ ✗ ❌ ⚠️ 🎉 🚀 📅 🆘 🔴 🟢 🟡 🟠 ▪️ ◆ ■ ▲ ⇒ ⇐ → ← (emoji + arrow + colored circle) | "đạt yêu cầu" / "không đạt" / " - " / " thì " narrative phrasing |
| **C1 Character set allowed** | non-typeable special chars | Vietnamese alphabet (a-z, đ, dấu thanh) + Latin alphabet + chữ số + standard punctuation |
| **C4 No-font-swap principle** — KHÔNG đổi font inline | Courier New cho `inline code` markdown / Cambria cho equations | Spec mandates TNR 13pt mọi đoạn văn body. Emphasis dùng *italic* / **bold** / UPPERCASE / "ngoặc kép" |
| **C4 Inline code rendering** | `instance_id` rendered with Courier New | `instance_id` rendered TNR italic OR UPPERCASE `INSTANCE_ID` per VN academic convention |
| C6 Draft-marker | `## TL;DR` | Strip — academic abstract goes in TÓM TẮT page riêng |
| C6 Draft-marker | `TODO`, `FIXME`, `XXX`, `[placeholder]`, `[stub]` | "[Đang thu thập số liệu — sẽ cập nhật trước defense]" honest form OR strip |
| C6 Draft-marker | `**Cập nhật lần cuối:** YYYY-MM-DD` | Strip from heading (move to frontmatter) |
| C6 Draft-marker | `v0.X-beta`, `v1.0.0-rc` version markers | Strip |

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

Mọi figure (markdown image, Mermaid block, embedded PNG) PHẢI có dòng italic source attribution NGAY SAU caption:

| Figure type | Required attribution |
|---|---|
| **Derived from external source** | `*Nguồn: [N, tr.NNN]*` HOẶC `*Nguồn: <URL>, truy cập DD/MM/YYYY*` |
| **Author-original** | `*Nguồn: tác giả tự xây dựng*` HOẶC `*Nguồn: tác giả tự xây dựng dựa trên [N, tr.NNN]*` |
| **Screenshot of own product** | `*Nguồn: ảnh chụp giao diện <product>, truy cập DD/MM/YYYY*` |

---

## 6. Worked self-test (template — apply to YOUR thesis V1)

Apply rubric to your thesis V1 retroactively. Compare estimated score against ≥75 C+ target.

| Category | Pts max | Your score | Notes |
|---|:---:|:---:|---|
| C1 Format | 15 | | logo present? margins? sub-section numbering strict? |
| C2 Content + page count | 15 | | within target cap? KẾT LUẬN 2-3 trang? đóng góp khoa học explicit? |
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

---

## 7. Enforcement (per `rule-change-process.md` §6.5)

### 7.1 Reviewer-checklist (active)

Pre-merge review cho PR touching `documents/<thesis-dir>/**`:

- [ ] Apply 9-category rubric §2 → estimated score documented in PR body
- [ ] Score ≥75/100 C+ for thesis V1+ release OR explicit "draft milestone" disclaimer
- [ ] C5 Project-internal scrub: grep `Claude\|Wave [0-9]\|Phase [0-9]\|GAP-[0-9]\|\.claude/` returns 0 matches in chapter MDs body
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
grep -rnE "Claude|Wave [0-9]|Phase [0-9]+ BETA|GAP-[0-9]|\.claude/" \
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
| Ship thesis với "Claude" / "Wave N" / "GAP-XXX" trong body narrative | Strip prior ship; bibliography vendor ref OK |
| Treat `## TL;DR` section as legitimate academic convention | Strip — academic TÓM TẮT is a separate page format |
| Mermaid code rendering as text trong DOCX | Render to PNG via headless browser pipeline; embed image |
| 110-page bachelor thesis "vì nhiều content tốt" | Trim to ≤70 trang — committee bias against verbose theses regardless content quality |
| Use "đối thủ" (business jargon) trong khóa luận | "đối tượng tham khảo" / "công trình nghiên cứu liên quan" academic phrasing |
| Skip TÓM TẮT page (claim "có trong Mở đầu") | TÓM TẮT separate page per VN convention |
| Single "DANH MỤC THUẬT NGỮ VÀ TỪ VIẾT TẮT" gộp | 2 danh mục TÁCH BIỆT |
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

## 10. Companion skill

Skill `document-generation/thesis/SKILL.md` (paired same kit version) provides workflow: planning → draft → 3-agent outside-in audit → fix → ship. Use this rule as the rubric; use the skill as the process.

---

## 11. Log

- **2026-05-23 (v1.0.0):** Rule promoted to upstream starter-kit v2.6.0 từ downstream project source (original v1.0.0..v1.1.0 ship 2026-05-19..05-20 trên Vietnamese-university bachelor thesis sprint). Light-scrub applied: UTC-specific → generic "your school's spec PDF"; project paths (`documents/08-thesis/` / `documents/04-quality/audits/`) → placeholder `<thesis-dir>` / `<your-audits-dir>`; project gap IDs / wave names → generic phrasing; project-specific personas + sample data → generic VN edu placeholders. Rubric structure (9 categories /100), 8 extension rules (S1-S8), banned patterns matrix preserved unchanged — those represent generalizable academic-writing constraints. Reviewer: @nguyenvankiet (starter-kit upstream maintainer — MINOR self-approve per `rule-change-process.md` §5; new rule with built-in enforcement per §6.5 Enforcement Parity Mandate; no constraint loosening). Companion skill `document-generation/thesis/SKILL.md` ships same kit version per §10.
