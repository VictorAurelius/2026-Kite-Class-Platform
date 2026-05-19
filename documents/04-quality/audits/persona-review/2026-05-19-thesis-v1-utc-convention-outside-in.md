---
title: Thesis V1 UTC Convention Outside-In Audit (Wave 102.2 prep)
status: complete
created: 2026-05-19
audience: dev
phase: phase-1-beta
gaps: [GAP-688, GAP-687]
audit_id: AUDIT-2026-05-19-thesis-v1-utc-convention
artifact: documents/08-thesis/thesis-v1.docx
---

# Thesis V1 UTC Convention Outside-In Audit — 3 questions

## Scope

Verify 3 user-flagged design questions cho Wave 102.2 thesis content edit pass bằng UTC official convention + sample DOCX + persona reasoning. Output: ranked recommendations với evidence cite-able từ UTC primary sources.

3 questions:
1. BRD section trong Ch.2 required cho cử nhân CNTT thesis?
2. "Công nghệ sử dụng" section required + đặt ở đâu?
3. Cut Phần A pháp lý + Phần B methodology trong Ch.1 Part 3 safe?

## Sources consulted

### UTC official spec (primary)
- `documents/07-archived/academic/word-reports/templates/Quy dinh trinh bay do an tot nghiep.pdf` — UTC official §1 Quy định bố cục báo cáo + §2.1-2.5 trình bày + §3 hướng dẫn trích dẫn (đã extract via pdfminer.six full text 13k chars)
- `Mau-Decuong DATN-Cử nhân.pdf` — UTC bachelor proposal blank template (4 sections: Nội dung+phạm vi / Công nghệ+công cụ+ngôn ngữ / Kết quả dự kiến / Kế hoạch)
- `CUONG_THAMKHAO_Decuong DATN-DuThao.pdf` — UTC sample đề cương cử nhân (Đặng Hữu Cương "Hệ thống học tập trực tuyến cá nhân hóa", GVHD TS. Nguyễn Đức Dư) — section 2 "Công nghệ, công cụ và ngôn ngữ lập trình" rất chi tiết với 2.1 Ngôn ngữ / 2.2 Framework / 2.3 Công cụ phát triển + triển khai / 2.4 Lý do lựa chọn
- `CUONG_THAMKHAO_BaoCaoTTTN-DuThao.pdf` — UTC sample báo cáo TTTN cử nhân (cùng tác giả) — 4 sections: 1. Giới thiệu đơn vị / 2. Nội dung thực tập (2.4 = Công nghệ, công cụ, kỹ thuật sử dụng) / 3. Kết quả + đánh giá / 4. Nhận xét + định hướng
- `DUC_THAMKHAO_Báo cáo thực tập tốt nghiệp.pdf` — UTC sample khoa Đào tạo Quốc tế (Trịnh Công Đức "Abivin")
- `Huong dan trinh bay bao cao TTTN.pdf` — Hướng dẫn TTTN blank template (mirror sample structure)

### Project-internal (secondary)
- `documents/07-archived/academic/word-reports/de-cuong-datn/DE_CUONG_DATN.docx` — Đề cương cá nhân Nguyễn Văn Kiệt 221230890 đã ship 2026-04 (4 sections KHỚP UTC blank template — section 2 "Công nghệ, công cụ và ngôn ngữ lập trình" liệt kê stack đầy đủ)
- `documents/08-thesis/chapter-2-system-architecture.md` (609 lines, ~430 paragraphs Vietnamese narrative)
- `documents/08-thesis/chapter-1-vn-law-methodology.md` (171 lines Phần A pháp lý + Phần B methodology)
- `documents/08-thesis/references/technology-stack.md` (full stack reference đã có sẵn, ~250 lines)
- `documents/08-thesis/chapter-mapping.md` (V1 status snapshot Wave 100.7 closure)

### Rule context
- `.claude/rules/thesis-content-standard.md` v1.0.2 §2 C2 Content completeness — page count cap cử nhân ≤90 trang + ≥4-6 chương + đầy đủ Mở đầu + Kết luận + Phụ lục
- `.claude/rules/thesis-content-standard.md` §2 C9 Compliance + legal sensitivity — DPO + DPIA + Decree 53 roadmap MUST explicit (KHÔNG cut hết)
- `.claude/rules/dev-readable-doc-language.md` §2 — narrative Vietnamese + English identifiers

---

## Question 1: BRD section trong Ch.2 required cho cử nhân CNTT thesis?

### Finding

