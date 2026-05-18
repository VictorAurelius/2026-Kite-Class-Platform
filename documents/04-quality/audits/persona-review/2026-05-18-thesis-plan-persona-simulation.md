---
title: Thesis Plan Persona Simulation — 3 nhân vật outside-in audit
status: complete
created: 2026-05-18
phase: pre-thesis-plan-lock (release 2 cover)
audit_type: outside-in persona simulation T1
personas: [strict-examiner, lenient-examiner, advisor]
related_action: documents/action-2.md (lines 17-45)
related_outline: documents/07-archived/academic/thesis/graduation-thesis-outline-v3.1.md
related_khung: documents/08-thesis/khung-chuan/khung-bao-cao-do-an.png
student: Nguyễn Văn Kiệt (MSSV 221230890, CNTT1-K63, UTC)
release_scope: v2.0.0 KiteHub/KiteClass Platform
rules_applied:
  - outside-in-coverage-trigger.md
  - dev-readable-doc-language.md
  - output-review-mandate.md
---

# Thesis Plan Persona Simulation — 3 nhân vật outside-in audit

**Date:** 2026-05-18
**Scope:** Pre-lock outside-in audit theo `outside-in-coverage-trigger.md` §3 cho thesis plan release 2 cover (đồ án tốt nghiệp UTC, sinh viên Nguyễn Văn Kiệt, MSSV 221230890, CNTT1-K63)
**Personas:** Strict examiner (giảng viên chấm khắt khe) / Lenient examiner (giảng viên chấm thoáng) / Advisor (giảng viên hướng dẫn)
**Methodology:** Mỗi persona role-play đánh giá thesis plan dựa trên user brief (action-2.md) + khung chuẩn UTC + đề cương v3.1 cũ → surface risks + recommendations → cross-persona convergence.

---

## Inside-out scope reviewed (từ action-2.md + khung-bc-do-an.png)

Brief user proposal (rút từ action-2.md lines 17-45):

- **Đề tài lock:** Cover bản release **v2.0.0** KiteClass Platform (multi-tenant SaaS quản lý trung tâm giáo dục, microservices + modular monolith, AI Agent branding, parent portal, gamification, VietQR payment, LMS + Marketing module per V4.1 outline).
- **Cấu trúc báo cáo:** 4 chương theo khung UTC + max **60 trang** (không tính phụ lục như manual, evidence triển khai, evidence end-user). Bìa, mục lục, danh mục hình/bảng/từ viết tắt theo khung chuẩn.
- **Chương 1 — Cơ sở lý luận:** Xác định bài toán (luật giáo dục) + công nghệ + công cụ → sử dụng dữ liệu trong `documents/` → trích dẫn tài liệu tham khảo thật, đúng chuẩn IEEE/APA/ACM.
- **Chương 2 — Phân tích & Thiết kế:** Yêu cầu chức năng + phi chức năng → viết lại từ `documents/` đúng chuẩn báo cáo. Kiến trúc + nhóm nghiệp vụ trình bày đủ. Nghiệp vụ chính chọn lọc: SaaS lifecycle (KiteHub) + B-learning (LMS) — KHÔNG trình bày tất cả nghiệp vụ.
- **Chương 3 — Lập trình:** Đại diện vài thao tác (chương ngắn nhất vì dự án lớn).
- **Chương 4 — Triển khai:** Quan trọng nhất + "ăn điểm" — cách triển khai cloud, deploy cho user, kết quả sử dụng thật. Cần trình bày đầy đủ dữ liệu.
- **Beta plan (chưa có evidence):** 2 giáo viên đơn lẻ + 2 business (trial + VIP) — phải có kế hoạch dùng thử trong báo cáo nhưng evidence bổ sung sau ở phụ lục.
- **Manual + video hướng dẫn:** PDF đang tạo ở wave 92 (chưa confirm), video hướng dẫn người dùng.
- **Thu thập feedback:** Evidence, log, bản nhận xét, chữ ký (quan trọng) — chưa có data thật → đánh giá có sẵn trong báo cáo, bổ sung phụ lục sau.
- **Constraints inside dev:**
  - KHÔNG đề cập "GAP ID", "wave", "PR", "Claude" trong báo cáo (giảng viên không nhận jargon dev).
  - Folder riêng cho image ID (BRD, ERD, AWS diagram, FE screenshot) → tự generate, dev không sửa tay.
  - Thông tin cá nhân dev đã có ở đề cương cũ → tìm đầy đủ, không để raw.
  - Focus khung đề cương + code thực tế — không vẽ feature chưa có trong kế hoạch release 2.
- **Tích hợp AI:** Có (AI Agent branding, marketing copy generation per đề cương v3.1).

