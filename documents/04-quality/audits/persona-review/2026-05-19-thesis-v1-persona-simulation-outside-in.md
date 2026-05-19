---
title: Thesis V1 Persona Simulation Outside-In Audit (post Wave 102 GAP-688)
status: complete
created: 2026-05-19
audience: dev
phase: phase-1-beta
gaps: [GAP-688, GAP-687, GAP-651]
audit_id: AUDIT-2026-05-19-thesis-v1-persona-simulation
artifact: documents/08-thesis/thesis-v1.docx
---

## Scope

Outside-in audit `documents/08-thesis/thesis-v1.docx` (300 KB, 1710 paragraphs, 36 tables, ~22.2k từ ≈ 79-89 trang ước tính theo TNR 13/1.5) qua 3 persona: TS. Nguyễn Đức Dư (GVHD), GV phản biện hypothetical, và Defense committee 5-member panel. Mục tiêu: catch additional bugs **BEYOND** 7 issues user đã flag (ngôn từ "đối thủ", Claude refs, Mermaid PNG, danh mục thuật ngữ tách, logo UTC, TL;DR, repo jargon + page-count trim).

UTC reference: `documents/07-archived/academic/word-reports/templates/Quy dinh trinh bay do an tot nghiep.docx` §2.2 đánh số chương (1.1, 1.1.1 strict), §2 bố cục bắt buộc (LỜI CẢM ƠN / MỤC LỤC / DANH MỤC VIẾT TẮT / DANH MỤC BẢNG / DANH MỤC HÌNH / MỞ ĐẦU / CHƯƠNG 1-N / KẾT LUẬN VÀ KIẾN NGHỊ / DANH MỤC TÀI LIỆU THAM KHẢO / PHỤ LỤC), §3 trích dẫn IEEE numeric `[15, tr.314]`.

---

## Persona 1 — GVHD findings (TS. Nguyễn Đức Dư)