**UTC convention KHÔNG mandate explicit "BRD" (Business Requirements Document) section trong thesis cử nhân CNTT.**

Bằng chứng từ 4 sources:

1. **UTC spec PDF §1** — quy định bố cục liệt kê: LỜI CẢM ƠN / MỤC LỤC / DANH MỤC viết tắt + bảng + hình / MỞ ĐẦU / **CHƯƠNG 1: (tuỳ theo đề tài của các lĩnh vực khác nhau)** / CHƯƠNG 2: (tuỳ đề tài) / CHƯƠNG 3 / KẾT LUẬN VÀ KIẾN NGHỊ / TÀI LIỆU THAM KHẢO / PHỤ LỤC. Spec EXPLICIT cho phép tuỳ chương theo đề tài — không mandate BRD.

2. **UTC sample đề cương cử nhân (CUONG_Decuong-DuThao)** — section 1 "Nội dung, phạm vi của đề tài" chỉ có bullet liệt kê chức năng cốt lõi (đăng ký + login + quản lý + recommendation) + section "Phạm vi" 2 đoạn. KHÔNG có separate BRD heading.

3. **UTC sample báo cáo TTTN (CUONG_BaoCaoTTTN)** — section 2.1 "Mục tiêu và yêu cầu của đợt thực tập" 1 paragraph. KHÔNG có BRD format.

4. **Đề cương Nguyễn Văn Kiệt 2026-04** — section 1 "Nội dung + phạm vi" 4 bullet content + 3 bullet phạm vi. Style consistent với UTC template — đã được GVHD TS. Nguyễn Đức Dư approve qua đề cương submission. KHÔNG có BRD format.

### Phân tích định nghĩa

User confuse có thể do thấy thuật ngữ "BRD" trong industry SaaS docs hoặc trong dự án (`documents/00-brd/` folder). Trong academic convention VN:
- **"Phân tích yêu cầu"** = umbrella covering FR + NFR (đã có §2.1 + §2.2)
- **Business context** thường nằm ở Ch.1 MỞ ĐẦU + Ch.1 Phần 1 (Tổng quan thị trường, đối thủ, lý do chọn đề tài)
- **Bussiness objectives** thường nằm trong "Mục tiêu nghiên cứu" của MỞ ĐẦU
- **Stakeholders + persona** thường nằm trong Ch.2 §2.1 Yêu cầu chức năng (đã có)

Ch.2 hiện tại Kite Platform thesis đã cover đầy đủ:
- §2.1 FR (6 nhóm capability, actor + persona inferred) ≈ BRD "Functional scope + stakeholders"
- §2.2 NFR ISO 25010 (6 hạng mục) ≈ BRD "Constraints + quality attributes"
- §2.3 Architecture (C4 + RLS + auth)
- §2.4 Mô hình SaaS (lifecycle + billing + plan tier) ≈ BRD "Business model + pricing"
- §2.5 Bối cảnh Blended Learning ≈ BRD "Market context"

### Recommendation Q1

**NO — KHÔNG cần thêm BRD section trong Ch.2.** Current §2.1+§2.2+§2.4+§2.5 đã cover toàn bộ scope mà thuật ngữ "BRD" hàm ý trong academic context.

Nếu muốn defensive hơn cho committee, có 2 micro-action không xáo trộn structure:

- **Option 1A (zero-effort):** Rename §2.1 từ "Yêu cầu chức năng (Functional Requirements)" thành **"Phân tích yêu cầu nghiệp vụ"** với 2 sub-sections `§2.1.1 Yêu cầu chức năng (FR)` + `§2.1.2 Bối cảnh nghiệp vụ và stakeholder` (move §2.5 Blended Learning context lên thành §2.1.2). Reader thấy "Phân tích yêu cầu nghiệp vụ" ≡ "BRD" về mặt scope.

- **Option 1B (1 paragraph add):** Thêm 1 paragraph mở đầu §2.1 cite explicit: *"Chương 2 trình bày phân tích yêu cầu nghiệp vụ và kiến trúc hệ thống Kite Platform. §2.1 phân tích yêu cầu chức năng theo 6 nhóm capability + actor; §2.2 yêu cầu phi chức năng theo ISO 25010; §2.3 kiến trúc đáp ứng các yêu cầu này; §2.4-2.5 trình bày mô hình SaaS và bối cảnh nghiệp vụ. Cách tiếp cận này tương đương với một Business Requirements Document chuẩn hóa cho domain SaaS giáo dục."* Smooth-out term + defensive cho committee chất vấn.