Khung UTC (từ ảnh khung-bc-do-an.png):
- Bìa (không số trang), Lời cảm ơn, MỤC LỤC, danh mục hình/bảng/từ viết tắt.
- Mở đầu (~1-2 trang): lý do chọn đề tài, mục tiêu, đối tượng, phạm vi, phương pháp, bố cục.
- Chương 1 — Cơ sở lý luận, hiện trạng và công nghệ: hiện trạng, bài toán cần giải quyết, công nghệ + công cụ + nền tảng (chọn phù hợp), môi trường phát triển (PTTK, Công cụ FE, công cụ BE, dev env, công cụ design CSDL, …) — quy trình phát triển phần mềm (developer).
- Chương 2 — Phân tích & Thiết kế: bài toán, yêu cầu chức năng (UC, …), yêu cầu phi chức năng, kiến trúc, ERD, …
- Chương 3 — Xây dựng chương trình: thiết kế UI, thiết kế UC chính (3-5 chính), code (đại diện), dữ liệu (đại diện).
- Chương 4 — Cài đặt, kiểm thử và triển khai: cài đặt, kiểm thử, triển khai.
- Kết luận: ngắn gọn (lợi ích thực hiện, kết quả đạt được, đánh giá vai trò + ưu nhược, hướng phát triển). Tài liệu tham khảo. Phụ lục (manual, evidence).

---

## Persona 1 — Strict examiner findings (giảng viên chấm khắt khe)

**Mindset:** Ưu tiên format chuẩn UTC + reference IEEE/APA chuẩn + ngôn ngữ formal academic Vietnamese. Reject mọi jargon dev (GAP/wave/PR/merge/deploy/DevOps/Claude) trong main report body. Đánh giá chương 1 cơ sở lý luận + chương 2 mô hình hóa UML chuẩn nghiêm ngặt. Không quan tâm "AI tích hợp" / "cloud deploy" nếu chương 1-2 yếu.

### Risks identified

| ID | Risk | Severity | Source |
|----|------|----------|--------|
| S-R1 | **Jargon dev "lậu" vào main report** — terms như "wave", "GAP", "PR", "merge", "squash", "deploy", "rollback", "BETA cohort" đầy ắp trong `documents/` source → high risk leak vào báo cáo khi copy-paste | HIGH | Brief constraint §38, nhưng nguồn data dày đặc jargon |
| S-R2 | **Reference không đủ chuẩn IEEE/APA** — đề cương cũ v3.1 chỉ liệt kê "BeeClass.net" làm tài liệu tham khảo → thiếu paper academic, sách giáo trình microservices, ITU/ISO/IEEE standard cho SaaS / multi-tenancy / cloud architecture | HIGH | Brief §27 "trích dẫn tài liệu tham khảo thật, đúng chuẩn" |
| S-R3 | **UML chuẩn thiếu** — đề cương v3.1 nhắc "Business Flow Diagram", "ERD", nhưng KHÔNG nhắc Use Case Diagram chính thức, Class Diagram, Sequence Diagram, Activity Diagram, Component Diagram theo UML 2.x notation | HIGH | Khung UTC §Chương 2 "UC, ..." implies UML chuẩn |
| S-R4 | **Ngôn ngữ business-tone không academic** — đề cương v3.1 dùng "ăn tiền", "ROI -95%", "tiết kiệm 3-5 ngày công" → ngôn ngữ marketing/business pitch, KHÔNG academic Vietnamese | HIGH | Khung UTC mặc định formal academic |
| S-R5 | **Hiện trạng phân tích nông** — chương 1 cần phân tích sâu pain point + competitive landscape (BeeClass, Moodle, Google Classroom, Misa EMIS) + theory background. Đề cương v3.1 chỉ mention BeeClass | MED | Khung Chương 1 §1 "hiện trạng" + §2 "bài toán" |
| S-R6 | **Bài toán pháp lý giáo dục thiếu** — brief §27 "luật giáo dục" → cần trích Luật Giáo dục 2019, Thông tư BGD&ĐT về quản lý trung tâm ngoại ngữ + tin học, Nghị định 86/2018/NĐ-CP về hợp tác đầu tư giáo dục, PDPL 2023 — đề cương cũ không nhắc | HIGH | Brief §27 |
| S-R7 | **Bố cục 60 trang không cân đối** — nếu chương 4 "trình bày đầy đủ" thì rủi ro chương 4 nuốt 30+ trang → chương 1-2 còn ~20 trang để cover cơ sở lý luận + phân tích thiết kế = thiếu | MED | Brief §31, max 60 trang |
| S-R8 | **Phụ lục evidence sign-off chưa kế hoạch khoa học** — brief §26 "bản nhận xét, ký tên (quan trọng)" — cần template biên bản nghiệm thu, mẫu phiếu khảo sát chuẩn + phương pháp định lượng (Likert scale, NPS) thay vì free-form | MED | Khoa học evaluation methodology |
| S-R9 | **Quy trình phát triển phần mềm chưa nêu** — khung Chương 1 cuối yêu cầu "quy trình phát triển phần mềm (developer)" → cần trình bày Agile/Scrum/Wave methodology nhưng KHÔNG dùng jargon dev "wave" — phải gọi "iteration plan" hoặc "sprint cycle" theo Scrum lý thuyết | HIGH | Khung UTC §Chương 1 cuối |
| S-R10 | **Code snippet chương 3 quá raw** — nếu copy code source thẳng vào báo cáo sẽ vi phạm "ngôn ngữ academic". Cần pseudo-code + giải thích thuật toán + UML class diagram đi kèm | MED | Chuẩn báo cáo academic UTC |