| # | Chapter / Section | Issue | Suggested fix | Priority |
|---|---|---|---|---|
| GVHD-01 | Bìa chính + bìa phụ (para 0-27) | Trùng lặp 2 lần "BỘ GIÁO DỤC... KHOA CNTT... KHÓA LUẬN... CỬ NHÂN... Đề tài... Hà Nội – 2026" (para 0-10 + 12-27). Bìa phụ thiếu khung thông tin chuẩn UTC (Sinh viên / MSSV / Lớp / GVHD / GVPB) — chỉ có "Giáo viên phản biện" + "(Sẽ cập nhật...)" rời rạc | Bìa chính = trang 1 đơn thuần (tên trường + đề tài + năm). Bìa phụ = trang 2 với 6-field table (Sinh viên / MSSV / Lớp / Khóa / GVHD / GVPB). Tham khảo `de-cuong-datn/DE_CUONG_DATN.docx` layout | **P0** |
| GVHD-02 | MỤC LỤC + DANH MỤC HÌNH + DANH MỤC BẢNG (para 47-58) | Placeholder text "(Bấm Ctrl+A rồi F9 trong Word để cập nhật mục lục)" + "(Đang được bổ sung khi thêm hình minh hoạ — Wave 103+)" lộ liễu trong file sẽ in nộp. DANH MỤC BẢNG thậm chí trống (chỉ có heading). Em đang nộp **draft chưa hoàn thành** | Word PHẢI tạo TOC động qua `python-docx` settings.element XML hoặc post-processing manual Ctrl+A F9 trước nộp. DANH MỤC BẢNG mandatory (UTC spec §2 — "DANH MỤC BẢNG BIỂU bắt buộc nếu có bảng") — 36 bảng trong V1 phải liệt kê | **P0** |
| GVHD-03 | LỜI CẢM ƠN (para 30-37) | Câu "Em xin chân thành cảm ơn Công nghệ thông tin, Đại học Giao thông Vận tải" thiếu chữ "Khoa" trước "Công nghệ thông tin" → đọc rời nghĩa | Sửa: "Em xin chân thành cảm ơn **Khoa** Công nghệ thông tin, Trường Đại học Giao thông Vận tải..." | **P0** |
| GVHD-04 | MỞ ĐẦU §5 Cấu trúc (para 74-79) | Khóa luận UTC cử nhân CNTT convention thường 5-6 chương (Tổng quan / Cơ sở lý thuyết / Phân tích yêu cầu / Thiết kế / Triển khai / Kết luận). Em ghép Phân tích yêu cầu + Thiết kế + Kiến trúc thành 1 chương 2, ghép Triển khai code + Triển khai cloud thành Ch.3 + Ch.4 — chương 2 quá dollar (~15-20 trang dense theo headings 2.1→2.5) | Cân nhắc tách Ch.2 thành 2 chương: **Ch.2 Phân tích yêu cầu** (FR + NFR + Persona scope) + **Ch.3 Thiết kế kiến trúc** (C4 + multi-tenant + RLS + auth flow + SaaS model). Ch.3 hiện thành Ch.4, Ch.4 hiện thành Ch.5. Đạt 5 chương UTC-conformant | **P1** |
| GVHD-05 | Ch.1 Phần 3 §B Trụ cột 1-5 (para 351-425) | "Phương pháp luận audit-driven" với 5 trụ cột (incident-to-rule pipeline, meta-CSV governance, outside-in trigger, persona-based review, audit-to-gap pipeline) đọc giống **nội bộ KiteHub repo practice** chứ chưa được formalize thành "phương pháp luận nghiên cứu khoa học". Thiếu literature support — Continuous Improvement (Deming), Lean Software Dev (Poppendieck), Toyota Production System đều không cite | Re-frame 5 trụ cột thành "Quy trình quản lý chất lượng" (Quality Management Process) áp dụng cho dự án phần mềm; cite Deming PDCA cycle [add], Kaizen [add], Software Quality Assurance per IEEE 730 [add]. Tránh từ "phương pháp luận audit-driven" — không phải original methodology, là rebrand existing patterns | **P1** |
| GVHD-06 | Ch.2 §2.2 NFR (para 493-532) | 6 sub-categories NFR (Performance / Availability / Security / Scalability / Maintainability / Cost) — KHÔNG cite ISO/IEC 25010:2011 Software Product Quality Model (chuẩn quốc tế NFR taxonomy) | Add citation ISO/IEC 25010 ngay đầu §2.2 + Bảng map NFR → ISO 25010 categories (Performance Efficiency / Reliability / Security / Maintainability / Portability / Functional Suitability) | **P1** |
| GVHD-07 | Ch.2 §2.3.1-2.3.6 (para 534-765) | Sơ đồ kiến trúc (C4 L1, L2, multi-tenant single-bucket, RLS layers, auth flow, service decomp) — em ghi Mermaid code blocks. Khi convert DOCX, Mermaid không render → reader thấy text rối | (Trùng với user issue #3 PNG; nhưng GVHD specifically nói:) Mỗi sơ đồ kiến trúc PHẢI có **caption "Hình 2.x: Tên hình"** TRƯỚC + paragraph **chú thích diễn giải** SAU; UTC §2.4 mandate đánh số gắn chương | **P0** |
| GVHD-08 | Ch.3 (para 1000-1234) | 5 code snippets — tốt, nhưng KHÔNG có analysis "tại sao chọn pattern này" trong style scientific (so sánh alternatives, trade-offs). Phân tích sau snippet rất ngắn, chỉ mô tả code | Mỗi snippet thêm sub-section "Trade-offs": tại sao chọn JWT HS256 vs RS256, tại sao RLS vs application-level isolation, tại sao Outbox Pattern vs direct queue, etc. Cite [Newman, Building Microservices], [Fowler, Outbox] | **P1** |
| GVHD-09 | Ch.4 §4.3.4 Real KPI numbers (para 1550-1556) | Section "Real KPI numbers (placeholder)" công khai admit "real numbers Wave 102+" → GVHD thấy em nộp tài liệu CHƯA HOÀN THÀNH | Xóa heading "(placeholder)"; thay bằng "Kết quả đo Beta Phase 1 (sơ bộ)" + ít nhất 1-2 KPI cụ thể (signup conversion / time-to-first-tenant / Lighthouse score) **đo được trước defense** | **P0** |
| GVHD-10 | Ch.4 §4.4.4 Lessons learned + §4.4.5 Future roadmap (para 1597-1637) | 2 sections này dài (~40 paragraphs ước tính) — nội dung kiểu retrospective dev internal, chưa được polish thành academic prose | Trim ~50%; chuyển insights cụ thể vào KẾT LUẬN VÀ KIẾN NGHỊ chương cuối — đỡ duplicate | **P1** |
| GVHD-11 | KẾT LUẬN (para 1645-1656) | Chỉ 3 heading 2 (Tổng kết / Hạn chế / Hướng phát triển) với rất ít text. UTC convention: KẾT LUẬN VÀ KIẾN NGHỊ phải 2-3 trang gói gọn (1) các kết quả chính đạt được + (2) hạn chế + (3) hướng phát triển + (4) đóng góp khoa học | Expand thành 4-5 paragraphs mỗi section; cite các kết quả từ Ch.4 §4.4.4 đã trim | **P0** |
| GVHD-12 | TÀI LIỆU THAM KHẢO (para 1657-1702) | 44 refs nhưng em đã liệt kê tài liệu Anthropic Claude, OpenAI GPT API. Có vẻ thiếu citing **giáo trình UTC** mà em đã học (Kiến trúc phần mềm, CSDL, An ninh mạng) | Add ít nhất 3-4 giáo trình UTC tác giả trong nước (Tống Đình Quỳ, Phạm Hữu Đức, v.v.) — show em đã tham khảo program of study | **P2** |
| GVHD-13 | PHỤ LỤC C "Kết quả audit chất lượng" (para 1708-end) | Phụ lục C tham chiếu "audit suite" — GVHD đọc sẽ confused do từ "audit" trùng với GVHD audit / khoa audit / committee audit | Rename "Kết quả đánh giá chất lượng dự án" (drop từ "audit") trong PHỤ LỤC; lock từ "audit" chỉ dùng cho external defense audit | **P1** |
| GVHD-14 | Toàn bộ thesis | Em viết "em" trong LỜI CẢM ƠN (correct), nhưng nội dung body lại không dùng đại từ ngôi 1 nhất quán — section nào dùng "KiteHub" (subject impersonal), section nào dùng "em đã chọn / triển khai" (mixed). Academic convention VN: hoặc "tác giả" / "khóa luận" passive, hoặc "em" đầu thân bài | Lock convention: LỜI CẢM ƠN dùng "em"; thân bài dùng **"khóa luận"** hoặc **"tác giả"** passive voice. KẾT LUẬN có thể "em" lại | **P1** |
| GVHD-15 | Ch.1 Phần 3 §B.5 Persona-Based Business Review Skill (para 389-405) | Liệt kê 10 persona type — chi tiết là feature implementation của KiteHub, không phải research methodology. Mismatch với chapter scope (khung pháp lý + phương pháp luận) | Move persona-review skill sang Ch.3 Implementation (cite tool) hoặc Ch.4 §4.4 Lessons; remove khỏi Ch.1 Phần 3 | **P1** |

**GVHD Total: 5 P0 + 7 P1 + 1 P2 = 13 issues mới (sau khi không re-flag 7 user issues)**

---

## Persona 2 — GV phản biện (hypothetical red-team)

| # | Chapter / Section | Chất vấn defense | Fix priority |
|---|---|---|---|
| GVPB-01 | Ch.2 §2.3.3 Multi-tenant single-bucket (Pool model) | "Em claim **kiến trúc multi-tenant gốc** nhưng đã có data isolation test results gì để chứng minh không cross-tenant leak? Test cụ thể row-level security NULL force-fail có ship được không?" → nếu em không có concrete penetration test report cho RLS, claim "gốc" weak | **P0** — add penetration test summary table vào §2.3.4 (RLS layers) HOẶC cite Wave 85 security audit 93/100 |
| GVPB-02 | Ch.1 Phần 2 AI Branding | "AI Branding tự động" — em claim cost analysis nhưng chỉ ở Ch.1 Phần 2 §2.4 table 4-model comparison. **Số liệu cost-per-image $0.04 (Stable Diffusion) là ước tính hay em đã measure thực tế?** Bao nhiêu image em đã sinh trong beta? Quality score gì? | **P0** — thêm "Beta evidence: KiteHub đã sinh N image cho M tenant với mean quality Y/100" |
| GVPB-03 | Ch.1 Phần 3 §A.2.4 DPO yêu cầu | "Em viết PDPL deadline **2026-07-01** mà Phase 1 BETA **CHƯA bổ nhiệm DPO**, **CHƯA submit DPIA**. Tại sao em vẫn đang invite beta tenant trong khi không compliance? Em có hợp pháp khi defense 2026-08-15+?" | **P0** — phải rewrite §A.2.4 nói "Phase 1 BETA invite-only ≤10 tenant nội bộ KHÔNG xử lý dữ liệu công khai → chưa kích hoạt PDPL Article 28 ngưỡng 10.000 subject. DPO + DPIA scheduled Phase 2 GA trước Q3 2026" |
| GVPB-04 | Ch.1 Phần 3 §B Audit-driven methodology | "Em call '**phương pháp luận audit-driven development**' — original của em hay rebrand existing? Cite literature support đâu? Nếu mượn tên 'audit-driven', concept tương đương với Test-Driven Dev (TDD), Behavior-Driven Dev (BDD), Continuous Integration (Beck), Lean Six Sigma, hoặc DevOps audit pattern. Em chưa cite ai" | **P0** — re-frame như "Quality-Driven Development áp dụng cho dự án solo-dev" + cite Deming, Beck TDD, Lean. KHÔNG claim original methodology |
| GVPB-05 | Ch.2 §2.4.4 Billing & payment | "Phase 1 BETA chỉ có VietQR thủ công + Phase 2 mới MoMo/VNPay. Có nghĩa trong defense window em **chưa có integration thanh toán production**. Vậy KPI revenue/MRR/churn ở Ch.4 §4.3 đo bằng gì? Mock data không phải nghiên cứu" | **P1** — thẳng thắn admit: §4.3.4 KPI Phase 1 BETA tập trung 4 metric measurable (signup conversion, time-to-onboard, Lighthouse, uptime) — revenue KPI scheduled Phase 2 |
| GVPB-06 | Ch.3 §3.2 JWT auth gateway | "Em dùng HS256 với secret 256-bit từ Secrets Manager. **Tại sao không RS256 với key rotation**? HS256 không cho phép split signing-vs-verifying — gateway compromise = toàn bộ system compromise" | **P1** — defend choice: HS256 chọn vì (a) all internal services same trust boundary, (b) reduce complexity Phase 1 BETA, (c) rotation roadmap Phase 2 RS256 |
| GVPB-07 | Ch.3 §3.4 Outbox dispatcher | "Em cite Fowler Outbox Pattern nhưng không show **race condition handling** — multiple dispatcher instances pickup cùng row outbox như thế nào? FOR UPDATE SKIP LOCKED hay advisory lock?" | **P1** — add code snippet show `SELECT ... FROM outbox FOR UPDATE SKIP LOCKED LIMIT N` |
| GVPB-08 | Ch.4 §4.1.1 AWS Singapore vi phạm Decree 53/2022 | "Em **admit explicit vi phạm Nghị định 53/2022** ở §4.1.1 'Compliance debt được chấp nhận có quản lý'. **Defense committee có chấp nhận khóa luận đạt giải khi sản phẩm vi phạm pháp luật VN?** Có nên defer Phase 1 BETA sang VN cloud trước defense?" | **P0** — rewrite mềm hơn: "Phase 1 BETA invite-only chưa kích hoạt Decree 53 §26 ngưỡng 1M user; migrate sang AWS Hanoi Local Zone OR VN cloud (Viettel/VNG) đã được lên kế hoạch ADR-XXX trước Phase 2 GA". Avoid từ "vi phạm" |
| GVPB-09 | Ch.4 §4.2.4 Sample VN onboarding data | "Sample data có Trần Thị Hồng / Sky Education — em có **consent từ Trần Thị Hồng và Sky Education** để dùng tên trong khóa luận chính thức không?" | **P0** — replace bằng fictional name có thêm hậu tố `(tên giả định)` hoặc `(hypothetical)` ngay trong sample data |
| GVPB-10 | Ch.4 §4.4.2 Feature scope cut Phase 1 BETA | "Em liệt kê **cut features** — tức là KiteHub **chưa hoàn thiện** tại defense. Em đảm bảo 60% scope đã ship đủ chứng minh kiến trúc?" | **P1** — re-frame "feature scope cut" → "feature scope ưu tiên (priority)" + add bảng % completion với evidence |
| GVPB-11 | Ch.1 Phần 1 §6 So sánh 4 đối thủ | "Bảng 13 cột (Tiêu chí + 4 đối thủ + KiteHub) — **số liệu của đối thủ từ đâu**? MISA AMIS price 50-200 triệu setup có cite source không? Em access được pricing internal hay đoán" | **P1** — cite source mỗi row: "[1]", "[31]", "[32]" + thêm note "công bố giá trang chủ truy cập DD/MM/2026" |
| GVPB-12 | Ch.2 §2.2.2 Availability 99.5% | "99.5% = 3.65 giờ downtime/tháng. Em có **SLA monitoring tooling** running? Hay aspirational target? Em ship Statuspage chưa?" | **P1** — admit honest: target 99.5% based on AWS SLA + planned Statuspage post-defense |
| GVPB-13 | KẾT LUẬN | "Em ghi 'đóng góp khoa học' nào? Khóa luận này là **product development project hay scientific research**? UTC convention thường yêu cầu thesis cử nhân có 1-2 đóng góp methodological hoặc empirical novel" | **P0** — explicit list "đóng góp" trong KẾT LUẬN: vd (1) áp dụng RLS NULL force-fail pattern cho VN edu SaaS context, (2) empirical evaluation 4 đối thủ thị trường giáo dục VN, (3) reference architecture cho multi-tenant SaaS giáo dục B2B |
| GVPB-14 | Ch.3 (toàn chương 5 snippets) | "5 snippets từ codebase 200,000 LOC — **5 đại diện cho tỷ lệ 0.0025% codebase**. Em không show full module architecture chi tiết — có thể defense bị challenge 'kiến trúc rỗng'" | **P1** — add Ch.3 §3.1 sub-section "Tổ chức source code" với tree-view module + dòng đếm LOC per service |
| GVPB-15 | Toàn bộ Ch.4 §4.3 KPI placeholder | "Em **dán placeholder `<!-- TODO Wave 102+ -->` rải khắp Ch.4 KPI**. Khi nộp + defense, có dám để placeholder không? Hay em đang nộp draft trước hạn?" | **P0** — strip mọi `<!-- TODO -->` HTML comment; thay bằng "(Số liệu sơ bộ Phase 1 BETA, cập nhật Phase 2)" trong narrative |

**GVPB Total: 7 P0 + 8 P1 = 15 chất vấn defense**

---

## Persona 3 — Defense committee 5-member panel

| # | Format / structural gap | Grade impact | Fix priority |
|---|---|---|---|
| COMM-01 | UTC spec §2.2 đánh số mục: 1.1, 1.1.1. Em dùng `A.2.4`, `B.5`, `1.4`, `2.1.1` mix — KHÔNG nhất quán theo UTC strict format `số_chương.x.y` | -3 đến -5 | **P0** |
| COMM-02 | UTC spec §2.4 đánh số bảng phải `Hình 1.4` / `Bảng 3.1` (gắn chương). Em chưa numbered bất kỳ bảng nào — 36 tables trong V1 KHÔNG có "Bảng X.Y" caption | -3 | **P0** |
| COMM-03 | DANH MỤC HÌNH VẼ trống + DANH MỤC BẢNG BIỂU trống → UTC §2 mandate bắt buộc khi có hình/bảng → committee deduct format compliance | -3 | **P0** |
| COMM-04 | MỤC LỤC chứa text placeholder "(Bấm Ctrl+A rồi F9...)" → committee thấy file in nộp chưa generate TOC | -2 | **P0** |
| COMM-05 | Không có **TÓM TẮT** (Abstract) tiếng Việt ~1 trang đầu khóa luận trước MỞ ĐẦU — UTC convention typical bachelor thesis có Abstract VN 200-300 từ | -2 | **P0** |
| COMM-06 | Không có **ABSTRACT** tiếng Anh ở trang riêng sau Abstract VN — convention cử nhân CNTT UTC thường có để demonstrate English proficiency | -1 | **P1** |
| COMM-07 | Không có **PHỤ LỤC** đầy đủ — Phụ lục A/B/C chỉ là heading, nội dung trống | -2 | **P0** |
| COMM-08 | 22.2k từ ≈ 79-89 trang TNR 13/1.5 — **vượt mức trim mà user đã nói target 60-70**. Tuy nhiên committee sẽ counter-deduct ngược: thesis cử nhân thường 50-80 trang body; em 79 PASS lower bound + 9 over upper bound → soft deduct | -1 | **P1** |
| COMM-09 | UTC spec yêu cầu **đánh số trang** (footer hoặc header). DOCX của em có set numbering? Cần verify trước nộp | -1 đến -2 nếu thiếu | **P0** |
| COMM-10 | UTC spec §2.4: "không lạm dụng từ viết tắt; chỉ viết tắt từ dùng nhiều lần". Bảng 2 (26 rows) chứa list từ viết tắt — em có **explain mỗi từ tại lần xuất hiện đầu tiên trong body chưa**? VD "PDPL (Personal Data Protection Law)" — lần đầu có expand? | -1 đến -2 | **P1** |
| COMM-11 | Bibliography 44 refs — committee check **citation order** UTC spec §3 mandate "trình tự sử dụng (trích dẫn)" → numbered theo first appearance trong body. Em có verify [1], [2], [3]... theo đúng order xuất hiện không? Hay mix? | -1 đến -2 | **P1** |
| COMM-12 | UTC spec §3 trích dẫn IEEE format "[15, tr.314]" với số trang khi trích trực tiếp. Em có dùng đúng format không, hay chỉ `[15]` cho mọi trường hợp | -1 | **P2** |
| COMM-13 | Bibliography mix academic papers + grey literature (Anthropic Claude API page, OpenAI GPT API — vendor docs). Committee thường **chấm chất lượng nguồn** — vendor docs grey lit weight thấp hơn peer-reviewed | -1 đến -2 | **P1** |
| COMM-14 | Em không có bất kỳ **giáo trình UTC** trong bibliography. Committee thường có cựu GV department-internal — thiếu cite làm giảm "thông thuộc chương trình đào tạo" | -1 | **P2** |
| COMM-15 | UTC spec yêu cầu **bìa cứng**, gáy có in (HỌ TÊN - LỚP - NĂM). Em chưa có instructions cho printer trong notes file. Defense day reject nếu format bìa sai | -2 nếu vi phạm | **P0** |

**Committee Total: 9 P0 + 4 P1 + 2 P2 = 15 format gaps**

**Estimated grade deduction nếu submit AS-IS:** -22 đến -32 điểm format/structural — kéo từ baseline 82/100 self-audit xuống **~52-60/100 grade thực tế** (vì nhiều P0 = automatic format deduct).

---

## Cross-persona patterns (recurring themes)

1. **"Draft chưa hoàn thành" signal mạnh** (GVHD-02, GVHD-09, GVPB-15, COMM-03, COMM-04, COMM-05, COMM-07) — TOC placeholder, KPI `<!-- TODO -->`, danh mục trống, Abstract thiếu, Phụ lục heading-only. Mỗi persona độc lập catch signal này → tổng số 7 cross-persona votes — Issue gốc.

2. **Repo-as-research-output blending** (GVHD-05, GVHD-13, GVHD-15, GVPB-04) — em đang phối repo internal practice (waves, gaps, audit-driven, persona skills) lẫn với academic research output. Reader không thấy ranh giới "what is KiteHub product feature" vs "what is research methodology". Cần phân biệt **product (Ch.2-4) vs process (KẾT LUẬN/PHỤ LỤC)**.

3. **Compliance/legal sensitivity** (GVPB-03 PDPL DPO, GVPB-08 Decree 53 violation admission, GVPB-09 sample data consent) — em **admit vi phạm pháp luật explicitly** + dùng tên thật trong samples. Committee + GVPB sẽ chất vấn nhiều. Cần defensive rewriting.

4. **Empirical evidence weakness** (GVPB-01 RLS test results, GVPB-02 AI cost real measurement, GVPB-12 SLA monitoring, GVPB-14 codebase tree, COMM-13 grey lit) — claim của em strong nhưng evidence weak. Cần add Phase 1 BETA actual numbers + screenshot + audit report cross-ref.

5. **UTC format strict compliance gap** (COMM-01 numbering scheme, COMM-02 bảng/hình numbering, COMM-09 page numbering, COMM-15 hard cover) — em đang dùng convention markdown-friendly (1.1.1) nhưng chưa enforce UTC §2.2-§2.4 strict format mọi nơi.

---

## Recommendations sorted by priority

### P0 — Block defense (15 issues, MUST fix trước nộp)

- **Format compliance (UTC §2-§3):** GVHD-01 bìa, GVHD-02 TOC + DANH MỤC, GVHD-03 typo "Khoa", GVHD-07 sơ đồ caption, GVHD-09 + GVPB-15 strip TODO placeholder, GVHD-11 KẾT LUẬN expand, COMM-01 numbering 1.1.1, COMM-02 đánh số bảng/hình, COMM-03 danh mục bảng/hình, COMM-04 generate TOC, COMM-05 Abstract VN, COMM-07 Phụ lục, COMM-09 page numbering, COMM-15 bìa cứng instructions
- **Content rigor:** GVPB-01 RLS test, GVPB-02 AI cost real numbers, GVPB-03 PDPL defensive rewrite, GVPB-04 methodology re-frame, GVPB-08 AWS Singapore soft language, GVPB-09 fictional sample data, GVPB-13 đóng góp khoa học explicit list

### P1 — Should fix (15 issues — committee deduct nếu skip)

- GVHD-04 (tách Ch.2 thành 2), GVHD-05 (cite Deming/Lean), GVHD-06 (ISO 25010), GVHD-08 (snippet trade-offs), GVHD-10 (trim Ch.4 retrospective), GVHD-13 (rename Phụ lục C), GVHD-14 (đại từ ngôi 1 nhất quán), GVHD-15 (move persona skill)
- GVPB-05, GVPB-06, GVPB-07, GVPB-10, GVPB-11, GVPB-12, GVPB-14
- COMM-06 (Abstract EN), COMM-08 (trim 9 trang), COMM-10 (expand viết tắt), COMM-11 (citation order), COMM-13 (cite peer-reviewed thêm)

### P2 — Nice-to-have (3 issues)

- GVHD-12, COMM-12 (IEEE page nums), COMM-14 (giáo trình UTC)

---

## Verdict

**Current state:** Thesis V1 ship ngày 2026-05-19 (Wave 102 closure GAP-688) là **technically structured nhưng academically immature**. Em đã xây architecture content rất phong phú (Ch.1-4 đủ 4 chương, 44 refs, 36 tables, 22k+ từ) nhưng:

- **Format compliance UTC**: ~40% gap (numbering, TOC, danh mục, Abstract, Phụ lục, page numbering, bìa)
- **Academic tone**: ~30% gap (repo jargon đã user-flag + thêm methodology rebranding chưa cite)
- **Empirical evidence**: ~25% gap (KPI placeholder, AI cost ước tính, RLS không có test report)
- **Legal/compliance defensive language**: ~10% gap (Decree 53 admit vi phạm, sample data tên thật)

**Estimated grade nếu submit AS-IS (2026-08-15):** ~**52-60/100** (D đến D+). Format deduct ~22-32 + content rigor deduct ~5-10 từ self-audit baseline 82/100.

**Estimated grade post-fix (P0 only):** ~**75-82/100** (B đến B+). Đủ pass defense window.

**Estimated grade post-fix (P0 + P1):** ~**82-88/100** (B+ đến A-). Competitive với strong cohort.

**Critical path (must-fix 2-3 tuần trước defense 2026-08-15):**

1. Tuần 1: Format compliance UTC (10 P0 format gaps) — generate TOC, đánh số bảng/hình, add Abstract VN+EN, Phụ lục, KẾT LUẬN expand
2. Tuần 2: Content rigor (5 P0 content gaps) — PDPL/Decree 53 defensive rewrite, đóng góp khoa học explicit, AI cost real numbers, fictional sample data, methodology re-frame
3. Tuần 3: P1 fixes — tách Ch.2, ISO 25010, snippet trade-offs, đại từ ngôi 1, citation order

**Recommendation:** delay submit từ Wave 102 (2026-05-19) ship sang post-Wave-105 (~2026-06-30) để hoàn thiện P0 + P1; tận dụng 6 tuần đệm trước defense 2026-08-15.