Recommended: **Option 1B** (low cost, defensive value cao, không xáo trộn structure).

---

## Question 2: "Công nghệ sử dụng" section required + ở đâu?

### Finding

**YES — UTC convention MANDATE explicit "Công nghệ sử dụng" section cho thesis cử nhân CNTT.** Bằng chứng EXPLICIT từ 4 sources:

1. **UTC blank template đề cương cử nhân (`Mau-Decuong DATN-Cử nhân.pdf`)** — section 2 PHÁP ĐỊNH là `"2. Công nghệ, công cụ và ngôn ngữ lập trình"` — section heading có sẵn trong template trống, chứng tỏ là MANDATORY field cho cử nhân CNTT scope.

2. **UTC sample đề cương cử nhân (CUONG_Decuong-DuThao)** — section 2 chi tiết với 4 sub-sections:
   - 2.1 Ngôn ngữ lập trình (TypeScript/JavaScript + Python)
   - 2.2 Framework và nền tảng chính (Frontend Next.js / Backend FastAPI / Database PostgreSQL / AI ML)
   - 2.3 Công cụ hỗ trợ phát triển và triển khai (IDE + Git + Test + Deploy + AI dev tools)
   - 2.4 Lý do lựa chọn bộ công nghệ này (xu hướng + dễ triển khai + tính ứng dụng + hạn chế)

3. **UTC sample báo cáo TTTN (CUONG_BaoCaoTTTN)** — section 2.4 `"Công nghệ, công cụ và kỹ thuật sử dụng"` — sub-section của Nội dung thực tập, cũng là MANDATORY trong báo cáo TTTN.

4. **Đề cương Nguyễn Văn Kiệt 2026-04** (DE_CUONG_DATN.docx) — section 2 `"Công nghệ, công cụ và ngôn ngữ lập trình"` đã liệt kê:
   - Backend: Java 21 LTS, Spring Boot 3.2, Spring Security, PostgreSQL 15, Redis 7.x
   - Frontend: Next.js 14, React, TypeScript
   - AI: OpenAI GPT-4, DALL-E 3
   - DevOps: Docker, K8s (AWS EKS), GitHub Actions, Terraform
   - Tools: Git/GitHub, IntelliJ, VS Code, Prometheus/Grafana

→ **Đây là MUST-HAVE section đã có ở đề cương ship 2026-04**, nhưng V1 thesis hiện tại **CHƯA có dedicated section** — tech stack mentions scattered Ch.2 §2.3 (architecture context) + Ch.3 code snippets + Ch.4 deployment. Đây là **content completeness gap thực sự**, không phải over-engineer.

### Vị trí placement

3 options điển hình trong VN academic convention:

| Option | Vị trí | Pros | Cons |
|---|---|---|---|
| **A** | Ch.1 Phần 4 mới (sau Phần 3 hiện tại) | Match UTC đề cương convention (section 2 sau Phần "Nội dung + phạm vi") | Tăng độ dài Ch.1 (hiện 3 Parts → 4 Parts) |
| **B** | Ch.2 §2.6 mới (cuối chương) | Logical flow: §2.3 Architecture (context) → §2.6 Stack (implementation lựa chọn) | Ch.2 đã dày ~430 paragraphs — risk vượt 90-trang cap |
| **C** | Ch.3 §3.1 mới (đầu chương Implementation) | Logical: stack precedes code snippets; Ch.3 hiện đầu bằng tree-view phù hợp với stack listing | Risk Ch.3 mất focus snippets-only |

### Granularity required

UTC sample cho thấy granularity **per phase** style là OPTIONAL — sample đề cương dùng granularity per layer (Ngôn ngữ + Framework + Công cụ + Lý do). User hỏi "công nghệ cho giai đoạn thiết kế, lập trình, kiểm thử, triển khai" — đây là cách phân chia theo SDLC phase, KHÁC với UTC sample (phân theo layer + tool category).

**2 styles đều acceptable**, nhưng UTC sample dùng layer-based:
- **Layer-based** (UTC convention dominant): Backend / Frontend / Database / DevOps / Testing / AI — match `references/technology-stack.md` structure đã có
- **Phase-based** (user proposal): Design / Development / Testing / Deployment / Monitoring — phù hợp khi muốn show methodology + lifecycle thinking

Recommend: **layer-based với 1 column "Phase" bổ sung trong table** = hybrid match UTC convention + add phase context user requested.

### Recommendation Q2