### Recommendations

1. **Tạo glossary dev-jargon → academic-term:** GAP → "yêu cầu cải tiến / issue", wave → "đợt phát triển / iteration", PR → "đề xuất tích hợp code / merge request", squash merge → "tích hợp gọn", deploy → "triển khai sản xuất", rollback → "hoàn nguyên phiên bản", Claude / agent → ẨN HOÀN TOÀN. Apply qua sed/script trước khi ship `.docx`.
2. **Build bibliography ≥30 entries** — chia category: (a) Microservices architecture: Newman "Building Microservices" + Fowler patterns, (b) Multi-tenant SaaS: Krebs paper + AWS whitepaper, (c) AI in education: paper Springer/IEEE EdTech, (d) Cloud-native: CNCF + The Twelve-Factor App, (e) Education law VN: Luật Giáo dục 2019 + thông tư BGD&ĐT.
3. **UML formal mandatory:** ≥1 Use Case Diagram tổng + ≥3 Sequence Diagram (login, AI provision instance, parent payment), ≥1 Class Diagram core domain, ≥1 Component Diagram architecture. Dùng PlantUML render PNG (đã có trong `documents/06-diagrams/`).
4. **Ngôn ngữ academic check pass:** rewrite phần "ăn tiền" → "có giá trị thương mại cao", "ROI -95%" → "tỷ suất sinh lời âm 95%, chi phí vượt lợi ích", "tiết kiệm 3-5 ngày công" → "giảm thời gian thực hiện từ 3-5 ngày xuống còn 3-5 phút".
5. **Chương 1 hiện trạng phân tích sâu:** so sánh ma trận 4-5 hệ thống (BeeClass, Moodle, Google Classroom, Misa EMIS, MyAloha) theo 8 tiêu chí (chức năng, giá, kiến trúc, multi-tenant, mobile, AI, integration, deployment) → bảng so sánh + nhận xét.
6. **Mục pháp lý chương 1:** bổ sung subsection riêng "Khung pháp lý" trích dẫn Luật Giáo dục 2019, Thông tư 21/2018/TT-BGDĐT (quản lý trung tâm ngoại ngữ tin học), PDPL 2023, Nghị định 13/2023/NĐ-CP, Luật An ninh mạng 2018.
7. **Page budget cân đối:** Bìa+frontmatter 5-7 trang, Mở đầu 2 trang, Chương 1 ~12 trang, Chương 2 ~18 trang, Chương 3 ~8 trang, Chương 4 ~12 trang, Kết luận+TLTK 3-5 trang → tổng 60-62 trang.
8. **Template biên bản nghiệm thu beta:** thiết kế mẫu Word/Excel với cột (Trung tâm, Người ký, Ngày, Chức năng đánh giá, Likert 1-5, NPS 0-10, Nhận xét, Chữ ký) → ship như `documents/08-thesis/templates/beta-feedback-template.docx`.
9. **Quy trình phát triển dùng tên academic:** Scrum + iteration-based development; phụ lục có thể đề cập agile + một số technical-doc (KHÔNG dùng từ "wave" trong main body).
10. **Chương 3 trình bày kết hợp:** pseudo-code + Class Diagram + giải thích thuật toán; code thực tế chỉ trích ≤30 dòng/snippet với syntax highlighting.

---

## Persona 2 — Lenient examiner findings (giảng viên chấm thoáng, industry-friendly)

**Mindset:** Đánh giá cao modern tech stack + cloud-native + microservices + AI integration + multi-tenant SaaS + real user evidence. Format quan trọng nhưng KHÔNG bắt bẻ jargon nếu jargon thông dụng industry. Focus chương 4 triển khai + value delivered + complexity demonstrated.

### Risks identified

| ID | Risk | Severity | Source |
|----|------|----------|--------|
| L-R1 | **Chương 4 "trình bày đầy đủ" rủi ro overshooting** — nếu dump tất cả AWS architecture + Helm + K8s + Terraform + CloudWatch dashboard → người chấm bị overload, mất focus value | HIGH | Brief §31 "trình bày hết dữ liệu" |
| L-R2 | **Evidence real user thiếu** — brief §24-26: 2 giáo viên đơn lẻ + 2 business (trial + VIP) chưa có evidence thật → chấm thoáng vẫn cần ≥1 screenshot UI thật + ≥1 metric real (signup count, page-view, time-on-task) | HIGH | Industry-friendly examiner expects concrete outcomes |
| L-R3 | **AI integration story quá technical, ít business angle** — đề cương v3.1 nói "$0.19/instance" + "30 giây" → cần thêm "tiết kiệm bao nhiêu giờ designer", "tăng conversion bao nhiêu %", "user satisfaction Likert score" | MED | "Value delivered" mindset |
| L-R4 | **Multi-tenancy isolation evidence:** brief mention "database-per-tenant" — cần screenshot 2 tenant database list + log isolation test + RLS policy snippet để demonstrate isolation working | MED | Industry-credible architecture proof |
| L-R5 | **SLO/SLA metric** — chương 4 cần uptime %, P95 latency, error rate, MTTR per DORA — đề cương v3.1 không nhắc | HIGH | Industry deploy maturity |
| L-R6 | **Cost analysis chỉ có $0.19/instance AI cost, thiếu total cost model** — cần TCO breakdown (compute + storage + bandwidth + AI per tenant per month) + comparison vs alternatives (Moodle self-host, BeeClass subscription) | MED | Business-credible analysis |
| L-R7 | **Mobile-first / responsive story thiếu** — VN edu user 70%+ mobile (theo Google research) — cần screenshot mobile responsive + Chrome DevTools mobile emulation | MED | Modern UX expectation |
| L-R8 | **Security narrative thiếu evidence** — OWASP Top 10 check, JWT auth flow, RLS multi-tenant, secrets management — cần ≥1 đoạn demonstrate (Trivy scan result, OWASP ZAP scan summary, JWT diagram) | MED | Security-conscious examiner |
| L-R9 | **Demo video không gắn QR code link** — manual PDF + video tutorial cần QR ở phụ lục để examiner scan thử trên điện thoại lúc chấm | LOW | Examiner experience |
| L-R10 | **CI/CD pipeline diagram thiếu** — GitHub Actions workflow visualization (build → test → scan → deploy) thường gây ấn tượng industry | LOW | Modern dev workflow proof |

### Recommendations

1. **Chương 4 cấu trúc 3 phần cân đối:** (a) Setup môi trường + infrastructure (AWS Free Tier Singapore, K8s, Helm, secret rotation) ~4 trang, (b) Deploy + smoke test + rollback runbook ~4 trang, (c) Kết quả sử dụng + SLO metric + user feedback ~4 trang. Tổng ~12 trang.
2. **Evidence beta dù mock** — nếu chưa có data thật, mock 4 user persona (2 teacher + 2 business) với mock feedback table + Likert 1-5 + signed acceptance template (kèm chú thích "mẫu dùng cho thu thập thực tế giai đoạn beta sau khi báo cáo nộp"). Tránh trống.
3. **AI integration business case:** thêm bảng so sánh "Trước AI Agent: thuê designer 5 ngày × 500k VND = 2.5M VND/instance" vs "Sau AI Agent: $0.19 × 24500 VND = ~4700 VND/instance" → tiết kiệm 99.8% chi phí branding initial.
4. **Multi-tenant isolation proof:** screenshot 2 PostgreSQL database `kitehub` + `kiteclass_tenant1` + `kiteclass_tenant2` + log RLS policy enforce; Row Level Security policy code snippet (≤15 dòng).
5. **SLO dashboard:** ship CloudWatch + Grafana dashboard screenshot với 4 SLO (Uptime 99.5% target, P95 latency <500ms, Error rate <1%, MTTR <15min) + monthly trend chart 1-2 tháng beta.
6. **TCO bảng:** cost per tenant per month breakdown: EC2 share (free tier hết → $5), RDS share ($3), S3 backup ($0.50), CloudWatch ($1), AI per instance one-time ($0.19) → total ~$10/tenant/month, comparison BeeClass subscription ~150k VND/tháng ($6) Moodle self-host ~$15/tháng ban đầu cao + dev cost.
7. **Mobile responsive showcase:** Chrome DevTools mobile emulation iPhone 12 + Galaxy S20 screenshot 4 page (landing, dashboard center, attendance, parent portal) → 1 figure full-page 8 ảnh grid 4×2.
8. **Security audit summary:** 1 bảng OWASP Top 10 với cột "Mitigation áp dụng trong KiteClass" + 1 Trivy CVE scan kết quả pass + JWT auth sequence diagram.
9. **QR code phụ lục:** scan-to-watch link YouTube unlisted cho video manual + scan-to-read GitHub Pages link cho PDF manual.
10. **CI/CD pipeline diagram:** PlantUML render "GitHub push → CI build Java + Maven → Test Junit + JaCoCo → Trivy scan → Docker build → ECR push → Helm upgrade → Smoke test → Production" → 1 figure.

---