**ADD Ch.3 §3.1 "Công nghệ, công cụ và môi trường phát triển" (~2-3 trang)** với 4-5 sub-sections theo layer convention UTC:

```
§3.1 Công nghệ, công cụ và môi trường phát triển
  §3.1.1 Ngôn ngữ lập trình (Java 21 LTS / TypeScript 5.7 / SQL)
  §3.1.2 Framework chính (Spring Boot 3.5 + Next.js 15 + React 19)
  §3.1.3 Cơ sở dữ liệu và caching (PostgreSQL 16 + Redis 7 + Flyway)
  §3.1.4 Công cụ phát triển và triển khai (IDE + Git + Docker + GitHub Actions + Terraform + AWS)
  §3.1.5 Công cụ kiểm thử và đảm bảo chất lượng (JUnit 5 + Mockito + Testcontainers + Vitest + Playwright + MSW)
  §3.1.6 Lý do lựa chọn bộ công nghệ
```

Lý do chọn Ch.3 (Option C) thay vì Ch.1 hoặc Ch.2:

- Ch.1 Phần 4 mới làm thay đổi cấu trúc Phần đã establish (Phần 1/2/3) — friction cao
- Ch.2 §2.6 tăng Ch.2 đã dày (~15-20 trang) → risk page count cap
- Ch.3 §3.1 đặt stack TRƯỚC code snippets là sequence logical nhất: reader đọc stack overview → context cho 5 code snippets sau đó. Match `references/technology-stack.md` đã có nội dung (chỉ cần compress + Vietnamese translate + add "Lý do" sub-section)

Cost estimate: 2-3 trang. Reuse trực tiếp `references/technology-stack.md` content, không phải research lại.

Self-test cite UTC source: *"Section 3.1 áp dụng convention UTC theo `Mau-Decuong DATN-Cử nhân.pdf` template section 2 (Công nghệ, công cụ và ngôn ngữ lập trình)"* — defensive cho committee.

---

## Question 3: Cut Phần A pháp lý + Phần B methodology trong Ch.1 Part 3 safe?

### Finding

**RISK ASSESSMENT cao cho cả 2 phần — KHUYẾN NGHỊ KHÔNG CUT cả hai phần đồng thời.** Bằng chứng + reasoning:

#### 3.1 Phần A — Khung pháp lý VN

**Risk CUT toàn bộ:** 🔴 HIGH

Lý do:
1. **UTC convention cử nhân CNTT KHÔNG explicit mandate** Section "Khung pháp lý" — đề cương Kiệt + sample CUONG_Decuong KHÔNG có section pháp lý dedicated.

2. **NHƯNG** thesis Kite Platform là **multi-tenant SaaS edu xử lý dữ liệu cá nhân + dữ liệu trẻ em (K-12 roadmap)** — domain cực kỳ nhạy cảm về compliance. Committee VN academic 2026 ĐANG nhạy với PDPL 2023 hiệu lực 2026-07-01 (5 tuần countdown từ defense estimate).

3. **`thesis-content-standard.md` v1.0.2 §2 C9 Compliance + legal sensitivity (5 points)** — rule EXPLICIT mandate cite các yêu cầu sau cho thesis B+ score:
   - PDPL 2023 + Cybersecurity 2018 + Decree 13/53/2022 + Thông tư 78/2021 references trong bibliography
   - DPO + DPIA roadmap explicit
   - Data localization rationale (Singapore vs Hanoi)
   - Sample data anonymization

4. **Persona-simulation audit Wave 102 (`2026-05-19-thesis-v1-persona-simulation-outside-in.md`)** đã flag finding GVPB-03 + GVPB-08 + GVPB-09 — committee/GVPB likely chất vấn pháp luật. Cutting hết = invite vulnerability.

5. **Defense failure-mode matrix (`2026-05-18-thesis-defense-failure-mode-matrix.md`)** top-10 likely committee questions chứa "PDPL 2023 compliance như thế nào?", "Data localization roadmap?", "K-12 child protection?".

**Risk CUT partial (compress):** 🟡 MEDIUM-LOW

Phần A hiện 86 lines markdown (~3-4 trang DOCX). Có thể compress xuống ~1.5 trang giữ KEY claims:
- PDPL 2023 hiệu lực 2026-07-01 + roadmap DPO/DPIA pre-GA (KEEP — defensive critical)
- Decree 53/2022 data localization rationale (KEEP — explains AWS Singapore choice)
- Thông tư 78/2021 eInvoice MISA partnership (KEEP — explains business decision)
- Detailed bảng 1.3.1 PDPL design (KEEP table, compress narrative)
- Decree 147/2024 (CUT — peripheral)

#### 3.2 Phần B — Quality-Driven Development methodology

**Risk CUT toàn bộ:** 🟠 MEDIUM-HIGH

Lý do:
1. **UTC convention bachelor thesis OPTIONAL section "Phương pháp luận"** — KHÔNG mandate Section dedicated. UTC sample đề cương + báo cáo TTTN KHÔNG có. NHƯNG thesis-content-standard §2 C8 Examiner readiness sub-criterion 4 (2 points): *"Methodology section explicit + literature-backed (Deming / Beck / Poppendieck / IEEE 730)"* — RULE mandate có methodology cite literature.

2. **Persona-simulation audit GVHD-05 + GVPB-04** — rebrand "audit-driven methodology" as "original methodology" without literature citation = HARD FAIL Cat C5. Phần B hiện ĐÃ cite Deming [45] + Beck [18] + Poppendieck [46] + IEEE 730 [47] — đáp ứng yêu cầu literature backing. Cut hết = LOSE 4 references citations (impact bibliography 100% utilization metric).

3. **CUT methodology thì Ch.1 Phần 3 còn lại CHỈ Phần A pháp lý** — title section "Khung pháp lý Việt Nam và phương pháp luận" sẽ phải rename, structure mất balance. Phần A standalone không đủ scope cho 1 Phần (typically Phần = 1/3 chương = ~5-7 trang); Phần A compress xuống 1.5 trang KHÔNG đủ standalone.

4. **Tính nguyên bản thesis** — methodology = "phương pháp nghiên cứu" thesis convention thường mandate (academic rigor signal). Cut = giảm scholarly weight.

**Risk CUT partial (compress + relocate):** 🟢 LOW

Có thể compress Phần B từ 78 lines → ~30 lines giữ KEY claims:
- 4 trụ cột với 1 paragraph mỗi pillar (Deming PDCA + Beck TDD + Poppendieck Lean + IEEE 730 SQA)
- Compress chi tiết application (incident-to-rule pipeline implementation detail, meta-CSV index pattern detail) — chỉ keep names + brief description
- KEEP 4 literature citations [18][45][46][47]

### Recommendation Q3

**KEEP cả Phần A + Phần B nhưng COMPRESS aggressively** — KHÔNG cut hoàn toàn.

| Option | Hành động | Trang savings | Risk |
|---|---|:---:|:---:|
| **A** — Cut both Phần A + Phần B hoàn toàn | Xóa toàn bộ Ch.1 Phần 3 | ~5-6 trang | 🔴 HIGH — fail C8 + C9 + GVPB chất vấn pháp luật + lose 4 citations |
| **B** — Cut Phần B only (giữ Phần A) | Xóa methodology, giữ pháp lý | ~3 trang | 🟠 MEDIUM-HIGH — fail C8 sub 4 (methodology literature-backed) + Ch.1 Phần 3 unbalanced |
| **C** — Cut Phần A only (giữ Phần B) | Xóa pháp lý, giữ methodology | ~3 trang | 🔴 HIGH — fail C9 compliance category + GVPB chất vấn vulnerability |
| **D** — Compress both (KEEP both) | Compress Phần A 86 lines → ~35 lines (1.5 trang); Phần B 78 lines → ~30 lines (1.5 trang); Phần 3 total ~3 trang | ~3 trang | 🟢 LOW — preserve compliance + methodology defensible content |
| **E** — Compress + relocate Phần B vào MỞ ĐẦU "Phương pháp nghiên cứu" sub-section | Phần A standalone Ch.1 Phần 3 ~1.5 trang; Phần B move MỞ ĐẦU + compress ~1 trang | ~3.5 trang | 🟡 MEDIUM — structure changes nhiều; rủi ro Ch.1 Phần 3 mất balance |

**Recommended: Option D — Compress both, KEEP both.**

Concrete compression targets:

**Phần A compressed (~1.5 trang DOCX, ~35 lines markdown):**
- 1 paragraph context VN compliance landscape (4-5 lines)
- 1 paragraph PDPL 2023 Article 11 + 17 + 28 (3-4 lines)
- 1 paragraph Cybersecurity Law 2018 + Decree 53/2022 data localization → AWS Singapore decision (3-4 lines)
- 1 paragraph Thông tư 78/2021 eInvoice → MISA partnership (3 lines)
- Bảng 1.3.1 PDPL design checklist (KEEP — 7 rows, defensive)
- 1 paragraph roadmap DPO + DPIA pre-GA (3-4 lines)
- CUT: Decree 147/2024 (peripheral), GAP-185/183 internal references (per `thesis-content-standard.md` §3 C5 banned)