## Persona 3 — Advisor findings (giảng viên hướng dẫn academic mentor)

**Mindset:** Mentor — muốn student demonstrate understanding + scope discipline + tránh over-engineering trong báo cáo. Quan tâm thesis defense — student có thể trả lời sao khi hội đồng hỏi? Scope chốt v2.0.0 — không vẽ feature chưa có. 4 chương cân đối, evidence đầy đủ, có completion criteria + testing evidence.

### Risks identified

| ID | Risk | Severity | Source |
|----|------|----------|--------|
| A-R1 | **Scope creep risk: V4.1 outline có LMS + Marketing + Parent Portal + Gamification + AI Agent + VietQR** — release 2 thực tế cover được hết không? Cần map đối chiếu **mã code thực tế đã ship** ↔ chapters | CRITICAL | Brief §43 "không vẽ feature chưa có" |
| A-R2 | **v2.0.0 release status chưa verify** — brief §35 "release thành công v2.0.0 — chốt bản sẽ báo cáo" nhưng hiện đang Wave 91 (chưa release v2.0.0?). Cần state-check trước khi lock scope | CRITICAL | `audit-to-gap-pipeline.md` §2.5 |
| A-R3 | **Đề cương v3.1 v.s. v4.1 phiên bản** — đề cương cũ ở `documents/07-archived/` ghi V4.1 (đã refactor LMS + Marketing). Brief mention v2.0.0 — version mapping chưa rõ | HIGH | Inconsistency |
| A-R4 | **Personal info từ đề cương cũ chưa rõ** — brief §42 "thông tin cá nhân ở folder cũ, cụ thể đề cương" → đề cương v3.1 KHÔNG có địa chỉ + SĐT + email + người hướng dẫn. Cần check folder khác | HIGH | Brief §42 |
| A-R5 | **Người hướng dẫn (advisor) chưa name** — bìa khung UTC yêu cầu "Giảng viên hướng dẫn: ..." — chưa thấy trong action-2 | HIGH | Khung UTC bìa |
| A-R6 | **Defense Q&A chuẩn bị thiếu** — không có section "câu hỏi dự kiến + answer" hoặc rehearsal plan trong brief | MED | Defense prep |
| A-R7 | **Test evidence quá nông** — brief §24-26 "evidence end-user", "log", "bản nhận xét, ký tên" chưa định nghĩa metric pass/fail. Cần acceptance criteria rõ ràng cho 4 actor (giáo viên trial, giáo viên VIP, manager trial, manager VIP) | HIGH | Brief §24-26 + completion criteria |
| A-R8 | **Cost model thesis-relevant** — advisor hỏi "em làm gì khác đề tài khoa giao chỉ tiêu?" → cần demonstrate cost-benefit cho student work (thời gian, công cụ, AI) + originality (chứng minh KHÔNG copy từ BeeClass/Moodle) | MED | Defense angle |
| A-R9 | **Risk + future work section thiếu sâu** — kết luận khung UTC yêu cầu "hướng phát triển" → cần liệt kê 4-5 hướng concrete (K-12 expansion, mobile native app, AI tutoring, integration Zalo OA, marketplace giáo viên) | MED | Khung UTC kết luận |
| A-R10 | **References chuẩn tham khảo chéo:** brief §27 "trích dẫn tài liệu tham khảo thật" — cần kiểm tra mỗi reference link còn live (DOI, URL working) + format (Author, Title, Journal, Year, DOI/URL). Advisor cẩn thận sẽ check ngẫu nhiên 3-5 reference | MED | Academic integrity |

### Recommendations

1. **State-check release status NGAY:** chạy `git tag | grep v2`, `git log --oneline | grep release` → verify v2.0.0 đã tag chưa hay đang plan. Nếu chưa → đổi scope báo cáo thành "tiến độ tới Wave N, kế hoạch hoàn thành v2.0.0 trước defense date" + plan TLDR.
2. **Build feature inventory matrix:** mỗi feature trong V4.1 outline (LMS, Marketing, Parent Portal, Gamification, AI Agent, VietQR, etc.) → đối chiếu actual code status (`Implemented` / `Partial` / `Planned for v2.1`) → CHỈ trình bày `Implemented` + `Partial` trong báo cáo, KHÔNG vẽ Planned.
3. **Personal info checklist** — search trong:
   - `documents/07-archived/academic/` (đề cương v3.1 đã đọc, không có raw info)
   - `documents/08-thesis/` (đang trống)
   - User claudemd: vannkite@outlook.com (đã có)
   - Thiếu: Họ tên đầy đủ ✓ Nguyễn Văn Kiệt, MSSV ✓ 221230890, Lớp ✓ CNTT1-K63, Trường ✓ UTC, Khoa ?, Bộ môn ?, Email ✓, SĐT ?, Địa chỉ ?, **Giảng viên hướng dẫn ?**
4. **Bìa template:** tạo `documents/08-thesis/cover-template.md` với placeholder cho 4 field còn thiếu (Khoa, Bộ môn, SĐT, GVHD) — user fill in trước khi render final.docx.
5. **Defense Q&A appendix (KHÔNG main body):** prep 15-20 câu hỏi dự kiến với answer template:
   - "Tại sao chọn microservices cho KiteClass mà không cho KiteHub?" → ROI analysis + service count threshold theory.
   - "Multi-tenancy isolation đảm bảo bằng cách nào?" → DB-per-tenant + RLS + JWT tenant claim.
   - "AI Agent vs ChatGPT chỉ là API wrapper, đâu là điểm originality?" → 3-stage moderation pipeline + retry logic + cost optimization + Vietnamese-specific prompt engineering.
   - "Phương án scale lên 1000 tenant?" → K8s HPA + DB sharding strategy + cost projection.
   - "An toàn dữ liệu PDPL?" → PDPL 2023 compliance checklist + audit log + immutable log.
6. **Acceptance criteria rõ cho 4 actor beta:** mỗi actor có 5-10 task chuẩn (đăng ký → onboarding → daily task 1 → daily task 2 → ...) + pass/fail criteria + Likert score 1-5 → tổng matrix 4 actor × 8 task = 32 ô đánh giá.
7. **Originality statement:** mục riêng "Sự khác biệt so với hệ thống tham khảo (BeeClass + Moodle + Google Classroom)" — 1 trang chứng minh không copy.
8. **Future work concrete:** 5 hướng phát triển: (1) K-12 expansion + PDPL trẻ em compliance, (2) Mobile native React Native + offline-first, (3) AI tutoring conversation agent, (4) Zalo OA bot integration, (5) Marketplace giáo viên matching.
9. **Reference live-check checklist:** sample 5/30 references → curl URL trả 200, DOI resolver pass, paper accessible (Google Scholar OR open-access OR thư viện UTC subscription).
10. **Risk register cho defense:** dự đoán 3-5 risk hội đồng có thể flag → có sẵn câu trả lời + evidence reference (vd: "demo crash → backup video", "hosting outage → Vercel + CloudFlare fallback diagram", "AI cost overrun → quota cap config + sample log").

---

## Cross-persona convergence

### ALIGN — Findings cả 3 personas đồng thuận

| # | Convergence finding | Source personas |
|---|---|---|
| C-A1 | **Scope clarity + state-check release v2.0.0 là blocker đầu tiên** — strict cần để cân page budget (S-R7), lenient cần để evidence đúng (L-R2), advisor critical (A-R1, A-R2) | All 3 |
| C-A2 | **Evidence end-user là điểm yếu lớn** — strict cần methodology khoa học (S-R8), lenient cần real user metric (L-R2), advisor cần acceptance criteria (A-R7) | All 3 |
| C-A3 | **Reference quality cần upgrade** — strict đòi IEEE chuẩn ≥30 entries (S-R2), lenient ngầm chấp nhận nhưng vẫn cần basics, advisor live-check (A-R10) | All 3 |
| C-A4 | **Page budget 60 trang cần plan cứng** — strict cân (S-R7), lenient không muốn chương 4 quá dày (L-R1), advisor cân 4 chương | All 3 |
| C-A5 | **Personal info + GVHD field missing** — không trực tiếp surface ở strict/lenient nhưng advisor đè rõ (A-R4, A-R5) → critical bìa report block | Advisor primary, all support |
| C-A6 | **AI integration cần story balance** — strict chấp nhận nếu có science angle (machine learning theory), lenient yêu cầu business value (L-R3), advisor yêu cầu originality (A-R8) | All 3 |
| C-A7 | **Security + multi-tenant isolation evidence cần demonstrate** — không phải lý thuyết mà phải có code/screenshot/audit result | Strict (UML chuẩn) + Lenient (L-R4, L-R8) |

### DISAGREE — Findings 2 personas trái nhau

| # | Disagreement | Strict says | Lenient says | Advisor verdict |
|---|---|---|---|---|
| C-D1 | **Mức độ trình bày code chương 3** | Pseudo-code + UML mandatory, code raw rất hạn chế (S-R10) | Snippet thực tế OK với syntax highlighting; demonstrate working code | **Cân bằng:** pseudo-code + Class Diagram cho thuật toán phức tạp; snippet thực tế ≤30 dòng cho thao tác core (login flow, RLS query, AI generation request) |
| C-D2 | **Marketing copy / business pitch tone** | Reject "ăn tiền", "ROI -95%" — academic only (S-R4) | OK nếu kèm số liệu cụ thể demonstrate value | **Cân bằng:** rewrite academic phrasing trong main body, business pitch tone OK cho mở đầu + kết luận section "lợi ích thực hiện" với số liệu |
| C-D3 | **Quy trình phát triển phần mềm naming** | Reject "wave"/"Claude" jargon, dùng Scrum/iteration academic term (S-R9) | Industry-friendly accept "agile sprint", "wave"-like cycle nếu defined | **Strict wins:** rename "wave" → "iteration" hoặc "sprint cycle" theo Scrum, KHÔNG mention Claude/agent. Phụ lục technical-doc có thể keep raw term |
| C-D4 | **Mức depth competitive analysis** | Ma trận 4-5 hệ thống × 8 tiêu chí (S-R5) | TCO comparison BeeClass + Moodle là đủ (L-R6) | **Strict wins:** ma trận đầy đủ cho chương 1 "hiện trạng"; TCO chi tiết chỉ riêng chương 4 + 1 hệ thống đối thủ tiêu biểu |