**Phần B compressed (~1.5 trang DOCX, ~30 lines markdown):**
- 1 paragraph context solo-developer + hard deadline (3 lines)
- 4 paragraphs (1 per pillar) cite Deming [45] + Beck [18] + Poppendieck [46] + IEEE 730 [47]:
  - PDCA → Incident-to-Rule pipeline (4 lines)
  - TDD → Audit-to-Gap traceability (4 lines)
  - Lean → Outside-In Coverage Trigger (4 lines)
  - IEEE 730 → Meta-Index Governance Pattern (4 lines)
- 1 paragraph kết quả định lượng (Quality 65→90 / Security 85→93) — keep brief, expand Ch.6 (3 lines)
- CUT: implementation detail (incident-to-rule-pipeline.md path references per §3 C5 banned), `.claude/rules/*.md` file references (per §3 C5 banned)

**Total Ch.1 Phần 3 post-compress: ~3 trang vs ~5-6 trang hiện tại** — savings ~3 trang đáng kể, giữ defensible content + literature backing + compliance roadmap.

---

## Verdict + ranked recommendations

### Top 3 actions Wave 102.2 priority

| Rank | Action | Cost | Value | Risk if skip |
|---|---|:---:|:---:|---|
| **1** | Add Ch.3 §3.1 "Công nghệ, công cụ và môi trường phát triển" (4-5 sub-sections layer-based + lý do lựa chọn) | ~2-3 trang content + 1-2 hours from `references/technology-stack.md` reuse | 🔴 HIGH — close missing UTC mandate section + match đề cương Kiệt 2026-04 GVHD-approved baseline | 🔴 GVHD chất vấn "đề cương đã có section 2 Công nghệ — thesis thiếu phần này?" |
| **2** | Compress Ch.1 Phần 3 Phần A + Phần B (Option D) — KEEP both, target ~3 trang post-compress | ~3 trang savings + ~1 hour content compress | 🟢 MEDIUM — defensive C8 methodology literature-backed + C9 compliance preserved | 🟠 Cut both = C8+C9 dual fail; cut one = unbalanced structure |
| **3** | Add Ch.2 §2.1 mở đầu paragraph clarify "phân tích yêu cầu nghiệp vụ" ≡ "BRD" scope (Option 1B) | <0.5 trang + 15 minutes | 🟢 LOW-MEDIUM — defensive cho committee có thể chất vấn "BRD đâu" | 🟡 Reader unfamiliar VN academic may flag "BRD missing" |

### Net page count impact

- Action 1 (Add Ch.3 §3.1): **+2-3 trang**
- Action 2 (Compress Ch.1 Phần 3): **-3 trang**
- Action 3 (Add Ch.2 paragraph): **+0.3 trang**
- **Net: ~0 đến +0.3 trang** — không ảnh hưởng đáng kể 90-trang cap per `thesis-content-standard.md` §2 C2 page count cap

### Cite-able UTC evidence summary cho defense

1. **Q2 "Công nghệ sử dụng" mandate:** `Mau-Decuong DATN-Cử nhân.pdf` section 2 = "Công nghệ, công cụ và ngôn ngữ lập trình" (UTC blank template chuẩn cử nhân CNTT)
2. **Q1 "BRD không mandate":** UTC spec PDF §1 cho phép "CHƯƠNG 1/2/3 tuỳ theo đề tài"; UTC sample đề cương không có BRD format
3. **Q3 "Pháp lý + methodology không mandate but recommended":** UTC spec OPTIONAL Section "Phương pháp luận"; pháp lý compliance specific domain — thesis Kite Platform SaaS edu xử lý PII justifies explicit compliance section per industry-specific scholarly rigor

### Defensive narrative cho thesis V1 → V2 transition

Thesis V1 ship Wave 100.7 (2026-05-19) hiện đạt rubric v1 82/100 B- nhưng rubric v2 §5 self-test estimate 42/100 F do missing 5 critical categories (academic tone + project-internal scrub + draft markers + diagram rendering + compliance sensitivity). Wave 102.1 fix scope (bundled) đã queue. Wave 102.2 content edit thêm 3 actions trên = path 75-87/100 C+/B+ target post Wave 105 (~2026-06-30) ship V2.