### UNIQUE — Findings chỉ 1 persona surface

**Strict-unique:**
- S-R6 Bài toán pháp lý giáo dục (Luật Giáo dục 2019 + Thông tư BGD&ĐT) — strict mandatory cho chương 1; lenient + advisor không mention.
- S-R9 Quy trình phát triển phần mềm academic naming.
- S-R1 Jargon dev leak detection (strict primary; advisor mention nhẹ qua A-R6 defense Q&A).

**Lenient-unique:**
- L-R7 Mobile responsive showcase (VN 70%+ mobile user).
- L-R9 QR code phụ lục video manual.
- L-R10 CI/CD pipeline diagram industry impressive.

**Advisor-unique:**
- A-R1 Scope creep risk feature inventory matrix (V4.1 vs v2.0.0 release status).
- A-R4 Personal info + A-R5 Giảng viên hướng dẫn field missing.
- A-R6 Defense Q&A rehearsal plan.
- A-R8 Originality statement vs BeeClass/Moodle/Google Classroom.

---

## Top 5 actionable items cho thesis plan

Ưu tiên theo blast radius + dependency:

### 1. **State-check release v2.0.0 + scope lock matrix** (BLOCKER — phải làm đầu tiên)

**Why:** A-R1 + A-R2 + C-A1 — không biết v2.0.0 đã tag hay đang plan thì không cố định được scope báo cáo. V4.1 outline vs v2.0.0 release mapping chưa rõ.

**Action:**
- Chạy `git tag --sort=-creatordate | head -20` + check `documents/03-planning/roadmap/release-1-plan-2026.md` + `documents/03-planning/roadmap/release-2-plan-2026.md` (nếu có).
- Tạo feature inventory matrix `documents/08-thesis/feature-inventory-v2-release.md` với cột: Feature | V4.1 outline status | v2.0.0 release status | Code path | Evidence in report (Y/N).
- CHỈ trình bày features `Implemented` + `Partial` trong main report; `Planned` chuyển sang "future work" kết luận.

### 2. **Personal info + giảng viên hướng dẫn template + bìa standard**

**Why:** A-R4 + A-R5 — bìa khung UTC bắt buộc 5 field (Họ tên SV, MSSV, Lớp, GVHD, Năm) + cấp thông tin (Khoa, Bộ môn, Trường). Hiện thiếu Khoa, Bộ môn, SĐT, GVHD.

**Action:**
- Tạo `documents/08-thesis/cover-info.md` với template Markdown placeholder rõ ràng:
  - Họ và tên: Nguyễn Văn Kiệt
  - MSSV: 221230890
  - Lớp: CNTT1-K63
  - Khoa: [USER FILL — Khoa Công nghệ thông tin?]
  - Bộ môn: [USER FILL]
  - Trường: ĐH Giao thông Vận tải (UTC)
  - Email: vannkite@outlook.com
  - SĐT: [USER FILL]
  - Giảng viên hướng dẫn: [USER FILL — đây là blocker, user phải confirm tên + chức danh]
- Block thesis docx render cho đến khi 4 placeholder fill xong.

### 3. **Dev-jargon → academic glossary + sed pipeline**

**Why:** S-R1 + brief §38 — `documents/` source có dày đặc "wave", "GAP", "PR", "Claude", "deploy", "rollback", "BETA cohort". User explicit ban trong main report. Strict examiner reject ngay.

**Action:**
- Tạo `documents/08-thesis/glossary-dev-to-academic.md` với 2 cột Source term → Replacement.
- Mapping core:
  - `wave 92` / `Wave 92` → `iteration 92` hoặc `đợt phát triển 92` (Scrum sprint terminology)
  - `GAP-XXX` → `mục cải tiến XXX` hoặc ẨN ID, chỉ giữ description
  - `PR #1234` → ẨN HOÀN TOÀN, chỉ giữ "thay đổi tích hợp"
  - `Claude` / `Claude Code` / `Claude agent` → ẨN HOÀN TOÀN, không xuất hiện
  - `squash merge` → `tích hợp gọn`
  - `deploy production` → `triển khai môi trường sản xuất`
  - `rollback` → `hoàn nguyên phiên bản`
  - `BETA cohort` → `nhóm dùng thử`
  - `audit-gate.py` / hook / lint → `quy trình kiểm tra tự động`
  - `DevOps` / `DORA metric` → giữ (industry-standard academic acceptable)
- Build script `scripts/thesis/sanitize-dev-jargon.sh` áp dụng `sed` mass-replace trên docx source TRƯỚC khi render final.

### 4. **Page budget 60 trang cứng + chapter outline detailed**

**Why:** C-A4 + S-R7 + L-R1 — không có plan cứng → chương 4 phình to nuốt budget.

**Action:**
- Tạo `documents/08-thesis/page-budget.md` với khóa cứng:

| Mục | Trang | Notes |
|-----|-------|-------|
| Bìa + frontmatter (mục lục, danh mục) | 5-7 | Không số trang |
| Mở đầu | 2 | Lý do, mục tiêu, phạm vi, phương pháp, bố cục |
| Chương 1 — Cơ sở lý luận | 12 | §1.1 Hiện trạng (3) + §1.2 Bài toán (2) + §1.3 Khung pháp lý (1.5) + §1.4 Công nghệ (3.5) + §1.5 Quy trình phát triển (2) |
| Chương 2 — Phân tích Thiết kế | 18 | §2.1 Yêu cầu chức năng (5) + §2.2 Phi chức năng (2) + §2.3 Kiến trúc (4) + §2.4 ERD (3) + §2.5 Nghiệp vụ chính SaaS + B-learning (4) |
| Chương 3 — Lập trình | 8 | §3.1 UI design (2) + §3.2 UC chính (3) + §3.3 Code đại diện (2) + §3.4 Dữ liệu đại diện (1) |
| Chương 4 — Triển khai | 12 | §4.1 Cài đặt + Infrastructure (4) + §4.2 Kiểm thử + Deploy (4) + §4.3 Kết quả + SLO + Feedback (4) |
| Kết luận | 2 | Lợi ích, đánh giá, hướng phát triển |
| Tài liệu tham khảo | 3 | ≥30 entries IEEE format |
| **Tổng** | **62** | Phụ lục KHÔNG tính (manual, evidence, Q&A) |

### 5. **Image folder + auto-generation pipeline cho diagrams + screenshots**

**Why:** Brief §39-40 "folder riêng chưa ID ảnh", "tự tạo diagram phục vụ chương 1,2,3,4", "ảnh chụp FE dùng tools chụp" → mục tiêu dev không sửa tay.

**Action:**
- Tạo cấu trúc folder `documents/08-thesis/figures/` với subfolder:
  - `ch1-co-so-ly-luan/` — so sánh competitor matrix, architecture pattern theory diagram
  - `ch2-phan-tich/` — Use Case Diagram (UML), ERD, Component Diagram, Sequence Diagram
  - `ch3-lap-trinh/` — UI mockup (Figma export), Class Diagram, code snippet syntax highlight
  - `ch4-trien-khai/` — AWS architecture diagram, Helm chart structure, CloudWatch dashboard screenshot, mobile responsive grid, CI/CD pipeline
- Reuse `documents/06-diagrams/` PlantUML PNG (đã có).
- Script `scripts/thesis/capture-fe-screenshots.sh` dùng Playwright auto-screenshot 10-15 page (landing, pricing, signup, dashboard center, attendance, parent portal, mobile emulation) — KHÔNG dev chụp tay.
- Naming convention: `ch{N}-{NN}-{slug}.png` (ví dụ `ch4-01-aws-architecture.png`).

---

## Status

**Outside-in audit T1 COMPLETE** — findings ready để merge vào thesis plan §1 Brainstorm Q1.

**Next steps (handoff coordinator):**

1. Spawn T2 outside-in agent: **benchmark external** — research thesis chuẩn UTC + sample thesis IT đạt giỏi/xuất sắc + reference style IEEE/APA cho Vietnamese tech thesis.
2. Spawn T3 outside-in agent: **3-axis simulation gap-finder** — actor × phase × format axis (4 actor beta × 4 chương × 5 deliverable type).
3. Cross-merge T1+T2+T3 findings vào thesis plan §1 Brainstorm.
4. Lock scope sau khi user confirm Top-5 actionable + giảng viên hướng dẫn field fill.

**Files produced:**
- `documents/04-quality/audits/persona-review/2026-05-18-thesis-plan-persona-simulation.md` (this file)

**Rules satisfied:**
- `outside-in-coverage-trigger.md` §3 Bước 4 (spawn agents song song outside-in)
- `dev-readable-doc-language.md` §2 (Vietnamese narrative + English technical identifiers preserved)
- `output-review-mandate.md` §3 row "Audit reports" (audit-report standard followed)
- `agent-action-bias.md` §1 Part A (do it yourself — audit shipped without proposing user manual steps)
